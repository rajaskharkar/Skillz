package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheBlueUiModelTest {
    @Test
    fun animalSpeciesMapToExpectedZones() {
        assertEquals(TheBlueZoneId.SUNLIT_REEF, zoneForFind(ShellContentCatalog.FOCUS_MINNOW))
        assertEquals(TheBlueZoneId.DEEPER_REEF, zoneForFind(ShellContentCatalog.FOCUS_SEAHORSE))
        assertEquals(TheBlueZoneId.DEEPER_REEF, zoneForFind(ShellContentCatalog.FOCUS_OCTOPUS))
        assertEquals(TheBlueZoneId.OPEN_BLUE, zoneForFind(ShellContentCatalog.FOCUS_MANTA))
        assertEquals(TheBlueZoneId.GREAT_BLUE, zoneForFind(ShellContentCatalog.FOCUS_WHALE))
    }

    @Test
    fun stateGroupsAnimalsByZoneAndComputesCounts() {
        val state = buildTheBlueUiState(
            finds = listOf(
                find("minnow-1", ShellContentCatalog.FOCUS_MINNOW, stage = "focus_minnow_base"),
                find("minnow-2", ShellContentCatalog.FOCUS_MINNOW, stage = "focus_minnow_luminous", isNew = true),
                find("seahorse-1", ShellContentCatalog.FOCUS_SEAHORSE),
                find("manta-1", ShellContentCatalog.FOCUS_MANTA),
                find("whale-1", ShellContentCatalog.FOCUS_WHALE),
                find("pebble-1", ShellContentCatalog.FOCUS_PEBBLE),
                find("trinket-1", ShellContentCatalog.TRINKET_SEA_GLASS_SHARD)
            ),
            focusPlacements = listOf(placement("minnow-1"))
        )

        val minnow = state.zones.single { it.zoneId == TheBlueZoneId.SUNLIT_REEF }.animals.single()
        assertEquals(5, state.totalAnimals)
        assertEquals(4, state.speciesCount)
        assertEquals(TheBlueZoneId.GREAT_BLUE, state.deepestZoneId)
        assertEquals(2, minnow.totalCount)
        assertEquals(1, minnow.displayedInFocusCount)
        assertEquals(1, minnow.restingCount)
        assertTrue(minnow.isNew)
        assertEquals("focus_minnow_luminous", minnow.bestFormStageId)
        assertEquals(TheBlueZoneId.OPEN_BLUE, state.zones.single { it.zoneId == TheBlueZoneId.OPEN_BLUE }.animals.single().zoneId)
        assertEquals(TheBlueZoneId.GREAT_BLUE, state.zones.single { it.zoneId == TheBlueZoneId.GREAT_BLUE }.animals.single().zoneId)
        assertFalse(state.zones.any { zone -> zone.animals.any { it.findId == ShellContentCatalog.FOCUS_PEBBLE } })
        assertFalse(state.zones.any { zone -> zone.animals.any { it.findId == ShellContentCatalog.TRINKET_SEA_GLASS_SHARD } })
    }


    @Test
    fun releasedAndUsedCreaturesAreHistoricalButNotActiveInTheBlue() {
        val state = buildTheBlueUiState(
            finds = listOf(
                find("manta-active", ShellContentCatalog.FOCUS_MANTA, level = 2),
                find("manta-released", ShellContentCatalog.FOCUS_MANTA, status = CreatureStatus.RELEASED, level = 8),
                find("manta-used", ShellContentCatalog.FOCUS_MANTA, status = CreatureStatus.USED_BEYOND_BLUE, level = 4)
            ),
            focusPlacements = listOf(placement("manta-active"))
        )

        val manta = state.zones.single { it.zoneId == TheBlueZoneId.OPEN_BLUE }.animals.single()
        assertEquals(1, manta.totalCount)
        assertEquals(3, manta.lifetimeEncounteredCount)
        assertEquals(1, manta.releasedCount)
        assertEquals(1, manta.usedBeyondBlueCount)
        assertEquals(2, manta.highestLevel)
        assertEquals("manta-active", manta.firstActiveInstanceId)
        assertEquals("manta-active", manta.highestLevelActiveInstanceId)
    }

    @Test
    fun animalLevelCountsGroupCopiesByLevelHighestFirst() {
        val state = buildTheBlueUiState(
            finds = listOf(
                find("minnow-l4", ShellContentCatalog.FOCUS_MINNOW, level = 4),
                find("minnow-l1-a", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                find("minnow-l1-b", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                find("minnow-l1-c", ShellContentCatalog.FOCUS_MINNOW, level = 1),
                find("minnow-l1-d", ShellContentCatalog.FOCUS_MINNOW, level = 1)
            ),
            focusPlacements = emptyList()
        )

        val minnow = state.zones.single { it.zoneId == TheBlueZoneId.SUNLIT_REEF }.animals.single()

        assertEquals(5, minnow.totalCount)
        assertEquals(4, minnow.highestLevel)
        assertEquals(listOf(FormCountUiModel("Level 4", 1), FormCountUiModel("Level 1", 4)), minnow.levelCounts)
    }


    @Test
    fun releaseValueSummaryOnlyShowsPerCreatureValueForSingleActiveLevel() {
        val state = buildTheBlueUiState(
            finds = listOf(
                find("seahorse-l4-a", ShellContentCatalog.FOCUS_SEAHORSE, level = 4),
                find("seahorse-l4-b", ShellContentCatalog.FOCUS_SEAHORSE, level = 4)
            ),
            focusPlacements = emptyList()
        )

        val seahorse = state.zones.single { it.zoneId == TheBlueZoneId.DEEPER_REEF }.animals.single()

        assertFalse(seahorse.releaseValueVariesByLevel)
        assertEquals(CreatureEconomy.releaseValuePearls(ShellContentCatalog.FOCUS_SEAHORSE, 4), seahorse.releaseValuePearls)
    }

    @Test
    fun releaseValueSummaryVariesByLevelWhenMultipleActiveLevelsExist() {
        val state = buildTheBlueUiState(
            finds = listOf(
                find("seahorse-l1", ShellContentCatalog.FOCUS_SEAHORSE, level = 1),
                find("seahorse-l20", ShellContentCatalog.FOCUS_SEAHORSE, level = 20)
            ),
            focusPlacements = emptyList()
        )

        val seahorse = state.zones.single { it.zoneId == TheBlueZoneId.DEEPER_REEF }.animals.single()

        assertTrue(seahorse.releaseValueVariesByLevel)
        assertNull(seahorse.releaseValuePearls)
    }

    @Test
    fun markingTheBlueAnimalsSeenClearsOnlyAnimalNewFlags() {
        val finds = listOf(
            find("minnow-1", ShellContentCatalog.FOCUS_MINNOW, isNew = true),
            find("pebble-1", ShellContentCatalog.FOCUS_PEBBLE, isNew = true)
        )

        assertEquals(1, buildTheBlueUiState(finds, emptyList()).newAnimalCount)

        val afterMark = finds.map { instance ->
            if (instance.findId in ShellContentCatalog.regularFlowAnimalFindIds) {
                instance.copy(isNew = false)
            } else {
                instance
            }
        }

        assertEquals(0, buildTheBlueUiState(afterMark, emptyList()).newAnimalCount)
        assertFalse(afterMark.first { it.findId == ShellContentCatalog.FOCUS_MINNOW }.isNew)
        assertTrue(afterMark.first { it.findId == ShellContentCatalog.FOCUS_PEBBLE }.isNew)
    }


    @Test
    fun pagerPageIndexMapsToTheBlueZones() {
        assertEquals(TheBlueZoneId.SUNLIT_REEF, theBlueZoneForPage(-1))
        assertEquals(TheBlueZoneId.SUNLIT_REEF, theBlueZoneForPage(0))
        assertEquals(TheBlueZoneId.DEEPER_REEF, theBlueZoneForPage(1))
        assertEquals(TheBlueZoneId.OPEN_BLUE, theBlueZoneForPage(2))
        assertEquals(TheBlueZoneId.GREAT_BLUE, theBlueZoneForPage(3))
        assertEquals(TheBlueZoneId.GREAT_BLUE, theBlueZoneForPage(99))
    }

    @Test
    fun depthRailNavigationPathVisitsIntermediateZonesWhenDescendingOrRising() {
        assertEquals(
            listOf(TheBlueZoneId.DEEPER_REEF, TheBlueZoneId.OPEN_BLUE, TheBlueZoneId.GREAT_BLUE),
            theBlueSequentialNavigationPath(TheBlueZoneId.SUNLIT_REEF, TheBlueZoneId.GREAT_BLUE)
        )
        assertEquals(
            listOf(TheBlueZoneId.OPEN_BLUE, TheBlueZoneId.DEEPER_REEF, TheBlueZoneId.SUNLIT_REEF),
            theBlueSequentialNavigationPath(TheBlueZoneId.GREAT_BLUE, TheBlueZoneId.SUNLIT_REEF)
        )
        assertEquals(
            listOf(TheBlueZoneId.OPEN_BLUE),
            theBlueSequentialNavigationPath(TheBlueZoneId.DEEPER_REEF, TheBlueZoneId.OPEN_BLUE)
        )
        assertTrue(theBlueSequentialNavigationPath(TheBlueZoneId.OPEN_BLUE, TheBlueZoneId.OPEN_BLUE).isEmpty())
    }

    @Test
    fun offscreenHorizontalPassStartsAndEndsPastAnimalBounds() {
        val screenWidth = 1000f
        val animalWidth = 200f
        val margin = 50f

        assertEquals(-250f, offscreenHorizontalPassX(0f, screenWidth, animalWidth, margin, leftToRight = true), 0.001f)
        assertEquals(1250f, offscreenHorizontalPassX(1f, screenWidth, animalWidth, margin, leftToRight = true), 0.001f)
        assertEquals(1250f, offscreenHorizontalPassX(0f, screenWidth, animalWidth, margin, leftToRight = false), 0.001f)
        assertEquals(-250f, offscreenHorizontalPassX(1f, screenWidth, animalWidth, margin, leftToRight = false), 0.001f)
    }

    @Test
    fun representativeVisibleCountUsesDensityTiersWithoutRenderingEveryCopy() {
        assertEquals(0, representativeVisibleCount(0, maxVisible = 12))
        assertEquals(1, representativeVisibleCount(1, maxVisible = 12))
        assertEquals(4, representativeVisibleCount(4, maxVisible = 12))
        assertEquals(6, representativeVisibleCount(8, maxVisible = 12))
        assertEquals(9, representativeVisibleCount(30, maxVisible = 12))
        assertEquals(12, representativeVisibleCount(100, maxVisible = 12))
        assertEquals(3, representativeVisibleCount(10, maxVisible = 3))
    }

    @Test
    fun displayDisabledReasonDistinguishesNoSlotFromNoRestingCopy() {
        assertEquals(TheBlueDisplayDisabledReason.NO_FOCUS_SLOT, theBlueDisplayDisabledReason(null, null))
        assertEquals(TheBlueDisplayDisabledReason.NO_FOCUS_SLOT, theBlueDisplayDisabledReason(null, "copy-1"))
        assertEquals(TheBlueDisplayDisabledReason.NO_RESTING_COPY, theBlueDisplayDisabledReason("slot-1", null))
        assertNull(theBlueDisplayDisabledReason("slot-1", "copy-1"))
    }

    @Test
    fun emptyStateAppearsWhenNoAnimalsAreOwned() {
        val state = buildTheBlueUiState(
            finds = listOf(find("pebble-1", ShellContentCatalog.FOCUS_PEBBLE)),
            focusPlacements = emptyList()
        )

        assertTrue(state.isEmpty)
        assertEquals(0, state.totalAnimals)
        assertEquals(0, state.speciesCount)
        assertNull(state.deepestZoneId)
        assertTrue(state.zones.all { it.animals.isEmpty() })
    }

    @Test
    fun animalCardAccessibilityTextContainsNameCountZoneAndSource() {
        val name = "Minnow"
        val count = 24
        val zone = "Sunlit Reef"
        val source = "From regular Flows lasting 10 minutes or more."
        val contentDescription = "$name. Animal. Count $count. $zone. $source Displayed in Focus: 2."

        assertTrue(contentDescription.contains(name))
        assertTrue(contentDescription.contains(count.toString()))
        assertTrue(contentDescription.contains(zone))
        assertTrue(contentDescription.contains(source))
    }

    private fun find(
        instanceId: String,
        findId: String,
        stage: String? = null,
        isNew: Boolean = false,
        status: String = CreatureStatus.ACTIVE,
        level: Int = 1
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = 1L,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = stage,
        customName = null,
        isNew = isNew,
        isArchivedInChest = false,
        animalLevel = level,
        creatureStatus = status
    )

    private fun placement(instanceId: String) = ShellPlacementEntity(
        placementId = "placement-$instanceId",
        roomId = ShellRoomId.FOCUS.name,
        slotId = "left_creature_perch",
        instanceId = instanceId,
        placedAt = 1L
    )
}
