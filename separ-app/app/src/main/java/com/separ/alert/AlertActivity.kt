package com.separ.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class AlertActivity : Activity() {
    private var flashAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val level = intent.getIntExtra(AlertService.EXTRA_LEVEL, 2).coerceIn(1, 3)
        val city = intent.getStringExtra(AlertService.EXTRA_CITY) ?: "کل ایران"
        val isTest = intent.getBooleanExtra(AlertService.EXTRA_TEST, true)
        val prefs = getSharedPreferences("separ_prefs", MODE_PRIVATE)
        val flashing = prefs.getBoolean("screen_flash", true) && level >= 2

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(125, 12, 20))
        }
        layout.addView(TextView(this).apply {
            text = if (level == 3) "هشدار اضطراری" else if (level == 2) "هشدار سپر" else "اطلاع‌رسانی سپر"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        layout.addView(TextView(this).apply {
            text = if (isTest) "این یک آزمایش محلی است" else "اعلان هشدار برای محدودهٔ $city"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 12)
        })
        layout.addView(TextView(this).apply {
            text = "محدوده: $city"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })
        layout.addView(Button(this).apply {
            text = "توقف آژیر و بستن هشدار"
            textSize = 16f
            setOnClickListener {
                stopService(Intent(this@AlertActivity, AlertService::class.java))
                finish()
            }
        })
        setContentView(layout)

        if (flashing) {
            flashAnimator = ValueAnimator.ofArgb(
                Color.rgb(155, 0, 12),
                Color.rgb(35, 0, 4)
            ).apply {
                duration = if (level == 3) 420L else 720L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener { layout.setBackgroundColor(it.animatedValue as Int) }
                start()
            }
        }
    }

    override fun onDestroy() {
        flashAnimator?.cancel()
        flashAnimator = null
        super.onDestroy()
    }
}
