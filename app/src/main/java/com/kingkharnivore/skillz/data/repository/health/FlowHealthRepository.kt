package com.kingkharnivore.skillz.data.repository.health

import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.health.FlowHealthDao
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FlowHealthRepository @Inject constructor(
    private val dao: FlowHealthDao,
    private val sessionDao: SessionDao
) {
    fun observeSnapshots(): Flow<List<FlowHealthSnapshotEntity>> = dao.observeSnapshots()
    suspend fun getSnapshot(sessionId: Long): FlowHealthSnapshotEntity? = dao.getSnapshot(sessionId)
    suspend fun getRewardBreakdown(sessionId: Long): FlowRewardBreakdownEntity? = dao.getRewardBreakdown(sessionId)
    suspend fun upsertSnapshot(snapshot: FlowHealthSnapshotEntity) = dao.upsertSnapshot(snapshot)
    suspend fun upsertCompletion(snapshot: FlowHealthSnapshotEntity, breakdown: FlowRewardBreakdownEntity) =
        dao.upsertCompletionSnapshotAndBreakdown(snapshot, breakdown)

    suspend fun hasPendingRefreshableSnapshots(): Boolean =
        dao.countSnapshotsWithStatus(refreshableStatuses) > 0

    suspend fun markRefreshableDisabled(nowMs: Long) =
        dao.markStatusesDisabled(statuses = refreshableStatuses, nowMs = nowMs)

    suspend fun getRefreshableSnapshots(nowMs: Long, limit: Int = 8): List<FlowHealthSnapshotEntity> =
        dao.getRefreshableSnapshots(
            statuses = refreshableStatuses,
            nowMs = nowMs,
            latestAllowedLastCheckMs = nowMs - THROTTLE_MS,
            maxCheckCount = MAX_CHECK_COUNT,
            limit = limit
        )

    suspend fun expireOldSnapshots(nowMs: Long) = dao.expireOldSnapshots(refreshableStatuses, nowMs)

    suspend fun updateSessionScyraPoints(sessionId: Long, finalScyraPoints: Int, arcBonusPoints: Int) {
        sessionDao.updateRewardPoints(sessionId, finalScyraPoints, arcBonusPoints)
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
