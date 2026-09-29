package com.kingkharnivore.skillz.domain.land

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LandMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SkillzDatabase::class.java)
    @Test fun shipped40To41PreservesExistingRecordsAndCreatesEmptyJournal() {
        val name="land-migration"
        helper.createDatabase(name,40).apply {
            execSQL("INSERT INTO pearl_ledger VALUES ('p',12345,'flow','session','1',1,NULL)")
            execSQL("INSERT INTO stillwater_ledger VALUES ('s',678,'session','2',1)")
            execSQL("INSERT INTO user_shell_find_instance (instanceId,findId,acquiredAt,sourceType,sourceId,currentUpgradeStageId,customName,isNew,isArchivedInChest,animalLevel,creatureStatus,creatureSource,flowTimeValueMinutes) VALUES ('sea','focus_whale',1,'session','1',NULL,NULL,0,1,99,'ACTIVE','FLOW_EARNED',120)")
            execSQL("INSERT INTO creature_mastery_event VALUES ('mastery:sea','sea','focus_whale',2,'grow:sea')")
            execSQL("INSERT INTO tags (id,name,createdAt) VALUES (1,'History',1)")
            execSQL("INSERT INTO sessions (id,title,description,tagId,startTime,endTime,durationMs,surgePoints,scyraPoints,isSoftMode,arcId,arcIndex,arcBonusPoints,createdAt) VALUES (1,'Flow','',1,1,2,1,0,1,0,42,1,0,1)")
            close()
        }
        helper.runMigrationsAndValidate(name,41,true,SkillzDatabaseMigrations.MIGRATION_40_41).use { db ->
            db.query("SELECT animalLevel,creatureStatus,findId FROM user_shell_find_instance WHERE instanceId='sea'").use { assertTrue(it.moveToFirst());assertEquals(99,it.getInt(0));assertEquals("ACTIVE",it.getString(1));assertEquals("focus_whale",it.getString(2)) }
            listOf("pearl_ledger" to 12345L,"stillwater_ledger" to 678L).forEach { (table,value) ->
                db.query("SELECT ${if(table=="pearl_ledger") "delta" else "units"} FROM $table").use { assertTrue(it.moveToFirst());assertEquals(value,it.getLong(0)) }
            }
            listOf("sessions","creature_mastery_event").forEach { table -> db.query("SELECT COUNT(*) FROM $table").use { assertTrue(it.moveToFirst());assertEquals(1,it.getInt(0)) } }
            db.query("SELECT COUNT(*) FROM arc_land_reward").use { assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0)) }
        }
    }
}
