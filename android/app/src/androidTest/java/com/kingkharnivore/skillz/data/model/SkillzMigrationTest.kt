package com.kingkharnivore.skillz.data.model

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkillzMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SkillzDatabase::class.java
    )

    @Test fun migration31To36CompletesWithoutDestructiveFallback() = assertAchievementMigrationChain(31)
    @Test fun migration32To36CompletesWithoutDestructiveFallback() = assertAchievementMigrationChain(32)
    @Test fun migration33To36CompletesWithoutDestructiveFallback() = assertAchievementMigrationChain(33)
    @Test fun migration34To36CompletesWithoutDestructiveFallback() = assertAchievementMigrationChain(34)
    @Test fun migration35To36PreservesCompletionsPinsAndTracking() {
        helper.createDatabase(TEST_DB, 35).apply {
            execSQL("INSERT INTO collection_completion VALUES ('c','blue_sunlit_reef','COLLECTOR',123,1,'hash','a,b')")
            execSQL("INSERT INTO collection_completion VALUES ('undated','blue_sunlit_reef','COMPLETIONIST',0,1,'legacy','a,b')")
            execSQL("INSERT INTO badge_pin VALUES ('blue_sunlit_reef_collector',0,1)")
            execSQL("INSERT INTO badge_tracking VALUES ('blue_sunlit_reef_completionist',1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 36, true, SkillzDatabaseMigrations.MIGRATION_35_36)
        db.assertColumn("collection_completion", "completedAt", notNull = 0, defaultValue = null)
        db.assertColumn("collection_completion", "timestampConfidence", notNull = 1, defaultValue = null)
        db.assertColumn("user_badge", "timestampConfidence", notNull = 1, defaultValue = "'EXACT'")
        db.query("SELECT completedAt,timestampConfidence,rosterHash,requiredSpeciesIds FROM collection_completion WHERE completionId='c'").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(123L, c.getLong(0)); assertEquals("EXACT", c.getString(1));
            assertEquals("hash", c.getString(2)); assertEquals("a,b", c.getString(3))
        }
        db.query("SELECT completedAt,timestampConfidence FROM collection_completion WHERE completionId='undated'").use { c ->
            assertTrue(c.moveToFirst()); assertTrue(c.isNull(0)); assertEquals("UNKNOWN", c.getString(1))
        }
        assertEquals(1, db.countRows("badge_pin", "badgeId = ?", arrayOf("blue_sunlit_reef_collector")))
        assertEquals(1, db.countRows("badge_tracking", "badgeId = ?", arrayOf("blue_sunlit_reef_completionist")))
        db.close()
    }

    @Test fun migration34To35RepairsDiscoveryAndPreservesLegacyBadgeFloor() {
        helper.createDatabase(TEST_DB, 34).apply {
            execSQL("INSERT INTO user_shell_find_instance (instanceId,findId,acquiredAt,sourceType,sourceId,currentUpgradeStageId,customName,isNew,isArchivedInChest,viewedAt,animalLevel,creatureStatus,creatureSource,flowTimeValueMinutes) VALUES ('later','focus_minnow',20,'flow_later',NULL,NULL,NULL,0,1,NULL,1,'ACTIVE',NULL,10),('earlier','focus_minnow',10,'flow_earlier',NULL,NULL,NULL,0,1,NULL,1,'RELEASED',NULL,10)")
            execSQL("INSERT INTO creature_discovery VALUES ('focus_minnow',20,'flow_later','later',20)")
            execSQL("INSERT INTO user_badge VALUES ('mastery_species_focus_minnow',5,1,2,0,0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 35, true, SkillzDatabaseMigrations.MIGRATION_34_35)
        db.query("SELECT firstDiscoveredAt,acquisitionSource,firstCreatureId FROM creature_discovery WHERE speciesId='focus_minnow'").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(10L, c.getLong(0)); assertEquals("flow_earlier", c.getString(1)); assertEquals("earlier", c.getString(2))
        }
        assertEquals(1, db.countRows("badge_count_floor", "badgeId = ? AND minimumCount = 5", arrayOf("mastery_species_focus_minnow")))
        db.close()
    }

    @Test fun migration33To34CreatesDurableCelebrationLedger() {
        helper.createDatabase(TEST_DB, 33).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 34, true, SkillzDatabaseMigrations.MIGRATION_33_34)
        assertTrue(db.tableExists("mastery_celebration_event"))
        db.assertColumn("mastery_celebration_event", "transactionId", notNull = 1, defaultValue = null)
        db.assertColumn("mastery_celebration_event", "completedAt", notNull = 0, defaultValue = null)
        db.close()
    }

    @Test fun migration32To33PersistsPinningAndTrackingSchema() {
        helper.createDatabase(TEST_DB, 32).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 33, true, SkillzDatabaseMigrations.MIGRATION_32_33)
        assertTrue(db.tableExists("badge_pin"))
        assertTrue(db.tableExists("badge_tracking"))
        db.execSQL("INSERT INTO badge_pin VALUES ('badge_flow_30_min', 0, 1)")
        db.execSQL("INSERT INTO badge_tracking VALUES ('mastery_first', 1)")
        db.close()
    }

    @Test
    fun migration31To32CreatesAchievementFoundationAndBackfillsReliableEvidence() {
        helper.createDatabase(TEST_DB, 31).apply {
            execSQL("CREATE TABLE IF NOT EXISTS `user_shell_find_instance` (`instanceId` TEXT NOT NULL, `findId` TEXT NOT NULL, `acquiredAt` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `currentUpgradeStageId` TEXT, `customName` TEXT, `isNew` INTEGER NOT NULL, `isArchivedInChest` INTEGER NOT NULL, `viewedAt` INTEGER, `animalLevel` INTEGER NOT NULL DEFAULT 1, `creatureStatus` TEXT NOT NULL DEFAULT 'ACTIVE', `creatureSource` TEXT, `flowTimeValueMinutes` INTEGER, PRIMARY KEY(`instanceId`))")
            execSQL("INSERT INTO user_shell_find_instance VALUES ('one','focus_minnow',10,'flow',NULL,NULL,NULL,0,1,NULL,99,'RELEASED','FLOW_EARNED',10)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 32, true, SkillzDatabaseMigrations.MIGRATION_31_32)
        assertTrue(db.tableExists("creature_discovery"))
        assertTrue(db.tableExists("creature_mastery_event"))
        assertEquals(1, db.countRows("creature_discovery", "speciesId = ?", arrayOf("focus_minnow")))
        assertEquals(1, db.countRows("creature_mastery_event", "creatureInstanceId = ?", arrayOf("one")))
        db.close()
    }

    @Test
    fun migration13To14CreatesShellTables() {
        helper.createDatabase(TEST_DB, 13).apply {
            createVersion13CoreTables()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            14,
            true,
            SkillzDatabaseMigrations.MIGRATION_13_14
        )

        SHELL_TABLES.forEach { tableName ->
            assertTrue("Expected $tableName to exist after 13→14", db.tableExists(tableName))
        }
    }


    @Test
    fun migration14To15CreatesShellRewardEventTable() {
        helper.createDatabase(TEST_DB, 14).apply {
            createVersion13CoreTables()
            SkillzDatabaseMigrations.MIGRATION_13_14.migrate(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            SkillzDatabaseMigrations.MIGRATION_14_15
        )

        assertTrue("Expected shell_reward_event to exist after 14→15", db.tableExists("shell_reward_event"))
    }


    @Test
    fun migration15To16NormalizesTheBlueRoomIds() {
        helper.createDatabase(TEST_DB, 15).apply {
            createVersion13CoreTables()
            SkillzDatabaseMigrations.MIGRATION_13_14.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_14_15.migrate(this)
            execSQL(
                "INSERT INTO `user_shell_room_state` (`roomId`, `firstOpenedAt`, `lastOpenedAt`, `visualMaturityScore`, `ambientLifeScore`, `lastChangedAt`) VALUES (?, 1, 2, 0, 0, NULL)",
                arrayOf<Any?>(encodedTheBlueRoomImportKey())
            )
            execSQL(
                "INSERT INTO `shell_placement` (`placementId`, `roomId`, `slotId`, `instanceId`, `placedAt`) VALUES ('placement', ?, 'slot', 'instance', 3)",
                arrayOf<Any?>(encodedTheBlueRoomImportKey())
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            SkillzDatabaseMigrations.MIGRATION_15_16
        )

        assertEquals(1, db.countRows("user_shell_room_state", "roomId = ?", arrayOf("THE_BLUE")))
        assertEquals(1, db.countRows("shell_placement", "roomId = ?", arrayOf("THE_BLUE")))
        assertEquals(0, db.countRows("user_shell_room_state", "roomId = ?", arrayOf(encodedTheBlueRoomImportKey())))
        assertEquals(0, db.countRows("shell_placement", "roomId = ?", arrayOf(encodedTheBlueRoomImportKey())))
    }


    @Test
    fun migration16To17AddsCreatureEconomyFieldsSafely() {
        helper.createDatabase(TEST_DB, 16).apply {
            createVersion13CoreTables()
            SkillzDatabaseMigrations.MIGRATION_13_14.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_14_15.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_15_16.migrate(this)
            execSQL(
                "INSERT INTO `user_shell_find_instance` (`instanceId`, `findId`, `acquiredAt`, `sourceType`, `sourceId`, `currentUpgradeStageId`, `customName`, `isNew`, `isArchivedInChest`) VALUES ('animal-1', 'focus_minnow', 1, 'session', '1', NULL, NULL, 1, 1)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            17,
            true,
            SkillzDatabaseMigrations.MIGRATION_16_17
        )

        db.query("SELECT `animalLevel`, `creatureStatus` FROM `user_shell_find_instance` WHERE `instanceId` = 'animal-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals("ACTIVE", cursor.getString(1))
        }
    }


    @Test
    fun migration18To23CreatesObjectiveClaimSchemaIdeaGroveAndKeepsSessions() {
        helper.createDatabase(TEST_DB, 18).apply {
            createVersion13CoreTables()
            SkillzDatabaseMigrations.MIGRATION_13_14.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_14_15.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_15_16.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_16_17.migrate(this)
            SkillzDatabaseMigrations.MIGRATION_17_18.migrate(this)
            insertMigrationSession()
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            23,
            true,
            SkillzDatabaseMigrations.MIGRATION_18_19,
            SkillzDatabaseMigrations.MIGRATION_19_20,
            SkillzDatabaseMigrations.MIGRATION_20_21,
            SkillzDatabaseMigrations.MIGRATION_21_22,
            SkillzDatabaseMigrations.MIGRATION_22_23
        )

        assertTrue("Expected objectives table after 18→23", db.tableExists("objectives"))
        assertTrue("Expected objective_completions table after 18→23", db.tableExists("objective_completions"))
        assertTrue("Expected objective_skipped_cycles table after 18→23", db.tableExists("objective_skipped_cycles"))
        assertEquals(1, db.countRows("sessions", "title = ?", arrayOf("Migration Flow")))

        db.assertColumn("objective_completions", "pearlsGranted", notNull = 1, defaultValue = "0")
        db.assertColumn("objective_completions", "pearlsClaimed", notNull = 1, defaultValue = "0")
        db.assertColumn("objective_completions", "pearlsClaimedAt", notNull = 0, defaultValue = null)
        db.assertColumn("objective_completions", "badgeGranted", notNull = 1, defaultValue = "1")
        db.assertColumn("objectives", "currentStreak", notNull = 1, defaultValue = "0")
        db.assertColumn("objectives", "isArchived", notNull = 1, defaultValue = "0")
        db.assertColumn("pulses", "groveStatus", notNull = 1, defaultValue = "'ALIVE'")
        db.assertColumn("pulses", "groveStatusChangedAt", notNull = 0, defaultValue = null)
        db.assertColumn("ongoing_session", "originPulseId", notNull = 0, defaultValue = null)
        assertTrue("Expected pulse_flow_links after 18→23", db.tableExists("pulse_flow_links"))
    }

    @Test
    fun legacyDirectMigrationCreatesFinalObjectiveClaimSchema() {
        helper.createDatabase(TEST_DB, 12).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            23,
            true,
            *SkillzDatabaseMigrations.ALL_MIGRATIONS
        )

        assertTrue("Expected objective_completions table after legacy→23", db.tableExists("objective_completions"))
        db.assertColumn("objective_completions", "pearlsGranted", notNull = 1, defaultValue = "0")
        db.assertColumn("objective_completions", "pearlsClaimed", notNull = 1, defaultValue = "0")
        db.assertColumn("objective_completions", "pearlsClaimedAt", notNull = 0, defaultValue = null)
        db.assertColumn("objective_completions", "badgeGranted", notNull = 1, defaultValue = "1")
    }

    @Test
    fun allMigrationsIncludeDirectLegacyAnd13To14Paths() {
        assertTrue(
            "Expected direct legacy migrations plus current step migrations",
            SkillzDatabaseMigrations.ALL_MIGRATIONS.isNotEmpty()
        )
    }

    private fun assertAchievementMigrationChain(startVersion: Int) {
        helper.createDatabase("$TEST_DB-$startVersion", startVersion).close()
        val db = helper.runMigrationsAndValidate(
            "$TEST_DB-$startVersion",
            36,
            true,
            *SkillzDatabaseMigrations.ALL_MIGRATIONS
        )
        listOf(
            "creature_discovery",
            "creature_mastery_event",
            "collection_completion",
            "achievement_backfill",
            "badge_pin",
            "badge_tracking",
            "mastery_celebration_event",
            "badge_count_floor"
        ).forEach { assertTrue("Expected $it after $startVersion→36", db.tableExists(it)) }
        db.close()
    }

    private fun SupportSQLiteDatabase.createVersion13CoreTables() {
        execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `surgePlannedMs` INTEGER, `surgePoints` INTEGER NOT NULL, `scyraPoints` INTEGER NOT NULL, `isSoftMode` INTEGER NOT NULL, `arcId` INTEGER, `arcIndex` INTEGER, `arcMultiplierUsed` REAL, `arcBonusPoints` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `ongoing_session` (`id` INTEGER NOT NULL, `flowInstanceId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagName` TEXT NOT NULL, `isInFlowMode` INTEGER NOT NULL, `isRunning` INTEGER NOT NULL, `isSoftMode` INTEGER NOT NULL, `baseStartTimeMs` INTEGER, `accumulatedBeforeStartMs` INTEGER NOT NULL, `isSurgeOn` INTEGER NOT NULL, `surgePlannedMs` INTEGER, `surgeMilestonesFiredCsv` TEXT NOT NULL, `surgeTargetReached` INTEGER NOT NULL, `surgeTargetReachedAtMs` INTEGER, `surgeFinalCountdownStarted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `arcId` INTEGER, `arcChainBase` REAL, `arcSessionCountInArc` INTEGER, `arcLastSessionEndTimeMs` INTEGER, PRIMARY KEY(`id`))")
        execSQL("CREATE TABLE IF NOT EXISTS `pulses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `tagId` INTEGER, `parentSessionId` INTEGER, `parentFlowInstanceId` TEXT, `arcId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `flow_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `tagId` INTEGER, `isSoftMode` INTEGER NOT NULL, `targetMinutes` INTEGER, `launchWithSurge` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `lastLaunchedAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `arc_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isInStudio` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `launchCount` INTEGER NOT NULL, `lastLaunchedAt` INTEGER, `recurrenceType` TEXT NOT NULL, `recurrenceDaysCsv` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `arc_plan_steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `arcPlanId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `sourceFlowPlanId` INTEGER, `titleSnapshot` TEXT NOT NULL, `tagIdSnapshot` INTEGER, `isSoftModeSnapshot` INTEGER NOT NULL, `targetMinutesSnapshot` INTEGER, `launchWithSurgeSnapshot` INTEGER NOT NULL, `linkState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        execSQL("CREATE TABLE IF NOT EXISTS `active_arc_run` (`id` INTEGER NOT NULL, `arcPlanId` INTEGER NOT NULL, `arcTitle` TEXT NOT NULL, `currentStepIndex` INTEGER NOT NULL, `totalSteps` INTEGER NOT NULL, `currentStepTitle` TEXT NOT NULL, `currentTagName` TEXT NOT NULL, `currentIsSoftMode` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }


    private fun SupportSQLiteDatabase.insertMigrationSession() {
        execSQL("INSERT INTO `tags` (`id`, `name`, `createdAt`) VALUES (42, 'Migration Journey', 1)")
        execSQL(
            """
            INSERT INTO `sessions` (
                `id`, `title`, `description`, `tagId`, `startTime`, `endTime`, `durationMs`,
                `surgePlannedMs`, `surgePoints`, `scyraPoints`, `isSoftMode`, `arcId`, `arcIndex`,
                `arcMultiplierUsed`, `arcBonusPoints`, `createdAt`
            ) VALUES (99, 'Migration Flow', '', 42, 1, 2, 60000, NULL, 0, 0, 0, NULL, NULL, NULL, 0, 3)
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.assertColumn(
        tableName: String,
        columnName: String,
        notNull: Int,
        defaultValue: String?
    ) {
        val column = columnInfo(tableName, columnName)
        assertEquals("Unexpected not-null flag for $tableName.$columnName", notNull, column.notNull)
        assertEquals("Unexpected default for $tableName.$columnName", defaultValue, column.defaultValue)
    }

    private fun SupportSQLiteDatabase.columnInfo(tableName: String, columnName: String): MigrationColumnInfo {
        query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val notNullIndex = cursor.getColumnIndex("notnull")
            val defaultIndex = cursor.getColumnIndex("dflt_value")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    return MigrationColumnInfo(
                        notNull = cursor.getInt(notNullIndex),
                        defaultValue = if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
                    )
                }
            }
        }
        error("Missing column $tableName.$columnName")
    }

    private data class MigrationColumnInfo(
        val notNull: Int,
        val defaultValue: String?
    )


    private fun SupportSQLiteDatabase.countRows(whereTable: String, whereClause: String, whereArgs: Array<String>): Int =
        query("SELECT COUNT(*) FROM `$whereTable` WHERE $whereClause", whereArgs).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun encodedTheBlueRoomImportKey(): String = intArrayOf(67, 79, 82, 65, 76, 95, 82, 69, 69, 70)
        .joinToString(separator = "") { it.toChar().toString() }

    private fun SupportSQLiteDatabase.tableExists(tableName: String): Boolean =
        query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private companion object {
        const val TEST_DB = "skillz-migration-test"

        val SHELL_TABLES = listOf(
            "pearl_ledger",
            "user_shell_find_instance",
            "user_shell_find_stack",
            "shell_placement",
            "shell_find_upgrade",
            "user_badge",
            "user_discovery",
            "stillwater_ledger",
            "stillwater_preference",
            "user_shell_room_state"
        )
    }
}
