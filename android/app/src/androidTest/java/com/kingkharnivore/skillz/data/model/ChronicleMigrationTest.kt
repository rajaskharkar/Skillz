package com.kingkharnivore.skillz.data.model

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChronicleMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), SkillzDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun migrationPreservesExactLegacyChroniclesAsCommittedMoments() {
        val exact = "  First line\nकथा 🐢  "
        helper.createDatabase("chronicle-migration", 38).apply {
            execSQL("INSERT INTO tags(id,name,createdAt) VALUES(1,'Journey',1)")
            execSQL("INSERT INTO sessions(id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt) VALUES(1,'Flow',?,1,1,2,1,0,0,0,0,3)", arrayOf(exact))
            listOf("", "   ", "\t", "\n", "\r\n").forEachIndexed { index, blank ->
                execSQL("INSERT INTO sessions(id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt) VALUES(?, 'Blank',?,1,1,2,1,0,0,0,0,3)", arrayOf<Any?>(index + 10, blank))
            }
            execSQL("INSERT INTO pulses(id,title,description,createdAt,updatedAt,groveStatus) VALUES(2,'Pulse',?,3,3,'ALIVE')", arrayOf(exact))
            execSQL("INSERT INTO ongoing_session(id,flowInstanceId,title,description,tagName,isInFlowMode,isRunning,isSoftMode,accumulatedBeforeStartMs,isSurgeOn,surgeMilestonesFiredCsv,surgeTargetReached,surgeFinalCountdownStarted,createdAt,healthEnabledAtStart,healthPermissionGrantedAtStart,movementBonusEligibleAtStart) VALUES(1,'active','Flow',?,'Journey',1,0,0,0,0,'',0,0,3,0,0,0)", arrayOf(exact))
            close()
        }
        helper.runMigrationsAndValidate("chronicle-migration", 39, true, SkillzDatabaseMigrations.MIGRATION_38_39).apply {
            // Migration is additive: the exact legacy payload remains available as a
            // rollback/recovery source even after Chronicle has copied it.
            query("SELECT title,description,tagId,createdAt FROM sessions WHERE id=1").use {
                assertTrue(it.moveToFirst())
                assertEquals("Flow", it.getString(0))
                assertEquals(exact, it.getString(1))
                assertEquals(1L, it.getLong(2))
                assertEquals(3L, it.getLong(3))
            }
            val expectedBlanks = listOf("", "   ", "\t", "\n", "\r\n")
            query("SELECT description FROM sessions WHERE id >= 10 ORDER BY id").use {
                expectedBlanks.forEach { expected ->
                    assertTrue(it.moveToNext())
                    assertEquals(expected, it.getString(0))
                }
                assertFalse(it.moveToNext())
            }
            query("SELECT title,description,createdAt,updatedAt FROM pulses WHERE id=2").use {
                assertTrue(it.moveToFirst())
                assertEquals("Pulse", it.getString(0))
                assertEquals(exact, it.getString(1))
                assertEquals(3L, it.getLong(2))
                assertEquals(3L, it.getLong(3))
            }
            query("SELECT flowInstanceId,title,description,tagName,createdAt FROM ongoing_session WHERE id=1").use {
                assertTrue(it.moveToFirst())
                assertEquals("active", it.getString(0))
                assertEquals("Flow", it.getString(1))
                assertEquals(exact, it.getString(2))
                assertEquals("Journey", it.getString(3))
                assertEquals(3L, it.getLong(4))
            }
            query("SELECT COUNT(*) FROM sessions").use { it.moveToFirst(); assertEquals(6, it.getInt(0)) }
            query("SELECT COUNT(*) FROM pulses").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            query("SELECT COUNT(*) FROM ongoing_session").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }

            query("SELECT draftText FROM chronicles WHERE ownerType='ACTIVE_FLOW'").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
            query("SELECT ownerType,ownerKey,id FROM chronicles ORDER BY ownerType").use {
                val owners = mutableSetOf<Pair<String,String>>()
                while (it.moveToNext()) owners += it.getString(0) to it.getString(1)
                assertEquals(setOf("ACTIVE_FLOW" to "active", "PULSE" to "2", "SESSION" to "1"), owners)
            }
            query("SELECT text,position FROM chronicle_moments ORDER BY id").use {
                var count=0; while(it.moveToNext()) { assertEquals(exact,it.getString(0)); assertEquals(0,it.getInt(1)); count++ }
                assertEquals(3,count)
            }
            query("SELECT COUNT(*) FROM chronicles").use { it.moveToFirst(); assertEquals(3, it.getInt(0)) }
            query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            close()
        }
    }

    @Test fun migrationFrom37Through40PreservesHistoryUsingTheRegisteredChain() {
        val exact = "Legacy Flow — déjà vu 🌱"
        helper.createDatabase("chronicle-migration-chain", 37).apply {
            execSQL("INSERT INTO tags(id,name,createdAt) VALUES(7,'Earlier Journey',11)")
            execSQL(
                "INSERT INTO sessions(id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt) VALUES(9,'Earlier Flow',?,7,12,34,22,5,8,1,3,44)",
                arrayOf(exact)
            )
            close()
        }

        helper.runMigrationsAndValidate(
            "chronicle-migration-chain",
            40,
            true,
            *SkillzDatabaseMigrations.ALL_MIGRATIONS
        ).apply {
            query("SELECT title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt FROM sessions WHERE id=9").use {
                assertTrue(it.moveToFirst())
                assertEquals("Earlier Flow", it.getString(0))
                assertEquals(exact, it.getString(1))
                assertEquals(7L, it.getLong(2))
                assertEquals(12L, it.getLong(3))
                assertEquals(34L, it.getLong(4))
                assertEquals(22L, it.getLong(5))
                assertEquals(5, it.getInt(6))
                assertEquals(8, it.getInt(7))
                assertEquals(1, it.getInt(8))
                assertEquals(3, it.getInt(9))
                assertEquals(44L, it.getLong(10))
            }
            query("SELECT c.ownerType,c.ownerKey,m.text,m.position FROM chronicles c JOIN chronicle_moments m ON m.chronicleId=c.id").use {
                assertTrue(it.moveToFirst())
                assertEquals("SESSION", it.getString(0))
                assertEquals("9", it.getString(1))
                assertEquals(exact, it.getString(2))
                assertEquals(0, it.getInt(3))
                assertFalse(it.moveToNext())
            }
            query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            close()
        }
    }
}
