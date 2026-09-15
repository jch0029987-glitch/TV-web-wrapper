package com.example.messengerwrapper

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var browserEngine: WebViewEngine
    private lateinit var nativeBridge: NativeBridge
    private lateinit var debugServer: TvDebugServer
    
    private lateinit var etUrlBar: EditText
    private lateinit var tvModeHud: TextView
    private var isCursorActive = false
    private var isDesktopMode = false

    // Long-press tracking variables for D-pad Center toggle
    private var dpadCenterDownTime: Long = 0
    private var hasTriggeredLongPress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 1. Global crash & exception interceptor
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = throwable.stackTraceToString()
            val errorMsg = "[CRASH/JAVA] Thread: ${thread.name} -> ${throwable.message}\n$stackTrace"
            DebugConsoleStore.addLog(errorMsg)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        nativeBridge = NativeBridge()
        
        // 2. Initialize WebView Engine
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

        // Expose keyboard & focus bridges safely via WebViewEngine wrapper
        browserEngine.addJavascriptInterface(KeyboardBridgeInterface(), "nativeBridge")

        val webViewContainer = findViewById<FrameLayout>(R.id.webViewContainer)
        webViewContainer.addView(browserEngine.view)

        etUrlBar = findViewById(R.id.etUrlBar)
        tvModeHud = findViewById(R.id.tvModeHud)
        tvModeHud.text = "Mode: Scroll"

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnReload = findViewById<Button>(R.id.btnReload)
        val btnGo = findViewById<Button>(R.id.btnGo)
        val btnDesktop = findViewById<Button>(R.id.btnDesktop)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnCheckUpdate = findViewById<Button>(R.id.btnCheckUpdate)

        btnBack.setOnClickListener { browserEngine.goBack() }
        btnForward.setOnClickListener { browserEngine.goForward() }
        btnHome.setOnClickListener { loadUrlAndSync("https://html.duckduckgo.com") }
        btnReload.setOnClickListener { browserEngine.reload() }
        btnGo.setOnClickListener { loadTypedUrl() }
        btnDesktop.setOnClickListener { toggleDesktopMode() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnCheckUpdate.setOnClickListener { checkForUpdates(manualCheck = true) }

        etUrlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                loadTypedUrl()
                true
            } else {
                false
            }
        }

        // 3. Start Tailscale Debug Web Server on Port 8080
        debugServer = TvDebugServer(
            port = 8080,
            onNavigate = { url ->
                runOnUiThread { loadUrlAndSync(url) }
            },
            onReloadExtensions = {
                runOnUiThread { reloadExtensions() }
            },
            onClearCache = {
                runOnUiThread { clearAppCache() }
            },
            getBlockedCount = {
                browserEngine.blockedAdsCount
            }
        )
        debugServer.start()

        // 4. Run background OTA update check on startup
        checkForUpdates(manualCheck = false)
        loadUrlAndSync("https://html.duckduckgo.com")
    }

    inner class KeyboardBridgeInterface {
        @JavascriptInterface
        fun onKeyboardInput(text: String, isBackspace: Boolean) {
            runOnUiThread {
                if (isBackspace) {
                    val currentText = etUrlBar.text.toString()
                    if (currentText.isNotEmpty()) {
                        etUrlBar.setText(currentText.dropLast(1))
                        etUrlBar.setSelection(etUrlBar.text.length)
                    }
                } else {
                    etUrlBar.append(text)
                }
            }
        }

        @JavascriptInterface
        fun submitToolbar() {
            runOnUiThread {
                loadTypedUrl()
            }
        }

        // --- NEW: Shifts Android focus from the toolbar directly into the WebView ---
        @JavascriptInterface
        fun requestWebViewFocus() {
            runOnUiThread {
                browserEngine.view.requestFocus()
            }
        }
    }

    private fun loadTypedUrl() {
        var target = etUrlBar.text.toString().trim()
        if (target.isNotEmpty()) {
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                target = "https://html.duckduckgo.com/html?q=$target"
            }
            loadUrlAndSync(target)
        }
    }

    private fun loadUrlAndSync(url: String) {
        etUrlBar.setText(url)
        browserEngine.loadUrl(url)
    }

    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        browserEngine.setDesktopMode(isDesktopMode)
        val modeText = if (isDesktopMode) "Desktop" else "Mobile"
        Toast.makeText(this, "Switched to $modeText mode", Toast.LENGTH_SHORT).show()
    }

    private fun reloadExtensions() {
        val cacheDir = filesDir
        cacheDir.listFiles()?.forEach { if (it.name.startsWith("cache_")) it.delete() }
        browserEngine.reload()
        Toast.makeText(this, "Extensions reloaded", Toast.LENGTH_SHORT).show()
    }

    private fun clearAppCache() {
        browserEngine.clearCache()
        Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.dispatchKeyEvent(event)

        val keyCode = event.keyCode

        if (event.action == KeyEvent.ACTION_DOWN) {
            // Track D-pad Center / Enter long-press for toggling mouse mode
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                if (event.repeatCount == 0) {
                    dpadCenterDownTime = System.currentTimeMillis()
                    hasTriggeredLongPress = false
                } else if (System.currentTimeMillis() - dpadCenterDownTime >= 800 && !hasTriggeredLongPress) {
                    hasTriggeredLongPress = true
                    isCursorActive = !isCursorActive
                    val status = if (isCursorActive) "ON" else "OFF"
                    tvModeHud.text = "Mode: " + if (isCursorActive) "Cursor" else "Scroll"
                    Toast.makeText(this, "Virtual Mouse: $status", Toast.LENGTH_SHORT).show()
                    browserEngine.evaluateJavascript("window.setCursorVisible($isCursorActive);", null)
                    return true
                }
            }

            val customToggleKey = KeyMappingHelper.getMappedKey(this)

            when (keyCode) {
                // Pressing Back jumps focus straight to the toolbar
                KeyEvent.KEYCODE_BACK -> {
                    etUrlBar.requestFocus()
                    Toast.makeText(this, "Toolbar Focused", Toast.LENGTH_SHORT).show()
                    return true
                }
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                    showSettingsDialog()
                    return true
                }
                customToggleKey -> {
                    isCursorActive = !isCursorActive
                    val status = if (isCursorActive) "ON" else "OFF"
                    tvModeHud.text = "Mode: " + if (isCursorActive) "Cursor" else "Scroll"
                    val mappedKey = KeyMappingHelper.getMappedKey(this)
                    Toast.makeText(this, "Virtual Mouse: $status (Mapped Key: $mappedKey)", Toast.LENGTH_SHORT).show()
                    browserEngine.evaluateJavascript("window.setCursorVisible($isCursorActive);", null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (isCursorActive) {
                        browserEngine.evaluateJavascript("window.tvStartMotion(0, -12);", null)
                        return true
                    } else {
                        // Jump up to toolbar buttons when at the top of scroll mode
                        etUrlBar.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN, 
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isCursorActive) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> browserEngine.evaluateJavascript("window.tvStartMotion(0, 12);", null)
                            KeyEvent.KEYCODE_DPAD_LEFT -> browserEngine.evaluateJavascript("window.tvStartMotion(-12, 0);", null)
                            KeyEvent.KEYCODE_DPAD_RIGHT -> browserEngine.evaluateJavascript("window.tvStartMotion(12, 0);", null)
                        }
                        return true
                    }
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                if (!hasTriggeredLongPress) {
                    if (isCursorActive) {
                        browserEngine.evaluateJavascript("if(window.handleEnterPress) { window.handleEnterPress(); } else { window.clickCursor(); }", null)
                    } else {
                        return super.dispatchKeyEvent(event)
                    }
                }
                return true
            }

            if (isCursorActive) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        browserEngine.evaluateJavascript("window.tvStopMotion('y');", null)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        browserEngine.evaluateJavascript("window.tvStopMotion('x');", null)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "View History", 
            "Clear WebView Cache", 
            "Reload Extensions Cache", 
            "View Local Debug Logs",
            "Switch Navigation Mode (Scroll / Mouse)"
        )
        AlertDialog.Builder(this)
            .setTitle("Browser Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showHistoryDialog()
                    1 -> clearAppCache()
                    2 -> reloadExtensions()
                    3 -> showDebugLogsDialog()
                    4 -> showModeSelectionDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showModeSelectionDialog() {
        val modes = arrayOf("Scroll Mode (Page panning)", "Mouse Mode (Virtual cursor)")
        AlertDialog.Builder(this)
            .setTitle("Choose Navigation Mode")
            .setItems(modes) { _, which ->
                isCursorActive = (which == 1)
                val statusText = if (isCursorActive) "Cursor" else "Scroll"
                tvModeHud.text = "Mode: $statusText"
                Toast.makeText(this, "Switched to $statusText Mode", Toast.LENGTH_SHORT).show()
                browserEngine.evaluateJavascript("window.setCursorVisible($isCursorActive);", null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkForUpdates(manualCheck: Boolean) {
        thread {
            try {
                val updateJsonStr = NetworkClient.fetchText("https://raw.githubusercontent.com/jch0029987-glitch/TV-web-wrapper/main/update.json")
                if (updateJsonStr != null) {
                    val json = JSONObject(updateJsonStr)
                    val latestVersionCode = json.getInt("versionCode")
                    val apkUrl = json.getString("apkUrl")
                    
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }

                    if (latestVersionCode > currentVersionCode) {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("Update Available")
                                .setMessage("A new version of the TV browser wrapper is available. Would you like to update now?")
                                .setPositiveButton("Update") { _, _ ->
                                    loadUrlAndSync(apkUrl)
                                    Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Later", null)
                                .show()
                        }
                    } else if (manualCheck) {
                        runOnUiThread {
                            Toast.makeText(this, "You are already on the latest version.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (manualCheck) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to check for updates.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                DebugConsoleStore.addLog("[JAVA ERROR] OTA Update check failed: ${e.message}")
                if (manualCheck) {
                    runOnUiThread {
                        Toast.makeText(this, "Error checking updates: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
                            loadUrlAndSync(historyList[index].url)
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
