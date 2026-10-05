package com.kingkharnivore.skillz.domain.land

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.utils.arc.*
import com.kingkharnivore.skillz.utils.shell.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LandPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "land-test-${UUID.randomUUID()}"
    private val prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = ArcPrefs(PreferenceDataStoreFactory.create(scope = prefsScope) { File(context.cacheDir, "$name.preferences_pb") })
    private lateinit var db: SkillzDatabase
    private lateinit var shell: ShellRepository
    private lateinit var finalizer: ArcLandRewardFinalizer

    private fun open() {
        db = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build()
        shell = ShellRepository(db, db.sessionDao(), db.pearlLedgerDao(), db.shellFindInstanceDao(),
            db.shellFindStackDao(), db.shellPlacementDao(), db.shellFindUpgradeDao(), db.userBadgeDao(),
            db.userDiscoveryDao(), db.stillwaterLedgerDao(), db.stillwaterPreferenceDao(),
            db.userShellRoomStateDao(), db.objectiveCompletionDao(), db.achievementDao())
        finalizer = ArcLandRewardFinalizer(db, shell, prefs)
    }
    @Before fun setup() = open()
    @After fun cleanup() { db.close(); prefsScope.cancel(); context.deleteDatabase(name) }

    private suspend fun arc(count: Int, id: Long = 42, end: Long = 1_000L) {
        db.tagDao().insertTag(TagEntity(id = 1, name = "Land", createdAt = 1))
        db.withTransaction {
            repeat(count) { i ->
                db.sessionDao().insertSession(SessionEntity(title = "Flow", description = "", tagId = 1,
                    startTime = end+i-1, endTime = end+i, durationMs = 1, mode = com.kingkharnivore.skillz.model.FlowMode.fromSoft(i%2==0),
                    arcId = id, arcIndex = i+1))
            }
            ArcLandRewardJournal.record(db, id)
        }
    }

    @Test fun expiryUsesCanonicalMembershipIncludesSoftFlowsAndPersistsExactPackageOnce() = runBlocking {
        arc(18)
        finalizer.finalizeExpired(1_017L + ArcRules.GRACE_WINDOW_MS)
        assertTrue(db.shellFindInstanceDao().getAll().isEmpty())
        finalizer.finalizeExpired(1_018L + ArcRules.GRACE_WINDOW_MS)
        assertEquals(listOf("creature_chicken", "creature_tiger"), db.shellFindInstanceDao().getAll().map { it.findId }.sorted())
        val ids = db.shellFindInstanceDao().getAll().map { it.instanceId }.toSet()
        db.close(); open()
        coroutineScope { List(4) { async { finalizer.finalizeExpired(999_999) } }.awaitAll() }
        assertEquals(ids, db.shellFindInstanceDao().getAll().map { it.instanceId }.toSet())
        assertEquals(18, db.arcLandRewardDao().get(42)?.flowCount)
        assertEquals(2L, db.shellRewardEventDao().getEventsForArc(42).sumOf { it.quantity })
    }

    @Test fun continuingArcCannotPayEarlyAndThirtyFlowsCreatesTwoIndependentTigers() = runBlocking {
        arc(30)
        finalizer.startFlow { ArcFlowStart(1_029, ArcRuntimeState(arcId=42, isPending=false,
            multiplier=1.3, progressMs=0, lastSessionEndTimeMs=1_029, sessionCountInArc=30)) }
        finalizer.finalizeExpired(999_999)
        assertTrue(db.shellFindInstanceDao().getAll().isEmpty())
        finalizer.flowEnded()
        finalizer.finalizeExpired(999_999)
        assertEquals(2, db.shellFindInstanceDao().getAll().size)
        assertEquals(setOf("creature_tiger"), db.shellFindInstanceDao().getAll().map { it.findId }.toSet())
        assertEquals(2, db.shellFindInstanceDao().getAll().map { it.instanceId }.toSet().size)
    }

    @Test fun arcEarnedTigersLevelIndependentlyAndRetainMasteryAfterReleaseAndReload() = runBlocking {
        arc(30)
        finalizer.finalizeExpired(999_999)
        shell.addPearls(2_000_000, "test", "test", "tiger-growth")
        val tigers = db.shellFindInstanceDao().getAll()
        tigers.forEachIndexed { index, tiger ->
            db.shellFindInstanceDao().updateAnimalLevel(tiger.instanceId, 98)
            shell.growCreature(tiger.instanceId, "tiger-mastery-$index")
        }
        assertEquals(2, db.achievementDao().getMasteries().count { it.speciesId == "creature_tiger" })
        assertTrue(db.shellFindInstanceDao().getAll().all { it.animalLevel == 99 })
        shell.releaseCreature(tigers.first().instanceId)
        db.close(); open()
        assertEquals(2, db.achievementDao().getMasteries().count { it.speciesId == "creature_tiger" })
        assertEquals(1, db.shellFindInstanceDao().getAll().count { it.creatureStatus == CreatureStatus.ACTIVE && it.animalLevel == 99 })
        finalizer.finalizeExpired(1_999_999)
        assertEquals(2, db.shellFindInstanceDao().getAll().size)
    }

    @Test fun arcTigerChargesPremiumGrowthAtomicallyAndReplaysWithoutChargingAgain() = runBlocking {
        arc(15)
        finalizer.finalizeExpired(999_999)
        val tiger = db.shellFindInstanceDao().getAll().single()
        assertEquals("creature_tiger", tiger.findId)
        shell.addPearls(28, "test", "test", "old-tiger-price")

        val failure = runCatching { shell.growCreature(tiger.instanceId, "premium-tiger-growth") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(28, shell.getPearlBalance())
        assertEquals(1, db.shellFindInstanceDao().getById(tiger.instanceId)?.animalLevel)
        assertNull(db.achievementDao().getEvent("premium-tiger-growth"))

        // An already-owned Arc reward picks up the corrected price after database reload.
        db.close(); open()
        shell.addPearls(3_414 - 28, "test", "test", "premium-tiger-funds")
        val result = shell.growCreature(tiger.instanceId, "premium-tiger-growth")
        assertEquals(3_414, result.pearlCost)
        assertEquals(2, result.resultingLevel)
        assertEquals(0, shell.getPearlBalance())

        db.close(); open()
        val replay = shell.growCreature(tiger.instanceId, "premium-tiger-growth")
        assertEquals(3_414, replay.pearlCost)
        assertEquals(0, shell.getPearlBalance())
        assertEquals(2, db.shellFindInstanceDao().getById(tiger.instanceId)?.animalLevel)
        assertEquals(1, db.shellFindInstanceDao().getAll().size)
    }

    @Test fun historicalArcWithoutProspectiveJournalIsNotRewarded() = runBlocking {
        db.tagDao().insertTag(TagEntity(id=1,name="Old",createdAt=1))
        repeat(15) { db.sessionDao().insertSession(SessionEntity(title="Old", description="", tagId=1,
            startTime=1,endTime=2,durationMs=1,mode=com.kingkharnivore.skillz.model.FlowMode.FLOW,arcId=70,arcIndex=it+1)) }
        finalizer.finalizeExpired(999_999)
        assertTrue(db.shellFindInstanceDao().getAll().isEmpty())
    }

    @Test fun landPurchasesMasteryReleaseAndTradeUseExistingLedger() = runBlocking {
        shell.addPearls(2_000_000, "test", "test", "funds")
        val duck = shell.encounterBeyondBlue("creature_duck", emptyList())
        assertEquals(1_999_760, shell.getPearlBalance())
        db.shellFindInstanceDao().updateAnimalLevel(duck.instanceId,98)
        shell.growCreature(duck.instanceId,"land-level99")
        assertEquals(99, db.shellFindInstanceDao().getById(duck.instanceId)?.animalLevel)
        assertEquals(1, db.achievementDao().getMasteries().count { it.speciesId=="creature_duck" })
        shell.releaseCreature(duck.instanceId)
        assertEquals(1, db.achievementDao().getMasteries().count { it.speciesId=="creature_duck" })
        val second = shell.encounterBeyondBlue("creature_duck", emptyList())
        shell.encounterBeyondBlue("creature_turkey", listOf(second.instanceId))
        assertEquals(CreatureStatus.USED_BEYOND_BLUE, db.shellFindInstanceDao().getById(second.instanceId)?.creatureStatus)
        for(id in listOf("creature_chicken","creature_deer","creature_camel","creature_moose","creature_tiger","creature_peacock")) {
            try { shell.encounterBeyondBlue(id,emptyList()); fail("Purchased $id") } catch (_:IllegalArgumentException) { }
        }
    }
    @Test fun landStillwaterSpendsSharedDropsAndPersistsIndependentCreaturesInTheirWorldZones() = runBlocking {
        shell.addStillwater(250_000, "test", "restorative-funds")
        val zones = CreatureZone.entries.toSet()
        val instances = LandStillwaterHabitat.entries.map { habitat ->
            val before = db.stillwaterLedgerDao().getTotal()
            val instance = shell.drawFromStillwater(habitat, zones)
            assertEquals(before - habitat.dropCost, db.stillwaterLedgerDao().getTotal())
            assertEquals(habitat, CreatureCatalog.require(instance.findId).restorativeHabitat)
            assertEquals(habitat.zone, CreatureCatalog.require(instance.findId).zone)
            assertEquals(CreatureStatus.ACTIVE, instance.creatureStatus)
            instance
        }
        val sea = shell.drawFromStillwater(StillwaterVessel.FISHBOWL, zones)
        assertEquals(CreatureSourceType.STILLWATER, CreatureCatalog.require(sea.findId).sourceType)
        assertEquals(15_000L, db.stillwaterLedgerDao().getTotal())
        db.close(); open()
        assertEquals(6, db.shellFindInstanceDao().getAll().size)
        assertEquals(15_000L, db.stillwaterLedgerDao().getTotal())
        val released = instances.first()
        val payout = CreatureEconomy.releaseValuePearls(released.findId)
        shell.releaseCreature(released.instanceId)
        assertEquals(payout, shell.getPearlBalance())
        assertEquals(CreatureStatus.RELEASED, db.shellFindInstanceDao().getById(released.instanceId)?.creatureStatus)
    }

    @Test fun concurrentLandDrawsCannotOverspendOrGrantAnUnpaidCreature() = runBlocking {
        val habitat = LandStillwaterHabitat.PASTURE
        shell.addStillwater(habitat.dropCost, "test", "single-draw-funds")
        val results = coroutineScope {
            List(4) { async(Dispatchers.IO) { runCatching { shell.drawFromStillwater(habitat, setOf(habitat.zone)) } } }.awaitAll()
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(0L, db.stillwaterLedgerDao().getTotal())
        assertEquals(1, db.shellFindInstanceDao().getAll().size)
        shell.addStillwater(habitat.dropCost, "test", "locked-draw-funds")
        try { shell.drawFromStillwater(habitat, emptySet()); fail("Drew from locked habitat") }
        catch (_: IllegalArgumentException) { }
        assertEquals(habitat.dropCost, db.stillwaterLedgerDao().getTotal())
        assertEquals(1, db.shellFindInstanceDao().getAll().size)
    }

    @Test fun showcaseAcceptsMoreThanThreePinsAndKeepsOrderAcrossReloadAndUnpin() = runBlocking {
        shell.addPearls(5_000, "test", "test", "badge-pins")
        shell.encounterBeyondBlue("creature_duck", emptyList())
        shell.addStillwater(15_000, "test", "badge-pins-drops")
        shell.drawFromStillwater(LandStillwaterHabitat.PASTURE, CreatureZone.entries.toSet())
        val ids = listOf("land_first", "land_encounter_first", "land_stillwater_first", "land_pasture_first", "stillwater_first_catch")
        ids.forEach { assertEquals(ShellRepository.PinResult.Pinned, shell.pinBadge(it)) }
        assertEquals(ShellRepository.PinResult.AlreadyPinned, shell.pinBadge(ids.first()))
        assertEquals(ids, db.achievementDao().getPins().map { it.badgeId })
        db.close(); open()
        assertEquals(ids, db.achievementDao().getPins().map { it.badgeId })
        shell.unpinBadge(ids[1])
        assertEquals(ids.filterIndexed { index, _ -> index != 1 }, db.achievementDao().getPins().map { it.badgeId })
        assertEquals((0..3).toList(), db.achievementDao().getPins().map { it.pinOrder })
        shell.pinBadge(ids[1])
        assertEquals(ids.filterIndexed { index, _ -> index != 1 } + ids[1], db.achievementDao().getPins().map { it.badgeId })
        try { shell.pinBadge("land_two_tigers"); fail("Pinned an unearned badge") }
        catch (_: IllegalArgumentException) { }
        assertEquals(5, db.achievementDao().getPins().size)
    }

    @Test fun landBadgesUseExistingTrackingAndPinsAndSurviveReleaseAndReload() = runBlocking {
        shell.trackBadge("land_variety")
        shell.addStillwater(15_000, "test", "badge-draw")
        val creature = shell.drawFromStillwater(LandStillwaterHabitat.PASTURE, CreatureZone.entries.toSet())
        assertEquals(ShellRepository.PinResult.Pinned, shell.pinBadge("land_first"))
        assertEquals(ShellRepository.PinResult.Pinned, shell.pinBadge("land_stillwater_first"))
        shell.releaseCreature(creature.instanceId)
        db.close(); open()
        val dashboard = com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator.calculate(
            db.userBadgeDao().getAll(), db.shellFindInstanceDao().getAll(),
            db.achievementDao().getDiscoveries(), db.achievementDao().getMasteries(),
            db.achievementDao().getCompletions(), db.achievementDao().getPins(), db.achievementDao().getTracking())
        assertTrue(dashboard.badges.single { it.badgeId == "land_first" }.earned)
        assertTrue(dashboard.badges.single { it.badgeId == "land_stillwater_first" }.earned)
        assertTrue(dashboard.badges.single { it.badgeId == "land_release_first" }.earned)
        assertFalse(dashboard.badges.single { it.badgeId == "land_variety" }.earned)
        assertEquals(1, dashboard.badges.single { it.badgeId == "land_variety" }.progress)
        assertEquals(setOf("land_first","land_stillwater_first"), db.achievementDao().getPins().map { it.badgeId }.toSet())
        assertTrue(db.achievementDao().getTracking().any { it.badgeId == "land_variety" })
    }

}
