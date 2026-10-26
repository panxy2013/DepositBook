package com.pxy.depositbook.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 日期工具：App 内部统一用 epoch millis 存储，按“当地日期零点”比较天数。
 */
object DateUtils {

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now()

    /** LocalDate -> 当天零点的 epoch millis */
    fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** 两个时间点相差的天数（b - a） */
    fun daysBetween(fromMillis: Long, toMillis: Long): Long =
        ChronoUnit.DAYS.between(toDate(fromMillis), toDate(toMillis))

    /** 从存入日到今天的已存天数，最小为 0 */
    fun daysPassed(startMillis: Long, today: LocalDate = today()): Long =
        ChronoUnit.DAYS.between(toDate(startMillis), today).coerceAtLeast(0)

    /** 距到期还剩多少天，可为负（已过期） */
    fun daysLeft(endMillis: Long, today: LocalDate = today()): Long =
        ChronoUnit.DAYS.between(today, toDate(endMillis))

    fun isExpired(endMillis: Long, today: LocalDate = today()): Boolean =
        daysLeft(endMillis, today) < 0

    fun isWithinDays(endMillis: Long, days: Long, today: LocalDate = today()): Boolean {
        val left = daysLeft(endMillis, today)
        return left in 0..days
    }
}
