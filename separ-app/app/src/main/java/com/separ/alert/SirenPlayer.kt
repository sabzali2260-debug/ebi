package com.separ.alert

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/** Continuous rising/falling siren. Uses streamed PCM rather than a short system beep. */
class SirenPlayer {
    private val running = AtomicBoolean(false)
    @Volatile private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    @Synchronized
    fun start(volume: Float = 0.82f) {
        stop()
        val sampleRate = 44_100
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
            .setBufferSizeInBytes(chunkSamples * 2 * 4)
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
            var phase = 0.0
            var elapsed = 0.0
            val step = 1.0 / sampleRate
            while (running.get()) {
                for (i in samples.indices) {
                    val sweep = 0.5 + 0.5 * sin(2.0 * PI * elapsed / 3.2)
                    val frequency = 620.0 + 460.0 * sweep
                    phase += 2.0 * PI * frequency / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    val pulse = 0.82 + 0.18 * sin(2.0 * PI * elapsed / 0.72)
                    samples[i] = (sin(phase) * 0.78 * pulse * Short.MAX_VALUE)
                        .toInt().toShort()
                    elapsed += step
                }
                if (!running.get()) break
                if (track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING) < 0) break
            }
        }, "SEPAR-Siren").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        running.set(false)
        try { worker?.join(350) } catch (_: InterruptedException) {
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
