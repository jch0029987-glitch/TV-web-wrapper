package com.example.messengerwrapper

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var browserEngine: IBrowserEngine
    private lateinit var nativeBridge: NativeBridge
    
    private lateinit var btnFacebook: Button
    private lateinit var btnMessenger: Button
    private lateinit var btnX: Button
    private lateinit var btnSettings: Button
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnDesktop: Button
    private lateinit var btnMobile: Button
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

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Initialize Native C Bridge and load libbridge_worker.so
        try {
            nativeBridge = NativeBridge()
            val pingResponse = nativeBridge.nativeBridgeWorker("PING")
            Log.d("NativeBridge", pingResponse)
        } catch (e: Exception) {
            Log.e("NativeBridge", "Failed to initialize native bridge library", e)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        requestAudioPlaybackFocus()

        val container = findViewById<FrameLayout>(R.id.webViewContainer)
        browserEngine = initBrowserEngine(container)
        browserEngine.clearCache()

        btnFacebook = findViewById(R.id.btnFacebook)
        btnMessenger = findViewById(R.id.btnMessenger)
        btnX = findViewById(R.id.btnX)
        btnSettings = findViewById(R.id.btnSettings)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)
        btnDesktop = findViewById(R.id.btnDesktop)
        btnMobile = findViewById(R.id.btnMobile)
        tvModeHud = findViewById(R.id.tvModeHud)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        val targetUrl = prefs.getString("custom_url", "https://www.facebook.com") ?: "https://www.facebook.com"
        
        // Pass target through native worker if needed
        try {
            val filterCheck = nativeBridge.nativeBridgeWorker("GET $targetUrl")
            if (filterCheck.startsWith("HTTP/1.1 200 OK")) {
                Toast.makeText(this, "Blocked tracker via C bridge", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Fallback if uninitialized
        }

        browserEngine.loadUrl(targetUrl)

        btnFacebook.setOnClickListener { browserEngine.loadUrl("https://www.facebook.com") }
        btnMessenger.setOnClickListener { browserEngine.loadUrl("https://www.facebook.com/messages") }
        btnX.setOnClickListener { browserEngine.loadUrl("https://x.com") }
        btnSettings.setOnClickListener { showKeyMappingDialog() }
        btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            checkForUpdates(manualCheck = true)
        }
        btnDesktop.setOnClickListener {
            browserEngine.setDesktopMode(true)
            Toast.makeText(this, "Switched to Desktop Mode", Toast.LENGTH_SHORT).show()
        }
        btnMobile.setOnClickListener {
            browserEngine.setDesktopMode(false)
            Toast.makeText(this, "Switched to Mobile Mode", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        checkForUpdates(manualCheck = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun initBrowserEngine(container: FrameLayout): IBrowserEngine {
        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        val useGecko = prefs.getBoolean("use_gecko", true)

        val engine: IBrowserEngine = try {
            if (useGecko) GeckoEngine(this) else WebViewEngine(this)
        } catch (e: Exception) {
            prefs.edit().putBoolean("use_gecko", false).apply()
            Toast.makeText(this, "Gecko engine failed. Falling back to WebView.", Toast.LENGTH_LONG).show()
            WebViewEngine(this)
        }

        container.addView(
            engine.view, 
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return engine
    }

    private fun requestAudioPlaybackFocus() {
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                browserEngine.evaluateJavascript("document.querySelectorAll('video, audio').forEach(el => el.pause());")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun toggleMouseMode() {
        isMouseModeActive = !isMouseModeActive
        if (isMouseModeActive) {
            Toast.makeText(this, "Mouse Mode: ON", Toast.LENGTH_SHORT).show()
            tvModeHud.text = "Mode: Mouse"
            browserEngine.evaluateJavascript("document.activeElement.blur(); window.setCursorVisible(true);")
        } else {
            Toast.makeText(this, "Mouse Mode: OFF (Sidebar)", Toast.LENGTH_SHORT).show()
            tvModeHud.text = "Mode: Scroll"
            browserEngine.evaluateJavascript("window.setCursorVisible(false);")
            btnFacebook.requestFocus()
        }
    }

    private fun showKeyMappingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Map Custom Remote Button")
            .setMessage("Press the remote button you want to use to toggle Mouse Mode.")
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                KeyMappingHelper.saveMappedKey(this, keyCode)
                Toast.makeText(this, "Button mapped successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        dialog.show()
    }

    private fun checkForUpdates(manualCheck: Boolean = false) {
        thread {
            try {
                val jsonURL = URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/update.json")
                val connection = jsonURL.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val remoteVersionCode = json.getInt("versionCode")
                val apkUrl = json.getString("apkUrl")
                val versionName = json.getString("versionName")
                val releaseNotes = json.optString("releaseNotes", "Performance improvements and bug fixes.")
                
                val localVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

                if (remoteVersionCode > localVersionCode) {
                    runOnUiThread { showUpdateDialog(apkUrl, versionName, releaseNotes) }
                } else if (manualCheck) {
                    runOnUiThread {
                        Toast.makeText(this, "You are using the latest version.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (manualCheck) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to check for updates. Check network.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, newVersion: String, releaseNotes: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available ($newVersion)")
            .setMessage("Here are the changes in this version:\n\n$releaseNotes\n\nThe app will update automatically.")
            .setPositiveButton("Update Now") { _, _ -> downloadAndInstallApk(apkUrl) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        try {
            Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show()
            val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (destination.exists()) destination.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("App Update")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun installDownloadedApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (!file.exists()) return

        val apkUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val mappedKey = KeyMappingHelper.getMappedKey(this)
        if (event.keyCode == mappedKey && event.action == KeyEvent.ACTION_DOWN) {
            toggleMouseMode()
            return true
        }

        val isTextEditing = (event.unicodeChar != 0 && event.action == KeyEvent.ACTION_DOWN) ||
                            event.keyCode == KeyEvent.KEYCODE_DEL ||
                            event.keyCode == KeyEvent.KEYCODE_ENTER ||
                            event.keyCode == KeyEvent.KEYCODE_SPACE ||
                            event.keyCode == KeyEvent.KEYCODE_TAB

        if (isTextEditing && !isMouseModeActive) {
            return super.dispatchKeyEvent(event)
        }

        if (isMouseModeActive && event.action == KeyEvent.ACTION_DOWN) {
            val step = 30
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { browserEngine.evaluateJavascript("window.moveCursor(0, $step);"); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { browserEngine.evaluateJavascript("window.moveCursor(0, -$step);"); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { browserEngine.evaluateJavascript("window.moveCursor(-$step, 0);"); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { browserEngine.evaluateJavascript("window.moveCursor($step, 0);"); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { browserEngine.evaluateJavascript("window.clickCursor();"); return true }
            }
        }

        val isSidebarFocused = btnFacebook.hasFocus() || 
                               btnMessenger.hasFocus() || 
                               btnX.hasFocus() || 
                               btnSettings.hasFocus() || 
                               btnCheckUpdate.hasFocus() ||
                               btnDesktop.hasFocus() ||
                               btnMobile.hasFocus()

        if (!isMouseModeActive && !isSidebarFocused && event.action == KeyEvent.ACTION_DOWN) {
            val scrollStep = 150
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { browserEngine.evaluateJavascript("window.scrollBy(0, $scrollStep);"); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { browserEngine.evaluateJavascript("window.scrollBy(0, -$scrollStep);"); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { browserEngine.evaluateJavascript("window.scrollBy(-$scrollStep, 0);"); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { browserEngine.evaluateJavascript("window.scrollBy($scrollStep, 0);"); return true }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isMouseModeActive) {
            toggleMouseMode()
        } else if (browserEngine.goBack()) {
            // Handled inside engine
        } else {
            btnFacebook.requestFocus()
        }
    }
}
