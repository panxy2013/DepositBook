package com.pxy.depositbook.data.model

import com.pxy.depositbook.data.entity.AssetSnapshot
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.util.I18n
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

    /** 推算 date 当天的在存本金与累计利息（用于没有快照的历史月份/年份） */
    private fun deriveAt(active: List<Deposit>, date: LocalDate): Pair<Double, Double> {
        var principal = 0.0
        var interest = 0.0
        active.forEach { d ->
            val start = DateUtils.toDate(d.startDate)
            val end = DateUtils.toDate(d.endDate)
            if (start.isAfter(date) || end.isBefore(date)) return@forEach
            principal += d.principal
            interest += InterestCalculator.dailyInterest(d.principal, d.annualRate) *
                    java.time.temporal.ChronoUnit.DAYS.between(start, date).coerceAtLeast(0)
        }
        return principal to interest
    }

    /**
     * 按月趋势（扩展版，供统计页折线图使用）：
     * - 窗口自当前月往前最长 maxMonths 个月
     * - 有历史快照的月份优先取快照（月末最后一天的记录），否则按存单推算
     * - 从第一个资产大于 0 的点开始显示，去掉前导零
     * - 跨年时月份标签带年份前缀（如 “25年8月”），否则仅显示 “8月”
     */
    fun monthlyTrendEx(
        deposits: List<Deposit>,
        history: List<AssetSnapshot> = emptyList(),
        maxMonths: Int = 12,
        today: LocalDate = DateUtils.today(),
    ): List<TrendPoint> {
        val snapshots = history.groupBy { it.day.take(7) }
            .mapValues { (_, list) -> list.maxBy { it.day } }
        val active = deposits.filter { it.isActive }

        data class Pt(val date: LocalDate, val principal: Double, val interest: Double)
        val points = mutableListOf<Pt>()
        for (i in maxMonths - 1 downTo 0) {
            val target = today.minusMonths(i.toLong())
            val snap = snapshots["%04d-%02d".format(target.year, target.monthValue)]
            val (p, interest) = if (snap != null) {
                snap.totalPrincipal to snap.totalInterest
            } else {
                // 无快照：以该月月末为锚点推算（当前月用今天）
                val anchor = if (i == 0) today else target.withDayOfMonth(target.lengthOfMonth())
                deriveAt(active, anchor)
            }
            points += Pt(target, p, interest)
        }
        return formatTrimmed(points.map { TrendPointHolder(it.date, it.principal, it.interest) }, today, monthly = true)
    }

    /**
     * 按年趋势：窗口自当前年往前最长 maxYears 年；口径同上（快照年末/推算年末），
     * 从第一个资产大于 0 的年份开始，标签如 “2025年”。
     */
    fun yearlyTrendEx(
        deposits: List<Deposit>,
        history: List<AssetSnapshot> = emptyList(),
        maxYears: Int = 12,
        today: LocalDate = DateUtils.today(),
    ): List<TrendPoint> {
        val snapshots = history.groupBy { it.day.take(4) }
            .mapValues { (_, list) -> list.maxBy { it.day } }
        val active = deposits.filter { it.isActive }

        data class Pt(val date: LocalDate, val principal: Double, val interest: Double)
        val points = mutableListOf<Pt>()
        for (i in maxYears - 1 downTo 0) {
            val year = today.year - i
            val snap = snapshots[year.toString()]
            val (p, interest) = if (snap != null) {
                snap.totalPrincipal to snap.totalInterest
            } else {
                val anchor = if (i == 0) today else LocalDate.of(year, 12, 31)
                deriveAt(active, anchor)
            }
            points += Pt(LocalDate.of(year, 1, 1), p, interest)
        }
        return formatTrimmed(points.map { TrendPointHolder(it.date, it.principal, it.interest) }, today, monthly = false)
    }

    /** 中间结构：保留完整日期，便于裁剪后生成标签 */
    private data class TrendPointHolder(val date: LocalDate, val principal: Double, val interest: Double)

    /** 裁掉前导零后生成带标签的 TrendPoint 列表；无任何非零点则返回空 */
    private fun formatTrimmed(
        points: List<TrendPointHolder>,
        today: LocalDate,
        monthly: Boolean,
    ): List<TrendPoint> {
        val firstNonZero = points.indexOfFirst { it.principal + it.interest > 0.0 }
        if (firstNonZero < 0) return emptyList()
        val trimmed = points.drop(firstNonZero)
        if (monthly) {
            val spansYears = trimmed.any { it.date.year != today.year }
            return trimmed.map {
                val label = when {
                    spansYears && I18n.isZh -> "%02d年%d月".format(it.date.year % 100, it.date.monthValue)
                    spansYears -> "%d/%d".format(it.date.year % 100, it.date.monthValue)
                    I18n.isZh -> "%d月".format(it.date.monthValue)
                    else -> "%d".format(it.date.monthValue)
                }
                TrendPoint(
                    monthLabel = label,
                    principal = it.principal,
                    interest = it.interest,
                )
            }
        }
        return trimmed.map {
            TrendPoint(
                monthLabel = if (I18n.isZh) "%d年".format(it.date.year) else "%d".format(it.date.year),
                principal = it.principal,
                interest = it.interest,
            )
        }
    }
}
