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
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.MainActivity
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlin.math.max

object AliveFlowNotificationFactory {

    const val CHANNEL_ID = "flow_alive_channel"
    const val CHANNEL_NAME = "Flow State"
    const val NOTIFICATION_ID = 1001

    const val REMINDER_CHANNEL_ID = "flow_hourly_reminder_channel"
    const val REMINDER_CHANNEL_NAME = "Flow reminders"
    const val REMINDER_NOTIFICATION_ID = 1002
    const val ACTION_RETURN_TO_FLOW = "com.kingkharnivore.skillz.anchor.RETURN_TO_FLOW"
    const val ACTION_PAUSE_ANCHOR = "com.kingkharnivore.skillz.anchor.PAUSE_ANCHOR"
    const val ACTION_RESUME_ANCHOR = "com.kingkharnivore.skillz.anchor.RESUME_ANCHOR"
    const val ACTION_TAKE_ANCHOR_BREAK = "com.kingkharnivore.skillz.anchor.TAKE_BREAK"
    const val ACTION_PAUSE_FLOW = "com.kingkharnivore.skillz.flow.PAUSE_FLOW"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val aliveChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a Flow State is active"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle check-ins during long Flow sessions"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(aliveChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }


    private fun openFlowPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val openFlowIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("skillz://flow"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            openFlowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, com.kingkharnivore.skillz.ui.service.AliveFlowService::class.java)
            .setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildNotification(
        context: Context,
        entity: OngoingSessionEntity,
        elapsedMs: Long,
        canPauseAnchor: Boolean = false,
        canResumeAnchor: Boolean = false
    ): Notification {
        val elapsedSeconds = max(0, elapsedMs / 1000)
        val startWhenMs = System.currentTimeMillis() - elapsedMs

        val trueStartTimeMs =
            if (entity.baseStartTimeMs != null) {
                entity.baseStartTimeMs - entity.accumulatedBeforeStartMs
            } else {
                System.currentTimeMillis() - elapsedMs
            }

        val startedAtText = formatClockTime(trueStartTimeMs)

        val title = entity.title.takeIf { it.isNotBlank() } ?: "Flow in progress"
        val tag = entity.tagName.takeIf { it.isNotBlank() } ?: "Unassigned Skill"
        val status = when {
            entity.anchorBreakEndsAtMs?.let { it > System.currentTimeMillis() } == true -> "Break active · Flow paused"
            entity.anchorPaused && entity.isRunning -> "Flow active · Anchor paused"
            entity.isRunning -> "Alive • Running"
            else -> "Alive • Paused"
        }
        val line2 =
            if (entity.isRunning) "$status • Started at $startedAtText"
            else "$status • Total ${formatElapsed(elapsedSeconds)}"

        val surgeLine = buildSurgeLine(entity, elapsedMs)

        val bigText = buildString {
            append(tag)

            entity.description.takeIf { !it.isNullOrBlank() }?.let {
                append("\n")
                append(it.trim())
            }

            surgeLine?.let {
                append("\n")
                append(it)
            }

            append("\n")
            append(line2)
        }

        // Prefer showing surge status when applicable
        val contentText = surgeLine ?: line2

        val openFlowPendingIntent = openFlowPendingIntent(context, 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            .setColor(BuildConfig.PRIMARY_COLOR)

        if (entity.isRunning) {
            builder.setUsesChronometer(true)
        }

        if (entity.isRunning) {
            builder.addAction(0, "Pause Flow", serviceActionPendingIntent(context, ACTION_PAUSE_FLOW, 2000))
        }
        if (canResumeAnchor) {
            builder.addAction(0, "Resume Anchor", serviceActionPendingIntent(context, ACTION_RESUME_ANCHOR, 2003))
        } else if (canPauseAnchor) {
            builder.addAction(0, "Pause Anchor", serviceActionPendingIntent(context, ACTION_PAUSE_ANCHOR, 2001))
        }
        builder.addAction(0, "Return to Flow", openFlowPendingIntent(context, 2002))

        val notification = builder.build()
        notification.flags = notification.flags or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR

        return notification
    }


    fun buildAnchorNudgeNotification(context: Context, entity: OngoingSessionEntity): Notification {
        val text = context.getString(R.string.anchor_notification_nudge)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(context.getString(R.string.anchor_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openFlowPendingIntent(context, 3000))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(BuildConfig.PRIMARY_COLOR)
            .addAction(0, context.getString(R.string.anchor_action_return_to_flow), openFlowPendingIntent(context, 3001))
            .addAction(0, context.getString(R.string.anchor_action_take_break), serviceActionPendingIntent(context, ACTION_TAKE_ANCHOR_BREAK, 3002))
            .addAction(0, context.getString(R.string.anchor_action_pause), serviceActionPendingIntent(context, ACTION_PAUSE_ANCHOR, 3003))

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        return notification
    }


    fun buildBreakOverNotification(context: Context, entity: OngoingSessionEntity): Notification {
        val text = context.getString(R.string.anchor_notification_break_over)
        val title = entity.title.takeIf { it.isNotBlank() } ?: "Flow in progress"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openFlowPendingIntent(context, 3100))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(BuildConfig.PRIMARY_COLOR)
            .addAction(0, "Return to Flow", openFlowPendingIntent(context, 3101))
            .build()
    }

    private fun buildSurgeLine(
        entity: OngoingSessionEntity,
        elapsedMs: Long
    ): String? {
        val plannedMs = entity.surgePlannedMs ?: return null
        if (!entity.isSurgeOn) return null

        return if (elapsedMs >= plannedMs) {
            "Surge Complete • Keep going"
        } else {
            "Surge • ${formatElapsed(elapsedMs / 1000)} / ${formatElapsed(plannedMs / 1000)}"
        }
    }

    private fun formatClockTime(timeMs: Long): String {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timeMs))
    }

    private fun formatElapsed(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return when {
            h > 0 -> "%d:%02d:%02d".format(h, m, s)
            else -> "%02d:%02d".format(m, s)
        }
    }

    fun buildBootNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Flow")
            .setContentText("Starting…")
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
        val title = "Still in Flow?"
        val text =
            "No action needed if this is deliberate. Tap to return to Flow and end it if you forgot."

        val flowTitle = entity.title.takeIf { it.isNotBlank() } ?: "Flow in progress"
        val tag = entity.tagName.takeIf { it.isNotBlank() } ?: "Unassigned Skill"
        val elapsedSeconds = max(0, elapsedMs / 1000)

        val bigText = buildString {
            append(text)
            append("\n\n")
            append(flowTitle)
            append("\n")
            append(tag)
            append("\n")
            append("Elapsed: ")
            append(formatElapsed(elapsedSeconds))
            append(" • Hour ")
            append(hourMark)
        }

        val openFlowPendingIntent = openFlowPendingIntent(context, 1002)

        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_scyra_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openFlowPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(BuildConfig.PRIMARY_COLOR)
            .build()
    }
}