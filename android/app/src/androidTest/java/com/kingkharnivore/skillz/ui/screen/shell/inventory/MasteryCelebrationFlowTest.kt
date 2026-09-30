package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kingkharnivore.skillz.debug.MasteryCelebrationVisualTestActivity
import org.junit.Rule
import org.junit.Test

class MasteryCelebrationFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MasteryCelebrationVisualTestActivity>()

    @Test fun footerIsStableAndRequiresAllThreeExplicitSteps() {
        composeRule.onNodeWithText("1 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-previous").assertDoesNotExist()
        composeRule.onNodeWithTag("mastery-next").performClick()

        composeRule.onNodeWithText("2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-previous").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-next").performClick()

        composeRule.onNodeWithText("3 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-previous").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-next").assertDoesNotExist()
        composeRule.onNodeWithTag("mastery-done").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-achievement-grid").assertExists()
    }

    @Test fun previousReturnsToThePriorStep() {
        composeRule.onNodeWithTag("mastery-next").performClick()
        composeRule.onNodeWithText("2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-previous").performClick()

        composeRule.onNodeWithText("1 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("mastery-previous").assertDoesNotExist()
    }
}
