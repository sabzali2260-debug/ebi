package com.separ.alert

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

/**
 * Embedded civil-warning style siren. It is generated locally as PCM:
 * no ringtone, network access, downloaded pack or installed TTS voice is required.
 */
class SirenPlayer {
    private val running = AtomicBoolean(false)
    @Volatile private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    @Synchronized
    fun start(volume: Float = 1.0f) {
        stop()
        val sampleRate = 48_000
        val chunkSamples = 2_048
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(chunkSamples * 2 * 6)
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }

        track.setVolume(volume.coerceIn(0.1f, 1.0f))
        track.play()
        audioTrack = track
        running.set(true)

        worker = Thread({
            val samples = ShortArray(chunkSamples)
            var phaseA = 0.0
            var phaseB = 0.0
            var elapsed = 0.0
            val step = 1.0 / sampleRate
            while (running.get()) {
                for (i in samples.indices) {
                    // Repeating rising/falling wail with a faster secondary warning tone.
                    val sweep = 0.5 - 0.5 * kotlin.math.cos(2.0 * PI * elapsed / 4.6)
                    val baseHz = 430.0 + 560.0 * sweep
                    val secondaryHz = 690.0 + 250.0 * (0.5 + 0.5 * sin(2.0 * PI * elapsed / 2.3))
                    phaseA += 2.0 * PI * baseHz / sampleRate
                    phaseB += 2.0 * PI * secondaryHz / sampleRate
                    if (phaseA >= 2.0 * PI) phaseA -= 2.0 * PI
                    if (phaseB >= 2.0 * PI) phaseB -= 2.0 * PI

                    // Strong, layered tone with controlled harmonics; soft limiting avoids digital clipping.
                    val fundamental = 0.62 * sin(phaseA) + 0.25 * sin(phaseB)
                    val harmonics = 0.10 * sin(phaseA * 2.0) + 0.05 * sin(phaseB * 2.0)
                    val raw = fundamental + harmonics
                    val limited = raw / (1.0 + 0.32 * abs(raw))
                    val fade = minOf(1.0, elapsed / 0.08)
                    val value = limited * fade * 0.92
                    samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(-32767,32767).toShort()
                    elapsed += step
                }
                if (!running.get()) break
                if (track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING) < 0) break
            }
        }, "SEPAR-Civil-Warning-Siren").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        running.set(false)
        try { worker?.join(500) } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        worker = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (_: Exception) { }
        try { audioTrack?.release() } catch (_: Exception) { }
        audioTrack = null
    }
}
