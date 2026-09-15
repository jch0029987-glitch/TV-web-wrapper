package com.example.messengerwrapper

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 1. Global crash & exception interceptor for dashboard streaming
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

        // Attach WebView view into the XML FrameLayout container
        val webViewContainer = findViewById<FrameLayout>(R.id.webViewContainer)
        webViewContainer.addView(browserEngine.view)

        // Bind XML UI elements
        etUrlBar = findViewById(R.id.etUrlBar)
        tvModeHud = findViewById(R.id.tvModeHud)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnReload = findViewById<Button>(R.id.btnReload)
        val btnGo = findViewById<Button>(R.id.btnGo)
        val btnDesktop = findViewById<Button>(R.id.btnDesktop)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnCheckUpdate = findViewById<Button>(R.id.btnCheckUpdate)

        // Button Click Listeners
        btnBack.setOnClickListener {
            if (browserEngine.canGoBack()) browserEngine.goBack()
        }
        btnForward.setOnClickListener {
            if (browserEngine.canGoForward()) browserEngine.goForward()
        }
        btnHome.setOnClickListener {
            loadUrlAndSync("https://html.duckduckgo.com")
        }
        btnReload.setOnClickListener {
            browserEngine.reload()
        }
        btnGo.setOnClickListener {
            loadTypedUrl()
        }
        btnDesktop.setOnClickListener {
            isDesktopMode = !isDesktopMode
            browserEngine.setDesktopMode(isDesktopMode)
            val modeText = if (isDesktopMode) "Desktop" else "Mobile"
            Toast.makeText(this, "Switched to $modeText mode", Toast.LENGTH_SHORT).show()
        }
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
        btnCheckUpdate.setOnClickListener {
            checkForUpdates(manualCheck = true)
        }

        // URL EditText IME Action Listener
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

        // 4. Check for OTA Updates automatically on launch
        checkForUpdates(manualCheck = false)

        // 5. Load initial homepage
        loadUrlAndSync("https://html.duckduckgo.com")
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

    private fun checkForUpdates(manualCheck: Boolean) {
        thread {
            try {
                val updateJsonStr = NetworkClient.fetchText("https://raw.githubusercontent.com/jch0029987-glitch/TV-web-wrapper/main/version.json")
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                showSettingsDialog()
                return true
            }
            KeyEvent.KEYCODE_PROG_RED -> {
                isCursorActive = !isCursorActive
                val status = if (isCursorActive) "ON" else "OFF"
                tvModeHud.text = "Mode: " + if (isCursorActive) "Cursor" else "Scroll"
                Toast.makeText(this, "Virtual Mouse: $status", Toast.LENGTH_SHORT).show()
                browserEngine.evaluateJavascript("if(window.setCursorVisible) window.setCursorVisible($isCursorActive);", null)
                return true
            }
            KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                Toast.makeText(this, "Custom Action Triggered", Toast.LENGTH_SHORT).show()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, 
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, 
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                if (isCursorActive) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                            browserEngine.evaluateJavascript("if(window.clickCursor) window.clickCursor();", null)
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            browserEngine.evaluateJavascript("if(window.moveCursor) window.moveCursor(0, -25); else window.tvScrollBy(0, -35);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            browserEngine.evaluateJavascript("if(window.moveCursor) window.moveCursor(0, 25); else window.tvScrollBy(0, 35);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            browserEngine.evaluateJavascript("if(window.moveCursor) window.moveCursor(-25, 0); else window.tvScrollBy(-35, 0);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            browserEngine.evaluateJavascript("if(window.moveCursor) window.moveCursor(25, 0); else window.tvScrollBy(35, 0);", null)
                        }
                    }
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
            "View Local Debug Logs",
            "Configure Custom Button Mapping"
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
                    4 -> showKeyMappingDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showKeyMappingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Custom Button Mapping")
            .setMessage("• Red Remote Button: Toggle Virtual Mouse / Scroll Mode\n• Green / Play-Pause: Custom Action Shortcut\n• Menu / Settings: Opens Settings Dialog\n• D-Pad (when mouse active): Steers Virtual Cursor")
            .setPositiveButton("OK", null)
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
