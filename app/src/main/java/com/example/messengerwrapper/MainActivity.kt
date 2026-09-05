package com.example.messengerwrapper

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        val webSettings: WebSettings = webView.settings

        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.facebook.com/messages")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (::webView.isInitialized) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    webView.pageBy(false, 100)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    webView.pageBy(false, -100)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    webView.pageBy(true, -100)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    webView.pageBy(true, 100)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
