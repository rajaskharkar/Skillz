package com.kingkharnivore.skillz.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ActiveArcRunDao
import com.kingkharnivore.skillz.data.model.dao.ArcPlanDao
import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.dao.shell.PearlLedgerDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindInstanceDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindStackDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellFindUpgradeDao
import com.kingkharnivore.skillz.data.model.dao.shell.ShellPlacementDao
import com.kingkharnivore.skillz.data.model.dao.shell.StillwaterLedgerDao
import com.kingkharnivore.skillz.data.model.dao.shell.StillwaterPreferenceDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserBadgeDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserDiscoveryDao
import com.kingkharnivore.skillz.data.model.dao.shell.UserShellRoomStateDao
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val LEGACY_TO_14_MIGRATIONS: Array<Migration> = (1..12).map { startVersion ->
        object : Migration(startVersion, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateLegacyDatabaseTo14(db)
            }
        }
    }.toTypedArray()

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createShellTables(db)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SkillzDatabase {
        return Room.databaseBuilder(
            context,
            SkillzDatabase::class.java,
            "skillz_db"
        ).addMigrations(*LEGACY_TO_14_MIGRATIONS, MIGRATION_13_14).build()
    }

    @Provides
    fun provideTagDao(db: SkillzDatabase): TagDao = db.tagDao()

    @Provides
    fun provideSessionDao(db: SkillzDatabase): SessionDao = db.sessionDao()

    @Provides
    fun providePulseDao(db: SkillzDatabase): PulseDao = db.pulseDao()

    @Provides
    fun provideOngoingSessionDao(db: SkillzDatabase): OngoingSessionDao =
        db.ongoingSessionDao()

    @Provides
    fun provideFlowPlanDao(db: SkillzDatabase): FlowPlanDao = db.flowPlanDao()

    @Provides
    fun provideArcPlanDao(db: SkillzDatabase): ArcPlanDao = db.arcPlanDao()

    @Provides
    fun provideActiveArcRunDao(db: SkillzDatabase): ActiveArcRunDao = db.activeArcRunDao()

    @Provides fun providePearlLedgerDao(db: SkillzDatabase): PearlLedgerDao = db.pearlLedgerDao()
    @Provides fun provideShellFindInstanceDao(db: SkillzDatabase): ShellFindInstanceDao = db.shellFindInstanceDao()
    @Provides fun provideShellFindStackDao(db: SkillzDatabase): ShellFindStackDao = db.shellFindStackDao()
    @Provides fun provideShellPlacementDao(db: SkillzDatabase): ShellPlacementDao = db.shellPlacementDao()
    @Provides fun provideShellFindUpgradeDao(db: SkillzDatabase): ShellFindUpgradeDao = db.shellFindUpgradeDao()
    @Provides fun provideUserBadgeDao(db: SkillzDatabase): UserBadgeDao = db.userBadgeDao()
    @Provides fun provideUserDiscoveryDao(db: SkillzDatabase): UserDiscoveryDao = db.userDiscoveryDao()
    @Provides fun provideStillwaterLedgerDao(db: SkillzDatabase): StillwaterLedgerDao = db.stillwaterLedgerDao()
    @Provides fun provideStillwaterPreferenceDao(db: SkillzDatabase): StillwaterPreferenceDao = db.stillwaterPreferenceDao()
    @Provides fun provideUserShellRoomStateDao(db: SkillzDatabase): UserShellRoomStateDao = db.userShellRoomStateDao()

    @Provides
    @Singleton
    fun provideArcPrefs(ds: DataStore<Preferences>): ArcPrefs = ArcPrefs(ds)

    private val Context.skillzDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "skillz_prefs"
    )

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.skillzDataStore

    private fun migrateLegacyDatabaseTo14(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        createCurrentCoreTables(db)
        rebuildTags(db)
        rebuildSessions(db)
        rebuildOngoingSession(db)
        rebuildPulses(db)
        rebuildFlowPlans(db)
        rebuildArcPlans(db)
        rebuildArcPlanSteps(db)
        rebuildActiveArcRun(db)
        createCurrentCoreIndices(db)
        createShellTables(db)
        db.execSQL("PRAGMA foreign_keys=ON")
    }

    private fun createCurrentCoreTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
    }

    private fun rebuildTags(db: SupportSQLiteDatabase) {
        if (tableExists(db, "tags")) {
            db.execSQL("INSERT OR IGNORE INTO `tags_new` (`id`, `name`, `createdAt`) SELECT `id`, `name`, ${columnOrDefault(db, "tags", "createdAt", nowSql())} FROM `tags`")
        } else if (tableExists(db, "skills")) {
            db.execSQL("INSERT OR IGNORE INTO `tags_new` (`id`, `name`, `createdAt`) SELECT `id`, `name`, ${nowSql()} FROM `skills`")
        }
        db.execSQL("INSERT OR IGNORE INTO `tags_new` (`id`, `name`, `createdAt`) VALUES (1, 'Flow', ${nowSql()})")
        replaceTable(db, "tags")
    }

    private fun rebuildSessions(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sessions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `surgePlannedMs` INTEGER, `surgePoints` INTEGER NOT NULL, `scyraPoints` INTEGER NOT NULL, `isSoftMode` INTEGER NOT NULL, `arcId` INTEGER, `arcIndex` INTEGER, `arcMultiplierUsed` REAL, `arcBonusPoints` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        if (tableExists(db, "sessions")) {
            val cols = columns(db, "sessions")
            val title = when {
                "title" in cols -> "`title`"
                "notes" in cols -> "COALESCE(`notes`, 'Flow')"
                else -> "'Flow'"
            }
            val description = when {
                "description" in cols -> "`description`"
                "notes" in cols -> "COALESCE(`notes`, '')"
                else -> "''"
            }
            val tagId = when {
                "tagId" in cols -> "`tagId`"
                "skillId" in cols -> "`skillId`"
                else -> "1"
            }
            db.execSQL(
                """
                INSERT OR IGNORE INTO `sessions_new` (`id`, `title`, `description`, `tagId`, `startTime`, `endTime`, `durationMs`, `surgePlannedMs`, `surgePoints`, `scyraPoints`, `isSoftMode`, `arcId`, `arcIndex`, `arcMultiplierUsed`, `arcBonusPoints`, `createdAt`)
                SELECT ${selectExpr(cols, "id", "NULL")}, $title, $description, COALESCE($tagId, 1), ${selectExpr(cols, "startTime", "0")}, ${selectExpr(cols, "endTime", "0")}, ${selectExpr(cols, "durationMs", "0")}, ${selectExpr(cols, "surgePlannedMs", "NULL")}, ${selectExpr(cols, "surgePoints", "0")}, ${selectExpr(cols, "scyraPoints", "0")}, ${selectExpr(cols, "isSoftMode", "0")}, ${selectExpr(cols, "arcId", "NULL")}, ${selectExpr(cols, "arcIndex", "NULL")}, ${selectExpr(cols, "arcMultiplierUsed", "NULL")}, ${selectExpr(cols, "arcBonusPoints", "0")}, ${selectExpr(cols, "createdAt", nowSql())}
                FROM `sessions`
                """.trimIndent()
            )
        }
        replaceTable(db, "sessions")
    }

    private fun rebuildOngoingSession(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ongoing_session_new` (`id` INTEGER NOT NULL, `flowInstanceId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagName` TEXT NOT NULL, `isInFlowMode` INTEGER NOT NULL, `isRunning` INTEGER NOT NULL, `isSoftMode` INTEGER NOT NULL, `baseStartTimeMs` INTEGER, `accumulatedBeforeStartMs` INTEGER NOT NULL, `isSurgeOn` INTEGER NOT NULL, `surgePlannedMs` INTEGER, `surgeMilestonesFiredCsv` TEXT NOT NULL, `surgeTargetReached` INTEGER NOT NULL, `surgeTargetReachedAtMs` INTEGER, `surgeFinalCountdownStarted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `arcId` INTEGER, `arcChainBase` REAL, `arcSessionCountInArc` INTEGER, `arcLastSessionEndTimeMs` INTEGER, PRIMARY KEY(`id`))")
        if (tableExists(db, "ongoing_session")) {
            val cols = columns(db, "ongoing_session")
            val isInFlowMode = when {
                "isInFlowMode" in cols -> "`isInFlowMode`"
                "isInFocusMode" in cols -> "`isInFocusMode`"
                else -> "0"
            }
            db.execSQL(
                """
                INSERT OR REPLACE INTO `ongoing_session_new` (`id`, `flowInstanceId`, `title`, `description`, `tagName`, `isInFlowMode`, `isRunning`, `isSoftMode`, `baseStartTimeMs`, `accumulatedBeforeStartMs`, `isSurgeOn`, `surgePlannedMs`, `surgeMilestonesFiredCsv`, `surgeTargetReached`, `surgeTargetReachedAtMs`, `surgeFinalCountdownStarted`, `createdAt`, `arcId`, `arcChainBase`, `arcSessionCountInArc`, `arcLastSessionEndTimeMs`)
                SELECT ${selectExpr(cols, "id", "1")}, ${selectExpr(cols, "flowInstanceId", "'legacy-' || lower(hex(randomblob(16)))")}, ${selectExpr(cols, "title", "''")}, ${selectExpr(cols, "description", "''")}, ${selectExpr(cols, "tagName", "''")}, $isInFlowMode, ${selectExpr(cols, "isRunning", "0")}, ${selectExpr(cols, "isSoftMode", "0")}, ${selectExpr(cols, "baseStartTimeMs", "NULL")}, ${selectExpr(cols, "accumulatedBeforeStartMs", "0")}, ${selectExpr(cols, "isSurgeOn", "0")}, ${selectExpr(cols, "surgePlannedMs", "NULL")}, ${selectExpr(cols, "surgeMilestonesFiredCsv", "''")}, ${selectExpr(cols, "surgeTargetReached", "0")}, ${selectExpr(cols, "surgeTargetReachedAtMs", "NULL")}, ${selectExpr(cols, "surgeFinalCountdownStarted", "0")}, ${selectExpr(cols, "createdAt", nowSql())}, ${selectExpr(cols, "arcId", "NULL")}, ${selectExpr(cols, "arcChainBase", "NULL")}, ${selectExpr(cols, "arcSessionCountInArc", "NULL")}, ${selectExpr(cols, "arcLastSessionEndTimeMs", "NULL")}
                FROM `ongoing_session`
                """.trimIndent()
            )
        }
        replaceTable(db, "ongoing_session")
    }

    private fun rebuildPulses(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pulses_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagId` INTEGER, `parentSessionId` INTEGER, `parentFlowInstanceId` TEXT, `arcId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`parentSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        copyIfExists(db, "pulses", "pulses_new", listOf(
            "id" to "NULL", "title" to "''", "description" to "''", "tagId" to "NULL", "parentSessionId" to "NULL", "parentFlowInstanceId" to "NULL", "arcId" to "NULL", "createdAt" to nowSql(), "updatedAt" to nowSql()
        ))
        replaceTable(db, "pulses")
    }

    private fun rebuildFlowPlans(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `flow_plans_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `tagId` INTEGER, `isSoftMode` INTEGER NOT NULL, `targetMinutes` INTEGER, `launchWithSurge` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `lastLaunchedAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        copyIfExists(db, "flow_plans", "flow_plans_new", listOf(
            "id" to "NULL", "title" to "''", "tagId" to "NULL", "isSoftMode" to "0", "targetMinutes" to "NULL", "launchWithSurge" to "0", "pinned" to "0", "archived" to "0", "launchCount" to "0", "lastLaunchedAt" to "NULL", "createdAt" to nowSql(), "updatedAt" to nowSql()
        ))
        replaceTable(db, "flow_plans")
    }

    private fun rebuildArcPlans(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `arc_plans_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isInStudio` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `lastLaunchedAt` INTEGER, `recurrenceType` TEXT NOT NULL, `recurrenceDaysCsv` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        copyIfExists(db, "arc_plans", "arc_plans_new", listOf(
            "id" to "NULL", "title" to "''", "isInStudio" to "0", "archived" to "0", "launchCount" to "0", "lastLaunchedAt" to "NULL", "recurrenceType" to "'one_time'", "recurrenceDaysCsv" to "''", "createdAt" to nowSql(), "updatedAt" to nowSql()
        ))
        replaceTable(db, "arc_plans")
    }

    private fun rebuildArcPlanSteps(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `arc_plan_steps_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `arcPlanId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `sourceFlowPlanId` INTEGER, `titleSnapshot` TEXT NOT NULL, `tagIdSnapshot` INTEGER, `isSoftModeSnapshot` INTEGER NOT NULL, `targetMinutesSnapshot` INTEGER, `launchWithSurgeSnapshot` INTEGER NOT NULL, `linkState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`arcPlanId`) REFERENCES `arc_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`sourceFlowPlanId`) REFERENCES `flow_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`tagIdSnapshot`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        copyIfExists(db, "arc_plan_steps", "arc_plan_steps_new", listOf(
            "id" to "NULL", "arcPlanId" to "0", "orderIndex" to "0", "sourceFlowPlanId" to "NULL", "titleSnapshot" to "''", "tagIdSnapshot" to "NULL", "isSoftModeSnapshot" to "0", "targetMinutesSnapshot" to "NULL", "launchWithSurgeSnapshot" to "0", "linkState" to "'linked'", "createdAt" to nowSql(), "updatedAt" to nowSql()
        ))
        replaceTable(db, "arc_plan_steps")
    }

    private fun rebuildActiveArcRun(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `active_arc_run_new` (`id` INTEGER NOT NULL, `arcPlanId` INTEGER NOT NULL, `arcTitle` TEXT NOT NULL, `currentStepIndex` INTEGER NOT NULL, `totalSteps` INTEGER NOT NULL, `currentStepTitle` TEXT NOT NULL, `currentTagName` TEXT NOT NULL, `currentIsSoftMode` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        copyIfExists(db, "active_arc_run", "active_arc_run_new", listOf(
            "id" to "1", "arcPlanId" to "0", "arcTitle" to "''", "currentStepIndex" to "0", "totalSteps" to "0", "currentStepTitle" to "''", "currentTagName" to "''", "currentIsSoftMode" to "0", "startedAt" to nowSql(), "updatedAt" to nowSql()
        ))
        replaceTable(db, "active_arc_run")
    }

    private fun createCurrentCoreIndices(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_tagId` ON `sessions` (`tagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_arcId` ON `sessions` (`arcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_tagId` ON `pulses` (`tagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_parentSessionId` ON `pulses` (`parentSessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_parentFlowInstanceId` ON `pulses` (`parentFlowInstanceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_arcId` ON `pulses` (`arcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_createdAt` ON `pulses` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_plans_tagId` ON `flow_plans` (`tagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_arcPlanId` ON `arc_plan_steps` (`arcPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_sourceFlowPlanId` ON `arc_plan_steps` (`sourceFlowPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_tagIdSnapshot` ON `arc_plan_steps` (`tagIdSnapshot`)")
    }

    private fun createShellTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pearl_ledger` (`id` TEXT NOT NULL, `delta` INTEGER NOT NULL, `reason` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `createdAt` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_shell_find_instance` (`instanceId` TEXT NOT NULL, `findId` TEXT NOT NULL, `acquiredAt` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `currentUpgradeStageId` TEXT, `customName` TEXT, `isNew` INTEGER NOT NULL, `isArchivedInChest` INTEGER NOT NULL, PRIMARY KEY(`instanceId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_findId` ON `user_shell_find_instance` (`findId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_sourceType` ON `user_shell_find_instance` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_sourceId` ON `user_shell_find_instance` (`sourceId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_shell_find_stack` (`findId` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `firstAcquiredAt` INTEGER NOT NULL, `lastAcquiredAt` INTEGER NOT NULL, `isNew` INTEGER NOT NULL, PRIMARY KEY(`findId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_placement` (`placementId` TEXT NOT NULL, `roomId` TEXT NOT NULL, `slotId` TEXT NOT NULL, `instanceId` TEXT NOT NULL, `placedAt` INTEGER NOT NULL, PRIMARY KEY(`placementId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_placement_roomId` ON `shell_placement` (`roomId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_placement_slotId` ON `shell_placement` (`slotId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shell_placement_roomId_slotId` ON `shell_placement` (`roomId`, `slotId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shell_placement_instanceId` ON `shell_placement` (`instanceId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `shell_find_upgrade` (`upgradeEventId` TEXT NOT NULL, `instanceId` TEXT NOT NULL, `fromStageId` TEXT, `toStageId` TEXT NOT NULL, `pearlCost` INTEGER NOT NULL, `upgradedAt` INTEGER NOT NULL, PRIMARY KEY(`upgradeEventId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_find_upgrade_instanceId` ON `shell_find_upgrade` (`instanceId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_badge` (`badgeId` TEXT NOT NULL, `count` INTEGER NOT NULL, `firstEarnedAt` INTEGER NOT NULL, `lastEarnedAt` INTEGER NOT NULL, `isNew` INTEGER NOT NULL, PRIMARY KEY(`badgeId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_discovery` (`userDiscoveryId` TEXT NOT NULL, `discoveryId` TEXT NOT NULL, `discoveredAt` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `grantedFindInstanceId` TEXT, `isNew` INTEGER NOT NULL, PRIMARY KEY(`userDiscoveryId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_discoveryId` ON `user_discovery` (`discoveryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_sourceType` ON `user_discovery` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_sourceId` ON `user_discovery` (`sourceId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `stillwater_ledger` (`id` TEXT NOT NULL, `units` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stillwater_ledger_sourceType` ON `stillwater_ledger` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stillwater_ledger_sourceId` ON `stillwater_ledger` (`sourceId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `stillwater_preference` (`id` INTEGER NOT NULL, `perspective` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_shell_room_state` (`roomId` TEXT NOT NULL, `firstOpenedAt` INTEGER, `lastOpenedAt` INTEGER, `visualMaturityScore` INTEGER NOT NULL, `ambientLifeScore` INTEGER NOT NULL, `lastChangedAt` INTEGER, PRIMARY KEY(`roomId`))")
    }

    private fun copyIfExists(
        db: SupportSQLiteDatabase,
        sourceTable: String,
        targetTable: String,
        columnsWithDefaults: List<Pair<String, String>>
    ) {
        if (!tableExists(db, sourceTable)) return
        val sourceColumns = columns(db, sourceTable)
        val targetColumns = columnsWithDefaults.joinToString(", ") { "`${it.first}`" }
        val selectExpressions = columnsWithDefaults.joinToString(", ") { (column, default) ->
            selectExpr(sourceColumns, column, default)
        }
        db.execSQL("INSERT OR IGNORE INTO `$targetTable` ($targetColumns) SELECT $selectExpressions FROM `$sourceTable`")
    }

    private fun replaceTable(db: SupportSQLiteDatabase, tableName: String) {
        if (tableExists(db, tableName)) {
            db.execSQL("ALTER TABLE `$tableName` RENAME TO `${tableName}_legacy_migration`")
        }
        db.execSQL("ALTER TABLE `${tableName}_new` RENAME TO `$tableName`")
        // Keep the renamed legacy table around instead of dropping it. It is outside the
        // Room entity graph, but preserves pre-migration rows if an unexpected legacy
        // schema variant cannot be copied perfectly.
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                names += cursor.getString(nameIndex)
            }
            return names
        }
    }

    private fun columnOrDefault(db: SupportSQLiteDatabase, tableName: String, column: String, default: String): String =
        selectExpr(columns(db, tableName), column, default)

    private fun selectExpr(sourceColumns: Set<String>, column: String, default: String): String =
        if (column in sourceColumns) "`$column`" else default

    private fun nowSql(): String = "CAST(strftime('%s','now') AS INTEGER) * 1000"
}
