package com.pxy.depositbook.data.backup

import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder

/**
 * 备份文件结构。
 * - schemaVersion = 2：多账户。accounts[] 每个账户带自己的存款；
 *   同时顶层 deposits/logs 镜像全部数据，旧版 App 仍可读。
 * - schemaVersion = 1：单账户（deposits/logs 导入到当前账户）。
 */
data class BackupFile(
    @PrimaryKey val schemaVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val accounts: List<AccountDto> = emptyList(),
    val deposits: List<DepositDto> = emptyList(),
    val logs: List<LogDto> = emptyList(),
)

data class AccountDto(
    val name: String = "",
    val sortOrder: Int = 0,
    val deposits: List<DepositDto> = emptyList(),
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
     * 解析备份文件，兼容三种格式：
     * 1. v2 新版：{"schemaVersion":2,"accounts":[...],"deposits":[...],"logs":[...]}
     * 2. v1：{"schemaVersion":1,"deposits":[...],"logs":[...]}
     * 3. 更旧：直接是 [ {...}, {...} ] 的存款数组
     */
    fun decodeBackup(json: String): BackupFile? {
        val text = json.trim()
        if (text.isEmpty()) return null

        return try {
            if (text.startsWith("[")) {
                // 更旧：纯存款数组
                val arr = gson.fromJson(text, Array<DepositDto>::class.java)
                BackupFile(deposits = arr?.toList() ?: emptyList())
            } else if (text.startsWith("{")) {
                val obj = gson.fromJson(text, BackupFile::class.java) ?: return null
                // 只要文件里确实有 deposits 或 accounts 字段就认作有效备份
                if (text.contains("\"deposits\"") || text.contains("\"accounts\"")) obj else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
