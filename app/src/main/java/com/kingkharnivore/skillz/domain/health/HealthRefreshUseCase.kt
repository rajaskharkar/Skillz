package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.data.health.HealthConnectMovementDataSource
import com.kingkharnivore.skillz.data.health.MovementReadResult
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class HealthRefreshUseCase @Inject constructor(
    private val settingsRepository: HealthSettingsRepository,
    private val permissionRepository: HealthPermissionRepository,
    private val movementDataSource: HealthConnectMovementDataSource,
    private val flowHealthRepository: FlowHealthRepository,
    private val calculator: MovementBonusCalculator
) {
    suspend fun refreshForeground(nowMs: Long = System.currentTimeMillis()) {
        if (!settingsRepository.settings.first().movementBonusEnabled) return
        flowHealthRepository.expireOldSnapshots(nowMs)
        if (!permissionRepository.isHealthConnectAvailable()) return
        if (!permissionRepository.isReadStepsGranted()) return

        flowHealthRepository.getRefreshableSnapshots(nowMs).forEach { snapshot ->
            refreshSnapshot(snapshot, nowMs)
        }
    }

    private suspend fun refreshSnapshot(snapshot: FlowHealthSnapshotEntity, nowMs: Long) {
        when (val result = readStepsForSnapshot(snapshot)) {
            MovementReadResult.HealthConnectUnavailable -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.ERROR_RETRYABLE, nowMs)
            MovementReadResult.PermissionMissing -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.PERMISSION_REVOKED, nowMs)
            MovementReadResult.NoData -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.PENDING, nowMs)
            is MovementReadResult.Error -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.ERROR_RETRYABLE, nowMs)
            is MovementReadResult.Success -> applySteps(snapshot, result.steps, nowMs)
        }
    }

    private suspend fun readStepsForSnapshot(snapshot: FlowHealthSnapshotEntity): MovementReadResult {
        val intervals = FlowActiveIntervalCodec.decode(snapshot.activeIntervalJson).ifEmpty {
            listOf(FlowActiveInterval(snapshot.flowStartTimeMs, snapshot.flowEndTimeMs))
        }
        var totalSteps = 0L
        var sawSuccess = false
        var sawNoData = false
        intervals.forEach { interval ->
            when (val result = movementDataSource.readStepsBetween(
                Instant.ofEpochMilli(interval.startTimeMs),
                Instant.ofEpochMilli(interval.endTimeMs)
            )) {
                MovementReadResult.HealthConnectUnavailable -> return MovementReadResult.HealthConnectUnavailable
                MovementReadResult.PermissionMissing -> return MovementReadResult.PermissionMissing
                MovementReadResult.NoData -> sawNoData = true
                is MovementReadResult.Error -> return result
                is MovementReadResult.Success -> {
                    sawSuccess = true
                    totalSteps += result.steps
                }
            }
        }
        return if (sawSuccess) MovementReadResult.Success(totalSteps) else if (sawNoData) MovementReadResult.NoData else MovementReadResult.NoData
    }

    private suspend fun applySteps(snapshot: FlowHealthSnapshotEntity, steps: Long, nowMs: Long) {
        val currentBreakdown = flowHealthRepository.getRewardBreakdown(snapshot.sessionId) ?: return
        val newRawMovement = maxOf(snapshot.rawMovementPoints, calculator.calculateMovementPoints(steps))
        val newBreakdown = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = currentBreakdown.nonMovementPreMultiplierPoints,
            pulseBonusPoints = currentBreakdown.pulseBonusPoints,
            surgeBonusPoints = currentBreakdown.surgeBonusPoints,
            otherPreMultiplierBonusPoints = currentBreakdown.otherPreMultiplierBonusPoints,
            movementPoints = newRawMovement,
            arcMultiplier = currentBreakdown.arcMultiplier,
            streakMultiplier = currentBreakdown.streakMultiplier,
            otherMultiplier = currentBreakdown.otherMultiplier,
            pearlEligible = currentBreakdown.pearlEligible
        )
        val delta = (newBreakdown.finalScyraPoints - currentBreakdown.finalScyraPoints).coerceAtLeast(0L)
        val status = if (newRawMovement > 0L) FlowHealthSyncStatus.CAPTURED else FlowHealthSyncStatus.NO_REWARD
        val updatedSnapshot = snapshot.copy(
            status = status,
            steps = maxOf(snapshot.steps ?: 0L, steps),
            rawMovementPoints = newRawMovement,
            finalMovementScyraContribution = newBreakdown.finalScyraPoints - (currentBreakdown.finalScyraPoints - snapshot.finalMovementScyraContribution),
            finalMovementPearlContribution = if (currentBreakdown.pearlEligible) newBreakdown.pearlsEarned else 0L,
            firstCheckedAtMs = snapshot.firstCheckedAtMs ?: nowMs,
            lastCheckedAtMs = nowMs,
            capturedAtMs = if (newRawMovement > 0L) nowMs else snapshot.capturedAtMs,
            checkCount = snapshot.checkCount + 1,
            updatedAfterSync = delta > 0L || snapshot.updatedAfterSync
        )
        val updatedBreakdown = currentBreakdown.copy(
            movementPoints = newBreakdown.movementPoints,
            preMultiplierTotal = newBreakdown.preMultiplierTotal,
            arcBonusPoints = newBreakdown.arcBonusPoints,
            finalScyraPoints = newBreakdown.finalScyraPoints,
            pearlsEarned = newBreakdown.pearlsEarned
        )
        val stablePearlReason = if (delta > 0L && currentBreakdown.pearlEligible) {
            "movement_bonus_delta_session_${snapshot.sessionId}_$newRawMovement"
        } else {
            null
        }
        flowHealthRepository.applyDelayedMovementUpdate(
            snapshot = updatedSnapshot,
            breakdown = updatedBreakdown,
            finalScyraPoints = newBreakdown.finalScyraPoints.toInt(),
            arcBonusPoints = newBreakdown.arcBonusPoints.toInt(),
            pearlDelta = delta.toInt(),
            stablePearlReason = stablePearlReason
        )
    }

    private suspend fun updateSnapshotOnly(snapshot: FlowHealthSnapshotEntity, status: FlowHealthSyncStatus, nowMs: Long) {
        flowHealthRepository.upsertSnapshot(
            snapshot.copy(
                status = status,
                firstCheckedAtMs = snapshot.firstCheckedAtMs ?: nowMs,
                lastCheckedAtMs = nowMs,
                checkCount = snapshot.checkCount + 1
            )
        )
    }
}
