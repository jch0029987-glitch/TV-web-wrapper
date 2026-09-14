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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.concurrent.thread

class WebViewEngine(
    private val context: Context,
    private val nativeBridge: NativeBridge
) : IBrowserEngine {

    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"
    private val blockedDomains = mutableSetOf<String>()

    init {
        // Fetch blocklist asynchronously on startup
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
                    Log.d("WebViewEngine", "Loaded ${blockedDomains.size} blocked domains from GitHub.")
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
                val urlStr = request?.url.toString()
                
                // 1. Check against remote blocklist
                if (blockedDomains.any { urlStr.contains(it) }) {
                    Log.d("WebViewEngine", "Blocked request: $urlStr")
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        200,
                        "Blocked by Policy",
                        emptyMap(),
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                // 2. Fallback to native bridge handler
                try {
                    val responseStr = nativeBridge.nativeBridgeWorker("GET $urlStr")
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
                    // Ignore bridge failure
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

    override val view: View get() = webView
    override fun loadUrl(url: String) { webView.loadUrl(url) }
    override fun goBack(): Boolean = if (webView.canGoBack()) { webView.goBack(); true } else false
    override fun goForward(): Boolean = if (webView.canGoForward()) { webView.goForward(); true } else false
    override fun reload() { webView.reload() }
    override fun setDesktopMode(desktop: Boolean) { /* handled via config/UA */ }
    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) { webView.evaluateJavascript(script, callback) }
    override fun clearCache() { webView.clearCache(true) }

    private fun injectCursorScript() {
        // (Existing cursor injection code remains here)
    }

    private fun loadAndExecuteRemoteExtensions(view: WebView?, currentUrl: String) {
        thread {
            val manifestJsonStr = NetworkClient.fetchText("https://raw.githubusercontent.com/$repoOwner/$repoName/main/manifest.json") ?: return@thread
            try {
                val manifestJson = JSONObject(manifestJsonStr)
                val scriptsArray = manifestJson.getJSONArray("scripts")
                val combinedScripts = StringBuilder()

                for (i in 0 until scriptsArray.length()) {
                    val scriptObj = scriptsArray.getJSONObject(i)
                    val fileName = scriptObj.getString("file")
                    val urlPattern = scriptObj.getString("match")

                    val patterns = urlPattern.split("|")
                    if (patterns.any { it == "*" || currentUrl.contains(it.trim()) }) {
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
                Log.e("WebViewEngine", "Manifest parsing error", e)
            }
        }
    }
}
