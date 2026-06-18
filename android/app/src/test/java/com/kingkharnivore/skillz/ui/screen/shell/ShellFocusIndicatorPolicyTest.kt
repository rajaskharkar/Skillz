package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShellFocusIndicatorPolicyTest {
    @Test
    fun focusIndicator_isFalseForEmptySlotsWithoutAffordablePearlAction() {
        val state = ShellUiState(pearlBalance = 0)

        assertFalse(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun focusIndicator_ignoresNewRestingItemsWithoutAffordablePearlAction() {
        val state = ShellUiState(
            pearlBalance = 0,
            finds = listOf(findInstance("minnow-1", ShellContentCatalog.FOCUS_MINNOW, isNew = true))
        )

        assertFalse(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun focusIndicator_ignoresDisplayedNewItemWithoutAffordableUpgrade() {
        val state = ShellUiState(
            pearlBalance = 0,
            finds = listOf(findInstance("minnow-1", ShellContentCatalog.FOCUS_MINNOW, isNew = true)),
            focusPlacements = listOf(placement("creature_perch_left", "minnow-1"))
        )

        assertFalse(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun focusIndicator_isTrueForDisplayedAffordableUpgrade() {
        val state = ShellUiState(
            pearlBalance = 80,
            finds = listOf(findInstance("minnow-1", ShellContentCatalog.FOCUS_MINNOW)),
            focusPlacements = listOf(placement("creature_perch_left", "minnow-1"))
        )

        assertTrue(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun focusIndicator_isTrueForAffordablePearlObjectWithCompatibleEmptySlot() {
        val state = ShellUiState(pearlBalance = 80)

        assertTrue(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun focusIndicator_isFalseForAffordablePearlObjectWithNoCompatibleEmptySlot() {
        val state = ShellUiState(
            pearlBalance = 1_000,
            focusPlacements = ShellContentCatalog.focusSlots
                .filterNot { it.slotId == "surge_current_nook" }
                .map { slot -> placement(slot.slotId, "occupied-${slot.slotId}") }
        )

        assertFalse(hasAffordableFocusPearlAction(state))
    }

    @Test
    fun shellIndicatorDots_doNotUseTertiaryDirectly() {
        val source = File("app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/ShellRootScreen.kt").readText()

        assertTrue(source.contains("color = shellIndicatorColor()"))
        assertFalse(source.contains("hasIndicator) {\n                Surface(\n                    shape = CircleShape,\n                    color = scheme.tertiary"))
    }

    private fun findInstance(
        instanceId: String,
        findId: String,
        isNew: Boolean = false,
        currentUpgradeStageId: String? = ShellContentCatalog.upgradesFor(findId).firstOrNull()?.upgradeStageId
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = 1L,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = currentUpgradeStageId,
        customName = null,
        isNew = isNew,
        isArchivedInChest = false
    )

    private fun placement(slotId: String, instanceId: String) = ShellPlacementEntity(
        placementId = "placement-$slotId",
        roomId = ShellRoomId.FOCUS.name,
        slotId = slotId,
        instanceId = instanceId,
        placedAt = 1L
    )
}
