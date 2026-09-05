package com.example.messengerwrapper

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val repoOwner = "jch0029987-glitch" // Replace with your GitHub username
    private val repoName = "TV-web-wrapper"       // Replace with your repository name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        webView = findViewById(R.id.webView)
        val webSettings: WebSettings = webView.settings

        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.facebook.com/messages")

        checkForUpdates()
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
            .setMessage("Version $newVersion is available. Would you like to download and install it?")
            .setPositiveButton("Update") { _, _ ->
                downloadAndInstallApk(apkUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Messenger TV Update")
            .setDescription("Downloading new version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "messenger-tv-update.apk")

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (::webView.isInitialized) {
            // Pass typing keys, space, enter, and delete directly to the WebView for typing
            if (event?.isPrintingKey == true || 
                keyCode == KeyEvent.KEYCODE_SPACE || 
                keyCode == KeyEvent.KEYCODE_ENTER || 
                keyCode == KeyEvent.KEYCODE_DEL) {
                return super.onKeyDown(keyCode, event)
            }

            // D-pad navigation scrolling fallback when not typing
            when (keyCode) {
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
