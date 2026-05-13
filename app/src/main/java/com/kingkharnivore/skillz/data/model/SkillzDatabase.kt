package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kingkharnivore.skillz.data.model.dao.ActiveArcRunDao
import com.kingkharnivore.skillz.data.model.dao.ArcPlanDao
import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.dao.shell.*
import com.kingkharnivore.skillz.data.model.entity.ActiveArcRunEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
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
        FlowPlanEntity::class,
        ArcPlanEntity::class,
        ArcPlanStepEntity::class,
        ActiveArcRunEntity::class,
        PearlLedgerEntity::class,
        UserShellFindInstanceEntity::class,
        UserShellFindStackEntity::class,
        ShellPlacementEntity::class,
        ShellFindUpgradeEntity::class,
        UserBadgeEntity::class,
        UserDiscoveryEntity::class,
        StillwaterLedgerEntity::class,
        StillwaterPreferenceEntity::class,
        UserShellRoomStateEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class SkillzDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao
    abstract fun pulseDao(): PulseDao
    abstract fun ongoingSessionDao(): OngoingSessionDao
    abstract fun flowPlanDao(): FlowPlanDao
    abstract fun arcPlanDao(): ArcPlanDao
    abstract fun activeArcRunDao(): ActiveArcRunDao
    abstract fun pearlLedgerDao(): PearlLedgerDao
    abstract fun shellFindInstanceDao(): ShellFindInstanceDao
    abstract fun shellFindStackDao(): ShellFindStackDao
    abstract fun shellPlacementDao(): ShellPlacementDao
    abstract fun shellFindUpgradeDao(): ShellFindUpgradeDao
    abstract fun userBadgeDao(): UserBadgeDao
    abstract fun userDiscoveryDao(): UserDiscoveryDao
    abstract fun stillwaterLedgerDao(): StillwaterLedgerDao
    abstract fun stillwaterPreferenceDao(): StillwaterPreferenceDao
    abstract fun userShellRoomStateDao(): UserShellRoomStateDao
}