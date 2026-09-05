package com.example.messengerwrapper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class TVAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_TOGGLE_MOUSE_MODE = "com.example.messengerwrapper.TOGGLE_MOUSE_MODE"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val currentPackage = rootInActiveWindow?.packageName?.toString()
            if (currentPackage == packageName) {
                val mappedKey = KeyMappingHelper.getMappedKey(this)
                if (event.keyCode == mappedKey) {
                    val intent = Intent(ACTION_TOGGLE_MOUSE_MODE)
                    sendBroadcast(intent)
                    return true 
                }
            }
        }
        return super.onKeyEvent(event)
    }
}
