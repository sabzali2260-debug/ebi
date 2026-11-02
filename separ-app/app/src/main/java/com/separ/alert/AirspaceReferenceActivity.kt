package com.separ.alert

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Opens a third-party map as a clearly attributed secondary reference.
 * It is not treated as an authoritative closure signal.
 */
class AirspaceReferenceActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text = "نمای مرجع ثانویهٔ محدودیت‌ها و NOTAM"
            textSize = 18f
            setTextColor(Color.rgb(20, 42, 66))
            setPadding(16, 12, 16, 8)
        })
        root.addView(TextView(this).apply {
            text = "این صفحهٔ تجمیعی برای بررسی کمکی است؛ مختصات و تفسیر نقشه می‌تواند تقریبی باشد. مرجع نهایی، NOTAM و AIP منتشرشده توسط Iran AIM است. وجود NOTAM به‌تنهایی به معنی بسته‌شدن کل آسمان ایران نیست."
            textSize = 12f
            setTextColor(Color.rgb(130, 65, 30))
            setPadding(16, 4, 16, 10)
        })
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        webView.loadUrl("https://oibk.ir/iran/airspace")
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
