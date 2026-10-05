package com.kingkharnivore.skillz.utils.shell.voyage

import com.kingkharnivore.skillz.model.FlowMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class PowerJourneyTrend(val journeyId: Long, val name: String?, val durationMs: Long, val share: Double, val months: Map<YearMonth, Double>)
data class PowerRangeStats(val durationMs: Long = 0, val sessions: Int = 0, val share: Double = 0.0, val journeys: List<PowerJourneyTrend> = emptyList())
data class PowerVoyageStats(val windows: Map<Int, PowerRangeStats> = emptyMap()) {
    val lifetime get() = windows[0] ?: PowerRangeStats()
}
object PowerVoyageCalculator {
    val windows = listOf(7, 30, 90, 0)
    fun calculate(sessions: List<VoyageSourceFlow>, now: Instant, zone: ZoneId): PowerVoyageStats {
        val eligible = sessions.filter { !it.isSoftMode && it.durationMs > 0 && it.endTime > 0 && it.endTime <= now.toEpochMilli() }
        fun share(flows: List<VoyageSourceFlow>): Double {
            val total = flows.sumOf { it.durationMs }
            return if(total == 0L) 0.0 else flows.filter { it.mode == FlowMode.POWER }.sumOf { it.durationMs }.toDouble() / total
        }
        return PowerVoyageStats(windows.associateWith { days ->
            val from = if(days == 0) Long.MIN_VALUE else now.atZone(zone).toLocalDate().minusDays(days.toLong() - 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val ranged = eligible.filter { it.endTime >= from }
            val power = ranged.filter { it.mode == FlowMode.POWER }
            PowerRangeStats(power.sumOf { it.durationMs }, power.size, share(ranged), ranged.groupBy { it.tagId }.map { (id, flows) ->
                PowerJourneyTrend(id, flows.last().tagName, flows.filter { it.mode == FlowMode.POWER }.sumOf { it.durationMs }, share(flows),
                    flows.groupBy { YearMonth.from(Instant.ofEpochMilli(it.endTime).atZone(zone)) }.toSortedMap().mapValues { share(it.value) })
            }.sortedWith(compareByDescending<PowerJourneyTrend> { it.durationMs }.thenBy { it.journeyId }))
        })
    }
}
