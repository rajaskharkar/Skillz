package com.kingkharnivore.skillz.utils.shell.voyage

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import javax.inject.Inject

class VoyageStatsCalculator @Inject constructor() {

    fun calculate(
        sessions: List<VoyageSourceFlow>,
        now: Instant,
        zoneId: ZoneId
    ): VoyageHallStats {
        val eligible = sessions
            .filter { it.isEligibleForVoyageHall() }
            .map { it.toEligibleFlow(zoneId) }

        if (eligible.isEmpty()) return emptyStats(hasEligibleFlows = false)

        val dates = eligible.map { it.completedDate }.toSet()
        val validArcGroups = eligible
            .filter { it.flow.arcId != null }
            .groupBy { it.flow.arcId!! }
            .filterValues { it.size >= 2 }
        val arcSummaries = validArcGroups.mapValues { (arcId, flows) -> arcSummary(arcId, flows) }

        return VoyageHallStats(
            currentDailyStreak = currentDailyStreak(dates, now, zoneId),
            longestDailyStreak = longestDailyStreak(dates),
            longestArcByTime = longestArcByTime(arcSummaries.values),
            highestArcMultiplier = highestArcMultiplier(arcSummaries.values),
            mostChainedFlowsInArc = mostChainedFlowsInArc(arcSummaries.values),
            bestDayByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.day(it.completedDate) }),
            bestWeekByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.week(it.completedDate) }),
            bestMonthByPoints = bestPeriodByPoints(eligible.groupByPeriod { Period.month(it.completedDate) }),
            bestFlowByPoints = bestFlowByPoints(eligible),
            longestFlow = longestFlow(eligible),
            mostFlowsInDay = bestPeriodByCount(eligible.groupByPeriod { Period.day(it.completedDate) }),
            mostTimeInDay = bestPeriodByDuration(eligible.groupByPeriod { Period.day(it.completedDate) }),
            mostTimeInWeek = bestPeriodByDuration(eligible.groupByPeriod { Period.week(it.completedDate) }),
            mostTimeInMonth = bestPeriodByDuration(eligible.groupByPeriod { Period.month(it.completedDate) }),
            mostArcsInDay = mostArcsByPeriod(arcSummaries.values, zoneId) { Period.day(it) },
            mostArcsInWeek = mostArcsByPeriod(arcSummaries.values, zoneId) { Period.week(it) },
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

    private fun VoyageSourceFlow.isEligibleForVoyageHall(): Boolean =
        endTime > 0L && durationMs > 0L && !isSoftMode

    private fun VoyageSourceFlow.toEligibleFlow(zoneId: ZoneId) = EligibleFlow(
        flow = this,
        completedDate = Instant.ofEpochMilli(endTime).atZone(zoneId).toLocalDate(),
        summary = toSummary()
    )

    private fun VoyageSourceFlow.toSummary() = VoyageFlowSummary(
        sessionId = id,
        title = title,
        tagName = tagName,
        durationMs = durationMs,
        points = scyraPoints,
        completedAtMillis = endTime,
        arcId = arcId,
        arcIndex = arcIndex,
        arcMultiplierUsed = arcMultiplierUsed
    )

    private fun currentDailyStreak(dates: Set<LocalDate>, now: Instant, zoneId: ZoneId): StreakRecord? {
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

    private fun longestArcByTime(arcs: Collection<VoyageArcSummary>): ArcRecord? = arcs
        .maxWithOrNull(compareBy<VoyageArcSummary> { it.totalDurationMs }.thenBy { it.latestFlowEndMillis }.thenByDescending { -it.arcId })
        ?.toArcRecord()

    private fun mostChainedFlowsInArc(arcs: Collection<VoyageArcSummary>): ArcRecord? = arcs
        .maxWithOrNull(compareBy<VoyageArcSummary> { it.flowCount }.thenBy { it.latestFlowEndMillis }.thenByDescending { -it.arcId })
        ?.toArcRecord()

    private fun highestArcMultiplier(arcs: Collection<VoyageArcSummary>): MultiplierRecord? = arcs
        .flatMap { arc ->
            arc.flows.mapNotNull { flow ->
                flow.arcMultiplierUsed?.let { multiplier ->
                    MultiplierCandidate(arc, flow, multiplier)
                }
            }
        }
        .maxWithOrNull(
            compareBy<MultiplierCandidate> { it.multiplier }
                .thenBy { it.flow.completedAtMillis }
                .thenByDescending { -it.flow.sessionId }
        )
        ?.let { candidate ->
            MultiplierRecord(
                arcId = candidate.arc.arcId,
                multiplier = candidate.multiplier,
                flowCount = candidate.arc.flowCount,
                reachedAtMillis = candidate.flow.completedAtMillis,
                reachedInSessionId = candidate.flow.sessionId,
                totalDurationMs = candidate.arc.totalDurationMs,
                totalPoints = candidate.arc.totalPoints,
                flows = candidate.arc.flows
            )
        }

    private fun arcSummary(arcId: Long, flows: List<EligibleFlow>): VoyageArcSummary {
        val summaries = flows.map { it.summary }.sortedBy { it.completedAtMillis }
        return VoyageArcSummary(
            arcId = arcId,
            flowCount = summaries.size,
            totalDurationMs = summaries.sumOf { it.durationMs },
            totalPoints = summaries.sumOf { it.points },
            peakMultiplier = summaries.mapNotNull { it.arcMultiplierUsed }.maxOrNull(),
            latestFlowEndMillis = summaries.maxOf { it.completedAtMillis },
            flows = summaries
        )
    }

    private fun VoyageArcSummary.toArcRecord() = ArcRecord(
        arcId = arcId,
        totalDurationMs = totalDurationMs,
        flowCount = flowCount,
        totalPoints = totalPoints,
        peakMultiplier = peakMultiplier,
        latestFlowEndMillis = latestFlowEndMillis,
        flows = flows
    )

    private fun bestFlowByPoints(flows: List<EligibleFlow>): VoyageFlowSummary? = flows
        .maxWithOrNull(compareBy<EligibleFlow> { it.flow.scyraPoints }.thenBy { it.flow.endTime }.thenByDescending { -it.flow.id })
        ?.summary

    private fun longestFlow(flows: List<EligibleFlow>): VoyageFlowSummary? = flows
        .maxWithOrNull(compareBy<EligibleFlow> { it.flow.durationMs }.thenBy { it.flow.endTime }.thenByDescending { -it.flow.id })
        ?.summary

    private fun bestPeriodByPoints(groups: Map<Period, List<EligibleFlow>>): PeriodPointsRecord? = groups
        .map { (period, flows) ->
            val summaries = flows.toChronologicalSummaries()
            PeriodPointsRecord(
                points = summaries.sumOf { it.points },
                totalDurationMs = summaries.sumOf { it.durationMs },
                flowCount = summaries.size,
                startDate = period.start,
                endDate = period.end,
                flows = summaries
            )
        }
        .maxWithOrNull(compareBy<PeriodPointsRecord> { it.points }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun bestPeriodByDuration(groups: Map<Period, List<EligibleFlow>>): PeriodDurationRecord? = groups
        .map { (period, flows) ->
            val summaries = flows.toChronologicalSummaries()
            PeriodDurationRecord(
                durationMs = summaries.sumOf { it.durationMs },
                points = summaries.sumOf { it.points },
                flowCount = summaries.size,
                startDate = period.start,
                endDate = period.end,
                flows = summaries
            )
        }
        .maxWithOrNull(compareBy<PeriodDurationRecord> { it.durationMs }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun bestPeriodByCount(groups: Map<Period, List<EligibleFlow>>): PeriodCountRecord? = groups
        .map { (period, flows) ->
            val summaries = flows.toChronologicalSummaries()
            PeriodCountRecord(
                count = summaries.size,
                totalDurationMs = summaries.sumOf { it.durationMs },
                points = summaries.sumOf { it.points },
                startDate = period.start,
                endDate = period.end,
                flows = summaries
            )
        }
        .maxWithOrNull(compareBy<PeriodCountRecord> { it.count }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun mostArcsByPeriod(
        arcs: Collection<VoyageArcSummary>,
        zoneId: ZoneId,
        periodForDate: (LocalDate) -> Period
    ): PeriodCountRecord? = arcs
        .groupBy { arc -> periodForDate(Instant.ofEpochMilli(arc.latestFlowEndMillis).atZone(zoneId).toLocalDate()) }
        .map { (period, periodArcs) ->
            val sortedArcs = periodArcs.sortedWith(compareBy<VoyageArcSummary> { it.latestFlowEndMillis }.thenBy { it.arcId })
            PeriodCountRecord(
                count = sortedArcs.size,
                totalDurationMs = sortedArcs.sumOf { it.totalDurationMs },
                points = sortedArcs.sumOf { it.totalPoints },
                startDate = period.start,
                endDate = period.end,
                arcs = sortedArcs
            )
        }
        .maxWithOrNull(compareBy<PeriodCountRecord> { it.count }.thenBy { it.endDate }.thenBy { it.startDate })

    private fun List<EligibleFlow>.toChronologicalSummaries(): List<VoyageFlowSummary> =
        map { it.summary }.sortedBy { it.completedAtMillis }

    private fun <K> List<EligibleFlow>.groupByPeriod(selector: (EligibleFlow) -> K): Map<K, List<EligibleFlow>> = groupBy(selector)

    private data class EligibleFlow(
        val flow: VoyageSourceFlow,
        val completedDate: LocalDate,
        val summary: VoyageFlowSummary
    )

    private data class MultiplierCandidate(
        val arc: VoyageArcSummary,
        val flow: VoyageFlowSummary,
        val multiplier: Double
    )

    private data class Period(val start: LocalDate, val end: LocalDate) {
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
