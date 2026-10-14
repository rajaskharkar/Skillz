package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.ObjectiveReminderPreference
import com.kingkharnivore.skillz.ui.screen.story.saga.SagaPulseSection
import com.kingkharnivore.skillz.ui.screen.shell.rooms.lookout.RewardDialog
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveRewardDialogState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SagaActionsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun pulseHasSeparateFlowAndShowActionsAndBlocksFlowWhileActive() {
        val enabled = mutableStateOf(true)
        var flowed: Long? = null
        var shown: Long? = null
        compose.setContent { SkillzTheme { Surface { SagaPulseSection(
            listOf(PulseListItemUiModel(7, "Write melody", "", tagId = 1, tagName = "Piano", createdAt = 1, parentSessionId = null, arcId = null)),
            onFlow = { flowed = it }, onShow = { shown = it }, flowEnabled = enabled.value
        ) } } }
        compose.onNodeWithContentDescription(context.getString(R.string.saga_pulse_section_expand)).performClick()
        screenshot("pulse-actions")
        compose.onNodeWithText(context.getString(R.string.lookout_start_flow)).performClick()
        compose.onNodeWithText(context.getString(R.string.saga_pulse_show)).performClick()
        compose.runOnIdle { assertEquals(7L, flowed); assertEquals(7L, shown); enabled.value = false }
        compose.onNodeWithText(context.getString(R.string.lookout_start_flow)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.saga_pulse_show)).assertIsEnabled()
    }

    @Test fun reminderSettingAcceptsCustomAndZeroButRejectsNegativeAndOverflow() {
        val minutes = mutableStateOf(30)
        compose.setContent { SkillzTheme { Surface { ObjectiveReminderPreference(minutes.value) { minutes.value = it } } } }
        compose.onNodeWithText(context.getString(R.string.help_objective_reminder_title)).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("-1")
        compose.onNodeWithText(context.getString(R.string.common_save)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("9999999999999999")
        compose.onNodeWithText(context.getString(R.string.common_save)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("45")
        screenshot("reminder-settings")
        compose.onNodeWithText(context.getString(R.string.common_save)).performClick()
        compose.runOnIdle { assertEquals(45, minutes.value) }
        compose.onNodeWithText(context.getString(R.string.help_objective_reminder_title)).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        compose.onNodeWithText(context.getString(R.string.common_save)).performClick()
        compose.runOnIdle { assertEquals(0, minutes.value) }
        compose.onNodeWithText(context.getString(R.string.help_reminder_at_target)).assertIsDisplayed()
    }

    @Test fun sagasDisplaysObjectivesAndRoutesClaimsAndMatchingFlowMode() {
        val card = mutableStateOf(com.kingkharnivore.skillz.viewmodel.shell.ObjectiveCardUiState(
            objectiveId = 1, journeyId = 1, journeyName = "Piano objective", periodLabel = "Daily", typeLabel = "One-time",
            progressPercent = 100, progressLabel = "30 min / 30 min", timeLeftLabel = "Completed",
            estimatedRewardLabel = "3,600 Drops", isRecurring = false, currentStreak = null, maxStreak = null,
            totalCompletions = null, streakBonusLabel = null, rewardPearls = 0, completionId = 42,
            pearlsClaimed = false, state = com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveCardState.Completed,
            flowMode = com.kingkharnivore.skillz.model.FlowMode.SOFT,
            cycleLabel = "Cycle: Oct 12, 2026 – Oct 12, 2026"
        ))
        val enabled = mutableStateOf(false)
        var claim: Long? = null
        var mode: com.kingkharnivore.skillz.model.FlowMode? = null
        compose.setContent { SkillzTheme { Surface {
            com.kingkharnivore.skillz.ui.screen.story.header.StoryHeaderScrollableWithStickyTabs(
                uiState = com.kingkharnivore.skillz.model.state.FlowListUiState(isLoading = false),
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                onTagToggled = {}, onClearAllTags = {}, onPeriodSelected = {}, onPrev = {}, onNext = {}, onToday = {},
                onOpenViewJourneys = {}, onSessionClick = {}, onDeleteSession = {}, onDeletePulse = {},
                onCreatePulseForSession = { _, _, _, _ -> }, onAddSessionClick = {}, onEditArc = {},
                createChronicleEditor = { _, _ -> error("Not used") }, onSaveEntry = { _, _, _, _ -> },
                objectives = listOf(card.value), onObjectiveClaim = { claim = it },
                onObjectiveFlow = { _, value -> mode = value }, sagaFlowEnabled = enabled.value
            )
        } } }
        compose.onNodeWithContentDescription(context.getString(R.string.story_tab_sagas)).performClick()
        compose.onNodeWithTag("story-list").performScrollToNode(hasText(context.getString(R.string.lookout_claim)))
        compose.onNodeWithText("Piano objective").assertExists()
        compose.onNodeWithText("Cycle: Oct 12, 2026 – Oct 12, 2026").assertExists()
        compose.onNodeWithText(context.getString(R.string.lookout_flow_soft)).assertExists()
        compose.onNodeWithText("3,600 Drops").assertIsDisplayed()
        screenshot("sagas-objectives")
        compose.onNodeWithText(context.getString(R.string.lookout_claim)).performClick()
        compose.runOnIdle {
            assertEquals(42L, claim)
            card.value = card.value.copy(state = com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveCardState.InProgress, completionId = null, progressPercent = 50)
        }
        compose.onNodeWithTag("story-list").performScrollToNode(hasText(context.getString(R.string.lookout_start_flow)))
        compose.onNodeWithText(context.getString(R.string.lookout_start_flow)).assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText(context.getString(R.string.lookout_start_flow)).performClick()
        compose.runOnIdle { assertEquals(com.kingkharnivore.skillz.model.FlowMode.SOFT, mode) }
    }

    @Test fun rewardScreenShowsMixedRewardsAndCloses() {
        val open = mutableStateOf(true)
        compose.setContent { SkillzTheme { if (open.value) RewardDialog(
            ObjectiveRewardDialogState("Rewards claimed", "Piano Daily Objective", "66 Scyra Points · 66 Pearls · 66 Pebbles", "+10% streak bonus", "2× early-start rewards")
        ) { open.value = false } } }
        compose.onNodeWithText("66 Scyra Points · 66 Pearls · 66 Pebbles").assertIsDisplayed()
        screenshot("claimed-rewards")
        compose.onNodeWithText(context.getString(R.string.common_done)).performClick()
        compose.runOnIdle { assertFalse(open.value) }
    }
    @Test fun pulseShowDisplaysLinkedFlowHistory() {
        val pulse = com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel(
            pulseId = 7, type = com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType.IDEA,
            title = "Write a melody", description = "", journeyName = "Piano", createdAt = 1, updatedAt = 1,
            groveStatus = "alive", groveStatusChangedAt = null, flowCount = 1, totalFlowDurationMs = 1_800_000,
            lastWorkedAt = 1, wasCapturedDuringFlow = false,
            flows = listOf(com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveFlowUiModel(
                sessionId = 3, title = "Linked practice", description = "Worked on the melody", journeyName = "Piano",
                durationMs = 1_800_000, startTime = 1_800_000, endTime = 3_600_000
            ))
        )
        compose.setContent { SkillzTheme { SagaPulseHistorySheet(pulse) {} } }
        compose.onNodeWithText("Linked practice").assertIsDisplayed()
        compose.onNodeWithText("Worked on the melody").assertIsDisplayed()
        screenshot("pulse-history")
    }

    private fun screenshot(name: String) {
        val directory = context.getExternalFilesDir("sagas-objectives")!!.apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use {
            compose.waitForIdle()
            requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
