package com.kingkharnivore.skillz.data.model.dao.shell

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import kotlinx.coroutines.flow.Flow

data class ObjectiveClaimAggregate(val pearlTotal: Int, val completionCount: Int)

@Dao
interface ObjectiveDao {
    @Query("SELECT * FROM objectives WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeActiveObjectives(): Flow<List<ObjectiveEntity>>

    @Query("SELECT * FROM objectives WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getActiveObjectives(): List<ObjectiveEntity>

    @Query("SELECT * FROM objectives WHERE id = :id LIMIT 1")
    suspend fun getObjective(id: Long): ObjectiveEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertObjective(entity: ObjectiveEntity): Long

    @Query("UPDATE objectives SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveObjective(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE objectives
        SET currentStreak = :currentStreak,
            maxStreak = :maxStreak,
            totalCompletions = :totalCompletions,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateRecurringStats(
        id: Long,
        currentStreak: Int,
        maxStreak: Int,
        totalCompletions: Int,
        updatedAt: Long
    )

    @Query("UPDATE objectives SET currentStreak = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun resetStreak(id: Long, updatedAt: Long)
}

@Dao
interface ObjectiveCompletionDao {
    @Query("SELECT COALESCE(SUM(finalRewardPearls), 0) FROM objective_completions WHERE pearlsClaimed = 0")
    fun observeUnclaimedPearlTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM objective_completions WHERE pearlsClaimed = 0")
    fun observeUnclaimedCompletionCount(): Flow<Int>

    @Query("SELECT * FROM objective_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<ObjectiveCompletionEntity>>

    @Query("SELECT * FROM objective_completions ORDER BY completedAt DESC")
    suspend fun getCompletions(): List<ObjectiveCompletionEntity>

    @Query(
        """
        SELECT * FROM objective_completions
        WHERE objectiveId = :objectiveId AND periodStartMs = :periodStartMs AND periodEndMs = :periodEndMs
        LIMIT 1
        """
    )
    suspend fun getCompletion(objectiveId: Long, periodStartMs: Long, periodEndMs: Long):
            ObjectiveCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(entity: ObjectiveCompletionEntity): Long

    @Query("SELECT * FROM objective_completions WHERE id = :id LIMIT 1")
    suspend fun getCompletionById(id: Long): ObjectiveCompletionEntity?

    @Query("SELECT COALESCE(SUM(finalRewardPearls), 0) AS pearlTotal, COUNT(*) AS completionCount FROM objective_completions WHERE pearlsClaimed = 0")
    suspend fun getAllUnclaimedAggregate(): ObjectiveClaimAggregate

    @Query("SELECT COALESCE(SUM(finalRewardPearls), 0) AS pearlTotal, COUNT(*) AS completionCount FROM objective_completions WHERE badgeKey = :badgeKey AND pearlsClaimed = 0")
    suspend fun getUnclaimedAggregateForBadge(badgeKey: String): ObjectiveClaimAggregate

    @Query("UPDATE objective_completions SET pearlsGranted = 1, pearlsClaimed = 1, pearlsClaimedAt = :claimedAt WHERE id = :id AND pearlsClaimed = 0")
    suspend fun markPearlsClaimed(id: Long, claimedAt: Long): Int

    @Query("UPDATE objective_completions SET pearlsGranted = 1, pearlsClaimed = 1, pearlsClaimedAt = :claimedAt WHERE badgeKey = :badgeKey AND pearlsClaimed = 0")
    suspend fun markBadgePearlsClaimed(badgeKey: String, claimedAt: Long): Int

    @Query("UPDATE objective_completions SET pearlsGranted = 1, pearlsClaimed = 1, pearlsClaimedAt = :claimedAt WHERE pearlsClaimed = 0")
    suspend fun markAllPearlsClaimed(claimedAt: Long): Int
}

@Dao
interface ObjectiveSkippedCycleDao {
    @Query("SELECT * FROM objective_skipped_cycles ORDER BY skippedAt DESC")
    fun observeSkippedCycles(): Flow<List<ObjectiveSkippedCycleEntity>>

    @Query("SELECT * FROM objective_skipped_cycles ORDER BY skippedAt DESC")
    suspend fun getSkippedCycles(): List<ObjectiveSkippedCycleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkippedCycle(entity: ObjectiveSkippedCycleEntity): Long
}
