package com.kingkharnivore.skillz.ui.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.domain.anchor.AnchorMode
import com.kingkharnivore.skillz.domain.anchor.AnchorRuntimeInput
import com.kingkharnivore.skillz.domain.anchor.AnchorRuntimePolicy
import com.kingkharnivore.skillz.domain.anchor.NeverAnchorPolicy
import com.kingkharnivore.skillz.domain.anchor.hasMeaningfulActiveFlow
import com.kingkharnivore.skillz.domain.anchor.RecentAppsProvider
import com.kingkharnivore.skillz.ui.notification.AliveFlowNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AliveFlowService : Service() {

    @Inject lateinit var aliveFlowRepository: AliveFlowRepository
    @Inject lateinit var surgeHapticsManager: SurgeHapticsManager
    @Inject lateinit var anchorRepository: AnchorRepository
    @Inject lateinit var recentAppsProvider: RecentAppsProvider
    @Inject lateinit var neverAnchorPolicy: NeverAnchorPolicy

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
    private var currentAnchorEpisodePackage: String? = null
    private var anchorGuideDetectionJob: Job? = null

    private fun buildHourlyReminderSessionKey(entity: OngoingSessionEntity): String {
        return entity.createdAt.toString()
    }

    private fun clearHourlyReminderRuntime() {
        hourlyReminderRuntime = null
        hourlyReminderSessionKey = null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AliveFlowNotificationFactory.ACTION_PAUSE_ANCHOR -> serviceScope.launch(Dispatchers.IO) {
                aliveFlowRepository.pauseAnchor()
            }
            AliveFlowNotificationFactory.ACTION_RESUME_ANCHOR -> serviceScope.launch(Dispatchers.IO) {
                aliveFlowRepository.resumeAnchor()
            }
            AliveFlowNotificationFactory.ACTION_TAKE_ANCHOR_BREAK -> serviceScope.launch(Dispatchers.IO) {
                aliveFlowRepository.startAnchorBreak(System.currentTimeMillis())
            }
            AliveFlowNotificationFactory.ACTION_PAUSE_FLOW -> serviceScope.launch(Dispatchers.IO) {
                aliveFlowRepository.pauseCurrentFlow(System.currentTimeMillis())
            }
        }
        return START_STICKY
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
                    syncAnchorGuideDetectionLoop(null)
                    stopSelfSafely()
                    return@collectLatest
                }

                if (!entity.isRunning) {
                    surgeHapticsManager.cancel()
                }

                syncSurgeRuntimeWithEntity(entity)
                syncHourlyReminderRuntimeWithEntity(entity)
                publishNotification(entity)
                syncAnchorGuideDetectionLoop(entity)
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
                evaluateAnchorBreakIfNeeded(entity)

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
            }
        }
    }


    private fun syncAnchorGuideDetectionLoop(entity: OngoingSessionEntity?) {
        serviceScope.launch(Dispatchers.IO) {
            val shouldPoll = shouldContinueGuidePolling(entity)
            if (shouldPoll && anchorGuideDetectionJob?.isActive != true) {
                anchorGuideDetectionJob = serviceScope.launch(Dispatchers.IO) {
                    if (BuildConfig.DEBUG) Log.d("AnchorGuide", "polling started")
                    while (isActive) {
                        val latest = aliveFlowRepository.getOngoingSessionNow()
                        if (!shouldContinueGuidePolling(latest)) break
                        evaluateAnchorIfNeeded(latest!!)
                        delay(1_500L)
                    }
                    if (BuildConfig.DEBUG) Log.d("AnchorGuide", "polling stopped")
                    currentAnchorEpisodePackage = null
                    anchorGuideDetectionJob = null
                }
            } else if (!shouldPoll) {
                if (BuildConfig.DEBUG && anchorGuideDetectionJob?.isActive == true) Log.d("AnchorGuide", "polling cancelled")
                anchorGuideDetectionJob?.cancel()
                anchorGuideDetectionJob = null
                currentAnchorEpisodePackage = null
                cancelAnchorNotifications()
            }
        }
    }

    private suspend fun shouldContinueGuidePolling(entity: OngoingSessionEntity?): Boolean {
        val settings = anchorRepository.settings.first()
        val input = AnchorRuntimeInput(
            entity = entity,
            now = System.currentTimeMillis(),
            mode = settings.mode,
            globallyEnabled = settings.enabled,
            selectedPackageCount = anchorRepository.getAnchoredPackageSet().size,
            usageAccessGranted = recentAppsProvider.hasUsageAccess(),
            notificationsEnabled = canPostReminderNotifications()
        )
        val shouldRun = AnchorRuntimePolicy.shouldRunGuide(input)
        if (BuildConfig.DEBUG) {
            Log.d(
                "AnchorGuide",
                "shouldPoll=$shouldRun mode=${settings.mode} usage=${input.usageAccessGranted} count=${input.selectedPackageCount} entity=${entity != null}"
            )
        }
        return shouldRun
    }

    private fun cancelAnchorNotifications() {
        NotificationManagerCompat.from(this).cancel(AliveFlowNotificationFactory.ANCHOR_GUIDE_NUDGE_NOTIFICATION_ID)
        NotificationManagerCompat.from(this).cancel(AliveFlowNotificationFactory.ANCHOR_GUARD_RETURN_NOTIFICATION_ID)
        NotificationManagerCompat.from(this).cancel(AliveFlowNotificationFactory.ANCHOR_BREAK_NOTIFICATION_ID)
    }

    private suspend fun evaluateAnchorIfNeeded(entity: OngoingSessionEntity) {
        val settings = anchorRepository.settings.first()
        val anchoredPackages = anchorRepository.getAnchoredPackageSet()
        val runtimeInput = AnchorRuntimeInput(
            entity = entity,
            now = System.currentTimeMillis(),
            mode = settings.mode,
            globallyEnabled = settings.enabled,
            selectedPackageCount = anchoredPackages.size,
            usageAccessGranted = recentAppsProvider.hasUsageAccess(),
            notificationsEnabled = canPostReminderNotifications()
        )
        if (!AnchorRuntimePolicy.shouldRunGuide(runtimeInput)) return

        if (!recentAppsProvider.hasUsageAccess()) {
            if (!entity.anchorUsageAccessRevoked) {
                aliveFlowRepository.markAnchorUsageAccessRevoked()
            }
            return
        }

        val currentPackage = recentAppsProvider.getCurrentForegroundPackage()
        val shouldNudge = currentPackage != null &&
                currentPackage in anchoredPackages &&
                !neverAnchorPolicy.isNeverAnchored(currentPackage)

        if (!shouldNudge) {
            if (currentPackage == packageName || currentPackage !in anchoredPackages) {
                currentAnchorEpisodePackage = null
            }
            return
        }

        if (currentAnchorEpisodePackage == currentPackage) return
        currentAnchorEpisodePackage = currentPackage

        aliveFlowRepository.markAnchorNudge()
        val updated = aliveFlowRepository.getOngoingSessionNow() ?: entity
        publishAnchorNudgeNotification(updated)
    }

    private suspend fun evaluateAnchorBreakIfNeeded(entity: OngoingSessionEntity) {
        val endsAt = entity.anchorBreakEndsAtMs ?: return
        val now = System.currentTimeMillis()
        if (now < endsAt) return
        if (!hasMeaningfulActiveFlow(entity, now)) return
        aliveFlowRepository.completeAnchorBreak(now)
        val updated = aliveFlowRepository.getOngoingSessionNow() ?: entity
        publishBreakOverNotification(updated)
    }

    @SuppressLint("MissingPermission")
    private fun publishAnchorNudgeNotification(entity: OngoingSessionEntity) {
        if (!canPostReminderNotifications()) {
            if (BuildConfig.DEBUG) Log.d("AnchorGuide", "notification blocked; return panel pending")
            return
        }
        NotificationManagerCompat.from(this).notify(
            AliveFlowNotificationFactory.ANCHOR_GUIDE_NUDGE_NOTIFICATION_ID,
            AliveFlowNotificationFactory.buildAnchorNudgeNotification(this, entity)
        )
    }

    @SuppressLint("MissingPermission")
    private fun publishBreakOverNotification(entity: OngoingSessionEntity) {
        if (!canPostReminderNotifications()) {
            if (BuildConfig.DEBUG) Log.d("AnchorGuide", "notification blocked; return panel pending")
            return
        }
        NotificationManagerCompat.from(this).notify(
            AliveFlowNotificationFactory.ANCHOR_BREAK_NOTIFICATION_ID,
            AliveFlowNotificationFactory.buildBreakOverNotification(this, entity)
        )
    }


    private fun canPauseAnchorFromNotification(entity: OngoingSessionEntity): Boolean = runBlocking(Dispatchers.IO) {
        if (!entity.isInFlowMode || !entity.isRunning || entity.anchorPaused) return@runBlocking false
        if (entity.anchorBreakEndsAtMs?.let { it > System.currentTimeMillis() } == true) return@runBlocking false
        if (entity.anchorDisabledForFlow) return@runBlocking false
        val settings = anchorRepository.settings.first()
        if (settings.mode == AnchorMode.GUIDE && !recentAppsProvider.hasUsageAccess()) return@runBlocking false
        val enabled = (settings.mode == AnchorMode.GUIDE || settings.mode == AnchorMode.GUARD) && (entity.anchorEnabledForFlow || settings.enabled)
        enabled && anchorRepository.getAnchoredPackageSet().isNotEmpty()
    }

    private fun canResumeAnchorFromNotification(entity: OngoingSessionEntity): Boolean = runBlocking(Dispatchers.IO) {
        if (!entity.isInFlowMode || !entity.isRunning || !entity.anchorPaused) return@runBlocking false
        if (entity.anchorDisabledForFlow) return@runBlocking false
        val settings = anchorRepository.settings.first()
        if (settings.mode == AnchorMode.GUIDE && !recentAppsProvider.hasUsageAccess()) return@runBlocking false
        val enabled = (settings.mode == AnchorMode.GUIDE || settings.mode == AnchorMode.GUARD) && (entity.anchorEnabledForFlow || settings.enabled)
        enabled && anchorRepository.getAnchoredPackageSet().isNotEmpty()
    }

    @SuppressLint("MissingPermission")
    private fun publishNotification(entity: OngoingSessionEntity) {

        val elapsedMs = computeElapsed(entity)

        val notification = AliveFlowNotificationFactory.buildNotification(
            context = this,
            entity = entity,
            elapsedMs = elapsedMs,
            canPauseAnchor = canPauseAnchorFromNotification(entity),
            canResumeAnchor = canResumeAnchorFromNotification(entity)
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
        anchorGuideDetectionJob?.cancel()
        anchorGuideDetectionJob = null

        clearHourlyReminderRuntime()
        cancelHourlyReminderNotification()
        cancelAnchorNotifications()

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
        anchorGuideDetectionJob?.cancel()
        anchorGuideDetectionJob = null
        cancelAnchorNotifications()
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