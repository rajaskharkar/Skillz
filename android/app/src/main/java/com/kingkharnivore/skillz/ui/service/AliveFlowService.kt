package com.kingkharnivore.skillz.ui.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.ui.notification.AliveFlowNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AliveFlowService : Service() {

    @Inject lateinit var aliveFlowRepository: AliveFlowRepository
    @Inject lateinit var surgeHapticsManager: SurgeHapticsManager

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
    }

    private data class HourlyReminderRuntime(
        val lastRemindedHour: Int = 0
    )

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var hasForegrounded = false
    private var latestEntity: OngoingSessionEntity? = null
    private var tickerJob: Job? = null

    private var surgeRuntime: SurgeRuntimeState? = null
    private var surgeSessionKey: String? = null

    private var hourlyReminderRuntime: HourlyReminderRuntime? = null
    private var hourlyReminderSessionKey: String? = null

    private fun buildHourlyReminderSessionKey(entity: OngoingSessionEntity): String {
        return entity.createdAt.toString()
    }

    private fun clearHourlyReminderRuntime() {
        hourlyReminderRuntime = null
        hourlyReminderSessionKey = null
    }


    override fun onCreate() {
        super.onCreate()

        AliveFlowNotificationFactory.ensureChannels(this)

        startForeground(
            AliveFlowNotificationFactory.NOTIFICATION_ID,
            AliveFlowNotificationFactory.buildBootNotification(this)
        )
        hasForegrounded = true

        observeOngoingSession()
        startTicker()
    }

    private fun observeOngoingSession() {
        serviceScope.launch(Dispatchers.IO) {
            aliveFlowRepository.getOngoingSession().collectLatest { entity ->
                latestEntity = entity

                if (entity == null || !entity.isInFlowMode) {
                    surgeHapticsManager.cancel()
                    clearSurgeRuntime()
                    clearHourlyReminderRuntime()
                    cancelHourlyReminderNotification()
                    stopSelfSafely()
                    return@collectLatest
                }

                if (!entity.isRunning) {
                    surgeHapticsManager.cancel()
                }

                syncSurgeRuntimeWithEntity(entity)
                syncHourlyReminderRuntimeWithEntity(entity)
                publishNotification(entity)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)

                val entity = latestEntity ?: continue
                if (!entity.isInFlowMode) continue

                if (entity.isRunning) {
                    evaluateSurgeIfNeeded(entity)
                    evaluateHourlyReminderIfNeeded(entity)
                }

                publishNotification(latestEntity ?: entity)
            }
        }
    }

    private fun syncSurgeRuntimeWithEntity(entity: OngoingSessionEntity) {
        if (!entity.isSurgeOn || entity.surgePlannedMs == null) {
            clearSurgeRuntime()
            return
        }

        val newKey = buildSurgeSessionKey(entity)

        if (surgeRuntime != null && surgeSessionKey == newKey) {
            return
        }

        val plannedMs = entity.surgePlannedMs
        val freshRuntime = SurgeRuntimeState(
            plannedMs = plannedMs,
        )

        val elapsedMs = computeElapsed(entity)

        surgeRuntime = SurgeRuntimeEvaluator.silentCatchUp(
            runtime = freshRuntime,
            elapsedMs = elapsedMs
        )
        surgeSessionKey = newKey
    }

    private fun evaluateSurgeIfNeeded(entity: OngoingSessionEntity) {
        if (!entity.isSurgeOn || entity.surgePlannedMs == null) return
        if (!entity.isRunning) return

        if (surgeRuntime == null) {
            syncSurgeRuntimeWithEntity(entity)
        }

        val runtime = surgeRuntime ?: return
        val elapsedMs = computeElapsed(entity)

        val (nextRuntime, events) = SurgeRuntimeEvaluator.evaluate(
            runtime = runtime,
            elapsedMs = elapsedMs
        )

        if (nextRuntime != runtime) {
            surgeRuntime = nextRuntime
        }

        events.forEach(::handleSurgeEvent)
    }

    private fun handleSurgeEvent(event: SurgeTickEvent) {
        when (event) {
            SurgeTickEvent.Midpoint -> {
                surgeHapticsManager.playMidpoint()
            }

            SurgeTickEvent.FiveMinutesLeft -> {
                surgeHapticsManager.playFiveMinutesLeft()
            }

            SurgeTickEvent.TwoMinutesLeft -> {
                surgeHapticsManager.playTwoMinutesLeft()
            }

            SurgeTickEvent.OneMinuteLeft -> {
                surgeHapticsManager.playOneMinuteLeft()
            }

            SurgeTickEvent.ThirtySecondsLeft -> {
                surgeHapticsManager.playTenSecondsLeft()
            }

            SurgeTickEvent.TenSecondsLeft -> {
                surgeHapticsManager.playTenSecondsLeft()
            }

            is SurgeTickEvent.CountdownTick -> {
                surgeHapticsManager.playCountdownTick(event.secondsRemaining)
            }

            SurgeTickEvent.TargetReached -> {
                surgeHapticsManager.playTargetReached()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun publishNotification(entity: OngoingSessionEntity) {

        val elapsedMs = computeElapsed(entity)

        val notification = AliveFlowNotificationFactory.buildNotification(
            context = this,
            entity = entity,
            elapsedMs = elapsedMs
        )

        val manager = NotificationManagerCompat.from(this)

        // Android 13+ requires POST_NOTIFICATIONS runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (manager.areNotificationsEnabled()) {
                manager.notify(
                    AliveFlowNotificationFactory.NOTIFICATION_ID,
                    notification
                )
            }
        } else {
            manager.notify(
                AliveFlowNotificationFactory.NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun stopSelfSafely() {
        tickerJob?.cancel()
        tickerJob = null

        clearHourlyReminderRuntime()
        cancelHourlyReminderNotification()

        if (hasForegrounded) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun clearSurgeRuntime() {
        surgeRuntime = null
        surgeSessionKey = null
    }

    private fun buildSurgeSessionKey(entity: OngoingSessionEntity): String {
        return listOf(
            entity.baseStartTimeMs?.toString().orEmpty(),
            entity.accumulatedBeforeStartMs.toString(),
            entity.surgePlannedMs?.toString().orEmpty(),
            entity.isSurgeOn.toString()
        ).joinToString("|")
    }

    private fun computeElapsed(entity: OngoingSessionEntity): Long {
        val base = entity.baseStartTimeMs
        return if (entity.isRunning && base != null) {
            entity.accumulatedBeforeStartMs +
                    (System.currentTimeMillis() - base).coerceAtLeast(0L)
        } else {
            entity.accumulatedBeforeStartMs
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        surgeHapticsManager.cancel()
        clearSurgeRuntime()
        clearHourlyReminderRuntime()
        cancelHourlyReminderNotification()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun syncHourlyReminderRuntimeWithEntity(entity: OngoingSessionEntity) {
        val newKey = buildHourlyReminderSessionKey(entity)

        if (hourlyReminderRuntime != null && hourlyReminderSessionKey == newKey) {
            return
        }

        val elapsedMs = computeElapsed(entity)
        val elapsedHours = (elapsedMs / HOUR_MS).toInt()

        // ✅ No retroactive catch-up notifications on restore/rebind.
        hourlyReminderRuntime = HourlyReminderRuntime(
            lastRemindedHour = elapsedHours
        )
        hourlyReminderSessionKey = newKey
    }

    private fun canPostReminderNotifications(): Boolean {
        val manager = NotificationManagerCompat.from(this)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.areNotificationsEnabled()
        } else {
            manager.areNotificationsEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    private fun publishHourlyReminderNotification(
        entity: OngoingSessionEntity,
        elapsedMs: Long,
        hourMark: Int
    ) {
        if (!canPostReminderNotifications()) return

        val notification = AliveFlowNotificationFactory.buildHourlyReminderNotification(
            context = this,
            entity = entity,
            elapsedMs = elapsedMs,
            hourMark = hourMark
        )

        NotificationManagerCompat.from(this).notify(
            AliveFlowNotificationFactory.REMINDER_NOTIFICATION_ID,
            notification
        )
    }

    private fun evaluateHourlyReminderIfNeeded(entity: OngoingSessionEntity) {
        if (!entity.isInFlowMode || !entity.isRunning) return

        if (hourlyReminderRuntime == null) {
            syncHourlyReminderRuntimeWithEntity(entity)
        }

        val runtime = hourlyReminderRuntime ?: return
        val elapsedMs = computeElapsed(entity)
        val elapsedHours = (elapsedMs / HOUR_MS).toInt()

        if (elapsedHours >= 1 && elapsedHours > runtime.lastRemindedHour) {
            publishHourlyReminderNotification(
                entity = entity,
                elapsedMs = elapsedMs,
                hourMark = elapsedHours
            )

            hourlyReminderRuntime = runtime.copy(
                lastRemindedHour = elapsedHours
            )
        }
    }

    private fun cancelHourlyReminderNotification() {
        NotificationManagerCompat.from(this).cancel(
            AliveFlowNotificationFactory.REMINDER_NOTIFICATION_ID
        )
    }
}