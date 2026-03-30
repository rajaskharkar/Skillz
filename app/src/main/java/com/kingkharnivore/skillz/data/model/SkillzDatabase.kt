package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kingkharnivore.skillz.data.model.dao.BeamDao
import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity

@Database(
    entities = [
        TagEntity::class,
        SessionEntity::class,
        PulseEntity::class,
        OngoingSessionEntity::class,
        BeamEntity::class,
        FlowPlanEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SkillzDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao
    abstract fun pulseDao(): PulseDao
    abstract fun ongoingSessionDao(): OngoingSessionDao
    abstract fun beamDao(): BeamDao
    abstract fun flowPlanDao(): FlowPlanDao
}