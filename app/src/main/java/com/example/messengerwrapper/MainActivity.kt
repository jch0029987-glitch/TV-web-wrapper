package com.example.messengerwrapper

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnFacebook: Button
    private lateinit var btnMessenger: Button
    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"
    private var downloadId: Long = -1L

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
        btnFacebook = findViewById(R.id.btnFacebook)
        btnMessenger = findViewById(R.id.btnMessenger)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        
        // Spoof User-Agent to force desktop/tablet web layout on TV
        webSettings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.facebook.com")

        btnFacebook.setOnClickListener { webView.loadUrl("https://www.facebook.com") }
        btnMessenger.setOnClickListener { webView.loadUrl("https://www.facebook.com/messages") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        checkForUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun checkForUpdates() {
        thread {
            try {
                val jsonURL = "https://raw.githubusercontent.com/$repoOwner/$repoName/main/update.json"
                val response = URL(jsonURL).readText()
                val json = JSONObject(response)
                
                val remoteVersionCode = json.getInt("versionCode")
                val apkUrl = json.getString("apkUrl")
                val localVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

                if (remoteVersionCode > localVersionCode) {
                    runOnUiThread { showUpdateDialog(apkUrl, json.getString("versionName")) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUpdateDialog(apkUrl: String, newVersion: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("Version $newVersion is available. The app will update automatically.")
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallApk(apkUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("App Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (::webView.isInitialized) {
            // Pass through typing keys and standard input
            if (event?.isPrintingKey == true || 
                keyCode == KeyEvent.KEYCODE_SPACE || 
                keyCode == KeyEvent.KEYCODE_ENTER || 
                keyCode == KeyEvent.KEYCODE_DEL) {
                return super.onKeyDown(keyCode, event)
            }

            // Handle remote control media keys for embedded videos/audio
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    return super.onKeyDown(keyCode, event)
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> { webView.scrollBy(0, 100); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { webView.scrollBy(0, -100); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { webView.scrollBy(-100, 0); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { webView.scrollBy(100, 0); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
