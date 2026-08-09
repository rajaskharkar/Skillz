package com.kingkharnivore.skillz.data.model.dao.shell

interface ShellDaoProvider {
    fun pearlLedgerDao(): PearlLedgerDao
    fun shellFindInstanceDao(): ShellFindInstanceDao
    fun shellFindStackDao(): ShellFindStackDao
    fun shellPlacementDao(): ShellPlacementDao
    fun shellFindUpgradeDao(): ShellFindUpgradeDao
    fun userBadgeDao(): UserBadgeDao
    fun userDiscoveryDao(): UserDiscoveryDao
    fun stillwaterLedgerDao(): StillwaterLedgerDao
    fun stillwaterPreferenceDao(): StillwaterPreferenceDao
    fun userShellRoomStateDao(): UserShellRoomStateDao
    fun shellRewardEventDao(): ShellRewardEventDao
    fun objectiveDao(): ObjectiveDao
    fun objectiveCompletionDao(): ObjectiveCompletionDao
    fun objectiveSkippedCycleDao(): ObjectiveSkippedCycleDao
    fun objectiveProcessedSessionDao(): ObjectiveProcessedSessionDao
    fun achievementDao(): AchievementDao
}
