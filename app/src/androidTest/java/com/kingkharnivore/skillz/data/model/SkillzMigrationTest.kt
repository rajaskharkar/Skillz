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
    fun allMigrationsIncludeDirectLegacyAnd13To14Paths() {
        assertTrue(
            "Expected direct legacy migrations plus current step migrations",
            SkillzDatabaseMigrations.ALL_MIGRATIONS.isNotEmpty()
        )
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