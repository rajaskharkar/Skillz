package com.kingkharnivore.skillz.data.repository.shell

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveProcessedSessionEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import java.time.LocalDate
import java.time.ZoneId
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

    @Test fun individualOccurrenceClaimCreditsExactlyOnce() = runBlocking {
        db.objectiveCompletionDao().insertCompletion(completion(1, 10, 17))
        assertEquals(17, repository.claimObjectivePearls(1))
        assertEquals(0, repository.claimObjectivePearls(1))
        assertEquals(17, db.pearlLedgerDao().getBalance())
        assertEquals(true, db.objectiveCompletionDao().getCompletionById(1)?.pearlsClaimed)
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
            mode = com.kingkharnivore.skillz.model.FlowMode.FLOW
        ))
        val session = requireNotNull(db.sessionDao().getSessionById(sessionId))
        val processor = ObjectiveCompletionProcessor(
            db,
            db.sessionDao(),
            db.objectiveProcessedSessionDao(),
            repository,
            ObjectiveProgressCalculator()
        )

        assertEquals(1, processor.reconcileUnprocessedSessions())
        assertEquals(0, processor.reconcileUnprocessedSessions())

        val completion = db.objectiveCompletionDao().getCompletions().single()
        assertEquals("objective_badge_7_daily", completion.badgeKey)
        assertEquals(false, completion.pearlsClaimed)
        assertEquals(1, db.userBadgeDao().get(completion.badgeKey)?.count)
        assertEquals(1, db.objectiveDao().getObjective(completion.objectiveId)?.totalCompletions)
        assertEquals(1, db.objectiveProcessedSessionDao().processedCount())
    }

    @Test fun recoveredHistoricalCompletionUsesHistoricalStreakMultiplier() = runBlocking {
        val day = 1_700_006_400_000L
        db.tagDao().insertTag(TagEntity(id = 8, name = "Run", createdAt = day))
        db.objectiveDao().insertObjective(ObjectiveEntity(
            journeyId = 8, journeyNameSnapshot = "Run", periodType = "daily",
            objectiveType = "recurring", targetDurationMs = 60_000, startAtMs = day,
            createdAt = day, updatedAt = day
        ))
        listOf(day + 60_000, day + 86_400_000 + 60_000).forEachIndexed { index, end ->
            db.sessionDao().insertSession(SessionEntity(
                title = "Run", description = "", tagId = 8, startTime = end - 60_000,
                endTime = end, durationMs = 60_000, mode = com.kingkharnivore.skillz.model.FlowMode.FLOW, createdAt = end + index
            ))
        }
        val processor = ObjectiveCompletionProcessor(
            db, db.sessionDao(), db.objectiveProcessedSessionDao(), repository,
            ObjectiveProgressCalculator()
        )
        assertEquals(2, processor.reconcileUnprocessedSessions())
        val rows = db.objectiveCompletionDao().getCompletions().sortedBy { it.periodStartMs }
        assertEquals(2, rows.size)
        assertEquals(0, rows[0].streakBeforeCompletion)
        assertEquals(1, rows[1].streakBeforeCompletion)
        assertEquals(1.1, rows[1].streakMultiplier, 0.0)
        assertEquals(1, rows[1].finalRewardPearls)
        assertEquals(2, db.objectiveDao().getObjective(rows[0].objectiveId)?.maxStreak)
        assertEquals(2, db.objectiveDao().getObjective(rows[0].objectiveId)?.totalCompletions)
        assertEquals(true, requireNotNull(db.objectiveDao().getObjective(rows[0].objectiveId)).updatedAt >= day)
    }

    @Test fun hotPathSelectsOnlyUnprocessedSessions() = runBlocking {
        db.tagDao().insertTag(TagEntity(id = 9, name = "History", createdAt = 1))
        repeat(450) { index ->
            val id = db.sessionDao().insertSession(SessionEntity(
                title = "Flow", description = "", tagId = 9, startTime = index * 2L,
                endTime = index * 2L + 1, durationMs = 1, mode = com.kingkharnivore.skillz.model.FlowMode.FLOW
            ))
            if (index < 10) db.objectiveProcessedSessionDao().markProcessed(
                ObjectiveProcessedSessionEntity(id, index.toLong())
            )
        }
        val processor = ObjectiveCompletionProcessor(
            db, db.sessionDao(), db.objectiveProcessedSessionDao(), repository,
            ObjectiveProgressCalculator()
        )
        val firstBatch = db.objectiveProcessedSessionDao().getUnprocessedRegularSessions(
            com.kingkharnivore.skillz.domain.lookout.OBJECTIVE_RECONCILIATION_BATCH_SIZE
        )
        assertEquals(200, firstBatch.size)
        assertEquals(firstBatch.sortedWith(compareBy<SessionEntity> { it.endTime }.thenBy { it.id }), firstBatch)
        assertEquals(440, processor.reconcileUnprocessedSessions())
        assertEquals(450, db.objectiveProcessedSessionDao().processedCount())
    }

    @Test fun processedFlowDoesNotCompleteObjectiveCreatedLater() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 11, name = "Piano", createdAt = start))
        val crossingTime = start + 1
        val createdAt = System.currentTimeMillis()
        val sessionId = db.sessionDao().insertSession(regularSession(11, crossingTime, 60))
        db.objectiveProcessedSessionDao().markProcessed(ObjectiveProcessedSessionEntity(sessionId, start + 4_000_000))
        val processor = processor()

        val objectiveId = processor.createObjectiveAndReconcile(
            objective(11, "Piano", start, 60).copy(createdAt = createdAt, updatedAt = createdAt)
        )
        assertEquals(false, processor.reconcileNewObjective(objectiveId))

        assertEquals(0, db.objectiveCompletionDao().getCompletions().size)
        assertEquals(null, db.userBadgeDao().get("objective_badge_11_daily"))
        assertEquals(1, db.objectiveProcessedSessionDao().processedCount())
        val persistedObjective = requireNotNull(db.objectiveDao().getObjective(objectiveId))
        assertEquals(true, persistedObjective.updatedAt >= persistedObjective.createdAt)
    }

    @Test fun partialProcessedProgressCompletesOnceAfterLaterRegularFlow() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 12, name = "Piano", createdAt = start))
        val firstId = db.sessionDao().insertSession(regularSession(12, start + 30 * 60_000 + 1, 30))
        db.objectiveProcessedSessionDao().markProcessed(ObjectiveProcessedSessionEntity(firstId, start + 2_000_000))
        val processor = processor()
        processor.createObjectiveAndReconcile(objective(12, "Piano", start, 60))
        assertEquals(0, db.objectiveCompletionDao().getCompletions().size)
        assertEquals(null, db.userBadgeDao().get("objective_badge_12_daily"))

        db.sessionDao().insertSession(regularSession(12, start + 60 * 60_000 + 2, 30))
        assertEquals(1, processor.reconcileUnprocessedSessions())
        assertEquals(1, db.objectiveCompletionDao().getCompletions().size)
        assertEquals(1, db.userBadgeDao().get("objective_badge_12_daily")?.count)
    }

    @Test fun targetedReconciliationExcludesSoftUnrelatedAndOutsideWindowFlows() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 13, name = "Piano", createdAt = start))
        db.tagDao().insertTag(TagEntity(id = 14, name = "Other", createdAt = start))
        db.sessionDao().insertSession(regularSession(14, start + 1, 60))
        db.sessionDao().insertSession(regularSession(13, start + 2, 60).copy(mode = com.kingkharnivore.skillz.model.FlowMode.SOFT))
        db.sessionDao().insertSession(regularSession(13, start - 1, 60))

        processor().createObjectiveAndReconcile(objective(13, "Piano", start, 60))
        assertEquals(0, db.objectiveCompletionDao().getCompletions().size)
    }

    @Test fun safeArchiveMaterializesEarnedRewardBeforeArchivingAndRemainsIdempotent() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 15, name = "Archive", createdAt = start))
        val objectiveId = repository.insertObjective(objective(15, "Archive", start, 60))
        db.sessionDao().insertSession(regularSession(15, start + 60 * 60_000 + 1, 60))
        val processor = processor()

        assertEquals(true, processor.archiveObjectiveSafely(objectiveId))
        assertEquals(false, processor.archiveObjectiveSafely(objectiveId))
        val completion = db.objectiveCompletionDao().getCompletions().single()
        assertEquals(false, completion.pearlsClaimed)
        assertEquals(1, db.userBadgeDao().get(completion.badgeKey)?.count)
        assertEquals(true, db.objectiveDao().getObjective(objectiveId)?.isArchived)
        assertEquals(1, processor.reconcileUnprocessedSessions())
        assertEquals(1, db.objectiveCompletionDao().getCompletions().size)
        assertEquals(1, db.userBadgeDao().get(completion.badgeKey)?.count)
    }

    @Test fun safeArchiveDoesNotFabricateRewardForPartialProgress() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 16, name = "Partial", createdAt = start))
        val objectiveId = repository.insertObjective(objective(16, "Partial", start, 60))
        db.sessionDao().insertSession(regularSession(16, start + 30 * 60_000 + 1, 30))

        assertEquals(true, processor().archiveObjectiveSafely(objectiveId))
        assertEquals(0, db.objectiveCompletionDao().getCompletions().size)
        assertEquals(null, db.userBadgeDao().get("objective_badge_16_daily"))
        assertEquals(true, db.objectiveDao().getObjective(objectiveId)?.isArchived)
    }

    @Test fun overlappingReconciliationCallsAreSingleFlightAndIdempotent() = runBlocking {
        db.tagDao().insertTag(TagEntity(id = 17, name = "Retry", createdAt = 1))
        repeat(20) { index ->
            db.sessionDao().insertSession(regularSession(17, index + 1L, 1))
        }
        val processor = processor()
        val processed = listOf(
            async { processor.reconcileUnprocessedSessions() },
            async { processor.reconcileUnprocessedSessions() }
        ).awaitAll()
        assertEquals(20, processed.sum())
        assertEquals(20, db.objectiveProcessedSessionDao().processedCount())
    }

    @Test fun mixedRewardsClaimAtomicallyAndCannotBeClaimedTwice() = runBlocking {
        db.objectiveCompletionDao().insertCompletion(completion(1, 10, 30).copy(finalRewardPoints = 30))
        db.objectiveCompletionDao().insertCompletion(completion(2, 20, 0).copy(
            flowMode = com.kingkharnivore.skillz.model.FlowMode.SOFT, finalRewardDrops = 3600
        ))
        db.objectiveCompletionDao().insertCompletion(completion(3, 30, 60).copy(
            flowMode = com.kingkharnivore.skillz.model.FlowMode.POWER, finalRewardPebbles = 60
        ))
        repository.claimAllObjectiveRewards()
        assertEquals(90, db.pearlLedgerDao().getBalance())
        assertEquals(3600L, db.stillwaterLedgerDao().getTotal())
        assertEquals(60, db.powerRewardDao().entry("objective:3")?.delta)
        assertEquals(0, repository.claimAllObjectiveRewards())
        assertEquals(0, repository.claimObjectivePearls(2))
        assertEquals(3600L, db.stillwaterLedgerDao().getTotal())
        assertEquals(3, db.objectiveCompletionDao().getCompletions().count { it.pearlsClaimed })
    }

    @Test fun softAndPowerSessionsMaterializeOnlyMatchingObjectives() = runBlocking {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.tagDao().insertTag(TagEntity(id = 18, name = "Modes", createdAt = start))
        com.kingkharnivore.skillz.model.FlowMode.entries.forEach { mode ->
            repository.insertObjective(objective(18, "Modes", start, 1).copy(flowMode = mode))
        }
        listOf(com.kingkharnivore.skillz.model.FlowMode.SOFT, com.kingkharnivore.skillz.model.FlowMode.POWER).forEach { mode ->
            db.sessionDao().insertSession(regularSession(18, start + 120_000, 1).copy(mode = mode))
        }
        assertEquals(2, processor().reconcileUnprocessedSessions())
        val rewards = db.objectiveCompletionDao().getCompletions()
        assertEquals(2, rewards.size)
        assertEquals(60L, rewards.single { it.flowMode == com.kingkharnivore.skillz.model.FlowMode.SOFT }.finalRewardDrops)
        assertEquals(1, rewards.single { it.flowMode == com.kingkharnivore.skillz.model.FlowMode.POWER }.finalRewardPebbles)
    }

    @Test fun claimedPointsAppearInHistoryWithoutChangingRawSessionRewards() = runBlocking {
        db.tagDao().insertTag(TagEntity(id = 19, name = "Points", createdAt = 1))
        val sessionId = db.sessionDao().insertSession(regularSession(19, 120_000, 1).copy(scyraPoints = 5))
        db.objectiveCompletionDao().insertCompletion(completion(1, 19, 2).copy(finalRewardPoints = 2, sourceSessionId = sessionId))
        val chronicles = com.kingkharnivore.skillz.data.repository.ChronicleRepository(db, db.chronicleDao())
        val flows = com.kingkharnivore.skillz.data.repository.FlowRepository(
            db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles
        )
        assertEquals(5, flows.getAllSessions().first().single().scyraPoints)
        repository.claimObjectivePearls(1)
        assertEquals(7, flows.getAllSessions().first().single().scyraPoints)
        assertEquals(5, db.sessionDao().getSessionById(sessionId)?.scyraPoints)
        // A delayed Health adjustment updates the underlying Flow while preserving the claimed bonus.
        db.sessionDao().updateRewardPoints(sessionId, 8, 0)
        assertEquals(10, flows.getAllSessions().first().single().scyraPoints)
        assertEquals(0, repository.claimAllObjectiveRewards())
        assertEquals(10, flows.getSessionsForTag(19).first().single().scyraPoints)
    }

    @Test fun newObjectiveIgnoresOldFlowAndAwardsEarlyBonusFromNewFlow() = runBlocking {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val minute = 60_000L
        db.tagDao().insertTag(TagEntity(id = 20, name = "New goal", createdAt = start))
        // Started before creation but finished afterwards: none of it counts.
        db.sessionDao().insertSession(regularSession(20, start + (7 * 60 + 25) * minute, 30))
        val objectiveId = processor().createObjectiveAndReconcile(objective(20, "New goal", start, 60).copy(
            objectiveType = "one_time", createdAt = start + 7 * 60 * minute, targetMinuteOfDay = 9 * 60
        ))
        db.sessionDao().insertSession(regularSession(20, start + (9 * 60 + 15) * minute, 30))
        processor().reconcileUnprocessedSessions()
        assertEquals(0, db.objectiveCompletionDao().getCompletions().size)
        db.sessionDao().insertSession(regularSession(20, start + 10 * 60 * minute, 30))
        processor().reconcileUnprocessedSessions()
        val reward = db.objectiveCompletionDao().getCompletions().single()
        assertEquals(objectiveId, reward.objectiveId)
        assertEquals(60 * minute, reward.achievedDurationMs)
        assertEquals(true, reward.startedBeforeTarget)
        assertEquals(120, reward.finalRewardPearls)
        assertEquals(240, repository.claimObjectivePearls(reward.id))
        assertEquals(0, repository.claimObjectivePearls(reward.id))
    }

    @Test fun failedClaimRollsBackReportsFailureAndCanBeRetried() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db.objectiveCompletionDao().insertCompletion(completion(1, 10, 17).copy(streakMultiplier = 1.2))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_objective_credit BEFORE INSERT ON pearl_ledger BEGIN SELECT RAISE(ABORT, 'Injected claim failure'); END")
        val chronicles = com.kingkharnivore.skillz.data.repository.ChronicleRepository(db, db.chronicleDao())
        val flows = com.kingkharnivore.skillz.data.repository.FlowRepository(
            db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles
        )
        val store = androidx.lifecycle.ViewModelStore()
        val vm = withContext(Dispatchers.Main) {
            com.kingkharnivore.skillz.viewmodel.shell.LookoutViewModel(
                context, flows, com.kingkharnivore.skillz.data.repository.JourneyRepository(db.tagDao()),
                repository, ObjectiveProgressCalculator(), processor(),
                com.kingkharnivore.skillz.domain.lookout.ObjectiveReminderScheduler(
                    context, db, ObjectiveProgressCalculator(), com.kingkharnivore.skillz.utils.user.UserPrefs(context)
                )
            ).also { store.put("lookout", it) }
        }
        try {
            withTimeout(10_000) { vm.uiState.first { !it.isLoading } }
            val error = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(10_000) { vm.events.first() } }
            withContext(Dispatchers.Main) { vm.claimReward(1) }.join()
            assertEquals(context.getString(com.kingkharnivore.skillz.R.string.lookout_claim_failed), error.await())
            assertEquals(false, db.objectiveCompletionDao().getCompletionById(1)?.pearlsClaimed)
            assertEquals(0, db.pearlLedgerDao().getBalance())
            assertEquals(false, vm.uiState.value.claimInProgress)
            assertEquals(null, vm.uiState.value.rewardDialog)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_objective_credit")
            withContext(Dispatchers.Main) { vm.claimReward(1) }.join()
            assertEquals(true, db.objectiveCompletionDao().getCompletionById(1)?.pearlsClaimed)
            assertEquals(17, db.pearlLedgerDao().getBalance())
            assertEquals(context.getString(com.kingkharnivore.skillz.R.string.lookout_rewards_claimed), vm.uiState.value.rewardDialog?.title)
            assertEquals(context.getString(com.kingkharnivore.skillz.R.string.lookout_streak_bonus_percent, 20), vm.uiState.value.rewardDialog?.streakBonus)
        } finally { withContext(Dispatchers.Main) { store.clear() } }
    }

    private fun processor() = ObjectiveCompletionProcessor(
        db, db.sessionDao(), db.objectiveProcessedSessionDao(), repository,
        ObjectiveProgressCalculator()
    )

    private fun objective(journeyId: Long, name: String, start: Long, minutes: Long) = ObjectiveEntity(
        journeyId = journeyId, journeyNameSnapshot = name, periodType = "daily",
        objectiveType = "recurring", targetDurationMs = minutes * 60_000,
        startAtMs = start, createdAt = start, updatedAt = start
    )

    private fun regularSession(tagId: Long, end: Long, minutes: Long) = SessionEntity(
        title = "Flow", description = "", tagId = tagId,
        startTime = end - minutes * 60_000, endTime = end,
        durationMs = minutes * 60_000, mode = com.kingkharnivore.skillz.model.FlowMode.FLOW
    )

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
