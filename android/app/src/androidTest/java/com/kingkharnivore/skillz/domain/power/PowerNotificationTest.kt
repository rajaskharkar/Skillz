package com.kingkharnivore.skillz.domain.power

import android.app.Notification
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.ui.notification.AliveFlowNotificationFactory
import com.kingkharnivore.skillz.ui.theme.PowerRed
import org.junit.Assert.*
import org.junit.Test

class PowerNotificationTest {
    @Test fun powerNotificationsUseAppLanguageAndPowerColorInRunningPausedAndReminderStates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("en", "es", "hi", "mr").forEach { language ->
            val local = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(language))
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
            })
            val session = OngoingSessionEntity(flowInstanceId="notification-test", title="", description="", tagName="", isInFlowMode=true,
                isRunning=true, mode=FlowMode.POWER, baseStartTimeMs=1, accumulatedBeforeStartMs=0)
            val running = AliveFlowNotificationFactory.buildNotification(local, session, 60_000)
            assertEquals(local.getString(R.string.power_notification_title, local.getString(R.string.alive_notification_fallback_title)), running.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertTrue(running.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().contains(local.getString(R.string.alive_notification_running)))
            assertEquals(PowerRed.toArgb(), running.color)
            val paused = AliveFlowNotificationFactory.buildNotification(local, session.copy(isRunning=false), 60_000)
            assertTrue(paused.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains(local.getString(R.string.alive_notification_paused)))
            val reminder = AliveFlowNotificationFactory.buildHourlyReminderNotification(local, session, 3_600_000, 1)
            assertEquals(local.getString(R.string.alive_reminder_title, local.getString(R.string.power_title)), reminder.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals(local.getString(R.string.alive_reminder_body), reminder.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertFalse(reminder.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().contains("%"))
        }
    }
}
