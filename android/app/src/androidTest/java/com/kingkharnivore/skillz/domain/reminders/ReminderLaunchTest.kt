package com.kingkharnivore.skillz.domain.reminders

import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test

class ReminderLaunchTest {
    @Test fun notificationTapIntentsHaveDistinctDestinationsAndReuseMainActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val habit = reminderLaunchIntent(context, 17)
        val plan = reminderLaunchIntent(context, 18)
        assertEquals(17L, habit.reminderActivityId())
        assertEquals(18L, plan.reminderActivityId())
        assertFalse(habit.filterEquals(plan))
        assertTrue(habit.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(habit.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        val a = PendingIntent.getActivity(context, 0, habit, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = PendingIntent.getActivity(context, 0, plan, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        assertNotEquals(a, b)
        a.cancel(); b.cancel()
        assertNull(Intent().reminderActivityId())
        assertNull(reminderLaunchIntent(context, -1).reminderActivityId())
    }
}
