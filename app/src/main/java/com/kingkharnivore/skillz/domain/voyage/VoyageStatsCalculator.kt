package com.kingkharnivore.skillz.domain.voyage

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import javax.inject.Inject

class VoyageStatsCalculator @Inject constructor() {

    fun calculate(
        sessions: List<SessionEntity>,
        now: Instant,
        zoneId: ZoneId
    ): VoyageHallStats {
        val eligible = sessions
            .filter { it.isEligibleForVoyageHall() }
            .map { it.toEligibleFlow(zoneId) }

        if (eligible.isEmpty()) {
            return emptyStats(hasEligibleFlows = false)
        }

        val dates = eligible.map { it.completedDate }.toSet()
        val validArcGroups = eligible
            .filter { it.session.arcId != null }
            .groupBy { it.session.arcId!! }
            .filterValues { it.size >= 2 }

        return VoyageHallStats(
            currentDailyStreak = currentDailyStreak(dates, now, zoneId),
            longestDailyStreak = longestDailyStreak(dates),
            longestArcByTime = longestArcByTime(validArcGroups),
            highestArcMultiplier = highestArcMultiplier(validArcGroups),
            mostChainedFlowsInArc = mostChainedFlowsInArc(validArcGroups),
            bestDayByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.day(it.completedDate) }),
            bestWeekByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.week(it.completedDate) }),
            bestMonthByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.month(it.completedDate) }),
            bestFlowByPoints = bestFlowByPoints(eligible),
            longestFlow = longestFlow(eligible),
            mostFlowsInDay = bestPeriodByCount(eligible.groupByPeriod { Period.day(it.completedDate) }),
            mostTimeInDay = bestPeriodByDuration(eligible.groupByPeriod { Period.day(it.completedDate) }),
            mostTimeInWeek = bestPeriodByDuration(eligible.groupByPeriod { Period.week(it.completedDate) }),
            mostTimeInMonth = bestPeriodByDuration(eligible.groupByPeriod { Period.month(it.completedDate) }),
            mostArcsInDay = mostArcsByPeriod(validArcGroups) { Period.day(it) },
            mostArcsInWeek = mostArcsByPeriod(validArcGroups) { Period.week(it) },
            hasEligibleFlows = true
        )
    }

    private fun emptyStats(hasEligibleFlows: Boolean) = VoyageHallStats(
        currentDailyStreak = null,
        longestDailyStreak = null,
        longestArcByTime = null,
        highestArcMultiplier = null,
        mostChainedFlowsInArc = null,
        bestDayByPoints = null,
        bestWeekByPoints = null,
        bestMonthByPoints = null,
        bestFlowByPoints = null,
        longestFlow = null,
        mostFlowsInDay = null,
        mostTimeInDay = null,
        mostTimeInWeek = null,
        mostTimeInMonth = null,
        mostArcsInDay = null,
        mostArcsInWeek = null,
        hasEligibleFlows = hasEligibleFlows
    )

    private fun SessionEntity.isEligibleForVoyageHall(): Boolean =
        endTime > 0L && durationMs > 0L && !isSoftMode

    private fun SessionEntity.toEligibleFlow(zoneId: ZoneId) = EligibleFlow(
        session = this,
        completedDate = Instant.ofEpochMilli(endTime).atZone(zoneId).toLocalDate()
    )

    private fun currentDailyStreak(
        dates: Set<LocalDate>,
        now: Instant,
        zoneId: ZoneId
    ): StreakRecord? {
        val today = now.atZone(zoneId).toLocalDate()
        val latest = dates.maxOrNull() ?: return null
        if (latest != today && latest != today.minusDays(1)) return null

        var cursor = latest
        var count = 0
        while (dates.contains(cursor)) {
            count += 1
            cursor = cursor.minusDays(1)
        }
        return StreakRecord(count, cursor.plusDays(1), latest).takeIf { it.days > 0 }
    }

    private fun longestDailyStreak(dates: Set<LocalDate>): StreakRecord? {
        if (dates.isEmpty()) return null
        val sorted = dates.sorted()
        var bestStart = sorted.first()
        var bestEnd = sorted.first()
        var runStart = sorted.first()
        var previous = sorted.first()

        for (date in sorted.drop(1)) {
            if (date == previous.plusDays(1)) {
                previous = date
            } else {
                if (isBetterStreak(runStart, previous, bestStart, bestEnd)) {
                    bestStart = runStart
                    bestEnd = previous
                }
                runStart = date
                previous = date
            }
        }
        if (isBetterStreak(runStart, previous, bestStart, bestEnd)) {
            bestStart = runStart
            bestEnd = previous
        }
        return StreakRecord((bestEnd.toEpochDay() - bestStart.toEpochDay() + 1).toInt(), bestStart, bestEnd)
    }

    private fun isBetterStreak(start: LocalDate, end: LocalDate, bestStart: LocalDate, bestEnd: LocalDate): Boolean {
        val length = end.toEpochDay() - start.toEpochDay() + 1
        val bestLength = bestEnd.toEpochDay() - bestStart.toEpochDay() + 1
        return length > bestLength || (length == bestLength && end > bestEnd)
    }

    private fun longestArcByTime(groups: Map<Long, List<EligibleFlow>>): ArcRecord? = groups
        .map { (arcId, flows) -> arcRecord(arcId, flows) }
        .maxWithOrNull(
            compareBy<ArcRecord> { it.totalDurationMs }
                .thenBy { it.latestFlowEndMillis }
                .thenByDescending { -it.arcId }
        )

    private fun mostChainedFlowsInArc(groups: Map<Long, List<EligibleFlow>>): ArcRecord? = groups
        .map { (arcId, flows) -> arcRecord(arcId, flows) }
        .maxWithOrNull(
            compareBy<ArcRecord> { it.flowCount }
                .thenBy { it.latestFlowEndMillis }
                .thenByDescending { -it.arcId }
        )

    private fun highestArcMultiplier(groups: Map<Long, List<EligibleFlow>>): MultiplierRecord? = groups
        .flatMap { (arcId, flows) ->
            val flowCount = flows.size
            flows.mapNotNull { flow ->
                flow.session.arcMultiplierUsed?.let { multiplier ->
                    MultiplierRecord(
                        arcId = arcId,
                        multiplier = multiplier,
                        flowCount = flowCount,
                        reachedAtMillis = flow.session.endTime
                    )
                }
            }
        }
        .maxWithOrNull(
            compareBy<MultiplierRecord> { it.multiplier }
                .thenBy { it.reachedAtMillis }
                .thenByDescending { -it.arcId }
        )

    private fun arcRecord(arcId: Long, flows: List<EligibleFlow>) = ArcRecord(
        arcId = arcId,
        totalDurationMs = flows.sumOf { it.session.durationMs },
        flowCount = flows.size,
        latestFlowEndMillis = flows.maxOf { it.session.endTime }
    )

    private fun bestFlowByPoints(flows: List<EligibleFlow>): FlowRecord? = flows
        .maxWithOrNull(
            compareBy<EligibleFlow> { it.session.scyraPoints }
                .thenBy { it.session.endTime }
                .thenByDescending { -it.session.id }
        )
        ?.toFlowRecord()

    private fun longestFlow(flows: List<EligibleFlow>): FlowRecord? = flows
        .maxWithOrNull(
            compareBy<EligibleFlow> { it.session.durationMs }
                .thenBy { it.session.endTime }
                .thenByDescending { -it.session.id }
        )
        ?.toFlowRecord()

    private fun bestPeriodByPoints(groups: Map<Period, List<EligibleFlow>>): PeriodPointsRecord? = groups
        .map { (period, flows) -> PeriodPointsRecord(flows.sumOf { it.session.scyraPoints }, period.start, period.end) }
        .maxWithOrNull(compareBy<PeriodPointsRecord> { it.points }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun bestPeriodByDuration(groups: Map<Period, List<EligibleFlow>>): PeriodDurationRecord? = groups
        .map { (period, flows) -> PeriodDurationRecord(flows.sumOf { it.session.durationMs }, period.start, period.end) }
        .maxWithOrNull(compareBy<PeriodDurationRecord> { it.durationMs }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun bestPeriodByCount(groups: Map<Period, List<EligibleFlow>>): PeriodCountRecord? = groups
        .map { (period, flows) -> PeriodCountRecord(flows.size, period.start, period.end) }
        .maxWithOrNull(compareBy<PeriodCountRecord> { it.count }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun mostArcsByPeriod(
        groups: Map<Long, List<EligibleFlow>>,
        periodForDate: (LocalDate) -> Period
    ): PeriodCountRecord? = groups
        .map { (_, flows) -> flows.maxBy { it.session.endTime }.completedDate }
        .groupBy(periodForDate)
        .map { (period, arcs) -> PeriodCountRecord(arcs.size, period.start, period.end) }
        .maxWithOrNull(compareBy<PeriodCountRecord> { it.count }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun EligibleFlow.toFlowRecord() = FlowRecord(
        sessionId = session.id,
        title = session.title,
        durationMs = session.durationMs,
        points = session.scyraPoints,
        completedAtMillis = session.endTime
    )

    private fun <K> List<EligibleFlow>.groupByPeriod(selector: (EligibleFlow) -> K): Map<K, List<EligibleFlow>> =
        groupBy(selector)

    private data class EligibleFlow(
        val session: SessionEntity,
        val completedDate: LocalDate
    )

    private data class Period(
        val start: LocalDate,
        val end: LocalDate
    ) {
        companion object {
            fun day(date: LocalDate) = Period(date, date)

            fun week(date: LocalDate): Period {
                val start = date.with(WeekFields.ISO.dayOfWeek(), 1)
                return Period(start, start.plusDays(6))
            }

            fun month(date: LocalDate): Period {
                val month = YearMonth.from(date)
                return Period(month.atDay(1), month.atEndOfMonth())
            }
        }
    }
}
