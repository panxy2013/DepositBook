package com.pxy.depositbook.data.backup

import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder

/**
 * 备份文件结构。带 schemaVersion，方便以后升级格式时兼容旧文件。
 */
data class BackupFile(
    @PrimaryKey val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val deposits: List<DepositDto> = emptyList(),
    val logs: List<LogDto> = emptyList(),
)

data class DepositDto(
    val accountName: String = "",
    val owner: String? = null,
    val productName: String? = null,
    val principal: Double = 0.0,
    val annualRate: Double = 0.0,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val termDays: Int? = null,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class LogDto(
    val action: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
)

object BackupCodec {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    fun encodeBackup(file: BackupFile): String = gson.toJson(file)

    /**
     * 解析备份文件，同时兼容两种格式：
     * 1. 新版：{"schemaVersion":1,"deposits":[...],"logs":[...]}
     * 2. 旧版：直接是 [ {...}, {...} ] 的存款数组
     */
    fun decodeBackup(json: String): BackupFile? {
        val text = json.trim()
        if (text.isEmpty()) return null

        return try {
            if (text.startsWith("[")) {
                // 旧版：纯存款数组
                val arr = gson.fromJson(text, Array<DepositDto>::class.java)
                BackupFile(deposits = arr?.toList() ?: emptyList())
            } else if (text.startsWith("{")) {
                val obj = gson.fromJson(text, BackupFile::class.java) ?: return null
                // 只要文件里确实有 deposits 字段就认作有效备份
                if (text.contains("\"deposits\"")) obj else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
