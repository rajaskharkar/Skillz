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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class StillwaterCatalogTest {


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
    fun stillwaterReleaseValuesArePositiveAndScaleByVesselTier() {
        val clam = CreatureEconomy.releaseValuePearls("stillwater_clam")
        val lionfish = CreatureEconomy.releaseValuePearls("stillwater_lionfish")
        val barracuda = CreatureEconomy.releaseValuePearls("stillwater_barracuda")
        val coelacanth = CreatureEconomy.releaseValuePearls("stillwater_coelacanth")

        assertEquals(125, clam)
        assertEquals(208, lionfish)
        assertEquals(375, barracuda)
        assertEquals(625, coelacanth)
        assertTrue(clam > 0)
        assertTrue(lionfish > clam)
        assertTrue(barracuda > lionfish)
        assertTrue(coelacanth > barracuda)
        assertTrue(clam < CreatureEconomy.canonicalPearlValue("stillwater_clam"))
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


}
