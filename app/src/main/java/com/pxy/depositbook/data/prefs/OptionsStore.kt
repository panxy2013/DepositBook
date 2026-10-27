package com.pxy.depositbook.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * 银行名称、产品名称等可维护的下拉选项，保存在本地 SharedPreferences。
 */
class OptionsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("deposit_options", Context.MODE_PRIVATE)

    init {
        migrateIfNeeded()
    }

    /**
     * 早期版本把一批默认银行/产品预置写进了本地缓存。
     * 默认值调整后，需要移除这些「旧预置项」，但保留用户自己添加的项。
     */
    private fun migrateIfNeeded() {
        if (prefs.getInt(KEY_VERSION, 0) >= CURRENT_VERSION) return

        val legacyBanks = setOf(
            "中关村银行", "辽宁振兴银行", "新韩银行", "新城银行", "三星人寿",
            "众邦银行", "辽沈银行", "微信", "支付宝", "其他",
        )
        val legacyProducts = setOf(
            "一年定期", "三年定期", "五年定期", "活期", "月有余", "大额存单", "其他",
        )

        stripLegacy(KEY_BANKS, legacyBanks)
        stripLegacy(KEY_PRODUCTS, legacyProducts)

        prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
    }

    private fun stripLegacy(key: String, legacy: Set<String>) {
        val raw = prefs.getString(key, null) ?: return
        val kept = raw.split("\n")
            .filter { it.isNotBlank() && it !in legacy }
        val editor = prefs.edit()
        if (kept.isEmpty()) editor.remove(key) else editor.putString(key, kept.joinToString("\n"))
        editor.apply()
    }

    /** 银行名称列表（不含姓名） */
    /** 隐私模式默认状态：总览页金额是否默认隐藏（默认开启） */
    fun privacyDefault(): Boolean = prefs.getBoolean("privacy_default", true)

    fun setPrivacyDefault(on: Boolean) {
        prefs.edit().putBoolean("privacy_default", on).apply()
    }

    /** 显示语言：zh / en（默认中文） */
    fun language(): String = prefs.getString("language", "zh") ?: "zh"

    fun setLanguage(tag: String) {
        prefs.edit().putString("language", tag).apply()
    }

    fun banks(): List<String> = read(KEY_BANKS, DEFAULT_BANKS)

    fun addBank(name: String) = add(KEY_BANKS, DEFAULT_BANKS, name)

    fun removeBank(name: String) = remove(KEY_BANKS, DEFAULT_BANKS, name)

    fun renameBank(oldName: String, newName: String) = rename(KEY_BANKS, DEFAULT_BANKS, oldName, newName)

    /** 产品名称列表 */
    fun products(): List<String> = read(KEY_PRODUCTS, DEFAULT_PRODUCTS)

    fun addProduct(name: String) = add(KEY_PRODUCTS, DEFAULT_PRODUCTS, name)

    fun removeProduct(name: String) = remove(KEY_PRODUCTS, DEFAULT_PRODUCTS, name)

    fun renameProduct(oldName: String, newName: String) = rename(KEY_PRODUCTS, DEFAULT_PRODUCTS, oldName, newName)

    /** 恢复成默认选项（银行清空，产品为三个定期） */
    fun resetToDefaults() {
        prefs.edit().remove(KEY_BANKS).remove(KEY_PRODUCTS).apply()
    }

    private fun read(key: String, defaults: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return defaults
        return raw.split("\n").filter { it.isNotBlank() }
    }

    private fun add(key: String, defaults: List<String>, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val list = read(key, defaults).toMutableList()
        if (list.none { it.equals(trimmed, ignoreCase = true) }) {
            list += trimmed
            prefs.edit().putString(key, list.joinToString("\n")).apply()
        }
    }

    private fun remove(key: String, defaults: List<String>, name: String) {
        val list = read(key, defaults).filterNot { it == name }
        prefs.edit().putString(key, list.joinToString("\n")).apply()
    }

    private fun rename(key: String, defaults: List<String>, oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == oldName) return
        val list = read(key, defaults).toMutableList()
        val idx = list.indexOf(oldName)
        if (idx >= 0 && list.none { it.equals(trimmed, ignoreCase = true) && it != oldName }) {
            list[idx] = trimmed
            prefs.edit().putString(key, list.joinToString("\n")).apply()
        }
    }

    companion object {
        private const val KEY_BANKS = "banks"
        private const val KEY_PRODUCTS = "products"
        private const val KEY_VERSION = "options_version"

        /** 默认值变更时递增，触发一次本地缓存清理 */
        private const val CURRENT_VERSION = 2

        /** 银行名称默认留空，由用户在「设置 → 选项维护」里自行添加 */
        val DEFAULT_BANKS = emptyList<String>()

        val DEFAULT_PRODUCTS = listOf(
            "一年定期",
            "三年定期",
            "五年定期",
        )
    }
}
