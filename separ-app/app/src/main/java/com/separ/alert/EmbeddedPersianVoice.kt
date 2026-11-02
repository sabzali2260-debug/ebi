package com.separ.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * Plays Persian announcement recordings packaged in app assets.
 * The files are generated during the reproducible CI build and work offline.
 */
class EmbeddedPersianVoice(private val context: Context) {
    private var player: MediaPlayer? = null
    private var tempFile: File? = null

    @Synchronized
    fun play(profile: String) {
        stop()
        val assetName = if (profile == "male") "voice_male.mp3" else "voice_female.mp3"
        try {
            val output = File(context.cacheDir, assetName)
            context.assets.open(assetName).use { input ->
                output.outputStream().use { target -> input.copyTo(target) }
            }
            tempFile = output
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            media.setOnCompletionListener { releasePlayer() }
            media.setOnErrorListener { _, _, _ -> releasePlayer(); true }
            media.setDataSource(output.absolutePath)
            media.setVolume(1.0f, 1.0f)
            media.prepareAsync()
            media.setOnPreparedListener { if (player === media) media.start() }
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    @Synchronized
    fun stop() {
        releasePlayer()
    }

    @Synchronized
    private fun releasePlayer() {
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
        try { tempFile?.delete() } catch (_: Exception) { }
        tempFile = null
    }
}
