package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.domain.achievement.AchievementTimestampConfidence

/** Permanent, sync-friendly evidence that a species has been owned. */
@Entity(tableName = "creature_discovery", indices = [Index("firstCreatureId")])
data class CreatureDiscoveryEntity(
    @PrimaryKey val speciesId: String,
    val firstDiscoveredAt: Long,
    val acquisitionSource: String?,
    val firstCreatureId: String?,
    val updatedAt: Long
)

/** Immutable evidence of an individual creature reaching level 99. */
@Entity(
    tableName = "creature_mastery_event",
    indices = [Index(value = ["creatureInstanceId"], unique = true), Index("speciesId")]
)
data class CreatureMasteryEventEntity(
    @PrimaryKey val eventId: String,
    val creatureInstanceId: String,
    val speciesId: String,
    val achievedAt: Long,
    val levelUpTransactionId: String
)

/** A completed roster is historical evidence and is never removed on expansion. */
@Entity(
    tableName = "collection_completion",
    indices = [Index(value = ["collectionId", "completionType", "rosterHash"], unique = true)]
)
data class CollectionCompletionEntity(
    @PrimaryKey val completionId: String,
    val collectionId: String,
    val completionType: String,
    val completedAt: Long?,
    val timestampConfidence: AchievementTimestampConfidence,
    val rosterVersion: Int,
    val rosterHash: String,
    val requiredSpeciesIds: String
)

/** Idempotency ledger. New payloads are versioned JSON; legacy pipe-delimited rows remain readable. */
@Entity(tableName = "achievement_event", indices = [Index("creatureInstanceId")])
data class AchievementEventEntity(
    @PrimaryKey val eventId: String,
    val eventType: String,
    val creatureInstanceId: String?,
    val speciesId: String?,
    val createdAt: Long,
    val resultPayload: String
)

@Entity(tableName = "achievement_backfill")
data class AchievementBackfillEntity(
    @PrimaryKey val version: Int,
    val completedAt: Long,
    val discoveredCount: Int,
    val masteryCount: Int,
    val completionCount: Int
)

@Entity(tableName = "badge_pin", indices = [Index(value = ["pinOrder"], unique = true)])
data class BadgePinEntity(
    @PrimaryKey val badgeId: String,
    val pinOrder: Int,
    val pinnedAt: Long
)

@Entity(tableName = "badge_tracking")
data class BadgeTrackingEntity(
    @PrimaryKey val badgeId: String,
    val trackedAt: Long
)

/** Immutable rendering snapshot plus durable presentation lifecycle for Level 99. */
@Entity(
    tableName = "mastery_celebration_event",
    indices = [Index(value = ["transactionId"], unique = true), Index("lifecycleState")]
)
data class MasteryCelebrationEventEntity(
    @PrimaryKey val eventId: String,
    val transactionId: String,
    val creatureInstanceId: String,
    val speciesId: String,
    val artworkKey: String,
    val regionId: String,
    val sourceId: String,
    val previousLevel: Int,
    val newLevel: Int,
    val speciesMasteryCount: Int,
    val totalMasteries: Int,
    val uniqueMasteredSpecies: Int,
    val regionalDiscovered: Int,
    val regionalTotal: Int,
    val regionalMastered: Int,
    val regionalCollectorEarned: Boolean,
    val regionalCompletionistEarned: Boolean,
    val blueMastered: Int,
    val blueTotal: Int,
    val stillwaterMastered: Int,
    val stillwaterTotal: Int,
    val allWatersMastered: Int,
    val allWatersTotal: Int,
    val newlyEarnedBadgeIds: String,
    val advancedBadgeIds: String,
    val milestonesReached: String,
    val originDestination: String,
    val createdAt: Long,
    val lifecycleState: String,
    val presentationStage: String,
    val completedAt: Long? = null
)

/** Aggregate lower bound retained when reliable history cannot be represented by per-creature events. */
@Entity(tableName = "badge_count_floor")
data class BadgeCountFloorEntity(
    @PrimaryKey val badgeId: String,
    val speciesId: String?,
    val minimumCount: Int,
    val verifiedCountAtReconciliation: Int,
    val source: String,
    val reconciledAt: Long
)
