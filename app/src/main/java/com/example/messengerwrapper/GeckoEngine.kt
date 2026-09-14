package com.example.messengerwrapper

import android.content.Context
import android.view.View
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController

class GeckoEngine(private val context: Context, private val runtime: GeckoRuntime) : IBrowserEngine {

    private val geckoSession = GeckoSession()
    private val geckoView = GeckoView(context).apply {
        setSession(geckoSession)
    }

    private var canGoBackState = false
    private var canGoForwardState = false

    init {
        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canGoBackState = canGoBack
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                canGoForwardState = canGoForward
            }
        }
        geckoSession.open(runtime)
    }

    val session: GeckoSession
        get() = geckoSession

    override val view: View
        get() = geckoView

    override fun loadUrl(url: String) {
        geckoSession.loadUri(url)
    }

    override fun goBack(): Boolean {
        if (canGoBackState) {
            geckoSession.goBack()
            return true
        }
        return false
    }

    override fun goForward(): Boolean {
        if (canGoForwardState) {
            geckoSession.goForward()
            return true
        }
        return false
    }

    override fun reload() {
        geckoSession.reload()
    }

    override fun setDesktopMode(enabled: Boolean) {
        val settings = geckoSession.settings
        settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
    }

    override fun setAdBlockEnabled(enabled: Boolean) {
        geckoSession.settings.useTrackingProtection = enabled
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        geckoSession.loadUri("javascript:$script")
        callback?.invoke(null)
    }

    override fun clearCache() {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL)
    }
}
