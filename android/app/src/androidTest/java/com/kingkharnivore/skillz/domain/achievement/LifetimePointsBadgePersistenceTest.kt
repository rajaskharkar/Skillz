package com.kingkharnivore.skillz.domain.achievement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.shell.LookoutRepository
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.model.FlowMode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class LifetimePointsBadgePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "lifetime-points-${UUID.randomUUID()}"
    private lateinit var db: SkillzDatabase
    private var tagId = 0L
    private fun open() { db = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build() }
    private fun flows() = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db,
        db.chronicleDao(), ChronicleRepository(db, db.chronicleDao()))
    private fun shell() = ShellRepository(db, db.sessionDao(), db.pearlLedgerDao(), db.shellFindInstanceDao(),
        db.shellFindStackDao(), db.shellPlacementDao(), db.shellFindUpgradeDao(), db.userBadgeDao(),
        db.userDiscoveryDao(), db.stillwaterLedgerDao(), db.stillwaterPreferenceDao(), db.userShellRoomStateDao(),
        db.objectiveCompletionDao(), db.achievementDao())
    private fun lookout() = LookoutRepository(db, db.objectiveDao(), db.objectiveCompletionDao(),
        db.objectiveSkippedCycleDao(), db.pearlLedgerDao(), db.userBadgeDao())
    private fun session(points: Int, mode: FlowMode = FlowMode.FLOW) = SessionEntity(
        title = "Practice", description = "", tagId = tagId, startTime = 100, endTime = 200,
        durationMs = 100, scyraPoints = points, mode = mode)
    private fun receipt(id: Long, source: Long?, points: Int, claimed: Boolean = false) = ObjectiveCompletionEntity(
        objectiveId = id, journeyId = tagId, journeyNameSnapshot = "Practice", periodType = "daily",
        objectiveType = "one_time", periodStartMs = 0, periodEndMs = 1000, completedAt = 200,
        achievedDurationMs = 100, targetDurationMs = 100, baseRewardPearls = 0,
        streakBeforeCompletion = 0, streakMultiplier = 1.0, finalRewardPearls = 0,
        badgeKey = "objective_$id", badgeLabelSnapshot = "Objective", pearlsClaimed = claimed,
        finalRewardPoints = points, sourceSessionId = source)
    private suspend fun awards() = db.userBadgeDao().getAll().filter { it.badgeId in LifetimePointsBadges.byId }
    @Before fun before() = runBlocking { open(); tagId = db.tagDao().insertTag(TagEntity(name = "Practice")) }
    @After fun after() { db.close(); context.deleteDatabase(name) }

    @Test fun flowCompletionAwardsAtomicallyAndRetriesDeletionAndReopenKeepOneAward() = runBlocking {
        val id = flows().addSessionAndPromoteChronicle("flow", session(1000))
        val award = awards().single()
        assertEquals(1, award.count)
        assertTrue(award.isNew)
        assertEquals(id, flows().addSessionAndPromoteChronicle("flow", session(1000)))
        shell().backfillAchievements()
        flows().deleteSessionAndCleanupTag(id)
        shell().backfillAchievements()
        db.close(); open()
        assertEquals(0L, db.sessionDao().lifetimeScyraPoints())
        assertEquals(listOf(award), awards())
    }

    @Test fun historicalTotalsAboveIntRangeUnlockAllSixteenWithoutChangingExistingAwards() = runBlocking {
        FlowMode.entries.forEach { db.sessionDao().insertSession(session(1_000_000_000, it)) }
        assertEquals(3_000_000_000L, db.sessionDao().lifetimeScyraPoints())
        shell().backfillAchievements()
        val first = awards().sortedBy { it.badgeId }
        assertEquals(16, first.size)
        assertTrue(first.all { it.count == 1 && !it.isNew && it.timestampConfidence == AchievementTimestampConfidence.UNKNOWN })
        shell().backfillAchievements()
        db.close(); open()
        assertEquals(first, awards().sortedBy { it.badgeId })
    }

    @Test fun objectiveClaimsIncludeOnlyRetainedSourcesWithoutDoubleCountingFlowPoints() = runBlocking {
        val id = flows().addSessionAndPromoteChronicle("flow", session(900))
        val first = db.objectiveCompletionDao().insertCompletion(receipt(1, id, 50))
        db.objectiveCompletionDao().insertCompletion(receipt(2, id, 50))
        db.objectiveCompletionDao().insertCompletion(receipt(3, null, 100_000, true))
        db.objectiveCompletionDao().insertCompletion(receipt(4, 9999, 100_000, true))
        assertEquals(900L, db.sessionDao().lifetimeScyraPoints())
        assertTrue(awards().isEmpty())
        lookout().claimObjectivePearls(first)
        assertEquals(950L, db.sessionDao().observeLifetimeScyraPoints().first())
        assertTrue(awards().isEmpty())
        lookout().claimAllObjectiveRewards()
        assertEquals(1000L, db.sessionDao().lifetimeScyraPoints())
        val award = awards().single()
        lookout().claimAllObjectiveRewards()
        assertEquals(listOf(award), awards())
        db.sessionDao().deleteSession(id)
        assertEquals(0L, db.sessionDao().lifetimeScyraPoints())
        shell().backfillAchievements()
        assertEquals(listOf(award), awards())
    }
    @Test fun arcAndDelayedMovementUpdatesAwardTheirNewMilestones() = runBlocking {
        val id = flows().addSessionAndPromoteChronicle("flow", session(900))
        flows().updateArcFields(id, 77, 1, 2.0, 900, 1800)
        assertEquals("lifetime_points_1000", awards().single().badgeId)
        val health = com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository(
            db, db.flowHealthDao(), db.sessionDao(), db.pearlLedgerDao())
        val snapshot = com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity(
            id, true, true, com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSyncStatus.CAPTURED,
            8200L, 8200, 8200, 0, 300, 300, 300, null, 1, 100, 200, null)
        val breakdown = com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity(
            id, 900, 0, 0, 0, 8200, 9100, 1.0, 1.0, 1.0, 900, 10000, 0, false)
        health.applyDelayedMovementUpdateTransactionally(snapshot, breakdown, 10000, 900, 0, null)
        val earned = awards().sortedBy { it.badgeId }
        assertEquals(2, earned.size)
        assertTrue(earned.all { it.count == 1 && it.isNew })
        health.applyDelayedMovementUpdateTransactionally(snapshot, breakdown, 10000, 900, 0, null)
        assertEquals(earned, awards().sortedBy { it.badgeId })
    }

}
