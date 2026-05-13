package com.kingkharnivore.skillz.data.model.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SkillzDatabaseMigrations {

    /**
     * We do not know exactly which DB version users may have installed.
     *
     * So every historical version from 1 through 12 gets a direct safe rebuild
     * into the current schema: version 13.
     *
     * This avoids destructive migration while still letting Room validate the
     * final schema.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        migrationTo13(1),
        migrationTo13(2),
        migrationTo13(3),
        migrationTo13(4),
        migrationTo13(5),
        migrationTo13(6),
        migrationTo13(7),
        migrationTo13(8),
        migrationTo13(9),
        migrationTo13(10),
        migrationTo13(11),
        migrationTo13(12)
    )

    private fun migrationTo13(fromVersion: Int): Migration {
        return object : Migration(fromVersion, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildToVersion13(db)
            }
        }
    }

    private fun rebuildToVersion13(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")

        createLegacySafetyTagIfNeeded(db)

        rebuildTags(db)
        rebuildSessions(db)
        rebuildOngoingSession(db)
        rebuildFlowPlans(db)
        rebuildArcPlans(db)
        rebuildArcPlanSteps(db)
        rebuildActiveArcRun(db)
        rebuildPulses(db)

        db.execSQL("PRAGMA foreign_keys=ON")
    }

    // ------------------------------------------------------------------------
    // Rebuild tables
    // ------------------------------------------------------------------------

    private fun rebuildTags(db: SupportSQLiteDatabase) {
        val oldTable = "tags"
        val newTable = "tags_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (`id`, `name`, `createdAt`)
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${expr(columns, "name", "'Legacy'")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildSessions(db: SupportSQLiteDatabase) {
        val oldTable = "sessions"
        val newTable = "sessions_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `tagId` INTEGER NOT NULL,
                `startTime` INTEGER NOT NULL,
                `endTime` INTEGER NOT NULL,
                `durationMs` INTEGER NOT NULL,
                `surgePlannedMs` INTEGER,
                `surgePoints` INTEGER NOT NULL,
                `scyraPoints` INTEGER NOT NULL,
                `isSoftMode` INTEGER NOT NULL,
                `arcId` INTEGER,
                `arcIndex` INTEGER,
                `arcMultiplierUsed` REAL,
                `arcBonusPoints` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `title`,
                    `description`,
                    `tagId`,
                    `startTime`,
                    `endTime`,
                    `durationMs`,
                    `surgePlannedMs`,
                    `surgePoints`,
                    `scyraPoints`,
                    `isSoftMode`,
                    `arcId`,
                    `arcIndex`,
                    `arcMultiplierUsed`,
                    `arcBonusPoints`,
                    `createdAt`
                )
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${expr(columns, "title", "'Untitled Flow'")},
                    ${expr(columns, "description", "''")},
                    ${validTagExpr(columns)},
                    ${expr(columns, "startTime", "strftime('%s','now') * 1000")},
                    ${expr(columns, "endTime", "strftime('%s','now') * 1000")},
                    ${expr(columns, "durationMs", "0")},
                    ${expr(columns, "surgePlannedMs", "NULL")},
                    ${expr(columns, "surgePoints", "0")},
                    ${expr(columns, "scyraPoints", "0")},
                    ${expr(columns, "isSoftMode", "0")},
                    ${expr(columns, "arcId", "NULL")},
                    ${expr(columns, "arcIndex", "NULL")},
                    ${expr(columns, "arcMultiplierUsed", "NULL")},
                    ${expr(columns, "arcBonusPoints", "0")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_tagId` ON `sessions` (`tagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_arcId` ON `sessions` (`arcId`)")
    }

    private fun rebuildOngoingSession(db: SupportSQLiteDatabase) {
        val oldTable = "ongoing_session"
        val newTable = "ongoing_session_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER NOT NULL,
                `flowInstanceId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `tagName` TEXT NOT NULL,
                `isInFlowMode` INTEGER NOT NULL,
                `isRunning` INTEGER NOT NULL,
                `isSoftMode` INTEGER NOT NULL,
                `baseStartTimeMs` INTEGER,
                `accumulatedBeforeStartMs` INTEGER NOT NULL,
                `isSurgeOn` INTEGER NOT NULL,
                `surgePlannedMs` INTEGER,
                `surgeMilestonesFiredCsv` TEXT NOT NULL,
                `surgeTargetReached` INTEGER NOT NULL,
                `surgeTargetReachedAtMs` INTEGER,
                `surgeFinalCountdownStarted` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `arcId` INTEGER,
                `arcChainBase` REAL,
                `arcSessionCountInArc` INTEGER,
                `arcLastSessionEndTimeMs` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `flowInstanceId`,
                    `title`,
                    `description`,
                    `tagName`,
                    `isInFlowMode`,
                    `isRunning`,
                    `isSoftMode`,
                    `baseStartTimeMs`,
                    `accumulatedBeforeStartMs`,
                    `isSurgeOn`,
                    `surgePlannedMs`,
                    `surgeMilestonesFiredCsv`,
                    `surgeTargetReached`,
                    `surgeTargetReachedAtMs`,
                    `surgeFinalCountdownStarted`,
                    `createdAt`,
                    `arcId`,
                    `arcChainBase`,
                    `arcSessionCountInArc`,
                    `arcLastSessionEndTimeMs`
                )
                SELECT
                    ${expr(columns, "id", "1")},
                    ${expr(columns, "flowInstanceId", "lower(hex(randomblob(16)))")},
                    ${expr(columns, "title", "'Untitled Flow'")},
                    ${expr(columns, "description", "''")},
                    ${expr(columns, "tagName", "''")},
                    ${expr(columns, "isInFlowMode", "1")},
                    ${expr(columns, "isRunning", "0")},
                    ${expr(columns, "isSoftMode", "0")},
                    ${expr(columns, "baseStartTimeMs", "NULL")},
                    ${expr(columns, "accumulatedBeforeStartMs", "0")},
                    ${expr(columns, "isSurgeOn", "0")},
                    ${expr(columns, "surgePlannedMs", "NULL")},
                    ${expr(columns, "surgeMilestonesFiredCsv", "''")},
                    ${expr(columns, "surgeTargetReached", "0")},
                    ${expr(columns, "surgeTargetReachedAtMs", "NULL")},
                    ${expr(columns, "surgeFinalCountdownStarted", "0")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "arcId", "NULL")},
                    ${expr(columns, "arcChainBase", "NULL")},
                    ${expr(columns, "arcSessionCountInArc", "NULL")},
                    ${expr(columns, "arcLastSessionEndTimeMs", "NULL")}
                FROM `$oldTable`
                WHERE ${expr(columns, "id", "1")} = 1
                LIMIT 1
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildFlowPlans(db: SupportSQLiteDatabase) {
        val oldTable = "flow_plans"
        val newTable = "flow_plans_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `tagId` INTEGER,
                `isSoftMode` INTEGER NOT NULL,
                `targetMinutes` INTEGER,
                `launchWithSurge` INTEGER NOT NULL,
                `pinned` INTEGER NOT NULL,
                `archived` INTEGER NOT NULL,
                `launchCount` INTEGER NOT NULL,
                `lastLaunchedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `title`,
                    `tagId`,
                    `isSoftMode`,
                    `targetMinutes`,
                    `launchWithSurge`,
                    `pinned`,
                    `archived`,
                    `launchCount`,
                    `lastLaunchedAt`,
                    `createdAt`,
                    `updatedAt`
                )
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${expr(columns, "title", "'Untitled Flow'")},
                    ${nullableValidTagExpr(columns, "tagId")},
                    ${expr(columns, "isSoftMode", "0")},
                    ${expr(columns, "targetMinutes", "NULL")},
                    ${expr(columns, "launchWithSurge", "0")},
                    ${expr(columns, "pinned", "0")},
                    ${expr(columns, "archived", "0")},
                    ${expr(columns, "launchCount", "0")},
                    ${expr(columns, "lastLaunchedAt", "NULL")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "updatedAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_plans_tagId` ON `flow_plans` (`tagId`)")
    }

    private fun rebuildArcPlans(db: SupportSQLiteDatabase) {
        val oldTable = "arc_plans"
        val newTable = "arc_plans_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `isInStudio` INTEGER NOT NULL,
                `archived` INTEGER NOT NULL,
                `launchCount` INTEGER NOT NULL,
                `lastLaunchedAt` INTEGER,
                `recurrenceType` TEXT NOT NULL,
                `recurrenceDaysCsv` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `title`,
                    `isInStudio`,
                    `archived`,
                    `launchCount`,
                    `lastLaunchedAt`,
                    `recurrenceType`,
                    `recurrenceDaysCsv`,
                    `createdAt`,
                    `updatedAt`
                )
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${expr(columns, "title", "'Untitled Arc'")},
                    ${expr(columns, "isInStudio", "0")},
                    ${expr(columns, "archived", "0")},
                    ${expr(columns, "launchCount", "0")},
                    ${expr(columns, "lastLaunchedAt", "NULL")},
                    ${expr(columns, "recurrenceType", "'one_time'")},
                    ${expr(columns, "recurrenceDaysCsv", "''")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "updatedAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildArcPlanSteps(db: SupportSQLiteDatabase) {
        val oldTable = "arc_plan_steps"
        val newTable = "arc_plan_steps_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `arcPlanId` INTEGER NOT NULL,
                `orderIndex` INTEGER NOT NULL,
                `sourceFlowPlanId` INTEGER,
                `titleSnapshot` TEXT NOT NULL,
                `tagIdSnapshot` INTEGER,
                `isSoftModeSnapshot` INTEGER NOT NULL,
                `targetMinutesSnapshot` INTEGER,
                `launchWithSurgeSnapshot` INTEGER NOT NULL,
                `linkState` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`arcPlanId`) REFERENCES `arc_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`sourceFlowPlanId`) REFERENCES `flow_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`tagIdSnapshot`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `arcPlanId`,
                    `orderIndex`,
                    `sourceFlowPlanId`,
                    `titleSnapshot`,
                    `tagIdSnapshot`,
                    `isSoftModeSnapshot`,
                    `targetMinutesSnapshot`,
                    `launchWithSurgeSnapshot`,
                    `linkState`,
                    `createdAt`,
                    `updatedAt`
                )
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${validArcPlanExpr(columns)},
                    ${expr(columns, "orderIndex", "0")},
                    ${nullableValidFlowPlanExpr(columns)},
                    ${expr(columns, "titleSnapshot", "'Untitled Flow'")},
                    ${nullableValidTagExpr(columns, "tagIdSnapshot")},
                    ${expr(columns, "isSoftModeSnapshot", "0")},
                    ${expr(columns, "targetMinutesSnapshot", "NULL")},
                    ${expr(columns, "launchWithSurgeSnapshot", "0")},
                    ${expr(columns, "linkState", "'linked'")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "updatedAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                WHERE ${validArcPlanWhereExpr(columns)}
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_arcPlanId` ON `arc_plan_steps` (`arcPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_sourceFlowPlanId` ON `arc_plan_steps` (`sourceFlowPlanId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_arc_plan_steps_tagIdSnapshot` ON `arc_plan_steps` (`tagIdSnapshot`)")
    }

    private fun rebuildActiveArcRun(db: SupportSQLiteDatabase) {
        val oldTable = "active_arc_run"
        val newTable = "active_arc_run_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER NOT NULL,
                `arcPlanId` INTEGER NOT NULL,
                `arcTitle` TEXT NOT NULL,
                `currentStepIndex` INTEGER NOT NULL,
                `totalSteps` INTEGER NOT NULL,
                `currentStepTitle` TEXT NOT NULL,
                `currentTagName` TEXT NOT NULL,
                `currentIsSoftMode` INTEGER NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `arcPlanId`,
                    `arcTitle`,
                    `currentStepIndex`,
                    `totalSteps`,
                    `currentStepTitle`,
                    `currentTagName`,
                    `currentIsSoftMode`,
                    `startedAt`,
                    `updatedAt`
                )
                SELECT
                    ${expr(columns, "id", "1")},
                    ${validArcPlanExpr(columns, "arcPlanId")},
                    ${expr(columns, "arcTitle", "'Untitled Arc'")},
                    ${expr(columns, "currentStepIndex", "0")},
                    ${expr(columns, "totalSteps", "1")},
                    ${expr(columns, "currentStepTitle", "'Untitled Flow'")},
                    ${expr(columns, "currentTagName", "''")},
                    ${expr(columns, "currentIsSoftMode", "0")},
                    ${expr(columns, "startedAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "updatedAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                WHERE ${validArcPlanWhereExpr(columns, "arcPlanId")}
                LIMIT 1
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildPulses(db: SupportSQLiteDatabase) {
        val oldTable = "pulses"
        val newTable = "pulses_v13"

        db.execSQL("DROP TABLE IF EXISTS `$newTable`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$newTable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `tagId` INTEGER,
                `parentSessionId` INTEGER,
                `parentFlowInstanceId` TEXT,
                `arcId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`parentSessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`,
                    `title`,
                    `description`,
                    `tagId`,
                    `parentSessionId`,
                    `parentFlowInstanceId`,
                    `arcId`,
                    `createdAt`,
                    `updatedAt`
                )
                SELECT
                    ${expr(columns, "id", "NULL")},
                    ${expr(columns, "title", "'Untitled Pulse'")},
                    ${expr(columns, "description", "''")},
                    ${nullableValidTagExpr(columns, "tagId")},
                    ${nullableValidSessionExpr(columns, "parentSessionId")},
                    ${expr(columns, "parentFlowInstanceId", "NULL")},
                    ${expr(columns, "arcId", "NULL")},
                    ${expr(columns, "createdAt", "strftime('%s','now') * 1000")},
                    ${expr(columns, "updatedAt", "strftime('%s','now') * 1000")}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_tagId` ON `pulses` (`tagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_parentSessionId` ON `pulses` (`parentSessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_parentFlowInstanceId` ON `pulses` (`parentFlowInstanceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_arcId` ON `pulses` (`arcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulses_createdAt` ON `pulses` (`createdAt`)")
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val result = mutableSetOf<String>()

        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                result += cursor.getString(nameIndex)
            }
        }

        return result
    }

    private fun expr(columns: Set<String>, columnName: String, fallbackSql: String): String {
        return if (columns.contains(columnName)) {
            "`$columnName`"
        } else {
            fallbackSql
        }
    }

    /**
     * If an ancient version somehow has sessions but no usable tagId, we need a
     * valid tag because SessionEntity.tagId is NOT NULL and foreign-keyed.
     */
    private fun createLegacySafetyTagIfNeeded(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "tags")) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tags` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        val hasTags = db.query("SELECT id FROM `tags` LIMIT 1").use { it.moveToFirst() }

        if (!hasTags) {
            db.execSQL(
                """
                INSERT INTO `tags` (`name`, `createdAt`)
                VALUES ('Legacy', strftime('%s','now') * 1000)
                """.trimIndent()
            )
        }
    }

    private fun validTagExpr(columns: Set<String>): String {
        return if (columns.contains("tagId")) {
            """
            CASE
                WHEN `tagId` IN (SELECT `id` FROM `tags`) THEN `tagId`
                ELSE (SELECT `id` FROM `tags` ORDER BY `id` ASC LIMIT 1)
            END
            """.trimIndent()
        } else {
            "(SELECT `id` FROM `tags` ORDER BY `id` ASC LIMIT 1)"
        }
    }

    private fun nullableValidTagExpr(columns: Set<String>, columnName: String): String {
        return if (columns.contains(columnName)) {
            """
            CASE
                WHEN `$columnName` IN (SELECT `id` FROM `tags`) THEN `$columnName`
                ELSE NULL
            END
            """.trimIndent()
        } else {
            "NULL"
        }
    }

    private fun nullableValidSessionExpr(columns: Set<String>, columnName: String): String {
        return if (columns.contains(columnName)) {
            """
            CASE
                WHEN `$columnName` IN (SELECT `id` FROM `sessions`) THEN `$columnName`
                ELSE NULL
            END
            """.trimIndent()
        } else {
            "NULL"
        }
    }

    private fun nullableValidFlowPlanExpr(columns: Set<String>): String {
        return if (columns.contains("sourceFlowPlanId")) {
            """
            CASE
                WHEN `sourceFlowPlanId` IN (SELECT `id` FROM `flow_plans`) THEN `sourceFlowPlanId`
                ELSE NULL
            END
            """.trimIndent()
        } else {
            "NULL"
        }
    }

    private fun validArcPlanExpr(
        columns: Set<String>,
        columnName: String = "arcPlanId"
    ): String {
        return if (columns.contains(columnName)) {
            """
            CASE
                WHEN `$columnName` IN (SELECT `id` FROM `arc_plans`) THEN `$columnName`
                ELSE (SELECT `id` FROM `arc_plans` ORDER BY `id` ASC LIMIT 1)
            END
            """.trimIndent()
        } else {
            "(SELECT `id` FROM `arc_plans` ORDER BY `id` ASC LIMIT 1)"
        }
    }

    private fun validArcPlanWhereExpr(
        columns: Set<String>,
        columnName: String = "arcPlanId"
    ): String {
        return if (columns.contains(columnName)) {
            """
            `$columnName` IN (SELECT `id` FROM `arc_plans`)
            """.trimIndent()
        } else {
            "EXISTS (SELECT 1 FROM `arc_plans` LIMIT 1)"
        }
    }
}