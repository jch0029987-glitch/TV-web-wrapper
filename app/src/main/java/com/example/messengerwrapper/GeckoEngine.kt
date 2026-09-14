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

    init {
        geckoSession.open(runtime)
    }

    override val session: GeckoSession
        get() = geckoSession

    override val view: View
        get() = geckoView

    override fun loadUrl(url: String) {
        geckoSession.loadUri(url)
    }

    override fun goBack(): Boolean {
        val navigation = geckoSession.navigation
        if (navigation != null && navigation.canGoBack()) {
            navigation.goBack()
            return true
        }
        return false
    }

    override fun goForward(): Boolean {
        val navigation = geckoSession.navigation
        if (navigation != null && navigation.canGoForward()) {
            navigation.goForward()
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
        runtime.storageController.clearData(StorageController.CLEAR_FLAGS_ALL)
    }
}
