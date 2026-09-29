package com.kingkharnivore.skillz.ui.screen.shell.rooms.stillwater

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StillwaterRoomScreenTest {
    @Test
    fun dropsCardUsesClaimableDropsAsPrimaryAndLifetimeOnlyAsSecondary() {
        val model = buildStillwaterDropsCardUiModel(
            ShellUiState(
                stillwaterClaimableDrops = 42_300L,
                stillwaterLifetimeDrops = 128_900L
            )
        )

        assertEquals(R.string.shell_stillwater_drops_available, model.primaryStringRes)
        assertEquals(42_300L, model.primaryDrops)
        assertEquals(128_900L, model.secondaryDrops)
    }

    @Test
    fun dropsCardShowsReadyToDrawOnlyWhenUnlockedAffordableVesselExists() {
        val notEnoughForFishbowl = buildStillwaterDropsCardUiModel(
            ShellUiState(
                stillwaterClaimableDrops = 14_999L,
                unlockedBlueZones = setOf(CreatureZone.SUNLIT_REEF)
            )
        )
        assertFalse(notEnoughForFishbowl.hasAvailableDraw)

        val enoughForFishbowl = buildStillwaterDropsCardUiModel(
            ShellUiState(
                stillwaterClaimableDrops = 15_000L,
                unlockedBlueZones = setOf(CreatureZone.SUNLIT_REEF)
            )
        )
        assertTrue(enoughForFishbowl.hasAvailableDraw)

        val noUnlockedAffordableVessel = buildStillwaterDropsCardUiModel(
            ShellUiState(
                stillwaterClaimableDrops = 100_000L,
                unlockedBlueZones = emptySet()
            )
        )
        assertFalse(noUnlockedAffordableVessel.hasAvailableDraw)

        val pondUnlockedAndAffordable = buildStillwaterDropsCardUiModel(
            ShellUiState(
                stillwaterClaimableDrops = 45_000L,
                unlockedBlueZones = setOf(CreatureZone.OPEN_BLUE)
            )
        )
        assertTrue(pondUnlockedAndAffordable.hasAvailableDraw)
    }

    @Test
    fun vesselAffordabilityAndProgressUseClaimableDrops() {
        val pond = buildStillwaterVesselCardUiModel(
            vessel = StillwaterVessel.POND,
            claimableDrops = 5_000L,
            isUnlocked = true
        )

        assertEquals(5_000L, pond.claimableDrops)
        assertFalse(pond.canAfford)
        assertFalse(pond.canDraw)
        assertEquals(40_000L, pond.dropsNeeded)
        assertEquals(5_000f / 45_000f, pond.progress, 0.0001f)

        val fishbowl = buildStillwaterVesselCardUiModel(
            vessel = StillwaterVessel.FISHBOWL,
            claimableDrops = 15_000L,
            isUnlocked = true
        )

        assertTrue(fishbowl.canAfford)
        assertTrue(fishbowl.canDraw)
        assertEquals(0L, fishbowl.dropsNeeded)
        assertEquals(1f, fishbowl.progress, 0.0001f)
    }

    @Test
    fun lockedVesselsCannotDrawEvenWhenClaimableDropsCoverCost() {
        val lake = buildStillwaterVesselCardUiModel(
            vessel = StillwaterVessel.LAKE,
            claimableDrops = 100_000L,
            isUnlocked = false
        )

        assertTrue(lake.canAfford)
        assertFalse(lake.canDraw)
    }
    @Test fun landVesselsUseTheSameAffordabilityAndProgressModels() {
        val habitats = com.kingkharnivore.skillz.utils.shell.LandStillwaterHabitat.entries
        habitats.forEach { habitat ->
            val filling = buildStillwaterVesselCardUiModel(habitat, habitat.dropCost - 1, true)
            assertFalse(filling.canDraw)
            assertEquals(1L, filling.dropsNeeded)
            val ready = buildStillwaterVesselCardUiModel(habitat, habitat.dropCost, true)
            assertTrue(ready.canDraw)
            assertEquals(1f, ready.progress)
            assertFalse(buildStillwaterVesselCardUiModel(habitat, habitat.dropCost, false).canDraw)
        }
        val state = ShellUiState(stillwaterClaimableDrops=15_000L, unlockedBlueZones=setOf(CreatureZone.GOLDEN_FIELDS))
        assertTrue(buildStillwaterDropsCardUiModel(state, habitats).hasAvailableDraw)
        assertFalse(buildStillwaterDropsCardUiModel(state).hasAvailableDraw)
    }

}
