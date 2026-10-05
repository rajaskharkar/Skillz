package com.kingkharnivore.skillz.domain.power

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PowerMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),SkillzDatabase::class.java)
    @Test fun released42To44PreservesHistoryAndNeverBackfillsRewards() {
        val name="power-migration-42"
        helper.createDatabase(name,42).apply {
            execSQL("INSERT INTO tags (id,name,createdAt) VALUES (1,'Journey',101)")
            listOf(0,1).forEach { soft -> execSQL("INSERT INTO sessions (id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcId,arcIndex,arcMultiplierUsed,arcBonusPoints,createdAt,activeIntervalJson) VALUES (${soft+1},'Flow','memo',1,101,60101,60000,9,42,$soft,17,${soft+1},1.2,7,60101,'101-60101')") }
            execSQL("INSERT INTO chronicles VALUES ('c','SESSION','1','notes',101,60101)")
            execSQL("INSERT INTO chronicle_moments (id,chronicleId,type,position,text,transcriptEdited,createdAt,updatedAt) VALUES ('m','c','MEDIA',0,'caption',0,101,60101)")
            execSQL("INSERT INTO chronicle_media_items (id,momentId,position,localPath,mimeType,createdAt) VALUES ('photo','m',0,'chronicle/photo.jpg','image/jpeg',101)")
            execSQL("INSERT INTO pearl_ledger VALUES ('old-pearl',123,'flow_reward','session','1',60101,NULL)")
            close()
        }
        helper.runMigrationsAndValidate(name,44,true,SkillzDatabaseMigrations.MIGRATION_42_44).use { db ->
            db.query("SELECT isSoftMode,tagId,startTime,endTime,durationMs,scyraPoints,arcId,arcIndex,arcMultiplierUsed,activeIntervalJson FROM sessions ORDER BY id").use { rows ->
                repeat(2) { soft -> assertTrue(rows.moveToNext());assertEquals(soft,rows.getInt(0));assertEquals(1,rows.getInt(1));assertEquals(101,rows.getInt(2));assertEquals(60101,rows.getInt(3));assertEquals(60000,rows.getInt(4));assertEquals(42,rows.getInt(5));assertEquals(17,rows.getInt(6));assertEquals(soft+1,rows.getInt(7));assertEquals(1.2,rows.getDouble(8),0.0);assertEquals("101-60101",rows.getString(9)) }
            }
            db.query("SELECT c.ownerKey,c.draftText,i.localPath,i.mimeType FROM chronicles c JOIN chronicle_moments m ON m.chronicleId=c.id JOIN chronicle_media_items i ON i.momentId=m.id").use { assertTrue(it.moveToFirst());assertEquals("1",it.getString(0));assertEquals("notes",it.getString(1));assertEquals("chronicle/photo.jpg",it.getString(2));assertEquals("image/jpeg",it.getString(3)) }
            listOf("pebble_ledger","session_shell_reward").forEach { table -> db.query("SELECT COUNT(*) FROM $table").use { assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0)) } }
            db.query("SELECT SUM(delta) FROM pearl_ledger").use { assertTrue(it.moveToFirst());assertEquals(123,it.getInt(0)) }
        }
    }
    @Test fun intermediate43RepairPreservesAllDependentsAndModeWithoutReset() {
        val name="power-repair-43"
        val tables=listOf("tags","session_creations","pulses","pulse_flow_links","flow_health_snapshots","flow_reward_breakdowns")
        fun rows(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<List<String?>> = db.query("SELECT * FROM `$table`").use { cursor ->
            buildList { while(cursor.moveToNext()) add((0 until cursor.columnCount).map { if(cursor.isNull(it)) null else cursor.getString(it) }) }
        }
        val before = helper.createDatabase(name,43).use { db ->
            db.apply {
            execSQL("INSERT INTO tags (`id`,`name`,`createdAt`) VALUES (1,'fixture',1)")
            execSQL("INSERT INTO sessions (`id`,`title`,`description`,`tagId`,`startTime`,`endTime`,`durationMs`,`surgePlannedMs`,`surgePoints`,`scyraPoints`,`isSoftMode`,`arcId`,`arcIndex`,`arcMultiplierUsed`,`arcBonusPoints`,`createdAt`,`activeIntervalJson`) VALUES (1,'fixture','fixture',1,1,1,1,NULL,1,1,'POWER',NULL,NULL,NULL,1,1,NULL)")
            execSQL("INSERT INTO session_creations (`flowInstanceId`,`sessionId`,`createdAt`) VALUES ('fixture',1,1)")
            execSQL("INSERT INTO pulses (`id`,`title`,`description`,`tagId`,`parentSessionId`,`parentFlowInstanceId`,`arcId`,`createdAt`,`updatedAt`,`groveStatus`,`groveStatusChangedAt`) VALUES (1,'fixture','fixture',1,1,NULL,NULL,1,1,'ALIVE',NULL)")
            execSQL("INSERT INTO pulse_flow_links (`id`,`pulseId`,`sessionId`,`linkedAt`) VALUES (1,1,1,1)")
            execSQL("INSERT INTO flow_health_snapshots (`sessionId`,`healthEnabledAtStart`,`permissionGrantedAtStart`,`status`,`steps`,`rawMovementPoints`,`finalMovementScyraContribution`,`finalMovementPearlContribution`,`firstCheckedAtMs`,`lastCheckedAtMs`,`capturedAtMs`,`expiresAtMs`,`checkCount`,`flowStartTimeMs`,`flowEndTimeMs`,`activeIntervalJson`,`sourceLabel`,`updatedAfterSync`) VALUES (1,1,1,'CAPTURED',NULL,1,1,1,NULL,NULL,NULL,NULL,1,1,1,NULL,NULL,1)")
            execSQL("INSERT INTO flow_reward_breakdowns (`sessionId`,`nonMovementPreMultiplierPoints`,`pulseBonusPoints`,`surgeBonusPoints`,`otherPreMultiplierBonusPoints`,`movementPoints`,`preMultiplierTotal`,`arcMultiplier`,`streakMultiplier`,`otherMultiplier`,`arcBonusPoints`,`finalScyraPoints`,`pearlsEarned`,`pearlEligible`,`roundingMode`) VALUES (1,1,1,1,1,1,1,1,1,1,1,1,1,1,'HALF_UP')")
            execSQL("INSERT INTO ongoing_session (`id`,`flowInstanceId`,`title`,`description`,`tagName`,`isInFlowMode`,`isRunning`,`isSoftMode`,`baseStartTimeMs`,`accumulatedBeforeStartMs`,`isSurgeOn`,`surgePlannedMs`,`surgeMilestonesFiredCsv`,`surgeTargetReached`,`surgeTargetReachedAtMs`,`surgeFinalCountdownStarted`,`createdAt`,`arcId`,`arcChainBase`,`arcSessionCountInArc`,`arcLastSessionEndTimeMs`,`originPulseId`,`originPulseTitleSnapshot`,`originPulseJourneyNameSnapshot`,`healthEnabledAtStart`,`healthPermissionGrantedAtStart`,`movementBonusEligibleAtStart`,`activeIntervalJson`) VALUES (1,'fixture','fixture','fixture','fixture',1,1,'POWER',NULL,1,1,NULL,'fixture',1,NULL,1,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,NULL)")
            }
            tables.associateWith { rows(db,it) }
        }
        helper.runMigrationsAndValidate(name,44,true,SkillzDatabaseMigrations.MIGRATION_43_44).use { db ->
            tables.forEach { assertEquals(it,before[it],rows(db,it)) }
            listOf("sessions","ongoing_session").forEach { table ->
                db.query("SELECT isSoftMode FROM `$table`").use { assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0)) }
            }
            db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
    }

    @Test fun unknownIntermediateModeRollsBackWithoutReclassifyingOrDeletingHistory() {
        val name = "power-repair-unknown"
        helper.createDatabase(name,43).use { db ->
            db.execSQL("INSERT INTO tags (id,name,createdAt) VALUES (1,'Journey',101)")
            db.execSQL("INSERT INTO sessions (id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt) VALUES (7,'Preserve me','memo',1,101,60101,60000,9,42,'UNKNOWN',0,60101)")
        }
        assertTrue(runCatching {
            helper.runMigrationsAndValidate(name,44,true,SkillzDatabaseMigrations.MIGRATION_43_44).close()
        }.isFailure)
        val file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(name)
        android.database.sqlite.SQLiteDatabase.openDatabase(file.path,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(43,db.version)
            db.rawQuery("SELECT id,title,isSoftMode,scyraPoints FROM sessions",null).use {
                assertTrue(it.moveToFirst()); assertEquals(7,it.getInt(0)); assertEquals("Preserve me",it.getString(1))
                assertEquals("UNKNOWN",it.getString(2)); assertEquals(42,it.getInt(3)); assertFalse(it.moveToNext())
            }
        }
    }

}
