package com.kingkharnivore.skillz.ui.screen.shell.rooms.lookout

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveCardUiState
import com.kingkharnivore.skillz.viewmodel.shell.SetObjectiveDialogState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ObjectiveFlowCardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun eachCardLabelsItsModeAndLaunchesThatMode() {
        val mode = mutableStateOf(FlowMode.FLOW)
        var launched: Pair<String, FlowMode>? = null
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        ObjectiveCard(card(mode.value), {}, {}, { journey, selectedMode -> launched = journey to selectedMode }, false)
                    }
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FlowMode.entries.forEach { selected ->
            compose.runOnIdle { mode.value = selected }
            val label = when (selected) {
                FlowMode.FLOW -> R.string.lookout_flow_regular
                FlowMode.SOFT -> R.string.lookout_flow_soft
                FlowMode.POWER -> R.string.lookout_flow_power
            }
            compose.onNodeWithText(context.getString(label)).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.lookout_start_flow)).performClick()
            compose.runOnIdle { assertEquals("Piano" to selected, launched) }
            val folder = context.getExternalFilesDir("lookout-objectives")!!.apply { mkdirs() }
            File(folder, "${selected.name.lowercase()}.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun optionalTargetCanBeEnabledAndRemovedForWeeklyObjective() {
        val state = mutableStateOf(SetObjectiveDialogState(period = ObjectivePeriod.Weekly))
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Surface { Column { ObjectiveTargetSelector(state.value) { transform -> state.value = transform(state.value) } } }
            }
        }
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle { assertEquals(540, state.value.targetMinuteOfDay) }
        compose.onNode(hasSetTextAction()).performTextReplacement("7")
        compose.runOnIdle { assertEquals("7", state.value.targetDayText) }
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle { assertNull(state.value.targetMinuteOfDay) }
    }

    private fun card(mode: FlowMode) = ObjectiveCardUiState(
        objectiveId = 1, journeyId = 1, journeyName = "Piano", periodLabel = "Daily", typeLabel = "Recurring",
        progressPercent = 25, progressLabel = "15 min / 1 h", timeLeftLabel = "8 h left",
        estimatedRewardLabel = when (mode) {
            FlowMode.FLOW -> "132 Scyra Points · 132 Pearls"
            FlowMode.SOFT -> "7,920 Drops"
            FlowMode.POWER -> "132 Scyra Points · 132 Pearls · 132 Pebbles"
        },
        isRecurring = true, currentStreak = 1, maxStreak = 3, totalCompletions = 7,
        streakBonusLabel = "+10% streak bonus", rewardPearls = null, completionId = null,
        pearlsClaimed = false, state = ObjectiveCardState.InProgress, flowMode = mode,
        targetLabel = "Start before 10/13/26, 5:00 PM for 2× rewards"
    )
}
