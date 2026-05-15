package com.kingkharnivore.skillz.data.model.shell

import com.kingkharnivore.skillz.R
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
            "Bubble Trail",
            "Pearl Cluster"
        ).forEach { retired ->
            assertFalse("Visible strings should not contain $retired", strings.contains(retired))
        }
    }

    @Test
    fun slotCompatibility_respectsRewardKind() {
        val creaturePerch = ShellContentCatalog.focusSlots.first { it.slotType == ShellSlotType.CREATURE_PERCH }
        val currentPath = ShellSlotDefinition("test_current_path", ShellRoomId.FOCUS, ShellSlotType.CURRENT_PATH, 0, 0f, 0f, 0f, 0f, 0, setOf(ShellFindCategory.CREATURES, ShellFindCategory.TRINKETS))
        val memoryNook = ShellContentCatalog.focusSlots.first { it.slotType == ShellSlotType.MEMORY_NOOK }
        val reefShelf = ShellContentCatalog.focusSlots.first { it.slotType == ShellSlotType.REEF_SHELF }

        assertTrue(ShellContentCatalog.isCompatibleWithSlot(creaturePerch, ShellContentCatalog.find(ShellContentCatalog.FOCUS_MINNOW)!!))
        assertFalse(ShellContentCatalog.isCompatibleWithSlot(creaturePerch, ShellContentCatalog.find(ShellContentCatalog.FOCUS_BUBBLES)!!))
        assertTrue(ShellContentCatalog.isCompatibleWithSlot(creaturePerch, ShellContentCatalog.find(ShellContentCatalog.FOCUS_PERCH)!!))
        assertTrue(ShellContentCatalog.isCompatibleWithSlot(memoryNook, ShellContentCatalog.find(ShellContentCatalog.FOCUS_PEBBLE)!!))
        assertTrue(ShellContentCatalog.isCompatibleWithSlot(currentPath, ShellContentCatalog.find(ShellContentCatalog.FOCUS_BUBBLES)!!))
        assertFalse(ShellContentCatalog.isCompatibleWithSlot(reefShelf, ShellContentCatalog.find(ShellContentCatalog.FOCUS_WHALE)!!))
    }

    @Test
    fun trinketsUseDistinctOceanNames() {
        assertEquals("trinket_glimmer", ShellContentCatalog.TRINKET_GLIMMER)
        assertEquals(ShellRewardKind.TRINKET, ShellContentCatalog.find(ShellContentCatalog.TRINKET_GLIMMER)?.kind)
        assertEquals(ShellRewardKind.TRINKET, ShellContentCatalog.find(ShellContentCatalog.TRINKET_SEA_GLASS_SHARD)?.kind)
        assertEquals(ShellContentCatalog.TRINKET_GLIMMER, ShellContentCatalog.discovery("discovery_glimmer")?.grantsFindId)
        val strings = File("app/src/main/res/values/strings.xml").readText()
        assertTrue(strings.contains(">Seaglass</string>"))
        assertTrue(strings.contains(">Glimmers</string>"))
        assertFalse(strings.contains(">Pearls</string>"))
    }

    @Test
    fun softFlowBadgeIsHiddenFromV1Catalog() {
        assertFalse(ShellContentCatalog.badges.any { it.badgeId == "badge_soft_flow" })
    }

    @Test
    fun theBlueRoom_usesTheBlueIdentifiers() {
        val theBlue = ShellContentCatalog.rooms.single { it.roomId == ShellRoomId.THE_BLUE }

        assertEquals("THE_BLUE", ShellRoomId.THE_BLUE.name)
        assertEquals(R.string.shell_room_the_blue_title, theBlue.titleRes)
        assertEquals(R.string.shell_room_the_blue_description, theBlue.descriptionRes)
    }

    @Test
    fun focusV1_hasReservedSurgeCurrentNookForSurgeRewardsOnly() {
        val slot = ShellContentCatalog.focusSlots.single { it.slotId == "surge_current_nook" }

        assertEquals(ShellSlotType.SURGE_CURRENT, slot.slotType)
        assertTrue(slot.titleRes != 0)
        listOf(
            ShellContentCatalog.FOCUS_MINNOW,
            ShellContentCatalog.FOCUS_SEAHORSE,
            ShellContentCatalog.FOCUS_MANTA,
            ShellContentCatalog.FOCUS_WHALE,
            ShellContentCatalog.FOCUS_OCTOPUS,
            ShellContentCatalog.FOCUS_BUBBLES,
            ShellContentCatalog.FOCUS_PERCH,
            ShellContentCatalog.FOCUS_PEBBLES,
            ShellContentCatalog.FOCUS_LAMP,
            ShellContentCatalog.FOCUS_CURTAIN
        ).forEach { findId ->
            assertFalse("$findId should not fit the reserved Surge Current Nook", ShellContentCatalog.isCompatibleWithSlot(slot, ShellContentCatalog.find(findId)!!))
        }
        assertFalse("No current V1 reward should fit the reserved Surge Current Nook", ShellContentCatalog.finds.any { ShellContentCatalog.isCompatibleWithSlot(slot, it) })
    }

    @Test
    fun shellV1Strings_labelTheBlueAsAnimalEcosystem() {
        val strings = File("app/src/main/res/values/strings.xml").readText()

        assertTrue(strings.contains("""<string name="shell_room_the_blue_title">The Blue</string>"""))
        assertTrue(strings.contains("Animals encountered through regular Flows swim here."))
        assertFalse(strings.contains("Coral " + "Reef"))
        assertTrue(strings.contains("""shell_basin_soon_upgrade">%1$s · %2$s"""))
        assertTrue(strings.contains("""<string name="shell_slot_surge_current_nook">Surge Current Nook</string>"""))
        assertTrue(strings.contains("Surge rewards can be displayed here. Complete a Surge to bring a Current into this nook."))
        assertFalse(strings.contains("Surge energy will bring the reef alive."))
        assertFalse(strings.contains("Reserved for Surge and Current echoes."))
    }

}
