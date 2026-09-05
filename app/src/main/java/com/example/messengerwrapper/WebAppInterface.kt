package com.example.messengerwrapper

import android.webkit.JavascriptInterface

class WebAppInterface(private val onToggle: () -> Unit) {
    @JavascriptInterface
    fun requestToggleMouse() {
        onToggle()
    }
}

