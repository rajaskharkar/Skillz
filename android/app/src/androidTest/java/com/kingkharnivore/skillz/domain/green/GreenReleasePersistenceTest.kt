package com.kingkharnivore.skillz.domain.green

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.StillwaterLedgerEntity
import com.kingkharnivore.skillz.data.repository.green.*
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.GreenPreviewFixtures
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class GreenReleasePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val name = "green-release-${UUID.randomUUID()}"
    private lateinit var db: SkillzDatabase
    private lateinit var repository: GreenRepository
    private fun open() { db = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build(); repository = GreenRepository(db) }
    @Before fun before() = open()
    @After fun after() { db.close(); context.deleteDatabase(name) }
    private suspend fun fund(amount: Long = 1000000) = db.stillwaterLedgerDao().insert(
        StillwaterLedgerEntity("funds", amount, "soft_flow", null, 1))
    private suspend fun seed(token: String) = (repository.plantSeed("daisy", token, 10) as PlantActionResult.Success).action.specimenId
    private suspend fun dashboard() = BadgeDashboardCalculator.calculate(db.userBadgeDao().getAll(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), greenPlants = db.greenDao().specimens(),
        greenAwards = db.greenDao().awards())

    @Test fun releaseCreditsSeedAndWaterInvestmentOnceAndKeepsOtherCopiesAndHistoryAcrossReopen() = runBlocking {
        fund(10000)
        val id = seed("first")
        val other = seed("second")
        repository.water(id, 1, "water", 20)
        val original = db.greenDao().specimen(id)!!
        assertEquals(2100L, original.investedDrops)
        val otherBefore = db.greenDao().specimen(other)
        val balanceBefore = db.stillwaterLedgerDao().getTotal()
        val release = (repository.release(id, 2, "release", 30) as PlantActionResult.Success).action
        assertEquals(-1575L, release.cost)
        assertEquals(balanceBefore + 1575, db.stillwaterLedgerDao().getTotal())
        assertEquals(listOf(other), repository.observeSpecimens().first().map { it.id })
        assertEquals(original.copy(releasedAt = 30), db.greenDao().specimen(id))
        assertEquals(otherBefore, db.greenDao().specimen(other))
        assertEquals(PlantActionResult.Released, repository.water(id, 2, "after-release"))
        assertEquals(PlantActionResult.Released, repository.release(id, 2, "new-release"))
        db.close(); open()
        assertEquals(release, (repository.release(id, 2, "release") as PlantActionResult.Success).action)
        assertEquals(balanceBefore + 1575, db.stillwaterLedgerDao().getTotal())
        assertEquals(2, repository.observeLifetimeSpecimens().first().size)
        assertEquals(listOf(other), repository.observeSpecimens().first().map { it.id })
        // The same spendable wallet funds the next seed, without a separate claim step.
        seed("reinvest")
        assertEquals(balanceBefore + 1575 - 1800, db.stillwaterLedgerDao().getTotal())
        assertNotNull(db.greenDao().action("first")); assertNotNull(db.greenDao().action("water"))
    }

    @Test fun masteryAndLifetimeProgressSurviveReleaseAndTheNextMasteryAddsAnotherCount() = runBlocking {
        fund()
        val species = GreenCatalogue.byId.getValue("daisy")
        val mastered = GreenPreviewFixtures.specimen(species, 99, 1)
        db.greenDao().insert(mastered)
        repository.reconcileBadges(100)
        val earned = db.userBadgeDao().getAll()
        val awards = db.greenDao().awards()
        val before = dashboard()
        repository.release(mastered.id, 99, "release-master", 200)
        db.close(); open(); repository.reconcileBadges(300)
        val after = dashboard()
        assertEquals(earned.toSet(), db.userBadgeDao().getAll().toSet())
        assertEquals(awards.toSet(), db.greenDao().awards().toSet())
        assertEquals(before.totalMasteries, after.totalMasteries)
        assertEquals(before.badges.filter { it.badgeId in GreenBadgeEvaluator.byId }.map { Triple(it.badgeId, it.count, it.progress) },
            after.badges.filter { it.badgeId in GreenBadgeEvaluator.byId }.map { Triple(it.badgeId, it.count, it.progress) })
        assertTrue(repository.observeSpecimens().first().isEmpty())
        val destination = after.badges.first { it.badgeId == "green_v1_species_daisy_mastery" }.action as BadgeActionDestination.Green
        assertTrue(destination.plantSeed)
        assertNull(destination.specimenId)
        val next = GreenPreviewFixtures.specimen(species, 98, 2)
        db.greenDao().insert(next)
        repository.water(next.id, 98, "next-mastery")
        assertEquals(2, db.userBadgeDao().get("green_v1_species_daisy_mastery")?.count)
        assertEquals(2, dashboard().totalMasteries)
    }

    @Test fun concurrentReleaseCommandsCannotDuplicateRewardAndStaleQuotesCannotRelease() = runBlocking {
        fund()
        val id = seed("seed")
        repository.water(id, 1, "water")
        assertEquals(PlantActionResult.StaleLevel, repository.release(id, 1, "stale"))
        assertNull(db.greenDao().specimen(id)?.releasedAt)
        val before = db.stillwaterLedgerDao().getTotal()
        val results = coroutineScope { List(8) { index -> async(Dispatchers.IO) { repository.release(id, 2, "release-$index") } }.awaitAll() }
        assertEquals(1, results.count { it is PlantActionResult.Success })
        assertEquals(7, results.count { it == PlantActionResult.Released })
        assertEquals(before + 1575, db.stillwaterLedgerDao().getTotal())
        assertEquals(1, db.stillwaterLedgerDao().sourceCount("plant_released", id))
        assertEquals(PlantActionResult.NotFound, repository.release("missing", 1, "missing"))
    }

    @Test fun failedWalletWriteRollsBackReleaseAndLeavesThePlantUsable() = runBlocking {
        fund()
        val id = seed("seed")
        val original = db.greenDao().specimen(id)
        db.stillwaterLedgerDao().insert(StillwaterLedgerEntity("green:collision", 1, "fixture", null, 1))
        val before = db.stillwaterLedgerDao().getTotal()
        assertTrue(runCatching { repository.release(id, 1, "collision") }.isFailure)
        assertEquals(original, db.greenDao().specimen(id))
        assertEquals(before, db.stillwaterLedgerDao().getTotal())
        assertNull(db.greenDao().action("collision"))
        assertTrue(repository.water(id, 1, "still-waterable") is PlantActionResult.Success)
    }
}
