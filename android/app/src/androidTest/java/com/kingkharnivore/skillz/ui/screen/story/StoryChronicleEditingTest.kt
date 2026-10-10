package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.chronicle.*
import com.kingkharnivore.skillz.ui.screen.story.chronicle.FlowCard
import com.kingkharnivore.skillz.ui.screen.story.chronicle.PulseCard
import com.kingkharnivore.skillz.ui.screen.story.header.StoryEntryEditSheet
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StoryChronicleEditingTest {
    @get:Rule val compose = createComposeRule()
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
    private val repository = ChronicleRepository(db, db.chronicleDao())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun close() { scope.cancel(); db.close() }

    @Test fun expandedFlowAndNestedPulseReadBeyondThePreviewAndExposeEdit() {
        val fullFlow = "Flow chronicle ".repeat(30) + "FLOW END"
        val fullPulse = "Pulse chronicle ".repeat(30) + "PULSE END"
        runBlocking {
            repository.addText("SESSION", "101", fullFlow)
            repository.addText("PULSE", "202", fullPulse)
        }
        var editFlow = false
        var editPulse = false
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(LocalChronicleReaderFactory provides { type, key -> ChronicleReadState(type, key, repository, scope) }) {
                    var expanded by remember { mutableStateOf(false) }
                    Surface {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            FlowCard(
                                session = FlowListItemUiModel(sessionId = 101, title = "Walk", description = "Preview", tagId = 1,
                                    tagName = "Outdoors", journeyColor = Color.Blue, durationMs = 60000, createdAt = 1, score = 0, isSurge = false, surgePoints = 0),
                                isExpanded = expanded, showScoreUi = false, calmMode = false,
                                onToggleExpand = { expanded = !expanded }, onDeleteSession = {},
                                onLongPress = { editFlow = true }, onClick = { error("Expansion must not navigate away") },
                                childPulses = listOf(PulseListItemUiModel(pulseId = 202, title = "Idea", description = "Preview", tagId = null,
                                    tagName = "", createdAt = 1, parentSessionId = 101, arcId = null)),
                                onEditPulse = { editPulse = true }
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Walk").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText(fullFlow).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(fullFlow).assertExists()
        compose.onNodeWithContentDescription("Edit flow").performScrollTo().performClick()
        assertEquals(true, editFlow)
        compose.onNodeWithText("Idea").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText(fullPulse).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(fullPulse).assertExists()
        compose.onNodeWithContentDescription("Edit entry").performScrollTo().performClick()
        assertEquals(true, editPulse)
    }

    @Test fun completedEntryEditorSavesMetadataAndNewChronicleText() {
        var saved: List<String>? = null
        var closed = false
        lateinit var editor: ChronicleStateHolder
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                StoryEntryEditSheet("SESSION", 303, "Old title", "Old journey",
                    createEditor = { type, key -> ChronicleStateHolder(type, key, repository, scope).also { editor = it } },
                    onSave = { _, _, title, tag -> saved = listOf(title, tag) }, onClose = { closed = true })
            }
        }
        compose.onNodeWithText("Old title").performTextReplacement("Evening walk")
        compose.onNodeWithText("Old journey").performTextReplacement("Outdoors")
        compose.onNodeWithText("Edit chronicle").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Added after the flow ended")
        compose.onNodeWithText("Add", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { db.chronicleDao().find("SESSION", "303")?.let { db.chronicleDao().moments(it.id).size == 1 } == true } }
        // Room can expose the new moment before the editor has finished clearing its draft.
        // Saving that intermediate state correctly opens the unfinished-draft warning.
        compose.waitUntil(5000) {
            editor.state.value.draft.isBlank() && !editor.state.value.isCommitting
        }
        // The soft keyboard animates the sheet bounds; activate the accessible
        // enabled Save action without a stale tap coordinate.
        compose.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.waitUntil(5000) { closed }
        assertEquals(listOf("Evening walk", "Outdoors"), saved)
        assertEquals("Added after the flow ended", runBlocking {
            val owner = db.chronicleDao().find("SESSION", "303")!!
            db.chronicleDao().moments(owner.id).single().text
        })
    }
}
