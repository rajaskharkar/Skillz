package com.kingkharnivore.skillz.utils.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

private val zone: ZoneId = ZoneId.systemDefault()

fun floorToDay(ms: Long): Long {
    val ld = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return ld.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun dayStartPlusDays(dayStartMs: Long, deltaDays: Long): Long {
    val ld = Instant.ofEpochMilli(dayStartMs).atZone(zone).toLocalDate().plusDays(deltaDays)
    return ld.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun mondayWeekStart(dayStartMs: Long): Long {
    val ld = Instant.ofEpochMilli(dayStartMs).atZone(zone).toLocalDate()
    val mon = ld.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    return mon.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun yearMonthStartMs(ym: YearMonth): Long {
    val ld = LocalDate.of(ym.year, ym.month, 1)
    return ld.atStartOfDay(zone).toInstant().toEpochMilli()
}

fun yearMonthEndMsExclusive(ym: YearMonth): Long {
    val next = ym.plusMonths(1)
    return yearMonthStartMs(next)
}

fun msToMinuteOfDay(ms: Long, dayStartMs: Long): Int {
    return ((ms - dayStartMs) / 60_000L).toInt()
}

fun clampToDay(ms: Long, dayStartMs: Long): Long {
    val dayEnd = dayStartMs + 24L * 60L * 60L * 1000L
    return ms.coerceIn(dayStartMs, dayEnd)
}

fun ceilDiv(a: Int, b: Int): Int = ((a + b - 1) / b)