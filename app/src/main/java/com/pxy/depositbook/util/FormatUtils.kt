package com.pxy.depositbook.util

import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FormatUtils {

    private val money = DecimalFormat("#,##0.00")
    private val rate = DecimalFormat("0.00%")
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun money(v: Double): String = "¥" + money.format(v)

    fun moneyPlain(v: Double): String = money.format(v)

    /** 大额简写，如 1.2万、360万 */
    fun moneyCompact(v: Double): String = when {
        kotlin.math.abs(v) >= 100_000_000 -> money.format(v / 100_000_000) + "亿"
        kotlin.math.abs(v) >= 10_000 -> money.format(v / 10_000) + "万"
        else -> money.format(v)
    }

    fun rate(v: Double): String = rate.format(v)

    fun date(millis: Long): String = DateUtils.toDate(millis).format(dateFmt)

    fun date(date: LocalDate): String = date.format(dateFmt)

    fun dateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(DateUtils.zone).format(dateTimeFmt)
}
