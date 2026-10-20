package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Rule
import org.junit.Test

class ShellBalancesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allBalancesRemainVisibleAsDestinationsAndWalletsChange() {
        val destination = mutableStateOf<ShellDestination>(ShellDestination.Heart)
        val drops = mutableStateOf(250_000L)
        compose.setContent {
            SkillzTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    ShellTopBar(destination.value, 12345, false, 2, {}, {},
                        pebbleBalance = 6789, dropBalance = drops.value)
                }
            }
        }
        listOf(ShellDestination.Heart, ShellDestination.TheRed, ShellDestination.TheBluePreview,
            ShellDestination.TheGreen, ShellDestination.Focus, ShellDestination.ShellChest,
            ShellDestination.Badges, ShellDestination.VoyagePreview, ShellDestination.IdeaGrovePreview,
            ShellDestination.LookoutPreview).forEach { target ->
            compose.runOnIdle { destination.value = target }
            compose.onNodeWithContentDescription("Pearls: 12,345").assertIsDisplayed()
            compose.onNodeWithContentDescription("Pebbles: 6,789").assertIsDisplayed()
            compose.onNodeWithContentDescription("Drops: 250,000").assertIsDisplayed()
        }
        compose.runOnIdle { drops.value = 3_000_000_000L }
        compose.onNodeWithContentDescription("Drops: 3,000,000,000").assertIsDisplayed()
    }

    @Test fun compactBalancesExposeExactLiveAmountsOnTap() {
        val drops = mutableStateOf(3_000_000_001L)
        compose.setContent { SkillzTheme {
            Box(Modifier.width(320.dp)) { ShellBalances(Int.MAX_VALUE, 999_999, drops.value) }
        } }
        compose.onNodeWithContentDescription("Drops: 3,000,000,001").performClick()
        compose.onNodeWithText("3,000,000,001").assertIsDisplayed()
        compose.runOnIdle { drops.value = 3_000_000_002L }
        compose.onNodeWithText("3,000,000,002").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithContentDescription("Pearls: 2,147,483,647").performClick()
        compose.onNodeWithText("2,147,483,647").assertIsDisplayed()
    }

    @Test fun badgeDestinationAndOriginSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var navigate: (ShellDestination, ShellDestination) -> Unit
        restoration.setContent {
            var destination by rememberSaveable(stateSaver = ShellDestination.StateSaver) {
                mutableStateOf<ShellDestination>(ShellDestination.Heart)
            }
            var origin by rememberSaveable(stateSaver = ShellDestination.StateSaver) {
                mutableStateOf<ShellDestination>(ShellDestination.Heart)
            }
            navigate = { next, from -> destination = next; origin = from }
            Text("${destination.savedKey}:${origin.savedKey}")
        }
        listOf(ShellDestination.TheRed, ShellDestination.TheGreen).forEach { origin ->
            compose.runOnIdle { navigate(ShellDestination.Badges, origin) }
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("badges:${origin.savedKey}").assertIsDisplayed()
        }
    }

    @Test fun heartShowsAllThreeBalancesIncludingZero() {
        compose.setContent {
            SkillzTheme(dynamicColor = false) {
                HeartRoomScreen(ShellUiState(pearlBalance = 0, pebbleBalance = 15), dropBalance = 200) {}
            }
        }
        compose.onNodeWithContentDescription("Pearls: 0").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pebbles: 15").assertIsDisplayed()
        compose.onNodeWithContentDescription("Drops: 200").assertIsDisplayed()
    }
}
