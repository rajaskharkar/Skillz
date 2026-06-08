package com.kingkharnivore.skillz.domain.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StillwaterCatalogTest {
    @Test
    fun stillwaterCatalogueContainsExclusiveCreaturesOnly() {
        assertEquals(32, StillwaterCatalog.creatures.size)
        assertEquals(32, CreatureCatalog.stillwater.size)
        assertTrue(CreatureCatalog.stillwater.all { it.sourceType == CreatureSourceType.STILLWATER })

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
        val forbidden = setOf("Seahorse", "Leviathan", "Urchin", "Octopus", "Anglerfish")
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
