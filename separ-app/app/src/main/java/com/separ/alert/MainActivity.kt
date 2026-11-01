package com.separ.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 32, 28, 28)
            setBackgroundColor(Color.rgb(247, 248, 250))
        }

        root.addView(TextView(this).apply {
            text = "سپر | SEPAR"
            textSize = 30f
            setTextColor(Color.rgb(20, 42, 70))
            gravity = Gravity.CENTER
        }, matchWidth())

        root.addView(TextView(this).apply {
            text = "سامانهٔ هشدار آزمایشی"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }, matchWidth())

        root.addView(TextView(this).apply {
            text = "محدودهٔ موردنظر"
            textSize = 16f
            setTextColor(Color.BLACK)
        }, matchWidth())

        val cities = arrayOf(
            "تهران", "کرج", "اصفهان", "شیراز", "تبریز",
            "مشهد", "اهواز", "قم", "رشت", "اراک", "سایر مناطق"
        )
        val cityPicker = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                cities
            )
        }
        root.addView(cityPicker, matchWidth())

        root.addView(TextView(this).apply {
            text = "صدای گوینده"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 22, 0, 4)
        }, matchWidth())

        val voices = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val male = RadioButton(this).apply {
            id = 1001
            text = "مردانه"
            isChecked = true
        }
        val female = RadioButton(this).apply {
            id = 1002
            text = "زنانه"
        }
        voices.addView(male)
        voices.addView(female)
        root.addView(voices, matchWidth())

        root.addView(TextView(this).apply {
            text = "توجه: این نسخه فقط برای آزمایش آژیر و اعلان است و هنوز به منبع رسمی هشدارهای ایران متصل نیست. برای هشدار واقعی به آن اتکا نکنید."
            textSize = 14f
            setTextColor(Color.rgb(155, 65, 20))
            setPadding(0, 26, 0, 26)
        }, matchWidth())

        root.addView(Button(this).apply {
            text = "آزمایش آژیر و پیام صوتی"
            setOnClickListener {
                val voice = if (voices.checkedRadioButtonId == female.id) "زنانه" else "مردانه"
                val intent = Intent(this@MainActivity, AlertService::class.java)
                    .putExtra(AlertService.EXTRA_CITY, cityPicker.selectedItem.toString())
                    .putExtra(AlertService.EXTRA_VOICE, voice)
                startForegroundService(intent)
            }
        }, matchWidth())

        root.addView(Button(this).apply {
            text = "توقف آژیر"
            setOnClickListener {
                stopService(Intent(this@MainActivity, AlertService::class.java))
            }
        }, matchWidth())

        setContentView(root)
    }

    private fun matchWidth(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
}
