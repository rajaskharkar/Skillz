package com.kingkharnivore.skillz.ui.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
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

    @Inject
    lateinit var aliveFlowRepository: AliveFlowRepository

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        AliveFlowNotificationFactory.ensureChannel(this)

        serviceScope.launch {
            aliveFlowRepository.getOngoingSession()
                .collectLatest { entity ->
                    if (entity == null || !entity.isInFlowMode) {
                        stopSelfSafely()
                        return@collectLatest
                    }

                    val elapsedMs = computeElapsed(entity)

                    val notification =
                        AliveFlowNotificationFactory.buildNotification(
                            this@AliveFlowService,
                            entity,
                            elapsedMs
                        )

                    startForeground(
                        AliveFlowNotificationFactory.NOTIFICATION_ID,
                        notification
                    )
                }
        }
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
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
