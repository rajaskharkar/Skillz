package com.kingkharnivore.skillz.domain.green

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.migration.SkillzDatabaseMigrations
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import org.junit.*
import org.junit.Assert.*

class GreenMigrationTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),SkillzDatabase::class.java)
    @Test fun everyExportedProductionSchemaPreservesDropsCopiesLevelsAndHistory() {
        // v31 is the oldest exported schema. Earlier upgrades share the existing legacy rebuild path.
        (31..46).forEach { version ->
            val name="green-migration-$version"
            helper.createDatabase(name,version).use { db ->
                db.execSQL("INSERT INTO stillwater_ledger VALUES ('earned',876543,'soft_flow','flow',100)")
                db.execSQL("INSERT INTO stillwater_ledger VALUES ('spent',-15000,'stillwater_draw','legacy-0',101)")
                db.execSQL("INSERT INTO stillwater_preference VALUES (1,'LAND',102)")
                listOf("stillwater_shrimp","stillwater_coelacanth","creature_peacock","creature_panda").forEachIndexed { index,id ->
                    repeat(2) { copy ->
                        db.execSQL("INSERT INTO user_shell_find_instance (instanceId,findId,acquiredAt,sourceType,sourceId,currentUpgradeStageId,customName,isNew,isArchivedInChest,animalLevel,creatureStatus,creatureSource,flowTimeValueMinutes) VALUES ('legacy-$index-$copy','$id',100,'stillwater','history','advanced-legacy','Keep me',0,1,${if(copy==0) 99 else 62},'${if(copy==0) "ACTIVE" else "RELEASED"}','STILLWATER',250)")
                    }
                }
                db.execSQL("INSERT INTO user_badge (badgeId,count,firstEarnedAt,lastEarnedAt,isNew) VALUES ('stillwater_mastery',4,100,101,0)")
            }
            helper.runMigrationsAndValidate(name,47,true,*SkillzDatabaseMigrations.ALL_MIGRATIONS).use { db ->
                // Running the additive migration again is safe and does not mutate historic data.
                SkillzDatabaseMigrations.MIGRATION_46_47.migrate(db)
                db.query("SELECT SUM(units) FROM stillwater_ledger").use { assertTrue(it.moveToFirst()); assertEquals(861543L,it.getLong(0)) }
                db.query("SELECT findId,animalLevel,currentUpgradeStageId,customName,creatureStatus FROM user_shell_find_instance ORDER BY instanceId").use {
                    var count=0
                    while(it.moveToNext()) {
                        assertEquals(CreatureSourceType.BEYOND_BLUE,CreatureCatalog.require(it.getString(0)).sourceType)
                        assertEquals(if(count%2==0) 99 else 62,it.getInt(1));assertEquals("advanced-legacy",it.getString(2));assertEquals("Keep me",it.getString(3))
                        assertEquals(if(count%2==0) "ACTIVE" else "RELEASED",it.getString(4));count++
                    }
                    assertEquals(8,count)
                }
                db.query("SELECT count FROM user_badge WHERE badgeId='stillwater_mastery'").use { assertTrue(it.moveToFirst());assertEquals(4,it.getInt(0)) }
                listOf("green_specimen","green_badge_award","green_action","green_event").forEach { table -> db.query("SELECT COUNT(*) FROM $table").use { assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0)) } }
                db.query("SELECT perspective FROM stillwater_preference").use { assertTrue(it.moveToFirst());assertEquals("LAND",it.getString(0)) }
            }
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            val reopened = Room.databaseBuilder(context,SkillzDatabase::class.java,name).addMigrations(*SkillzDatabaseMigrations.ALL_MIGRATIONS).build()
            try { assertTrue(reopened.openHelper.writableDatabase.isOpen) } finally { reopened.close() }
            context.deleteDatabase(name)
        }
    }
    @Test fun version46PreservesEveryLegacyTableByteForByteIncludingMasteryAndAdvancedState() {
        val name="green-exact-preservation"
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        fun rows(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String) = db.query("SELECT * FROM `$table` ORDER BY rowid").use { c ->
            buildList { while(c.moveToNext()) add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) }) }
        }
        val before=helper.createDatabase(name,46).use { db ->
            db.execSQL("INSERT INTO creature_mastery_event VALUES ('m','copy','stillwater_shrimp',111,'water')")
            db.execSQL("INSERT INTO collection_completion VALUES ('c','collection_stillwater','COMPLETIONIST',112,'EXACT',1,'frozen','stillwater_shrimp')")
            db.execSQL("INSERT INTO stillwater_ledger VALUES ('drops',7654321,'soft_flow','flow',100)")
            db.execSQL("INSERT INTO badge_pin VALUES ('stillwater_mastery',0,140)")
            db.execSQL("INSERT INTO badge_tracking VALUES ('stillwater_variety',141)")
            db.execSQL("INSERT INTO user_badge VALUES ('stillwater_mastery',7,111,120,0,135,'EXACT')")
            db.execSQL("INSERT INTO stillwater_preference VALUES (1,'LAND',102)")
            db.execSQL("INSERT INTO user_shell_find_instance (instanceId,findId,acquiredAt,sourceType,sourceId,currentUpgradeStageId,customName,isNew,isArchivedInChest,animalLevel,creatureStatus,creatureSource,flowTimeValueMinutes) VALUES ('copy','stillwater_shrimp',100,'stillwater','history','advanced-legacy','Keep me',0,1,99,'ACTIVE','STILLWATER',250)")
            val tables = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table' AND name != 'android_metadata'").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            tables.associateWith { rows(db,it) }
        }
        helper.runMigrationsAndValidate(name,47,true,SkillzDatabaseMigrations.MIGRATION_46_47).use { db -> before.forEach { (table,values) -> assertEquals(values,rows(db,table)) } }
        context.deleteDatabase(name)
    }
}
