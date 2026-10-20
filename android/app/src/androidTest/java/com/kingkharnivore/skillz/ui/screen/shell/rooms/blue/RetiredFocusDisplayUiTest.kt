package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.ui.screen.shell.HeartRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.buildTheBlueUiState
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RetiredFocusDisplayUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun creatureDetailsKeepChestAndGrowthActionsWithoutFocusDisplayControls() {
        val creature = UserShellFindInstanceEntity("minnow", "focus_minnow", 1, "test", null, null, null, false, false)
        val animal = buildTheBlueUiState(listOf(creature)).zones.first().animals.single()
        var chestOpened = false
        compose.setContent { SkillzTheme(dynamicColor = false) {
            TheBlueAnimalDetailSheet(animal, 1000, {}, {}, {}, {}, { chestOpened = true })
        } }
        compose.onAllNodes(hasText("Focus", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("View in The Chest").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(chestOpened) }
        compose.onNodeWithText("Level Up").performScrollTo().assertIsEnabled()
    }

    @Test fun homeDoesNotAdvertiseObjectSlotsEvenWithLegacyPlacements() {
        val legacy = ShellPlacementEntity("legacy", "FOCUS", "creature_perch_left", "old-creature", 1)
        compose.setContent { SkillzTheme(dynamicColor = false) {
            HeartRoomScreen(ShellUiState(focusPlacements = listOf(legacy))) {}
        } }
        compose.onAllNodes(hasText("nook", substring = true, ignoreCase = true)).assertCountEquals(0)
        compose.onNodeWithText("Take a quiet moment in the Focus Room.").assertExists()
    }
}
