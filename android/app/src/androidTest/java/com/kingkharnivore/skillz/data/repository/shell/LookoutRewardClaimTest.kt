package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
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
