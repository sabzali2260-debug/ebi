package com.separ.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

class AlertService : Service() {
    private val siren = SirenPlayer()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speech: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }
    private var level = 2
    private var city = "کل ایران"
    private var voiceProfile = "female"
    private var isTest = true
    private var message = ""
    private var stopped = false

    private val repeatSpeech = object : Runnable {
        override fun run() {
            if (stopped || level < 3 || !getSharedPreferences("separ_prefs", MODE_PRIVATE)
                    .getBoolean("repeat_speech", true)) return
            speakAlert()
            mainHandler.postDelayed(this, 22_000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopped = false
        level = (intent?.getIntExtra(EXTRA_LEVEL, 2) ?: 2).coerceIn(1, 3)
        city = intent?.getStringExtra(EXTRA_CITY) ?: "کل ایران"
        voiceProfile = intent?.getStringExtra(EXTRA_VOICE) ?: "female"
        isTest = intent?.getBooleanExtra(EXTRA_TEST, true) ?: true
        val minimumLevel = getSharedPreferences("separ_prefs", MODE_PRIVATE)
            .getInt("minimum_level", 1).coerceIn(1, 3)
        if (!isTest && level < minimumLevel) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val levelLabel = levelLabel(level)
        message = if (isTest) {
            "آزمایش سطح $levelLabel برنامهٔ سپر. محدودهٔ انتخاب‌شده: $city. این پیام واقعی نیست."
        } else {
            "هشدار سپر برای محدودهٔ $city. سطح $levelLabel. اطلاعیهٔ منبع را بررسی کنید."
        }

        createNotificationChannel()
        startForegroundCompat(buildNotification())
        acquireWakeLock()
        requestAlarmAudioFocus()

        if (level >= 2) {
            val volume = getSharedPreferences("separ_prefs", MODE_PRIVATE)
                .getInt("siren_volume", 100).coerceIn(10, 100) / 100f
            siren.start(if (level == 3) (volume * 1.08f).coerceAtMost(1.0f) else volume)
            startVibration()
        }
        initializeSpeech()
        if (level == 3) mainHandler.postDelayed(repeatSpeech, 22_000L)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, StopAlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val screenIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, AlertActivity::class.java)
                .putExtra(EXTRA_LEVEL, level)
                .putExtra(EXTRA_CITY, city)
                .putExtra(EXTRA_TEST, isTest),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val allowFullScreen = level >= 2 && (
            Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("سپر | سطح $level · ${levelLabel(level)}")
            .setContentText(if (isTest) "آزمایش برای $city" else "اعلان هشدار برای $city")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(screenIntent)
            .setFullScreenIntent(screenIntent, allowFullScreen)
            .addAction(android.R.drawable.ic_media_pause, "توقف هشدار", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدارهای سپر",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های هشدار و آزمایش آژیر سپر"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(level >= 2)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun requestAlarmAudioFocus() {
        try {
            val manager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager = manager
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } catch (_: Exception) {
        }
    }

    private fun acquireWakeLock() {
        try {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SEPAR:Alert")
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (_: Exception) {
        }
    }

    private fun startVibration() {
        val prefs = getSharedPreferences("separ_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("vibration", true)) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = if (level == 3) {
                longArrayOf(0, 850, 160, 850, 160, 1200, 220)
            } else {
                longArrayOf(0, 350, 240, 350, 520)
            }
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun initializeSpeech() {
        try { speech?.stop(); speech?.shutdown() } catch (_: Exception) { }
        speech = TextToSpeech(this) { status ->
            if (stopped || status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = speech ?: return@TextToSpeech
            val prefs = getSharedPreferences("separ_prefs", MODE_PRIVATE)
            val key = if (voiceProfile == "male") "male_voice_name" else "female_voice_name"
            val selectedName = prefs.getString(key, null)
            val selectedVoice = engine.voices?.firstOrNull {
                it.name == selectedName && it.locale.language == "fa"
            }
            if (selectedVoice != null) {
                engine.setVoice(selectedVoice)
            } else {
                val languageStatus = engine.setLanguage(Locale("fa", "IR"))
                if (languageStatus < TextToSpeech.LANG_AVAILABLE) {
                    updateNotificationSpeechUnavailable()
                    return@TextToSpeech
                }
            }
            val speechRate = prefs.getInt("speech_rate", 93).coerceIn(75, 115) / 100f
            engine.setSpeechRate(speechRate)
            engine.setPitch(1.0f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { }
                override fun onDone(utteranceId: String?) { }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { }
            })
            speakAlert()
        } 
    }

    private fun speakAlert() {
        if (stopped) return
        try {
            speech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "separ-alert-${System.currentTimeMillis()}")
        } catch (_: Exception) {
        }
    }

    private fun updateNotificationSpeechUnavailable() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("سپر | موتور گفتار فارسی در دسترس نیست")
                .setContentText("صدای فارسی برای پروفایل انتخاب‌شده روی این گوشی نصب نیست.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .build()
        )
    }

    private fun levelLabel(value: Int): String = when (value) {
        1 -> "اطلاع‌رسانی"
        2 -> "هشدار"
        else -> "اضطراری"
    }

    override fun onDestroy() {
        stopped = true
        mainHandler.removeCallbacksAndMessages(null)
        siren.stop()
        try {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } catch (_: Exception) { }
        audioFocusRequest = null
        audioManager = null
        try { speech?.stop(); speech?.shutdown() } catch (_: Exception) { }
        speech = null
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) { }
        wakeLock = null
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).cancel()
            }
        } catch (_: Exception) { }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_CITY = "extra_city"
        const val EXTRA_VOICE = "extra_voice"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_TEST = "extra_test"
        private const val CHANNEL_ID = "separ_alert_channel_v3"
        private const val NOTIFICATION_ID = 731
    }
}
