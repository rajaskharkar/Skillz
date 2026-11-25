package com.kingkharnivore.skillz.domain.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import com.kingkharnivore.skillz.data.repository.FlowPlanRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ActivityReminderPersistenceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    @get:Rule val helper = MigrationTestHelper(instrumentation, SkillzDatabase::class.java)

    // Android applies notification posts/cancellations asynchronously in system_server.
    private suspend fun awaitNotificationPresence(manager: NotificationManager, tag: String, present: Boolean) {
        kotlinx.coroutines.withTimeout(5_000) {
            while (manager.activeNotifications.any { it.tag == tag } != present) kotlinx.coroutines.delay(25)
        }
    }

    @Test fun migrationPreservesExistingActivitiesAndDefaultsRemindersOff() {
        val name = "reminders-migration"
        helper.createDatabase(name, 49).use { db ->
            db.execSQL("INSERT INTO flow_plans(id,title,isSoftMode,launchWithSurge,pinned,archived,launchCount,createdAt,updatedAt,kind,mode) VALUES(1,'Read',0,0,1,0,12,1,2,'HABIT',0)")
        }
        helper.runMigrationsAndValidate(name, 50, true, SkillzDatabaseMigrations.MIGRATION_49_50).use { db ->
            db.query("SELECT title,launchCount,pinned,reminder_enabled,reminder_frequency,reminder_minuteOfDay FROM flow_plans WHERE id=1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Read", c.getString(0)); assertEquals(12, c.getInt(1)); assertEquals(1, c.getInt(2))
                assertEquals(0, c.getInt(3)); assertEquals("DAILY", c.getString(4)); assertEquals(540, c.getInt(5))
            }
        }
    }

    @Test fun createEditDisableAndReopenPreserveAllSelectedDates() = runBlocking {
        val name = "reminders-${UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build()
        var db = open()
        try {
            val repository = FlowPlanRepository(db.flowPlanDao())
            val settings = ActivityReminder(true, ActivityReminder.MONTHLY, 22 * 60 + 17, 21, 1L or (1L shl 1) or (1L shl 15) or (1L shl 31))
            val id = repository.createFlowPlan("Read", null, false, null, false, reminder = settings)
            val original = repository.getFlowPlanById(id)!!
            assertEquals(settings, original.reminder)
            repository.updateFlowPlan(original.copy(title = "Read a book", reminder = settings.copy(enabled = false)))
            db.close()
            db = open()
            val restored = db.flowPlanDao().getFlowPlanById(id)!!
            assertEquals("Read a book", restored.title)
            assertEquals(settings.copy(enabled = false), restored.reminder)
            val weekly = settings.copy(frequency = ActivityReminder.WEEKLY, weekDays = 65)
            FlowPlanRepository(db.flowPlanDao()).updateFlowPlan(restored.copy(reminder = weekly))
            assertEquals(weekly, db.flowPlanDao().getFlowPlanById(id)!!.reminder)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun deliveryChecksLatestSettingsAndStopsForArchivedCompletedAndDeletedActivities() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.inMemoryDatabaseBuilder(context, SkillzDatabase::class.java).build()
        val scheduler = ActivityReminderScheduler(context, db)
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = 900001L
        val tag = "activity:$id"
        val now = ZonedDateTime.now().withSecond(0).withNano(0)
        val settings = ActivityReminder(enabled = true, minuteOfDay = now.hour * 60 + now.minute)
        val intent = Intent(ActivityReminderScheduler.ACTION).putExtra("id", id)
            .putExtra("target", now.toInstant().toEpochMilli()).putExtra("settings", settings.toString())
        try {
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = id, title = "Test reminder", kind = "PLAN", reminder = settings))
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, true)
            manager.cancel(tag, 0)
            val repository = FlowPlanRepository(db.flowPlanDao())
            repository.updateFlowPlan(repository.getFlowPlanById(id)!!.copy(reminder = settings.copy(minuteOfDay = (settings.minuteOfDay + 1) % 1440)))
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, false)
            repository.updateFlowPlan(repository.getFlowPlanById(id)!!.copy(reminder = settings))
            repository.setArchived(id, true)
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, false)
            repository.setArchived(id, false)
            db.flowPlanDao().markCompleted(id, System.currentTimeMillis())
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, false)
            repository.reopenPlan(id)
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, true)
            repository.deleteFlowPlanById(id)
            scheduler.schedule(id)
            awaitNotificationPresence(manager, tag, false)
            scheduler.deliver(intent)
            awaitNotificationPresence(manager, tag, false)
        } finally {
            db.flowPlanDao().deleteFlowPlanById(id)
            scheduler.schedule(id)
            db.close()
        }
    }
    @Test fun disablingEveryScheduleCancelsAlarmAndNotificationAndRejectsQueuedDelivery() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.inMemoryDatabaseBuilder(context, SkillzDatabase::class.java).build()
        val scheduler = ActivityReminderScheduler(context, db)
        val repository = FlowPlanRepository(db.flowPlanDao())
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = 900002L
        val alarmIntent = Intent(context, ActivityReminderReceiver::class.java)
            .setAction(ActivityReminderScheduler.ACTION).setData(android.net.Uri.parse("scyra://activity-reminder/$id"))
        fun alarm() = android.app.PendingIntent.getBroadcast(context, 0, alarmIntent,
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE)
        try {
            for (kind in listOf("HABIT", "PLAN")) {
                for (frequency in listOf(ActivityReminder.DAILY, ActivityReminder.WEEKLY, ActivityReminder.MONTHLY)) {
                    val now = ZonedDateTime.now().withSecond(0).withNano(0)
                    val settings = ActivityReminder(true, frequency, now.hour * 60 + now.minute,
                        1 shl (now.dayOfWeek.value - 1), 1L shl now.dayOfMonth)
                    db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = id, title = "Read", kind = kind, reminder = settings))
                    val delivery = Intent(ActivityReminderScheduler.ACTION).putExtra("id", id)
                        .putExtra("target", now.toInstant().toEpochMilli()).putExtra("settings", settings.toString())
                    scheduler.deliver(delivery)
                    assertNotNull(alarm())
                    awaitNotificationPresence(manager, "activity:$id", true)

                    repository.updateFlowPlan(repository.getFlowPlanById(id)!!.copy(reminder = settings.copy(enabled = false)))
                    scheduler.schedule(id)
                    assertNull("$kind $frequency retained an alarm after disabling", alarm())
                    awaitNotificationPresence(manager, "activity:$id", false)
                    scheduler.deliver(delivery)
                    scheduler.rescheduleAll()
                    assertNull(alarm())
                    awaitNotificationPresence(manager, "activity:$id", false)

                    repository.updateFlowPlan(repository.getFlowPlanById(id)!!.copy(reminder = settings))
                    scheduler.schedule(id)
                    assertNotNull("Enabling again should restore the alarm", alarm())
                }
            }
        } finally {
            db.flowPlanDao().deleteFlowPlanById(id)
            scheduler.schedule(id)
            db.close()
        }
    }

    @Test fun habitAndPlanNotificationsIncludeTheLatestTitleInHeadingAndBody() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.inMemoryDatabaseBuilder(context, SkillzDatabase::class.java).build()
        val scheduler = ActivityReminderScheduler(context, db)
        val repository = FlowPlanRepository(db.flowPlanDao())
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = 900003L
        try {
            for (kind in listOf("HABIT", "PLAN")) {
                val now = ZonedDateTime.now().withSecond(0).withNano(0)
                val settings = ActivityReminder(enabled = true, minuteOfDay = now.hour * 60 + now.minute)
                val title = "Read a chapter of my favorite book ($kind)"
                db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = id, title = "Old title", kind = kind, reminder = settings))
                repository.updateFlowPlan(repository.getFlowPlanById(id)!!.copy(title = title))
                scheduler.deliver(Intent(ActivityReminderScheduler.ACTION).putExtra("id", id)
                    .putExtra("target", now.toInstant().toEpochMilli()).putExtra("settings", settings.toString()))
                kotlinx.coroutines.withTimeout(5_000) {
                    while (manager.activeNotifications.firstOrNull { it.tag == "activity:$id" }?.notification?.extras
                            ?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() != title) kotlinx.coroutines.delay(25)
                }
                val extras = manager.activeNotifications.single { it.tag == "activity:$id" }.notification.extras
                assertEquals(title, extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString())
                assertTrue(extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains(title))
                assertTrue(extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT).toString().contains(title))
            }
        } finally {
            db.flowPlanDao().deleteFlowPlanById(id)
            scheduler.schedule(id)
            db.close()
        }
    }

    @Test fun oneTimeMigrationPreservesRecurringSchedules() {
        val name = "one-time-reminders-migration"
        helper.createDatabase(name, 50).use { db ->
            db.execSQL("INSERT INTO flow_plans(id,title,isSoftMode,launchWithSurge,pinned,archived,launchCount,createdAt,updatedAt,kind,mode,reminder_enabled,reminder_frequency,reminder_minuteOfDay,reminder_weekDays,reminder_monthDays) VALUES(1,'Read',0,0,1,0,12,1,2,'HABIT',0,1,'MONTHLY',1035,5,2147516419)")
        }
        helper.runMigrationsAndValidate(name, 51, true, SkillzDatabaseMigrations.MIGRATION_50_51).use { db ->
            db.query("SELECT title,reminder_enabled,reminder_frequency,reminder_minuteOfDay,reminder_weekDays,reminder_monthDays,reminder_oneTimeDateEpochDay,reminder_oneTimeConsumedAt FROM flow_plans WHERE id=1").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("Read", c.getString(0)); assertEquals(1, c.getInt(1))
                assertEquals("MONTHLY", c.getString(2)); assertEquals(1035, c.getInt(3)); assertEquals(5, c.getInt(4))
                assertEquals(2147516419L, c.getLong(5)); assertTrue(c.isNull(6)); assertTrue(c.isNull(7))
            }
        }
    }

    @Test fun oneTimeDateSurvivesReopenAndCanBeDisabledAndRescheduled() = runBlocking {
        val name = "one-time-${UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build()
        var db = open()
        val settings = ActivityReminder(true, ActivityReminder.ONE_TIME, 18 * 60 + 35,
            oneTimeDateEpochDay = java.time.LocalDate.now().plusDays(4).toEpochDay())
        try {
            val id = FlowPlanRepository(db.flowPlanDao()).createFlowPlan("Write", null, false, null, false, kind = "PLAN", reminder = settings)
            db.close(); db = open()
            val repo = FlowPlanRepository(db.flowPlanDao())
            assertEquals(settings, repo.getFlowPlanById(id)!!.reminder)
            val old = repo.getFlowPlanById(id)!!
            repo.updateFlowPlan(old.copy(reminder = settings.copy(enabled = false)))
            assertEquals(settings.copy(enabled = false), repo.getFlowPlanById(id)!!.reminder)
            val changed = settings.copy(oneTimeDateEpochDay = settings.oneTimeDateEpochDay!! + 1)
            repo.updateFlowPlan(old.copy(reminder = changed))
            assertEquals(changed, repo.getFlowPlanById(id)!!.reminder)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun oneTimeDeliversOnceForBothKindsAndStaleEditorCannotRearmIt() = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.inMemoryDatabaseBuilder(context, SkillzDatabase::class.java).build()
        val scheduler = ActivityReminderScheduler(context, db)
        val repo = FlowPlanRepository(db.flowPlanDao())
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = 900004L
        val tag = "activity:$id"
        val alarmIntent = Intent(context, ActivityReminderReceiver::class.java)
            .setAction(ActivityReminderScheduler.ACTION).setData(android.net.Uri.parse("scyra://activity-reminder/$id"))
        fun alarm() = android.app.PendingIntent.getBroadcast(context, 0, alarmIntent,
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE)
        try {
            for (kind in listOf("HABIT", "PLAN")) {
                val now = ZonedDateTime.now().withSecond(0).withNano(0)
                val settings = ActivityReminder(true, ActivityReminder.ONE_TIME, now.hour * 60 + now.minute,
                    oneTimeDateEpochDay = now.toLocalDate().toEpochDay())
                val draft = FlowPlanEntity(id = id, title = "Read once", kind = kind, reminder = settings)
                db.flowPlanDao().insertFlowPlan(draft)
                val delivery = Intent(ActivityReminderScheduler.ACTION).putExtra("id", id)
                    .putExtra("target", now.toInstant().toEpochMilli()).putExtra("settings", settings.toString())
                scheduler.deliver(delivery)
                awaitNotificationPresence(manager, tag, true)
                assertNotNull(repo.getFlowPlanById(id)!!.reminder.oneTimeConsumedAt)
                assertNull(alarm())
                manager.cancel(tag, 0)
                awaitNotificationPresence(manager, tag, false)
                repo.updateFlowPlan(draft.copy(title = "Edited after delivery"))
                assertNotNull(repo.getFlowPlanById(id)!!.reminder.oneTimeConsumedAt)
                scheduler.deliver(delivery)
                scheduler.rescheduleAll()
                assertNull(alarm())
                awaitNotificationPresence(manager, tag, false)

                val tomorrow = settings.copy(oneTimeDateEpochDay = settings.oneTimeDateEpochDay!! + 1)
                repo.updateFlowPlan(draft.copy(reminder = tomorrow))
                assertNull(repo.getFlowPlanById(id)!!.reminder.oneTimeConsumedAt)
                scheduler.schedule(id)
                assertNotNull(alarm())
                repo.updateFlowPlan(repo.getFlowPlanById(id)!!.copy(reminder = tomorrow.copy(enabled = false)))
                scheduler.schedule(id)
                assertNull(alarm())
                scheduler.deliver(delivery)
                awaitNotificationPresence(manager, tag, false)
            }
        } finally {
            db.flowPlanDao().deleteFlowPlanById(id); scheduler.schedule(id); db.close()
        }
    }

}
