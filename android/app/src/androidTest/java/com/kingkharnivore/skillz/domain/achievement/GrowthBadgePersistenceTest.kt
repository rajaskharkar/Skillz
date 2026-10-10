package com.kingkharnivore.skillz.domain.achievement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.utils.shell.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class GrowthBadgePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "growth-${UUID.randomUUID()}"
    private lateinit var db: SkillzDatabase
    private fun open() { db=Room.databaseBuilder(context,SkillzDatabase::class.java,name).build() }
    private fun shell() = ShellRepository(db,db.sessionDao(),db.pearlLedgerDao(),db.shellFindInstanceDao(),
        db.shellFindStackDao(),db.shellPlacementDao(),db.shellFindUpgradeDao(),db.userBadgeDao(),
        db.userDiscoveryDao(),db.stillwaterLedgerDao(),db.stillwaterPreferenceDao(),db.userShellRoomStateDao(),db.objectiveCompletionDao(),db.achievementDao())
    @Before fun before() = open()
    @After fun after() { db.close();context.deleteDatabase(name) }
    private fun creature(id: String, species: String, level: Int) = UserShellFindInstanceEntity(id,species,100,"fixture",null,null,null,false,true,animalLevel=level)

    @Test fun thresholdAwardsCommitWithGrowthAndRetryDoesNotSpendOrAwardTwice() = runBlocking {
        db.pearlLedgerDao().insert(PearlLedgerEntity("funds",1_000_000,"fixture","fixture",null,1,null))
        CreatureRealm.entries.forEach { realm ->
            val spec=CreatureGrowthBadges.byId.getValue("growth_v1_${realm.name.lowercase()}_hello")
            val row=creature(realm.name,spec.species.first(),4)
            db.shellFindInstanceDao().insert(row)
            val result=shell().growCreature(row.instanceId,"grow-${realm.name}")
            assertEquals(5,result.resultingLevel)
            assertTrue(result.changes.any { it.badgeId==spec.id && it.type==AchievementChangeType.BADGE_NEWLY_EARNED })
            val award=db.userBadgeDao().get(spec.id)!!
            assertEquals(1,award.count);assertEquals(AchievementTimestampConfidence.EXACT,award.timestampConfidence)
            val balance=db.pearlLedgerDao().getBalance()
            shell().growCreature(row.instanceId,"grow-${realm.name}")
            assertEquals(balance,db.pearlLedgerDao().getBalance());assertEquals(award,db.userBadgeDao().get(spec.id))
        }
        db.close();open()
        assertEquals(3,db.userBadgeDao().getAll().count { it.badgeId.startsWith("growth_v1_") && it.badgeId.endsWith("hello") })
    }

    @Test fun historicalReleasedCopiesBackfillPermanentlyWithoutChangingWalletOrOldAwards() = runBlocking {
        val spec=CreatureGrowthBadges.byId.getValue("growth_v1_land_pair")
        val legacy=CreatureCatalog.all.first { it.realm==CreatureRealm.LAND && it.isHeritageSpecies }
        val copies=List(2) { creature("legacy-$it",legacy.creatureId,45).copy(creatureStatus=if(it==0) CreatureStatus.RELEASED else CreatureStatus.USED_BEYOND_BLUE) }
        copies.forEach { db.shellFindInstanceDao().insert(it) }
        db.stillwaterLedgerDao().insert(StillwaterLedgerEntity("drops",987654,"soft_flow",null,1))
        val old=UserBadgeEntity("stillwater_mastery",7,100,200,false,viewedAt=300)
        db.userBadgeDao().upsert(old)
        shell().backfillAchievements()
        val award=db.userBadgeDao().get(spec.id)!!
        assertEquals(AchievementTimestampConfidence.UNKNOWN,award.timestampConfidence)
        assertFalse(award.isNew)
        shell().backfillAchievements()
        db.close();open()
        assertEquals(award,db.userBadgeDao().get(spec.id));assertEquals(old,db.userBadgeDao().get(old.badgeId))
        assertEquals(987654L,db.stillwaterLedgerDao().getTotal())
        assertEquals(copies.toSet(),db.shellFindInstanceDao().getAll().toSet())
    }

    @Test fun earlyCollectionCompletesAtFifteenAndSurvivesReconciliation() = runBlocking {
        db.pearlLedgerDao().insert(PearlLedgerEntity("funds",1_000_000,"fixture","fixture",null,1,null))
        val spec=CreatureGrowthBadges.byId.getValue("growth_v1_sea_trio")
        spec.species.take(3).forEachIndexed { index,id -> db.shellFindInstanceDao().insert(creature("copy-$index",id,if(index==2) 14 else 15)) }
        shell().backfillAchievements()
        assertNull(db.userBadgeDao().get("book_growth_sea_early_complete"))
        shell().growCreature("copy-2","complete-trio")
        val award=db.userBadgeDao().get("book_growth_sea_early_complete")!!
        assertEquals(1,award.count)
        shell().backfillAchievements()
        assertEquals(award,db.userBadgeDao().get(award.badgeId))
    }
}
