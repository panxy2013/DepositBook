package com.pxy.depositbook.data.prefs

import android.content.Context
import android.content.SharedPreferences

class SecurityStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
    }

    fun getLockType(): String = prefs.getString("lock_type", "pin") ?: "pin"

    fun setLockType(type: String) {
        prefs.edit().putString("lock_type", type).apply()
    }

    fun getPin(): String = prefs.getString("pin", "") ?: ""

    fun setPin(pin: String) {
        prefs.edit().putString("pin", pin).apply()
    }

    fun getPattern(): String = prefs.getString("pattern", "") ?: ""

    fun setPattern(pattern: String) {
        prefs.edit().putString("pattern", pattern).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
