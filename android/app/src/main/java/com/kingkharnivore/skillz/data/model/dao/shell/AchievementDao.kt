package com.kingkharnivore.skillz.data.model.dao.shell

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.shell.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun recordDiscovery(value: CreatureDiscoveryEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun recordMastery(value: CreatureMasteryEventEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun recordCompletion(value: CollectionCompletionEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun recordEvent(value: AchievementEventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun recordBackfill(value: AchievementBackfillEntity)

    @Query("SELECT * FROM creature_discovery") fun observeDiscoveries(): Flow<List<CreatureDiscoveryEntity>>
    @Query("SELECT * FROM creature_discovery") suspend fun getDiscoveries(): List<CreatureDiscoveryEntity>
    @Query("SELECT * FROM creature_mastery_event") fun observeMasteries(): Flow<List<CreatureMasteryEventEntity>>
    @Query("SELECT * FROM creature_mastery_event") suspend fun getMasteries(): List<CreatureMasteryEventEntity>
    @Query("SELECT * FROM collection_completion") suspend fun getCompletions(): List<CollectionCompletionEntity>
    @Query("SELECT * FROM collection_completion") fun observeCompletions(): Flow<List<CollectionCompletionEntity>>
    @Query("SELECT * FROM achievement_event WHERE eventId = :id") suspend fun getEvent(id: String): AchievementEventEntity?
    @Query("SELECT * FROM achievement_backfill WHERE version = :version") suspend fun getBackfill(version: Int): AchievementBackfillEntity?
    @Query("SELECT * FROM achievement_backfill ORDER BY version DESC LIMIT 1") fun observeLatestBackfill(): Flow<AchievementBackfillEntity?>
    @Query("SELECT * FROM badge_pin ORDER BY pinOrder") fun observePins(): Flow<List<BadgePinEntity>>
    @Query("SELECT * FROM badge_pin ORDER BY pinOrder") suspend fun getPins(): List<BadgePinEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPin(value: BadgePinEntity)
    @Query("DELETE FROM badge_pin WHERE badgeId = :badgeId") suspend fun deletePin(badgeId: String): Int
    @Query("UPDATE badge_pin SET pinOrder = :pinOrder WHERE badgeId = :badgeId") suspend fun updatePinOrder(badgeId: String, pinOrder: Int)
    @Query("SELECT * FROM badge_tracking ORDER BY trackedAt") fun observeTracking(): Flow<List<BadgeTrackingEntity>>
    @Query("SELECT * FROM badge_tracking ORDER BY trackedAt") suspend fun getTracking(): List<BadgeTrackingEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTracking(value: BadgeTrackingEntity): Long
    @Query("DELETE FROM badge_tracking WHERE badgeId = :badgeId") suspend fun deleteTracking(badgeId: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCelebration(value: MasteryCelebrationEventEntity): Long
    @Query("SELECT * FROM mastery_celebration_event WHERE lifecycleState != 'COMPLETED' ORDER BY createdAt ASC LIMIT 1")
    fun observePendingCelebration(): Flow<MasteryCelebrationEventEntity?>
    @Query("SELECT * FROM mastery_celebration_event WHERE eventId = :eventId") suspend fun getCelebration(eventId: String): MasteryCelebrationEventEntity?
    @Query("UPDATE mastery_celebration_event SET lifecycleState = :lifecycle, presentationStage = :stage, completedAt = :completedAt WHERE eventId = :eventId AND lifecycleState != 'COMPLETED'")
    suspend fun updateCelebrationState(eventId: String, lifecycle: String, stage: String, completedAt: Long?): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCountFloor(value: BadgeCountFloorEntity)
    @Query("SELECT * FROM badge_count_floor") fun observeCountFloors(): Flow<List<BadgeCountFloorEntity>>
    @Query("SELECT * FROM badge_count_floor") suspend fun getCountFloors(): List<BadgeCountFloorEntity>
}
