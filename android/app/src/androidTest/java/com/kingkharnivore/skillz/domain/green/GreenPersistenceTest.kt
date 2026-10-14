package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.GreenPreviewFixtures
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity
import com.kingkharnivore.skillz.data.repository.green.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class GreenPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val name = "green-persistence-test"
    private lateinit var db: SkillzDatabase
    private lateinit var repository: GreenRepository
    @Before fun before() { context.deleteDatabase(name); open() }
    private fun open() { db=Room.databaseBuilder(context,SkillzDatabase::class.java,name).build(); repository=GreenRepository(db) }
    @After fun after() { db.close(); context.deleteDatabase(name) }
    private suspend fun funds(amount: Long = 10_000_000L) = db.stillwaterLedgerDao().insert(StillwaterLedgerEntity("funds",amount,"soft_flow","fixture",1))
    private suspend fun plant(token: String="seed") = (repository.plantSeed("daisy",token,100) as PlantActionResult.Success).action.specimenId
    @Test fun plantingRetriesAndConcurrentWateringAreAtomic() = runBlocking {
        funds(5_000)
        val results=coroutineScope { List(8) { async(Dispatchers.IO) { plant() } }.awaitAll() }
        assertEquals(1,results.distinct().size); assertEquals(1,db.greenDao().specimens().size)
        assertEquals(3_200L,db.stillwaterLedgerDao().getTotal())
        val id=results.first()
        val waters=coroutineScope { List(8) { index -> async(Dispatchers.IO) { repository.water(id,1,"water-$index",200) } }.awaitAll() }
        assertEquals(1,waters.count { it is PlantActionResult.Success })
        assertEquals(7,waters.count { it == PlantActionResult.StaleLevel })
        assertEquals(2,db.greenDao().specimen(id)?.level)
        assertEquals(2_900L,db.stillwaterLedgerDao().getTotal())
        val success=(waters.first { it is PlantActionResult.Success } as PlantActionResult.Success).action
        assertEquals(success,(repository.water(id,1,success.id,300) as PlantActionResult.Success).action)
        assertEquals(2_900L,db.stillwaterLedgerDao().getTotal())
        plant("intentional-duplicate")
        assertEquals(2,db.greenDao().specimens().size)
        assertEquals(1_100L,db.stillwaterLedgerDao().getTotal())
        assertEquals(PlantActionResult.InsufficientDrops,repository.plantSeed("daisy","cannot-afford"))
        assertEquals(2,db.greenDao().specimens().size)
    }
    @Test fun milestoneDatesInvestmentDuplicatesAndAwardsSurviveReopen() = runBlocking {
        funds()
        val id=plant()
        (1..98).forEach { level ->
            val result=repository.water(id,level,"water:$level",1000L+level) as PlantActionResult.Success
            assertEquals(level+1,result.action.newLevel)
            assertEquals(GreenEconomy.waterCost(1,level),result.action.cost)
        }
        val specimen=db.greenDao().specimen(id)!!
        assertEquals(99,specimen.level);assertEquals(1089L,specimen.fullyGrownAt);assertEquals(1098L,specimen.masteredAt)
        assertEquals(GreenEconomy.totalCost(1,99),specimen.investedDrops)
        assertEquals(PlantActionResult.Mastered,repository.water(id,99,"over-cap",2000))
        val other=plant("second-seed")
        db.close();open()
        assertEquals(specimen,db.greenDao().specimen(id))
        assertEquals(1,db.greenDao().specimen(other)?.level)
        assertEquals(1,db.greenDao().awards().count { it.id=="green_v1_species_daisy_mastery" })
        assertEquals(10_000_000L-GreenEconomy.totalCost(1,99)-1800,db.stillwaterLedgerDao().getTotal())
        assertEquals(specimen.positionKey,db.greenDao().specimen(id)?.positionKey)
        db.openHelper.readableDatabase.query("SELECT name,COUNT(*) FROM green_event WHERE name IN ('plant_fully_grown','plant_mastered') GROUP BY name").use {
            assertTrue(it.moveToNext()); assertEquals(1,it.getInt(1)); assertTrue(it.moveToNext());assertEquals(1,it.getInt(1)); assertFalse(it.moveToNext())
        }
    }
    @Test fun earlyBadgesAndBookAwardPersistAtLevelFifteenWithoutMastery() = runBlocking {
        funds()
        val id=plant()
        (1..14).forEach { level -> repository.water(id,level,"early:$level",1000L+level) }
        listOf("green_v2_gentle_start", "green_v2_first_seedling", "green_v2_small_ritual", "book_green_beginnings_complete").forEach {
            assertEquals(it,1,db.userBadgeDao().get(it)?.count)
        }
        assertNull(db.userBadgeDao().get("green_v2_young_promise"))
        val before=db.userBadgeDao().get("book_green_beginnings_complete")
        val balance=db.stillwaterLedgerDao().getTotal()
        db.close();open();repository.reconcileBadges(9999)
        assertEquals(before,db.userBadgeDao().get("book_green_beginnings_complete"))
        assertEquals(balance,db.stillwaterLedgerDao().getTotal())
    }
    @Test fun insufficientWaterAndClockRollbackDoNotCorruptState() = runBlocking {
        funds(1800)
        val id=plant()
        val before=db.greenDao().specimen(id)
        assertEquals(PlantActionResult.InsufficientDrops,repository.water(id,1,"dry",50))
        assertEquals(before,db.greenDao().specimen(id));assertEquals(0L,db.stillwaterLedgerDao().getTotal())
        db.stillwaterLedgerDao().insert(StillwaterLedgerEntity("more",300,"soft_flow","later",2))
        repository.water(id,1,"wet",50)
        assertEquals(100L,db.greenDao().specimen(id)?.lastWateredAt)
        assertEquals(0L,db.stillwaterLedgerDao().getTotal())
    }
    @Test fun simultaneousDifferentSeedsCannotOverspendTheSharedWallet() = runBlocking {
        funds(1800)
        val results = coroutineScope { List(8) { index -> async(Dispatchers.IO) { repository.plantSeed("daisy", "intent:$index") } }.awaitAll() }
        assertEquals(1, results.count { it is PlantActionResult.Success })
        assertEquals(7, results.count { it == PlantActionResult.InsufficientDrops })
        assertEquals(0L, db.stillwaterLedgerDao().getTotal())
        assertEquals(1, db.greenDao().specimens().size)
    }
    @Test fun failedLedgerInsertRollsBackPlantAndBadges() = runBlocking {
        funds()
        db.stillwaterLedgerDao().insert(StillwaterLedgerEntity("green:collision",1,"fixture",null,1))
        assertTrue(runCatching { repository.plantSeed("daisy","collision") }.isFailure)
        assertTrue(db.greenDao().specimens().isEmpty());assertTrue(db.greenDao().awards().isEmpty());assertNull(db.greenDao().action("collision"))
        assertEquals(10_000_001L,db.stillwaterLedgerDao().getTotal())
    }
    private fun shell() = ShellRepository(db, db.sessionDao(), db.pearlLedgerDao(), db.shellFindInstanceDao(),
        db.shellFindStackDao(), db.shellPlacementDao(), db.shellFindUpgradeDao(), db.userBadgeDao(),
        db.userDiscoveryDao(), db.stillwaterLedgerDao(), db.stillwaterPreferenceDao(),
        db.userShellRoomStateDao(), db.objectiveCompletionDao(), db.achievementDao())

    @Test fun existingGreenAwardsBridgeToSharedHubWithoutChangingDatesPlantsOrWallet() = runBlocking {
        funds(7654321)
        val plant=GreenPreviewFixtures.specimen(GreenCatalogue.byId.getValue("daisy"),99,1)
        db.greenDao().insert(plant)
        val oldAward=GreenBadgeAwardEntity("green_v1_species_daisy_mastery",345,1)
        db.greenDao().award(oldAward)
        val creatureBadge=UserBadgeEntity("stillwater_mastery",7,100,300,false,viewedAt=400)
        db.userBadgeDao().upsert(creatureBadge)
        // Migration reconciliation also runs when achievement backfill was completed on a prior release.
        shell().backfillAchievements()
        val imported=db.userBadgeDao().get(oldAward.id)!!
        assertEquals(345L,imported.firstEarnedAt);assertEquals(345L,imported.lastEarnedAt)
        assertEquals(1,imported.count);assertFalse(imported.isNew)
        assertEquals(ShellRepository.PinResult.Pinned,shell().pinBadge(oldAward.id))
        assertTrue(shell().trackBadge("green_v2_second_spring"))
        val before=db.userBadgeDao().getAll()
        val pins=db.achievementDao().getPins();val tracks=db.achievementDao().getTracking()
        db.close();open()
        shell().backfillAchievements()
        repository.reconcileBadges(9999)
        assertEquals(before.toSet(),db.userBadgeDao().getAll().toSet())
        assertEquals(oldAward,db.greenDao().awards().first { it.id==oldAward.id })
        assertEquals(plant,db.greenDao().specimen(plant.id));assertEquals(7654321L,db.stillwaterLedgerDao().getTotal())
        assertEquals(pins,db.achievementDao().getPins());assertEquals(tracks,db.achievementDao().getTracking())
        // Existing creature award metadata must not be touched by the botanical bridge.
        assertEquals(creatureBadge,db.userBadgeDao().get(creatureBadge.badgeId))
    }
    @Test fun speciesMasteryCountsAdvanceInSharedHubOncePerCopyAndRetainFirstAward() = runBlocking {
        funds()
        val species=GreenCatalogue.byId.getValue("daisy")
        val first=GreenPreviewFixtures.specimen(species,99,1)
        val second=GreenPreviewFixtures.specimen(species,98,2)
        db.greenDao().insert(first);db.greenDao().insert(second)
        repository.reconcileBadges(400)
        val badgeId="green_v1_species_daisy_mastery"
        val old=db.userBadgeDao().get(badgeId)!!
        repository.water(second.id,98,"second-mastery",GreenPreviewFixtures.DATE+999999999)
        repository.water(second.id,98,"second-mastery",GreenPreviewFixtures.DATE+999999999)
        val earned=db.userBadgeDao().get(badgeId)!!
        assertEquals(2,earned.count);assertEquals(old.firstEarnedAt,earned.firstEarnedAt)
        assertTrue(earned.isNew);assertNull(earned.viewedAt)
        assertEquals(1,db.greenDao().awards().count { it.id==badgeId })
    }

    @Test fun completedGoalsLeaveTrackingWhileRepeatableMasteryGoalsRemain() = runBlocking {
        funds()
        val id=plant()
        assertTrue(shell().trackBadge("green_v2_first_water"))
        assertTrue(shell().trackBadge("green_v1_species_daisy_mastery"))
        repository.water(id,1,"first-water",200)
        assertEquals(listOf("green_v1_species_daisy_mastery"),db.achievementDao().getTracking().map { it.badgeId })
        assertEquals(1,db.userBadgeDao().get("green_v2_first_water")?.count)
        assertEquals(ShellRepository.PinResult.Pinned,shell().pinBadge("green_v2_first_water"))
        repository.reconcileBadges(300)
        assertEquals(listOf("green_v2_first_water"),db.achievementDao().getPins().map { it.badgeId })
    }

    @Test fun sharedOnlyAwardsRecoverTheirOriginalHistoryWithoutReearningOrSpending() = runBlocking {
        funds(87654)
        val old = UserBadgeEntity("green_v1_species_daisy_mastery", 3, 123, 456, false, viewedAt=789)
        db.userBadgeDao().upsert(old)
        db.achievementDao().insertPin(com.kingkharnivore.skillz.data.model.entity.shell.BadgePinEntity(old.badgeId, 0, 800))
        val pins = db.achievementDao().getPins()
        repository.reconcileBadges(9999, historical=false)
        assertEquals(123L, db.greenDao().awards().single().awardedAt)
        assertEquals(old, db.userBadgeDao().get(old.badgeId))
        db.close(); open(); repository.reconcileBadges(10000)
        assertEquals(old, db.userBadgeDao().get(old.badgeId))
        assertEquals(pins, db.achievementDao().getPins())
        assertEquals(87654L, db.stillwaterLedgerDao().getTotal())
        assertTrue(db.greenDao().specimens().isEmpty())
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM green_event").use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
    }

    @Test fun staleFeedbackAcknowledgementCannotDismissAnotherActionOrRepeatedError() = runBlocking {
        funds()
        val vm = com.kingkharnivore.skillz.viewmodel.green.GreenViewModel(repository)
        val store = androidx.lifecycle.ViewModelStore()
        store.put("green", vm)
        try {
            suspend fun awaitState(predicate: (com.kingkharnivore.skillz.viewmodel.green.GreenUiState) -> Boolean) =
                withTimeout(10000) { vm.state.first(predicate) }
            withContext(Dispatchers.Main) { vm.water("missing", 1, "first-error") }
            val first = awaitState { it.messageRes != null }
            withContext(Dispatchers.Main) { vm.water("missing", 1, "second-error") }
            val second = awaitState { it.messageRes != null && it.feedbackId != first.feedbackId }
            withContext(Dispatchers.Main) { vm.dismissFeedback(first.feedbackId) }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(second.feedbackId, vm.state.value.feedbackId)
            assertNotNull(vm.state.value.messageRes)
            withContext(Dispatchers.Main) { vm.plant("daisy", "feedback-seed") }
            val planted = awaitState { it.action?.id == "feedback-seed" }
            withContext(Dispatchers.Main) { vm.dismissFeedback(second.feedbackId) }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals("feedback-seed", vm.state.value.action?.id)
            withContext(Dispatchers.Main) { vm.dismissFeedback(planted.feedbackId) }
            awaitState { it.action == null && !it.busy && it.messageRes == null }
        } finally { withContext(Dispatchers.Main) { store.clear() } }
        Unit
    }

}
