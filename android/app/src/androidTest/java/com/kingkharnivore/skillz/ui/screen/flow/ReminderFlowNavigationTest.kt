package com.kingkharnivore.skillz.ui.screen.flow

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.MainActivity
import com.kingkharnivore.skillz.data.di.DatabaseModule
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.domain.reminders.reminderLaunchIntent
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Exercises the actual notification destination and production Flow completion path. */
class ReminderFlowNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var db: SkillzDatabase
    private lateinit var prefs: ArcPrefs
    private var scenario: ActivityScenario<MainActivity>? = null
    private var previousFlow: OngoingSessionEntity? = null
    private var previousArc: com.kingkharnivore.skillz.ui.model.ArcRuntimeState? = null
    private val activityId = 980001L
    private val tagId = 980001L
    private val sourceId = "reminder-navigation-test-${java.util.UUID.randomUUID()}"
    private var previousRun: ActiveArcRunEntity? = null
    private val generatedFlowIds = mutableListOf<String>()
    private var generatedArcId: Long? = null

    @Before fun setup(): Unit = runBlocking {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        db = DatabaseModule.provideDatabase(context)
        prefs = ArcPrefs(DatabaseModule.providePreferencesDataStore(context))
        previousFlow = db.ongoingSessionDao().getOngoingSession().first()?.takeUnless {
            it.originPlanId == activityId || it.flowInstanceId.startsWith("reminder-navigation-test-")
        }
        previousArc = prefs.load()
        previousRun = db.activeArcRunDao().getActiveArcRun().first()
        db.activeArcRunDao().clear()
        db.ongoingSessionDao().clear()
        prefs.clear(); prefs.clearRecentlyEnded(); prefs.clearPlannedFlowHandoff()
        db.tagDao().insertTag(TagEntity(id = tagId, name = "Reminder test journey"))
        db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = activityId, title = "Read the reminder book", tagId = tagId))
    }

    @After fun cleanup() = runBlocking {
        scenario?.onActivity { it.aliveFlowServiceController.stop() }
        scenario?.close()
        db.ongoingSessionDao().clear()
        previousFlow?.let { db.ongoingSessionDao().upsert(it) }
        prefs.clear(); prefs.clearRecentlyEnded(); prefs.clearPlannedFlowHandoff()
        previousArc?.let { prefs.save(it) }
        (generatedFlowIds + sourceId).forEach { source ->
            db.sessionDao().findCreatedSession(source)?.let { db.sessionDao().deleteSessionById(it) }
        }
        generatedArcId?.let {
            db.openHelper.writableDatabase.execSQL("DELETE FROM arc_land_reward WHERE arcId = ?", arrayOf(it))
        }
        db.activeArcRunDao().clear()
        previousRun?.let { db.activeArcRunDao().upsert(it) }
        db.openHelper.writableDatabase.execSQL("DELETE FROM arc_plans WHERE id = ?", arrayOf(activityId))
        db.flowPlanDao().deleteFlowPlanById(activityId)
        db.flowPlanDao().deleteFlowPlanById(activityId + 1)
        db.openHelper.writableDatabase.execSQL("DELETE FROM tags WHERE id = ?", arrayOf(tagId))
        db.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(reminderLaunchIntent(context, activityId))
    }
    private fun awaitText(text: String) {
        try {
            compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: ComposeTimeoutException) {
            throw AssertionError("Missing '$text':\n${compose.onRoot().printToString()}", failure)
        }
    }
    private fun ongoing() = runBlocking { db.ongoingSessionDao().getOngoingSession().first() }
    private fun seedFlow(elapsed: Long = 60_000) = runBlocking {
        db.ongoingSessionDao().upsert(OngoingSessionEntity(flowInstanceId = sourceId,
            title = "Current test work", description = "", tagName = "Reminder test journey",
            isInFlowMode = false, isRunning = false, baseStartTimeMs = null,
            accumulatedBeforeStartMs = elapsed))
    }

    @Test fun coldTapPreloadsLatestTitleTagAndAssociationWithoutStartingTimer() {
        launch()
        awaitText("Read the reminder book")
        compose.waitUntil(10_000) { ongoing()?.originPlanId == activityId }
        val draft = ongoing()!!
        assertEquals("Reminder test journey", draft.tagName)
        assertFalse(draft.isRunning)
        assertEquals(0L, draft.accumulatedBeforeStartMs)
        scenario!!.recreate()
        awaitText("Read the reminder book")
        assertEquals(draft.flowInstanceId, ongoing()!!.flowInstanceId)
    }

    @Test fun pausedFlowRequiresConfirmationAndCancelPreservesItsTime() {
        seedFlow()
        launch()
        awaitText("Keep current flow")
        scenario!!.recreate()
        awaitText("Keep current flow")
        compose.onNodeWithText("Keep current flow").performClick()
        compose.waitForIdle()
        assertEquals(sourceId, ongoing()!!.flowInstanceId)
        assertEquals(60_000L, ongoing()!!.accumulatedBeforeStartMs)
        assertNull(runBlocking { db.sessionDao().findCreatedSession(sourceId) })
    }

    @Test fun acceptedSwitchSavesOldFlowAndPreparesTheHabitAfterRewards() {
        seedFlow()
        launch()
        awaitText("Continue with habit")
        compose.onNodeWithText("Continue with habit").performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(15_000) { ongoing()?.originPlanId == activityId }
        val saved = runBlocking { db.sessionDao().findCreatedSession(sourceId) }
        assertNotNull(saved)
        assertFalse(ongoing()!!.isRunning)
        assertEquals("Read the reminder book", ongoing()!!.title)
        assertNotEquals(sourceId, ongoing()!!.flowInstanceId)
        scenario!!.onActivity { it.startActivity(reminderLaunchIntent(it, activityId)) }
        compose.waitForIdle()
        compose.onNodeWithText("Continue with habit").assertDoesNotExist()
        assertEquals(saved, runBlocking { db.sessionDao().findCreatedSession(sourceId) })
    }

    @Test fun nonemptyDraftIsProtectedBeforeReplacingIt() {
        seedFlow(elapsed = 0)
        launch()
        awaitText("Keep draft")
        compose.onNodeWithText("Keep draft").performClick()
        assertEquals(sourceId, ongoing()!!.flowInstanceId)
    }

    @Test fun changedReminderRequiresConfirmationOfItsLatestTitleAndKind() {
        seedFlow()
        launch()
        awaitText("Continue with habit")
        runBlocking {
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = activityId, title = "Updated reminder plan",
                tagId = tagId, kind = HorizonKind.PLAN))
        }
        compose.onNodeWithText("Continue with habit").performClick()
        awaitText("Continue with plan")
        assertEquals(sourceId, ongoing()!!.flowInstanceId)
        assertNull(runBlocking { db.sessionDao().findCreatedSession(sourceId) })
        compose.onNodeWithText("Continue with plan").performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(15_000) { ongoing()?.originPlanId == activityId }
        assertEquals("Updated reminder plan", ongoing()!!.title)
    }

    @Test fun standaloneSoftFlowContinuesToReminderAfterSaving() {
        seedFlow()
        runBlocking { db.ongoingSessionDao().upsert(ongoing()!!.copy(mode = FlowMode.SOFT)) }
        launch()
        awaitText("Continue with habit")
        compose.onNodeWithText("Continue with habit").performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(15_000) { ongoing()?.originPlanId == activityId }
        assertNotNull(runBlocking { db.sessionDao().findCreatedSession(sourceId) })
        assertFalse(ongoing()!!.isRunning)
        assertEquals("Read the reminder book", ongoing()!!.title)
    }

    @Test fun newerNotificationReplacesAnUnacceptedPromptAndSurvivesRecreation() {
        seedFlow()
        runBlocking {
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = activityId + 1, title = "Second reminder plan",
                tagId = tagId, kind = HorizonKind.PLAN))
        }
        launch()
        awaitText("Continue with habit")
        var originalIntent: android.content.Intent? = null
        scenario!!.onActivity {
            originalIntent = it.intent
            it.startActivity(reminderLaunchIntent(it, activityId + 1))
        }
        try {
            awaitText("Continue with plan")
        } finally {
            // Keep ActivityScenario's launch identity for subsequent lifecycle operations.
            scenario!!.onActivity { it.intent = originalIntent }
        }
        // onNewIntent changes the Intent while ActivityScenario observes the resume event.
        // Ask Android to recreate directly instead of relying on its stale PAUSED snapshot.
        var replacedActivity: MainActivity? = null
        scenario!!.onActivity { replacedActivity = it; it.recreate() }
        compose.waitUntil(10_000) { replacedActivity?.isDestroyed == true }
        awaitText("Continue with plan")
        compose.onNodeWithText("Continue with habit").assertDoesNotExist()
        compose.onNodeWithText("Continue with plan").performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(15_000) { ongoing()?.originPlanId == activityId + 1 }
        assertEquals("Second reminder plan", ongoing()!!.title)
        assertFalse(ongoing()!!.isRunning)
    }

    @Test fun deletedActivityDoesNotInterruptTheCurrentFlow() {
        seedFlow()
        runBlocking { db.flowPlanDao().deleteFlowPlanById(activityId) }
        launch()
        awaitText("Activity unavailable")
        assertEquals(sourceId, ongoing()!!.flowInstanceId)
        assertNull(runBlocking { db.sessionDao().findCreatedSession(sourceId) })
    }
    @Test fun warmPlanTapUsesPlanTitleTagAndSurgeSettings() {
        runBlocking {
            db.flowPlanDao().insertFlowPlan(FlowPlanEntity(id = activityId, title = "Prepare the plan", tagId = tagId,
                kind = HorizonKind.PLAN, launchWithSurge = true, targetMinutes = 25))
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        var originalIntent: android.content.Intent? = null
        scenario!!.onActivity {
            originalIntent = it.intent
            it.startActivity(reminderLaunchIntent(it, activityId))
        }
        awaitText("Prepare the plan")
        compose.waitUntil(10_000) { ongoing()?.originPlanId == activityId }
        assertEquals("Reminder test journey", ongoing()!!.tagName)
        assertEquals(25 * 60_000L, ongoing()!!.surgePlannedMs)
        assertFalse(ongoing()!!.isRunning)
        // ActivityScenario matches lifecycle callbacks by the original launch Intent.
        // Restore that test identity after verifying onNewIntent delivered the reminder.
        scenario!!.onActivity { it.intent = originalIntent }
    }

    @Test fun plannedArcDetourReturnsToTheNextUncompletedStep() {
        seedFlow()
        runBlocking {
            val now = System.currentTimeMillis()
            generatedArcId = now
            val arc = com.kingkharnivore.skillz.ui.model.ArcRuntimeState.afterFirstFlow(now, now - 120_000, now - 60_000, false)
            prefs.save(arc)
            db.ongoingSessionDao().upsert(ongoing()!!.copy(arcId = arc.arcId, arcSessionCountInArc = 1,
                arcLastSessionEndTimeMs = arc.lastSessionEndTimeMs, arcChainBase = arc.multiplier))
            db.arcPlanDao().insertArcPlan(ArcPlanEntity(id = activityId, title = "Test planned arc"))
            listOf("Current test work", "Waiting planned step", "Later planned step").forEachIndexed { index, title ->
                db.arcPlanDao().insertArcPlanStep(ArcPlanStepEntity(arcPlanId = activityId, orderIndex = index,
                    titleSnapshot = title, tagIdSnapshot = tagId))
            }
            db.activeArcRunDao().upsert(ActiveArcRunEntity(arcPlanId = activityId, arcTitle = "Test planned arc",
                currentStepIndex = 0, totalSteps = 3, currentStepTitle = "Current test work",
                currentTagName = "Reminder test journey", currentIsSoftMode = false))
        }
        launch()
        awaitText("Continue with habit")
        compose.onNodeWithText("Continue with habit").performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(15_000) { ongoing()?.originPlanId == activityId }
        generatedFlowIds += ongoing()!!.flowInstanceId
        assertEquals(1, runBlocking { db.activeArcRunDao().getActiveArcRun().first() }!!.currentStepIndex)
        assertEquals(ArcPrefs.PlannedFlowHandoff.REMINDER_DETOUR, runBlocking { prefs.loadPlannedFlowHandoff() })
        scenario!!.recreate()
        awaitText("Read the reminder book")
        compose.onNodeWithText("Enter Flow").performScrollTo().performClick()
        compose.waitUntil(8_000) { ongoing()?.baseStartTimeMs?.let { System.currentTimeMillis() - it > 1500 } == true }
        compose.onNodeWithText("Exit Flow").performScrollTo().performClick()
        compose.onNodeWithText("Continue Arc").performScrollTo().performClick()
        awaitText("Done")
        compose.onNodeWithText("Done").performClick()
        awaitText("Waiting planned step")
        assertEquals(1, runBlocking { db.activeArcRunDao().getActiveArcRun().first() }!!.currentStepIndex)
        assertNull(ongoing()!!.originPlanId)
    }

    @Test fun runningFlowKeepsRunningWhileTheSwitchPromptIsOpen() {
        seedFlow()
        val startedAt = System.currentTimeMillis() - 60_000
        runBlocking {
            db.ongoingSessionDao().upsert(ongoing()!!.copy(isRunning = true, isInFlowMode = true,
                baseStartTimeMs = startedAt, accumulatedBeforeStartMs = 0))
        }
        launch()
        awaitText("Keep current flow")
        assertTrue(ongoing()!!.isRunning)
        assertEquals(startedAt, ongoing()!!.baseStartTimeMs)
        compose.onNodeWithText("Keep current flow").performClick()
        assertTrue(ongoing()!!.isRunning)
        assertNull(runBlocking { db.sessionDao().findCreatedSession(sourceId) })
    }

}
