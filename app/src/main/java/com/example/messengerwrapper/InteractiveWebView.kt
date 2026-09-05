package com.example.messengerwrapper

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebView

class InteractiveWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var onKeyInterceptListener: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (onKeyInterceptListener?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
