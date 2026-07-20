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
    @Query("SELECT * FROM achievement_event WHERE eventId = :id") suspend fun getEvent(id: String): AchievementEventEntity?
    @Query("SELECT * FROM achievement_backfill WHERE version = :version") suspend fun getBackfill(version: Int): AchievementBackfillEntity?
}
