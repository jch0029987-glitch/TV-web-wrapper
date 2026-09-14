package com.example.messengerwrapper

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var browserEngine: IBrowserEngine
    private lateinit var nativeBridge: NativeBridge

    private lateinit var etUrlBar: EditText
    private lateinit var btnGo: Button
    private lateinit var btnHome: Button
    private lateinit var btnBack: Button
    private lateinit var btnForward: Button
    private lateinit var btnReload: Button
    private lateinit var btnDesktop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnCheckUpdate: Button
    private lateinit var tvModeHud: TextView
    
    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"
    private var downloadId: Long = -1L
    private var isMouseModeActive = false
    private lateinit var audioManager: AudioManager

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                installDownloadedApk()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        try {
            nativeBridge = NativeBridge()
            Log.d("NativeBridge", nativeBridge.nativeBridgeWorker("PING"))
        } catch (e: Exception) {
            Log.e("NativeBridge", "Failed to initialize native bridge library", e)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        requestAudioPlaybackFocus()

        val container = findViewById<FrameLayout>(R.id.webViewContainer)
        browserEngine = initBrowserEngine(container)
        browserEngine.clearCache()

        etUrlBar = findViewById(R.id.etUrlBar)
        btnGo = findViewById(R.id.btnGo)
        btnHome = findViewById(R.id.btnHome)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnReload = findViewById(R.id.btnReload)
        btnDesktop = findViewById(R.id.btnDesktop)
        btnSettings = findViewById(R.id.btnSettings)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)
        tvModeHud = findViewById(R.id.tvModeHud)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        val targetUrl = prefs.getString("custom_url", "https://duckduckgo.com") ?: "https://duckduckgo.com"
        
        browserEngine.loadUrl(targetUrl)
        etUrlBar.setText(targetUrl)

        btnGo.setOnClickListener { executeUrlSearch() }
        etUrlBar.setOnEditorActionListener { _, _, _ -> executeUrlSearch(); true }
        
        btnHome.setOnClickListener { browserEngine.loadUrl(targetUrl); etUrlBar.setText(targetUrl) }
        btnBack.setOnClickListener { browserEngine.goBack() }
        btnForward.setOnClickListener { browserEngine.goForward() }
        btnReload.setOnClickListener { browserEngine.reload() }
        
        btnDesktop.setOnClickListener { 
            val isDesktop = btnDesktop.tag?.toString() == "desktop"
            browserEngine.setDesktopMode(!isDesktop)
            if (!isDesktop) {
                btnDesktop.tag = "desktop"
                btnDesktop.text = "Mobile Mode"
                Toast.makeText(this, "Desktop Mode Activated", Toast.LENGTH_SHORT).show()
            } else {
                btnDesktop.tag = "mobile"
                btnDesktop.text = "Desktop Mode"
                Toast.makeText(this, "Mobile Mode Activated", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnSettings.setOnClickListener { showKeyMappingDialog() }
        btnCheckUpdate.setOnClickListener { checkForUpdates(manualCheck = true) }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), flags)

        checkForUpdates(manualCheck = false)
    }

    private fun executeUrlSearch() {
        var input = etUrlBar.text.toString().trim()
        if (input.isNotEmpty()) {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = if (input.contains(".") && !input.contains(" ")) "https://$input" else "https://duckduckgo.com/?q=${Uri.encode(input)}"
            }
            browserEngine.loadUrl(input)
            etUrlBar.setText(input)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun initBrowserEngine(container: FrameLayout): IBrowserEngine {
        val engine = WebViewEngine(this, nativeBridge)
        container.addView(engine.view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return engine
    }

    private fun requestAudioPlaybackFocus() {
        val listener = AudioManager.OnAudioFocusChangeListener { focus ->
            if (focus == AudioManager.AUDIOFOCUS_LOSS || focus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                browserEngine.evaluateJavascript("document.querySelectorAll('video, audio').forEach(el => el.pause());", null)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(listener).build())
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun toggleMouseMode() {
        isMouseModeActive = !isMouseModeActive
        if (isMouseModeActive) {
            tvModeHud.text = "Mode: Mouse & Scroll"
            browserEngine.evaluateJavascript("document.activeElement.blur(); window.setCursorVisible(true);", null)
        } else {
            tvModeHud.text = "Mode: Scroll"
            browserEngine.evaluateJavascript("window.setCursorVisible(false);", null)
            etUrlBar.requestFocus()
        }
    }

    private fun showKeyMappingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Map Custom Remote Button")
            .setMessage("Press the remote button you want to use to toggle Mouse/Scroll Mode.")
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                KeyMappingHelper.saveMappedKey(this, keyCode)
                Toast.makeText(this, "Button mapped successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                true
            } else false
        }
        dialog.show()
    }

    private fun checkForUpdates(manualCheck: Boolean = false) {
        thread {
            try {
                val connection = URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/update.json").openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (json.getInt("versionCode") > packageManager.getPackageInfo(packageName, 0).longVersionCode) {
                    runOnUiThread { showUpdateDialog(json.getString("apkUrl"), json.getString("versionName"), json.optString("releaseNotes", "")) }
                } else if (manualCheck) {
                    runOnUiThread { Toast.makeText(this, "Up to date.", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                if (manualCheck) runOnUiThread { Toast.makeText(this, "Update check failed.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, version: String, notes: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available ($version)")
            .setMessage("$notes\n\nInstall update now?")
            .setPositiveButton("Update") { _, _ -> downloadAndInstallApk(apkUrl) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()
        val request = DownloadManager.Request(Uri.parse(url)).setDestinationUri(Uri.fromFile(destination))
        downloadId = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }

    private fun installDownloadedApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyMappingHelper.getMappedKey(this) && event.action == KeyEvent.ACTION_DOWN) {
            toggleMouseMode()
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_L) { 
                etUrlBar.requestFocus()
                etUrlBar.selectAll()
                return true 
            }
            if (event.keyCode == KeyEvent.KEYCODE_F5 || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_R)) { 
                browserEngine.reload()
                return true 
            }
        }

        val isTextEditing = (event.unicodeChar != 0 && event.action == KeyEvent.ACTION_DOWN) ||
                            event.keyCode == KeyEvent.KEYCODE_DEL || event.keyCode == KeyEvent.KEYCODE_ENTER ||
                            event.keyCode == KeyEvent.KEYCODE_SPACE || event.keyCode == KeyEvent.KEYCODE_TAB

        if (etUrlBar.hasFocus() || (isTextEditing && !isMouseModeActive)) {
            return super.dispatchKeyEvent(event)
        }

        if (isMouseModeActive && event.action == KeyEvent.ACTION_DOWN) {
            val scrollStep = 75
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> { 
                    browserEngine.evaluateJavascript("window.scrollBy(0, $scrollStep);", null)
                    return true 
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> { 
                    browserEngine.evaluateJavascript("window.scrollBy(0, -$scrollStep);", null)
                    return true 
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> { 
                    browserEngine.evaluateJavascript("window.scrollBy(-$scrollStep, 0);", null)
                    return true 
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> { 
                    browserEngine.evaluateJavascript("window.scrollBy($scrollStep, 0);", null)
                    return true 
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { 
                    browserEngine.evaluateJavascript("window.clickCursor();", null)
                    return true 
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isMouseModeActive) toggleMouseMode()
        else if (!browserEngine.goBack()) etUrlBar.requestFocus()
    }
}
