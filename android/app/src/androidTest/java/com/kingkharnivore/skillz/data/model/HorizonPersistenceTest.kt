package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.viewmodel.groupHorizonMemories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class HorizonPersistenceTest {
    private lateinit var db: SkillzDatabase
    private lateinit var flows: FlowRepository
    private lateinit var chronicles: ChronicleRepository
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
        chronicles = ChronicleRepository(db, db.chronicleDao())
        flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
        runBlocking { db.tagDao().insertTag(TagEntity(id = 1, name = "Outdoors")) }
    }
    @After fun close() { db.close() }
    private fun session(plan: Long?, end: Long = 60_000, duration: Long = 60_000) = SessionEntity(
        title = "An evening walk", description = "", tagId = 1, startTime = end-duration, endTime = end,
        durationMs = duration, mode = FlowMode.SOFT, originPlanId = plan)

    @Test fun changedJourneySavesStoryButDoesNotCreditHabitOrCompletePlan() = runBlocking {
        db.tagDao().insertTag(TagEntity(id=2, name="Reading"))
        for (kind in listOf(HorizonKind.HABIT, HorizonKind.PLAN)) {
            val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title="Walk", tagId=1, kind=kind))
            val changed = session(plan).copy(tagId=2)
            val id = flows.addSessionAndPromoteChronicle("changed-$kind", changed)
            assertEquals(id, flows.addSessionAndPromoteChronicle("changed-$kind", changed))
            assertEquals(2L, db.sessionDao().getSessionById(id)!!.tagId)
            assertNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
            assertFalse(db.horizonMemoryDao().observeAll().first().any { it.sourcePlanId == plan })
            flows.addSessionAndPromoteChronicle("renamed-$kind", session(plan).copy(title="A different title"))
            assertEquals("A different title", db.horizonMemoryDao().observeAll().first().single { it.sourcePlanId == plan }.title)
            if (kind == HorizonKind.PLAN) assertNotNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        }
    }

    @Test fun removingMemoryChangesOnlyThatHabitReceiptAndPreservesEveryOtherTable() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title="Walk", tagId=1))
        val otherHabit = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title="Read", tagId=1))
        flows.addSessionAndPromoteChronicle("older", session(plan))
        flows.addSessionAndPromoteChronicle("other-habit", session(otherHabit))
        chronicles.addText("ACTIVE_FLOW", "newer", "A walk to remember")
        db.pulseDao().insertPulse(PulseEntity(title="Notice the trees", description="Keep this thought", tagId=1, parentFlowInstanceId="newer"))
        val saved = session(plan, 120_000).copy(arcId=9, arcIndex=1)
        val id = flows.addSessionAndPromoteChronicle("newer", saved)
        db.arcMetadataDao().upsert(ArcMetadataEntity(9,"Walking arc","Keep the arc",null,null,null,1,1))
        db.stillwaterLedgerDao().insert(com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity("memory-drops",250,"soft_flow",id.toString(),1))
        val before = snapshotOutsideMemories()
        val otherReceipt = db.horizonMemoryDao().getByFlowInstanceId("other-habit")
        // A stale or mismatched activity cannot remove another activity's memory.
        flows.removeMemoryFromActivity("newer", otherHabit)
        assertNotNull(db.horizonMemoryDao().getByFlowInstanceId("newer"))
        flows.removeMemoryFromActivity("newer", plan)
        flows.removeMemoryFromActivity("newer", plan) // Repeated removal is harmless.
        assertEquals(before, snapshotOutsideMemories())
        assertEquals(otherReceipt, db.horizonMemoryDao().getByFlowInstanceId("other-habit"))
        assertEquals("A walk to remember", db.chronicleDao().moments(db.chronicleDao().find("SESSION", id.toString())!!.id).single().text)
        assertEquals(id, flows.findCreatedSession("newer"))
        // Retrying a completed save must neither recreate the memory nor duplicate the Flow/rewards.
        assertEquals(id, flows.addSessionAndPromoteChronicle("newer", saved))
        assertNull(db.horizonMemoryDao().getByFlowInstanceId("newer"))
        val groups = groupHorizonMemories(db.horizonMemoryDao().observeAll().first(), db.sessionDao().getAllSessions().first(), mapOf(1L to "Outdoors"))
        assertEquals(listOf("older"), groups.single { it.sourcePlanId == plan }.flows.map { it.receipt.flowInstanceId })
        flows.removeMemoryFromActivity("older", plan)
        assertEquals(listOf(otherReceipt), db.horizonMemoryDao().observeAll().first())
        assertEquals(before, snapshotOutsideMemories())
    }

    private fun snapshotOutsideMemories(): Map<String, List<List<String?>>> {
        val sqlite = db.openHelper.readableDatabase
        val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','android_metadata','horizon_memories')").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return tables.associateWith { table ->
            sqlite.query("SELECT * FROM \"$table\"").use { cursor ->
                buildList { while (cursor.moveToNext()) add(List(cursor.columnCount) { column ->
                    when (cursor.getType(column)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(column), android.util.Base64.NO_WRAP)
                        else -> cursor.getString(column)
                    }
                }) }.sortedBy { it.toString() }
            }
        }
    }

    @Test fun deletingDetachedPlanMemoryKeepsPlanCompleted() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title="Walk", tagId=1, kind=HorizonKind.PLAN))
        val id = flows.addSessionAndPromoteChronicle("done", session(plan))
        flows.deleteSession(id)
        assertNull(db.horizonMemoryDao().observeAll().first().single().sessionId)
        flows.removeMemoryFromActivity("done", plan)
        assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())
        assertNotNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        assertTrue(db.flowPlanDao().getActiveFlowPlans().first().isEmpty())
    }

    @Test fun memoryRemovalNeverAttemptsToDeleteStory() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title="Walk", tagId=1))
        chronicles.addText("ACTIVE_FLOW", "kept", "Keep this chronicle")
        val id = flows.addSessionAndPromoteChronicle("kept", session(plan))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_delete BEFORE DELETE ON sessions BEGIN SELECT RAISE(ABORT,'test failure'); END")
        flows.removeMemoryFromActivity("kept", plan)
        assertNotNull(db.sessionDao().getSessionById(id))
        assertNotNull(db.chronicleDao().find("SESSION", id.toString()))
        assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())
    }

    @Test fun powerPlansRetainTheirModeThroughEditingArcSnapshotsAndCompletion() = runBlocking {
        val repo = FlowPlanRepository(db.flowPlanDao())
        val id = repo.createFlowPlan("Power writing", 1, false, 25, true, HorizonKind.PLAN, FlowMode.POWER)
        val stored = FlowPlanRepository(db.flowPlanDao()).getFlowPlanById(id)!!
        assertEquals(FlowMode.POWER, stored.mode)
        assertFalse(stored.isSoftMode)
        assertTrue(stored.launchWithSurge)
        repo.updateFlowPlan(stored.copy(mode = FlowMode.SOFT))
        val soft = repo.getFlowPlanById(id)!!
        assertTrue(soft.isSoftMode)
        assertFalse(soft.launchWithSurge)
        repo.updateFlowPlan(soft.copy(mode = FlowMode.POWER))
        assertFalse(repo.getFlowPlanById(id)!!.isSoftMode)
        val arc = db.arcPlanDao().insertArcPlan(ArcPlanEntity(title = "Writing routine"))
        db.arcPlanDao().insertArcPlanStep(ArcPlanStepEntity(arcPlanId=arc, orderIndex=0,
            sourceFlowPlanId=id, titleSnapshot=stored.title, mode=stored.mode))
        assertEquals(FlowMode.POWER, db.arcPlanDao().getStepsForArcPlanOnce(arc).single().mode)
        flows.addSessionAndPromoteChronicle("power-plan", session(id).copy(mode=FlowMode.POWER))
        assertEquals(FlowMode.POWER, db.horizonMemoryDao().observeAll().first().single().mode)
        assertTrue(db.flowPlanDao().getActiveFlowPlans().first().isEmpty())
        val route = com.kingkharnivore.skillz.ui.navigation.SkillzDestinations.addSkillRoute(originPlanId=id, prefillMode=stored.mode)
        assertEquals("POWER", android.net.Uri.parse("skillz://" + route).getQueryParameter("prefillMode"))
    }

    @Test fun habitsCountSuccessfulFlowsExactlyOnceAndStayAvailable() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Walk", tagId = 1))
        db.flowPlanDao().markLaunched(plan)
        assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())
        val id = flows.addSessionAndPromoteChronicle("first", session(plan))
        assertEquals(id, flows.addSessionAndPromoteChronicle("first", session(plan)))
        flows.addSessionAndPromoteChronicle("second", session(plan, 120_000))
        assertEquals(2, db.horizonMemoryDao().observeAll().first().size)
        assertNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        assertEquals(plan, db.flowPlanDao().getActiveFlowPlans().first().single().id)
        val groups = groupHorizonMemories(db.horizonMemoryDao().observeAll().first(), db.sessionDao().getAllSessions().first(), mapOf(1L to "Outdoors"))
        assertEquals(2, groups.single().flows.size)
        assertEquals(listOf("second", "first"), groups.single().flows.map { it.receipt.flowInstanceId })
    }
    @Test fun oneTimePlanMovesOnlyAfterSuccessfulTimedSaveAndContinuationIsIndependent() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Sort photos", kind = HorizonKind.PLAN))
        flows.addSessionAndPromoteChronicle("zero", session(plan, duration = 0))
        assertNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())
        chronicles.addText("ACTIVE_FLOW", "timed", "A complete chronicle")
        val id = flows.addSessionAndPromoteChronicle("timed", session(plan))
        flows.updateArcFields(id, 9, 1, 1.2, 2, 12)
        flows.addSessionAndPromoteChronicle("continuation", session(null, 120_000))
        assertTrue(db.flowPlanDao().getActiveFlowPlans().first().isEmpty())
        assertEquals(60_000L, db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        val receipt = db.horizonMemoryDao().observeAll().first().single()
        assertEquals(id, receipt.sessionId)
        assertEquals(9L, receipt.arcId)
        assertEquals(12, receipt.score)
        assertEquals("A complete chronicle", db.chronicleDao().moments(db.chronicleDao().find("SESSION", id.toString())!!.id).single().text)
    }
    @Test fun failedSaveRollsBackPlanAndMemoryAndCanBeRetried() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Read", kind = HorizonKind.PLAN))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_memory BEFORE INSERT ON horizon_memories BEGIN SELECT RAISE(ABORT,'test failure'); END")
        try { flows.addSessionAndPromoteChronicle("retry", session(plan)); fail("Expected failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(db.sessionDao().getAllSessions().first().isEmpty())
        assertNull(db.flowPlanDao().getFlowPlanById(plan)!!.completedAt)
        assertTrue(db.horizonMemoryDao().observeAll().first().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_memory")
        flows.addSessionAndPromoteChronicle("retry", session(plan))
        assertEquals(1, db.horizonMemoryDao().observeAll().first().size)
    }
    @Test fun staleEditCannotUndoCompletionAndPlanAgainCreatesIndependentCopy() = runBlocking {
        val repo = FlowPlanRepository(db.flowPlanDao())
        val plan = repo.createFlowPlan("Read", 1, false, 25, true, HorizonKind.PLAN)
        val stale = repo.getFlowPlanById(plan)!!
        flows.addSessionAndPromoteChronicle("completed", session(plan))
        try { repo.updateFlowPlan(stale.copy(title = "Late edit")); fail("Expected stale edit rejection") }
        catch (_: IllegalStateException) { }
        val copy = repo.createFlowPlan(stale.title, stale.tagId, stale.isSoftMode, stale.targetMinutes, stale.launchWithSurge, HorizonKind.PLAN)
        assertNotEquals(plan, copy)
        assertEquals(listOf(copy), db.flowPlanDao().getActiveFlowPlans().first().map { it.id })
        assertEquals(plan, db.horizonMemoryDao().observeAll().first().single().sourcePlanId)
        assertEquals(25, repo.getFlowPlanById(copy)!!.targetMinutes)
    }
    @Test fun editedDetailsAndCountsSurviveDeletingTemplateAndStoryFlow() = runBlocking {
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Walk", tagId = 1))
        val id = flows.addSessionAndPromoteChronicle("kept", session(plan))
        flows.updateSessionDetails(id, "Updated walk", 1)
        db.flowPlanDao().deleteFlowPlanById(plan)
        flows.deleteSession(id)
        val receipt = db.horizonMemoryDao().observeAll().first().single()
        assertEquals("Updated walk", receipt.title)
        assertEquals("Outdoors", receipt.tagName)
        assertNull(receipt.sessionId)
        assertEquals(60_000L, receipt.durationMs)
    }
    @Test fun memoriesSortByLatestCompletionAndUseCurrentFlowDetails() = runBlocking {
        val habit = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Walk"))
        val plan = db.flowPlanDao().insertFlowPlan(FlowPlanEntity(title = "Read", kind = HorizonKind.PLAN))
        flows.addSessionAndPromoteChronicle("habit-old", session(habit, 1))
        flows.addSessionAndPromoteChronicle("plan", session(plan, 2))
        val latest = flows.addSessionAndPromoteChronicle("habit-new", session(habit, 3))
        flows.updateSessionDetails(latest, "Latest walk", 1)
        val groups = groupHorizonMemories(db.horizonMemoryDao().observeAll().first(), db.sessionDao().getAllSessions().first(), mapOf(1L to "Outdoors"))
        assertEquals(listOf(habit, plan), groups.map { it.sourcePlanId })
        assertEquals("Latest walk", groups.first().latest.session!!.title)
        assertEquals(2, groups.first().flows.size)
    }
}
