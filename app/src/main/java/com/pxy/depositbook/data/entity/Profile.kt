package com.pxy.depositbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账户档案：支持多账户（多本账）各记各的存款。
 * 昵称 + 头像（avatarPath 存相对文件名，实际文件在 filesDir/avatars/ 下；
 * 为空时 UI 显示系统默认设计的头像）。
 */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 昵称，如 默认账户、 honey 的账本 */
    val name: String,
    /** 头像文件名（相对 filesDir/avatars/），为空 = 使用默认头像 */
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** 排序权重：小的在前 */
    val sortOrder: Int = 0,
)
