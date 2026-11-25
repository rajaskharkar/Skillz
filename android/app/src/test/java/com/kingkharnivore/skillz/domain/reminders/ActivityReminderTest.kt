package com.kingkharnivore.skillz.domain.reminders

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ActivityReminderTest {
    private val utc = ZoneId.of("UTC")
    private fun next(settings: ActivityReminder, after: String, zone: ZoneId = utc) =
        nextActivityReminder(settings, Instant.parse(after), zone)?.toString()
    private val daily = ActivityReminder(enabled = true)

    @Test fun disabledAndInvalidSelectionsNeverSchedule() {
        assertNull(next(ActivityReminder(), "2026-01-01T00:00:00Z"))
        assertNull(next(daily.copy(minuteOfDay = 1440), "2026-01-01T00:00:00Z"))
        assertNull(next(daily.copy(frequency = ActivityReminder.WEEKLY, weekDays = 0), "2026-01-01T00:00:00Z"))
        assertNull(next(daily.copy(frequency = ActivityReminder.MONTHLY, monthDays = 0), "2026-01-01T00:00:00Z"))
    }

    @Test fun dailyUsesTodayOrTomorrowAndDoesNotRepeatAtTheSameInstant() {
        assertEquals("2026-11-16T09:00:00Z", next(daily, "2026-11-16T08:59:59Z"))
        assertEquals("2026-11-17T09:00:00Z", next(daily, "2026-11-16T09:00:00Z"))
    }

    @Test fun weeklySupportsSeveralDaysAndWrapsToNextWeek() {
        val weekly = daily.copy(frequency = ActivityReminder.WEEKLY, weekDays = (1 shl 0) or (1 shl 4))
        assertEquals("2026-11-20T09:00:00Z", next(weekly, "2026-11-16T09:00:00Z"))
        assertEquals("2026-11-23T09:00:00Z", next(weekly, "2026-11-20T09:00:00Z"))
    }

    @Test fun monthlySupportsFirstLastAndSeveralCustomDatesWithoutDuplicates() {
        val monthly = daily.copy(frequency = ActivityReminder.MONTHLY, monthDays = 1L or (1L shl 1) or (1L shl 15) or (1L shl 31))
        assertEquals("2026-01-15T09:00:00Z", next(monthly, "2026-01-01T09:00:00Z"))
        assertEquals("2026-01-31T09:00:00Z", next(monthly, "2026-01-15T09:00:00Z"))
        assertEquals("2026-02-01T09:00:00Z", next(monthly, "2026-01-31T09:00:00Z"))
        assertEquals("2026-02-28T09:00:00Z", next(monthly, "2026-02-15T09:00:00Z"))
    }

    @Test fun customDatesSkipShortMonthsAndLastDayHandlesLeapYears() {
        val monthly = daily.copy(frequency = ActivityReminder.MONTHLY, monthDays = 1L shl 31)
        assertEquals("2026-03-31T09:00:00Z", next(monthly, "2026-01-31T09:00:00Z"))
        assertEquals("2028-02-29T09:00:00Z", next(monthly.copy(monthDays = 1), "2028-02-01T00:00:00Z"))
        assertEquals("2027-01-31T09:00:00Z", next(monthly, "2026-12-31T09:00:00Z"))
    }

    @Test fun usesLocalTimeAndHandlesDstGapsAndOverlapsOnce() {
        val chicago = ZoneId.of("America/Chicago")
        assertEquals("2026-11-16T15:00:00Z", next(daily, "2026-11-16T14:00:00Z", chicago))
        assertEquals("2026-03-08T08:30:00Z", next(daily.copy(minuteOfDay = 150), "2026-03-08T06:00:00Z", chicago))
        assertEquals("2026-11-01T06:30:00Z", next(daily.copy(minuteOfDay = 90), "2026-11-01T05:00:00Z", chicago))
        assertEquals("2026-11-02T07:30:00Z", next(daily.copy(minuteOfDay = 90), "2026-11-01T06:30:00Z", chicago))
    }
    @Test fun oneTimeUsesItsExactDateAndNeverRollsForward() {
        val once = daily.copy(frequency = ActivityReminder.ONE_TIME,
            oneTimeDateEpochDay = java.time.LocalDate.of(2027, 12, 25).toEpochDay())
        assertEquals("2027-12-25T09:00:00Z", next(once, "2026-11-16T00:00:00Z"))
        assertEquals("2027-12-25T09:00:00Z", next(once, "2027-12-25T08:59:59Z"))
        assertNull(next(once, "2027-12-25T09:00:00Z"))
        assertNull(next(once, "2027-12-26T00:00:00Z"))
        assertNull(next(once.copy(enabled = false), "2026-11-16T00:00:00Z"))
        assertNull(next(once.copy(oneTimeDateEpochDay = null), "2026-11-16T00:00:00Z"))
    }

    @Test fun consumedOneTimeDoesNotRearmAfterClockOrTimeZoneChanges() {
        val once = daily.copy(frequency = ActivityReminder.ONE_TIME,
            oneTimeDateEpochDay = java.time.LocalDate.of(2026, 11, 16).toEpochDay(), oneTimeConsumedAt = 1234)
        assertNull(next(once, "2026-11-15T00:00:00Z"))
        assertNull(next(once, "2026-11-15T00:00:00Z", ZoneId.of("America/Chicago")))
    }

    @Test fun newOrReenabledOneTimeMustBeFutureButUnchangedPastCanBeRetained() {
        val now = Instant.parse("2026-11-16T10:00:00Z")
        val past = daily.copy(frequency = ActivityReminder.ONE_TIME,
            oneTimeDateEpochDay = java.time.LocalDate.of(2026, 11, 16).toEpochDay())
        assertFalse(past.canSave(now = now, zone = utc))
        assertFalse(past.canSave(past.copy(enabled = false), now, utc))
        assertTrue(past.canSave(past, now, utc))
        assertTrue(past.copy(enabled = false).canSave(now = now, zone = utc))
        assertTrue(past.copy(oneTimeDateEpochDay = past.oneTimeDateEpochDay!! + 1).canSave(now = now, zone = utc))
        assertFalse(past.copy(minuteOfDay = 600).canSave(now = now, zone = utc))
    }

    @Test fun oneTimeUsesLocalDateAndResolvesDstGap() {
        val once = daily.copy(frequency = ActivityReminder.ONE_TIME, minuteOfDay = 150,
            oneTimeDateEpochDay = java.time.LocalDate.of(2026, 3, 8).toEpochDay())
        assertEquals("2026-03-08T08:30:00Z", next(once, "2026-03-08T00:00:00Z", ZoneId.of("America/Chicago")))
    }

}
