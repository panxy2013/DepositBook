package com.pxy.depositbook.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * 银行名称、产品名称等可维护的下拉选项，保存在本地 SharedPreferences。
 */

/** 产品定义：默认存期（天）+ 是否允许用户在存款里修改存期 */
data class ProductDef(
    val name: String,
    /** 默认存期天数；null 表示由用户自行填写 */
    val termDays: Long?,
    /** true = 选择该产品后存期可修改；false = 锁定 */
    val allowModify: Boolean,
)

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
        val from = prefs.getInt(KEY_VERSION, 0)

        val legacyBanks = setOf(
            "中关村银行", "辽宁振兴银行", "新韩银行", "新城银行", "三星人寿",
            "众邦银行", "辽沈银行", "微信", "支付宝", "其他",
        )
        val legacyProducts = setOf(
            "一年定期", "三年定期", "五年定期", "活期", "月有余", "大额存单", "其他",
        )

        if (from < 2) {
            stripLegacy(KEY_BANKS, legacyBanks)
            stripLegacy(KEY_PRODUCTS, legacyProducts)
        }
        if (from < 3) {
            // 迁移产品：旧字符串列表 → 带默认存期的定义；老默认名换成对应整存整取
            val old = read(KEY_PRODUCTS, emptyList())
            val defs = old.filter { it.isNotBlank() && it !in legacyProducts }
                .map { ProductDef(it, guessLegacyTerm(it), allowModify = true) }
                .toMutableList()
            DEFAULT_PRODUCT_DEFS.forEach { p ->
                if (defs.none { it.name == p.name }) defs.add(p)
            }
            prefs.edit().putString(KEY_PRODUCT_DEFS, gson.toJson(defs)).apply()
            prefs.edit().remove(KEY_PRODUCTS).apply()
        }

        prefs.edit().putInt(KEY_VERSION, CURRENT_VERSION).apply()
    }

    /** 旧产品名里猜测默认存期（五年定期 → 1825 天等） */
    private fun guessLegacyTerm(name: String): Long? = when {
        name.contains("五年") || name.contains("5年") -> 365L * 5
        name.contains("三年") || name.contains("3年") -> 365L * 3
        name.contains("两年") || name.contains("二年") || name.contains("2年") -> 365L * 2
        name.contains("一年") || name.contains("1年") -> 365L
        else -> null
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

    /** 产品定义（含默认存期与是否允许修改） */
    fun productDefs(): List<ProductDef> {
        val raw = prefs.getString(KEY_PRODUCT_DEFS, null) ?: return DEFAULT_PRODUCT_DEFS
        return runCatching {
            gson.fromJson(raw, Array<ProductDef>::class.java).toList()
        }.getOrDefault(DEFAULT_PRODUCT_DEFS)
    }

    fun setProductDefs(list: List<ProductDef>) {
        prefs.edit().putString(KEY_PRODUCT_DEFS, gson.toJson(list)).apply()
    }

    /** 添加/覆盖产品定义（同名覆盖） */
    fun addProductDef(def: ProductDef) {
        val list = productDefs().filterNot { it.name == def.name }.toMutableList()
        list.add(def)
        setProductDefs(list)
    }

    /** 产品名称列表（显示用） */
    fun products(): List<String> = productDefs().map { it.name }

    fun addProduct(name: String) = addProductDef(ProductDef(name, null, true))

    fun removeProduct(name: String) {
        setProductDefs(productDefs().filterNot { it.name == name })
    }

    fun renameProduct(oldName: String, newName: String) {
        val defs = productDefs().toMutableList()
        val idx = defs.indexOfFirst { it.name == oldName }
        if (idx >= 0 && defs.none { it.name == newName }) {
            defs[idx] = defs[idx].copy(name = newName)
            setProductDefs(defs)
        }
    }

    /** 恢复成默认选项（银行清空，产品为四个整存整取预置） */
    fun resetToDefaults() {
        prefs.edit().remove(KEY_BANKS).apply()
        setProductDefs(DEFAULT_PRODUCT_DEFS)
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
        private const val KEY_PRODUCT_DEFS = "product_defs"

        /** 默认值变更时递增，触发一次本地迁移 */
        private const val CURRENT_VERSION = 3

        private val gson = Gson()

        /** 银行名称默认留空，由用户在「设置 → 选项维护」里自行添加 */
        val DEFAULT_BANKS = emptyList<String>()

        /**
         * 系统预置存期产品：不允许选择后再更改存期。
         * 1年期整存整取 365 天、2年期 730 天、3年期 1095 天、5年期 1825 天
         */
        val DEFAULT_PRODUCT_DEFS = listOf(
            ProductDef("1年期整存整取", 365L, false),
            ProductDef("2年期整存整取", 365L * 2, false),
            ProductDef("3年期整存整取", 365L * 3, false),
            ProductDef("5年期整存整取", 365L * 5, false),
        )
    }
}
