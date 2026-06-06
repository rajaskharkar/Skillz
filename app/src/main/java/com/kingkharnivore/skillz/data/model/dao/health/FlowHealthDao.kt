package com.kingkharnivore.skillz.data.model.dao.health

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowHealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: FlowHealthSnapshotEntity)

    @Update
    suspend fun updateSnapshot(snapshot: FlowHealthSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRewardBreakdown(breakdown: FlowRewardBreakdownEntity)

    @Query("SELECT * FROM flow_health_snapshots WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSnapshot(sessionId: Long): FlowHealthSnapshotEntity?

    @Query("SELECT * FROM flow_reward_breakdowns WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getRewardBreakdown(sessionId: Long): FlowRewardBreakdownEntity?

    @Query("SELECT * FROM flow_health_snapshots")
    fun observeSnapshots(): Flow<List<FlowHealthSnapshotEntity>>

    @Query("SELECT * FROM flow_health_snapshots WHERE sessionId IN (:sessionIds)")
    suspend fun getSnapshotsForSessions(sessionIds: List<Long>): List<FlowHealthSnapshotEntity>

    @Query("""
        SELECT COUNT(*) FROM flow_health_snapshots
        WHERE status IN (:statuses)
          AND (:nowMs < expiresAtMs OR expiresAtMs IS NULL)
          AND checkCount < :maxCheckCount
    """)
    suspend fun countRefreshableSnapshots(
        statuses: List<FlowHealthSyncStatus>,
        nowMs: Long,
        maxCheckCount: Int
    ): Int

    @Query("""
        SELECT * FROM flow_health_snapshots
        WHERE status IN (:statuses)
          AND (:nowMs < expiresAtMs OR expiresAtMs IS NULL)
          AND checkCount < :maxCheckCount
          AND (lastCheckedAtMs IS NULL OR lastCheckedAtMs <= :latestAllowedLastCheckMs)
        ORDER BY lastCheckedAtMs ASC, flowEndTimeMs DESC
        LIMIT :limit
    """)
    suspend fun getRefreshableSnapshots(
        statuses: List<FlowHealthSyncStatus>,
        nowMs: Long,
        latestAllowedLastCheckMs: Long,
        maxCheckCount: Int,
        limit: Int
    ): List<FlowHealthSnapshotEntity>

    @Query("""
        UPDATE flow_health_snapshots
        SET status = :disabledStatus, lastCheckedAtMs = :nowMs
        WHERE status IN (:statuses)
          AND (:nowMs < expiresAtMs OR expiresAtMs IS NULL)
          AND checkCount < :maxCheckCount
    """)
    suspend fun markRefreshableDisabled(
        statuses: List<FlowHealthSyncStatus>,
        disabledStatus: FlowHealthSyncStatus = FlowHealthSyncStatus.DISABLED_BEFORE_CAPTURE,
        nowMs: Long,
        maxCheckCount: Int
    )

    @Query("""
        UPDATE flow_health_snapshots
        SET status = :expiredStatus
        WHERE status IN (:statuses) AND expiresAtMs IS NOT NULL AND expiresAtMs <= :nowMs
    """)
    suspend fun expireOldSnapshots(
        statuses: List<FlowHealthSyncStatus>,
        nowMs: Long,
        expiredStatus: FlowHealthSyncStatus = FlowHealthSyncStatus.EXPIRED
    )

    @Transaction
    suspend fun upsertCompletionSnapshotAndBreakdown(
        snapshot: FlowHealthSnapshotEntity,
        breakdown: FlowRewardBreakdownEntity
    ) {
        upsertSnapshot(snapshot)
        upsertRewardBreakdown(breakdown)
    }
}
