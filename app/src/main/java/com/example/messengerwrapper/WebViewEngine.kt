package com.example.messengerwrapper

import android.content.Context
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class WebViewEngine(
    private val context: Context,
    private val nativeBridge: NativeBridge
) : IBrowserEngine {

    private val webView = InteractiveWebView(context).apply {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val urlString = request.url.toString()
                try {
                    val response = nativeBridge.nativeBridgeWorker("GET $urlString")
                    if (response.startsWith("HTTP/1.1 200 OK")) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            200,
                            "OK",
                            mapOf("Access-Control-Allow-Origin" to "*"),
                            ByteArrayInputStream(byteArrayOf())
                        )
                    }
                } catch (e: Exception) {
                    // Fallback on native exception
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
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
        // Native filter is handled via the C blocklist in nativeBridgeWorker
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavascript(script, callback)
    }

    override fun clearCache() {
        webView.clearCache(true)
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
    }
}
