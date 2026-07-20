package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val completedAt: Long,
    val rosterVersion: Int,
    val rosterHash: String,
    val requiredSpeciesIds: String
)

/** Idempotency ledger for a level transition; resultPayload is forward compatible. */
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
