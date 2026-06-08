package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellChestInventoryMapperTest {
    @Test
    fun activeCreaturesStackByCreatureAndLevel() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("minnow-1", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                creature("minnow-2", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                creature("minnow-3", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                creature("minnow-4", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                creature("minnow-5", ShellContentCatalog.FOCUS_MINNOW, level = 3),
                creature("minnow-6", ShellContentCatalog.FOCUS_MINNOW, level = 3),
                creature("minnow-7", ShellContentCatalog.FOCUS_MINNOW, level = 3)
            )
        )

        val minnowLevelOne = stacks.single { it.creatureId == ShellContentCatalog.FOCUS_MINNOW && it.level == 1 }
        val minnowLevelThree = stacks.single { it.creatureId == ShellContentCatalog.FOCUS_MINNOW && it.level == 3 }
        assertEquals(4, minnowLevelOne.count)
        assertEquals(3, minnowLevelThree.count)
    }

    @Test
    fun inactiveAndLegacyRecordsDoNotAppearInChestStacks() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("active", ShellContentCatalog.FOCUS_MINNOW, level = 3),
                creature("released", ShellContentCatalog.FOCUS_MINNOW, level = 3, status = CreatureStatus.RELEASED),
                creature("beyond", ShellContentCatalog.FOCUS_MINNOW, level = 3, status = CreatureStatus.USED_BEYOND_BLUE),
                creature("object", ShellContentCatalog.FOCUS_PEBBLE, level = 1)
            )
        )

        assertEquals(1, stacks.size)
        assertEquals(ShellContentCatalog.FOCUS_MINNOW, stacks.single().creatureId)
        assertEquals(3, stacks.single().level)
        assertEquals(1, stacks.single().count)
    }

    @Test
    fun releaseSelectionTargetsOnlySelectedStackLevelAndClampsCount() {
        val stack = ChestInventoryStackUiModel(
            creatureId = ShellContentCatalog.FOCUS_MINNOW,
            creatureName = "Minnow",
            level = 3,
            count = 4,
            iconKey = "minnow",
            rarityLabel = "Common",
            sortOrder = 1
        )

        assertEquals(mapOf(3 to 2), chestReleaseSelection(stack, 2))
        assertEquals(mapOf(3 to 4), chestReleaseSelection(stack, 10))
        assertEquals(mapOf(3 to 1), chestReleaseSelection(stack, 0))
    }

    @Test
    fun releaseRewardPreviewUsesSelectedStackLevelAndClampsCount() {
        val stack = ChestInventoryStackUiModel(
            creatureId = ShellContentCatalog.FOCUS_MINNOW,
            creatureName = "Minnow",
            level = 3,
            count = 4,
            iconKey = "minnow",
            rarityLabel = "Common",
            sortOrder = 1
        )
        val eachReward = CreatureEconomy.releaseValuePearls(ShellContentCatalog.FOCUS_MINNOW, 3)

        assertEquals(eachReward * 2, chestReleaseRewardPearls(stack, 2))
        assertEquals(eachReward * 4, chestReleaseRewardPearls(stack, 10))
        assertEquals(eachReward, chestReleaseRewardPearls(stack, 0))
    }

    @Test
    fun countBadgeIsOnlyShownForStackedCopies() {
        assertFalse(shouldShowChestCountBadge(1))
        assertTrue(shouldShowChestCountBadge(2))
    }

    private fun creature(
        instanceId: String,
        findId: String,
        level: Int,
        status: String = CreatureStatus.ACTIVE
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = 1L,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = null,
        customName = null,
        isNew = false,
        isArchivedInChest = false,
        animalLevel = level,
        creatureStatus = status
    )
}
