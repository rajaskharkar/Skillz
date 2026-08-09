package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LookoutRewardClaimTest {
    private lateinit var db: SkillzDatabase
    private lateinit var repository: LookoutRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SkillzDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = LookoutRepository(
            db,
            db.objectiveDao(),
            db.objectiveCompletionDao(),
            db.objectiveSkippedCycleDao(),
            db.pearlLedgerDao(),
            db.userBadgeDao()
        )
    }

    @After fun tearDown() = db.close()

    @Test fun claimAllCreditsExactSnapshotOnceAndEmptyRetryNoOps() = runBlocking {
        db.objectiveCompletionDao().insertCompletion(completion(1, 10, 60))
        db.objectiveCompletionDao().insertCompletion(completion(2, 20, 40))

        assertEquals(100, repository.claimAllObjectiveRewards())
        assertEquals(100, db.pearlLedgerDao().getBalance())
        assertEquals(0, repository.claimAllObjectiveRewards())
        assertEquals(100, db.pearlLedgerDao().getBalance())
        assertEquals(2, db.objectiveCompletionDao().getCompletions().count { it.pearlsClaimed })
    }

    @Test fun claimAllScalesBeyondSqliteBindVariableLimit() = runBlocking {
        repeat(1_200) { index ->
            db.objectiveCompletionDao().insertCompletion(completion(index + 1L, index + 10L, 2))
        }
        assertEquals(2_400, repository.claimAllObjectiveRewards())
        assertEquals(1_200, db.objectiveCompletionDao().getCompletions().count { it.pearlsClaimed })
        assertEquals(0, repository.claimAllObjectiveRewards())
        assertEquals(2_400, db.pearlLedgerDao().getBalance())
    }

    @Test fun achievementClaimUsesBadgeKeyAcrossRecreatedObjectives() = runBlocking {
        val badgeKey = "objective_badge_10_daily"
        db.objectiveCompletionDao().insertCompletion(completion(1, 100, 30).copy(badgeKey = badgeKey))
        db.objectiveCompletionDao().insertCompletion(completion(2, 200, 40).copy(badgeKey = badgeKey))
        assertEquals(70, repository.claimAchievementRewards(badgeKey))
        assertEquals(2, db.objectiveCompletionDao().getCompletions().count { it.pearlsClaimed })
        assertEquals(0, repository.claimAchievementRewards(badgeKey))
    }

    @Test fun persistedFlowMaterializesCompletionWithoutLookoutAndRetryIsIdempotent() = runBlocking {
        val day = 1_700_000_000_000L
        db.tagDao().insertTag(TagEntity(id = 7, name = "Piano", createdAt = day))
        db.objectiveDao().insertObjective(ObjectiveEntity(
            journeyId = 7,
            journeyNameSnapshot = "Piano",
            periodType = "daily",
            objectiveType = "recurring",
            targetDurationMs = 60_000,
            startAtMs = day,
            createdAt = day,
            updatedAt = day
        ))
        val sessionId = db.sessionDao().insertSession(SessionEntity(
            title = "Practice",
            description = "",
            tagId = 7,
            startTime = day + 1_000,
            endTime = day + 61_000,
            durationMs = 60_000,
            surgePoints = 0,
            scyraPoints = 0,
            isSoftMode = false
        ))
        val session = requireNotNull(db.sessionDao().getSessionById(sessionId))
        val processor = ObjectiveCompletionProcessor(
            FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db),
            repository,
            ObjectiveProgressCalculator()
        )

        processor.processCompletedSession(session)
        processor.processCompletedSession(session)

        val completion = db.objectiveCompletionDao().getCompletions().single()
        assertEquals("objective_badge_7_daily", completion.badgeKey)
        assertEquals(false, completion.pearlsClaimed)
        assertEquals(1, db.userBadgeDao().get(completion.badgeKey)?.count)
        assertEquals(1, db.objectiveDao().getObjective(completion.objectiveId)?.totalCompletions)
    }

    private fun completion(id: Long, objectiveId: Long, pearls: Int) = ObjectiveCompletionEntity(
        id = id,
        objectiveId = objectiveId,
        journeyId = objectiveId,
        journeyNameSnapshot = "Journey $objectiveId",
        periodType = "daily",
        objectiveType = "recurring",
        periodStartMs = id * 1_000,
        periodEndMs = id * 1_000 + 999,
        completedAt = id * 1_000 + 500,
        achievedDurationMs = 60_000,
        targetDurationMs = 60_000,
        baseRewardPearls = pearls,
        streakBeforeCompletion = 0,
        streakMultiplier = 1.0,
        finalRewardPearls = pearls,
        badgeKey = "badge-$objectiveId",
        badgeLabelSnapshot = "Daily Objective"
    )
}
