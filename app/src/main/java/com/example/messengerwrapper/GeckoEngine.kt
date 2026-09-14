package com.example.messengerwrapper

import android.content.Context
import android.view.View
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoEngine(private val context: Context) : IBrowserEngine {

    private val session = GeckoSession()
    private val runtime = GeckoRuntime.create(context)
    private val geckoView = GeckoView(context).apply {
        setSession(session)
    }

    init {
        session.open(runtime)
    }

    override val view: View
        get() = geckoView

    override fun loadUrl(url: String) {
        session.loadUri(url)
    }

    override fun goBack(): Boolean {
        if (session.navigation.canGoBack()) {
            session.navigation.goBack()
            return true
        }
        return false
    }

    override fun setDesktopMode(enabled: Boolean) {
        val settings = session.settings
        if (enabled) {
            settings.userAgentMode = GeckoSession.Settings.USER_AGENT_MODE_DESKTOP
        } else {
            settings.userAgentMode = GeckoSession.Settings.USER_AGENT_MODE_MOBILE
        }
    }

    override fun evaluateJavascript(script: String) {
        session.evaluateJS(script) { _, _ -> }
    }

    override fun clearCache() {
        runtime.storage.clearData(GeckoRuntime.Storage.STORE_ALL) { }
    }
}
