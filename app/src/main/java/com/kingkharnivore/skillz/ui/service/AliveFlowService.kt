package com.kingkharnivore.skillz.ui.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.ui.notification.AliveFlowNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AliveFlowService : Service() {

    @Inject lateinit var aliveFlowRepository: AliveFlowRepository

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var hasForegrounded = false

    override fun onCreate() {
        super.onCreate()

        // ✅ Always ensure channel first
        AliveFlowNotificationFactory.ensureChannel(this)

        // ✅ CRITICAL: startForeground immediately (do NOT wait on Room/Flow)
        startForeground(
            AliveFlowNotificationFactory.NOTIFICATION_ID,
            AliveFlowNotificationFactory.buildBootNotification(this)
        )
        hasForegrounded = true

        // Now you can safely do async work
        serviceScope.launch(Dispatchers.IO) {
            aliveFlowRepository.getOngoingSession()
                .collectLatest { entity ->
                    if (entity == null || !entity.isInFlowMode) {
                        stopSelfSafely()
                        return@collectLatest
                    }

                    val elapsedMs = computeElapsed(entity)

                    val notification = AliveFlowNotificationFactory.buildNotification(
                        this@AliveFlowService,
                        entity,
                        elapsedMs
                    )

                    // ✅ Update the existing foreground notification
                    NotificationManagerCompat.from(this@AliveFlowService)
                        .notify(AliveFlowNotificationFactory.NOTIFICATION_ID, notification)
                }
        }
    }

    private fun stopSelfSafely() {
        // ✅ Always legal after we've foregrounded immediately
        if (hasForegrounded) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
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
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}