package com.kingkharnivore.skillz.data.model.dao.anchor

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.anchor.AnchoredAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnchoredAppDao {
    @Query("SELECT * FROM anchored_apps ORDER BY addedAt DESC")
    fun observeAnchoredApps(): Flow<List<AnchoredAppEntity>>

    @Query("SELECT * FROM anchored_apps ORDER BY addedAt DESC")
    suspend fun getAnchoredApps(): List<AnchoredAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AnchoredAppEntity)

    @Query("DELETE FROM anchored_apps WHERE packageName = :packageName")
    suspend fun remove(packageName: String)
}
