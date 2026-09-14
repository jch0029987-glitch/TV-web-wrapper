package com.example.messengerwrapper

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class WebViewEngine(
    private val context: Context,
    private val nativeBridge: NativeBridge
) : IBrowserEngine {

    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"

    private val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
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
                loadAndExecuteRemoteExtension(view)
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
            null
        }
        webView.reload()
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavascript(script, callback)
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
                cursor.style.left = '50%';
                cursor.style.top = '50%';
                cursor.style.width = '18px';
                cursor.style.height = '18px';
                cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.85)';
                cursor.style.border = '2px solid white';
                cursor.style.borderRadius = '50%';
                cursor.style.pointerEvents = 'none';
                cursor.style.zIndex = '999999';
                cursor.style.display = 'none';
                cursor.style.transform = 'translate(-50%, -50%)';
                document.documentElement.appendChild(cursor);

                window.setCursorVisible = function(visible) {
                    cursor.style.display = visible ? 'block' : 'none';
                };

                window.clickCursor = function() {
                    cursor.style.backgroundColor = '#ff5252';
                    setTimeout(() => cursor.style.backgroundColor = 'rgba(0, 230, 118, 0.85)', 150);
                    
                    const x = window.innerWidth / 2;
                    const y = window.innerHeight / 2;
                    const target = document.elementFromPoint(x, y);
                    
                    if (target) {
                        target.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, clientX: x, clientY: y }));
                        target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, clientX: x, clientY: y }));
                        target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: x, clientY: y }));
                        target.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: x, clientY: y }));
                        if (typeof target.focus === 'function') target.focus();
                    }
                };
            }
        """.trimIndent()
        webView.evaluateJavascript(cursorScript, null)
    }

    private fun loadAndExecuteRemoteExtension(view: WebView?) {
        val cacheFile = File(context.filesDir, "cached_extension.js")

        thread {
            var scriptContent = ""
            try {
                val remoteUrl = URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/extension.js")
                scriptContent = remoteUrl.readText()
                cacheFile.writeText(scriptContent)
            } catch (e: Exception) {
                if (cacheFile.exists()) {
                    scriptContent = cacheFile.readText()
                    Log.d("WebViewEngine", "Using locally cached extension.js fallback")
                }
            }

            if (scriptContent.isNotEmpty()) {
                view?.post {
                    view.evaluateJavascript(scriptContent, null)
                }
            }
        }
    }
}
