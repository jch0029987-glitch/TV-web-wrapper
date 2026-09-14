package com.example.messengerwrapper

import android.content.Context
import android.view.View
import android.webkit.WebViewClient

class WebViewEngine(context: Context) : IBrowserEngine {
    private val webView = InteractiveWebView(context).apply {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        webViewClient = WebViewClient()
    }

    override val view: View get() = webView

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun goBack(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    override fun goForward(): Boolean {
        if (webView.canGoForward()) {
            webView.goForward()
            return true
        }
        return false
    }

    override fun reload() {
        webView.reload()
    }

    override fun setDesktopMode(enabled: Boolean) {
        val desktopAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        val mobileAgent = "Mozilla/5.0 (Linux; Android 10; SM-T870) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        webView.settings.userAgentString = if (enabled) desktopAgent else mobileAgent
        webView.reload()
    }

    override fun setAdBlockEnabled(enabled: Boolean) {
        // WebView has no built-in tracking-protection API like GeckoView's
        // useTrackingProtection. No-op unless a request-filtering
        // WebViewClient (shouldInterceptRequest) is wired in separately.
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavascript(script, callback)
    }

    override fun clearCache() {
        webView.clearCache(false)
    }
}
