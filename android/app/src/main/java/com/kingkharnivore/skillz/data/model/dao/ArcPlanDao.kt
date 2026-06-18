package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArcPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArcPlan(plan: ArcPlanEntity): Long

    @Update
    suspend fun updateArcPlan(plan: ArcPlanEntity)

    @Query("SELECT * FROM arc_plans WHERE id = :id LIMIT 1")
    suspend fun getArcPlanById(id: Long): ArcPlanEntity?

    @Query(
        """
        SELECT * FROM arc_plans
        WHERE archived = 0
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """
    )
    fun getActiveArcPlans(): Flow<List<ArcPlanEntity>>

    @Query(
        """
        SELECT * FROM arc_plans
        WHERE archived = 1
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """
    )
    fun getArchivedArcPlans(): Flow<List<ArcPlanEntity>>

    @Query(
        """
        SELECT * FROM arc_plans
        WHERE isInStudio = 1 AND archived = 0
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """
    )
    fun getStudioArcPlans(): Flow<List<ArcPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArcPlanStep(step: ArcPlanStepEntity): Long

    @Update
    suspend fun updateArcPlanStep(step: ArcPlanStepEntity)

    @Query(
        """
        SELECT * FROM arc_plan_steps
        WHERE arcPlanId = :arcPlanId
        ORDER BY orderIndex ASC, id ASC
        """
    )
    fun getStepsForArcPlan(arcPlanId: Long): Flow<List<ArcPlanStepEntity>>

    @Query(
        """
        SELECT * FROM arc_plan_steps
        WHERE arcPlanId = :arcPlanId
        ORDER BY orderIndex ASC, id ASC
        """
    )
    suspend fun getStepsForArcPlanOnce(arcPlanId: Long): List<ArcPlanStepEntity>

    @Query("DELETE FROM arc_plan_steps WHERE id = :stepId")
    suspend fun deleteArcPlanStepById(stepId: Long)

    @Query(
        """
        UPDATE arc_plans
        SET isInStudio = :isInStudio,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setInStudio(
        id: Long,
        isInStudio: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE arc_plans
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
        UPDATE arc_plans
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

    @Query("DELETE FROM arc_plans WHERE id = :id")
    suspend fun deleteArcPlanById(id: Long)

    @Transaction
    suspend fun replaceAllSteps(
        arcPlanId: Long,
        newSteps: List<ArcPlanStepEntity>
    ) {
        deleteAllStepsForArcPlan(arcPlanId)
        newSteps.forEach { insertArcPlanStep(it) }
    }

    @Query("DELETE FROM arc_plan_steps WHERE arcPlanId = :arcPlanId")
    suspend fun deleteAllStepsForArcPlan(arcPlanId: Long)
}