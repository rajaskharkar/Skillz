package com.kingkharnivore.skillz.domain.power

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.ui.screen.flow.SessionModeSelector
import com.kingkharnivore.skillz.ui.screen.shell.rooms.red.RedScreen
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.utils.shell.RedEra
import com.kingkharnivore.skillz.viewmodel.shell.RedUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PowerRedUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun powerCanBeSelectedBeforeStarting() {
        var selected=false
        compose.setContent { SkillzTheme(dynamicColor=false) { SessionModeSelector(FlowMode.FLOW,{selected=true},false,{},{}) } }
        compose.onNode(isSelectable() and hasText("Flow")).assertIsSelected()
        compose.onNode(isSelectable() and hasText("Power")).assertIsNotSelected()
        compose.onNodeWithText("Power",useUnmergedTree=true).performClick()
        compose.runOnIdle { assertTrue(selected) }
    }
    @Test fun lockedPowerOnlyShowsCurrentMode() {
        compose.setContent { SkillzTheme(dynamicColor=false) { SessionModeSelector(FlowMode.POWER,{},true,{},{}) } }
        compose.onNodeWithText("Power",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("Soft",useUnmergedTree=true).assertDoesNotExist()
    }
    @Test fun redPurchaseConfirmsKnownDinosaurAndPrice() {
        var purchased: String?=null
        compose.setContent { SkillzTheme(dynamicColor=false) { RedScreen(RedUiState(pebbles=1000),onPurchase={id,_->purchased=id},simulatedSceneTime=0f,initialEra=RedEra.TRIASSIC,initialCreature="triassic_saturnalia") } }
        compose.onNodeWithText("Acquire dinosaur").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("triassic_saturnalia",purchased) }
    }
    @Test fun insufficientBalanceDisablesPurchase() {
        compose.setContent { SkillzTheme(dynamicColor=false) { RedScreen(RedUiState(pebbles=0),simulatedSceneTime=0f,initialEra=RedEra.TRIASSIC,initialCreature="triassic_saturnalia") } }
        compose.onNodeWithText("Insufficient Pebbles").assertIsNotEnabled()
    }

    @Test fun allThreeModeLabelsShareOneRow() {
        compose.setContent { SkillzTheme(dynamicColor=false) { SessionModeSelector(FlowMode.POWER,{},false,{},{}) } }
        val labels=listOf("Flow","Soft","Power").map { compose.onNodeWithText(it,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot }
        assertEquals(labels[0].top,labels[1].top,1f)
        assertEquals(labels[0].top,labels[2].top,1f)
        assertTrue(labels[0].right<labels[1].left && labels[1].right<labels[2].left)
    }

    @Test fun redErasSwipeHorizontallyAndAnimalsStayBelowText() {
        compose.setContent { SkillzTheme(dynamicColor=false) { RedScreen(RedUiState(),simulatedSceneTime=12f) } }
        compose.onNodeWithTag("red-era-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("201–145 million years ago").assertIsDisplayed()
        val catalog=compose.onAllNodesWithTag("red-open-catalog").fetchSemanticsNodes().first { it.boundsInRoot.left>=0 }
        val animals=compose.onAllNodesWithTag("red-animal-lanes").fetchSemanticsNodes().first { it.boundsInRoot.left>=0 }
        assertTrue(animals.boundsInRoot.top>=catalog.boundsInRoot.bottom)
        compose.onNodeWithTag("red-era-CRETACEOUS").performClick()
        compose.onNodeWithText("145–66 million years ago").assertIsDisplayed()
        compose.onNodeWithTag("red-era-CRETACEOUS").assertIsSelected()
        compose.onNodeWithTag("red-total-progress").assertTextEquals("The Red · 0 / 69 dinosaurs")
        compose.onNodeWithTag("red-open-catalog").performClick()
        compose.onNodeWithTag("red-catalog-sheet").assertIsDisplayed()
        compose.onNode(hasText("Choose a dinosaur to join this world.") and hasAnyAncestor(hasTestTag("red-catalog-sheet"))).assertIsDisplayed()
    }

    @Test fun blueAndRedHaveEqualShellTileBounds() {
        compose.setContent { SkillzTheme(dynamicColor=false) {
            com.kingkharnivore.skillz.ui.screen.shell.HeartRoomScreen(com.kingkharnivore.skillz.viewmodel.shell.ShellUiState()) {}
        } }
        val blue=compose.onNodeWithText("The Blue").fetchSemanticsNode().boundsInRoot
        val red=compose.onNodeWithText("The Red").fetchSemanticsNode().boundsInRoot
        assertEquals(blue.top,red.top,1f)
        assertEquals(blue.height,red.height,1f)
    }
    @Test fun returningToRedRestoresTheLastSettledEra() {
        val visible=androidx.compose.runtime.mutableStateOf(true)
        var lastEra=RedEra.TRIASSIC
        compose.setContent { SkillzTheme(dynamicColor=false) {
            if(visible.value) RedScreen(RedUiState(),simulatedSceneTime=12f,
                initialEra=lastEra,onEraChanged={lastEra=it})
        } }
        compose.onNodeWithTag("red-era-CRETACEOUS").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(RedEra.CRETACEOUS,lastEra); visible.value=false }
        compose.waitForIdle()
        compose.runOnIdle { visible.value=true }
        compose.onNodeWithText("145–66 million years ago").assertIsDisplayed()
        compose.onNodeWithTag("red-era-CRETACEOUS").assertIsSelected()
    }
    @Test fun creatureDeepLinkWorksWhileRedIsAlreadyOpenAndIsConsumedOnce() {
        val request=androidx.compose.runtime.mutableStateOf<String?>(null)
        var consumed=0
        compose.setContent { SkillzTheme(dynamicColor=false) {
            RedScreen(RedUiState(pebbles=1000),simulatedSceneTime=12f,
                initialCreature=request.value,onInitialCreatureOpened={consumed++;request.value=null})
        } }
        compose.runOnIdle { request.value="cretaceous_psittacosaurus" }
        compose.onNodeWithText("Psittacosaurus").assertIsDisplayed()
        compose.onNodeWithText("700 Pebbles").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1,consumed) }
        compose.onNode(hasClickAction() and hasText("Acquire dinosaur")).assertIsEnabled()
        compose.runOnIdle { request.value="triassic_saturnalia" }
        compose.onNodeWithText("Saturnalia").assertIsDisplayed()
        compose.onNodeWithText("180 Pebbles").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2,consumed) }
    }
    @Test fun largeOwnedCollectionOnlyComposesVisibleTrayItemsAndCanOpenTheLast() {
        val entries=com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog.entries.filter { it.era==RedEra.CRETACEOUS }
        val creatures=entries.mapIndexed { index,entry ->
            com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity(
                "red-$index",entry.id,1,"red_purchase",null,null,null,false,true)
        }
        compose.setContent { SkillzTheme(dynamicColor=false) {
            RedScreen(RedUiState(creatures=creatures),simulatedSceneTime=12f,initialEra=RedEra.CRETACEOUS)
        } }
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val last=context.getString(entries.last().nameRes)
        compose.onNodeWithText(last).assertDoesNotExist()
        compose.onNodeWithTag("red-owned-creatures").performScrollToNode(hasText(last))
        compose.onNodeWithText(last).assertIsDisplayed().performClick()
        compose.onNodeWithText("Release",substring=true).performScrollTo().assertIsDisplayed()
    }

}
