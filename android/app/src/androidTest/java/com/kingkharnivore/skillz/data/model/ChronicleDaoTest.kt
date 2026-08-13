package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.entity.*
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
}
