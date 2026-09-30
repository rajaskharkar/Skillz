package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneUiModel
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LandPresenceRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun thirteenTigersRenderCopiesAndOverflowAndKeepTheExactCountWhenTapped() {
        var selectedCount = 0
        showTigers(13) { selectedCount = it.totalCount }
        repeat(4) { index ->
            compose.onNodeWithTag("land-presence:creature_tiger:direct:$index").assertIsDisplayed()
        }
        compose.onNodeWithTag("land-presence:creature_tiger:direct:4").assertDoesNotExist()
        compose.onNodeWithTag("land-presence:creature_tiger:cohort").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(13, selectedCount) }
    }

    @Test fun oneTigerDoesNotRenderInventedCopiesOrOverflow() {
        showTigers(1) {}
        compose.onNodeWithTag("land-presence:creature_tiger:direct:0").assertIsDisplayed()
        compose.onNodeWithTag("land-presence:creature_tiger:direct:1").assertDoesNotExist()
        compose.onNodeWithTag("land-presence:creature_tiger:cohort").assertDoesNotExist()
    }

    private fun showTigers(count: Int, onSelect: (TheBlueAnimalGroupUiModel) -> Unit) {
        val animal = TheBlueAnimalGroupUiModel("creature_tiger", TheBlueZoneId.GREAT_WILD,
            count, 0, count, null, emptyList(), null, false)
        compose.setContent {
            SkillzTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.size(390.dp, 750.dp)) {
                    LandZonePage(TheBlueZoneUiModel(TheBlueZoneId.GREAT_WILD, listOf(animal)),
                        { 0f }, onSelect, {}, {})
                }
            }
        }
    }
}
