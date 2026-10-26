package com.pxy.depositbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 存款记录（对应 Excel “总账” sheet 的一行）
 */
@Entity(tableName = "deposit")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 理财账户 / 银行名称 */
    val accountName: String,
    /** 归属人（可选） */
    val owner: String? = null,
    /** 产品名称，如 一年定期、5年定期、活期 */
    val productName: String? = null,
    /** 本金 */
    val principal: Double,
    /** 年化利率，0.04 表示 4% */
    val annualRate: Double,
    /** 存入 / 起息日期（epoch millis, 当地零点的 UTC 表示） */
    val startDate: Long,
    /** 到期日期 */
    val endDate: Long,
    /** 需存天数（可选，用于校验） */
    val termDays: Int? = null,
    /** 备注 */
    val note: String? = null,
    /** 是否仍在计息，false 表示已结清 */
    val isActive: Boolean = true,
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class RateType { FIXED, DEMAND, OTHER }
