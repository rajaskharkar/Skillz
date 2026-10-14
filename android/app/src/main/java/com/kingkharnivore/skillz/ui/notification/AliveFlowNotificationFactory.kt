package com.kingkharnivore.skillz.ui.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.toArgb
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.MainActivity
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlin.math.max

object AliveFlowNotificationFactory {

    const val CHANNEL_ID = "flow_alive_channel"
    const val NOTIFICATION_ID = 1001

    const val REMINDER_CHANNEL_ID = "flow_hourly_reminder_channel"
    const val REMINDER_NOTIFICATION_ID = 1002

    // AppCompat applies pre-Android-13 app languages to activities, not Service contexts.
    private fun appLocaleContext(context: Context): Context {
        val selected = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        if (selected.isEmpty) return context
        val configuration = android.content.res.Configuration(context.resources.configuration).apply {
            setLocales(android.os.LocaleList.forLanguageTags(selected.toLanguageTags()))
        }
        return context.createConfigurationContext(configuration)
    }

    fun ensureChannels(context: Context) {
        val localizedContext = appLocaleContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                localizedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val aliveChannel = NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(R.string.alive_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = localizedContext.getString(R.string.alive_notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                localizedContext.getString(R.string.alive_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localizedContext.getString(R.string.alive_reminder_channel_description)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(aliveChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun buildNotification(
        context: Context,
        entity: OngoingSessionEntity,
        elapsedMs: Long
    ): Notification {
        val localizedContext = appLocaleContext(context)
        val elapsedSeconds = max(0, elapsedMs / 1000)
        val startWhenMs = System.currentTimeMillis() - elapsedMs

        val trueStartTimeMs =
            if (entity.baseStartTimeMs != null) {
                entity.baseStartTimeMs - entity.accumulatedBeforeStartMs
            } else {
                System.currentTimeMillis() - elapsedMs
            }

        val startedAtText = android.text.format.DateFormat.getTimeFormat(localizedContext).format(java.util.Date(trueStartTimeMs))

        val baseTitle = entity.title.takeIf { it.isNotBlank() } ?: localizedContext.getString(R.string.alive_notification_fallback_title)
        val title = if (entity.mode == com.kingkharnivore.skillz.model.FlowMode.POWER) localizedContext.getString(R.string.power_notification_title, baseTitle) else baseTitle
        val tag = entity.tagName.takeIf { it.isNotBlank() } ?: localizedContext.getString(R.string.alive_notification_no_journey)
        val status = localizedContext.getString(if (entity.isRunning) R.string.alive_notification_running else R.string.alive_notification_paused)
        val line2 =
            if (entity.isRunning) localizedContext.getString(R.string.alive_notification_started, status, startedAtText)
            else localizedContext.getString(R.string.alive_notification_total, status, formatElapsed(localizedContext, elapsedSeconds))

        val surgeLine = buildSurgeLine(localizedContext, entity, elapsedMs)

        val bigText = buildString {
            append(tag)

            surgeLine?.let {
                append("\n")
                append(it)
            }

            append("\n")
            append(line2)
        }

        // Prefer showing surge status when applicable
        val contentText = surgeLine ?: line2

        val openFlowIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("skillz://flow"),
            localizedContext,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openFlowPendingIntent = PendingIntent.getActivity(
            localizedContext,
            0,
            openFlowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openFlowPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(entity.isRunning)
            .setWhen(startWhenMs)
            .setColorized(true)
            .setColor(notificationColor(localizedContext, entity))

        if (entity.isRunning) {
            builder.setUsesChronometer(true)
        }

        val notification = builder.build()
        notification.flags = notification.flags or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR

        return notification
    }

    private fun buildSurgeLine(
        context: Context,
        entity: OngoingSessionEntity,
        elapsedMs: Long
    ): String? {
        val plannedMs = entity.surgePlannedMs ?: return null
        if (!entity.isSurgeOn) return null

        return if (elapsedMs >= plannedMs) {
            context.getString(R.string.alive_notification_surge_complete)
        } else {
            context.getString(R.string.alive_notification_surge_progress, formatElapsed(context, elapsedMs / 1000), formatElapsed(context, plannedMs / 1000))
        }
    }

    private fun formatElapsed(context: Context, seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return when {
            h > 0 -> "%d:%02d:%02d".format(context.resources.configuration.locales[0], h, m, s)
            else -> "%02d:%02d".format(context.resources.configuration.locales[0], m, s)
        }
    }

    private fun notificationColor(context: Context, entity: OngoingSessionEntity): Int {
        if (entity.mode != com.kingkharnivore.skillz.model.FlowMode.POWER) return BuildConfig.PRIMARY_COLOR
        val dark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        return (if (dark) com.kingkharnivore.skillz.ui.theme.PowerRedDark else com.kingkharnivore.skillz.ui.theme.PowerRed).toArgb()
    }

    fun buildBootNotification(context: Context): Notification {
        val localizedContext = appLocaleContext(context)
        return NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(localizedContext.getString(R.string.flow_card_type_flow))
            .setContentText(localizedContext.getString(R.string.alive_notification_starting))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildHourlyReminderNotification(
        context: Context,
        entity: OngoingSessionEntity,
        elapsedMs: Long,
        hourMark: Int
    ): Notification {
        val localizedContext = appLocaleContext(context)
        val mode = localizedContext.getString(when (entity.mode) {
            com.kingkharnivore.skillz.model.FlowMode.POWER -> R.string.power_title
            com.kingkharnivore.skillz.model.FlowMode.SOFT -> R.string.flow_screen_soft_short
            else -> R.string.flow_card_type_flow
        })
        val title = localizedContext.getString(R.string.alive_reminder_title, mode)
        val text =
            localizedContext.getString(R.string.alive_reminder_body)

        val flowTitle = entity.title.takeIf { it.isNotBlank() } ?: localizedContext.getString(R.string.alive_notification_fallback_title)
        val tag = entity.tagName.takeIf { it.isNotBlank() } ?: localizedContext.getString(R.string.alive_notification_no_journey)
        val elapsedSeconds = max(0, elapsedMs / 1000)

        val bigText = buildString {
            append(text)
            append("\n\n")
            append(flowTitle)
            append("\n")
            append(tag)
            append("\n")
            append(localizedContext.getString(R.string.alive_reminder_elapsed, formatElapsed(localizedContext, elapsedSeconds), hourMark))
        }

        val openFlowIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("skillz://flow"),
            localizedContext,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openFlowPendingIntent = PendingIntent.getActivity(
            localizedContext,
            1002,
            openFlowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(localizedContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openFlowPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(notificationColor(localizedContext, entity))
            .build()
    }
}
