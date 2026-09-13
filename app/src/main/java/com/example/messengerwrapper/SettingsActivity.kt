package com.example.messengerwrapper

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchGecko: Switch
    private lateinit var etCustomUrl: EditText
    private lateinit var tvMappedKey: TextView
    private lateinit var btnMapKey: Button
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchGecko = findViewById(R.id.switchGeckoEngine)
        etCustomUrl = findViewById(R.id.etCustomUrl)
        tvMappedKey = findViewById(R.id.tvMappedKey)
        btnMapKey = findViewById(R.id.btnMapKey)
        btnSave = findViewById(R.id.btnSaveSettings)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        
        // Load existing preferences
        switchGecko.isChecked = prefs.getBoolean("use_gecko", true)
        etCustomUrl.setText(prefs.getString("custom_url", "https://www.facebook.com"))
        updateMappedKeyDisplay()

        btnMapKey.setOnClickListener {
            showKeyMappingDialog()
        }

        btnSave.setOnClickListener {
            val useGecko = switchGecko.isChecked
            val customUrl = etCustomUrl.text.toString().trim()

            prefs.edit().apply {
                putBoolean("use_gecko", useGecko)
                putString("custom_url", if (customUrl.isNotEmpty()) customUrl else "https://www.facebook.com")
                apply()
            }

            Toast.makeText(
                this,
                "Settings saved! Restart app to apply engine changes.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun updateMappedKeyDisplay() {
        val keyCode = KeyMappingHelper.getMappedKey(this)
        tvMappedKey.text = "Current Mapped Button KeyCode: $keyCode"
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
                updateMappedKeyDisplay()
                Toast.makeText(this, "Button mapped successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        dialog.show()
    }
}
