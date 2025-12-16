package com.kingkharnivore.skillz.utils.score

enum class ScoreFilter(
    val label: String,
    val durationMs: Long? // null = all time
) {
    LAST_24_HOURS("Last 24 hours", 24L * 60 * 60 * 1000),
    LAST_7_DAYS("Last 7 days", 7L * 24 * 60 * 60 * 1000),
    LAST_30_DAYS("Last 30 days", 30L * 24 * 60 * 60 * 1000),
    ALL_TIME("All time", null)
}