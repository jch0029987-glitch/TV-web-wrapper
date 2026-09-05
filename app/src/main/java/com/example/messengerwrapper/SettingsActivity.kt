package com.example.messengerwrapper

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

    private val repoOwner = "jch0029987-glitch"
    private val repoName = "TV-web-wrapper"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)

        val switchAdBlock = findViewById<Switch>(R.id.switchAdBlock)
        val switchDesktopDefault = findViewById<Switch>(R.id.switchDesktopDefault)
        val btnClearCache = findViewById<Button>(R.id.btnClearCache)
        val btnMapRemote = findViewById<Button>(R.id.btnMapRemote)
        val btnCheckUpdate = findViewById<Button>(R.id.btnCheckUpdate)

        // Load saved preferences
        switchAdBlock.isChecked = prefs.getBoolean("ad_block_enabled", true)
        switchDesktopDefault.isChecked = prefs.getBoolean("desktop_mode_default", true)

        switchAdBlock.requestFocus()

        // Save preferences on change
        switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("ad_block_enabled", isChecked).apply()
            Toast.makeText(this, "Ad blocker " + if (isChecked) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
        }

        switchDesktopDefault.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("desktop_mode_default", isChecked).apply()
            Toast.makeText(this, "Desktop default " + if (isChecked) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
        }

        btnClearCache.setOnClickListener {
            try {
                val cacheDir = cacheDir
                cacheDir.deleteRecursively()
                Toast.makeText(this, "Browser cache cleared successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to clear cache.", Toast.LENGTH_SHORT).show()
            }
        }

        btnMapRemote.setOnClickListener {
            showKeyMappingDialog()
        }

        btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            checkForUpdates()
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

    private fun checkForUpdates() {
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
                val versionName = json.getString("versionName")
                val releaseNotes = json.optString("releaseNotes", "Performance improvements.")
                
                val localVersionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

                if (remoteVersionCode > localVersionCode) {
                    runOnUiThread {
                        Toast.makeText(this, "Update available: $versionName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "You are using the latest version.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to check for updates. Check network.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
