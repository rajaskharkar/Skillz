package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AliveFlowRepository @Inject constructor(
    private val ongoingSessionDao: OngoingSessionDao
){
    fun getOngoingSession(): Flow<OngoingSessionEntity?> =
        ongoingSessionDao.getOngoingSession()

    suspend fun saveOngoingSession(entity: OngoingSessionEntity) {
        ongoingSessionDao.upsert(entity)
    }

    suspend fun getOngoingSessionNow(): OngoingSessionEntity? =
        ongoingSessionDao.getOngoingSessionNow()

    suspend fun updateOngoingSession(transform: (OngoingSessionEntity) -> OngoingSessionEntity) {
        val current = ongoingSessionDao.getOngoingSessionNow() ?: return
        ongoingSessionDao.upsert(transform(current))
    }

    suspend fun pauseCurrentFlow(now: Long) = updateOngoingSession { entity ->
        val accumulated = if (entity.isRunning && entity.baseStartTimeMs != null) {
            entity.accumulatedBeforeStartMs + (now - entity.baseStartTimeMs).coerceAtLeast(0L)
        } else {
            entity.accumulatedBeforeStartMs
        }
        entity.copy(isRunning = false, baseStartTimeMs = null, accumulatedBeforeStartMs = accumulated)
    }

    suspend fun enableAnchorForCurrentFlow() = updateOngoingSession {
        it.copy(
            anchorEnabledForFlow = true,
            anchorDisabledForFlow = false,
            anchorPaused = false,
            anchorUsageAccessRevoked = false
        )
    }

    suspend fun disableAnchorForCurrentFlow() = updateOngoingSession {
        it.copy(
            anchorEnabledForFlow = false,
            anchorDisabledForFlow = true,
            anchorPaused = false,
            anchorReturnPanelPending = false
        )
    }

    suspend fun pauseAnchor() = updateOngoingSession {
        it.copy(anchorPaused = true, anchorPausedCount = it.anchorPausedCount + if (it.anchorPaused) 0 else 1)
    }

    suspend fun resumeAnchor() = updateOngoingSession {
        it.copy(anchorPaused = false, anchorReturnPanelPending = false)
    }

    suspend fun startAnchorBreak(now: Long) = updateOngoingSession { entity ->
        val accumulated = if (entity.isRunning && entity.baseStartTimeMs != null) {
            entity.accumulatedBeforeStartMs + (now - entity.baseStartTimeMs).coerceAtLeast(0L)
        } else {
            entity.accumulatedBeforeStartMs
        }
        entity.copy(
            isRunning = false,
            baseStartTimeMs = null,
            accumulatedBeforeStartMs = accumulated,
            anchorPaused = true,
            anchorBreakStartedAtMs = now,
            anchorBreakEndsAtMs = now + 60_000L,
            anchorBreakCount = entity.anchorBreakCount + 1,
            anchorBreakOverPending = false,
            anchorReturnPanelPending = false
        )
    }

    suspend fun completeAnchorBreak(now: Long) = updateOngoingSession { entity ->
        val endsAt = entity.anchorBreakEndsAtMs ?: now
        val startedAt = entity.anchorBreakStartedAtMs ?: endsAt
        entity.copy(
            isRunning = false,
            anchorPaused = true,
            anchorBreakStartedAtMs = null,
            anchorBreakEndsAtMs = null,
            anchorTotalBreakDurationMs = entity.anchorTotalBreakDurationMs + (endsAt - startedAt).coerceAtLeast(0L),
            anchorBreakOverPending = true,
            anchorReturnPanelPending = true
        )
    }

    suspend fun markAnchorNudge() = updateOngoingSession {
        it.copy(
            anchorDistractionAttemptCount = it.anchorDistractionAttemptCount + 1,
            anchorReturnPanelPending = true
        )
    }

    suspend fun consumeAnchorReturnPanel() = updateOngoingSession {
        it.copy(anchorReturnPanelPending = false, anchorBreakOverPending = false)
    }

    suspend fun markAnchorUsageAccessRevoked() = updateOngoingSession {
        it.copy(anchorPaused = true, anchorUsageAccessRevoked = true)
    }

    suspend fun clearOngoingSession() {
        ongoingSessionDao.clear()
    }
}