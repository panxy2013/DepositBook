package com.pxy.depositbook.data.model

import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.util.DateUtils
import com.pxy.depositbook.util.InterestCalculator
import java.time.LocalDate

/** 首页汇总统计 */
data class SummaryStats(
    val totalPrincipal: Double = 0.0,
    val todayInterest: Double = 0.0,
    val totalValueNow: Double = 0.0,
    val totalInterestEarned: Double = 0.0,
    val totalValueAtMaturity: Double = 0.0,
    val expiringSoonCount: Int = 0,
    val activeCount: Int = 0,
)

/** 按账户/银行分组的统计 */
data class GroupStat(
    val name: String,
    val principal: Double,
    val interest: Double,
    val valueNow: Double,
    val ratio: Float = 0f,
)

/** 月度趋势点 */
data class TrendPoint(
    val monthLabel: String,
    val principal: Double,
    val interest: Double,
) {
    val total: Double = principal + interest
}

/** 单条存款的展示模型 */
data class DepositUi(
    val deposit: Deposit,
    val daysPassed: Long,
    val daysLeft: Long,
    val dailyInterest: Double,
    val interestEarned: Double,
    val valueNow: Double,
    val valueAtMaturity: Double,
    val progress: Float,
) {
    val expiringSoon: Boolean get() = daysLeft in 0..30
    val urgent: Boolean get() = daysLeft in 0..7
    val expired: Boolean get() = daysLeft < 0
}

object StatsCalculator {

    fun summarize(deposits: List<Deposit>, today: LocalDate = DateUtils.today()): SummaryStats {
        val active = deposits.filter { it.isActive }
        var principal = 0.0
        var todayInterest = 0.0
        var interest = 0.0
        var valueNow = 0.0
        var valueAtMaturity = 0.0
        var expiring = 0
        active.forEach { d ->
            principal += d.principal
            todayInterest += InterestCalculator.dailyInterest(d.principal, d.annualRate)
            interest += InterestCalculator.interestEarned(d, today)
            valueNow += InterestCalculator.valueNow(d, today)
            valueAtMaturity += InterestCalculator.valueAtMaturity(d)
            val left = InterestCalculator.daysLeft(d, today)
            if (left in 0..30) expiring++
        }
        return SummaryStats(
            totalPrincipal = principal,
            todayInterest = todayInterest,
            totalValueNow = valueNow,
            totalInterestEarned = interest,
            totalValueAtMaturity = valueAtMaturity,
            expiringSoonCount = expiring,
            activeCount = active.size,
        )
    }

    fun toUi(deposit: Deposit, today: LocalDate = DateUtils.today()): DepositUi = DepositUi(
        deposit = deposit,
        daysPassed = InterestCalculator.daysPassed(deposit, today),
        daysLeft = InterestCalculator.daysLeft(deposit, today),
        dailyInterest = InterestCalculator.dailyInterest(deposit.principal, deposit.annualRate),
        interestEarned = InterestCalculator.interestEarned(deposit, today),
        valueNow = InterestCalculator.valueNow(deposit, today),
        valueAtMaturity = InterestCalculator.valueAtMaturity(deposit),
        progress = InterestCalculator.progress(deposit, today),
    )

    fun toUiList(deposits: List<Deposit>, today: LocalDate = DateUtils.today()): List<DepositUi> =
        deposits.map { toUi(it, today) }

    fun groupByAccount(deposits: List<Deposit>, today: LocalDate = DateUtils.today()): List<GroupStat> {
        val active = deposits.filter { it.isActive }
        val total = active.sumOf { it.principal }.takeIf { it > 0 } ?: 1.0
        return active
            .groupBy { it.accountName }
            .map { (name, list) ->
                val p = list.sumOf { it.principal }
                val interestEarned = list.sumOf { InterestCalculator.interestEarned(it, today) }
                GroupStat(
                    name = name,
                    principal = p,
                    interest = interestEarned,
                    valueNow = p + interestEarned,
                    ratio = (p / total).toFloat(),
                )
            }
            .sortedByDescending { it.principal }
    }

    /**
     * 生成最近 N 个月的累计本金 + 累计利息趋势。
     * 以“存款存在的月份”为口径：某月的累计本金 = 该月月末已存入且未到期的本金。
     */
    fun monthlyTrend(deposits: List<Deposit>, months: Int = 6, today: LocalDate = DateUtils.today()): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val active = deposits.filter { it.isActive }
        for (i in months - 1 downTo 0) {
            val target = today.minusMonths(i.toLong())
            var principal = 0.0
            var interest = 0.0
            active.forEach { d ->
                val start = DateUtils.toDate(d.startDate)
                val end = DateUtils.toDate(d.endDate)
                if (start.isAfter(target) || end.isBefore(target)) return@forEach
                principal += d.principal
                interest += InterestCalculator.dailyInterest(d.principal, d.annualRate) *
                        java.time.temporal.ChronoUnit.DAYS.between(start, target).coerceAtLeast(0)
            }
            result += TrendPoint(
                monthLabel = "%d月".format(target.monthValue),
                principal = principal,
                interest = interest,
            )
        }
        return result
    }
}
