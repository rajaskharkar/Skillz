package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SkillEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SkillzDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
    abstract fun sessionDao(): SessionDao
}