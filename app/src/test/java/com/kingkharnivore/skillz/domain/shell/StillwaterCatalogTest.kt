package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.hasKnownStillwaterStaticIcon
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.hasKnownTheBlueCreatureRenderer
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import com.kingkharnivore.skillz.utils.shell.CreatureRenderFamily
import com.kingkharnivore.skillz.utils.shell.CreatureScaleClass
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterCatalog
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.shell.calculateDropsForSoftFlow
import com.kingkharnivore.skillz.utils.shell.requiresStillwaterConfirmation
import com.kingkharnivore.skillz.utils.shell.validateStillwaterDraw
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
        assertEquals("Scorpionfish", CreatureCatalog.require("creature_lionfish").displayName)
        assertEquals("Needlefish", CreatureCatalog.require("creature_barracuda").displayName)
        assertFalse(CreatureCatalog.beyondBlue.any { it.displayName == "Lionfish" || it.displayName == "Barracuda" })

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
        val forbidden = setOf("Seahorse", "Leviathan", "Urchin", "Octopus", "Anglerfish", "Triggerfish", "Tuna", "Starfish")
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
            validateStillwaterDraw(
                StillwaterVessel.FISHBOWL,
                setOf(CreatureZone.SUNLIT_REEF),
                14_999L
            )
        }
        validateStillwaterDraw(StillwaterVessel.FISHBOWL, setOf(CreatureZone.SUNLIT_REEF), 15_000L)
    }

    @Test
    fun stillwaterReleaseValuesArePositiveAndScaleByVesselTier() {
        val clam = CreatureEconomy.releaseValuePearls("stillwater_clam")
        val lionfish = CreatureEconomy.releaseValuePearls("stillwater_lionfish")
        val barracuda = CreatureEconomy.releaseValuePearls("stillwater_barracuda")
        val coelacanth = CreatureEconomy.releaseValuePearls("stillwater_coelacanth")

        assertTrue(clam > 0)
        assertTrue(lionfish > clam)
        assertTrue(barracuda > lionfish)
        assertTrue(coelacanth > barracuda)
    }

    @Test
    fun allStillwaterCreaturesResolveToExplicitVisualHandlers() {
        StillwaterCatalog.creatures.forEach { entry ->
            val definition = CreatureCatalog.require(entry.creatureId)
            assertTrue(hasKnownStillwaterStaticIcon(definition.staticIconKey))
            assertTrue(hasKnownTheBlueCreatureRenderer(entry.creatureId))
        }
        assertTrue(hasKnownStillwaterStaticIcon(CreatureCatalog.require("stillwater_clam").staticIconKey))
        assertTrue(hasKnownTheBlueCreatureRenderer("stillwater_clam"))
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
