package com.kingkharnivore.skillz.utils.score

data class ScoreBreakdown(
    val minutes: Int,
    val basePoints: Int,
    val tenMinuteBonuses: Int,
    val thirtyMinuteBonuses: Int,
    val sixtyMinuteBonuses: Int,
    val totalPoints: Int
)