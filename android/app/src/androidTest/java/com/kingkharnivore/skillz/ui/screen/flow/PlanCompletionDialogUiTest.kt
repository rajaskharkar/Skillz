package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlanCompletionDialogUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explicitAnswersAreDisabledWhileSavingAndSendTheChosenOutcome() {
        val saving = mutableStateOf(true)
        val answers = mutableListOf<Boolean>()
        compose.setContent { SkillzTheme {
            PlanCompletionDialog(saving.value, error = null, onAnswer = { answers += it })
        } }
        compose.onNodeWithText("Was this Plan successfully completed?").assertIsDisplayed()
        compose.onNodeWithText("Yes, this Plan is complete").assertIsNotEnabled()
        compose.onNodeWithText("No, I need to work on this some more").assertIsNotEnabled()
        compose.runOnIdle { saving.value = false }
        compose.onNodeWithText("No, I need to work on this some more").performClick()
        compose.runOnIdle { assertEquals(listOf(false), answers) }
        compose.onNodeWithText("Yes, this Plan is complete").performClick()
        compose.runOnIdle { assertEquals(listOf(false, true), answers) }
    }
}
