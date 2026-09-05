package com.example.messengerwrapper

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: InteractiveWebView
    private lateinit var btnFacebook: Button
    private lateinit var btnMessenger: Button
    private lateinit var btnX: Button
    private lateinit var btnSettings: Button
    private lateinit var btnCheckUpdate: Button
    
    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"
    private var downloadId: Long = -1L
    private var isMouseModeActive = false

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

        webView = findViewById(R.id.webView)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        btnFacebook = findViewById(R.id.btnFacebook)
        btnMessenger = findViewById(R.id.btnMessenger)
        btnX = findViewById(R.id.btnX)
        btnSettings = findViewById(R.id.btnSettings)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        webView.addJavascriptInterface(WebAppInterface {
            runOnUiThread { toggleMouseMode() }
        }, "AndroidBridge")

        webView.onKeyInterceptListener = { event ->
            val mappedKey = KeyMappingHelper.getMappedKey(this)
            if (event.keyCode == mappedKey && event.action == KeyEvent.ACTION_DOWN) {
                toggleMouseMode()
                true
            } else {
                false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectVirtualCursor()
            }
        }

        webView.loadUrl("https://www.facebook.com")

        btnFacebook.setOnClickListener { webView.loadUrl("https://www.facebook.com") }
        btnMessenger.setOnClickListener { webView.loadUrl("https://www.facebook.com/messages") }
        btnX.setOnClickListener { webView.loadUrl("https://x.com") }
        btnSettings.setOnClickListener { showKeyMappingDialog() }
        btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            checkForUpdates(manualCheck = true)
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
        CookieManager.getInstance().flush()
    }

    private fun toggleMouseMode() {
        isMouseModeActive = !isMouseModeActive
        if (isMouseModeActive) {
            Toast.makeText(this, "Mouse Mode: ON", Toast.LENGTH_SHORT).show()
            webView.evaluateJavascript("document.activeElement.blur(); window.setCursorVisible(true);", null)
        } else {
            Toast.makeText(this, "Mouse Mode: OFF (Sidebar)", Toast.LENGTH_SHORT).show()
            webView.evaluateJavascript("window.setCursorVisible(false);", null)
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

    private fun injectVirtualCursor() {
        val cursorScript = """
            (function() {
                if (document.getElementById('tv-virtual-cursor')) return;
                
                const cursor = document.createElement('div');
                cursor.id = 'tv-virtual-cursor';
                cursor.style.position = 'fixed';
                cursor.style.width = '20px';
                cursor.style.height = '20px';
                cursor.style.borderRadius = '50%';
                cursor.style.backgroundColor = 'rgba(255, 0, 0, 0.7)';
                cursor.style.border = '2px solid white';
                cursor.style.zIndex = '999999';
                cursor.style.pointerEvents = 'none';
                cursor.style.display = 'none';
                cursor.style.transition = 'transform 0.05s linear';
                cursor.style.left = '50vw';
                cursor.style.top = '50vh';
                document.body.appendChild(cursor);

                window.setCursorVisible = function(visible) {
                    cursor.style.display = visible ? 'block' : 'none';
                };

                window.moveCursor = function(dx, dy) {
                    const rect = cursor.getBoundingClientRect();
                    let x = rect.left + dx;
                    let y = rect.top + dy;
                    x = Math.max(0, Math.min(window.innerWidth - 20, x));
                    y = Math.max(0, Math.min(window.innerHeight - 20, y));
                    cursor.style.left = x + 'px';
                    cursor.style.top = y + 'px';
                };

                window.clickCursor = function() {
                    const rect = cursor.getBoundingClientRect();
                    const x = rect.left + 10;
                    const y = rect.top + 10;
                    const target = document.elementFromPoint(x, y);
                    if (target) {
                        target.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, clientX: x, clientY: y }));
                        target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, clientX: x, clientY: y }));
                        target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: x, clientY: y }));
                        target.click();
                    }
                };
            })();
        """.trimIndent()
        webView.evaluateJavascript(cursorScript, null)
    }

    private fun checkForUpdates(manualCheck: Boolean = false) {
        thread {
            try {
                val jsonURL = "https://raw.githubusercontent.com/$repoOwner/$repoName/main/update.json"
                val response = URL(jsonURL).readText()
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
                        Toast.makeText(this, "Failed to check for updates. Check connection.", Toast.LENGTH_SHORT).show()
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

        if (isMouseModeActive && event.action == KeyEvent.ACTION_DOWN) {
            val step = 30
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { webView.evaluateJavascript("window.moveCursor(0, $step);", null); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { webView.evaluateJavascript("window.moveCursor(0, -$step);", null); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { webView.evaluateJavascript("window.moveCursor(-$step, 0);", null); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { webView.evaluateJavascript("window.moveCursor($step, 0);", null); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { webView.evaluateJavascript("window.clickCursor();", null); return true }
            }
        }

        val isSidebarFocused = btnFacebook.hasFocus() || btnMessenger.hasFocus() || btnX.hasFocus() || btnSettings.hasFocus() || btnCheckUpdate.hasFocus()
        if (!isMouseModeActive && !isSidebarFocused && event.action == KeyEvent.ACTION_DOWN) {
            val scrollStep = 150
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { webView.evaluateJavascript("window.scrollBy(0, $scrollStep);", null); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { webView.evaluateJavascript("window.scrollBy(0, -$scrollStep);", null); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { webView.evaluateJavascript("window.scrollBy(-$scrollStep, 0);", null); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { webView.evaluateJavascript("window.scrollBy($scrollStep, 0);", null); return true }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isMouseModeActive) {
            toggleMouseMode()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            btnFacebook.requestFocus()
        }
    }
}
