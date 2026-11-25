package com.kingkharnivore.skillz.domain.reminders

import androidx.room.ColumnInfo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Week bits are ISO Monday=0 through Sunday=6. Month bits are dates 1..31, with bit 0 for last day. */
data class ActivityReminder(
    @ColumnInfo(defaultValue = "0") val enabled: Boolean = false,
    @ColumnInfo(defaultValue = "'DAILY'") val frequency: String = DAILY,
    @ColumnInfo(defaultValue = "540") val minuteOfDay: Int = 9 * 60,
    @ColumnInfo(defaultValue = "127") val weekDays: Int = 127,
    @ColumnInfo(defaultValue = "2") val monthDays: Long = 2L,
    val oneTimeDateEpochDay: Long? = null,
    val oneTimeConsumedAt: Long? = null
) {
    fun isValid(): Boolean = minuteOfDay in 0..1439 && when (frequency) {
        ONE_TIME -> oneTimeInstant(ZoneId.systemDefault()) != null
        DAILY -> true
        WEEKLY -> weekDays and 127 != 0
        MONTHLY -> monthDays and 0xFFFFFFFFL != 0L
        else -> false
    }

    fun oneTimeInstant(zone: ZoneId): Instant? = oneTimeDateEpochDay?.let { day ->
        runCatching { LocalDate.ofEpochDay(day).atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)).atZone(zone).toInstant() }.getOrNull()
    }

    /** An unchanged past reminder may be retained when editing the activity title or other details. */
    fun canSave(previous: ActivityReminder? = null, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        !enabled || (isValid() && (frequency != ONE_TIME || oneTimeInstant(zone)?.isAfter(now) == true ||
            (previous?.enabled == true && previous.frequency == ONE_TIME &&
                previous.oneTimeDateEpochDay == oneTimeDateEpochDay && previous.minuteOfDay == minuteOfDay)))

    fun isPastOneTime(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        frequency == ONE_TIME && (oneTimeConsumedAt != null || oneTimeInstant(zone)?.let { it <= now } == true)

    companion object {
        const val ONE_TIME = "ONE_TIME"
        const val DAILY = "DAILY"
        const val WEEKLY = "WEEKLY"
        const val MONTHLY = "MONTHLY"
    }
}

/** One occurrence per local date, even when last-day and custom dates overlap or DST repeats an hour. */
fun nextActivityReminder(reminder: ActivityReminder, after: Instant, zone: ZoneId): Instant? {
    if (!reminder.enabled || !reminder.isValid()) return null
    if (reminder.frequency == ActivityReminder.ONE_TIME) {
        if (reminder.oneTimeConsumedAt != null) return null
        return reminder.oneTimeInstant(zone)?.takeIf { it > after }
    }
    val today = after.atZone(zone).toLocalDate()
    val time = LocalTime.of(reminder.minuteOfDay / 60, reminder.minuteOfDay % 60)
    // A selected day 31 can require skipping a month. Invalid dates are never clamped.
    for (offset in 0L..62L) {
        val date = today.plusDays(offset)
        val selected = when (reminder.frequency) {
            ActivityReminder.DAILY -> true
            ActivityReminder.WEEKLY -> reminder.weekDays and (1 shl (date.dayOfWeek.value - 1)) != 0
            else -> reminder.monthDays and (1L shl date.dayOfMonth) != 0L ||
                (reminder.monthDays and 1L != 0L && date.dayOfMonth == date.lengthOfMonth())
        }
        if (selected) {
            // java.time moves nonexistent spring-forward times ahead by the DST gap.
            val instant = date.atTime(time).atZone(zone).toInstant()
            if (instant > after) return instant
        }
    }
    return null
}
