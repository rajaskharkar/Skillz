package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import org.junit.Assert.*
import org.junit.Test

class LandCatalogTest {
    @Test fun exactApprovedRoster() {
        val rows = javaClass.getResourceAsStream("/land/catalog.tsv")!!.bufferedReader().readLines().drop(1)
        assertEquals(114, rows.size)
        assertEquals(63, LandCreatureCatalog.main.size)
        assertEquals(51, LandCreatureCatalog.restorative.size)
        rows.forEach { line ->
            val values = line.split('\t')
            val creature = CreatureCatalog.require(values[0])
            assertEquals(values[1], creature.displayName)
            assertEquals(values[2], creature.zone.name)
            assertEquals(values[3], creature.sourceType.name)
            assertEquals(values[4].toIntOrNull(), creature.pearlPrice)
            assertEquals(values[5].toIntOrNull(), creature.arcFlowRequirement)
            assertEquals(CreatureRealm.LAND, creature.realm)
            assertNotNull(ShellContentCatalog.find(creature.creatureId))
            assertTrue(creature.titleRes != 0)
        }
        assertEquals(listOf(11, 13, 14, 12, 13), LandCreatureCatalog.mainZones.map { z -> LandCreatureCatalog.main.count { it.zone == z } })
        assertEquals(listOf(11, 13, 9, 8, 10), LandStillwaterHabitat.entries.map { h -> LandCreatureCatalog.restorative.count { it.restorativeHabitat == h } })
        assertEquals(58, LandCreatureCatalog.main.count { it.sourceType == CreatureSourceType.BEYOND_BLUE })
    }

    @Test fun identitiesAreGloballyUniqueAndRestorativeUsesDrops() {
        assertEquals(CreatureCatalog.all.size, CreatureCatalog.all.map { it.creatureId }.toSet().size)
        assertEquals(114, LandCreatureCatalog.all.map { it.displayName.lowercase() }.toSet().size)
        LandCreatureCatalog.restorative.forEach {
            assertTrue(it.isAvailable)
            assertEquals(it.restorativeHabitat!!.zone, it.zone)
            assertNull(it.pearlPrice)
            assertNull(it.requirementMinutes)
            assertNull(it.arcFlowRequirement)
            assertEquals(CreatureRequirement.Drops(it.restorativeHabitat!!.dropCost), it.requirement)
        }
    }

    @Test fun arcRequirementsNeverBecomeMinutesOrPurchases() {
        val flagships = LandCreatureCatalog.main.filter { it.sourceType == CreatureSourceType.ARC_EARNED }
        assertEquals(listOf(3,6,9,12,15), flagships.map { it.arcFlowRequirement })
        flagships.forEach {
            assertNull(it.requirementMinutes)
            assertNull(it.flowTimeValueMinutes)
            assertNull(it.pearlPrice)
            assertEquals(CreatureRequirement.ArcDepth(it.arcFlowRequirement!!), it.requirement)
            assertThrows(IllegalArgumentException::class.java) {
                CreatureEconomy.quoteBeyondBluePayment(it.creatureId, 0, Int.MAX_VALUE)
            }
        }
    }
}
