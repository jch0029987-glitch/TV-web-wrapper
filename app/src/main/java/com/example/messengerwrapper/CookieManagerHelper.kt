package com.example.messengerwrapper

import android.webkit.CookieManager
import android.webkit.WebView

object CookieManagerHelper {

    init {
        // Global cookie acceptance doesn't require a WebView instance
        CookieManager.getInstance().setAcceptCookie(true)
    }

    // Call this once you have instantiated your WebView in MainActivity/Fragment
    fun setupThirdPartyCookies(webView: WebView) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    fun syncCookies() {
        CookieManager.getInstance().flush()
    }

    fun clearAllCookies(onCleared: () -> Unit = {}) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            onCleared()
        }
    }

    fun getCookiesForUrl(url: String): String? {
        return CookieManager.getInstance().getCookie(url)
    }

    fun setCookiesForUrl(url: String, cookieString: String) {
        val cookieManager = CookieManager.getInstance()
        for (cookie in cookieString.split(";")) {
            cookieManager.setCookie(url, cookie.trim())
        }
        cookieManager.flush()
    }
}
