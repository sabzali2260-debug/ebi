package com.separ.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class AlertService : Service() {
    private var tone: ToneGenerator? = null
    private var speech: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val city = intent?.getStringExtra(EXTRA_CITY) ?: "محدودهٔ انتخاب‌شده"
        val voice = intent?.getStringExtra(EXTRA_VOICE) ?: "مردانه"
        createNotificationChannel()

        val stopIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, StopAlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val screenIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, AlertActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("سپر | هشدار آزمایشی")
            .setContentText("آزمایش صدا برای $city")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(screenIntent, true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "توقف",
                stopIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val manager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SEPAR:DemoAlert")
            wakeLock?.acquire(180_000L)
        } catch (_: Exception) {
            // The alert demo remains active even if a wake lock is unavailable.
        }

        try {
            tone?.release()
            tone = ToneGenerator(AudioManager.STREAM_ALARM, 100).also {
                it.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 5000)
            }
        } catch (_: Exception) {
            // Some devices do not expose the requested system tone.
        }

        speech?.shutdown()
        speech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = speech ?: return@TextToSpeech
                engine.setLanguage(Locale("fa", "IR"))
                engine.speak(
                    "این یک پیام آزمایشی از برنامهٔ سپر است. محدودهٔ انتخاب‌شده: $city. صدای انتخابی: $voice.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "separ-demo-alert"
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدارهای سپر",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های آزمایشی برنامهٔ سپر"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            tone?.stopTone()
            tone?.release()
        } catch (_: Exception) {
        }
        tone = null
        speech?.stop()
        speech?.shutdown()
        speech = null
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_CITY = "extra_city"
        const val EXTRA_VOICE = "extra_voice"
        private const val CHANNEL_ID = "separ_alert_channel"
        private const val NOTIFICATION_ID = 731
    }
}
