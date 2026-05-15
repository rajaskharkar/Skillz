package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.dao.shell.ShellRewardEventDao
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventTypes
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellRewardEventRecorderTest {
    @Test
    fun recordingSameSessionRewardTwiceDoesNotDuplicateEvents() = runBlocking {
        val dao = FakeShellRewardEventDao()
        val recorder = ShellRewardEventRecorder(dao)
        val session = session(id = 123L, arcId = 99L)
        val reward = ShellRewardResult(
            pearlsEarned = 482,
            grantedFindIds = listOf(ShellContentCatalog.FOCUS_MINNOW),
            badgeIds = listOf("badge_flow_10_min")
        )

        recorder.recordSessionRewards(session, reward)
        recorder.recordSessionRewards(session, reward)

        val events = dao.getEventsForArc(99L)
        assertEquals(3, events.size)
        assertEquals(setOf(123L), events.map { it.sourceSessionId }.toSet())
        assertEquals(setOf(99L), events.map { it.arcId }.toSet())
        assertEquals(482L, events.single { it.rewardType == ShellRewardEventTypes.PEARLS_CARRIED }.quantity)
    }

    @Test
    fun standaloneFlowEventsHaveNullArcIdAndArcQueryIsScoped() = runBlocking {
        val dao = FakeShellRewardEventDao()
        val recorder = ShellRewardEventRecorder(dao)

        recorder.recordSessionRewards(session(id = 1L, arcId = null), ShellRewardResult(pearlsEarned = 10))
        recorder.recordSessionRewards(session(id = 2L, arcId = 7L), ShellRewardResult(pearlsEarned = 20))
        recorder.recordSessionRewards(session(id = 3L, arcId = 8L), ShellRewardResult(pearlsEarned = 30))

        assertNull(dao.getEventsForSession(1L).single().arcId)
        assertEquals(listOf(2L), dao.getEventsForArc(7L).map { it.sourceSessionId })
    }

    @Test
    fun softFlowRecordsStillwaterOnly() = runBlocking {
        val dao = FakeShellRewardEventDao()
        val recorder = ShellRewardEventRecorder(dao)

        recorder.recordSessionRewards(
            session(id = 4L, arcId = 7L, isSoftMode = true),
            ShellRewardResult(pearlsEarned = 100, stillwaterUnits = 42L, grantedFindIds = listOf(ShellContentCatalog.FOCUS_MINNOW), badgeIds = listOf("badge_flow_10_min"))
        )

        val events = dao.getEventsForArc(7L)
        assertEquals(1, events.size)
        assertEquals(ShellRewardEventTypes.STILLWATER_ADDED, events.single().rewardType)
        assertEquals(42L, events.single().quantity)
    }

    private fun session(id: Long, arcId: Long?, isSoftMode: Boolean = false) = SessionEntity(
        id = id,
        title = "Flow",
        description = "",
        tagId = 1L,
        startTime = 0L,
        endTime = 1L,
        durationMs = 600_000L,
        scyraPoints = 100,
        isSoftMode = isSoftMode,
        arcId = arcId
    )

    private class FakeShellRewardEventDao : ShellRewardEventDao {
        private val eventsById = linkedMapOf<String, ShellRewardEventEntity>()

        override suspend fun insertAll(events: List<ShellRewardEventEntity>) {
            events.forEach { eventsById.putIfAbsent(it.id, it) }
        }

        override suspend fun getEventsForArc(arcId: Long): List<ShellRewardEventEntity> =
            eventsById.values.filter { it.arcId == arcId }.sortedBy { it.occurredAt }

        override suspend fun getEventsForSession(sourceSessionId: Long): List<ShellRewardEventEntity> =
            eventsById.values.filter { it.sourceSessionId == sourceSessionId }.sortedBy { it.occurredAt }
    }
}
