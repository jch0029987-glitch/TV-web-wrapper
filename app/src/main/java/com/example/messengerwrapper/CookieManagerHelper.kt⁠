package com.example.messengerwrapper

import android.webkit.CookieManager

object CookieManagerHelper {

    init {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(null, true)
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
