package com.example.messengerwrapper

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.CookieManager
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchGeckoEngine: Switch
    private lateinit var switchAdBlock: Switch
    private lateinit var switchDesktopDefault: Switch
    private lateinit var btnClearCache: Button
    private lateinit var btnMapRemote: Button
    private lateinit var btnCheckUpdate: Button
    private lateinit var btnCustomUrl: Button
    
    private var capturedKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchGeckoEngine = findViewById(R.id.switchGeckoEngine)
        switchAdBlock = findViewById(R.id.switchAdBlock)
        switchDesktopDefault = findViewById(R.id.switchDesktopDefault)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnMapRemote = findViewById(R.id.btnMapRemote)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)
        btnCustomUrl = findViewById(R.id.btnCustomUrl)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        
        switchGeckoEngine.isChecked = prefs.getBoolean("use_gecko", true)
        switchAdBlock.isChecked = prefs.getBoolean("ad_block", true)
        switchDesktopDefault.isChecked = prefs.getBoolean("desktop_default", true)
        
        // Fetch using our KeyMappingHelper
        capturedKeyCode = KeyMappingHelper.getMappedKey(this)

        switchGeckoEngine.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_gecko", isChecked).apply()
            Toast.makeText(this, "Engine updated. Restart app to apply.", Toast.LENGTH_SHORT).show()
        }

        switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("ad_block", isChecked).apply()
        }

        switchDesktopDefault.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("desktop_default", isChecked).apply()
        }

        btnClearCache.setOnClickListener {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            Toast.makeText(this, "Cache and cookies cleared.", Toast.LENGTH_SHORT).show()
        }

        btnMapRemote.setOnClickListener {
            showKeyMappingDialog()
        }

        btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
        }

        btnCustomUrl.setOnClickListener {
            showCustomUrlDialog()
        }
    }

    private fun showKeyMappingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Map Remote Button")
            .setMessage("Press any remote control button to assign as the mouse toggle...")
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                capturedKeyCode = keyCode
                // Save using our KeyMappingHelper
                KeyMappingHelper.saveMappedKey(this, capturedKeyCode)
                Toast.makeText(this, "Button mapped successfully ($keyCode)", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        dialog.show()
    }

    private fun showCustomUrlDialog() {
        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        val currentUrl = prefs.getString("custom_url", "https://www.facebook.com") ?: "https://www.facebook.com"
        
        val input = EditText(this).apply {
            setText(currentUrl)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Custom URL")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    prefs.edit().putString("custom_url", newUrl).apply()
                    Toast.makeText(this, "Custom URL saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
