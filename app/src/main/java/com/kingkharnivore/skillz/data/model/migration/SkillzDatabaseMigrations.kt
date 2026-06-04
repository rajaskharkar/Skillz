package com.kingkharnivore.skillz.data.model.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SkillzDatabaseMigrations {

    /**
     * Current database version is 23.
     *
     * Versions 1 through 12 are legacy/unknown-ish schemas, so we migrate them
     * directly into the v15 schema using a safe rebuild strategy, then v16
     * normalizes room identifiers.
     *
     * Version 13 is the known pre-Shell schema, so it first adds the Shell
     * tables, v14 adds the Shell reward event read model, v16 normalizes
     * room identifiers, v17 adds creature economy fields, and v18 is a
     * no-op compatibility migration.
     */
    val LEGACY_TO_15_MIGRATIONS: Array<Migration> = (1..12).map { startVersion ->
        object : Migration(startVersion, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateLegacyDatabaseTo15(db)
            }
        }
    }.toTypedArray()

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createShellTables(db)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createShellRewardEventTable(db)
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            normalizeTheBlueRoomIdentifiers(db)
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addCreatureEconomyFields(db)
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema-op migration.
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createObjectiveTables(db)
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateObjectiveCompletionsToClaimSchema(db)
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateObjectiveCompletionsToClaimSchema(db)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addIdeaGroveTables(db)
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No physical schema change. Version 23 refreshes Room's identity hash
            // after the v22 entity schema metadata was corrected to declare the
            // ALIVE default value on PulseEntity.groveStatus.
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
//            createAnchorTables(db)
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
//            addColumnIfMissing(
//                db = db,
//                table = "ongoing_session",
//                column = "anchorBreakOverPending",
//                definition = "INTEGER NOT NULL DEFAULT 0"
//            )
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            normalizePostAnchorTestSchemaToTargetBranch(db)
        }
    }

    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createMovementBonusTables(db)
        }
    }

    private fun normalizePostAnchorTestSchemaToTargetBranch(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")

        /*
         * The Anchor PR/test build may have created Anchor-only tables and/or added
         * Anchor-only columns to ongoing_session while still leaving the local app DB
         * at version 25. This target branch does not contain Anchor entities.
         *
         * We preserve all normal Scyra data and only remove Anchor-only schema pieces
         * that prevent Room from validating the current target-branch schema.
         */

        db.execSQL("DROP TABLE IF EXISTS `anchored_apps`")
        db.execSQL("DROP TABLE IF EXISTS `anchor_session_summary`")

        rebuildOngoingSessionForTargetBranch(db)

        db.execSQL("PRAGMA foreign_keys=ON")
    }

    private fun rebuildOngoingSessionForTargetBranch(db: SupportSQLiteDatabase) {
        val oldTable = "ongoing_session"
        val newTable = "ongoing_session_v26"

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
            `originPulseId` INTEGER,
            `originPulseTitleSnapshot` TEXT,
            `originPulseJourneyNameSnapshot` TEXT,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
        )

        if (tableExists(db, oldTable)) {
            val columns = columns(db, oldTable)

            val isInFlowMode = when {
                "isInFlowMode" in columns -> "`isInFlowMode`"
                "isInFocusMode" in columns -> "`isInFocusMode`"
                else -> "0"
            }

            db.execSQL(
                """
            INSERT OR REPLACE INTO `$newTable` (
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
                `arcLastSessionEndTimeMs`,
                `originPulseId`,
                `originPulseTitleSnapshot`,
                `originPulseJourneyNameSnapshot`
            )
            SELECT
                ${expr(columns, "id", "1")},
                ${expr(columns, "flowInstanceId", "'legacy-' || lower(hex(randomblob(16)))")},
                ${expr(columns, "title", "''")},
                ${expr(columns, "description", "''")},
                ${expr(columns, "tagName", "''")},
                $isInFlowMode,
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
                ${expr(columns, "createdAt", nowSql())},
                ${expr(columns, "arcId", "NULL")},
                ${expr(columns, "arcChainBase", "NULL")},
                ${expr(columns, "arcSessionCountInArc", "NULL")},
                ${expr(columns, "arcLastSessionEndTimeMs", "NULL")},
                ${expr(columns, "originPulseId", "NULL")},
                ${expr(columns, "originPulseTitleSnapshot", "NULL")},
                ${expr(columns, "originPulseJourneyNameSnapshot", "NULL")}
            FROM `$oldTable`
            WHERE ${expr(columns, "id", "1")} = 1
            LIMIT 1
            """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    val ALL_MIGRATIONS: Array<Migration> =
        LEGACY_TO_15_MIGRATIONS +
                MIGRATION_13_14 +
                MIGRATION_14_15 +
                MIGRATION_15_16 +
                MIGRATION_16_17 +
                MIGRATION_17_18 +
                MIGRATION_18_19 +
                MIGRATION_19_20 +
                MIGRATION_20_21 +
                MIGRATION_21_22 +
                MIGRATION_22_23 +
                MIGRATION_23_24 +
                MIGRATION_24_25 +
                MIGRATION_25_26 +
                MIGRATION_26_27

    private fun createMovementBonusTables(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "ongoing_session", "healthEnabledAtStart", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "ongoing_session", "healthPermissionGrantedAtStart", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "ongoing_session", "movementBonusEligibleAtStart", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "ongoing_session", "activeIntervalJson", "TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `flow_health_snapshots` (
                `sessionId` INTEGER NOT NULL,
                `healthEnabledAtStart` INTEGER NOT NULL,
                `permissionGrantedAtStart` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `steps` INTEGER,
                `rawMovementPoints` INTEGER NOT NULL,
                `finalMovementScyraContribution` INTEGER NOT NULL,
                `finalMovementPearlContribution` INTEGER NOT NULL,
                `firstCheckedAtMs` INTEGER,
                `lastCheckedAtMs` INTEGER,
                `capturedAtMs` INTEGER,
                `expiresAtMs` INTEGER,
                `checkCount` INTEGER NOT NULL,
                `flowStartTimeMs` INTEGER NOT NULL,
                `flowEndTimeMs` INTEGER NOT NULL,
                `activeIntervalJson` TEXT,
                `sourceLabel` TEXT,
                `updatedAfterSync` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`sessionId`),
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_health_snapshots_sessionId` ON `flow_health_snapshots` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_health_snapshots_status` ON `flow_health_snapshots` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_health_snapshots_expiresAtMs` ON `flow_health_snapshots` (`expiresAtMs`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `flow_reward_breakdowns` (
                `sessionId` INTEGER NOT NULL,
                `nonMovementPreMultiplierPoints` INTEGER NOT NULL,
                `pulseBonusPoints` INTEGER NOT NULL,
                `surgeBonusPoints` INTEGER NOT NULL,
                `otherPreMultiplierBonusPoints` INTEGER NOT NULL,
                `movementPoints` INTEGER NOT NULL,
                `preMultiplierTotal` INTEGER NOT NULL,
                `arcMultiplier` REAL NOT NULL,
                `streakMultiplier` REAL NOT NULL,
                `otherMultiplier` REAL NOT NULL,
                `arcBonusPoints` INTEGER NOT NULL,
                `finalScyraPoints` INTEGER NOT NULL,
                `pearlsEarned` INTEGER NOT NULL,
                `pearlEligible` INTEGER NOT NULL,
                `roundingMode` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`),
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_flow_reward_breakdowns_sessionId` ON `flow_reward_breakdowns` (`sessionId`)")
    }


    private fun addIdeaGroveTables(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `pulses` ADD COLUMN `groveStatus` TEXT NOT NULL DEFAULT 'ALIVE'")
        db.execSQL("ALTER TABLE `pulses` ADD COLUMN `groveStatusChangedAt` INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pulse_flow_links` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `pulseId` INTEGER NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `linkedAt` INTEGER NOT NULL,
                FOREIGN KEY(`pulseId`) REFERENCES `pulses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pulse_flow_links_pulseId` ON `pulse_flow_links` (`pulseId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pulse_flow_links_sessionId` ON `pulse_flow_links` (`sessionId`)")
        db.execSQL("ALTER TABLE `ongoing_session` ADD COLUMN `originPulseId` INTEGER")
        db.execSQL("ALTER TABLE `ongoing_session` ADD COLUMN `originPulseTitleSnapshot` TEXT")
        db.execSQL("ALTER TABLE `ongoing_session` ADD COLUMN `originPulseJourneyNameSnapshot` TEXT")
    }

    private fun createObjectiveTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `objectives` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `journeyId` INTEGER NOT NULL,
                `journeyNameSnapshot` TEXT NOT NULL,
                `periodType` TEXT NOT NULL,
                `objectiveType` TEXT NOT NULL,
                `targetDurationMs` INTEGER NOT NULL,
                `startAtMs` INTEGER NOT NULL,
                `weeklyBoundaryDay` INTEGER,
                `currentStreak` INTEGER NOT NULL DEFAULT 0,
                `maxStreak` INTEGER NOT NULL DEFAULT 0,
                `totalCompletions` INTEGER NOT NULL DEFAULT 0,
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_objectives_journeyId` ON `objectives` (`journeyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_objectives_periodType` ON `objectives` (`periodType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_objectives_journeyId_periodType_isArchived` ON `objectives` (`journeyId`, `periodType`, `isArchived`)")

        createObjectiveCompletionsTable(db, "objective_completions")
        createObjectiveCompletionIndices(db)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `objective_skipped_cycles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `objectiveId` INTEGER NOT NULL,
                `periodStartMs` INTEGER NOT NULL,
                `periodEndMs` INTEGER NOT NULL,
                `skippedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_objective_skipped_cycles_objectiveId_periodStartMs_periodEndMs` ON `objective_skipped_cycles` (`objectiveId`, `periodStartMs`, `periodEndMs`)")
    }

    private fun createObjectiveCompletionsTable(db: SupportSQLiteDatabase, tableName: String) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `objectiveId` INTEGER NOT NULL,
                `journeyId` INTEGER NOT NULL,
                `journeyNameSnapshot` TEXT NOT NULL,
                `periodType` TEXT NOT NULL,
                `objectiveType` TEXT NOT NULL,
                `periodStartMs` INTEGER NOT NULL,
                `periodEndMs` INTEGER NOT NULL,
                `completedAt` INTEGER NOT NULL,
                `achievedDurationMs` INTEGER NOT NULL,
                `targetDurationMs` INTEGER NOT NULL,
                `baseRewardPearls` INTEGER NOT NULL,
                `streakBeforeCompletion` INTEGER NOT NULL,
                `streakMultiplier` REAL NOT NULL,
                `finalRewardPearls` INTEGER NOT NULL,
                `badgeKey` TEXT NOT NULL,
                `badgeLabelSnapshot` TEXT NOT NULL,
                `pearlsGranted` INTEGER NOT NULL DEFAULT 0,
                `pearlsClaimed` INTEGER NOT NULL DEFAULT 0,
                `pearlsClaimedAt` INTEGER,
                `badgeGranted` INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }

    private fun createObjectiveCompletionIndices(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_objective_completions_objectiveId_periodStartMs_periodEndMs` ON `objective_completions` (`objectiveId`, `periodStartMs`, `periodEndMs`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_objective_completions_journeyId` ON `objective_completions` (`journeyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_objective_completions_periodType` ON `objective_completions` (`periodType`)")
    }

    private fun migrateObjectiveCompletionsToClaimSchema(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "objective_completions")) {
            createObjectiveCompletionsTable(db, "objective_completions")
            createObjectiveCompletionIndices(db)
            return
        }

        val existingColumns = columns(db, "objective_completions")
        val pearlsGrantedExpression = if ("pearlsGranted" in existingColumns) "`pearlsGranted`" else "0"
        val pearlsClaimedExpression = if ("pearlsClaimed" in existingColumns) "`pearlsClaimed`" else pearlsGrantedExpression
        val pearlsClaimedAtExpression = if ("pearlsClaimedAt" in existingColumns) "`pearlsClaimedAt`" else "NULL"
        val badgeGrantedExpression = if ("badgeGranted" in existingColumns) "`badgeGranted`" else "1"

        val replacement = "objective_completions_claim_schema"
        db.execSQL("DROP TABLE IF EXISTS `$replacement`")
        createObjectiveCompletionsTable(db, replacement)
        db.execSQL(
            """
            INSERT INTO `$replacement` (
                `id`, `objectiveId`, `journeyId`, `journeyNameSnapshot`, `periodType`, `objectiveType`,
                `periodStartMs`, `periodEndMs`, `completedAt`, `achievedDurationMs`, `targetDurationMs`,
                `baseRewardPearls`, `streakBeforeCompletion`, `streakMultiplier`, `finalRewardPearls`,
                `badgeKey`, `badgeLabelSnapshot`, `pearlsGranted`, `pearlsClaimed`, `pearlsClaimedAt`, `badgeGranted`
            )
            SELECT
                `id`, `objectiveId`, `journeyId`, `journeyNameSnapshot`, `periodType`, `objectiveType`,
                `periodStartMs`, `periodEndMs`, `completedAt`, `achievedDurationMs`, `targetDurationMs`,
                `baseRewardPearls`, `streakBeforeCompletion`, `streakMultiplier`, `finalRewardPearls`,
                `badgeKey`, `badgeLabelSnapshot`, $pearlsGrantedExpression, $pearlsClaimedExpression, $pearlsClaimedAtExpression, $badgeGrantedExpression
            FROM `objective_completions`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `objective_completions`")
        db.execSQL("ALTER TABLE `$replacement` RENAME TO `objective_completions`")
        createObjectiveCompletionIndices(db)
    }

    private fun addCreatureEconomyFields(db: SupportSQLiteDatabase) {
        addColumnIfMissing(
            db = db,
            table = "user_shell_find_instance",
            column = "animalLevel",
            definition = "INTEGER NOT NULL DEFAULT 1"
        )
        addColumnIfMissing(
            db = db,
            table = "user_shell_find_instance",
            column = "creatureStatus",
            definition = "TEXT NOT NULL DEFAULT 'ACTIVE'"
        )
        addColumnIfMissing(
            db = db,
            table = "user_shell_find_instance",
            column = "creatureSource",
            definition = "TEXT"
        )
        addColumnIfMissing(
            db = db,
            table = "user_shell_find_instance",
            column = "flowTimeValueMinutes",
            definition = "INTEGER"
        )
    }

    private fun addColumnIfMissing(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
        definition: String
    ) {
        if (column !in columns(db, table)) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }
    }

    private fun normalizeTheBlueRoomIdentifiers(db: SupportSQLiteDatabase) {
        val importRoomKey = encodedTheBlueRoomImportKey()
        db.execSQL(
            "UPDATE `shell_placement` SET `roomId` = ? WHERE `roomId` = ?",
            arrayOf<Any?>("THE_BLUE", importRoomKey)
        )
        db.execSQL(
            "UPDATE `user_shell_room_state` SET `roomId` = ? WHERE `roomId` = ?",
            arrayOf<Any?>("THE_BLUE", importRoomKey)
        )
    }

    private fun encodedTheBlueRoomImportKey(): String = intArrayOf(67, 79, 82, 65, 76, 95, 82, 69, 69, 70)
        .joinToString(separator = "") { it.toChar().toString() }

    private fun migrateLegacyDatabaseTo15(db: SupportSQLiteDatabase) {
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

        createCurrentCoreIndices(db)
        createShellTables(db)
        createShellRewardEventTable(db)
        createObjectiveTables(db)

        db.execSQL("PRAGMA foreign_keys=ON")
    }

    // ------------------------------------------------------------------------
    // Core table rebuilds
    // ------------------------------------------------------------------------

    private fun rebuildTags(db: SupportSQLiteDatabase) {
        val oldTable = "tags"
        val newTable = "tags_v14"

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

        when {
            tableExists(db, oldTable) -> {
                val columns = columns(db, oldTable)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `$newTable` (`id`, `name`, `createdAt`)
                    SELECT
                        ${expr(columns, "id", "NULL")},
                        ${expr(columns, "name", "'Legacy'")},
                        ${expr(columns, "createdAt", nowSql())}
                    FROM `$oldTable`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `$oldTable`")
            }

            tableExists(db, "skills") -> {
                val columns = columns(db, "skills")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `$newTable` (`id`, `name`, `createdAt`)
                    SELECT
                        ${expr(columns, "id", "NULL")},
                        ${expr(columns, "name", "'Legacy'")},
                        ${expr(columns, "createdAt", nowSql())}
                    FROM `skills`
                    """.trimIndent()
                )
            }
        }

        db.execSQL(
            """
            INSERT OR IGNORE INTO `$newTable` (`id`, `name`, `createdAt`)
            VALUES (1, 'Flow', ${nowSql()})
            """.trimIndent()
        )

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildSessions(db: SupportSQLiteDatabase) {
        val oldTable = "sessions"
        val newTable = "sessions_v14"

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

            val title = when {
                "title" in columns -> "`title`"
                "notes" in columns -> "COALESCE(`notes`, 'Untitled Flow')"
                else -> "'Untitled Flow'"
            }

            val description = when {
                "description" in columns -> "`description`"
                "notes" in columns -> "COALESCE(`notes`, '')"
                else -> "''"
            }

            db.execSQL(
                """
                INSERT OR IGNORE INTO `$newTable` (
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
                    $title,
                    $description,
                    ${validTagExpr(columns)},
                    ${expr(columns, "startTime", nowSql())},
                    ${expr(columns, "endTime", nowSql())},
                    ${expr(columns, "durationMs", "0")},
                    ${expr(columns, "surgePlannedMs", "NULL")},
                    ${expr(columns, "surgePoints", "0")},
                    ${expr(columns, "scyraPoints", "0")},
                    ${expr(columns, "isSoftMode", "0")},
                    ${expr(columns, "arcId", "NULL")},
                    ${expr(columns, "arcIndex", "NULL")},
                    ${expr(columns, "arcMultiplierUsed", "NULL")},
                    ${expr(columns, "arcBonusPoints", "0")},
                    ${expr(columns, "createdAt", nowSql())}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildOngoingSession(db: SupportSQLiteDatabase) {
        val oldTable = "ongoing_session"
        val newTable = "ongoing_session_v14"

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
            val isInFlowMode = when {
                "isInFlowMode" in columns -> "`isInFlowMode`"
                "isInFocusMode" in columns -> "`isInFocusMode`"
                else -> "0"
            }

            db.execSQL(
                """
                INSERT OR REPLACE INTO `$newTable` (
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
                    ${expr(columns, "flowInstanceId", "'legacy-' || lower(hex(randomblob(16)))")},
                    ${expr(columns, "title", "''")},
                    ${expr(columns, "description", "''")},
                    ${expr(columns, "tagName", "''")},
                    $isInFlowMode,
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
                    ${expr(columns, "createdAt", nowSql())},
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
        val newTable = "flow_plans_v14"

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

        copyIfExists(
            db = db,
            sourceTable = oldTable,
            targetTable = newTable,
            columnsWithDefaults = listOf(
                "id" to "NULL",
                "title" to "'Untitled Flow'",
                "tagId" to "NULL",
                "isSoftMode" to "0",
                "targetMinutes" to "NULL",
                "launchWithSurge" to "0",
                "pinned" to "0",
                "archived" to "0",
                "launchCount" to "0",
                "lastLaunchedAt" to "NULL",
                "createdAt" to nowSql(),
                "updatedAt" to nowSql()
            ),
            sourceCleanup = true
        )

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildArcPlans(db: SupportSQLiteDatabase) {
        val oldTable = "arc_plans"
        val newTable = "arc_plans_v14"

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

        copyIfExists(
            db = db,
            sourceTable = oldTable,
            targetTable = newTable,
            columnsWithDefaults = listOf(
                "id" to "NULL",
                "title" to "'Untitled Arc'",
                "isInStudio" to "0",
                "archived" to "0",
                "launchCount" to "0",
                "lastLaunchedAt" to "NULL",
                "recurrenceType" to "'one_time'",
                "recurrenceDaysCsv" to "''",
                "createdAt" to nowSql(),
                "updatedAt" to nowSql()
            ),
            sourceCleanup = true
        )

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildArcPlanSteps(db: SupportSQLiteDatabase) {
        val oldTable = "arc_plan_steps"
        val newTable = "arc_plan_steps_v14"

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
                INSERT OR IGNORE INTO `$newTable` (
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
                    ${expr(columns, "createdAt", nowSql())},
                    ${expr(columns, "updatedAt", nowSql())}
                FROM `$oldTable`
                WHERE ${validArcPlanWhereExpr(columns)}
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    private fun rebuildActiveArcRun(db: SupportSQLiteDatabase) {
        val oldTable = "active_arc_run"
        val newTable = "active_arc_run_v14"

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
                INSERT OR REPLACE INTO `$newTable` (
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
                    ${expr(columns, "startedAt", nowSql())},
                    ${expr(columns, "updatedAt", nowSql())}
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
        val newTable = "pulses_v14"

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
                INSERT OR IGNORE INTO `$newTable` (
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
                    ${expr(columns, "createdAt", nowSql())},
                    ${expr(columns, "updatedAt", nowSql())}
                FROM `$oldTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$oldTable`")
        }

        db.execSQL("ALTER TABLE `$newTable` RENAME TO `$oldTable`")
    }

    // ------------------------------------------------------------------------
    // Shell tables
    // ------------------------------------------------------------------------

    private fun createShellTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pearl_ledger` (
                `id` TEXT NOT NULL,
                `delta` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceId` TEXT,
                `createdAt` INTEGER NOT NULL,
                `note` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_shell_find_instance` (
                `instanceId` TEXT NOT NULL,
                `findId` TEXT NOT NULL,
                `acquiredAt` INTEGER NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceId` TEXT,
                `currentUpgradeStageId` TEXT,
                `customName` TEXT,
                `isNew` INTEGER NOT NULL,
                `isArchivedInChest` INTEGER NOT NULL,
                `animalLevel` INTEGER NOT NULL DEFAULT 1,
                `creatureStatus` TEXT NOT NULL DEFAULT 'ACTIVE',
                `creatureSource` TEXT,
                `flowTimeValueMinutes` INTEGER,
                PRIMARY KEY(`instanceId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_findId` ON `user_shell_find_instance` (`findId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_sourceType` ON `user_shell_find_instance` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_shell_find_instance_sourceId` ON `user_shell_find_instance` (`sourceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_shell_find_stack` (
                `findId` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `firstAcquiredAt` INTEGER NOT NULL,
                `lastAcquiredAt` INTEGER NOT NULL,
                `isNew` INTEGER NOT NULL,
                PRIMARY KEY(`findId`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shell_placement` (
                `placementId` TEXT NOT NULL,
                `roomId` TEXT NOT NULL,
                `slotId` TEXT NOT NULL,
                `instanceId` TEXT NOT NULL,
                `placedAt` INTEGER NOT NULL,
                PRIMARY KEY(`placementId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_placement_roomId` ON `shell_placement` (`roomId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_placement_slotId` ON `shell_placement` (`slotId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shell_placement_roomId_slotId` ON `shell_placement` (`roomId`, `slotId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shell_placement_instanceId` ON `shell_placement` (`instanceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shell_find_upgrade` (
                `upgradeEventId` TEXT NOT NULL,
                `instanceId` TEXT NOT NULL,
                `fromStageId` TEXT,
                `toStageId` TEXT NOT NULL,
                `pearlCost` INTEGER NOT NULL,
                `upgradedAt` INTEGER NOT NULL,
                PRIMARY KEY(`upgradeEventId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_find_upgrade_instanceId` ON `shell_find_upgrade` (`instanceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_badge` (
                `badgeId` TEXT NOT NULL,
                `count` INTEGER NOT NULL,
                `firstEarnedAt` INTEGER NOT NULL,
                `lastEarnedAt` INTEGER NOT NULL,
                `isNew` INTEGER NOT NULL,
                PRIMARY KEY(`badgeId`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_discovery` (
                `userDiscoveryId` TEXT NOT NULL,
                `discoveryId` TEXT NOT NULL,
                `discoveredAt` INTEGER NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceId` TEXT,
                `grantedFindInstanceId` TEXT,
                `isNew` INTEGER NOT NULL,
                PRIMARY KEY(`userDiscoveryId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_discoveryId` ON `user_discovery` (`discoveryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_sourceType` ON `user_discovery` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_discovery_sourceId` ON `user_discovery` (`sourceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `stillwater_ledger` (
                `id` TEXT NOT NULL,
                `units` INTEGER NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceId` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stillwater_ledger_sourceType` ON `stillwater_ledger` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stillwater_ledger_sourceId` ON `stillwater_ledger` (`sourceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `stillwater_preference` (
                `id` INTEGER NOT NULL,
                `perspective` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_shell_room_state` (
                `roomId` TEXT NOT NULL,
                `firstOpenedAt` INTEGER,
                `lastOpenedAt` INTEGER,
                `visualMaturityScore` INTEGER NOT NULL,
                `ambientLifeScore` INTEGER NOT NULL,
                `lastChangedAt` INTEGER,
                PRIMARY KEY(`roomId`)
            )
            """.trimIndent()
        )
    }


    private fun createShellRewardEventTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shell_reward_event` (
                `id` TEXT NOT NULL,
                `sourceSessionId` INTEGER NOT NULL,
                `arcId` INTEGER,
                `rewardType` TEXT NOT NULL,
                `rewardId` TEXT,
                `quantity` INTEGER NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_reward_event_sourceSessionId` ON `shell_reward_event` (`sourceSessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_reward_event_arcId` ON `shell_reward_event` (`arcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shell_reward_event_rewardType` ON `shell_reward_event` (`rewardType`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shell_reward_event_sourceSessionId_rewardType_rewardId` ON `shell_reward_event` (`sourceSessionId`, `rewardType`, `rewardId`)")
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

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private fun copyIfExists(
        db: SupportSQLiteDatabase,
        sourceTable: String,
        targetTable: String,
        columnsWithDefaults: List<Pair<String, String>>,
        sourceCleanup: Boolean
    ) {
        if (!tableExists(db, sourceTable)) return

        val sourceColumns = columns(db, sourceTable)
        val targetColumns = columnsWithDefaults.joinToString(", ") { "`${it.first}`" }
        val selectExpressions = columnsWithDefaults.joinToString(", ") { (column, default) ->
            selectExpr(sourceColumns, column, default)
        }

        db.execSQL(
            """
            INSERT OR IGNORE INTO `$targetTable` ($targetColumns)
            SELECT $selectExpressions
            FROM `$sourceTable`
            """.trimIndent()
        )

        if (sourceCleanup) {
            db.execSQL("DROP TABLE `$sourceTable`")
        }
    }

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

    private fun selectExpr(sourceColumns: Set<String>, column: String, default: String): String {
        return if (column in sourceColumns) "`$column`" else default
    }

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
                INSERT INTO `tags` (`id`, `name`, `createdAt`)
                VALUES (1, 'Legacy', ${nowSql()})
                """.trimIndent()
            )
        }
    }

    private fun validTagExpr(columns: Set<String>): String {
        return when {
            "tagId" in columns -> {
                """
                CASE
                    WHEN `tagId` IN (SELECT `id` FROM `tags`) THEN `tagId`
                    ELSE (SELECT `id` FROM `tags` ORDER BY `id` ASC LIMIT 1)
                END
                """.trimIndent()
            }

            "skillId" in columns -> {
                """
                CASE
                    WHEN `skillId` IN (SELECT `id` FROM `tags`) THEN `skillId`
                    ELSE (SELECT `id` FROM `tags` ORDER BY `id` ASC LIMIT 1)
                END
                """.trimIndent()
            }

            else -> "(SELECT `id` FROM `tags` ORDER BY `id` ASC LIMIT 1)"
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
            "`$columnName` IN (SELECT `id` FROM `arc_plans`)"
        } else {
            "EXISTS (SELECT 1 FROM `arc_plans` LIMIT 1)"
        }
    }

    private fun nowSql(): String = "CAST(strftime('%s','now') AS INTEGER) * 1000"
}