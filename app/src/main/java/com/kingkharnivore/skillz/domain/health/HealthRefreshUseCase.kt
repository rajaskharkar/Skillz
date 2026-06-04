package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.data.health.HealthConnectMovementDataSource
import com.kingkharnivore.skillz.data.health.MovementReadResult
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class HealthRefreshUseCase @Inject constructor(
    private val settingsRepository: HealthSettingsRepository,
    private val permissionRepository: HealthPermissionRepository,
    private val movementDataSource: HealthConnectMovementDataSource,
    private val flowHealthRepository: FlowHealthRepository,
    private val shellRepository: ShellRepository,
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
        val result = movementDataSource.readStepsBetween(
            Instant.ofEpochMilli(snapshot.flowStartTimeMs),
            Instant.ofEpochMilli(snapshot.flowEndTimeMs)
        )
        when (result) {
            MovementReadResult.HealthConnectUnavailable -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.ERROR_RETRYABLE, nowMs)
            MovementReadResult.PermissionMissing -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.PERMISSION_REVOKED, nowMs)
            MovementReadResult.NoData -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.PENDING, nowMs)
            is MovementReadResult.Error -> updateSnapshotOnly(snapshot, FlowHealthSyncStatus.ERROR_RETRYABLE, nowMs)
            is MovementReadResult.Success -> applySteps(snapshot, result.steps, nowMs)
        }
    }

    private suspend fun applySteps(snapshot: FlowHealthSnapshotEntity, steps: Long, nowMs: Long) {
        val currentBreakdown = flowHealthRepository.getRewardBreakdown(snapshot.sessionId) ?: return
        val newRawMovement = maxOf(snapshot.rawMovementPoints, calculator.calculateMovementPoints(steps))
        val newBreakdown = MovementRewardRecalculator.withMovementPoints(
            baseFlowPoints = currentBreakdown.baseFlowPoints,
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
        flowHealthRepository.upsertCompletion(updatedSnapshot, updatedBreakdown)
        if (delta > 0L) {
            flowHealthRepository.updateSessionScyraPoints(
                snapshot.sessionId,
                newBreakdown.finalScyraPoints.toInt(),
                newBreakdown.arcBonusPoints.toInt()
            )
            if (currentBreakdown.pearlEligible) {
                shellRepository.addPearls(delta.toInt(), "movement_bonus_delta_${newRawMovement}", "session", snapshot.sessionId.toString())
            }
        }
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
