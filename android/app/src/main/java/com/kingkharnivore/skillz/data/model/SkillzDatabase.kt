package com.kingkharnivore.skillz.data.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kingkharnivore.skillz.data.model.dao.ActiveArcRunDao
import com.kingkharnivore.skillz.data.model.dao.ArcPlanDao
import com.kingkharnivore.skillz.data.model.dao.ArcMetadataDao
import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.entity.health.FlowRewardBreakdownEntity
import com.kingkharnivore.skillz.data.model.entity.health.FlowHealthSnapshotEntity
import com.kingkharnivore.skillz.data.model.dao.health.FlowHealthDao
import com.kingkharnivore.skillz.data.model.dao.shell.IdeaGroveDao
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellDaoProvider
import com.kingkharnivore.skillz.data.model.entity.ActiveArcRunEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcMetadataEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseFlowLinkEntity
import com.kingkharnivore.skillz.data.model.entity.PulseCreationEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.SessionCreationEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveProcessedSessionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.PearlLedgerEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellFindUpgradeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellPlacementEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity
import com.kingkharnivore.skillz.data.model.entity.shell.StillwaterPreferenceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindStackEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellRoomStateEntity
import com.kingkharnivore.skillz.data.model.entity.shell.AchievementBackfillEntity
import com.kingkharnivore.skillz.data.model.entity.shell.AchievementEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CollectionCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.BadgePinEntity
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeTrackingEntity
import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity

@Database(
    entities = [
        TagEntity::class,
        SessionEntity::class,
        SessionCreationEntity::class,
        PulseEntity::class,
        PulseFlowLinkEntity::class,
        PulseCreationEntity::class,
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
        UserShellRoomStateEntity::class,
        ShellRewardEventEntity::class,
        ObjectiveEntity::class,
        ObjectiveCompletionEntity::class,
        ObjectiveSkippedCycleEntity::class,
        ObjectiveProcessedSessionEntity::class,
        FlowHealthSnapshotEntity::class,
        FlowRewardBreakdownEntity::class,
        CreatureDiscoveryEntity::class,
        CreatureMasteryEventEntity::class,
        CollectionCompletionEntity::class,
        AchievementEventEntity::class,
        AchievementBackfillEntity::class,
        BadgePinEntity::class,
        BadgeTrackingEntity::class,
        MasteryCelebrationEventEntity::class,
        BadgeCountFloorEntity::class,
        ArcMetadataEntity::class,
        ChronicleEntity::class,
        ChronicleMomentEntity::class,
        ChronicleMediaItemEntity::class
    ],
    version = 39,
    exportSchema = true
)
abstract class SkillzDatabase : RoomDatabase(), ShellDaoProvider {
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao
    abstract fun pulseDao(): PulseDao
    abstract fun ideaGroveDao(): IdeaGroveDao
    abstract fun ongoingSessionDao(): OngoingSessionDao
    abstract fun flowPlanDao(): FlowPlanDao
    abstract fun arcPlanDao(): ArcPlanDao
    abstract fun activeArcRunDao(): ActiveArcRunDao
    abstract fun arcMetadataDao(): ArcMetadataDao
    abstract fun flowHealthDao(): FlowHealthDao
    abstract fun chronicleDao(): ChronicleDao
    override abstract fun shellRewardEventDao(): com.kingkharnivore.skillz.data.model.dao.shell.ShellRewardEventDao
    override abstract fun objectiveDao(): com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveDao
    override abstract fun objectiveCompletionDao(): com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveCompletionDao
    override abstract fun objectiveSkippedCycleDao(): com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveSkippedCycleDao
    override abstract fun objectiveProcessedSessionDao(): com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveProcessedSessionDao
}
