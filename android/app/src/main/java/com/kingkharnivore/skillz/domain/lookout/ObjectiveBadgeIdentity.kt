package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectivePeriodTypes

data class ObjectiveBadgeIdentity(val journeyId: Long, val periodType: String) {
    val badgeId: String get() = badgeId(journeyId, periodType)

    companion object {
        private val pattern = Regex("^objective_badge_(\\d+)_(daily|weekly|monthly)$")
        private val periods = setOf(
            ObjectivePeriodTypes.DAILY,
            ObjectivePeriodTypes.WEEKLY,
            ObjectivePeriodTypes.MONTHLY
        )

        fun fromBadgeId(badgeId: String): ObjectiveBadgeIdentity? {
            val match = pattern.matchEntire(badgeId) ?: return null
            val journeyId = match.groupValues[1].toLongOrNull() ?: return null
            return ObjectiveBadgeIdentity(journeyId, match.groupValues[2])
        }

        fun badgeId(journeyId: Long, periodType: String): String {
            require(journeyId >= 0 && periodType in periods)
            return "objective_badge_${journeyId}_$periodType"
        }
    }
}
