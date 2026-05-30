package com.kingkharnivore.skillz.domain.voyage

import java.time.LocalDate

data class VoyageHallStats(
    val currentDailyStreak: StreakRecord?,
    val longestDailyStreak: StreakRecord?,
    val longestArcByTime: ArcRecord?,
    val highestArcMultiplier: MultiplierRecord?,
    val mostChainedFlowsInArc: ArcRecord?,
    val bestDayByPoints: PeriodPointsRecord?,
    val bestWeekByPoints: PeriodPointsRecord?,
    val bestMonthByPoints: PeriodPointsRecord?,
    val bestFlowByPoints: FlowRecord?,
    val longestFlow: FlowRecord?,
    val mostFlowsInDay: PeriodCountRecord?,
    val mostTimeInDay: PeriodDurationRecord?,
    val mostTimeInWeek: PeriodDurationRecord?,
    val mostTimeInMonth: PeriodDurationRecord?,
    val mostArcsInDay: PeriodCountRecord?,
    val mostArcsInWeek: PeriodCountRecord?,
    val hasEligibleFlows: Boolean
)

data class StreakRecord(
    val days: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

data class FlowRecord(
    val sessionId: Long,
    val title: String,
    val durationMs: Long,
    val points: Int,
    val completedAtMillis: Long
)

data class ArcRecord(
    val arcId: Long,
    val totalDurationMs: Long,
    val flowCount: Int,
    val latestFlowEndMillis: Long
)

data class MultiplierRecord(
    val arcId: Long,
    val multiplier: Double,
    val flowCount: Int,
    val reachedAtMillis: Long
)

data class PeriodPointsRecord(
    val points: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class PeriodDurationRecord(
    val durationMs: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class PeriodCountRecord(
    val count: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)
