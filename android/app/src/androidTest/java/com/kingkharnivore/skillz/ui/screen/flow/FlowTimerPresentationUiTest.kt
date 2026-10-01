package com.kingkharnivore.skillz.ui.screen.flow

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises the production timer and Surge control with authoritative elapsed/target fixtures. */
class FlowTimerPresentationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun timerAndPillRenderAndRestoreAcrossNormalCountdownZeroAndOvertime() {
        val state = mutableStateOf(flowState(754_000L, surge = false))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.width(360.dp).padding(16.dp)) {
                        FlowTimer(state.value.mainTimerText, calmMode = false)
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth()) {
                            SurgeMiniControl(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                isInFlow = state.value.isInFlowMode,
                                hasReachedTarget = state.value.hasReachedSurgeTarget,
                                locked = true,
                                isSurgeOn = state.value.isSurgeOn,
                                plannedMs = state.value.surgePlannedMs,
                                minutesInline = "30",
                                onMinutesChange = {}, onCommit = {}, onToggleOff = {}, onLongPress = {}
                            )
                        }
                    }
                }
            }
        }
        verifyTimer("12:34")
        screenshot("normal-12-34")
        compose.runOnIdle { state.value = flowState(755_000L, surge = false) }
        verifyTimer("12:35")

        compose.runOnIdle { state.value = flowState(328_000L) }
        verifyTimer("24:32")
        screenshot("surge-24-32")
        restoration.emulateSavedInstanceStateRestore()
        verifyTimer("24:32")
        compose.runOnIdle { state.value = state.value.copy(title = "Unrelated recomposition") }
        verifyTimer("24:32")

        listOf(1_799_427L to "00:01", 1_800_427L to "00:00", 1_801_427L to "-00:01",
            1_802_427L to "-00:02", 2_001_000L to "-03:21").forEach { (elapsed, text) ->
            compose.runOnIdle { state.value = flowState(elapsed) }
            verifyTimer(text)
            if (text == "00:00") screenshot("surge-zero")
        }
        screenshot("surge-overtime-03-21")
        restoration.emulateSavedInstanceStateRestore()
        verifyTimer("-03:21")
    }

    @Test fun surgePillShowsFixedTargetThroughOvertimeAndRestoration() {
        val state = mutableStateOf(flowState(328_000L))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.width(360.dp).padding(16.dp)) {
                        ArcPill(
                            arcMultiplier = 1.3,
                            arcNextIndex = 2,
                            isPending = false,
                            graceRemainingMs = null,
                            pauseRemainingMs = null,
                            isInFlow = true,
                            modifier = Modifier.testTag("arc-pill")
                        )
                        FlowTimer(state.value.mainTimerText, calmMode = false)
                        SurgeMiniControl(
                            modifier = Modifier.align(Alignment.End).testTag("surge-pill"),
                            isInFlow = state.value.isInFlowMode,
                            hasReachedTarget = state.value.hasReachedSurgeTarget,
                            locked = true,
                            isSurgeOn = state.value.isSurgeOn,
                            plannedMs = state.value.surgePlannedMs,
                            minutesInline = "30",
                            onMinutesChange = {}, onCommit = {}, onToggleOff = {}, onLongPress = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("· 30 min").assertIsDisplayed()
        compose.onNodeWithText("24:32").assertIsDisplayed()
        compose.onNode(hasText("· 30 min") and hasAnyAncestor(hasTestTag("surge-pill")), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("· 30 min") and hasAnyAncestor(hasTestTag("arc-pill")), useUnmergedTree = true).assertDoesNotExist()
        screenshot("surge-pill-target")
        compose.runOnIdle { state.value = flowState(2_001_000L) }
        compose.onNodeWithText("-03:21").assertIsDisplayed()
        compose.onNodeWithText("· 30 min").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("· 30 min").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(surgePlannedMs = 45 * 60_000L) }
        compose.onNodeWithText("· 45 min").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(isSurgeOn = false) }
        compose.onNodeWithText("· 45 min").assertDoesNotExist()
        compose.onNodeWithText("Arc").assertIsDisplayed()
        compose.onNodeWithText("Flow Active").assertIsDisplayed()
    }

    private fun verifyTimer(text: String) {
        compose.onAllNodesWithText(text).assertCountEquals(1)
        val timer = compose.onNodeWithText(text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(root.center.x, timer.center.x, 1f)
        compose.onNodeWithText("Surge").assertIsDisplayed()
        compose.onNodeWithText("Complete").assertDoesNotExist()
        // Only the main timer has a countdown; the pill may show a fixed minute target.
        compose.onAllNodes(hasText(":", substring = true)).assertCountEquals(1)
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = context.getExternalFilesDir("flow-timer")!!.apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun flowState(elapsed: Long, surge: Boolean = true) = FlowUiState(
        isInFlowMode = true,
        isSurgeOn = surge,
        surgePlannedMs = if (surge) 1_800_000L else null,
        stopwatch = StopwatchState(isRunning = true, elapsedMs = elapsed)
    )
}
