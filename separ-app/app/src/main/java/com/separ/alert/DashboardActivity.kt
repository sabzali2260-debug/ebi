package com.separ.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DashboardActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("separ_prefs", MODE_PRIVATE) }
    private val regions = listOf("کل ایران","تهران","البرز","آذربایجان شرقی","آذربایجان غربی","اردبیل","اصفهان","ایلام","بوشهر","چهارمحال و بختیاری","خراسان جنوبی","خراسان رضوی","خراسان شمالی","خوزستان","زنجان","سمنان","سیستان و بلوچستان","فارس","قزوین","قم","کردستان","کرمان","کرمانشاه","کهگیلویه و بویراحمد","گلستان","گیلان","لرستان","مازندران","مرکزی","هرمزگان","همدان","یزد")
    private val bg = Color.rgb(4, 13, 12)
    private val panel = Color.rgb(9, 25, 21)
    private val green = Color.rgb(80, 255, 151)
    private val dim = Color.rgb(110, 166, 135)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(14), dp(14), dp(26))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("سپر  /  SEPAR", 27, green, true))
        root.addView(text("سامانه پایش و هشدار منطقه‌ای", 13, dim))
        root.addView(text("COMMAND • MONITOR • ALERT     /     BUILD 0.4", 10, dim))
        root.addView(line())

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(12, 38, 29))
        }
        status.addView(text("●", 17, green, true))
        status.addView(text("  سامانه آماده پایش محلی", 13, green, true), LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(text("OFFICIAL FEED: NOT CONNECTED", 9, Color.rgb(255,190,75)))
        root.addView(status, margin(-1, -2, 0, 12))

        val radarCard = card(root, "رادار منطقه‌ای  /  LOCAL RADAR")
        val radar = RadarView(this).apply { setBackgroundColor(Color.rgb(2, 15, 11)) }
        radarCard.addView(radar, LinearLayout.LayoutParams(-1, dp(270)))
        radarCard.addView(text("نمایشگر جاروب راداری گرافیکی است؛ کشف واقعی هدف یا ردیابی هواگرد انجام نمی‌دهد.", 11, dim))

        val areaCard = card(root, "انتخاب محدوده پایش")
        areaCard.addView(text("استان یا محدوده‌ای را که می‌خواهی وضعیت هشدار آن را دنبال کنی انتخاب کن.", 12, dim))
        val picker = Spinner(this).apply {
            adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_dropdown_item, regions)
            setSelection(regions.indexOf(prefs.getString("selected_city", "کل ایران")).coerceAtLeast(0))
        }
        areaCard.addView(picker, LinearLayout.LayoutParams(-1, dp(52)))
        picker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("selected_city", regions[position]).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val alertCard = card(root, "پنل هشدار اضطراری")
        alertCard.addView(text("دکمه‌ها فقط برای آزمایش محلی صدا و صفحه هشدار هستند؛ هشدار واقعی از منبع رسمی دریافت نمی‌شود.", 11, dim))
        addAction(alertCard, "آزمایش سطح ۱  /  اطلاع‌رسانی", green) { launch(1) }
        addAction(alertCard, "آزمایش سطح ۲  /  هشدار", Color.rgb(255,190,65)) { launch(2) }
        addAction(alertCard, "آزمایش سطح ۳  /  وضعیت اضطراری", Color.rgb(255,75,65)) { launch(3) }
        addAction(alertCard, "توقف فوری آژیر و لرزش", Color.WHITE) {
            stopService(Intent(this, AlertService::class.java))
        }

        val settings = card(root, "تنظیمات عملیاتی")
        addToggle(settings, "چشمک قرمز صفحه در هشدار", "screen_flash", true)
        addToggle(settings, "لرزش هشدار", "vibration", true)
        addToggle(settings, "تکرار پیام صوتی سطح اضطراری", "repeat_speech", true)
        addToggle(settings, "فعال بودن هشدارهای سطح ۱", "allow_level_1", true)
        settings.addView(text("شدت آژیر  •  صدای خروجی گوشی نیز باید بالا باشد", 12, green, true))
        val volumeLabel = text("", 11, dim)
        val volume = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("siren_volume", 100).coerceIn(30,100)
        }
        fun updateVolume(v: Int) {
            prefs.edit().putInt("siren_volume", v.coerceIn(30,100)).apply()
            volumeLabel.text = "شدت انتخاب‌شده: ${v.coerceIn(30,100)}٪"
        }
        updateVolume(volume.progress)
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateVolume(progress)
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        settings.addView(volumeLabel)
        settings.addView(volume, LinearLayout.LayoutParams(-1, dp(48)))
        settings.addView(text("نوع آژیر: هشدار ممتد موجی با تغییر زیر و بمی و الگوی تکرارشونده؛ صدای داخل برنامه تولید می‌شود و به فایل صوتی گوشی وابسته نیست.", 11, dim))

        val sources = card(root, "منابع و اعتبار هشدار")
        addAction(sources, "Iran AIM  •  مرجع رسمی اطلاعات هوانوردی", green) { open("https://ais.airport.ir") }
        addAction(sources, "EASA  •  بولتن‌های ایمنی هوانوردی", green) { open("https://www.easa.europa.eu/en/domains/air-operations/czibs") }
        addAction(sources, "Flightradar24  •  مشاهده دستی پروازها", green) { open("https://www.flightradar24.com/") }
        sources.addView(text("در این نسخه هنوز خوراک ماشینی معتبر و تأییدشده برای هشدار خودکار ایران متصل نیست. مشاهده پروازها یا انیمیشن رادار به معنی کشف تهدید نیست. تا اتصال منبع رسمی، برنامه را ابزار کمکی بدان نه سامانه تضمین‌کننده ایمنی.", 11, Color.rgb(255,190,75)))

        root.addView(text("SEPAR • CONTROL PANEL • 0.4.0", 10, dim))
    }

    private fun launch(level: Int) {
        val city = prefs.getString("selected_city", "کل ایران") ?: "کل ایران"
        val voice = prefs.getString("preferred_voice", "female") ?: "female"
        val i = Intent(this, AlertService::class.java)
            .putExtra(AlertService.EXTRA_LEVEL, level)
            .putExtra(AlertService.EXTRA_CITY, city)
            .putExtra(AlertService.EXTRA_VOICE, voice)
            .putExtra(AlertService.EXTRA_TEST, true)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }

    private fun addToggle(parent: LinearLayout, title: String, key: String, default: Boolean) {
        parent.addView(Switch(this).apply {
            text = title
            setTextColor(green)
            textSize = 13f
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
    }

    private fun card(root: LinearLayout, heading: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(13), dp(13), dp(13))
            setBackgroundColor(panel)
        }
        box.addView(text(heading, 14, green, true))
        root.addView(box, margin(-1, -2, 0, 12))
        return box
    }

    private fun addAction(parent: LinearLayout, title: String, color: Int, action: () -> Unit) {
        val b = Button(this).apply {
            text = title
            textSize = 12f
            setTextColor(color)
            setBackgroundColor(Color.rgb(19, 42, 32))
            isAllCaps = false
            setOnClickListener { action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(5) })
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
        setBackgroundColor(Color.rgb(30, 83, 57))
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(12) }
    }

    private fun margin(w: Int, h: Int, top: Int, bottom: Int) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top); bottomMargin = dp(bottom) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun open(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "مرورگر در دسترس نیست", Toast.LENGTH_SHORT).show() }
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
            p.color = Color.rgb(100,255,150)
            p.strokeWidth = dp(2).toFloat()
            val rad = Math.toRadians(angle)
            canvas.drawLine(cx, cy, cx + cos(rad).toFloat()*radius, cy + sin(rad).toFloat()*radius, p)
            p.style = Paint.Style.FILL
            p.color = green
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
