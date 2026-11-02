package com.separ.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DashboardActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("separ_prefs", MODE_PRIVATE) }
    private val regions = listOf("کل ایران","تهران","البرز","آذربایجان شرقی","آذربایجان غربی","اردبیل","اصفهان","ایلام","بوشهر","چهارمحال و بختیاری","خراسان جنوبی","خراسان رضوی","خراسان شمالی","خوزستان","زنجان","سمنان","سیستان و بلوچستان","فارس","قزوین","قم","کردستان","کرمان","کرمانشاه","کهگیلویه و بویراحمد","گلستان","گیلان","لرستان","مازندران","مرکزی","هرمزگان","همدان","یزد")
    private val bg = Color.rgb(3, 10, 12)
    private val panel = Color.rgb(8, 24, 27)
    private val green = Color.rgb(89, 255, 166)
    private val cyan = Color.rgb(76, 220, 230)
    private val amber = Color.rgb(255, 190, 75)
    private val red = Color.rgb(255, 82, 82)
    private val dim = Color.rgb(112, 158, 153)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prefs.getBoolean("dashboard_043_migrated", false)) {
            prefs.edit().putInt("siren_volume", 100).putBoolean("dashboard_043_migrated", true).apply()
        }
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(14), dp(12), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("سپر  /  SEPAR", 28, green, true))
        root.addView(text("FLIGHT DECK  •  پنل فرماندهی و پایش", 11, cyan, true))
        root.addView(text("CONTROL SYSTEM  /  BUILD 0.4.3", 9, dim))
        root.addView(line())

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(10, 42, 32))
        }
        status.addView(text("●  SYSTEM READY  /  سامانه آماده", 14, green, true))
        status.addView(text("منبع هشدار رسمی: متصل نیست  •  رادار: نمایشی  •  حالت: پایش محلی", 10, amber))
        root.addView(status, margin(-1, -2, 0, 12))

        val instruments = GridLayout(this).apply {
            columnCount = 2
            rowCount = 2
            setBackgroundColor(bg)
        }
        instrument(instruments, "RADAR SWEEP", "جاروب گرافیکی فعال", green)
        instrument(instruments, "ALERT CHANNEL", "آمادهٔ آزمایش محلی", cyan)
        instrument(instruments, "AUDIO SYSTEM", "آژیر و گفتار داخلی", amber)
        instrument(instruments, "DATA LINK", "بدون خوراک رسمی زنده", red)
        root.addView(instruments, margin(-1, -2, 0, 12))

        val radarCard = card(root, "01  /  RADAR DISPLAY  •  رادار منطقه‌ای")
        val radar = RadarView(this).apply { setBackgroundColor(Color.rgb(1, 13, 14)) }
        radarCard.addView(radar, LinearLayout.LayoutParams(-1, dp(230)))
        radarCard.addView(text("این نمایشگر فقط جاروب گرافیکی است؛ کشف هدف، رهگیری هواگرد یا تشخیص حمله انجام نمی‌دهد.", 10, amber))

        val quick = card(root, "02  /  FLIGHT DECK KEYS  •  کلیدهای سریع")
        val keys = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
        }
        key(keys, "◉  نقشه ایران", cyan) { openActivity(IranMapActivity::class.java) }
        key(keys, "⌁  منابع هوانوردی", green) { openActivity(AirspaceReferenceActivity::class.java) }
        key(keys, "♫  تست آژیر", amber) { launch(2) }
        key(keys, "◖  تست پیام صوتی", cyan) { launch(1) }
        key(keys, "▲  هشدار سطح ۲", amber) { launch(2) }
        key(keys, "▲▲  اضطراری سطح ۳", red) { launch(3) }
        key(keys, "■  توقف فوری", Color.WHITE) { stopAlert() }
        key(keys, "⚙  تنظیمات دستگاه", green) { openNotificationSettings() }
        quick.addView(keys, LinearLayout.LayoutParams(-1, -2))

        val area = card(root, "03  /  AREA SELECTOR  •  انتخاب منطقه")
        area.addView(text("محدودهٔ انتخابی برای آزمایش و نمایش هشدار", 11, dim))
        val picker = Spinner(this).apply {
            adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_dropdown_item, regions)
            setSelection(regions.indexOf(prefs.getString("selected_city", "کل ایران")).coerceAtLeast(0))
        }
        area.addView(picker, LinearLayout.LayoutParams(-1, dp(50)))
        picker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("selected_city", regions[position]).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val alert = card(root, "04  /  ALERT PANEL  •  پنل سه سطح هشدار")
        alert.addView(text("این کلیدها برای آزمایش محلی هستند و هشدار واقعی دریافت نمی‌کنند.", 10, amber))
        addAction(alert, "LEVEL 1  /  اطلاع‌رسانی و پیام صوتی", green) { launch(1) }
        addAction(alert, "LEVEL 2  /  هشدار + آژیر + لرزش", amber) { launch(2) }
        addAction(alert, "LEVEL 3  /  اضطراری + تکرار پیام", red) { launch(3) }
        addAction(alert, "EMERGENCY CUT-OFF  /  توقف همه هشدارها", Color.WHITE) { stopAlert() }

        val controls = card(root, "05  /  SYSTEM CONFIG  •  تنظیمات سامانه")
        addToggle(controls, "چراغ چشمک‌زن قرمز هنگام هشدار", "screen_flash", true)
        addToggle(controls, "لرزش الگوی اضطراری", "vibration", true)
        addToggle(controls, "تکرار پیام گفتاری در سطح ۳", "repeat_speech", true)

        controls.addView(text("حداقل سطح اعلان خودکار (در صورت اتصال منبع معتبر)", 11, cyan, true))
        val levelPicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("سطح ۱، ۲ و ۳", "فقط سطح ۲ و ۳", "فقط سطح ۳"))
            setSelection((prefs.getInt("minimum_level", 1) - 1).coerceIn(0, 2))
        }
        levelPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("minimum_level", position + 1).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        controls.addView(levelPicker, LinearLayout.LayoutParams(-1, dp(48)))

        controls.addView(text("پروفایل گویندهٔ فارسی آفلاین", 11, cyan, true))
        val voicePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("صدای بم / پروفایل مردانه", "صدای زیر / پروفایل زنانه"))
            setSelection(if (prefs.getString("preferred_voice", "female") == "male") 0 else 1)
        }
        voicePicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("preferred_voice", if (position == 0) "male" else "female").apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        controls.addView(voicePicker, LinearLayout.LayoutParams(-1, dp(48)))
        controls.addView(text("دو پروفایل گفتار مصنوعی فارسی داخل برنامه قرار دارند و به صدای نصب‌شده روی گوشی وابسته نیستند.", 10, dim))

        controls.addView(text("SIREN MASTER VOLUME  •  شدت آژیر", 11, cyan, true))
        val volumeLabel = text("", 11, green)
        val volume = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("siren_volume", 100).coerceIn(10, 100)
        }
        fun updateVolume(v: Int) {
            val value = v.coerceIn(10, 100)
            prefs.edit().putInt("siren_volume", value).apply()
            volumeLabel.text = "خروجی آژیر: $value٪"
        }
        updateVolume(volume.progress)
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateVolume(progress)
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        controls.addView(volumeLabel)
        controls.addView(volume, LinearLayout.LayoutParams(-1, dp(48)))

        val support = card(root, "06  /  SYSTEM ACCESS  •  دسترسی‌های دستگاه")
        addAction(support, "بازکردن تنظیمات اعلان‌های سپر", cyan) { openNotificationSettings() }
        addAction(support, "تنظیمات نمایش تمام‌صفحه روی قفل", green) { openFullScreenSettings() }
        addAction(support, "بازکردن صفحه تنظیمات پیشرفته", amber) { openActivity(MainActivity::class.java) }

        val sources = card(root, "07  /  DATA SOURCES  •  منابع اطلاعات")
        addAction(sources, "Iran AIM  •  مرجع رسمی اطلاعات هوانوردی", green) { open("https://ais.airport.ir") }
        addAction(sources, "EASA  •  بولتن‌های ایمنی هوانوردی", green) { open("https://www.easa.europa.eu/en/domains/air-operations/czibs") }
        addAction(sources, "Flightradar24  •  مشاهده دستی پروازها", green) { open("https://www.flightradar24.com/") }
        sources.addView(text("در این نسخه خوراک ماشینی معتبر و تأییدشده برای هشدار خودکار ایران متصل نیست. انیمیشن رادار یا مشاهده پروازها، کشف تهدید محسوب نمی‌شود. برای تصمیم ایمنی فقط اطلاعیه‌های رسمی را بررسی کن.", 10, amber))

        root.addView(text("SEPAR FLIGHT DECK  •  VERSION 0.4.3", 9, dim))
    }

    private fun instrument(parent: GridLayout, heading: String, value: String, color: Int) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            setBackgroundColor(panel)
        }
        box.addView(text(heading, 9, dim, true))
        box.addView(text(value, 11, color, true))
        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(62)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
        parent.addView(box, params)
    }

    private fun key(parent: GridLayout, title: String, color: Int, action: () -> Unit) {
        val b = Button(this).apply {
            text = title
            textSize = 11f
            setTextColor(color)
            setBackgroundColor(Color.rgb(14, 43, 43))
            isAllCaps = false
            minHeight = dp(54)
            setOnClickListener { action() }
        }
        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(58)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
        parent.addView(b, params)
    }

    private fun launch(level: Int) {
        val city = prefs.getString("selected_city", "کل ایران") ?: "کل ایران"
        val voice = prefs.getString("preferred_voice", "female") ?: "female"
        val i = Intent(this, AlertService::class.java)
            .putExtra(AlertService.EXTRA_LEVEL, level)
            .putExtra(AlertService.EXTRA_CITY, city)
            .putExtra(AlertService.EXTRA_VOICE, voice)
            .putExtra(AlertService.EXTRA_TEST, true)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (_: Exception) {
            Toast.makeText(this, "شروع هشدار آزمایشی ممکن نشد؛ مجوز اعلان را بررسی کن.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAlert() {
        stopService(Intent(this, AlertService::class.java))
        Toast.makeText(this, "فرمان توقف ارسال شد", Toast.LENGTH_SHORT).show()
    }

    private fun addToggle(parent: LinearLayout, title: String, key: String, default: Boolean) {
        parent.addView(Switch(this).apply {
            text = title
            setTextColor(green)
            textSize = 12f
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
        }, LinearLayout.LayoutParams(-1, dp(46)))
    }

    private fun card(root: LinearLayout, heading: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(panel)
        }
        box.addView(text(heading, 13, green, true))
        root.addView(box, margin(-1, -2, 0, 10))
        return box
    }

    private fun addAction(parent: LinearLayout, title: String, color: Int, action: () -> Unit) {
        val b = Button(this).apply {
            text = title
            textSize = 11f
            setTextColor(color)
            setBackgroundColor(Color.rgb(16, 42, 42))
            isAllCaps = false
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(4) })
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun line(): View = View(this).apply {
        setBackgroundColor(Color.rgb(30, 92, 72))
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(9); bottomMargin = dp(12) }
    }

    private fun margin(w: Int, h: Int, top: Int, bottom: Int) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top); bottomMargin = dp(bottom) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun openActivity(clazz: Class<out Activity>) {
        try { startActivity(Intent(this, clazz)) }
        catch (_: Exception) { Toast.makeText(this, "این بخش باز نشد", Toast.LENGTH_SHORT).show() }
    }

    private fun open(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "مرورگر در دسترس نیست", Toast.LENGTH_SHORT).show() }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (_: Exception) { Toast.makeText(this, "تنظیمات اعلان باز نشد", Toast.LENGTH_SHORT).show() }
    }

    private fun openFullScreenSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } else openNotificationSettings()
        } catch (_: Exception) { openNotificationSettings() }
    }

    inner class RadarView(context: android.content.Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var angle = 0.0
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width, height) * 0.43f
            p.style = Paint.Style.STROKE
            p.strokeWidth = dp(1).toFloat()
            p.color = Color.rgb(35, 105, 69)
            for (k in 1..4) canvas.drawCircle(cx, cy, radius * k / 4f, p)
            canvas.drawLine(cx-radius, cy, cx+radius, cy, p)
            canvas.drawLine(cx, cy-radius, cx, cy+radius, p)
            canvas.drawLine(cx-radius*0.7f, cy-radius*0.7f, cx+radius*0.7f, cy+radius*0.7f, p)
            canvas.drawLine(cx-radius*0.7f, cy+radius*0.7f, cx+radius*0.7f, cy-radius*0.7f, p)
            p.style = Paint.Style.FILL
            p.color = Color.argb(55, 70, 255, 135)
            val sweep = RectF(cx-radius, cy-radius, cx+radius, cy+radius)
            canvas.drawArc(sweep, angle.toFloat()-32f, 32f, true, p)
            p.color = green
            p.strokeWidth = dp(2).toFloat()
            val rad = Math.toRadians(angle)
            canvas.drawLine(cx, cy, cx + cos(rad).toFloat()*radius, cy + sin(rad).toFloat()*radius, p)
            p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, dp(4).toFloat(), p)
            p.textSize = dp(10).toFloat()
            p.color = dim
            canvas.drawText("N", cx-4, cy-radius-8, p)
            canvas.drawText("E", cx+radius+4, cy+4, p)
            canvas.drawText("S", cx-3, cy+radius+18, p)
            canvas.drawText("W", cx-radius-18, cy+4, p)
            angle = (angle + 2.0) % 360.0
            postInvalidateDelayed(40)
        }
    }
}
