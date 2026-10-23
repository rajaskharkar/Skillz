package com.kingkharnivore.skillz.domain.achievement

/** Stable identities: changing display copy never creates another award. */
data class LifetimePointsBadge(val target: Int, val label: String) {
    val id = "lifetime_points_$target"
    val definition = AchievementBadgeDefinition(
        badgeId = id,
        family = BadgeFamily.FLOW,
        countType = BadgeCountType.ONE_TIME,
        requirement = BadgeRequirement.EXACT_COUNT,
        milestones = listOf(target)
    )

    // Aggregate in Long; only the bounded UI progress uses Int.
    fun progress(total: Long): Int = total.coerceIn(0L, target.toLong()).toInt()
}

object LifetimePointsBadges {
    val specs = listOf(
        LifetimePointsBadge(1_000, "1K"),
        LifetimePointsBadge(10_000, "10K"),
        LifetimePointsBadge(50_000, "50K"),
        LifetimePointsBadge(100_000, "100K"),
        LifetimePointsBadge(200_000, "200K"),
        LifetimePointsBadge(500_000, "500K"),
        LifetimePointsBadge(1_000_000, "1M"),
        LifetimePointsBadge(2_000_000, "2M"),
        LifetimePointsBadge(5_000_000, "5M"),
        LifetimePointsBadge(10_000_000, "10M"),
        LifetimePointsBadge(25_000_000, "25M"),
        LifetimePointsBadge(50_000_000, "50M"),
        LifetimePointsBadge(100_000_000, "100M"),
        LifetimePointsBadge(200_000_000, "200M"),
        LifetimePointsBadge(500_000_000, "500M"),
        LifetimePointsBadge(1_000_000_000, "1B")
    )
    val byId = specs.associateBy { it.id }
}
