package com.kingkharnivore.skillz.utils.time

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.utils.health.FlowActiveInterval
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StorySessionWindowTest {
    private val minute = 60_000L
    private val day = 24 * 60 * minute

    @Test fun storyProjectionDoesNotModifyOriginalHistoryOrLoseScoresAcrossDays() {
        val original = flow(day - minute, day + minute).copy(
            title = "Original title", description = "Original reflection 🌀",
            scyraPoints = 123, surgePoints = 45, arcBonusPoints = 67
        )
        val history = listOf(original)
        val before = history.map { it.copy() }
        val projected = StorySessionWindow.project(history, TimeWindow(0, day)) +
            StorySessionWindow.project(history, TimeWindow(day, 2 * day))
        assertEquals(before, history)
        assertEquals(original.durationMs, projected.sumOf { it.durationMs })
        assertEquals(original.scyraPoints, projected.sumOf { it.scyraPoints })
        assertEquals(original.surgePoints, projected.sumOf { it.surgePoints })
        assertEquals(original.arcBonusPoints, projected.sumOf { it.arcBonusPoints })
        projected.forEach {
            assertEquals(original.title, it.title)
            assertEquals(original.description, it.description)
            assertEquals(original.arcId, it.arcId)
        }
    }

    @Test fun continuousArcAcrossBothMidnightsCountsOnlyOneDay() {
        val flows = listOf(
            flow(day - 20 * minute, day + 20 * minute),
            flow(day + 20 * minute, 2 * day - 20 * minute),
            flow(2 * day - 20 * minute, 2 * day + 20 * minute)
        )
        val inDay = StorySessionWindow.project(flows, TimeWindow(day, 2 * day))
        assertEquals(day, inDay.sumOf { it.durationMs })
        assertEquals(listOf(20 * minute, day - 40 * minute, 20 * minute), inDay.map { it.durationMs })
        assertEquals(flows.sumOf { it.durationMs }, (0L..2L).sumOf { index ->
            StorySessionWindow.project(flows, TimeWindow(index * day, (index + 1) * day))
                .sumOf { it.durationMs }
        })
    }

    @Test fun midnightPauseDoesNotBecomeFlowTimeAndLateSaveDoesNotMoveTime() {
        val session = flow(day - 30 * minute, day + 90 * minute).copy(
            durationMs = 60 * minute,
            activeIntervalJson = FlowActiveIntervalCodec.encode(listOf(
                FlowActiveInterval(day - 30 * minute, day),
                FlowActiveInterval(day + 60 * minute, day + 90 * minute)
            )),
            createdAt = 3 * day
        )
        assertEquals(30 * minute, StorySessionWindow.durationMs(session, TimeWindow(0, day)))
        assertEquals(30 * minute, StorySessionWindow.durationMs(session, TimeWindow(day, 2 * day)))
        assertEquals(0L, StorySessionWindow.durationMs(session, TimeWindow(3 * day, 4 * day)))
    }

    @Test fun legacyPausedFlowIsProratedWithoutLosingOrInventingDuration() {
        val session = flow(day - 60 * minute, day + 60 * minute).copy(
            durationMs = 41 * minute + 1,
            activeIntervalJson = null
        )
        val before = StorySessionWindow.durationMs(session, TimeWindow(0, day))
        val after = StorySessionWindow.durationMs(session, TimeWindow(day, 2 * day))
        assertEquals(session.durationMs, before + after)
        assertEquals(session.durationMs / 2, before)
    }

    @Test fun boundariesAreHalfOpenAndScoresAreAwardedOnlyOnce() {
        val session = flow(day - minute, day).copy(scyraPoints = 5)
        val before = StorySessionWindow.project(listOf(session), TimeWindow(0, day)).single()
        val after = StorySessionWindow.project(listOf(session), TimeWindow(day, 2 * day)).single()
        assertEquals(minute, before.durationMs)
        assertEquals(0, before.scyraPoints)
        assertEquals(0L, after.durationMs)
        assertEquals(5, after.scyraPoints)
    }

    @Test fun calendarWindowsHonorDaylightSavingRatherThanHardCappingAt24Hours() {
        val zone = ZoneId.of("America/Chicago")
        listOf("2026-03-08" to 23L, "2026-11-01" to 25L).forEach { (dateText, hours) ->
            val date = LocalDate.parse(dateText)
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            assertEquals(hours * 60 * minute,
                StorySessionWindow.durationMs(flow(start - minute, end + minute), TimeWindow(start, end)))
        }
    }

    @Test fun weekAndMonthBoundariesUseTheSameClipping() {
        listOf(7 * day, 30 * day).forEach { length ->
            val session = flow(day - minute, day + length + minute)
            assertEquals(length, StorySessionWindow.durationMs(session, TimeWindow(day, day + length)))
        }
    }

    private fun flow(start: Long, end: Long) = SessionEntity(
        title = "Flow", description = "", tagId = 1, arcId = 42,
        startTime = start, endTime = end, durationMs = end - start, createdAt = end,
        activeIntervalJson = FlowActiveIntervalCodec.encode(listOf(FlowActiveInterval(start, end)))
    )
}
