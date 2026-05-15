package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventTypes
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellRewardEventAggregatorTest {
    @Test
    fun groupsArcRewardsByTypeAndId() {
        val summary = ShellRewardEventAggregator.aggregate(
            listOf(
                event(1, ShellRewardEventTypes.ANIMAL_GRANTED, ShellContentCatalog.FOCUS_MINNOW),
                event(2, ShellRewardEventTypes.ANIMAL_GRANTED, ShellContentCatalog.FOCUS_MINNOW),
                event(3, ShellRewardEventTypes.ANIMAL_GRANTED, ShellContentCatalog.FOCUS_SEAHORSE),
                event(1, ShellRewardEventTypes.BADGE_UPDATED, "badge_flow_10_min"),
                event(2, ShellRewardEventTypes.BADGE_UPDATED, "badge_flow_10_min"),
                event(3, ShellRewardEventTypes.BADGE_UPDATED, "badge_flow_10_min"),
                event(1, ShellRewardEventTypes.TRINKET_GRANTED, ShellContentCatalog.TRINKET_SEA_GLASS_SHARD, 2),
                event(2, ShellRewardEventTypes.TRINKET_GRANTED, ShellContentCatalog.TRINKET_GLIMMER),
                event(1, ShellRewardEventTypes.DISCOVERY_RECORDED, "discovery_octopus"),
                event(1, ShellRewardEventTypes.PEARLS_CARRIED, null, 482),
                event(2, ShellRewardEventTypes.STILLWATER_ADDED, null, 42)
            )
        )

        assertEquals(2, summary.animals.first { it.id == ShellContentCatalog.FOCUS_MINNOW }.count)
        assertEquals(1, summary.animals.first { it.id == ShellContentCatalog.FOCUS_SEAHORSE }.count)
        assertEquals(3, summary.badges.first { it.id == "badge_flow_10_min" }.count)
        assertEquals(2, summary.trinkets.first { it.id == ShellContentCatalog.TRINKET_SEA_GLASS_SHARD }.count)
        assertEquals(1, summary.trinkets.first { it.id == ShellContentCatalog.TRINKET_GLIMMER }.count)
        assertEquals("discovery_octopus", summary.discoveries.single().id)
        assertEquals(482, summary.pearlsCarried)
        assertEquals(42L, summary.stillwaterAdded)
    }

    private fun event(
        sessionId: Long,
        type: String,
        rewardId: String?,
        quantity: Long = 1L,
        arcId: Long? = 99L
    ) = ShellRewardEventEntity(
        id = "$sessionId-$type-${rewardId ?: "none"}",
        sourceSessionId = sessionId,
        arcId = arcId,
        rewardType = type,
        rewardId = rewardId,
        quantity = quantity,
        occurredAt = sessionId
    )
}
