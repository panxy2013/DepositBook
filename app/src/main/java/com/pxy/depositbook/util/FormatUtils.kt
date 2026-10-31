package com.pxy.depositbook.util

import com.pxy.depositbook.util.I18n
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FormatUtils {

    private val money = DecimalFormat("#,##0").apply { groupingSize = 4 }
    private val rate = DecimalFormat("0.00%")
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 金额显示：4 位分段（万进位习惯），只显示整数部分；小数只存储不显示 */
    fun money(v: Double): String = "¥" + money.format(v)

    fun moneyPlain(v: Double): String = money.format(v)

    /** 大额简写，如 1.2万、360万；英文版用 k / M */
    fun moneyCompact(v: Double): String {
        val a = kotlin.math.abs(v)
        return if (I18n.isZh) when {
            a >= 100_000_000 -> money.format(v / 100_000_000) + "亿"
            a >= 10_000 -> money.format(v / 10_000) + "万"
            else -> money.format(v)
        } else when {
            a >= 1_000_000 -> money.format(v / 1_000_000) + "M"
            a >= 1_000 -> money.format(v / 1_000) + "k"
            else -> money.format(v)
        }
    }

    fun rate(v: Double): String = rate.format(v)

    fun date(millis: Long): String = DateUtils.toDate(millis).format(dateFmt)

    fun date(date: LocalDate): String = date.format(dateFmt)

    fun dateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(DateUtils.zone).format(dateTimeFmt)
}
