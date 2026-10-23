package com.kingkharnivore.skillz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.domain.analytics.LifetimeStats
import com.kingkharnivore.skillz.model.FlowMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class LifetimeAnalyticsRepositoryTest {
    @Test fun observedTotalsFollowInsertsJourneyEditsAndDeletions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build()
        val chronicle = ChronicleRepository(db, db.chronicleDao())
        val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicle)
        val repository = LifetimeAnalyticsRepository(flows, JourneyRepository(db.tagDao()))
        val values = Channel<LifetimeStats>(Channel.UNLIMITED)
        val collection = launch { repository.observe().collect { values.send(it) } }
        suspend fun await(predicate: (LifetimeStats) -> Boolean): LifetimeStats = withTimeout(10_000) {
            var current = values.receive()
            while (!predicate(current)) current = values.receive()
            current
        }
        try {
            await { it.flowCount == 0 }
            val art = db.tagDao().insertTag(TagEntity(name = "Art"))
            val music = db.tagDao().insertTag(TagEntity(name = "Music"))
            val id = db.sessionDao().insertSession(SessionEntity(title = "Practice", description = "", tagId = art,
                startTime = 1_700_000_000_000, endTime = 1_700_003_600_000, durationMs = 3_600_000,
                mode = FlowMode.SOFT, scyraPoints = 0))
            val initial = await { it.flowCount == 1 && it.journeys.singleOrNull()?.name == "Art" }
            assertEquals(3_600_000L, initial.totalDurationMs)
            assertEquals(1, initial.modes.single { it.mode == FlowMode.SOFT }.count)
            db.sessionDao().updateDetails(id, "Updated practice", music)
            val edited = await { it.journeys.singleOrNull()?.name == "Music" }
            assertEquals(initial.totalDurationMs, edited.totalDurationMs)
            db.sessionDao().deleteSessionById(id)
            val empty = await { it.flowCount == 0 }
            assertEquals(0L, empty.totalDurationMs)
            assertEquals(0, empty.activeDays)
        } finally {
            collection.cancelAndJoin()
            values.close()
            db.close()
        }
    }
}
