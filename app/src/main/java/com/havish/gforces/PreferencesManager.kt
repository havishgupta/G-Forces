package com.havish.gforces

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gforce_prefs", Context.MODE_PRIVATE)

    var maxG: Float
        get() = prefs.getFloat("max_g", 1.5f)
        set(value) = prefs.edit().putFloat("max_g", value).apply()

    var isDebugMode: Boolean
        get() = prefs.getBoolean("debug_mode", false)
        set(value) = prefs.edit().putBoolean("debug_mode", value).apply()

    var calibX: Float
        get() = prefs.getFloat("calib_x", 0f)
        set(value) = prefs.edit().putFloat("calib_x", value).apply()

    var calibY: Float
        get() = prefs.getFloat("calib_y", 0f)
        set(value) = prefs.edit().putFloat("calib_y", value).apply()

    var calibZ: Float
        get() = prefs.getFloat("calib_z", 9.81f)
        set(value) = prefs.edit().putFloat("calib_z", value).apply()
}
