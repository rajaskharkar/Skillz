package com.kingkharnivore.skillz.data.model

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.assertEquals
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
            execSQL("INSERT INTO tags(id,name) VALUES(1,'Journey')")
            execSQL("INSERT INTO sessions(id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcBonusPoints,createdAt) VALUES(1,'Flow',?,1,1,2,1,0,0,0,0,3)", arrayOf(exact))
            execSQL("INSERT INTO pulses(id,title,description,createdAt,updatedAt,groveStatus) VALUES(2,'Pulse',?,3,3,'ALIVE')", arrayOf(exact))
            execSQL("INSERT INTO ongoing_session(id,flowInstanceId,title,description,tagName,isInFlowMode,isRunning,isSoftMode,accumulatedBeforeStartMs,isSurgeOn,surgeMilestonesFiredCsv,surgeTargetReached,surgeFinalCountdownStarted,createdAt,healthEnabledAtStart,healthPermissionGrantedAtStart,movementBonusEligibleAtStart) VALUES(1,'active','Flow',?,'Journey',1,0,0,0,0,'',0,0,3,0,0,0)", arrayOf(exact))
            close()
        }
        helper.runMigrationsAndValidate("chronicle-migration", 39, true, SkillzDatabaseMigrations.MIGRATION_38_39).apply {
            query("SELECT draftText FROM chronicles WHERE ownerType='ACTIVE_FLOW'").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
            query("SELECT text,position FROM chronicle_moments ORDER BY id").use {
                var count=0; while(it.moveToNext()) { assertEquals(exact,it.getString(0)); assertEquals(0,it.getInt(1)); count++ }
                assertEquals(3,count)
            }
            close()
        }
    }
}
