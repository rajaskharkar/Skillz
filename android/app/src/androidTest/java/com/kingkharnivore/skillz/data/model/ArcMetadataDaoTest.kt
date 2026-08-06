package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.entity.ArcMetadataEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArcMetadataDaoTest {
    private lateinit var database: SkillzDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun insertObserveUpdateDeleteAndIsolateArcIds() = runBlocking {
        val dao = database.arcMetadataDao()
        val first = metadata(1, "First")
        val second = metadata(2, "Second")
        dao.upsert(first)
        dao.upsert(second)
        assertEquals("First", dao.get(1)?.title)
        assertEquals(setOf(1L, 2L), dao.observeAll().first().map { it.arcId }.toSet())

        dao.upsert(first.copy(title = null, summary = "Updated", updatedAtEpochMillis = 2))
        assertNull(dao.get(1)?.title)
        assertEquals("Updated", dao.observe(1).first()?.summary)
        assertEquals("Second", dao.get(2)?.title)

        dao.delete(1)
        assertNull(dao.get(1))
        assertNotNull(dao.get(2))
    }

    @Test fun metadataIsRemovedOnlyWhenFinalArcFlowIsDeleted() = runBlocking {
        val repository = FlowRepository(
            database.sessionDao(), database.tagDao(), database.pulseDao(),
            database.arcMetadataDao(), database
        )
        database.tagDao().insertTag(TagEntity(id = 1, name = "Journey", createdAt = 1))
        val firstId = database.sessionDao().insertSession(session(arcId = 10, arcIndex = 1))
        val finalId = database.sessionDao().insertSession(session(arcId = 10, arcIndex = 2))
        database.arcMetadataDao().upsert(metadata(10, "Keep until final Flow"))
        database.arcMetadataDao().upsert(metadata(20, "Other Arc"))

        repository.deleteSession(firstId)
        assertNotNull(database.arcMetadataDao().get(10))
        assertNotNull(database.arcMetadataDao().get(20))

        repository.deleteSession(finalId)
        assertNull(database.arcMetadataDao().get(10))
        assertNotNull(database.arcMetadataDao().get(20))
    }

    @Test fun deletingStandaloneFlowDoesNotTouchArcMetadata() = runBlocking {
        val repository = FlowRepository(
            database.sessionDao(), database.tagDao(), database.pulseDao(),
            database.arcMetadataDao(), database
        )
        database.tagDao().insertTag(TagEntity(id = 1, name = "Journey", createdAt = 1))
        val standaloneId = database.sessionDao().insertSession(session(arcId = null, arcIndex = null))
        database.arcMetadataDao().upsert(metadata(10, "Arc"))

        repository.deleteSession(standaloneId)
        assertNotNull(database.arcMetadataDao().get(10))
    }

    private fun metadata(arcId: Long, title: String) = ArcMetadataEntity(
        arcId, title, null, null, null, null, 1, 1
    )

    private fun session(arcId: Long?, arcIndex: Int?) = SessionEntity(
        title = "Flow", description = "", tagId = 1, startTime = 1, endTime = 2,
        durationMs = 1, arcId = arcId, arcIndex = arcIndex, createdAt = 1
    )
}
