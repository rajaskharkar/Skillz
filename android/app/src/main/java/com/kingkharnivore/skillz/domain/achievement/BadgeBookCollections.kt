package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity

/** Stable V1 rosters. Difficulty is an editorial estimate, never a lock or eligibility rule. */
data class BadgeBookCollection(
    val id: String,
    val titleRes: Int,
    val difficulty: Int,
    val memberIds: List<String>,
    val completionBadgeId: String,
    val createsAward: Boolean = false
) {
    val definition get() = AchievementBadgeDefinition(completionBadgeId, BadgeFamily.COLLECTION,
        BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT, milestones = listOf(memberIds.size))
}

object BadgeBookCollections {
    private fun regions(vararg ids: String, suffix: String) = ids.map { "${it}_$suffix" }
    val collections = listOf(
        BadgeBookCollection("badge_book_first_steps", R.string.book_collection_first_steps, 10,
            listOf("badge_flow_10_min", "power_spark", "land_first", "red_first_footprint"), "book_first_steps_complete", true),
        BadgeBookCollection("badge_book_flow", R.string.book_collection_flow, 20,
            listOf("badge_flow_10_min", "badge_flow_30_min", "badge_flow_60_min", "badge_flow_120_min"), "badge_flow_120_min"),
        BadgeBookCollection("badge_book_growing", R.string.book_collection_growing, 30,
            listOf("mastery_first", "land_mastery_first", "stillwater_mastery"), "book_growing_complete", true),
        BadgeBookCollection("badge_book_sea", R.string.book_collection_sea, 40,
            regions("blue_sunlit_reef", "blue_deeper_reef", "blue_open_blue", "blue_great_blue", suffix = "collector"), "collection_the_blue_collector"),
        BadgeBookCollection("badge_book_land", R.string.book_collection_land, 45,
            regions("blue_golden_fields", "blue_ancient_woods", "blue_open_sands", "blue_high_peaks", "blue_great_wild", suffix = "collector"), "collection_land_collector"),
        BadgeBookCollection("badge_book_red", R.string.book_collection_red, 50,
            listOf("red_first_footprint", "red_dawn", "red_giants", "red_last_age"), "red_deep_time"),
        BadgeBookCollection("badge_book_stillwater", R.string.book_collection_stillwater, 55,
            regions("stillwater_fishbowl", "stillwater_aquarium", "stillwater_pond", "stillwater_lake", "stillwater_pasture", "stillwater_glade", "stillwater_oasis", "stillwater_ravine", "stillwater_sanctuary", suffix = "collector"), "collection_stillwater_collector"),
        BadgeBookCollection("badge_book_power", R.string.book_collection_power, 60,
            listOf("power_spark", "power_resolve", "power_pressure", "power_bedrock", "power_unyielding", "power_strata", "power_adaptation"), "book_power_complete", true),
        BadgeBookCollection("badge_book_specialists", R.string.book_collection_specialists, 70,
            listOf("red_apex", "red_colossus", "red_armored", "red_horned", "red_raptor", "red_crested", "red_feathered", "red_titans", "red_clawed", "red_kings"), "book_specialists_complete", true),
        BadgeBookCollection("badge_book_sea_mastery", R.string.book_collection_sea_mastery, 80,
            regions("blue_sunlit_reef", "blue_deeper_reef", "blue_open_blue", "blue_great_blue", "stillwater_fishbowl", "stillwater_aquarium", "stillwater_pond", "stillwater_lake", suffix = "completionist"), "collection_all_waters_completionist"),
        BadgeBookCollection("badge_book_land_mastery", R.string.book_collection_land_mastery, 85,
            regions("blue_golden_fields", "blue_ancient_woods", "blue_open_sands", "blue_high_peaks", "blue_great_wild", "stillwater_pasture", "stillwater_glade", "stillwater_oasis", "stillwater_ravine", "stillwater_sanctuary", suffix = "completionist"), "collection_all_land_completionist"),
        BadgeBookCollection("badge_book_red_mastery", R.string.book_collection_red_mastery, 90,
            listOf("red_ascendant", "red_dominion", "red_extinction"), "red_the_red")
    ).sortedBy { it.difficulty }
    val byId = collections.associateBy { it.id }
    val newAwards = collections.filter { it.createsAward }
    val byAward = newAwards.associateBy { it.completionBadgeId }

    fun earnedMembers(collection: BadgeBookCollection, badges: Map<String, BadgeProgressModel>): Int =
        collection.memberIds.count { badges[it]?.everEarned == true }

    /** A second pass over ordinary badge evidence avoids recursion and preserves hidden missing members. */
    fun awards(base: List<BadgeProgressModel>, stored: List<UserBadgeEntity>, pins: Map<String, Int>, tracked: Set<String>): List<BadgeProgressModel> {
        val badges = base.associateBy { it.badgeId }
        val ledger = stored.associateBy { it.badgeId }
        return newAwards.map { collection ->
            val old = ledger[collection.completionBadgeId]
            val progress = earnedMembers(collection, badges)
            val earned = progress == collection.memberIds.size || (old?.count ?: 0) > 0
            BadgeProgressModel(collection.completionBadgeId, if (earned) 1 else 0, earned, BadgeUiCategory.COLLECTIONS,
                progress, collection.memberIds.size, (collection.memberIds.size - progress).coerceAtLeast(0),
                MilestoneEngine.evaluate(progress, thresholds = listOf(collection.memberIds.size)),
                firstEarnedAt = old?.firstEarnedAt, lastAdvancedAt = old?.lastEarnedAt,
                timestampConfidence = old?.timestampConfidence ?: AchievementTimestampConfidence.UNKNOWN,
                pinnedOrder = pins[collection.completionBadgeId], tracked = collection.completionBadgeId in tracked,
                action = BadgeActionDestination.CollectionDetails(collection.id),
                newlyEarned = old != null && old.viewedAt == null,
                canTrack = !earned, canNavigate = true, canProgressNow = !earned,
                disabledReason = if (earned) BadgeDisabledReason.COMPLETE else null,
                goalType = BadgeGoalType.ONE_TIME, countType = BadgeCountType.ONE_TIME, terminal = earned, nextMilestoneTarget = null)
        }
    }

    fun completionEvidence(collection: BadgeBookCollection, badges: Map<String, BadgeProgressModel>): EvidenceTimestamp? =
        AchievementTimestampCalculator.completionTimestamp(collection.memberIds.map { id ->
            badges[id]?.takeIf { it.everEarned }?.let { badge ->
                badge.firstEarnedAt?.let { EvidenceTimestamp(it, badge.timestampConfidence) }
            }
        })
}
