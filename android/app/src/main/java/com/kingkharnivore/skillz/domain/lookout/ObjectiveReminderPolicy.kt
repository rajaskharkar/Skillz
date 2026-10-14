package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveKind
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveWindow
import java.time.Instant
import java.time.ZoneId

const val OBJECTIVE_REMINDER_LEAD_MS = 30 * 60_000L

data class ObjectiveReminderPlan(val window: ObjectiveWindow, val targetAtMs: Long, val leadMs: Long = OBJECTIVE_REMINDER_LEAD_MS) {
    val remindAtMs: Long get() = targetAtMs - leadMs
}

fun nextObjectiveReminder(
    objective: ObjectiveEntity,
    now: Instant,
    zone: ZoneId,
    unavailableWindows: Set<ObjectiveWindow>,
    calculator: ObjectiveProgressCalculator = ObjectiveProgressCalculator(),
    leadMs: Long = OBJECTIVE_REMINDER_LEAD_MS
): ObjectiveReminderPlan? {
    if (objective.isArchived || objective.targetMinuteOfDay == null) return null
    require(leadMs >= 0)
    // Jump to the first possible cycle when a reminder is more than one cycle early.
    var window = calculator.windowFor(objective, now.plusMillis(leadMs), zone)
    while (true) {
        val target = calculator.targetAtMs(objective, window, zone) ?: return null
        val plan = ObjectiveReminderPlan(window, target, leadMs)
        if (plan.remindAtMs > now.toEpochMilli() && window !in unavailableWindows) return plan
        if (objective.objectiveType != ObjectiveKind.Recurring.storageValue) return null
        window = calculator.windowFor(objective, Instant.ofEpochMilli(window.endMs), zone)
    }
}
