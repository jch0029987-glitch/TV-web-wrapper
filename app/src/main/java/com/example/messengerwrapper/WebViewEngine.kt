package com.example.messengerwrapper

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.concurrent.thread

class WebViewEngine(
    private val context: Context,
    private val nativeBridge: NativeBridge,
    private val onAdBlocked: ((String) -> Unit)? = null,
    private val onDownloadRequested: ((String, String, String) -> Unit)? = null
) : IBrowserEngine {

    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"
    private val blockedDomains = mutableSetOf<String>()
    
    var blockedAdsCount: Int = 0
        private set

    private val defaultDesktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val defaultMobileUA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    init {
        loadRemoteBlocklist()
    }

    private fun loadRemoteBlocklist() {
        thread {
            val jsonStr = NetworkClient.fetchText("https://raw.githubusercontent.com/$repoOwner/$repoName/main/blocklist.json")
            if (jsonStr != null) {
                try {
                    val jsonObj = JSONObject(jsonStr)
                    val domains: JSONArray = jsonObj.getJSONArray("blockedDomains")
                    for (i in 0 until domains.length()) {
                        blockedDomains.add(domains.getString(i))
                    }
                    Log.d("WebViewEngine", "Loaded ${blockedDomains.size} blocked domains.")
                } catch (e: Exception) {
                    Log.e("WebViewEngine", "Failed to parse blocklist.json", e)
                }
            }
        }
    }

    private val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // CRITICAL: Ensure the WebView retains native window & touch focus 
        // so JS web apps (like Facebook login) don't reject focus bindings.
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus(View.FOCUS_DOWN)

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

        setDownloadListener { url, _, _, mimetype, _ ->
            onDownloadRequested?.invoke(url, mimetype, "")
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("WebViewConsole", "[${it.messageLevel().name}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }
        }
        
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val urlStr = request?.url.toString()
                
                if (blockedDomains.any { urlStr.contains(it) }) {
                    blockedAdsCount++
                    onAdBlocked?.invoke(urlStr)
                    return WebResourceResponse(
                        "text/plain", "utf-8", 200, "Blocked by Policy",
                        emptyMap(), ByteArrayInputStream(ByteArray(0))
                    )
                }

                try {
                    val responseStr = nativeBridge.nativeBridgeWorker("GET $urlStr")
                    if (responseStr.startsWith("HTTP/1.1 403") || responseStr.contains("BLOCKED")) {
                        blockedAdsCount++
                        return WebResourceResponse(
                            "text/plain", "utf-8", 200, "Blocked",
                            emptyMap(), ByteArrayInputStream(ByteArray(0))
                        )
                    }
                } catch (e: Exception) {
                    // Ignore bridge failure
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                // Ensure focus is requested back upon page completion
                view?.requestFocus()

                if (url != null) {
                    val db = BrowserDatabase.getDatabase(context)
                    thread {
                        kotlin.runCatching {
                            db.browserDao().insertItem(
                                HistoryItem(url = url, title = view?.title ?: url)
                            )
                        }
                    }
                    loadAndExecuteRemoteExtensions(view, url)
                }
                CookieManagerHelper.syncCookies()
            }
        }
    }

    override val view: View get() = webView
    override fun loadUrl(url: String) { webView.loadUrl(url) }
    override fun goBack(): Boolean = if (webView.canGoBack()) { webView.goBack(); true } else false
    override fun goForward(): Boolean = if (webView.canGoForward()) { webView.goForward(); true } else false
    override fun reload() { webView.reload() }
    override fun setDesktopMode(desktop: Boolean) {
        webView.settings.userAgentString = if (desktop) defaultDesktopUA else defaultMobileUA
        webView.reload()
    }
    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) { webView.evaluateJavascript(script, callback) }
    override fun clearCache() { webView.clearCache(true) }

    fun addJavascriptInterface(objectToBind: Any, name: String) {
        webView.addJavascriptInterface(objectToBind, name)
    }

    private fun loadAndExecuteRemoteExtensions(view: WebView?, currentUrl: String) {
        thread {
            val manifestStr = NetworkClient.fetchText("https://raw.githubusercontent.com/$repoOwner/$repoName/main/manifest.json") ?: return@thread
            try {
                val manifestJson = JSONObject(manifestStr)
                val scriptsArray = manifestJson.getJSONArray("scripts")
                val combinedScripts = StringBuilder()

                for (i in 0 until scriptsArray.length()) {
                    val scriptObj = scriptsArray.getJSONObject(i)
                    val fileName = scriptObj.getString("file")
                    val urlPattern = scriptObj.getString("match")

                    val patterns = urlPattern.split("|")
                    val isMatch = patterns.any { it == "*" || currentUrl.contains(it.trim()) }

                    if (isMatch) {
                        val cacheFile = File(context.filesDir, "cache_$fileName")
                        var scriptContent = NetworkClient.fetchText("https://raw.githubusercontent.com/$repoOwner/$repoName/main/$fileName")

                        if (scriptContent != null) {
                            cacheFile.writeText(scriptContent)
                        } else if (cacheFile.exists()) {
                            scriptContent = cacheFile.readText()
                        }

                        if (!scriptContent.isNullOrEmpty()) {
                            combinedScripts.append("\n// --- Module: $fileName ---\n").append(scriptContent)
                        }
                    }
                }

                if (combinedScripts.isNotEmpty()) {
                    view?.post { view.evaluateJavascript(combinedScripts.toString(), null) }
                }
            } catch (e: Exception) {
                Log.e("WebViewEngine", "Manifest execution error", e)
            }
        }
    }
}
