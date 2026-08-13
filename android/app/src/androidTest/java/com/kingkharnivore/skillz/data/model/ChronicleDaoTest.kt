package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChronicleDaoTest {
    private lateinit var db: SkillzDatabase
    @Before fun open() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SkillzDatabase::class.java).build() }
    @After fun close() = db.close()

    @Test fun reorderValidatesAndUpdatePreservesChildren() = runBlocking {
        val dao = db.chronicleDao(); val now = 1L
        dao.insertChronicle(ChronicleEntity("c", "SESSION", "1", "", now, now))
        val a = ChronicleMomentEntity("a", "c", "MEDIA", 0, createdAt=now, updatedAt=now)
        val b = ChronicleMomentEntity("b", "c", "TEXT", 1, text=" exact ", createdAt=now, updatedAt=now)
        dao.insertMoment(a); dao.insertMoment(b)
        dao.insertMedia(listOf(ChronicleMediaItemEntity("media", "a", 0, "owned", "image/jpeg", createdAt=now)))
        dao.reorderMoments("c", listOf("b", "a"), now)
        assertEquals(listOf("b", "a"), dao.moments("c").map { it.id })
        assertThrows(IllegalArgumentException::class.java) { runBlocking { dao.reorderMoments("c", listOf("a"), now) } }
        assertEquals(listOf("b", "a"), dao.moments("c").map { it.id })
        dao.updateMoment(a.copy(displayName="updated"))
        assertEquals(listOf("media"), dao.media("a").map { it.id })
    }

    @Test fun flowPromotionIsAtomicAndNextFlowIsIndependent() = runBlocking {
        db.tagDao().insertTag(TagEntity(name = "Journey"))
        val repository = ChronicleRepository(db, db.chronicleDao())
        repository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "flow-a", "A")
        repository.addText(ChronicleOwnerType.ACTIVE_FLOW, "flow-a", "A")
        val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao())
        val sessionId = flows.addSessionAndPromoteChronicle("flow-a", SessionEntity(
            title="Flow A", description="", tagId=1, startTime=1, endTime=2, durationMs=1))
        assertEquals(null, db.chronicleDao().find(ChronicleOwnerType.ACTIVE_FLOW, "flow-a"))
        val completed = db.chronicleDao().find(ChronicleOwnerType.SESSION, sessionId.toString())!!
        repository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "flow-b", "B")
        val next = db.chronicleDao().find(ChronicleOwnerType.ACTIVE_FLOW, "flow-b")!!
        assertEquals("A", db.chronicleDao().moments(completed.id).single().text)
        assertEquals("B", next.draftText)
    }
}
