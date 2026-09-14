package com.example.messengerwrapper

import android.content.Context
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

interface IBrowserEngine {
    val view: View
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun setDesktopMode(desktop: Boolean)
    fun evaluateJavascript(script: String)
    fun clearCache()
}

class WebViewEngine(
    private val context: Context,
    private val nativeBridge: NativeBridge
) : IBrowserEngine {

    private val webView: WebView = WebView(context).apply {
        layoutParams = View.LayoutParams(
            View.LayoutParams.MATCH_PARENT,
            View.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()
                try {
                    val responseStr = nativeBridge.nativeBridgeWorker("GET $url")
                    if (responseStr.startsWith("HTTP/1.1 403") || responseStr.contains("BLOCKED")) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            200,
                            "Blocked",
                            emptyMap(),
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                } catch (e: Exception) {
                    // Fallback to normal loading if bridge fails
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectCursorScript()
            }
        }
    }

    override val view: View
        get() = webView

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

    override fun setDesktopMode(desktop: Boolean) {
        webView.settings.userAgentString = if (desktop) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            null // Default mobile user agent
        }
        webView.reload()
    }

    override fun evaluateJavascript(script: String) {
        webView.evaluateJavascript(script, null)
    }

    override fun clearCache() {
        webView.clearCache(true)
    }

    private fun injectCursorScript() {
        val cursorScript = """
            if (!document.getElementById('tv-mouse-cursor')) {
                const cursor = document.createElement('div');
                cursor.id = 'tv-mouse-cursor';
                cursor.style.position = 'fixed';
                cursor.style.width = '16px';
                cursor.style.height = '16px';
                cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.8)';
                cursor.style.border = '2px solid white';
                cursor.style.borderRadius = '50%';
                cursor.style.pointerEvents = 'none';
                cursor.style.zIndex = '999999';
                cursor.style.display = 'none';
                cursor.style.transform = 'translate(-50%, -50%)';
                cursor.style.transition = 'left 0.05s linear, top 0.05s linear';
                document.documentElement.appendChild(cursor);

                window.cursorX = window.innerWidth / 2;
                window.cursorY = window.innerHeight / 2;
                cursor.style.left = window.cursorX + 'px';
                cursor.style.top = window.cursorY + 'px';

                window.setCursorVisible = function(visible) {
                    cursor.style.display = visible ? 'block' : 'none';
                };

                window.moveCursor = function(dx, dy) {
                    window.cursorX = Math.max(0, Math.min(window.innerWidth, window.cursorX + dx));
                    window.cursorY = Math.max(0, Math.min(window.innerHeight, window.cursorY + dy));
                    cursor.style.left = window.cursorX + 'px';
                    cursor.style.top = window.cursorY + 'px';
                };

                window.clickCursor = function() {
                    cursor.style.backgroundColor = '#ff5252';
                    setTimeout(() => cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.8)', 150);
                    
                    const target = document.elementFromPoint(window.cursorX, window.cursorY);
                    if (target) {
                        target.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, clientX: window.cursorX, clientY: window.cursorY }));
                        target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, clientX: window.cursorX, clientY: window.cursorY }));
                        target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: window.cursorX, clientY: window.cursorY }));
                        target.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: window.cursorX, clientY: window.cursorY }));
                        if (typeof target.focus === 'function') target.focus();
                    }
                };
            }
        """.trimIndent()
        webView.evaluateJavascript(cursorScript, null)
    }
}
