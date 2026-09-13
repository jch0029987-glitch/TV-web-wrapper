package com.example.messengerwrapper

import android.view.View

interface IBrowserEngine {
    val view: View
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun setDesktopMode(enabled: Boolean)
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)
    fun clearCache()
}
