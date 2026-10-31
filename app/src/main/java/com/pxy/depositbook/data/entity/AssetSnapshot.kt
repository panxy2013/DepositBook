package com.pxy.depositbook.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资产快照：记录每天的真实总资产。
 *
 * 这是「历史数据」：之后删除/结清存单不影响已有快照，
 * 只有「清空所有数据」才会一起删除。
 */
@Entity(
    tableName = "asset_snapshot",
    indices = [Index(value = ["day", "profileId"], unique = true)],
)
data class AssetSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 归属账户（多账户） */
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1L,
    /** 快照日期，格式 yyyy-MM-dd，每天每账户一条（当天多次记录取最新值） */
    val day: String,
    /** 在存本金总数 */
    val totalPrincipal: Double,
    /** 累计已获利息 */
    val totalInterest: Double,
    /** 本金+利息 */
    val totalValue: Double,
    /** 在存笔数 */
    val activeCount: Int,
    /** 最后记录时间戳 */
    val recordedAt: Long,
)
