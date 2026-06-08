package com.kingkharnivore.skillz.domain.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class StillwaterCatalogTest {
    @Test
    fun stillwaterCatalogueContainsExclusiveCreaturesOnly() {
        assertEquals(32, StillwaterCatalog.creatures.size)
        assertEquals(32, CreatureCatalog.stillwater.size)
        assertTrue(CreatureCatalog.stillwater.all { it.sourceType == CreatureSourceType.STILLWATER })
        assertTrue(CreatureCatalog.flowEarned.none { it.sourceType == CreatureSourceType.STILLWATER })
        assertTrue(CreatureCatalog.beyondBlue.none { it.sourceType == CreatureSourceType.STILLWATER })
        assertTrue(StillwaterCatalog.creatures.any { it.displayName == "Lionfish" })
        assertTrue(StillwaterCatalog.creatures.any { it.displayName == "Barracuda" })

        val blueCreatures = CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER }
        val stillwaterCreatures = CreatureCatalog.stillwater
        assertFalse(
            blueCreatures.any { blue ->
                stillwaterCreatures.any { stillwater ->
                    blue.displayName.equals(stillwater.displayName, ignoreCase = true) ||
                        blue.creatureId.equals(stillwater.creatureId, ignoreCase = true)
                }
            }
        )
    }

    @Test
    fun forbiddenStillwaterNamesAreNotPresent() {
        val forbidden = setOf("Seahorse", "Leviathan", "Urchin", "Octopus", "Anglerfish", "Triggerfish", "Tuna")
        val names = CreatureCatalog.stillwater.map { it.displayName }.toSet()
        assertTrue(names.none { it in forbidden })
    }

    @Test
    fun softFlowDropsUseSecondsAndNeverGoNegative() {
        assertEquals(600L, calculateDropsForSoftFlow(600L))
        assertEquals(0L, calculateDropsForSoftFlow(0L))
        assertEquals(0L, calculateDropsForSoftFlow(-10L))
    }

    @Test
    fun stillwaterScaleAndDrawValidationFollowEconomyRules() {
        assertEquals(CreatureScaleClass.LARGE, CreatureCatalog.require("stillwater_barracuda").scaleClass)
        assertEquals(CreatureScaleClass.LARGE, CreatureCatalog.require("stillwater_coelacanth").scaleClass)
        assertEquals(CreatureRenderFamily.LARGE_FISH, CreatureCatalog.require("stillwater_coelacanth").renderFamily)
        assertFailsWith<IllegalArgumentException> {
            validateStillwaterDraw(StillwaterVessel.LAKE, setOf(CreatureZone.SUNLIT_REEF), 100_000L)
        }
        assertFailsWith<IllegalArgumentException> {
            validateStillwaterDraw(StillwaterVessel.FISHBOWL, setOf(CreatureZone.SUNLIT_REEF), 14_999L)
        }
        validateStillwaterDraw(StillwaterVessel.FISHBOWL, setOf(CreatureZone.SUNLIT_REEF), 15_000L)
    }

    @Test
    fun vesselsHaveExpectedCostsAndConfirmationRules() {
        assertEquals(15_000L, StillwaterVessel.FISHBOWL.dropCost)
        assertEquals(25_000L, StillwaterVessel.AQUARIUM.dropCost)
        assertEquals(45_000L, StillwaterVessel.POND.dropCost)
        assertEquals(75_000L, StillwaterVessel.LAKE.dropCost)
        assertFalse(requiresStillwaterConfirmation(StillwaterVessel.FISHBOWL))
        assertFalse(requiresStillwaterConfirmation(StillwaterVessel.AQUARIUM))
        assertTrue(requiresStillwaterConfirmation(StillwaterVessel.POND))
        assertTrue(requiresStillwaterConfirmation(StillwaterVessel.LAKE))
    }
}
