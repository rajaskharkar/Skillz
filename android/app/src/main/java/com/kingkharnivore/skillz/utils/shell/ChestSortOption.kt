package com.kingkharnivore.skillz.utils.shell

enum class ChestSortOption(val key: String) {
    Level("level"),
    Recent("recent"),
    NewestArrival("newest_arrival"),
    OldestArrival("oldest_arrival"),
    Alphabetical("alphabetical"),
    Value("value"),
    Count("count"),
    ClosestToMastery("closest_to_mastery");

    companion object {
        fun fromKey(key: String?): ChestSortOption = entries.firstOrNull { it.key == key } ?: Level
    }
}
