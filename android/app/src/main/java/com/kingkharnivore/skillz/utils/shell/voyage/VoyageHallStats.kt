package com.kingkharnivore.skillz.utils.shell.voyage

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
    val bestFlowByPoints: VoyageFlowSummary?,
    val longestFlow: VoyageFlowSummary?,
    val mostFlowsInDay: PeriodCountRecord?,
    val mostTimeInDay: PeriodDurationRecord?,
    val mostTimeInWeek: PeriodDurationRecord?,
    val mostTimeInMonth: PeriodDurationRecord?,
    val mostArcsInDay: PeriodCountRecord?,
    val mostArcsInWeek: PeriodCountRecord?,
    val hasEligibleFlows: Boolean
)

data class VoyageSourceFlow(
    val id: Long,
    val title: String,
    val tagName: String?,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val scyraPoints: Int,
    val isSoftMode: Boolean,
    val arcId: Long?,
    val arcIndex: Int?,
    val arcMultiplierUsed: Double?,
    val arcBonusPoints: Int = 0
)

data class VoyageFlowSummary(
    val sessionId: Long,
    val title: String,
    val tagName: String?,
    val durationMs: Long,
    val points: Int,
    val completedAtMillis: Long,
    val arcId: Long?,
    val arcIndex: Int?,
    val arcMultiplierUsed: Double?
)

data class VoyageArcSummary(
    val arcId: Long,
    val flowCount: Int,
    val regularFlowCount: Int,
    val softFlowCount: Int,
    val totalDurationMs: Long,
    val totalPoints: Int,
    val totalArcBonusPoints: Int,
    val peakMultiplier: Double?,
    val latestFlowEndMillis: Long,
    val flows: List<VoyageFlowSummary>
)

data class StreakRecord(
    val days: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

data class ArcRecord(
    val arcId: Long,
    val totalDurationMs: Long,
    val flowCount: Int,
    val totalPoints: Int,
    val peakMultiplier: Double?,
    val latestFlowEndMillis: Long,
    val flows: List<VoyageFlowSummary>
)

data class MultiplierRecord(
    val arcId: Long,
    val multiplier: Double,
    val flowCount: Int,
    val reachedAtMillis: Long,
    val reachedInSessionId: Long?,
    val totalDurationMs: Long,
    val totalPoints: Int,
    val flows: List<VoyageFlowSummary>
)

data class PeriodPointsRecord(
    val points: Int,
    val totalDurationMs: Long,
    val flowCount: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val flows: List<VoyageFlowSummary>
)

data class PeriodDurationRecord(
    val durationMs: Long,
    val points: Int,
    val flowCount: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val flows: List<VoyageFlowSummary>
)

data class PeriodCountRecord(
    val count: Int,
    val totalDurationMs: Long,
    val points: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val flows: List<VoyageFlowSummary> = emptyList(),
    val arcs: List<VoyageArcSummary> = emptyList()
)
