package com.example.messengerwrapper

import android.view.View

interface IBrowserEngine {
    val view: View
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun setDesktopMode(desktop: Boolean)
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)
    fun clearCache()
}
