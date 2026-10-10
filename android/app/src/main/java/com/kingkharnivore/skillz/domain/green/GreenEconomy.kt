package com.kingkharnivore.skillz.domain.green

import kotlin.math.floor
import kotlin.math.pow

/** Single source of pricing. Positive half ties round up to the nearest 50 Drops. */
object GreenEconomy {
    data class Tier(val seed: Long, val waterBase: Long)
    val tiers = mapOf(
        1 to Tier(1_800, 300), 2 to Tier(2_700, 450), 3 to Tier(3_600, 600),
        4 to Tier(5_400, 900), 5 to Tier(7_200, 1_200), 6 to Tier(10_800, 1_800),
        7 to Tier(14_400, 2_400), 8 to Tier(21_600, 3_600),
        9 to Tier(32_400, 5_400), 10 to Tier(43_200, 7_200)
    )
    private fun tier(tier: Int) = requireNotNull(tiers[tier]) { "Invalid Green tier" }
    fun seedCost(tier: Int): Long = tier(tier).seed
    fun waterCost(tier: Int, currentLevel: Int): Long {
        require(currentLevel in 1..98) { "Only levels 1 through 98 can be watered" }
        val raw = tier(tier).waterBase * (1 + 0.035 * (currentLevel - 1).toDouble().pow(1.35))
        return floor(raw / 50.0 + 0.5).toLong() * 50L
    }
    fun totalCost(tier: Int, targetLevel: Int): Long {
        require(targetLevel in 1..99)
        return seedCost(tier) + (1 until targetLevel).sumOf { waterCost(tier, it) }
    }
}
