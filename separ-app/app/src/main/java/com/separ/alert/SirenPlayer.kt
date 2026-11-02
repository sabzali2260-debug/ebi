package com.separ.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Plays the air-raid siren supplied by the app owner.
 * The MP3 is bundled in assets and loops until the alert is stopped.
 */
class SirenPlayer {
    @Volatile private var player: MediaPlayer? = null

    @Synchronized
    fun start(context: Context, volume: Float = 1.0f) {
        stop()
        try {
            val mp = MediaPlayer()
            val afd = context.assets.openFd("air_raid_siren.mp3")
            try {
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } finally {
                afd.close()
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.isLooping = true
            val safeVolume = volume.coerceIn(0.05f, 1.0f)
            mp.setVolume(safeVolume, safeVolume)
            mp.setOnPreparedListener { it.start() }
            mp.setOnErrorListener { failed, _, _ ->
                try { failed.release() } catch (_: Exception) { }
                synchronized(this) { if (player === failed) player = null }
                true
            }
            player = mp
            mp.prepare()
            if (player === mp && !mp.isPlaying) mp.start()
        } catch (_: Exception) {
            stop()
        }
    }

    @Synchronized
    fun stop() {
        val old = player
        player = null
        try { old?.stop() } catch (_: Exception) { }
        try { old?.reset() } catch (_: Exception) { }
        try { old?.release() } catch (_: Exception) { }
    }
}
