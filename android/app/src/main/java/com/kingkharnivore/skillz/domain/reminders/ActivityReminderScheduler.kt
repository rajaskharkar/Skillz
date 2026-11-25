package com.kingkharnivore.skillz.domain.reminders

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
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ActivityReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SkillzDatabase
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    private val mutex = Mutex()

    /** Observe committed changes, including completion paths which write directly through the DAO. */
    suspend fun observeChanges() {
        var previous = emptyMap<Long, FlowPlanEntity>()
        db.flowPlanDao().observeAll().collect { plans ->
            val current = plans.associateBy { it.id }
            (previous.keys + current.keys).filter { previous[it] != current[it] }.forEach { id ->
                try { schedule(id) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { android.util.Log.e("ActivityReminders", "Unable to schedule $id", e) }
            }
            previous = current
        }
    }

    suspend fun rescheduleAll() {
        db.flowPlanDao().observeAll().first().forEach { schedule(it.id) }
    }

    suspend fun schedule(id: Long) = mutex.withLock { scheduleLatest(id) }

    private fun isActive(plan: FlowPlanEntity) = !plan.archived && plan.completedAt == null &&
        plan.kind in setOf(HorizonKind.HABIT, HorizonKind.PLAN) && plan.reminder.enabled

    private suspend fun scheduleLatest(id: Long) {
        PendingIntent.getBroadcast(context, 0, reminderIntent(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
        val plan = db.flowPlanDao().getFlowPlanById(id)
        if (plan == null || !isActive(plan)) {
            notifications.cancel("activity:$id", 0)
            return
        }
        val target = nextActivityReminder(plan.reminder, Instant.now(), ZoneId.systemDefault()) ?: return
        val alarm = pendingIntent(id, target.toEpochMilli(), plan.reminder.toString())
        // Use Android's deferred fallback when exact alarm access has not been granted.
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.toEpochMilli(), alarm) }
            catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.toEpochMilli(), alarm) }
        } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.toEpochMilli(), alarm)
    }

    suspend fun deliver(intent: Intent) = mutex.withLock {
        val id = intent.getLongExtra("id", -1)
        val target = intent.getLongExtra("target", 0)
        try {
            val plan = db.flowPlanDao().getFlowPlanById(id) ?: return@withLock
            val now = Instant.now()
            val targetInstant = Instant.ofEpochMilli(target)
            val zone = ZoneId.systemDefault()
            // Recheck saved settings so canceled/edited/completed activities cannot send stale reminders.
            if (isActive(plan) && intent.getStringExtra("settings") == plan.reminder.toString() &&
                now >= targetInstant && now.atZone(zone).toLocalDate() == targetInstant.atZone(zone).toLocalDate() &&
                nextActivityReminder(plan.reminder, targetInstant.minusMillis(1), zone) == targetInstant) {
                val claimed = plan.reminder.frequency != ActivityReminder.ONE_TIME || db.flowPlanDao().consumeOneTimeReminder(
                    id, requireNotNull(plan.reminder.oneTimeDateEpochDay), plan.reminder.minuteOfDay, now.toEpochMilli()
                ) == 1
                if (claimed) notify(plan)
            }
        } finally { scheduleLatest(id) }
    }

    private fun notify(plan: FlowPlanEntity) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.activity_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, reminderLaunchIntent(context, plan.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body = context.getString(
            if (plan.kind == HorizonKind.HABIT) R.string.activity_reminder_habit_body else R.string.activity_reminder_plan_body,
            plan.title
        )
        notifications.notify("activity:${plan.id}", 0, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(plan.title)
            .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private fun pendingIntent(id: Long, target: Long = 0, settings: String = ""): PendingIntent =
        PendingIntent.getBroadcast(context, 0, reminderIntent(id)
            .putExtra("id", id).putExtra("target", target).putExtra("settings", settings),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun reminderIntent(id: Long) = Intent(context, ActivityReminderReceiver::class.java)
        .setAction(ACTION).setData(Uri.parse("scyra://activity-reminder/$id"))

    companion object {
        const val ACTION = "com.kingkharnivore.skillz.ACTIVITY_REMINDER"
        const val CHANNEL = "activity_reminders"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActivityReminderEntryPoint {
    fun activityReminderScheduler(): ActivityReminderScheduler
}

class ActivityReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = EntryPointAccessors.fromApplication(context.applicationContext, ActivityReminderEntryPoint::class.java).activityReminderScheduler()
                if (intent.action == ActivityReminderScheduler.ACTION) scheduler.deliver(intent)
                else scheduler.rescheduleAll()
            } catch (e: Exception) {
                android.util.Log.e("ActivityReminders", "Unable to update reminders", e)
            } finally { pending.finish() }
        }
    }
}
