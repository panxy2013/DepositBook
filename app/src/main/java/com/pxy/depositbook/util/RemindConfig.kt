package com.pxy.depositbook.util

import android.content.Context
import com.pxy.depositbook.data.prefs.OptionsStore

/**
 * 到期提醒配置（进程级）：
 * - 应用启动时从 OptionsStore 读取「提前提醒天数」（默认 30 天）
 * - 设置页修改时同步更新，全 App 的到期判断（首页、提醒、卡片配色）随之生效
 */
object RemindConfig {
    @Volatile
    var days: Int = 30

    fun refresh(context: Context) {
        days = OptionsStore(context).reminderDays()
    }
}
