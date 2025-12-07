package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase

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