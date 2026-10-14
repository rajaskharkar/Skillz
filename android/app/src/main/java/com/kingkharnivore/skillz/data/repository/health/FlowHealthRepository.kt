package com.kingkharnivore.skillz.data.repository.health

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.health.FlowHealthDao
import com.kingkharnivore.skillz.data.model.dao.shell.PearlLedgerDao
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import com.kingkharnivore.skillz.data.model.entity.shell.PearlLedgerEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class FlowHealthRepository @Inject constructor(
    private val db: SkillzDatabase,
    private val dao: FlowHealthDao,
    private val sessionDao: SessionDao,
    private val pearlLedgerDao: PearlLedgerDao
) {
    fun observeSnapshots(): Flow<List<FlowHealthSnapshotEntity>> = dao.observeSnapshots()
    suspend fun getSnapshot(sessionId: Long): FlowHealthSnapshotEntity? = dao.getSnapshot(sessionId)
    suspend fun getRewardBreakdown(sessionId: Long): FlowRewardBreakdownEntity? = dao.getRewardBreakdown(sessionId)
    suspend fun upsertSnapshot(snapshot: FlowHealthSnapshotEntity) = dao.upsertSnapshot(snapshot)
    suspend fun upsertCompletion(snapshot: FlowHealthSnapshotEntity, breakdown: FlowRewardBreakdownEntity) =
        dao.upsertCompletionSnapshotAndBreakdown(snapshot, breakdown)

    suspend fun hasPendingRefreshableSnapshots(nowMs: Long = System.currentTimeMillis()): Boolean {
        expireOldSnapshots(nowMs)
        return dao.countRefreshableSnapshots(
            statuses = refreshableStatuses,
            nowMs = nowMs,
            maxCheckCount = MAX_CHECK_COUNT
        ) > 0
    }

    suspend fun markRefreshableDisabled(nowMs: Long) =
        dao.markRefreshableDisabled(
            statuses = refreshableStatuses,
            nowMs = nowMs,
            maxCheckCount = MAX_CHECK_COUNT
        )

    suspend fun getRefreshableSnapshots(nowMs: Long, limit: Int = 8): List<FlowHealthSnapshotEntity> =
        dao.getRefreshableSnapshots(
            statuses = refreshableStatuses,
            nowMs = nowMs,
            latestAllowedLastCheckMs = nowMs - THROTTLE_MS,
            maxCheckCount = MAX_CHECK_COUNT,
            limit = limit
        )

    suspend fun expireOldSnapshots(nowMs: Long) = dao.expireOldSnapshots(refreshableStatuses, nowMs)

    suspend fun applyDelayedMovementUpdateTransactionally(
        snapshot: FlowHealthSnapshotEntity,
        breakdown: FlowRewardBreakdownEntity,
        finalScyraPoints: Int,
        arcBonusPoints: Int,
        pearlDelta: Int,
        stablePearlReason: String?
    ) = db.withTransaction {
        val session = sessionDao.getSessionById(snapshot.sessionId) ?: return@withTransaction
        val power = session.mode == com.kingkharnivore.skillz.model.FlowMode.POWER
        // Health reads may overlap or return out of order. Power's time/Arc award is
        // already final; reconcile only the additional movement against the persisted score.
        if (power && (finalScyraPoints < session.scyraPoints ||
                breakdown.movementPoints < (dao.getRewardBreakdown(session.id)?.movementPoints ?: 0))) return@withTransaction
        val actualDelta = if (power) (finalScyraPoints - session.scyraPoints).coerceAtLeast(0) else pearlDelta
        val preservedArcBonus = if (power) session.arcBonusPoints else arcBonusPoints
        dao.upsertSnapshot(if (power) snapshot.copy(
            finalMovementPearlContribution = if (breakdown.pearlEligible) snapshot.finalMovementScyraContribution else 0L
        ) else snapshot)
        dao.upsertRewardBreakdown(if (power) breakdown.copy(arcBonusPoints = preservedArcBonus.toLong()) else breakdown)
        sessionDao.updateRewardPoints(snapshot.sessionId, finalScyraPoints, preservedArcBonus)
        if (actualDelta > 0 && stablePearlReason != null) {
            val sourceId = snapshot.sessionId.toString()
            val alreadyAwarded = pearlLedgerDao.sourceRewardCount("session", sourceId, stablePearlReason) > 0
            if (!alreadyAwarded) {
                // If initial rewards are still pending, their authoritative score already
                // includes this movement. Persist a zero-value receipt so replay cannot
                // award the delta again after the pending grant finishes.
                val grantDelta = if (db.powerRewardDao().reward(snapshot.sessionId)?.completed == false) 0 else actualDelta
                if (grantDelta > 0 && power) {
                    db.powerRewardDao().insert(com.kingkharnivore.skillz.data.model.entity.shell.PebbleLedgerEntity(
                        "movement:$sourceId:$stablePearlReason", grantDelta, sourceId, System.currentTimeMillis()))
                }
                pearlLedgerDao.insert(
                    PearlLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        delta = grantDelta,
                        reason = stablePearlReason,
                        sourceType = "session",
                        sourceId = sourceId,
                        createdAt = System.currentTimeMillis(),
                        note = "Movement Bonus delayed sync"
                    )
                )
            }
        }
    }

    companion object {
        const val REFRESH_WINDOW_MS: Long = 72L * 60L * 60L * 1000L
        const val THROTTLE_MS: Long = 30L * 60L * 1000L
        const val MAX_CHECK_COUNT: Int = 10
        val refreshableStatuses = listOf(
            FlowHealthSyncStatus.PENDING,
            FlowHealthSyncStatus.NO_REWARD,
            FlowHealthSyncStatus.CAPTURED,
            FlowHealthSyncStatus.ERROR_RETRYABLE
        )
    }
}
