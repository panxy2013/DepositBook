package com.pxy.depositbook.util

import com.pxy.depositbook.data.entity.Deposit
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * 利息计算，与 Excel “总账” 思路保持一致：
 * 日利息 = 本金 * 年化 / 365
 * 已获利息 = 日利息 * 已存天数
 * 预估资产 = 本金 + 已获利息
 */
object InterestCalculator {

    const val DAYS_PER_YEAR = 365.0

    fun dailyInterest(principal: Double, annualRate: Double): Double =
        principal * annualRate / DAYS_PER_YEAR

    fun daysPassed(deposit: Deposit, today: LocalDate = DateUtils.today()): Long =
        DateUtils.daysPassed(deposit.startDate, today)

    fun daysLeft(deposit: Deposit, today: LocalDate = DateUtils.today()): Long =
        DateUtils.daysLeft(deposit.endDate, today)

    /** 截止今天已产生的利息（不超过到期日） */
    fun interestEarned(deposit: Deposit, today: LocalDate = DateUtils.today()): Double {
        if (!deposit.isActive) return 0.0
        val totalDays = DateUtils.daysBetween(deposit.startDate, deposit.endDate)
        val passed = daysPassed(deposit, today).toDouble().coerceAtMost(totalDays.toDouble())
        return dailyInterest(deposit.principal, deposit.annualRate) * passed
    }

    /** 到期时的总利息 */
    fun interestAtMaturity(deposit: Deposit): Double {
        val totalDays = DateUtils.daysBetween(deposit.startDate, deposit.endDate)
        return dailyInterest(deposit.principal, deposit.annualRate) * totalDays
    }

    /** 截止今日 本金 + 已获利息 */
    fun valueNow(deposit: Deposit, today: LocalDate = DateUtils.today()): Double =
        deposit.principal + interestEarned(deposit, today)

    /** 到期预估本金 + 利息 */
    fun valueAtMaturity(deposit: Deposit): Double =
        deposit.principal + interestAtMaturity(deposit)

    /** 进度 0..1 */
    fun progress(deposit: Deposit, today: LocalDate = DateUtils.today()): Float {
        val total = DateUtils.daysBetween(deposit.startDate, deposit.endDate).toFloat()
        if (total <= 0f) return 1f
        return (daysPassed(deposit, today) / total).coerceIn(0f, 1f)
    }

    fun round2(v: Double): Double = (v * 100).roundToLong() / 100.0
}
