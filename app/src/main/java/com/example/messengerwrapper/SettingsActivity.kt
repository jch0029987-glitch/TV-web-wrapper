package com.example.messengerwrapper

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etCustomUrl: EditText
    private lateinit var tvMappedKey: TextView
    private lateinit var btnMapKey: Button
    private lateinit var btnSaveSettings: Button
    private var capturedKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etCustomUrl = findViewById(R.id.etCustomUrl)
        tvMappedKey = findViewById(R.id.tvMappedKey)
        btnMapKey = findViewById(R.id.btnMapKey)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        val prefs = getSharedPreferences("BrowserPrefs", Context.MODE_PRIVATE)
        val currentUrl = prefs.getString("custom_url", "https://www.facebook.com")
        val currentKey = KeyMappingHelper.getMappedKey(this)

        etCustomUrl.setText(currentUrl)
        capturedKeyCode = currentKey
        tvMappedKey.text = "Mapped Key Code: $capturedKeyCode"

        btnMapKey.setOnClickListener {
            showKeyMappingDialog()
        }

        btnSaveSettings.setOnClickListener {
            val newUrl = etCustomUrl.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                prefs.edit().putString("custom_url", newUrl).apply()
                KeyMappingHelper.saveMappedKey(this, capturedKeyCode)
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showKeyMappingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Map Remote Button")
            .setMessage("Press any button on your remote control to map it...")
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                capturedKeyCode = keyCode
                tvMappedKey.text = "Mapped Key Code: $capturedKeyCode"
                Toast.makeText(this, "Key captured: $keyCode", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        dialog.show()
    }
}
