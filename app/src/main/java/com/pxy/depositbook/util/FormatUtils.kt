package com.pxy.depositbook.util

import com.pxy.depositbook.util.I18n

import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object FormatUtils {

    // 四舍五入统一用 HALF_UP（DecimalFormat 默认是 HALF_EVEN，需显式指定）
    private val money = DecimalFormat("#,##0").apply {
        groupingSize = 4
        roundingMode = RoundingMode.HALF_UP
    }

    // 完整显示带小数的金额（最多 2 位，即到「分」；不截断整数部分）
    private val moneyFull = DecimalFormat("#,##0.##").apply {
        groupingSize = 4
        roundingMode = RoundingMode.HALF_UP
    }

    private val rate = DecimalFormat("0.00%").apply { roundingMode = RoundingMode.HALF_UP }
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 金额显示：4 位分段（万进位习惯）。
     *  四舍五入到整数只用于不足 1 元的金额（如 0.6→1）；1 元及以上完整显示，有小数保留到分 */
    fun money(v: Double): String = "¥" + moneyPlain(v)

    fun moneyPlain(v: Double): String {
        val a = abs(v)
        return when {
            a < 1 -> money.format(v)          // 不足 1 元：四舍五入
            a % 1.0 == 0.0 -> money.format(v)  // 整数金额：完整显示
            else -> moneyFull.format(v)       // 带小数：完整显示到分
        }
    }

    /**
     * 金额简写已取消：一律完整显示实际金额（4 位分段），如 80000 → 8,0000、30001 → 3,0001。
     * 不再显示 3万 / 80k 等缩写，避免任何歧义。
     */
    fun moneyCompact(v: Double): String = moneyPlain(v)

    fun rate(v: Double): String = rate.format(v)

    fun date(millis: Long): String = DateUtils.toDate(millis).format(dateFmt)

    fun date(date: LocalDate): String = date.format(dateFmt)

    fun dateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(DateUtils.zone).format(dateTimeFmt)
}
