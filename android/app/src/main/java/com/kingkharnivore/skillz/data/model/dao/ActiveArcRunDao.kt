package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.ActiveArcRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveArcRunDao {

    @Query("SELECT * FROM active_arc_run WHERE id = 1")
    fun getActiveArcRun(): Flow<ActiveArcRunEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveArcRunEntity)

    @Query("DELETE FROM active_arc_run WHERE id = 1")
    suspend fun clear()

    @Query(
        """
        UPDATE active_arc_run
        SET currentStepIndex = :currentStepIndex,
            currentStepTitle = :currentStepTitle,
            currentTagName = :currentTagName,
            currentIsSoftMode = :currentIsSoftMode,
            updatedAt = :updatedAt
        WHERE id = 1
        """
    )
    suspend fun updateCurrentStep(
        currentStepIndex: Int,
        currentStepTitle: String,
        currentTagName: String,
        currentIsSoftMode: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
}