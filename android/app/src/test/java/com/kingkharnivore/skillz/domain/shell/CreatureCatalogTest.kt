package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import org.junit.Assert.*
import org.junit.Test

class CreatureCatalogTest {
    @Test fun catalogHasRequiredZonesAndMetadata() {
        assertEquals(CreatureZone.SUNLIT_REEF, CreatureCatalog.require("creature_sea_turtle").zone)
        assertEquals(CreatureZone.GREAT_BLUE, CreatureCatalog.require("creature_leatherback_turtle").zone)
        assertEquals(CreatureZone.OPEN_BLUE, CreatureCatalog.require("creature_great_white_shark").zone)
        assertEquals(CreatureZone.GREAT_BLUE, CreatureCatalog.require("creature_leviathan").zone)
        assertEquals("Anglerfish", CreatureCatalog.require("creature_anglerfish").displayName)
        CreatureCatalog.all.forEach { creature ->
            assertTrue(creature.creatureId.isNotBlank())
            assertTrue(creature.displayName.isNotBlank())
            assertTrue(creature.staticIconKey.isNotBlank())
            assertTrue(creature.animatedRendererKey.isNotBlank())
            assertTrue(creature.renderFamily.key.isNotBlank())
            assertTrue(creature.flowTimeValueMinutes != null || creature.requirementMinutes != null)
        }
        assertEquals(1, CreatureCatalog.all.count { it.displayName == "Sea Turtle" })
        assertEquals(72, CreatureCatalog.all.size)
    }

    @Test fun bannedCreaturesAreAbsent() {
        val banned = setOf("Anchovy", "Sardine", "Salmon", "Tuna", "Mackerel", "Crab", "Lobster", "Shrimp", "Goldfish", "Betta", "Narwhal", "False Killer Whale", "Basking Shark", "Greenland Shark", "Whale Shark", "Deep-Sea Anglerfish")
        assertTrue(CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER }.none { it.displayName in banned })
    }

    @Test
    fun octopusIsBeyondBlueCreatureInDeeperReef() {
        val octopus = CreatureCatalog.require(com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog.FOCUS_OCTOPUS)

        assertEquals(CreatureSourceType.BEYOND_BLUE, octopus.sourceType)
        assertEquals(CreatureZone.DEEPER_REEF, octopus.zone)
        assertEquals(480, octopus.requirementMinutes)
    }
}
