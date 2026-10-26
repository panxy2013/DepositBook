package com.pxy.depositbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 操作日志：记录每一次新增 / 修改 / 删除 / 结清等操作
 */
@Entity(tableName = "operation_log")
data class OperationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 关联的存款 id，删除后仍保留 */
    val depositId: Long? = null,
    /** CREATE / UPDATE / DELETE / SETTLE / RESTORE / IMPORT */
    val action: String,
    /** 人类可读的日志内容 */
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
