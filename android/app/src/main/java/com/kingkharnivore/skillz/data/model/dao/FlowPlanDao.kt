package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowPlanDao {

    @Query("SELECT * FROM flow_plans ORDER BY pinned DESC, updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<FlowPlanEntity>>

    @Query("UPDATE flow_plans SET completedAt = :at WHERE id = :id AND kind = 'PLAN' AND completedAt IS NULL")
    suspend fun markCompleted(id: Long, at: Long)

    @Query("SELECT COUNT(*) FROM flow_plans WHERE tagId = :tagId")
    suspend fun countForTag(tagId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlowPlan(plan: FlowPlanEntity): Long

    @Query("""UPDATE flow_plans SET title=:title, tagId=:tagId, kind=:kind,
        isSoftMode=:soft, mode=:mode, targetMinutes=:minutes, launchWithSurge=:surge, updatedAt=:at
        WHERE id=:id AND completedAt IS NULL""")
    suspend fun updateDetails(id: Long, title: String, tagId: Long?, kind: String,
        soft: Boolean, mode: com.kingkharnivore.skillz.model.FlowMode, minutes: Int?, surge: Boolean, at: Long): Int

    @Query("SELECT * FROM flow_plans WHERE id = :id LIMIT 1")
    suspend fun getFlowPlanById(id: Long): FlowPlanEntity?

    @Query(
        """
        SELECT * FROM flow_plans
        WHERE archived = 0 AND completedAt IS NULL
        ORDER BY pinned DESC, updatedAt DESC, title COLLATE NOCASE ASC
        """
    )
    fun getActiveFlowPlans(): Flow<List<FlowPlanEntity>>

    @Query(
        """
        SELECT * FROM flow_plans
        WHERE archived = 1
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """
    )
    fun getArchivedFlowPlans(): Flow<List<FlowPlanEntity>>

    @Query(
        """
        UPDATE flow_plans
        SET archived = :archived,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setArchived(
        id: Long,
        archived: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE flow_plans
        SET pinned = :pinned,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setPinned(
        id: Long,
        pinned: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE flow_plans
        SET launchCount = launchCount + 1,
            lastLaunchedAt = :launchedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markLaunched(
        id: Long,
        launchedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM flow_plans WHERE id = :id")
    suspend fun deleteFlowPlanById(id: Long)
}