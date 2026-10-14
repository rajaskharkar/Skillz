package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ObjectiveReminderPolicyTest {
    private val zone = ZoneId.of("America/Chicago")
    private val day = LocalDate.of(2026, 10, 13)
    private val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
    private val obj = ObjectiveEntity(id = 1, journeyId = 1, journeyNameSnapshot = "Practice", periodType = "daily",
        objectiveType = "recurring", targetDurationMs = 60_000, startAtMs = start, createdAt = start, updatedAt = start,
        targetMinuteOfDay = 9 * 60)
    private fun at(hour: Int, minute: Int = 0) = day.atTime(hour, minute).atZone(zone).toInstant()

    @Test fun schedulesExactlyThirtyMinutesBeforeTarget() {
        val plan = requireNotNull(nextObjectiveReminder(obj, at(8), zone, emptySet()))
        assertEquals(at(8, 30).toEpochMilli(), plan.remindAtMs)
        assertEquals(at(9).toEpochMilli(), plan.targetAtMs)
    }
    @Test fun missedReminderAdvancesRecurringCycleButNotOneTime() {
        val plan = requireNotNull(nextObjectiveReminder(obj, at(10), zone, emptySet()))
        assertEquals(day.plusDays(1).atTime(8, 30).atZone(zone).toInstant().toEpochMilli(), plan.remindAtMs)
        assertNull(nextObjectiveReminder(obj.copy(objectiveType = "one_time"), at(10), zone, emptySet()))
    }
    @Test fun completedAndSkippedCyclesAreBypassed() {
        val calculator = ObjectiveProgressCalculator()
        val first = calculator.windowFor(obj, at(8), zone)
        val second = calculator.windowFor(obj, Instant.ofEpochMilli(first.endMs), zone)
        val plan = requireNotNull(nextObjectiveReminder(obj, at(8), zone, setOf(first, second)))
        assertEquals(day.plusDays(2).atTime(8, 30).atZone(zone).toInstant().toEpochMilli(), plan.remindAtMs)
    }
    @Test fun untimedAndArchivedObjectivesHaveNoReminders() {
        assertNull(nextObjectiveReminder(obj.copy(targetMinuteOfDay = null), at(8), zone, emptySet()))
        assertNull(nextObjectiveReminder(obj.copy(isArchived = true), at(8), zone, emptySet()))
    }
    @Test fun midnightTargetRemindsOnPreviousDay() {
        val plan = requireNotNull(nextObjectiveReminder(obj.copy(targetMinuteOfDay = 10),
            day.minusDays(1).atTime(22, 0).atZone(zone).toInstant(), zone, emptySet()))
        assertEquals(day.minusDays(1).atTime(23, 40).atZone(zone).toInstant().toEpochMilli(), plan.remindAtMs)
    }

    @Test fun customLeadTimeAndAtTargetUseTheSameTarget() {
        val five = requireNotNull(nextObjectiveReminder(obj, at(8), zone, emptySet(), leadMs = 5 * 60_000))
        assertEquals(at(8, 55).toEpochMilli(), five.remindAtMs)
        val zero = requireNotNull(nextObjectiveReminder(obj, at(8), zone, emptySet(), leadMs = 0))
        assertEquals(at(9).toEpochMilli(), zero.remindAtMs)
    }
    @Test fun remindersLongerThanOneCycleContinueDailyWithoutSkipping() {
        val lead = 2 * 24 * 60 * 60_000L
        val first = requireNotNull(nextObjectiveReminder(obj, at(8), zone, emptySet(), leadMs = lead))
        assertEquals(at(9).toEpochMilli(), first.remindAtMs)
        assertEquals(day.plusDays(2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), first.targetAtMs)
        val next = requireNotNull(nextObjectiveReminder(obj, Instant.ofEpochMilli(first.remindAtMs + 1), zone, emptySet(), leadMs = lead))
        assertEquals(day.plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), next.remindAtMs)
    }
    @Test fun largeUserPreferenceDoesNotRequireIteratingEveryInterveningDay() {
        val lead = Int.MAX_VALUE.toLong() * 60_000L
        val plan = requireNotNull(nextObjectiveReminder(obj, at(8), zone, emptySet(), leadMs = lead))
        assertTrue(plan.remindAtMs > at(8).toEpochMilli())
        assertTrue(plan.remindAtMs - at(8).toEpochMilli() <= 25 * 60 * 60_000L)
    }
}
