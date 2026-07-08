package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
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
    fun defaultLevelSortOrdersHighestLevelThenNameThenStableKey() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("seahorse-1", ShellContentCatalog.FOCUS_SEAHORSE, level = 1),
                creature("minnow-1", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                creature("seahorse-2", ShellContentCatalog.FOCUS_SEAHORSE, level = 2),
                creature("minnow-2", ShellContentCatalog.FOCUS_MINNOW, level = 5),
                creature("minnow-3", ShellContentCatalog.FOCUS_MINNOW, level = 3)
            )
        )

        assertEquals(
            listOf(
                ShellContentCatalog.FOCUS_MINNOW to 5,
                ShellContentCatalog.FOCUS_MINNOW to 3,
                ShellContentCatalog.FOCUS_SEAHORSE to 2,
                ShellContentCatalog.FOCUS_MINNOW to 1,
                ShellContentCatalog.FOCUS_SEAHORSE to 1
            ),
            stacks.map { it.creatureId to it.level }
        )
    }

    @Test
    fun invalidSortKeysFallBackToLevel() {
        assertEquals(ChestSortOption.Level, ChestSortOption.fromKey(null))
        assertEquals(ChestSortOption.Level, ChestSortOption.fromKey("unknown"))
        assertEquals(ChestSortOption.NewestArrival, ChestSortOption.fromKey("newest_arrival"))
    }

    @Test
    fun recentSortUsesBestAvailableRecentTimestampDescending() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("minnow-old", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 10L, viewedAt = 500L),
                creature("seahorse-new", ShellContentCatalog.FOCUS_SEAHORSE, level = 1, acquiredAt = 20L, viewedAt = 900L),
                creature("minnow-newer", ShellContentCatalog.FOCUS_MINNOW, level = 2, acquiredAt = 30L, viewedAt = null)
            ),
            ChestSortOption.Recent
        )

        assertEquals(
            listOf(
                ShellContentCatalog.FOCUS_SEAHORSE to 1,
                ShellContentCatalog.FOCUS_MINNOW to 1,
                ShellContentCatalog.FOCUS_MINNOW to 2
            ),
            stacks.map { it.creatureId to it.level }
        )
    }

    @Test
    fun newestArrivalSortUsesNewestAcquiredCreatureWithinStack() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("minnow-early", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 10L),
                creature("minnow-late", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 100L),
                creature("seahorse", ShellContentCatalog.FOCUS_SEAHORSE, level = 1, acquiredAt = 50L)
            ),
            ChestSortOption.NewestArrival
        )

        assertEquals(
            listOf(ShellContentCatalog.FOCUS_MINNOW to 1, ShellContentCatalog.FOCUS_SEAHORSE to 1),
            stacks.map { it.creatureId to it.level }
        )
        assertEquals(2, stacks.first().count)
    }

    @Test
    fun oldestArrivalSortUsesOldestAcquiredCreatureWithinStack() {
        val stacks = buildChestInventoryStacks(
            listOf(
                creature("minnow-early", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 10L),
                creature("minnow-late", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 100L),
                creature("seahorse", ShellContentCatalog.FOCUS_SEAHORSE, level = 1, acquiredAt = 5L)
            ),
            ChestSortOption.OldestArrival
        )

        assertEquals(
            listOf(ShellContentCatalog.FOCUS_SEAHORSE to 1, ShellContentCatalog.FOCUS_MINNOW to 1),
            stacks.map { it.creatureId to it.level }
        )
    }

    @Test
    fun sortingDoesNotChangeGroupingCountsOrInputData() {
        val finds = listOf(
            creature("minnow-1", ShellContentCatalog.FOCUS_MINNOW, level = 3, acquiredAt = 100L),
            creature("minnow-2", ShellContentCatalog.FOCUS_MINNOW, level = 3, acquiredAt = 200L),
            creature("minnow-3", ShellContentCatalog.FOCUS_MINNOW, level = 1, acquiredAt = 300L)
        )
        val before = finds.toList()

        val stacks = buildChestInventoryStacks(finds, ChestSortOption.NewestArrival)

        assertEquals(before, finds)
        assertEquals(listOf(ShellContentCatalog.FOCUS_MINNOW to 1, ShellContentCatalog.FOCUS_MINNOW to 3), stacks.map { it.creatureId to it.level })
        assertEquals(listOf(1, 2), stacks.map { it.count })
    }

    @Test
    fun releaseSelectionTargetsOnlySelectedStackLevelAndClampsCount() {
        val stack = ChestInventoryStackUiModel(
            creatureId = ShellContentCatalog.FOCUS_MINNOW,
            creatureName = "Minnow",
            level = 3,
            count = 4,
            iconKey = "minnow"
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
            iconKey = "minnow"
        )
        val eachReward = CreatureEconomy.releaseValuePearls(ShellContentCatalog.FOCUS_MINNOW, 3)

        assertEquals(eachReward * 2, chestReleaseRewardPearls(stack, 2))
        assertEquals(eachReward * 4, chestReleaseRewardPearls(stack, 10))
        assertEquals(eachReward, chestReleaseRewardPearls(stack, 0))
    }


    @Test
    fun stillwaterClamAppearsInChestAndCanReleaseForPearls() {
        val stacks = buildChestInventoryStacks(
            listOf(creature("clam-1", "stillwater_clam", level = 1))
        )

        val stack = stacks.single()
        assertEquals("stillwater_clam", stack.creatureId)
        assertTrue(stack.isStillwaterExclusive)
        assertTrue(chestReleaseRewardPearls(stack, 1) > 0)
    }

    @Test
    fun normalBlueCreatureReleaseBehaviorRemainsEnabled() {
        val stack = ChestInventoryStackUiModel(
            creatureId = ShellContentCatalog.FOCUS_MINNOW,
            creatureName = "Minnow",
            level = 1,
            count = 1,
            iconKey = "minnow"
        )

        assertTrue(chestReleaseRewardPearls(stack, 1) > 0)
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
        status: String = CreatureStatus.ACTIVE,
        acquiredAt: Long = 1L,
        viewedAt: Long? = null
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = acquiredAt,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = null,
        customName = null,
        isNew = false,
        isArchivedInChest = false,
        viewedAt = viewedAt,
        animalLevel = level,
        creatureStatus = status
    )
}
