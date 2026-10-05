package com.kingkharnivore.skillz.domain.power

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.repository.shell.*
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.model.entity.health.*
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PowerPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "power-test-${UUID.randomUUID()}"
    private lateinit var db: SkillzDatabase
    private lateinit var shell: ShellRepository
    private lateinit var rewards: ShellRewardOrchestrator
    private fun open() {
        db = Room.databaseBuilder(context,SkillzDatabase::class.java,name).build()
        shell = ShellRepository(db,db.sessionDao(),db.pearlLedgerDao(),db.shellFindInstanceDao(),db.shellFindStackDao(),db.shellPlacementDao(),db.shellFindUpgradeDao(),db.userBadgeDao(),db.userDiscoveryDao(),db.stillwaterLedgerDao(),db.stillwaterPreferenceDao(),db.userShellRoomStateDao(),db.objectiveCompletionDao(),db.achievementDao())
        val lookout = LookoutRepository(db,db.objectiveDao(),db.objectiveCompletionDao(),db.objectiveSkippedCycleDao(),db.pearlLedgerDao(),db.userBadgeDao())
        rewards = ShellRewardOrchestrator(db,shell,ShellRewardEventRecorder(db.shellRewardEventDao()),ObjectiveCompletionProcessor(db,db.sessionDao(),db.objectiveProcessedSessionDao(),lookout,ObjectiveProgressCalculator()))
    }
    @Before fun setup() { open(); runBlocking { db.tagDao().insertTag(TagEntity(1,"Power",1)) } }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }
    private suspend fun session(mode: FlowMode, score: Int): SessionEntity {
        val s = SessionEntity(title="Resolve",description="",tagId=1,startTime=1,endTime=600_001,durationMs=600_000,scyraPoints=score,mode=mode)
        return s.copy(id=db.sessionDao().insertSession(s))
    }
    @Test fun rewardsMirrorExactlyAndReplayAfterRestart() = runBlocking {
        val samples = listOf(0,1,15,57,72,999)
        samples.forEach { points ->
            val flow = session(FlowMode.POWER,points)
            coroutineScope { List(3) { async { rewards.onSessionCompleted(flow) } }.awaitAll() }
            assertEquals(points, db.powerRewardDao().reward(flow.id)?.pebbles)
            assertEquals(points, db.powerRewardDao().reward(flow.id)?.pearls)
        }
        assertEquals(samples.sum(),shell.getPearlBalance())
        assertEquals(samples.sum(),db.powerRewardDao().balance())
        val stored = db.sessionDao().completedSessions()
        db.close(); open()
        stored.forEach { rewards.onSessionCompleted(it) }
        assertEquals(samples.sum(),db.powerRewardDao().balance())
        val before = db.powerRewardDao().balance()
        rewards.onSessionCompleted(session(FlowMode.FLOW,20))
        rewards.onSessionCompleted(session(FlowMode.SOFT,0))
        assertEquals(before,db.powerRewardDao().balance())
        assertEquals(1,db.userBadgeDao().get("power_spark")?.count)
    }
    @Test fun rewardFailureRollsBackCurrencyAndRetriesDurably() = runBlocking {
        val s = session(FlowMode.POWER,57)
        db.powerRewardDao().enqueue(SessionShellRewardEntity(sessionId=s.id,queuedAt=1))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_pebble BEFORE INSERT ON pebble_ledger BEGIN SELECT RAISE(ABORT, 'simulated'); END")
        assertTrue(runCatching { rewards.onSessionCompleted(s) }.isFailure)
        assertEquals(0,shell.getPearlBalance())
        assertNotNull(db.sessionDao().getSessionById(s.id))
        assertFalse(db.powerRewardDao().reward(s.id)!!.completed)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_pebble")
        db.close(); open()
        rewards.retryPending()
        assertEquals(57,shell.getPearlBalance())
        assertEquals(57,db.powerRewardDao().balance())
    }
    @Test fun purchasesAreAtomicConcurrentReplaySafeAndPersist() = runBlocking {
        db.powerRewardDao().insert(PebbleLedgerEntity("seed",360,"test",1))
        val results = coroutineScope { List(4) { i -> async { runCatching { shell.purchaseRedCreature("triassic_saturnalia","buy-$i") } } }.awaitAll() }
        assertEquals(2,results.count { it.isSuccess })
        assertEquals(0,db.powerRewardDao().balance())
        assertEquals(2,db.shellFindInstanceDao().getAll().size)
        assertEquals(1,db.userBadgeDao().get("red_first_footprint")?.count)
        val first = results.first { it.isSuccess }.getOrThrow()
        assertEquals(first.instanceId,shell.purchaseRedCreature(first.findId,first.sourceId!!).instanceId)
        assertTrue(runCatching { shell.purchaseRedCreature("not-a-dinosaur","invalid") }.isFailure)
        db.close(); open()
        assertEquals(0,db.powerRewardDao().balance())
        assertEquals(2,db.shellFindInstanceDao().getAll().size)
        assertTrue(runCatching { shell.purchaseRedCreature("triassic_saturnalia","overdraw") }.isFailure)
    }
    @Test fun purchaseFailureRollsBackOwnershipAndBadgeThenSharedMasterySurvivesRelease() = runBlocking {
        db.powerRewardDao().insert(PebbleLedgerEntity("seed",180,"test",1))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_purchase BEFORE INSERT ON pebble_ledger WHEN NEW.delta < 0 BEGIN SELECT RAISE(ABORT, 'simulated'); END")
        assertTrue(runCatching { shell.purchaseRedCreature("triassic_saturnalia","buy") }.isFailure)
        assertEquals(180,db.powerRewardDao().balance())
        assertTrue(db.shellFindInstanceDao().getAll().isEmpty())
        assertNull(db.userBadgeDao().get("red_first_footprint"))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_purchase")
        val instance = shell.purchaseRedCreature("triassic_saturnalia","buy")
        shell.addPearls(1_000_000,"test","test","growth")
        db.shellFindInstanceDao().updateAnimalLevel(instance.instanceId,98)
        shell.growCreature(instance.instanceId,"mastery")
        shell.growCreature(instance.instanceId,"mastery")
        assertEquals(1,db.achievementDao().getMasteries().count { it.speciesId == instance.findId })
        shell.releaseCreature(instance.instanceId)
        db.close(); open()
        assertEquals(1,db.achievementDao().getMasteries().count { it.speciesId == instance.findId })
        assertEquals(CreatureStatus.RELEASED,db.shellFindInstanceDao().getAll().single().creatureStatus)
    }
    @Test fun mixedArcAndOngoingPowerRoundTrip() = runBlocking {
        FlowMode.entries.forEachIndexed { index,mode ->
            val s = session(mode,10)
            db.sessionDao().updateArcFields(s.id,42,index+1,1.0,0,s.scyraPoints)
        }
        assertEquals(3,db.sessionDao().getSessionCountForArc(42))
        assertEquals(FlowMode.entries.toList(),db.sessionDao().getSessionsForArc(42).map { it.mode })
        db.ongoingSessionDao().upsert(OngoingSessionEntity(flowInstanceId="power",title="Draft",description="",tagName="Journey",isInFlowMode=true,isRunning=true,mode=FlowMode.POWER,baseStartTimeMs=123,accumulatedBeforeStartMs=456))
        db.close();open()
        val cursor = db.openHelper.readableDatabase.query("SELECT isSoftMode,flowInstanceId,baseStartTimeMs FROM ongoing_session")
        cursor.use { assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0));assertEquals("power",it.getString(1));assertEquals(123,it.getInt(2)) }
    }

    @Test fun delayedMovementMirrorsOnceAndPendingGrantUsesLatestPersistedScore() = runBlocking {
        val health = FlowHealthRepository(db,db.flowHealthDao(),db.sessionDao(),db.pearlLedgerDao())
        suspend fun update(s: SessionEntity) {
            val snapshot = FlowHealthSnapshotEntity(s.id,true,true,FlowHealthSyncStatus.CAPTURED,
                700,7,7,7,1,1,1,1000,1,s.startTime,s.endTime,null)
            val breakdown = FlowRewardBreakdownEntity(s.id,57,0,0,0,7,64,1.0,1.0,1.0,0,64,64,true)
            health.applyDelayedMovementUpdateTransactionally(snapshot,breakdown,64,0,7,"movement-${s.id}-7")
        }
        val granted = session(FlowMode.POWER,57)
        rewards.onSessionCompleted(granted)
        repeat(2) { update(granted) }
        assertEquals(64,shell.getPearlBalance())
        assertEquals(64,db.powerRewardDao().balance())

        val pending = session(FlowMode.POWER,57)
        db.powerRewardDao().enqueue(SessionShellRewardEntity(sessionId=pending.id,queuedAt=1))
        update(pending)
        assertEquals(64,shell.getPearlBalance())
        // Simulate the original completion callback holding an older score snapshot.
        val result = rewards.onSessionCompleted(pending)
        assertEquals(64,result.pearlsEarned)
        assertEquals(64,result.pebblesEarned)
        repeat(2) { update(pending); rewards.onSessionCompleted(pending) }
        assertEquals(128,shell.getPearlBalance())
        assertEquals(128,db.powerRewardDao().balance())
    }
    @Test fun delayedPowerMovementPreservesArcAndReconcilesOverlappingReads() = runBlocking {
        val health = FlowHealthRepository(db,db.flowHealthDao(),db.sessionDao(),db.pearlLedgerDao())
        val flow = session(FlowMode.POWER,72)
        db.sessionDao().updateArcFields(flow.id,42,2,1.2,12,72)
        rewards.onSessionCompleted(flow)
        // Both reads started from the same 72-point completion. Their caller deltas
        // overlap; only the still-missing movement may be awarded inside the transaction.
        suspend fun apply(movement: Long) {
            val total=72+movement.toInt()
            val snapshot=FlowHealthSnapshotEntity(flow.id,true,true,FlowHealthSyncStatus.CAPTURED,
                movement*100,movement,movement,total.toLong(),1,1,1,1000,1,flow.startTime,flow.endTime,null)
            val breakdown=FlowRewardBreakdownEntity(flow.id,72,0,0,0,movement,total.toLong(),1.0,1.0,1.0,0,total.toLong(),total.toLong(),true)
            health.applyDelayedMovementUpdateTransactionally(snapshot,breakdown,total,0,movement.toInt(),"movement-${flow.id}-$movement")
        }
        apply(7); apply(10); apply(7); apply(10)
        assertEquals(82,db.sessionDao().getSessionById(flow.id)!!.scyraPoints)
        assertEquals(12,db.sessionDao().getSessionById(flow.id)!!.arcBonusPoints)
        assertEquals(12L,health.getRewardBreakdown(flow.id)!!.arcBonusPoints)
        assertEquals(10L,health.getSnapshot(flow.id)!!.finalMovementPearlContribution)
        assertEquals(82,shell.getPearlBalance())
        assertEquals(82,db.powerRewardDao().balance())
    }

    @Test fun firstPowerBadgeAppearsInCompletionAndReplayWithoutAnotherGrant() = runBlocking {
        val flow=session(FlowMode.POWER,15)
        val first=rewards.onSessionCompleted(flow)
        assertTrue(first.badgeIds.contains("power_spark"))
        assertEquals(first.badgeIds.toSet(),rewards.onSessionCompleted(flow).badgeIds.toSet())
        assertEquals(1,db.userBadgeDao().get("power_spark")!!.count)
        val second=rewards.onSessionCompleted(session(FlowMode.POWER,15))
        assertFalse(second.badgeIds.contains("power_spark"))
        assertEquals(30,db.powerRewardDao().balance())
    }

    @Test fun failedRetryDoesNotStarveLaterSessionsAndDeletedSessionsCannotAward() = runBlocking {
        val failed=session(FlowMode.POWER,15)
        val later=session(FlowMode.POWER,57)
        listOf(failed,later).forEach { db.powerRewardDao().enqueue(SessionShellRewardEntity(sessionId=it.id,queuedAt=1)) }
        db.powerRewardDao().enqueue(SessionShellRewardEntity(sessionId=9999,queuedAt=1))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_one_reward BEFORE INSERT ON pebble_ledger WHEN NEW.id = 'power:${failed.id}' BEGIN SELECT RAISE(ABORT, 'simulated'); END")
        rewards.retryPending()
        assertFalse(db.powerRewardDao().reward(failed.id)!!.completed)
        assertTrue(db.powerRewardDao().reward(later.id)!!.completed)
        assertNull(db.powerRewardDao().reward(9999))
        assertEquals(57,shell.getPearlBalance())
        assertEquals(57,db.powerRewardDao().balance())
        rewards.onSessionCompleted(later.copy(id=9999))
        assertEquals(57,db.powerRewardDao().balance())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_one_reward")
        db.powerRewardDao().defer(failed.id,1)
        rewards.retryPending()
        assertEquals(72,shell.getPearlBalance())
        assertEquals(72,db.powerRewardDao().balance())
    }
    @Test fun allNamedRedBadgesPersistOnceWithHistoricalDatesAndNoGenericDuplicates() = runBlocking {
        val legacyDuplicate = UserBadgeEntity("red_triassic_collector",1,90,90,false)
        db.userBadgeDao().upsert(legacyDuplicate)
        val specs = com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog.specs.filter { it.id.startsWith("red_") }
        // Tracking is available before any collection/mastery evidence exists.
        specs.forEach { assertTrue(shell.trackBadge(it.id)) }
        RedCreatureCatalog.entries.forEachIndexed { index, entry ->
            db.achievementDao().recordDiscovery(CreatureDiscoveryEntity(entry.id,100L+index,"red_purchase",entry.id,100L+index))
            db.achievementDao().recordMastery(CreatureMasteryEventEntity("master-${entry.id}",entry.id,entry.id,200L+index,"growth-${entry.id}"))
        }
        // Retain collection completion evidence without generating another unnamed reward.
        db.achievementDao().recordCompletion(CollectionCompletionEntity("red-completion","red_jurassic","COLLECTOR",134,
            com.kingkharnivore.skillz.domain.achievement.AchievementTimestampConfidence.EXACT,1,"test-roster",""))
        repeat(2) { shell.reconcilePowerBadges() }
        val earned = specs.associate { it.id to db.userBadgeDao().get(it.id)!! }
        specs.forEach { spec ->
            val row=earned.getValue(spec.id)
            assertEquals(spec.id,1,row.count)
            val expected = if (spec.mastery) spec.species.maxOf { RedCreatureCatalog.byId.getValue(it).catalogOrder }.toLong()+200
                else if (spec.target==1) 100L else spec.species.maxOf { RedCreatureCatalog.byId.getValue(it).catalogOrder }.toLong()+100
            assertEquals(spec.id,expected,row.firstEarnedAt)
        }
        assertEquals(legacyDuplicate,db.userBadgeDao().get(legacyDuplicate.badgeId))
        assertNull(db.userBadgeDao().get("red_jurassic_collector"))
        assertTrue(db.achievementDao().getCompletions().any { it.completionId=="red-completion" })
        db.close(); open()
        shell.reconcilePowerBadges()
        specs.forEach { assertEquals(earned.getValue(it.id),db.userBadgeDao().get(it.id)) }
    }

    @Test fun badgeBookCompletionAwardsAreIdempotentAcrossRestartAndPreserveWallets() = runBlocking {
        val groups=com.kingkharnivore.skillz.domain.achievement.BadgeBookCollections.newAwards
        groups.flatMap { it.memberIds }.distinct().forEachIndexed { i,id ->
            db.userBadgeDao().upsert(UserBadgeEntity(id,1,100L+i,100L+i,false))
        }
        val before= db.userBadgeDao().getAll().associateBy { it.badgeId }
        val pearls=shell.getPearlBalance()
        val pebbles=db.powerRewardDao().balance()
        repeat(2) { shell.reconcilePowerBadges() }
        val awards=groups.associate { it.completionBadgeId to db.userBadgeDao().get(it.completionBadgeId)!! }
        groups.forEach { group ->
            assertEquals(1,awards.getValue(group.completionBadgeId).count)
            assertEquals(group.memberIds.maxOf { before.getValue(it).firstEarnedAt },awards.getValue(group.completionBadgeId).firstEarnedAt)
        }
        db.close(); open()
        shell.reconcilePowerBadges()
        awards.forEach { (id,row) -> assertEquals(row,db.userBadgeDao().get(id)) }
        before.forEach { (id,row) -> assertEquals(row,db.userBadgeDao().get(id)) }
        assertEquals(pearls,shell.getPearlBalance())
        assertEquals(pebbles,db.powerRewardDao().balance())
    }

}
