package com.example.messengerwrapper

import android.content.Context
import android.view.KeyEvent

object KeyMappingHelper {
    private const val PREFS_NAME = "tv_wrapper_prefs"
    private const val KEY_CUSTOM_BUTTON = "custom_button_code"

    fun getMappedKey(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CUSTOM_BUTTON, KeyEvent.KEYCODE_STAR)
    }

    fun saveMappedKey(context: Context, keyCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CUSTOM_BUTTON, keyCode).apply()
    }
}
