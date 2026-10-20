package com.kingkharnivore.skillz.domain.power

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class RedGrowthEconomyTest {
    @Test fun redCostsThirtyPercentLessThanEqualPriceBlueCreaturesAtEveryLevel() {
        val pairs = listOf("triassic_saturnalia" to "creature_angelfish", "jurassic_stegosaurus" to "creature_kraken")
        pairs.forEach { (red, blue) ->
            for (level in 1..98) {
                val redQuote = CreatureEconomy.growthCost(red, level)
                val blueQuote = CreatureEconomy.growthCost(blue, level)
                assertEquals(blueQuote.amount * 70 / 100, redQuote.amount)
                assertEquals(CreatureGrowthCurrency.PEBBLES, redQuote.currency)
                assertEquals(CreatureGrowthCurrency.PEARLS, blueQuote.currency)
            }
        }
    }

    @Test fun everyDinosaurUsesItsPriceAndCostsIncreaseThroughMastery() {
        RedCreatureCatalog.entries.forEach { entry ->
            assertEquals(entry.pebbleCost / 2, CreatureEconomy.baseGrowthCost(entry.id))
            val costs = (1..98).map { CreatureEconomy.growthCost(entry.id, it) }
            assertTrue(costs.all { it.currency == CreatureGrowthCurrency.PEBBLES })
            assertTrue(costs.zipWithNext().all { (a, b) -> a.amount < b.amount })
            assertThrows(IllegalArgumentException::class.java) { CreatureEconomy.growthCostPearls(entry.id, 1) }
        }
        assertEquals(71, CreatureEconomy.growthCost("triassic_saturnalia", 1).amount)
        assertEquals(1194, CreatureEconomy.growthCost("jurassic_stegosaurus", 1).amount)
        assertEquals(2788, CreatureEconomy.growthCost("cretaceous_tyrannosaurus", 1).amount)
    }

    @Test fun pebbleGrowthDoesNotInflateExistingPearlReleaseValuations() {
        val red = "cretaceous_tyrannosaurus"
        assertEquals(7000, CreatureEconomy.releaseValuePearls(red, 1))
        // Red's historical Pearl salvage curve used the same 25-unit base as Minnow.
        for (level in listOf(2, 10, 50, 99)) {
            assertEquals(CreatureEconomy.cumulativeGrowthCostPearls("focus_minnow", level),
                CreatureEconomy.cumulativeGrowthCostPearls(red, level))
        }
    }
}
