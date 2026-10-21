package com.kingkharnivore.skillz.domain.green

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GreenSceneResilienceTest {
    @get:Rule val compose = createComposeRule()
    private fun plants(count: Int) = (1..count).map {
        GreenPreviewFixtures.specimen(GreenCatalogue.inEnvironment(GreenEnvironment.GARDEN)[(it - 1) % 10],
            GreenPreviewFixtures.levels[(it - 1) % 8], it.toLong())
    }

    @Test fun rapidReflowDisablesSelectionAndSettlesOnLatestPopulation() {
        val all = plants(30)
        val population = mutableStateOf(all.take(6))
        var selected: String? = null
        compose.setContent { SkillzTheme { Surface {
            GreenEnvironmentScene(GreenEnvironment.GARDEN, population.value, { selected = it }, Modifier.size(240.dp, 360.dp))
        } } }
        compose.mainClock.autoAdvance = false
        listOf(7, 20, 3, 30).forEach { count ->
            compose.runOnIdle { population.value = all.take(count) }
            compose.mainClock.advanceTimeBy(48)
            compose.onAllNodesWithTag("green-plant:${all.first().id}").fetchSemanticsNodes().firstOrNull()?.let {
                compose.onNodeWithTag("green-plant:${all.first().id}").assertIsNotEnabled()
            }
        }
        compose.mainClock.advanceTimeBy(600)
        all.forEach { compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed().assertIsEnabled() }
        compose.onNodeWithTag("green-plant:${all.last().id}").performClick()
        compose.runOnIdle { assertEquals(all.last().id, selected) }
    }

    @Test fun resizeDuringReflowImmediatelyFitsTheNewViewport() {
        val all = plants(40)
        val population = mutableStateOf(all.take(10))
        val short = mutableStateOf(false)
        compose.setContent { SkillzTheme { Surface {
            GreenEnvironmentScene(GreenEnvironment.GARDEN, population.value, {},
                Modifier.size(240.dp, if (short.value) 120.dp else 400.dp).testTag("scene"))
        } } }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { population.value = all }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { short.value = true }
        compose.mainClock.advanceTimeByFrame()
        all.forEach { compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed().assertIsEnabled() }
    }

    @Test fun calmModeAndReleaseRefitWithoutLeavingStaleSelectablePlants() {
        val all = plants(15)
        val population = mutableStateOf(all)
        compose.setContent { CompositionLocalProvider(LocalGreenCalmMode provides true) {
            SkillzTheme { Surface { GreenEnvironmentScene(GreenEnvironment.GARDEN, population.value, {}, Modifier.size(240.dp, 360.dp)) } }
        } }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { population.value = all.takeLast(7).map { it.copy(level = 90) } }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        all.take(8).forEach { compose.onNodeWithTag("green-plant:${it.id}").assertDoesNotExist() }
        all.takeLast(7).forEach { compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed().assertIsEnabled() }
        compose.runOnIdle { population.value = emptyList() }
        compose.mainClock.advanceTimeByFrame()
        all.forEach { compose.onNodeWithTag("green-plant:${it.id}").assertDoesNotExist() }
    }

    @Test fun denseWorldKeepsRealTouchTargetsAndControlsClearAtLargeText() {
        val all = plants(80)
        var selected: String? = null
        var chestOpened = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                SkillzTheme { Surface {
                    GreenWorld(GreenEnvironment.GARDEN, all, {}, { selected = it }, {}, { chestOpened = true }, 130.dp)
                } }
            }
        }
        all.forEach { compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed().assertIsEnabled() }
        // Physical taps, including tiny sprouts, must resolve to the displayed specimen.
        all.filterIndexed { index, _ -> index % 8 == 0 }.forEach { plant ->
            compose.onNodeWithTag("green-plant:${plant.id}").performTouchInput { click(center) }
            compose.runOnIdle { assertEquals(plant.id, selected) }
        }
        compose.onNodeWithText("View Plants in The Chest").performClick()
        compose.runOnIdle { assertTrue(chestOpened) }
    }

    @Test fun portraitWithDoubleTextKeepsTheWholeCollectionClearOfOverlays() {
        val all = plants(80)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                SkillzTheme { Surface { GreenScreen(GreenUiState(specimens = all)) } }
            }
        }
        val header = compose.onNodeWithTag("green-controls").fetchSemanticsNode().boundsInRoot
        val footer = compose.onNodeWithTag("green-footer:garden").fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithTag("green-rail:garden").fetchSemanticsNode().boundsInRoot
        all.forEach {
            val bounds = compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.top >= header.bottom)
            assertTrue(bounds.bottom <= footer.top)
            assertTrue(bounds.right <= rail.left)
        }
        compose.onNodeWithTag("green-plant:${all.first().id}").performTouchInput { click(center) }
        compose.onNodeWithTag("green-specimen-dialog").assertIsDisplayed()
    }

    @Test fun landscapeWorldReservesPlantSpaceAndKeepsNavigationWorking() {
        val all = plants(40)
        compose.setContent {
            // Fit a landscape window even on a portrait emulator.
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                SkillzTheme { Surface(Modifier.requiredSize(700.dp, 320.dp)) {
                    GreenScreen(GreenUiState(specimens = all))
                } }
            }
        }
        all.forEach { compose.onNodeWithTag("green-plant:${it.id}").assertIsDisplayed() }
        val controls = compose.onNodeWithTag("green-controls").fetchSemanticsNode().boundsInRoot
        val footer = compose.onNodeWithTag("green-footer:garden").fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithTag("green-rail:garden").fetchSemanticsNode().boundsInRoot
        all.forEach {
            val bounds = compose.onNodeWithTag("green-plant:${it.id}").fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= controls.right)
            assertTrue(bounds.bottom <= footer.top)
            assertTrue(bounds.right <= rail.left)
        }
        compose.onNodeWithTag("green-specimens:garden").performTouchInput { swipeUp() }
        compose.onNodeWithTag("green-specimens:woodlands").assertIsDisplayed()
        compose.onNode(hasText("Choose a Seed") and hasAnyAncestor(hasTestTag("green-footer:woodlands"))).performClick()
        compose.onNodeWithText("Beyond The Green").assertIsSelected()
    }
}
