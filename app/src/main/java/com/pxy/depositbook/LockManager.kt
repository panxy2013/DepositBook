package com.pxy.depositbook

import com.pxy.depositbook.util.I18n

import android.util.Log

/**
 * 应用锁挂起管理：
 * 发起系统文件选择器、系统分享等I18n.s("短暂离开应用", "Briefly left the app")的操作前调用 [suspend()]，
 * 期间应用退后台不触发锁屏；拿到结果返回后由 MainActivity 清除标记。
 *
 * 安全性：挂起有时限，超时后视为用户真正离开，返回仍会要求解锁，
 * 防止用户借文件选择器长期绕过应用锁。
 */
object LockManager {

    /** 挂起有效期：2 分钟 */
    private const val SUSPEND_TIMEOUT_MS = 2 * 60_000L

    @Volatile
    private var suspendedAt: Long = 0L

    /** 开始挂起（发起文件选择/分享前调用） */
    fun suspend() {
        suspendedAt = System.currentTimeMillis()
        Log.d("DepositBook", "LockManager suspend")
    }

    /** 是否处于有效挂起状态 */
    fun isSuspended(): Boolean {
        val started = suspendedAt
        return started != 0L && System.currentTimeMillis() - started < SUSPEND_TIMEOUT_MS
    }

    /** 清除挂起（返回应用时调用） */
    fun clear() {
        if (suspendedAt != 0L) {
            Log.d("DepositBook", "LockManager resume, suspended=${System.currentTimeMillis() - suspendedAt}ms")
        }
        suspendedAt = 0L
    }
}
