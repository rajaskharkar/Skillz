package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveCompletionDao
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveDao
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveSkippedCycleDao
import com.kingkharnivore.skillz.data.model.dao.shell.PearlLedgerDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserBadgeDao
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.data.model.entity.shell.PearlLedgerEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LookoutRepository @Inject constructor(
    private val db: SkillzDatabase,
    private val objectiveDao: ObjectiveDao,
    private val completionDao: ObjectiveCompletionDao,
    private val skippedCycleDao: ObjectiveSkippedCycleDao,
    private val pearlLedgerDao: PearlLedgerDao,
    private val badgeDao: UserBadgeDao
) {
    fun observeObjectives(): Flow<List<ObjectiveEntity>> = objectiveDao.observeActiveObjectives()
    fun observeCompletions(): Flow<List<ObjectiveCompletionEntity>> =
        completionDao.observeCompletions()
    fun observeSkippedCycles(): Flow<List<ObjectiveSkippedCycleEntity>> =
        skippedCycleDao.observeSkippedCycles()
    fun observeUnclaimedPearlTotal(): Flow<Int> = completionDao.observeUnclaimedPearlTotal()
    fun observeUnclaimedCompletionCount(): Flow<Int> = completionDao.observeUnclaimedCompletionCount()

    suspend fun getActiveObjectives(): List<ObjectiveEntity> = objectiveDao.getActiveObjectives()
    suspend fun getCompletions(): List<ObjectiveCompletionEntity> = completionDao.getCompletions()
    suspend fun getSkippedCycles(): List<ObjectiveSkippedCycleEntity> = skippedCycleDao.getSkippedCycles()
    suspend fun insertObjective(objective: ObjectiveEntity): Long =
        objectiveDao.insertObjective(objective)
    suspend fun archiveObjective(id: Long) =
        objectiveDao.archiveObjective(id, System.currentTimeMillis())

    suspend fun skipCycle(objectiveId: Long, periodStartMs: Long, periodEndMs: Long) {
        skippedCycleDao.insertSkippedCycle(
            ObjectiveSkippedCycleEntity(
                objectiveId = objectiveId,
                periodStartMs = periodStartMs,
                periodEndMs = periodEndMs,
                skippedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun applyCompletionGrant(
        grant: ObjectiveCompletionEntity,
        newCurrentStreak: Int?,
        newMaxStreak: Int?,
        newTotalCompletions: Int?
    ): Boolean = db.withTransaction {
        if (completionDao
            .getCompletion(
                grant.objectiveId, grant.periodStartMs, grant.periodEndMs
            ) != null) return@withTransaction false
        val inserted = completionDao
            .insertCompletion(
                grant.copy(
                    pearlsGranted = false, pearlsClaimed = false, pearlsClaimedAt = null
                )
            ) != -1L
        if (!inserted) return@withTransaction false

        incrementBadgeInTransaction(grant.badgeKey, grant.completedAt)

        if (newCurrentStreak != null && newMaxStreak != null && newTotalCompletions != null) {
            objectiveDao.updateRecurringStats(
                id = grant.objectiveId,
                currentStreak = newCurrentStreak,
                maxStreak = newMaxStreak,
                totalCompletions = newTotalCompletions,
                updatedAt = grant.completedAt
            )
        }
        true
    }

    suspend fun claimObjectivePearls(completionId: Long):
            Int = db.withTransaction {
        val completion = completionDao.getCompletionById(completionId)
            ?: return@withTransaction 0
        if (completion.pearlsClaimed) return@withTransaction 0
        val now = System.currentTimeMillis()
        if (completionDao.markPearlsClaimed(completion.id, now) != 1) return@withTransaction 0
        creditClaim(completion.finalRewardPearls, 1, "objective_completion", completion.id.toString(), now)
    }

    /** Claims every outstanding occurrence for one Objective definition atomically. */
    suspend fun claimAchievementRewards(badgeKey: String): Int = db.withTransaction {
        val aggregate = completionDao.getUnclaimedAggregateForBadge(badgeKey)
        if (aggregate.completionCount == 0) return@withTransaction 0
        val now = System.currentTimeMillis()
        check(completionDao.markBadgePearlsClaimed(badgeKey, now) == aggregate.completionCount)
        creditClaim(aggregate.pearlTotal, aggregate.completionCount, "objective_achievement", badgeKey, now)
    }

    /** Claims the exact snapshot of all outstanding Objective rewards in one transaction. */
    suspend fun claimAllObjectiveRewards(): Int = db.withTransaction {
        val aggregate = completionDao.getAllUnclaimedAggregate()
        if (aggregate.completionCount == 0) return@withTransaction 0
        val now = System.currentTimeMillis()
        check(completionDao.markAllPearlsClaimed(now) == aggregate.completionCount)
        creditClaim(aggregate.pearlTotal, aggregate.completionCount, "objective_claim_all", null, now)
    }

    suspend fun resetStreak(objectiveId: Long) {
        objectiveDao.resetStreak(objectiveId, System.currentTimeMillis())
    }

    private suspend fun incrementBadgeInTransaction(badgeId: String, now: Long) {
        val current = badgeDao.get(badgeId)
        badgeDao.upsert(
            current?.copy(count = current.count + 1, lastEarnedAt = now, isNew = true, viewedAt = null)
                ?: UserBadgeEntity(
                    badgeId = badgeId,
                    count = 1,
                    firstEarnedAt = now,
                    lastEarnedAt = now,
                    isNew = true
                )
        )
    }

    private suspend fun creditClaim(
        total: Int,
        count: Int,
        sourceType: String,
        sourceId: String?,
        now: Long
    ): Int {
        if (total > 0) {
            pearlLedgerDao.insert(
                PearlLedgerEntity(
                    id = UUID.randomUUID().toString(),
                    delta = total,
                    reason = "objective_completion_claim",
                    sourceType = sourceType,
                    sourceId = sourceId,
                    createdAt = now,
                    note = "$count Objective reward(s)"
                )
            )
        }
        return total
    }
}
