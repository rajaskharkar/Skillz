package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.R

enum class PlantGrowthStage(val firstLevel: Int, val assetIndex: Int, val nameRes: Int) {
    SPROUT(1, 0, R.string.green_stage_sprout),
    SEEDLING(15, 1, R.string.green_stage_seedling),
    YOUNG(30, 2, R.string.green_stage_young),
    ROOTED(45, 3, R.string.green_stage_rooted),
    THRIVING(60, 4, R.string.green_stage_thriving),
    MATURE(75, 5, R.string.green_stage_mature),
    FULLY_GROWN(90, 6, R.string.green_stage_fully_grown),
    MASTERED(99, 6, R.string.green_stage_mastered)
}
object PlantGrowthStageResolver {
    fun resolve(level: Int): PlantGrowthStage {
        require(level in 1..99)
        return PlantGrowthStage.entries.last { level >= it.firstLevel }
    }
    fun nextVisibleGrowth(level: Int): Int? {
        resolve(level)
        return PlantGrowthStage.entries.firstOrNull { it.firstLevel > level && it != PlantGrowthStage.MASTERED }?.firstLevel
    }
}

