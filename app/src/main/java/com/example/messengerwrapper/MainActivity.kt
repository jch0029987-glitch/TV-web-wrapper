package com.example.messengerwrapper

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var browserEngine: WebViewEngine
    private lateinit var nativeBridge: NativeBridge
    private lateinit var debugServer: TvDebugServer
    private var isCursorActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Setup Global Native Exception & Crash Interceptor
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = throwable.stackTraceToString()
            val errorMsg = "[CRASH/JAVA] Thread: ${thread.name} -> ${throwable.message}\n$stackTrace"
            DebugConsoleStore.addLog(errorMsg)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        nativeBridge = NativeBridge()
        
        browserEngine = WebViewEngine(
            context = this,
            nativeBridge = nativeBridge,
            onAdBlocked = { url ->
                DebugConsoleStore.addLog("[AD-BLOCK] Blocked: $url")
            },
            onDownloadRequested = { url, _, _ ->
                Toast.makeText(this, "Download triggered: $url", Toast.LENGTH_SHORT).show()
            }
        )

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(browserEngine.view)
        }
        setContentView(container)

        // 2. Start Tailscale Debug Web Server on Port 8080
        debugServer = TvDebugServer(
            port = 8080,
            onNavigate = { url ->
                runOnUiThread { browserEngine.loadUrl(url) }
            },
            onReloadExtensions = {
                runOnUiThread {
                    val cacheDir = filesDir
                    cacheDir.listFiles()?.forEach { if (it.name.startsWith("cache_")) it.delete() }
                    browserEngine.reload()
                    Toast.makeText(this, "Remote: Extensions reloaded", Toast.LENGTH_SHORT).show()
                }
            },
            onClearCache = {
                runOnUiThread {
                    browserEngine.clearCache()
                    Toast.makeText(this, "Remote: Cache cleared", Toast.LENGTH_SHORT).show()
                }
            },
            getBlockedCount = {
                browserEngine.blockedAdsCount
            }
        )
        debugServer.start()

        // 3. Load initial homepage
        browserEngine.loadUrl("https://html.duckduckgo.com")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                showSettingsDialog()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                if (isCursorActive) {
                    browserEngine.evaluateJavascript("window.clickCursor();", null)
                    return true
                }
            }
            KeyEvent.KEYCODE_PROG_RED -> {
                // Toggle free-roaming mouse cursor mode via remote red button
                isCursorActive = !isCursorActive
                val status = if (isCursorActive) "ON" else "OFF"
                Toast.makeText(this, "TV Mouse Cursor: $status", Toast.LENGTH_SHORT).show()
                browserEngine.evaluateJavascript("window.setCursorVisible($isCursorActive);", null)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isCursorActive) {
                    browserEngine.evaluateJavascript("window.tvScrollBy(0, -35);", null)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isCursorActive) {
                    browserEngine.evaluateJavascript("window.tvScrollBy(0, 35);", null)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isCursorActive) {
                    browserEngine.evaluateJavascript("window.tvScrollBy(-35, 0);", null)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isCursorActive) {
                    browserEngine.evaluateJavascript("window.tvScrollBy(35, 0);", null)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "View History", 
            "Clear WebView Cache", 
            "Reload Extensions Cache", 
            "View Local Debug Logs"
        )
        AlertDialog.Builder(this)
            .setTitle("Browser Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showHistoryDialog()
                    1 -> {
                        browserEngine.clearCache()
                        Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val cacheDir = filesDir
                        cacheDir.listFiles()?.forEach { if (it.name.startsWith("cache_")) it.delete() }
                        browserEngine.reload()
                        Toast.makeText(this, "Extensions reloaded", Toast.LENGTH_SHORT).show()
                    }
                    3 -> showDebugLogsDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showHistoryDialog() {
        thread {
            try {
                val db = BrowserDatabase.getDatabase(this)
                val historyList = db.browserDao().getRecentHistory()
                runOnUiThread {
                    if (historyList.isEmpty()) {
                        Toast.makeText(this, "No history recorded yet.", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    val titles = historyList.map { "${it.title}\n${it.url}" }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Browsing History")
                        .setItems(titles) { _, index ->
                            browserEngine.loadUrl(historyList[index].url)
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            } catch (e: Exception) {
                DebugConsoleStore.addLog("[JAVA ERROR] Failed to load history: ${e.message}")
            }
        }
    }

    private fun showDebugLogsDialog() {
        val logs = DebugConsoleStore.getLogs()
        val displayLogs = if (logs.isEmpty()) arrayOf("No logs recorded yet.") else logs.reversed().toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("In-App Debug Logs")
            .setItems(displayLogs, null)
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        debugServer.stop()
    }
}
