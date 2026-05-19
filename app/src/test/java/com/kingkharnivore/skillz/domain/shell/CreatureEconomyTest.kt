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

    @Test fun flowTimeValueAndPayoutAreLevelIndependent() {
        assertEquals(10, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_MINNOW, level = 1))
        assertEquals(30, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_SEAHORSE, level = 1))
        assertEquals(60, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_MANTA, level = 1))
        assertEquals(120, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_WHALE, level = 1))
        assertEquals(120, CreatureEconomy.flowTimeValueMinutes(ShellContentCatalog.FOCUS_WHALE, level = 10))
        assertEquals(120, CreatureEconomy.beyondBlueTradeContributionMinutes(ShellContentCatalog.FOCUS_WHALE, level = 10))
        assertEquals(120, CreatureEconomy.releaseValuePearls(ShellContentCatalog.FOCUS_WHALE, level = 10))
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

    @Test fun growthCostScaleAndCountsWork() {
        assertEquals(25, CreatureEconomy.growthCostPearls(ShellContentCatalog.FOCUS_MINNOW, 1))
        assertEquals(600, CreatureEconomy.growthCostPearls(ShellContentCatalog.FOCUS_WHALE, 1))
        assertEquals(360, CreatureEconomy.growthCostPearls("creature_dolphin", 1))
        assertTrue(CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_MINNOW, 10) > CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_MINNOW, 1))
        assertTrue(CreatureEconomy.animalVisualScale(ShellContentCatalog.FOCUS_WHALE, 1000) <= 1.30f)
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

    private fun assertRewards(minutes: Int, vararg expected: Pair<String, Int>) {
        assertEquals(expected.toMap(), CreatureEconomy.creaturesForRegularFlowMinutes(minutes).associate { it.creatureId to it.quantity })
    }
}
