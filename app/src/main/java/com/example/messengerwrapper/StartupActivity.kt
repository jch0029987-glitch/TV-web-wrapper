package com.example.messengerwrapper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)

        val btnLaunchBrowser = findViewById<Button>(R.id.btnLaunchBrowser)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        
        btnLaunchBrowser.requestFocus()

        btnLaunchBrowser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
