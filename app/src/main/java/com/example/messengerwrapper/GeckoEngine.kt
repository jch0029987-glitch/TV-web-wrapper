package com.example.messengerwrapper

import android.content.Context
import android.view.View
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoEngine(context: Context) : IBrowserEngine {
    private val geckoView = GeckoView(context)
    private val runtime = GeckoRuntime.create(context)
    private val session = GeckoSession().apply { open(runtime) }

    init {
        geckoView.setSession(session)
    }

    override val view: View get() = geckoView

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
        session.settings.userAgentMode = if (enabled) {
            GeckoSession.Settings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSession.Settings.USER_AGENT_MODE_MOBILE
        }
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        session.evaluateJS(script).then({ value ->
            callback?.invoke(value?.toString())
            null
        }, { _ ->
            callback?.invoke(null)
            null
        })
    }

    override fun clearCache() {
        runtime.storage.clearData(GeckoRuntime.STORE_ALL)
    }
}
