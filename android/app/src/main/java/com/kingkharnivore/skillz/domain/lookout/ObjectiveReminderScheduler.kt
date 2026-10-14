package com.kingkharnivore.skillz.domain.lookout

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kingkharnivore.skillz.MainActivity
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveWindow
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.kingkharnivore.skillz.utils.user.UserPrefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Wall-clock alarms are rebuilt after reboot or changes to the clock/time zone. */
@Singleton
class ObjectiveReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SkillzDatabase,
    private val calculator: ObjectiveProgressCalculator,
    private val userPrefs: UserPrefs
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val schedulingMutex = Mutex()

    suspend fun rescheduleAll() {
        db.objectiveDao().getActiveObjectives().forEach { schedule(it) }
    }

    suspend fun cancel(objectiveId: Long) = schedulingMutex.withLock {
        cancelAlarm(objectiveId)
    }

    private fun cancelAlarm(objectiveId: Long) {
        alarms.cancel(pendingIntent(objectiveId))
    }

    suspend fun schedule(objective: ObjectiveEntity, now: Instant = Instant.now()) = schedulingMutex.withLock {
        // A caller's snapshot can predate an archive or a concurrent settings update.
        scheduleLatest(objective.id, now)
    }

    private suspend fun scheduleLatest(objectiveId: Long, now: Instant) {
        cancelAlarm(objectiveId)
        val objective = db.objectiveDao().getObjective(objectiveId) ?: return
        if (objective.isArchived || objective.targetMinuteOfDay == null) return
        val skipped = db.objectiveSkippedCycleDao().getSkippedCycles().filter { it.objectiveId == objective.id }
        val completed = db.objectiveCompletionDao().getCompletionsForObjective(objective.id)
        val unavailable = skipped.map { ObjectiveWindow(it.periodStartMs, it.periodEndMs) }.toSet() +
            completed.map { ObjectiveWindow(it.periodStartMs, it.periodEndMs) }
        val leadMs = userPrefs.objectiveReminderMinutes.first().toLong() * 60_000L
        val plan = nextObjectiveReminder(objective, now, ZoneId.systemDefault(), unavailable, calculator, leadMs) ?: return
        val alarm = pendingIntent(objective.id, plan.window, plan.targetAtMs, leadMs)
        // Exact delivery where the user has allowed it; Android can defer the fallback alarm.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.remindAtMs, alarm)
            } catch (_: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.remindAtMs, alarm)
            }
        } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.remindAtMs, alarm)
    }

    suspend fun deliver(intent: Intent) = schedulingMutex.withLock {
        val id = intent.getLongExtra("objectiveId", -1)
        val objective = db.objectiveDao().getObjective(id) ?: return@withLock
        val start = intent.getLongExtra("windowStart", -1)
        val end = intent.getLongExtra("windowEnd", -1)
        val target = intent.getLongExtra("target", -1)
        val now = System.currentTimeMillis()
        val leadMs = userPrefs.objectiveReminderMinutes.first().toLong() * 60_000L
        val alarmLeadMs = intent.getLongExtra("leadMs", OBJECTIVE_REMINDER_LEAD_MS)
        val completed = db.objectiveCompletionDao().getCompletion(id, start, end) != null
        val skipped = db.objectiveSkippedCycleDao().getSkippedCycles().any {
            it.objectiveId == id && it.periodStartMs == start && it.periodEndMs == end
        }
        try {
            if (!objective.isArchived && !completed && !skipped && alarmLeadMs == leadMs && now >= target - leadMs && now < maxOf(target, target - leadMs + 60_000L) &&
                calculator.targetAtMs(objective, ObjectiveWindow(start, end), ZoneId.systemDefault()) == target) {
                notify(objective, target)
            }
        } finally {
            // A notification failure must not stop reminders for later recurring cycles.
            scheduleLatest(objective.id, Instant.ofEpochMilli(now + 1))
        }
    }

    private fun notify(objective: ObjectiveEntity, target: Long) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.lookout_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val targetLocal = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault())
        val formatter = if (targetLocal.toLocalDate() == java.time.LocalDate.now()) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        } else DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val time = formatter.format(targetLocal)
        val body = if (target <= System.currentTimeMillis()) context.getString(R.string.lookout_reminder_due_body, objective.journeyNameSnapshot)
            else context.getString(R.string.lookout_reminder_body, objective.journeyNameSnapshot, time)
        manager.notify("objective:${objective.id}", 0, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(context.getString(R.string.lookout_reminder_title))
            .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private fun pendingIntent(id: Long, window: ObjectiveWindow? = null, target: Long = 0, leadMs: Long = OBJECTIVE_REMINDER_LEAD_MS): PendingIntent {
        val intent = Intent(context, ObjectiveReminderReceiver::class.java)
            .setAction(ACTION).setData(Uri.parse("scyra://objective-reminder/$id"))
            .putExtra("objectiveId", id).putExtra("windowStart", window?.startMs ?: 0)
            .putExtra("windowEnd", window?.endMs ?: 0).putExtra("target", target).putExtra("leadMs", leadMs)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION = "com.kingkharnivore.skillz.OBJECTIVE_REMINDER"
        private const val CHANNEL = "objective_reminders"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ObjectiveReminderEntryPoint {
    fun scheduler(): ObjectiveReminderScheduler
}

class ObjectiveReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = EntryPointAccessors.fromApplication(context.applicationContext, ObjectiveReminderEntryPoint::class.java).scheduler()
                if (intent.action == ObjectiveReminderScheduler.ACTION) scheduler.deliver(intent)
                else scheduler.rescheduleAll()
            } catch (failure: Exception) {
                android.util.Log.e("ObjectiveReminders", "Unable to update reminders", failure)
            } finally { pending.finish() }
        }
    }
}
