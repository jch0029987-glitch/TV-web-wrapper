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
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
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
                if (url != null) {
                    loadAndExecuteRemoteExtensions(view, url)
                }
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

    private fun loadAndExecuteRemoteExtensions(view: WebView?, currentUrl: String) {
        thread {
            try {
                // 1. Fetch the manifest registry configuration from GitHub
                val manifestUrl = URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/manifest.json")
                val connection = manifestUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode != 200) {
                    Log.e("WebViewEngine", "Failed to fetch manifest.json, HTTP code: ${connection.responseCode}")
                    return@thread
                }

                val manifestJson = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val scriptsArray = manifestJson.getJSONArray("scripts")
                val combinedScripts = StringBuilder()

                // 2. Iterate through declared extension modules
                for (i in 0 until scriptsArray.length()) {
                    val scriptObj = scriptsArray.getJSONObject(i)
                    val fileName = scriptObj.getString("file")
                    val urlPattern = scriptObj.getString("match")

                    // 3. Match URL pattern (wildcard "*" matches everywhere, or check domain substring)
                    if (urlPattern == "*" || currentUrl.contains(urlPattern)) {
                        val cacheFile = File(context.filesDir, "cache_$fileName")
                        var scriptContent = ""

                        try {
                            val scriptUrl = URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/$fileName")
                            val scriptConn = scriptUrl.openConnection() as HttpURLConnection
                            scriptConn.connectTimeout = 4000
                            
                            if (scriptConn.responseCode == 200) {
                                scriptContent = scriptConn.inputStream.bufferedReader().use { it.readText() }
                                cacheFile.writeText(scriptContent)
                                Log.d("WebViewEngine", "Successfully fetched remote script: $fileName")
                            } else {
                                throw Exception("HTTP ${scriptConn.responseCode}")
                            }
                        } catch (e: Exception) {
                            Log.w("WebViewEngine", "Fetch failed for $fileName: ${e.message}. Checking cache...")
                            if (cacheFile.exists()) {
                                scriptContent = cacheFile.readText()
                                Log.d("WebViewEngine", "Loaded $fileName from local cache fallback.")
                            }
                        }

                        if (scriptContent.isNotEmpty()) {
                            combinedScripts.append("\n// --- Module: $fileName ---\n").append(scriptContent)
                        }
                    }
                }

                // 4. Inject and execute all matching script modules together
                if (combinedScripts.isNotEmpty()) {
                    view?.post {
                        view.evaluateJavascript(combinedScripts.toString()) { result ->
                            Log.d("WebViewEngine", "Extension modules executed successfully.")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("WebViewEngine", "Extension manifest execution error: ${e.message}")
            }
        }
    }
}
