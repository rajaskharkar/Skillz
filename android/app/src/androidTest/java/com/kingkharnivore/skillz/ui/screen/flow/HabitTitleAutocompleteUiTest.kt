package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.flow.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HabitTitleAutocompleteUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun planSuggestionSelectsItsSavedTypeTagAndPlanLink() {
        val state = mutableStateOf(FlowUiState(mode = FlowMode.SOFT))
        compose.setContent { SkillzTheme {
            GrandTitleField(
                value = state.value.title,
                onValueChange = { state.value = state.value.withEditedTitle(it) },
                activitySuggestions = activityTitleSuggestions(
                    listOf(FlowPlanEntity(id = 9, title = "Write chapter", tagId = 2, kind = HorizonKind.PLAN, mode = FlowMode.POWER)),
                    listOf(TagEntity(id = 2, name = "writing")), state.value
                ),
                onActivitySelected = { state.value = state.value.withActivityTitle(it) }
            )
        } }
        compose.onNode(hasSetTextAction()).performTextInput("WRI")
        compose.onNodeWithText("Write chapter").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(9L, state.value.originPlanId)
            assertEquals(HorizonKind.PLAN, state.value.originPlanKind)
            assertEquals(FlowMode.POWER, state.value.mode)
            assertEquals("writing", state.value.tagName)
        }
    }

    @Test fun typingAndSelectingHabitFillsTitleAndTagAndHidesSuggestions() {
        val state = mutableStateOf(FlowUiState())
        compose.setContent {
            SkillzTheme {
                Column {
                    GrandTitleField(
                        value = state.value.title,
                        onValueChange = { state.value = state.value.withEditedTitle(it) },
                        activitySuggestions = activityTitleSuggestions(
                            listOf(FlowPlanEntity(id = 7, title = "Brush teeth", tagId = 2)),
                            listOf(TagEntity(id = 2, name = "ready")), state.value
                        ),
                        onActivitySelected = { state.value = state.value.withActivityTitle(it) }
                    )
                    Text("Tag: ${state.value.tagName}")
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("BRU")
        compose.onNodeWithText("Brush teeth").assertIsDisplayed().performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("Brush teeth")
        compose.onNodeWithText("Tag: ready").assertIsDisplayed()
        compose.onAllNodesWithText("Brush teeth").assertCountEquals(1)
        compose.runOnIdle { assertEquals(7L, state.value.originPlanId) }
        compose.onNode(hasSetTextAction()).performTextReplacement("Read a book")
        compose.runOnIdle { org.junit.Assert.assertNull(state.value.originPlanId) }
        compose.onNode(hasSetTextAction()).performTextReplacement("Brush teeth")
        compose.runOnIdle { org.junit.Assert.assertNull(state.value.originPlanId) }
    }
}
