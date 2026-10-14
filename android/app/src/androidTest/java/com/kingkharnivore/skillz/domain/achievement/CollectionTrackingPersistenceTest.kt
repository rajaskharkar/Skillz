package com.kingkharnivore.skillz.domain.achievement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class CollectionTrackingPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "collection-tracking-${UUID.randomUUID()}"
    private lateinit var db: SkillzDatabase
    private fun open() { db = Room.databaseBuilder(context, SkillzDatabase::class.java, name).build() }
    private fun shell() = ShellRepository(db, db.sessionDao(), db.pearlLedgerDao(), db.shellFindInstanceDao(),
        db.shellFindStackDao(), db.shellPlacementDao(), db.shellFindUpgradeDao(), db.userBadgeDao(),
        db.userDiscoveryDao(), db.stillwaterLedgerDao(), db.stillwaterPreferenceDao(), db.userShellRoomStateDao(), db.objectiveCompletionDao(), db.achievementDao())
    @Before fun before() = open()
    @After fun after() { db.close(); context.deleteDatabase(name) }

    @Test fun collectionAndIndividualTrackingPersistIndependently() = runBlocking {
        val collection = BadgeBookCollections.byId.getValue("badge_book_flow")
        assertTrue(shell().trackBadge(collection.completionBadgeId))
        assertTrue(shell().trackBadge("badge_flow_10_min"))
        assertFalse(shell().trackBadge(collection.completionBadgeId))
        db.close(); open()
        assertEquals(setOf(collection.completionBadgeId, "badge_flow_10_min"), db.achievementDao().getTracking().map { it.badgeId }.toSet())
        shell().untrackBadge(collection.completionBadgeId)
        assertEquals(listOf("badge_flow_10_min"), db.achievementDao().getTracking().map { it.badgeId })
    }

    @Test fun everyCompletedCollectionAwardsOnceAndItsBadgeCanBePinnedAfterRestart() = runBlocking {
        BadgeBookCollections.collections.flatMap { it.memberIds }.distinct().forEachIndexed { index, id ->
            db.userBadgeDao().upsert(UserBadgeEntity(id, 1, 100L + index, 100L + index, false, viewedAt = 500))
        }
        val oldBadge = db.userBadgeDao().get("badge_flow_120_min")!!
        shell().backfillAchievements()
        val awards = BadgeBookCollections.collections.map { collection ->
            val badge = requireNotNull(db.userBadgeDao().get(collection.completionBadgeId)) { collection.id }
            assertEquals(1, badge.count)
            badge
        }
        val flowAward = BadgeBookCollections.byId.getValue("badge_book_flow").completionBadgeId
        assertEquals(ShellRepository.PinResult.Pinned, shell().pinBadge(flowAward))
        assertEquals(ShellRepository.PinResult.Pinned, shell().pinBadge("badge_flow_120_min"))
        shell().backfillAchievements()
        db.close(); open()
        assertEquals(oldBadge, db.userBadgeDao().get(oldBadge.badgeId))
        awards.forEach { assertEquals(it, db.userBadgeDao().get(it.badgeId)) }
        assertEquals(setOf(flowAward, "badge_flow_120_min"), db.achievementDao().getPins().map { it.badgeId }.toSet())
        assertEquals(ShellRepository.PinResult.AlreadyPinned, shell().pinBadge(flowAward))
        assertEquals(0, db.pearlLedgerDao().getBalance())
    }
}
