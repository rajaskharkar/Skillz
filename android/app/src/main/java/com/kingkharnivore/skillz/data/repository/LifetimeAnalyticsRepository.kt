package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.domain.analytics.FlowObservation
import com.kingkharnivore.skillz.domain.analytics.LifetimeAnalytics
import com.kingkharnivore.skillz.domain.analytics.LifetimeStats
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/** Read-only analytics; Room remains the source of truth, including edits and deletions. */
class LifetimeAnalyticsRepository @Inject constructor(
    private val flows: FlowRepository,
    private val journeys: JourneyRepository
) {
    private data class CalendarContext(val today: LocalDate, val zone: ZoneId, val locale: Locale)

    fun observe(): Flow<LifetimeStats> {
        val calendar = flow {
            while (true) {
                val zone = ZoneId.systemDefault()
                emit(CalendarContext(LocalDate.now(zone), zone, Locale.getDefault()))
                delay(60_000)
            }
        }.distinctUntilChanged()
        return combine(flows.getAllSessions(), journeys.getAllTags(), calendar) { rows, tags, context ->
            LifetimeAnalytics.calculate(
                flows = rows.map { row ->
                    FlowObservation(row.id, row.tagId, row.mode, row.startTime, row.endTime,
                        row.createdAt, row.durationMs, FlowActiveIntervalCodec.decode(row.activeIntervalJson))
                },
                tagNames = tags.associate { it.id to it.name },
                today = context.today, zone = context.zone, locale = context.locale
            )
        }.flowOn(Dispatchers.Default).distinctUntilChanged()
    }
}
