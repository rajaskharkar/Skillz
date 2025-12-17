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
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlin.math.max

object AliveFlowNotificationFactory {

    const val CHANNEL_ID = "flow_alive_channel"
    const val CHANNEL_NAME = "Flow State"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // ongoing, non-intrusive
            ).apply {
                description = "Shows when a Flow State is active"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildRestoringNotification(context: Context): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Flow is alive")
            .setContentText("Restoring…")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val n = builder.build()
        n.flags = n.flags or
                android.app.Notification.FLAG_ONGOING_EVENT or
                android.app.Notification.FLAG_NO_CLEAR
        return n
    }

    fun buildNotification(
        context: Context,
        entity: OngoingSessionEntity,
        elapsedMs: Long
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
        val status = if (entity.isRunning) "Alive • Running" else "Alive • Paused"
        val line2 =
            if (entity.isRunning) "$status • Started at $startedAtText"
            else "$status • Total ${formatElapsed(elapsedSeconds)}"

        val bigText = buildString {
            append(tag)
            entity.description.takeIf { !it.isNullOrBlank() }?.let {
                append("\n")
                append(it.trim())
            }
            append("\n")
            append(line2)
        }

        // Tap notification -> open Flow screen (deep link handled by NavGraph)
        val openFlowIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("skillz://flow"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openFlowPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openFlowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(line2)
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

        if (entity.isRunning) builder.setUsesChronometer(true)

        val notification = builder.build()
        notification.flags = notification.flags or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR

        return notification
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
}
