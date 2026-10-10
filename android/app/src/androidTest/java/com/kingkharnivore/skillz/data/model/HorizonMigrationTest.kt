package com.kingkharnivore.skillz.data.model

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HorizonMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SkillzDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory())
    private fun snapshot(db: SupportSQLiteDatabase, table: String, columns: String = "*") = db.query("SELECT $columns FROM $table ORDER BY id").use { c ->
        buildList { while(c.moveToNext()) add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) }) }
    }
    @Test fun modeMigrationPreservesRegularSoftAndArchivedPlansAndArcSteps() {
        val name = "horizon-mode-migration"
        val columns = "id,title,tagId,isSoftMode,targetMinutes,launchWithSurge,pinned,archived,launchCount,lastLaunchedAt,createdAt,updatedAt,kind,completedAt"
        val before: List<List<String?>>
        helper.createDatabase(name, 45).apply {
            execSQL("INSERT INTO flow_plans($columns) VALUES(10,'Read',NULL,0,25,1,1,0,15,99,1,100,'HABIT',NULL)")
            execSQL("INSERT INTO flow_plans($columns) VALUES(11,'Walk',NULL,1,NULL,0,0,1,3,90,2,101,'HABIT',NULL)")
            execSQL("INSERT INTO flow_plans($columns) VALUES(12,'Done',NULL,0,NULL,0,0,0,1,90,2,101,'PLAN',101)")
            execSQL("INSERT INTO arc_plans(id,title,isInStudio,archived,launchCount,recurrenceType,recurrenceDaysCsv,createdAt,updatedAt) VALUES(3,'Morning',0,0,0,'daily','',1,1)")
            execSQL("INSERT INTO arc_plan_steps(id,arcPlanId,orderIndex,sourceFlowPlanId,titleSnapshot,isSoftModeSnapshot,launchWithSurgeSnapshot,linkState,createdAt,updatedAt) VALUES(4,3,0,10,'Read',0,0,'linked',1,1)")
            execSQL("INSERT INTO arc_plan_steps(id,arcPlanId,orderIndex,sourceFlowPlanId,titleSnapshot,isSoftModeSnapshot,launchWithSurgeSnapshot,linkState,createdAt,updatedAt) VALUES(5,3,1,11,'Walk',1,0,'linked',1,1)")
            before = snapshot(this, "flow_plans")
            close()
        }
        helper.runMigrationsAndValidate(name,46,true,SkillzDatabaseMigrations.MIGRATION_45_46).apply {
            assertEquals(before, snapshot(this,"flow_plans",columns))
            query("SELECT mode FROM flow_plans ORDER BY id").use { c ->
                listOf(0,1,0).forEach { expected -> assertTrue(c.moveToNext()); assertEquals(expected,c.getInt(0)) }
            }
            query("SELECT mode FROM arc_plan_steps ORDER BY id").use { c ->
                listOf(0,1).forEach { expected -> assertTrue(c.moveToNext()); assertEquals(expected,c.getInt(0)) }
            }
            query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            close()
        }
    }

    @Test fun existingPlansDreamsAndLegacyArcsArePreservedExactly() {
        val name = "horizon-migration"
        val columns = "id,title,tagId,isSoftMode,targetMinutes,launchWithSurge,pinned,archived,launchCount,lastLaunchedAt,createdAt,updatedAt"
        val before: Map<String,List<List<String?>>>
        helper.createDatabase(name, 44).apply {
            execSQL("INSERT INTO tags(id,name,createdAt) VALUES(7,'Learning',1)")
            execSQL("INSERT INTO flow_plans($columns) VALUES(10,'Read',7,0,25,1,1,0,15,99,1,100)")
            execSQL("INSERT INTO flow_plans($columns) VALUES(11,'Walk',7,1,NULL,0,0,1,3,90,2,101)")
            execSQL("INSERT INTO arc_plans(id,title,isInStudio,archived,launchCount,lastLaunchedAt,recurrenceType,recurrenceDaysCsv,createdAt,updatedAt) VALUES(3,'Morning',1,0,9,90,'custom','1,3,5',1,90)")
            execSQL("INSERT INTO arc_plan_steps(id,arcPlanId,orderIndex,sourceFlowPlanId,titleSnapshot,tagIdSnapshot,isSoftModeSnapshot,targetMinutesSnapshot,launchWithSurgeSnapshot,linkState,createdAt,updatedAt) VALUES(4,3,0,10,'Read',7,0,25,1,'linked',1,90)")
            execSQL("INSERT INTO active_arc_run(id,arcPlanId,arcTitle,currentStepIndex,totalSteps,currentStepTitle,currentTagName,currentIsSoftMode,startedAt,updatedAt) VALUES(1,3,'Morning',0,1,'Read','Learning',0,90,91)")
            before = listOf("flow_plans", "tags", "arc_plans", "arc_plan_steps", "active_arc_run").associateWith { snapshot(this,it) }
            close()
        }
        helper.runMigrationsAndValidate(name,45,true,SkillzDatabaseMigrations.MIGRATION_44_45).apply {
            before.forEach { (table, rows) -> assertEquals(table, rows, snapshot(this, table, if(table=="flow_plans") columns else "*")) }
            query("SELECT kind,completedAt FROM flow_plans").use { c -> while(c.moveToNext()) { assertEquals("HABIT",c.getString(0)); assertTrue(c.isNull(1)) } }
            query("SELECT COUNT(*) FROM horizon_memories").use { assertTrue(it.moveToFirst()); assertEquals(0,it.getInt(0)) }
            query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            close()
        }
    }
}
