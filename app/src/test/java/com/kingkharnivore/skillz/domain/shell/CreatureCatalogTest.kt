package com.kingkharnivore.skillz.domain.shell

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
        assertEquals(40, CreatureCatalog.all.size)
    }

    @Test fun bannedCreaturesAreAbsent() {
        val banned = setOf("Anchovy", "Sardine", "Salmon", "Tuna", "Mackerel", "Crab", "Lobster", "Shrimp", "Goldfish", "Betta", "Narwhal", "False Killer Whale", "Basking Shark", "Greenland Shark", "Whale Shark", "Deep-Sea Anglerfish")
        assertTrue(CreatureCatalog.all.none { it.displayName in banned })
    }
}
