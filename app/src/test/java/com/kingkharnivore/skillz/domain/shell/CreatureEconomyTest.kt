package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import org.junit.Assert.*
import org.junit.Test

class CreatureEconomyTest {
    @Test fun creatureEarningUsesGreedyDurationConversion() {
        assertRewards(9)
        assertRewards(10, ShellContentCatalog.FOCUS_MINNOW to 1)
        assertRewards(29, ShellContentCatalog.FOCUS_MINNOW to 2)
        assertRewards(30, ShellContentCatalog.FOCUS_SEAHORSE to 1)
        assertRewards(60, ShellContentCatalog.FOCUS_MANTA to 1)
        assertRewards(80, ShellContentCatalog.FOCUS_MANTA to 1, ShellContentCatalog.FOCUS_MINNOW to 2)
        assertRewards(120, ShellContentCatalog.FOCUS_WHALE to 1)
        assertRewards(150, ShellContentCatalog.FOCUS_WHALE to 2, ShellContentCatalog.FOCUS_SEAHORSE to 1)
        assertTrue(CreatureEconomy.creaturesForRegularFlowMinutes(150, isSoftFlow = true).isEmpty())
    }

    @Test fun flowTimeValueAndBeyondBlueTradeRemainLevelIndependent() {
        assertEquals(10, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_MINNOW, level = 1))
        assertEquals(30, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_SEAHORSE, level = 1))
        assertEquals(60, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_MANTA, level = 1))
        assertEquals(120, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_WHALE, level = 1))
        assertEquals(120, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_WHALE, level = 10))
        assertEquals(120, CreatureEconomy.beyondBlueTradeContributionMinutes(ShellContentCatalog.FOCUS_WHALE, level = 10))
    }

    @Test fun maxLevelAndMasteryTitlesUseLevel99Cap() {
        assertEquals(99, CreatureEconomy.MAX_CREATURE_LEVEL)
        assertTrue(10 < CreatureEconomy.MAX_CREATURE_LEVEL)
        assertTrue(50 < CreatureEconomy.MAX_CREATURE_LEVEL)
        assertTrue(98 < CreatureEconomy.MAX_CREATURE_LEVEL)
        assertFalse(99 < CreatureEconomy.MAX_CREATURE_LEVEL)
        assertNull(CreatureEconomy.creatureMasteryTier(9))
        assertEquals(CreatureMasteryTier.SEASONED, CreatureEconomy.creatureMasteryTier(10))
        assertEquals(CreatureMasteryTier.PROVEN, CreatureEconomy.creatureMasteryTier(25))
        assertEquals(CreatureMasteryTier.VETERAN, CreatureEconomy.creatureMasteryTier(50))
        assertEquals(CreatureMasteryTier.ASCENDANT, CreatureEconomy.creatureMasteryTier(75))
        assertEquals(CreatureMasteryTier.MASTERED, CreatureEconomy.creatureMasteryTier(99))
    }

    @Test fun growthCostsUseRisingLevel99Curve() {
        val minnow = ShellContentCatalog.FOCUS_MINNOW
        assertEquals(25, CreatureEconomy.baseGrowthCost(minnow))
        assertEquals(75, CreatureEconomy.baseGrowthCost(ShellContentCatalog.FOCUS_SEAHORSE))
        assertEquals(200, CreatureEconomy.baseGrowthCost(ShellContentCatalog.FOCUS_MANTA))
        assertEquals(600, CreatureEconomy.baseGrowthCost(ShellContentCatalog.FOCUS_WHALE))
        assertEquals(360, CreatureEconomy.baseGrowthCost("creature_dolphin"))

        val level1 = CreatureEconomy.growthCostPearls(minnow, 1)
        val level2 = CreatureEconomy.growthCostPearls(minnow, 2)
        val level10 = CreatureEconomy.growthCostPearls(minnow, 10)
        val level25 = CreatureEconomy.growthCostPearls(minnow, 25)
        val level50 = CreatureEconomy.growthCostPearls(minnow, 50)
        val level75 = CreatureEconomy.growthCostPearls(minnow, 75)
        val level98 = CreatureEconomy.growthCostPearls(minnow, 98)
        assertTrue(level2 > level1)
        assertTrue(level10 > level2)
        assertTrue(level25 > level10)
        assertTrue(level50 > level25)
        assertTrue(level75 > level50)
        assertTrue(level98 > level75)
        assertTrue(CreatureEconomy.growthCostPearls(ShellContentCatalog.FOCUS_WHALE, 98) > 0)
        assertEquals(level98, CreatureEconomy.growthCostPearls(minnow, 99))
    }

    @Test fun cumulativeGrowthCostSumsEachLevelThroughTarget() {
        val seahorse = ShellContentCatalog.FOCUS_SEAHORSE
        assertEquals(0L, CreatureEconomy.cumulativeGrowthCostPearls(seahorse, 1))
        assertEquals(
            CreatureEconomy.growthCostPearls(seahorse, 1).toLong(),
            CreatureEconomy.cumulativeGrowthCostPearls(seahorse, 2)
        )
        assertEquals(
            (1 until 10).sumOf { CreatureEconomy.growthCostPearls(seahorse, it).toLong() },
            CreatureEconomy.cumulativeGrowthCostPearls(seahorse, 10)
        )
        assertEquals(
            (1 until 99).sumOf { CreatureEconomy.growthCostPearls(seahorse, it).toLong() },
            CreatureEconomy.cumulativeGrowthCostPearls(seahorse, 99)
        )
        assertTrue(CreatureEconomy.cumulativeGrowthCostPearls(ShellContentCatalog.FOCUS_WHALE, 99) > 0L)
    }

    @Test fun releaseValueUsesInvestmentBasedSalvage() {
        val minnow = ShellContentCatalog.FOCUS_MINNOW
        val base = CreatureEconomy.canonicalPearlValue(minnow)
        assertEquals(base, CreatureEconomy.releaseValuePearls(minnow, level = 1))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 10) > CreatureEconomy.releaseValuePearls(minnow, 1))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 25) > CreatureEconomy.releaseValuePearls(minnow, 10))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 50) > CreatureEconomy.releaseValuePearls(minnow, 25))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 75) > CreatureEconomy.releaseValuePearls(minnow, 50))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 99) > CreatureEconomy.releaseValuePearls(minnow, 75))
        assertEquals(0.35, CreatureEconomy.releaseSalvageRate(99), 0.0)
        val investment = CreatureEconomy.cumulativeGrowthCostPearls(minnow, 99)
        val expectedLevel99 = base + (investment * 0.35).toLong()
        assertEquals(expectedLevel99.toInt(), CreatureEconomy.releaseValuePearls(minnow, 99))
        assertTrue(CreatureEconomy.releaseValuePearls(minnow, 99) < base + investment)
        assertEquals(180, CreatureEconomy.releaseValuePearls("creature_starfish", level = 1))
    }

    @Test fun visualScaleAndCountsWork() {
        assertTrue(CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_MINNOW, 10) > CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_MINNOW, 1))
        assertTrue(CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_WHALE, 99) <= 1.55f)
        val counts = CreatureEconomy.counts(
            listOf(
                CreatureLedgerEntry(ShellContentCatalog.FOCUS_WHALE, CreatureStatus.ACTIVE, 2),
                CreatureLedgerEntry(ShellContentCatalog.FOCUS_WHALE, CreatureStatus.RELEASED, 7),
                CreatureLedgerEntry(ShellContentCatalog.FOCUS_WHALE, CreatureStatus.USED_BEYOND_BLUE, 3)
            ),
            ShellContentCatalog.FOCUS_WHALE
        )
        assertEquals(1, counts.activeCount)
        assertEquals(3, counts.lifetimeCount)
        assertEquals(1, counts.releasedCount)
        assertEquals(1, counts.usedBeyondBlueCount)
        assertEquals(2, counts.highestLevel)
    }

    @Test fun hybridPaymentQuotesPearlOnlyCreatureOnlyHybridUnderpayAndOverpay() {
        val dolphin = "creature_dolphin"
        assertEquals(720, CreatureCatalog.require(dolphin).pearlPrice)
        assertEquals(CreaturePaymentQuote(360, 0, 360, 720, 0, true), CreatureEconomy.quoteBeyondBluePayment(dolphin, 0, availablePearls = 720))
        assertEquals(CreaturePaymentQuote(360, 360, 0, 0, 0, true), CreatureEconomy.quoteBeyondBluePayment(dolphin, 360, availablePearls = 0))
        assertEquals(CreaturePaymentQuote(360, 180, 180, 360, 0, true), CreatureEconomy.quoteBeyondBluePayment(dolphin, 180, availablePearls = 360))
        assertFalse(CreatureEconomy.quoteBeyondBluePayment(dolphin, 180, availablePearls = 359).canEncounter)
        assertEquals(30, CreatureEconomy.quoteBeyondBluePayment("creature_ocean_sunfish", 510, availablePearls = 0).pearlReturnForOverpay)
    }

    private fun assertRewards(minutes: Int, vararg expected: Pair<String, Int>) {
        assertEquals(expected.toMap(), CreatureEconomy.creaturesForRegularFlowMinutes(minutes).associate { it.creatureId to it.quantity })
    }
}
