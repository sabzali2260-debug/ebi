package com.separ.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlertActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(125, 20, 24))
        }
        layout.addView(TextView(this).apply {
            text = "سپر | هشدار آزمایشی"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "این اعلان صرفاً جهت آزمایش برنامه است."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 28)
        })
        layout.addView(Button(this).apply {
            text = "توقف آژیر"
            setOnClickListener {
                stopService(Intent(this@AlertActivity, AlertService::class.java))
                finish()
            }
        })
        setContentView(layout)
    }
}
