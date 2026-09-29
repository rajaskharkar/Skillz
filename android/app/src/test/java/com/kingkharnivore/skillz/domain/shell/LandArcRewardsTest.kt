package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.LandArcRewards
import org.junit.Assert.*
import org.junit.Test

class LandArcRewardsTest {
    @Test fun exactPackagesIncludingEveryRequestedBoundary() {
        val cases = mapOf(
            0 to "", 1 to "", 2 to "", 3 to "chicken", 4 to "chicken", 5 to "chicken",
            6 to "deer", 7 to "deer", 8 to "deer", 9 to "camel", 10 to "camel", 11 to "camel",
            12 to "moose", 13 to "moose", 14 to "moose", 15 to "tiger", 16 to "tiger", 17 to "tiger",
            18 to "tiger,chicken", 21 to "tiger,deer", 24 to "tiger,camel", 27 to "tiger,moose",
            30 to "tiger,tiger", 33 to "tiger,tiger,chicken", 38 to "tiger,tiger,deer",
            45 to "tiger,tiger,tiger", 63 to "tiger,tiger,tiger,tiger,chicken"
        )
        cases.forEach { (count, expected) ->
            assertEquals("$count Flows", expected, LandArcRewards.forFlowCount(count)
                .flatMap { reward -> List(reward.quantity) { reward.creatureId.removePrefix("creature_") } }.joinToString(","))
        }
    }
    @Test fun stackingIsUnboundedAndDoesNotAllocatePerTiger() {
        val reward = LandArcRewards.forFlowCount(Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE / 15, reward.first().quantity)
        assertTrue(reward.size <= 2)
    }
    @Test fun negativeDepthIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { LandArcRewards.forFlowCount(-1) }
    }
}
