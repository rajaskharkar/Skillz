package com.kingkharnivore.skillz.data.di

import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveCompletionDao
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveDao
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveSkippedCycleDao
import com.kingkharnivore.skillz.data.model.dao.shell.PearlLedgerDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindInstanceDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellRewardEventDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindStackDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindUpgradeDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellPlacementDao
import com.kingkharnivore.skillz.data.model.dao.shell.StillwaterLedgerDao
import com.kingkharnivore.skillz.data.model.dao.shell.StillwaterPreferenceDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserBadgeDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserDiscoveryDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserShellRoomStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ShellDatabaseModule {
    @Provides
    fun providePearlLedgerDao(db: SkillzDatabase): PearlLedgerDao = db.pearlLedgerDao()

    @Provides
    fun provideShellFindInstanceDao(db: SkillzDatabase): ShellFindInstanceDao =
        db.shellFindInstanceDao()

    @Provides
    fun provideShellFindStackDao(db: SkillzDatabase): ShellFindStackDao = db.shellFindStackDao()

    @Provides
    fun provideShellPlacementDao(db: SkillzDatabase): ShellPlacementDao = db.shellPlacementDao()

    @Provides
    fun provideShellFindUpgradeDao(db: SkillzDatabase): ShellFindUpgradeDao =
        db.shellFindUpgradeDao()

    @Provides
    fun provideUserBadgeDao(db: SkillzDatabase): UserBadgeDao = db.userBadgeDao()

    @Provides
    fun provideUserDiscoveryDao(db: SkillzDatabase): UserDiscoveryDao = db.userDiscoveryDao()

    @Provides
    fun provideStillwaterLedgerDao(db: SkillzDatabase): StillwaterLedgerDao =
        db.stillwaterLedgerDao()

    @Provides
    fun provideStillwaterPreferenceDao(db: SkillzDatabase): StillwaterPreferenceDao =
        db.stillwaterPreferenceDao()

    @Provides
    fun provideUserShellRoomStateDao(db: SkillzDatabase): UserShellRoomStateDao =
        db.userShellRoomStateDao()

    @Provides
    fun provideShellRewardEventDao(db: SkillzDatabase): ShellRewardEventDao =
        db.shellRewardEventDao()

    @Provides
    fun provideObjectiveDao(db: SkillzDatabase): ObjectiveDao = db.objectiveDao()

    @Provides
    fun provideObjectiveCompletionDao(db: SkillzDatabase): ObjectiveCompletionDao =
        db.objectiveCompletionDao()

    @Provides
    fun provideObjectiveSkippedCycleDao(db: SkillzDatabase): ObjectiveSkippedCycleDao =
        db.objectiveSkippedCycleDao()
}
