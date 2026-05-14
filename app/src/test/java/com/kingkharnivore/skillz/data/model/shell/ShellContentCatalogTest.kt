package com.kingkharnivore.skillz.data.model.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShellContentCatalogTest {
    @Test
    fun focusPearlObjects_areObjectsWithStableV1CostsAndCompatibleSlots() {
        val objects = ShellContentCatalog.focusPearlObjects

        assertEquals(5, objects.size)
        assertEquals(80, objects.first { it.findId == ShellContentCatalog.FOCUS_LAMP }.pearlCost)
        assertEquals(120, objects.first { it.findId == ShellContentCatalog.FOCUS_PERCH }.pearlCost)
        assertEquals(60, objects.first { it.findId == ShellContentCatalog.FOCUS_PEBBLES }.pearlCost)
        assertTrue(objects.all { it.kind == ShellRewardKind.OBJECT })
        assertTrue(objects.all { it.isPearlObject && it.placeable })
        assertTrue(objects.all { it.acceptedSlotTypes.isNotEmpty() })
    }

    @Test
    fun coreRewards_haveAnimalObjectTaxonomyAndDepth() {
        mapOf(
            ShellContentCatalog.FOCUS_MINNOW to ShellDepthTier.REEF,
            ShellContentCatalog.FOCUS_SEAHORSE to ShellDepthTier.DEEPER_REEF,
            ShellContentCatalog.FOCUS_MANTA to ShellDepthTier.OPEN_BLUE,
            ShellContentCatalog.FOCUS_WHALE to ShellDepthTier.DEEP_OCEAN
        ).forEach { (findId, depth) ->
            val def = ShellContentCatalog.find(findId)
            assertNotNull(def)
            assertEquals(ShellRewardKind.ANIMAL, def!!.kind)
            assertEquals(depth, def.depthTier)
        }

        assertEquals(ShellRewardKind.ANIMAL, ShellContentCatalog.find(ShellContentCatalog.FOCUS_OCTOPUS)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_PEBBLE)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_LAMP)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_PERCH)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_PEBBLES)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_CURTAIN)?.kind)
        assertEquals(ShellRewardKind.OBJECT, ShellContentCatalog.find(ShellContentCatalog.FOCUS_BUBBLES)?.kind)
    }

    @Test
    fun focusPearlObjects_areIndividualCopiesNotStacks() {
        assertTrue(ShellContentCatalog.focusPearlObjects.all { it.isPearlObject })
        assertFalse(ShellContentCatalog.focusPearlObjects.any { it.stackable })
    }

    @Test
    fun focusSlots_useLocalizedTitles() {
        assertTrue(ShellContentCatalog.focusSlots.all { it.titleRes != 0 })
    }

    @Test
    fun coreAnimalsAndPebbleHaveUpgradeForms() {
        listOf(
            ShellContentCatalog.FOCUS_MINNOW,
            ShellContentCatalog.FOCUS_SEAHORSE,
            ShellContentCatalog.FOCUS_MANTA,
            ShellContentCatalog.FOCUS_WHALE,
            ShellContentCatalog.FOCUS_OCTOPUS,
            ShellContentCatalog.FOCUS_PEBBLE
        ).forEach { findId ->
            val forms = ShellContentCatalog.upgradesFor(findId)
            assertTrue("$findId should have upgrade forms", forms.size >= 3)
            assertNotNull(ShellContentCatalog.nextUpgrade(findId, forms.first().upgradeStageId))
        }
    }

    @Test
    fun discoveriesMapToOctopusAndPebble() {
        assertEquals(ShellContentCatalog.FOCUS_OCTOPUS, ShellContentCatalog.discovery("discovery_octopus")?.grantsFindId)
        assertEquals(ShellContentCatalog.FOCUS_PEBBLE, ShellContentCatalog.discovery("discovery_pebble")?.grantsFindId)
    }
    @Test
    fun visibleShellStrings_doNotContainRetiredRewardNames() {
        val strings = File("app/src/main/res/values/strings.xml").readText()
        listOf(
            "Abyss Lanternfish",
            "Glow Shell",
            "Current Conch",
            "Anchor Coral",
            "Threshold Seahorse",
            "Return Turtle Stone",
            "Moon Coral Light",
            "Seahorse Perch",
            "Reef Pebble Bed",
            "Kelp Curtain",
            "Bubble Trail"
        ).forEach { retired ->
            assertFalse("Visible strings should not contain $retired", strings.contains(retired))
        }
    }

}
