package com.pxy.depositbook.data.entity

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
    indices = [Index(value = ["day"], unique = true)],
)
data class AssetSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 快照日期，格式 yyyy-MM-dd，每天一条（当天多次记录取最新值） */
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
