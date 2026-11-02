package com.separ.alert

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("separ_prefs", MODE_PRIVATE) }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var persianVoices: List<Voice> = emptyList()
    private lateinit var maleVoicePicker: Spinner
    private lateinit var femaleVoicePicker: Spinner
    private lateinit var voiceStatus: TextView
    private lateinit var cityPicker: Spinner
    private lateinit var minimumLevel: RadioGroup

    private val navy = Color.rgb(19, 43, 68)
    private val muted = Color.rgb(89, 105, 122)
    private val background = Color.rgb(244, 247, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(background)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("سپر | SEPAR", 30, navy, true))
        root.addView(label("سامانهٔ هشدار و اطلاع‌رسانی", 16, muted))
        root.addView(label("نسخهٔ ۰٫۳٫۰ · آزمایش قابلیت‌های دستگاه", 12, muted))

        val dataCard = card(root, "وضعیت منابع و داده‌ها")
        dataCard.addView(message(
            "اتصال زنده به فهرست رسمی NOTAM هنوز برقرار نشده است. این نسخه هیچ‌وقت از کم‌شدن تعداد هواپیماها نتیجهٔ «بسته‌شدن آسمان» نمی‌گیرد."
        ))
        dataCard.addView(actionButton("بازکردن Iran AIM (منبع رسمی هوانوردی ایران)") {
            openUrl("https://ais.airport.ir")
        })
        dataCard.addView(actionButton("بازکردن هشدارهای ایمنی EASA دربارهٔ ایران") {
            openUrl("https://www.easa.europa.eu/en/domains/air-operations/czibs")
        })
        dataCard.addView(actionButton("مشاهدهٔ دستی پروازها در Flightradar24") {
            openUrl("https://www.flightradar24.com/")
        })
        dataCard.addView(message(
            "Flightradar24 فقط برای مشاهدهٔ دستی ترافیک پرواز است؛ دادهٔ آن مبنای هشدار ایمنی یا تشخیص بسته‌شدن حریم هوایی نیست."
        ))

        val mapCard = card(root, "نقشهٔ ایران")
        mapCard.addView(message(
            "نقشهٔ تعاملی، شهرهای اصلی و مرکز جغرافیایی ایران. لایهٔ محدودیت‌ها و مناطق NOTAM تا زمان اتصال منبع معتبر به‌صورت قطعی نمایش داده نمی‌شود."
        ))
        mapCard.addView(actionButton("بازکردن نقشهٔ تعاملی ایران") {
            startActivity(Intent(this, IranMapActivity::class.java))
        })

        val testCard = card(root, "آزمایش سه سطح هشدار")
        testCard.addView(message(
            "این دکمه‌ها آزمایش محلی هستند و دریافت هشدار واقعی را شبیه‌سازی نمی‌کنند. سطح ۲ و ۳ آژیر پیوسته و صفحهٔ قرمز چشمک‌زن دارند."
        ))
        testCard.addView(actionButton("سطح ۱ · اطلاع‌رسانی (بدون آژیر)") {
            launchAlert(1, true)
        })
        testCard.addView(actionButton("سطح ۲ · هشدار (آژیر + گفتار + لرزش)") {
            launchAlert(2, true)
        })
        testCard.addView(actionButton("سطح ۳ · اضطراری (آژیر پیوسته + تکرار پیام)") {
            launchAlert(3, true)
        })
        testCard.addView(actionButton("توقف فوری آژیر و هشدار") {
            stopService(Intent(this, AlertService::class.java))
        })

        val settingsCard = card(root, "تنظیمات هشدار")
        settingsCard.addView(label("حداقل سطحی که به‌صورت هشدار اعلام شود", 15, navy, true))
        minimumLevel = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val levelOptions = listOf(
            1 to "همهٔ سطوح (۱، ۲ و ۳)",
            2 to "فقط هشدار و اضطراری (۲ و ۳)",
            3 to "فقط اضطراری (۳)"
        )
        levelOptions.forEach { (level, text) ->
            minimumLevel.addView(RadioButton(this).apply {
                id = level
                this.text = text
                textSize = 14f
                isChecked = prefs.getInt("minimum_level", 1) == level
                setOnClickListener { prefs.edit().putInt("minimum_level", level).apply() }
            })
        }
        settingsCard.addView(minimumLevel)

        val flash = Switch(this).apply {
            text = "چشمک قرمز صفحه در هشدار سطح ۲ و ۳"
            isChecked = prefs.getBoolean("screen_flash", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("screen_flash", checked).apply()
            }
        }
        settingsCard.addView(flash, matchWidth())

        val vibration = Switch(this).apply {
            text = "لرزش هشدار"
            isChecked = prefs.getBoolean("vibration", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("vibration", checked).apply()
            }
        }
        settingsCard.addView(vibration, matchWidth())

        val repeatSpeech = Switch(this).apply {
            text = "تکرار گفتار در سطح اضطراری"
            isChecked = prefs.getBoolean("repeat_speech", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("repeat_speech", checked).apply()
            }
        }
        settingsCard.addView(repeatSpeech, matchWidth())

        settingsCard.addView(label("محدودهٔ موردنظر", 15, navy, true))
        cityPicker = Spinner(this)
        val cities = arrayOf(
            "کل ایران", "تهران", "کرج", "تبریز", "ارومیه", "رشت", "مشهد",
            "اصفهان", "اهواز", "شیراز", "بندرعباس", "کرمان", "زاهدان", "سنندج"
        )
        cityPicker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cities)
        settingsCard.addView(cityPicker, matchWidth())
        val savedCity = prefs.getString("selected_city", "کل ایران")
        cityPicker.setSelection(cities.indexOf(savedCity).coerceAtLeast(0))
        cityPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("selected_city", cities[position]).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val voiceCard = card(root, "مدیریت صدای فارسی زن و مرد")
        voiceCard.addView(message(
            "برنامه فهرست صداهای فارسی نصب‌شده روی گوشی را می‌خواند. دو صدا را جداگانه به پروفایل زنانه و مردانه اختصاص بده و با دکمهٔ آزمایش گوش کن. نام موتور به‌تنهایی جنسیت را تضمین نمی‌کند."
        ))
        voiceStatus = label("در حال بررسی موتور گفتار فارسی…", 13, muted)
        voiceCard.addView(voiceStatus)
        voiceCard.addView(label("صدای پروفایل مردانه", 14, navy, true))
        maleVoicePicker = Spinner(this)
        voiceCard.addView(maleVoicePicker, matchWidth())
        voiceCard.addView(actionButton("آزمایش صدای مردانه") { testVoice(maleVoicePicker, "این پیام آزمایشی با صدای انتخاب‌شده برای پروفایل مردانه پخش می‌شود.") })
        voiceCard.addView(label("صدای پروفایل زنانه", 14, navy, true))
        femaleVoicePicker = Spinner(this)
        voiceCard.addView(femaleVoicePicker, matchWidth())
        voiceCard.addView(actionButton("آزمایش صدای زنانه") { testVoice(femaleVoicePicker, "این پیام آزمایشی با صدای انتخاب‌شده برای پروفایل زنانه پخش می‌شود.") })
        voiceCard.addView(actionButton("تنظیمات موتور گفتار و نصب صدای فارسی") {
            try {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            } catch (_: Exception) {
                showToast("تنظیمات موتور گفتار در این گوشی در دسترس نیست.")
            }
        })

        val deviceCard = card(root, "مجوزهای لازم برای صفحهٔ قفل")
        deviceCard.addView(message(
            "نمایش صفحهٔ هشدار روی قفل به مجوز اعلان تمام‌صفحه و تنظیمات سازندهٔ گوشی وابسته است. اندروید ممکن است نمایش تمام‌صفحه را محدود کند؛ در این حالت اعلان با اولویت بالا نمایش داده می‌شود."
        ))
        deviceCard.addView(actionButton("بررسی / فعال‌سازی مجوز نمایش تمام‌صفحه") {
            requestFullScreenPermission()
        })
        deviceCard.addView(actionButton("تنظیمات اعلان‌های سپر") {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (_: Exception) {
                showToast("تنظیمات اعلان این دستگاه باز نشد.")
            }
        })

        root.addView(label(
            "هشدار ایمنی: این نسخه آژیر، گفتار، نقشه و اعلان محلی را آزمایش می‌کند؛ هنوز سامانهٔ تأییدشدهٔ هشدار عمومی یا تشخیص خودکار بسته‌شدن آسمان ایران نیست. برای تصمیم‌های ایمنی فقط اطلاعیه‌های رسمی را بررسی کن.",
            13, Color.rgb(150, 55, 28)
        ))

        initPersianTts()
    }

    private fun initPersianTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val engine = tts ?: return@TextToSpeech
                engine.setLanguage(Locale("fa", "IR"))
                persianVoices = engine.voices
                    ?.filter { it.locale.language.equals("fa", ignoreCase = true) }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                runOnUiThread { populateVoicePickers() }
            } else {
                runOnUiThread {
                    voiceStatus.text = "موتور گفتار فارسی راه‌اندازی نشد. از تنظیمات موتور گفتار، یک موتور سازگار نصب یا فعال کن."
                }
            }
        }
    }

    private fun populateVoicePickers() {
        if (persianVoices.isEmpty()) {
            voiceStatus.text = "هیچ صدای فارسی روی موتور گفتار فعلی پیدا نشد. تا نصب صدای فارسی، گفتار فارسی در دسترس نیست."
            val unavailable = listOf("صدای فارسی نصب نیست")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, unavailable)
            maleVoicePicker.adapter = adapter
            femaleVoicePicker.adapter = adapter
            return
        }

        val names = persianVoices.map { voice ->
            val quality = when (voice.quality) {
                Voice.QUALITY_VERY_HIGH -> "کیفیت بسیار بالا"
                Voice.QUALITY_HIGH -> "کیفیت بالا"
                Voice.QUALITY_NORMAL -> "کیفیت معمولی"
                Voice.QUALITY_LOW -> "کیفیت پایین"
                else -> "کیفیت نامشخص"
            }
            voice.name + " · " + quality + if (voice.isNetworkConnectionRequired) " · آنلاین" else " · آفلاین"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        maleVoicePicker.adapter = adapter
        femaleVoicePicker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)

        val maleSaved = prefs.getString("male_voice_name", null)
        val femaleSaved = prefs.getString("female_voice_name", null)
        maleVoicePicker.setSelection(persianVoices.indexOfFirst { it.name == maleSaved }.coerceAtLeast(0))
        femaleVoicePicker.setSelection(persianVoices.indexOfFirst { it.name == femaleSaved }.coerceAtLeast(0))
        voiceStatus.text = "تعداد صداهای فارسی نصب‌شده: ${persianVoices.size}. برای تشخیص واقعی، هر دو نمونه را پخش و پروفایل مناسب را انتخاب کن."
        saveVoiceAssignments()
    }

    private fun saveVoiceAssignments() {
        if (persianVoices.isEmpty()) return
        val maleIndex = maleVoicePicker.selectedItemPosition.coerceIn(persianVoices.indices)
        val femaleIndex = femaleVoicePicker.selectedItemPosition.coerceIn(persianVoices.indices)
        prefs.edit()
            .putString("male_voice_name", persianVoices[maleIndex].name)
            .putString("female_voice_name", persianVoices[femaleIndex].name)
            .apply()
    }

    private fun testVoice(spinner: Spinner, text: String) {
        if (!ttsReady || persianVoices.isEmpty()) {
            showToast("ابتدا یک صدای فارسی روی گوشی نصب کن.")
            return
        }
        saveVoiceAssignments()
        val index = spinner.selectedItemPosition.coerceIn(persianVoices.indices)
        val engine = tts ?: return
        engine.stop()
        engine.setVoice(persianVoices[index])
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "separ-voice-test")
    }

    private fun launchAlert(level: Int, test: Boolean) {
        saveVoiceAssignments()
        val city = prefs.getString("selected_city", "کل ایران") ?: "کل ایران"
        val voice = prefs.getString("preferred_voice", "female") ?: "female"
        val intent = Intent(this, AlertService::class.java)
            .putExtra(AlertService.EXTRA_CITY, city)
            .putExtra(AlertService.EXTRA_LEVEL, level)
            .putExtra(AlertService.EXTRA_TEST, test)
            .putExtra(AlertService.EXTRA_VOICE, voice)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (_: Exception) {
            showToast("شروع سرویس هشدار ممکن نشد. مجوز اعلان را بررسی کن.")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
    }

    private fun requestFullScreenPermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (manager.canUseFullScreenIntent()) {
                showToast("مجوز نمایش تمام‌صفحه فعال است.")
                return
            }
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                showToast("این نسخهٔ اندروید تنظیم جداگانهٔ نمایش تمام‌صفحه ندارد.")
            }
        } else {
            showToast("در این نسخهٔ اندروید، مجوز جداگانهٔ نمایش تمام‌صفحه لازم نیست.")
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { showToast("مرورگر برای بازکردن منبع پیدا نشد.") }
    }

    private fun card(parent: LinearLayout, title: String): LinearLayout {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
        parent.addView(shell, params)
        shell.addView(label(title, 18, navy, true))
        return shell
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun message(text: String): TextView =
        label(text, 13, muted).apply { setPadding(0, dp(5), 0, dp(8)) }

    private fun actionButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 13f
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showToast(text: String) =
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }
        tts = null
        super.onDestroy()
    }
}
