package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity

@Database(
    entities = [TagEntity::class, SessionEntity::class, OngoingSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SkillzDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao
    abstract fun ongoingSessionDao(): OngoingSessionDao   // 👈 NEW
}