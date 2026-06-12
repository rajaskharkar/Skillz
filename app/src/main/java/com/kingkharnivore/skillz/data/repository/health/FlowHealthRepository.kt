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
        dao.upsertSnapshot(snapshot)
        dao.upsertRewardBreakdown(breakdown)
        sessionDao.updateRewardPoints(snapshot.sessionId, finalScyraPoints, arcBonusPoints)
        if (pearlDelta > 0 && stablePearlReason != null) {
            val sourceId = snapshot.sessionId.toString()
            val alreadyAwarded = pearlLedgerDao.sourceRewardCount("session", sourceId, stablePearlReason) > 0
            if (!alreadyAwarded) {
                pearlLedgerDao.insert(
                    PearlLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        delta = pearlDelta,
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
