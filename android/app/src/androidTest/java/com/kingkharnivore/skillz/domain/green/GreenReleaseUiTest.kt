package com.kingkharnivore.skillz.domain.green

import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellChestScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GreenReleaseUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun greenReleaseRequiresConfirmationCanBeCancelledAndKeepsErrorsVisible() {
        val plant = GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("daisy"), 2, 1)
        val state = mutableStateOf(GreenUiState(1000, listOf(plant)))
        var calls = 0
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(state.value, initialSpecimenId = plant.id, onRelease = { id, level, _ ->
                assertEquals(plant.id, id); assertEquals(2, level); calls++
                state.value = state.value.copy(busy = true)
            })
        } } }
        compose.onNodeWithTag("green-release").performClick()
        compose.onNodeWithText("Release Daisy?").assertIsDisplayed()
        compose.onNodeWithText("1575", substring = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Keep Plant").performClick()
        compose.onNodeWithTag("green-specimen-dialog").assertExists()
        compose.onNodeWithTag("green-release").performClick()
        compose.onNodeWithTag("green-confirm-release").performClick().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, calls); state.value = state.value.copy(busy = false, messageRes = R.string.green_error) }
        compose.onNodeWithText("Growth could not be saved. Please try again.").assertIsDisplayed()
        compose.onNodeWithTag("green-release-confirmation").assertExists()
    }

    @Test fun chestReleaseRemovesOnlyTheSelectedMasteredCopyAndShowsDropCredit() {
        val species = GreenCatalogue.byId.getValue("daisy")
        val first = GreenPreviewFixtures.specimen(species, 99, 1)
        val selected = GreenPreviewFixtures.specimen(species, 99, 2)
        val state = mutableStateOf(GreenUiState(1000, listOf(first, selected)))
        compose.setContent { SkillzTheme { Surface {
            ShellChestScreen(ShellUiState(), { _, _ -> }, { _, _, _ -> }, {}, {}, {}, greenState = state.value,
                focusPlantSpeciesId = species.id, focusPlantSpecimenId = selected.id,
                onReleasePlant = { id, level, token ->
                    assertEquals(selected.id, id); assertEquals(99, level)
                    state.value = state.value.copy(specimens = listOf(first),
                        lifetimeSpecimens = listOf(first, selected.copy(releasedAt = GreenPreviewFixtures.DATE + 200000000)),
                        drops = 182838, action = GreenActionEntity(token, "plant_released", id, id, 99, 99, -181838, 182838, 1))
                })
        } } }
        compose.onNodeWithText("Water").assertDoesNotExist()
        compose.onNodeWithTag("green-release").performClick()
        compose.onNodeWithTag("green-confirm-release").performClick()
        compose.onNodeWithTag("green-release-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("chest-plant:${first.id}").assertIsDisplayed()
        compose.onNodeWithTag("chest-plant:${selected.id}").assertDoesNotExist()
        compose.onNodeWithText("Plant released. 181838 Drops added.").assertIsDisplayed()
        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, state.value.lifetimeSpecimens.count { it.level == 99 }) }
    }

    @Test fun successfulGreenReleaseClosesTheDialogAndAnnouncesTheReward() {
        val plant = GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("daisy"), 1, 1)
        val state = mutableStateOf(GreenUiState(1000, listOf(plant)))
        compose.setContent { SkillzTheme { Surface {
            GreenScreen(state.value, initialSpecimenId = plant.id, onRelease = { id, level, token ->
                state.value = state.value.copy(specimens = emptyList(), drops = 2350,
                    action = GreenActionEntity(token, "plant_released", id, id, level, level, -1350, 2350, 1))
            })
        } } }
        compose.onNodeWithTag("green-release").performClick()
        compose.onNodeWithTag("green-confirm-release").performClick()
        compose.onNodeWithTag("green-release-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("green-specimen-dialog").assertDoesNotExist()
        compose.onNodeWithText("Plant released. 1350 Drops added.").assertIsDisplayed()
    }
}
