package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class CreatureBulkGrowthTest {
    @Test fun projectionUsesEveryLevelPriceAndTheCorrectCurrency() {
        for (id in listOf("focus_minnow", "creature_tiger", "triassic_saturnalia")) {
            val total = (1..5).sumOf { CreatureEconomy.growthCost(id, it).amount }
            val quote = CreatureEconomy.growthCostForLevels(id, 1, 5)
            assertEquals(total, quote.amount)
            assertEquals(CreatureEconomy.growthCost(id, 1).currency, quote.currency)
            assertEquals(5, CreatureEconomy.affordableGrowthLevels(id, 1, total))
            assertEquals(4, CreatureEconomy.affordableGrowthLevels(id, 1, total - 1))
            assertEquals(0, CreatureEconomy.affordableGrowthLevels(id, 1, 0))
        }
    }

    @Test fun allSpeciesStopAtMasteryAndSingleLevelPricesStayIdentical() {
        CreatureCatalog.all.forEach { creature ->
            val id = creature.creatureId
            assertEquals(CreatureEconomy.growthCost(id, 98), CreatureEconomy.growthCostForLevels(id, 98, 1))
            assertEquals(1, CreatureEconomy.affordableGrowthLevels(id, 98, Int.MAX_VALUE))
            assertEquals(0, CreatureEconomy.affordableGrowthLevels(id, 99, Int.MAX_VALUE))
            assertEquals(98, CreatureEconomy.affordableGrowthLevels(id, 1, Int.MAX_VALUE))
        }
    }

    @Test fun invalidRangesCannotProduceAFreeOrOverCapUpgrade() {
        listOf(1 to 0, 1 to -1, 98 to 2, 99 to 1, 0 to 1, 1 to Int.MAX_VALUE).forEach { (level, count) ->
            assertThrows(IllegalArgumentException::class.java) { CreatureEconomy.growthCostForLevels("focus_minnow", level, count) }
        }
    }
}
