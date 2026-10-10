package com.kingkharnivore.skillz.ui.screen.paths

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.paths.*
import com.kingkharnivore.skillz.ui.screen.chronicle.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.assertEquals

class HorizonUiTest {
    @get:Rule val compose = createComposeRule()
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
    private val repository = ChronicleRepository(db, db.chronicleDao())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun close() { scope.cancel(); db.close() }
    private fun flow(id: Long, title: String, kind: String = HorizonKind.HABIT): MemoryFlowUi {
        val session = SessionEntity(id=id, title=title, description="", tagId=1, startTime=0, endTime=id*60_000,
            durationMs=60_000, mode=FlowMode.SOFT)
        return MemoryFlowUi(HorizonMemoryEntity("flow-$id",1,kind,"Evening walks",id,title,"Outdoors",FlowMode.SOFT,
            60_000,0,0,null,null,null,id*60_000), session, "Outdoors")
    }
    @Test fun newFlowEditorOffersPowerAndSavesTheSelectedMode() {
        var savedMode: FlowMode? = null
        compose.setContent { SkillzTheme(darkTheme=false,dynamicColor=false) {
            ActivityEditor(null, PathsPrimaryTab.PLANS, emptyList(), false, null, onClose={},
                onSave={ _, _, _, mode, _, _ -> savedMode=mode })
        } }
        compose.onNodeWithText("New flow").assertExists()
        compose.onNodeWithText("Title").performTextInput("Power writing")
        // The IME can keep moving the sheet after scroll-to completes. Exercise the
        // accessible button action rather than injecting a tap at a moving coordinate.
        compose.onNodeWithText("Power Flow").performScrollTo().assertIsEnabled()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Power Flow").assertIsSelected()
        compose.onNodeWithText("Save").performScrollTo().assertIsDisplayed().assertIsEnabled()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(FlowMode.POWER,savedMode) }
    }

    @Test fun horizonSeparatesPlansAndPlannedArcsAndRestoresArcActions() {
        var created = false
        var opened: Long? = null
        val arc = com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel(17, "Morning routine", false, 2, null,
            listOf(com.kingkharnivore.skillz.model.ui.ArcPlanStepPreviewUiModel("Read", 15, false, false)))
        compose.setContent {
            var section by remember { mutableStateOf(HorizonSection.PLANS) }
            var tab by remember { mutableStateOf(PathsPrimaryTab.HABITS) }
            SkillzTheme(darkTheme=false, dynamicColor=false) { Surface {
                HorizonContent(PathsUiState(isLoading=false, selectedSection=section, selectedPrimaryTab=tab, arcPlans=listOf(arc)),
                    showDreams=false, onToggleDreams={}, onTab={ tab=it }, onNew={}, onEdit={}, onLaunch={},
                    onPin={ _, _ -> }, onArchive={ _, _ -> }, onDelete={}, onPlanArc={ created=true },
                    onOpenArc={ opened=it }, onDeleteArc={}, onOpenSuggestedRoute={}, onSection={ section=it }, onPlanAgain={})
            } }
        }
        compose.onNodeWithContentDescription("Habits").assertIsDisplayed()
        compose.onNodeWithContentDescription("Memories").performClick()
        compose.onNodeWithText("Memories begin with a Flow").assertExists()
        compose.onNodeWithContentDescription("Planned Arcs").performClick()
        compose.onNodeWithContentDescription("Habits").assertDoesNotExist()
        compose.onNodeWithText("Morning routine").assertIsDisplayed().performClick()
        assertEquals(17L, opened)
        compose.onNodeWithText("Create Arc").performClick()
        assertEquals(true, created)
        compose.onNodeWithContentDescription("Plans").performClick()
        compose.onNodeWithText("Memories begin with a Flow").assertExists()
    }

    @Test fun habitMemoryExpandsEveryFlowAndItsFullChronicle() {
        val longText = "A moment on this walk. ".repeat(25) + "THE END"
        runBlocking {
            repository.addText("SESSION", "2", longText)
            repository.addText("SESSION", "1", "The earlier walk")
        }
        compose.setContent {
            SkillzTheme(darkTheme=false,dynamicColor=false) {
                CompositionLocalProvider(LocalChronicleReaderFactory provides { type,key -> ChronicleReadState(type,key,repository,scope) }) {
                    Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        MemoryCard(MemoryGroupUi(1,HorizonKind.HABIT,"Evening walks",listOf(flow(2,"Latest walk"),flow(1,"First walk")))) {}
                    } }
                }
            }
        }
        compose.onNodeWithText("2 completions").assertIsDisplayed()
        compose.onNodeWithText("Latest walk").assertIsDisplayed()
        compose.onNodeWithText("First walk").assertDoesNotExist()
        compose.onNodeWithText("Show Habit Journey").performClick()
        compose.onNodeWithText("Latest walk").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText(longText).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(longText).assertExists()
        compose.onNodeWithText("First walk").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("The earlier walk").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Plan again").assertDoesNotExist()
    }
    @Test fun singleHabitExpandsChroniclesAndConfirmsDeletionWithoutJourneyButton() {
        var deleted: String? = null
        val single = flow(1, "First walk").let { it.copy(session = it.session!!.copy(description = "The entire chronicle")) }
        compose.setContent { SkillzTheme(darkTheme=false,dynamicColor=false) { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                MemoryCard(MemoryGroupUi(1,HorizonKind.HABIT,"Evening walks",listOf(single)),
                    onRemoveMemory={ deleted=it.receipt.flowInstanceId }, onPlanAgain={})
            }
        } } }
        compose.onNodeWithText("Show Habit Journey").assertDoesNotExist()
        compose.onNodeWithText("The entire chronicle").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove from Memories").assertDoesNotExist()
        compose.onNodeWithText("First walk").performClick()
        compose.onNodeWithText("The entire chronicle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove from Memories").performClick()
        compose.onNodeWithText("Remove this memory?").assertIsDisplayed()
        compose.onNodeWithText("This removes the Flow only from this activity’s Memories. Its Story entry, Chronicles, recorded activity, and rewards will be kept.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, deleted)
        compose.onNodeWithContentDescription("Remove from Memories").performClick()
        compose.onNodeWithText("Remove from Memories", useUnmergedTree=true).performClick()
        assertEquals("flow-1", deleted)
    }

    @Test fun deletingOlderHabitFlowUpdatesCountAndHidesJourneyWithoutExpandingAnotherFlow() {
        compose.setContent {
            var memories by remember { mutableStateOf(listOf(flow(2,"Latest walk"),flow(1,"First walk"))) }
            SkillzTheme(darkTheme=false,dynamicColor=false) { Surface {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    MemoryCard(MemoryGroupUi(1,HorizonKind.HABIT,"Evening walks",memories),
                        onRemoveMemory={ removed -> memories = memories.filter { it.receipt.flowInstanceId != removed.receipt.flowInstanceId } },
                        onPlanAgain={})
                }
            } }
        }
        compose.onNodeWithText("Show Habit Journey").performClick()
        compose.onNodeWithText("First walk").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Remove from Memories").performScrollTo().performClick()
        compose.onNodeWithText("Remove from Memories", useUnmergedTree=true).performClick()
        compose.onNodeWithText("First walk").assertDoesNotExist()
        compose.onNodeWithText("1 completion").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hide Habit Journey").assertDoesNotExist()
        compose.onNodeWithText("Show Habit Journey").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove from Memories").assertDoesNotExist()
    }

    @Test fun oneTimeMemoryShowsFlowAndOffersPlanAgain() {
        var planned = false
        compose.setContent { SkillzTheme(darkTheme=false,dynamicColor=false) { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                MemoryCard(MemoryGroupUi(1,HorizonKind.PLAN,"Sort photos",listOf(flow(3,"Sorted holiday photos",HorizonKind.PLAN)))) { planned=true }
            }
        } } }
        compose.onNodeWithText("Sorted holiday photos").assertIsDisplayed()
        compose.onNodeWithText("Outdoors").assertIsDisplayed()
        compose.onNodeWithText("Plan again").performScrollTo().performClick()
        assertEquals(true,planned)
    }
}
