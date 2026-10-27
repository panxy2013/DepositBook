package com.pxy.depositbook.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用内多语言：中文（默认）/ 英文。
 *
 * - 在设置页切换，选择持久化到本地
 * - s() 会读取内部 State：在 Composable 中调用时，切换语言立即触发全局重组
 * - 非界面场景（通知、导出）读取当前 lang 值
 */
object I18n {
    const val ZH = "zh"
    const val EN = "en"

    var lang: String by mutableStateOf(ZH)
        private set

    val isZh: Boolean get() = lang != EN

    private var saver: ((String) -> Unit)? = null

    /** 应用启动时调用；saved = 已保存的语言代码，persist = 写入偏好 */
    fun init(saved: String, persist: (String) -> Unit) {
        lang = saved.ifBlank { ZH }
        saver = persist
    }

    /** 切换语言（zh / en）并持久化 */
    fun switchTo(tag: String) {
        lang = tag
        saver?.invoke(tag)
    }

    /** 翻译：返回 zh 或 en 两版之一 */
    fun s(zh: String, en: String): String = if (isZh) zh else en
}
