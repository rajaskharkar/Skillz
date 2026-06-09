package com.kingkharnivore.skillz.viewmodel.shell

import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StillwaterUnlocksTest {
    @Test
    fun derivesUnlockedZonesFromHistoricalNonStillwaterFinds() {
        val zones = deriveUnlockedBlueZonesFromHistoricalFinds(
            listOf(
                creature("released-manta", ShellContentCatalog.FOCUS_MANTA, CreatureStatus.RELEASED),
                creature("stillwater-lake", "stillwater_coelacanth", CreatureStatus.ACTIVE)
            )
        )

        assertTrue(CreatureZone.SUNLIT_REEF in zones)
        assertTrue(CreatureZone.OPEN_BLUE in zones)
        assertFalse(CreatureZone.GREAT_BLUE in zones)
    }

    @Test
    fun deepestHistoricalZoneUnlocksShallowerZones() {
        val zones = deriveUnlockedBlueZonesFromHistoricalFinds(
            listOf(creature("released-whale", ShellContentCatalog.FOCUS_WHALE, CreatureStatus.RELEASED))
        )

        assertTrue(CreatureZone.SUNLIT_REEF in zones)
        assertTrue(CreatureZone.DEEPER_REEF in zones)
        assertTrue(CreatureZone.OPEN_BLUE in zones)
        assertTrue(CreatureZone.GREAT_BLUE in zones)
    }

    private fun creature(
        instanceId: String,
        findId: String,
        status: String
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = 1L,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = null,
        customName = null,
        isNew = false,
        isArchivedInChest = true,
        animalLevel = 1,
        creatureStatus = status
    )
}
