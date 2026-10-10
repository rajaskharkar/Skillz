package com.kingkharnivore.skillz.data.model.dao.green

import androidx.room.*
import com.kingkharnivore.skillz.data.model.entity.green.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GreenDao {
    @Query("SELECT * FROM green_specimen ORDER BY createdOrder")
    fun observeSpecimens(): Flow<List<PlantSpecimenEntity>>
    @Query("SELECT * FROM green_specimen ORDER BY createdOrder")
    suspend fun specimens(): List<PlantSpecimenEntity>
    @Query("SELECT * FROM green_specimen WHERE id = :id")
    suspend fun specimen(id: String): PlantSpecimenEntity?
    @Query("SELECT COALESCE(MAX(createdOrder), 0) + 1 FROM green_specimen")
    suspend fun nextOrder(): Long
    @Insert suspend fun insert(specimen: PlantSpecimenEntity)
    @Update suspend fun update(specimen: PlantSpecimenEntity)
    @Query("SELECT * FROM green_badge_award ORDER BY awardedAt, id")
    fun observeAwards(): Flow<List<GreenBadgeAwardEntity>>
    @Query("SELECT * FROM green_badge_award")
    suspend fun awards(): List<GreenBadgeAwardEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun award(badge: GreenBadgeAwardEntity): Long
    @Query("SELECT * FROM green_action WHERE id = :id")
    suspend fun action(id: String): GreenActionEntity?
    @Insert suspend fun record(action: GreenActionEntity)
    @Insert suspend fun event(event: GreenEventEntity)
}
