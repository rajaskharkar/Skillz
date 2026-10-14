package com.kingkharnivore.skillz.data.model

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.PulseRepository
import com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleStateHolder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
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

    @Test fun softFlowPulsePersistsBeforeAndAfterCompletion() = runBlocking {
        val chronicles = ChronicleRepository(db, db.chronicleDao())
        val pulses = PulseRepository(db.pulseDao(), db.sessionDao(), db.tagDao(), db, db.chronicleDao(), chronicles)
        val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
        val tagId = db.tagDao().insertTag(TagEntity(name = "Outdoors"))
        chronicles.addText(ChronicleOwnerType.PULSE_DRAFT, "soft-pulse", "Captured during a soft flow")
        val pulseId = pulses.addPulseAndPromoteDraft("soft-pulse", PulseEntity(
            title = "A thought on the walk", description = "", parentFlowInstanceId = "soft-flow"))
        assertEquals("soft-flow", pulses.getPulseById(pulseId)!!.parentFlowInstanceId)
        assertEquals("Captured during a soft flow", chronicles.observeContent("PULSE", pulseId.toString()).first().moments
            .filterIsInstance<com.kingkharnivore.skillz.model.ui.ChronicleMomentUi.Text>().single().text)
        val sessionId = flows.addSessionAndPromoteChronicle("soft-flow", SessionEntity(
            title = "Evening walk", description = "", tagId = tagId, startTime = 1, endTime = 101,
            durationMs = 100, mode = com.kingkharnivore.skillz.model.FlowMode.SOFT))
        val persisted = pulses.getPulseById(pulseId)!!
        assertEquals(sessionId, persisted.parentSessionId)
        assertEquals(null, persisted.parentFlowInstanceId)
        assertEquals(pulseId, pulses.getPulsesForSession(sessionId).single().id)
    }

    @Test fun editingCompletedFlowPreservesHistoryAndAcceptsNewChronicleMedia() = runBlocking {
        val chronicles = ChronicleRepository(db, db.chronicleDao())
        val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), chronicles)
        val oldTag = db.tagDao().insertTag(TagEntity(name = "Old journey"))
        val newTag = db.tagDao().insertTag(TagEntity(name = "Photography"))
        val original = SessionEntity(title = "Old title", description = "", tagId = oldTag,
            startTime = 1, endTime = 101, durationMs = 100, scyraPoints = 42)
        val id = flows.addSessionAndPromoteChronicle("finished-flow", original)
        assertEquals(oldTag, flows.updateSessionDetails(id, "Photo walk", newTag))
        assertEquals(original.copy(id = id, title = "Photo walk", tagId = newTag), flows.getSessionById(id))
        chronicles.addText("SESSION", id.toString(), "Added after completion")
        val chronicle = db.chronicleDao().find("SESSION", id.toString())!!
        db.chronicleDao().insertMoment(ChronicleMomentEntity("later-photo", chronicle.id, "MEDIA", 1, createdAt = 1000, updatedAt = 1000))
        db.chronicleDao().insertMedia(listOf(ChronicleMediaItemEntity("later-image", "later-photo", 0, "photo.jpg", "image/jpeg", createdAt = 1000)))
        assertEquals(2, chronicles.observeContent("SESSION", id.toString()).first().moments.size)
        assertEquals(42, flows.getSessionById(id)!!.scyraPoints)
    }

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
        val second = ChronicleMediaItemEntity("second", "a", 1, "owned-2", "video/mp4", createdAt=now)
        dao.replaceMedia("a", listOf(second, dao.media("a").single()))
        assertEquals(listOf("second", "media"), dao.media("a").map { it.id })
        assertEquals(listOf(0, 1), dao.media("a").map { it.position })
        assertEquals(1, dao.mediaPathReferenceCount("owned"))
    }

    @Test fun flowPromotionIsAtomicAndNextFlowIsIndependent() = runBlocking {
        db.tagDao().insertTag(TagEntity(name = "Journey"))
        val repository = ChronicleRepository(db, db.chronicleDao())
        repository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "flow-a", "A")
        repository.addText(ChronicleOwnerType.ACTIVE_FLOW, "flow-a", "A")
        val flows = FlowRepository(db.sessionDao(), db.tagDao(), db.pulseDao(), db.arcMetadataDao(), db, db.chronicleDao(), repository)
        val sessionId = flows.addSessionAndPromoteChronicle("flow-a", SessionEntity(
            title="Flow A", description="", tagId=1, startTime=1, endTime=2, durationMs=1))
        val retryId = flows.addSessionAndPromoteChronicle("flow-a", SessionEntity(
            title="Flow A", description="", tagId=1, startTime=1, endTime=2, durationMs=1))
        assertEquals(sessionId, retryId)
        assertEquals(null, db.chronicleDao().find(ChronicleOwnerType.ACTIVE_FLOW, "flow-a"))
        val completed = db.chronicleDao().find(ChronicleOwnerType.SESSION, sessionId.toString())!!
        repository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "flow-b", "B")
        val next = db.chronicleDao().find(ChronicleOwnerType.ACTIVE_FLOW, "flow-b")!!
        assertEquals("A", db.chronicleDao().moments(completed.id).single().text)
        assertEquals("B", next.draftText)
        val restartedRepository = ChronicleRepository(db, db.chronicleDao())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { restartedRepository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "flow-a", "late") }
        }
        assertEquals(null, db.chronicleDao().find(ChronicleOwnerType.ACTIVE_FLOW, "flow-a"))
    }

    @Test fun pulseCreationKeyIsIdempotentAcrossRepositoryRecreation() = runBlocking {
        val chronicles = ChronicleRepository(db, db.chronicleDao())
        chronicles.setDraft(ChronicleOwnerType.PULSE_DRAFT, "draft-x", "moment")
        chronicles.addText(ChronicleOwnerType.PULSE_DRAFT, "draft-x", "moment")
        val firstRepository = PulseRepository(db.pulseDao(), db.sessionDao(), db.tagDao(), db, db.chronicleDao(), chronicles)
        val pulse = PulseEntity(title="", description="")
        val first = firstRepository.addPulseAndPromoteDraft("draft-x", pulse)
        // Simulates process recreation: in-memory owner locks/tombstones are gone.
        val recreatedChronicles = ChronicleRepository(db, db.chronicleDao())
        val recreatedRepository = PulseRepository(db.pulseDao(), db.sessionDao(), db.tagDao(), db, db.chronicleDao(), recreatedChronicles)
        val retry = recreatedRepository.addPulseAndPromoteDraft("draft-x", pulse)
        assertEquals(first, retry)
        assertEquals(1, db.pulseDao().getAllPulses().first().size)
        assertEquals(null, db.chronicleDao().find(ChronicleOwnerType.PULSE_DRAFT, "draft-x"))
        assertThrows(IllegalStateException::class.java) {
            runBlocking { recreatedChronicles.setDraft(ChronicleOwnerType.PULSE_DRAFT, "draft-x", "late") }
        }
        Unit
    }

    @Test fun storySummaryIsBounded() = runBlocking {
        val repository = ChronicleRepository(db, db.chronicleDao())
        val longText = "x".repeat(2_000)
        repository.setDraft(ChronicleOwnerType.ACTIVE_FLOW, "summary", longText)
        repository.addText(ChronicleOwnerType.ACTIVE_FLOW, "summary", longText)
        val summary = repository.observeSummaries().first().single()
        assertEquals(1, summary.momentCount)
        assertEquals(240, summary.excerpt?.length)
    }

    @Test fun retranscriptionUpdatesOriginalWithoutOverwritingEditedTranscript() = runBlocking {
        val dao = db.chronicleDao()
        dao.insertChronicle(ChronicleEntity("audio-c", ChronicleOwnerType.SESSION, "42", "", 1, 1))
        val audio = ChronicleMomentEntity("audio-m", "audio-c", ChronicleMomentType.VOICE, 0,
            audioPath = "chronicle/audio-c/audio/a.m4a", transcript = "mine", transcriptEdited = true,
            createdAt = 1, updatedAt = 1)
        dao.insertMoment(audio)
        assertEquals(1, dao.setOriginalTranscript(audio.id, "late", 2))
        val updated = dao.moments("audio-c").single()
        assertEquals("late", updated.originalTranscript)
        assertEquals("mine", updated.transcript)
    }

    @Test fun committedDraftStaysClearedWhenRoomDeliversMomentAndDraftSnapshots() = runBlocking {
        val repository = ChronicleRepository(db, db.chronicleDao())
        val holder = ChronicleStateHolder(ChronicleOwnerType.SESSION, "saved-drafts", repository, this)
        try {
            repeat(3) { index ->
                val text = "Saved moment $index"
                holder.setDraft(text)
                val committed = kotlinx.coroutines.CompletableDeferred<Unit>()
                holder.add { committed.complete(Unit) }
                // The IME can move the cursor while the pending Add write is completing.
                holder.setDraft(androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange.Zero))
                withTimeout(5_000) { committed.await() }
                val observed = withTimeout(5_000) {
                    holder.state.first { it.contentMoments.size == index + 1 && !it.isCommitting }
                }
                assertEquals("", observed.draft)
                val owner = db.chronicleDao().find(ChronicleOwnerType.SESSION, "saved-drafts")!!
                assertEquals("", owner.draftText)
                assertEquals(index + 1, db.chronicleDao().moments(owner.id).size)
            }
            val ready = kotlinx.coroutines.CompletableDeferred<Unit>()
            holder.quiesce { ready.complete(Unit) }
            withTimeout(5_000) { ready.await() }
        } finally { holder.close() }
    }

    @Test fun pendingMomentRemovalIsUndoableBeforeDatabaseDeletion() = runBlocking {
        val repository = ChronicleRepository(db, db.chronicleDao())
        repository.addText(ChronicleOwnerType.ACTIVE_FLOW, "undo-flow", "Keep me")
        val holder = ChronicleStateHolder(
            ChronicleOwnerType.ACTIVE_FLOW,
            "undo-flow",
            repository,
            this,
        )
        val moment = withTimeout(2_000) {
            holder.state.first { it.contentMoments.isNotEmpty() }.contentMoments.single()
        }

        holder.requestDelete(moment)
        assertEquals(moment.id, holder.state.value.pendingDeletion?.id)
        assertEquals(1, db.chronicleDao().moments(moment.chronicleId).size)

        holder.undoPendingDelete(moment.id)
        assertEquals(null, holder.state.value.pendingDeletion)
        assertEquals(1, db.chronicleDao().moments(moment.chronicleId).size)

        holder.requestDelete(moment)
        holder.commitPendingDelete(moment.id)
        withTimeout(2_000) {
            db.chronicleDao().observeMoments(moment.chronicleId).first { it.isEmpty() }
        }
        holder.close()
    }

    @Test fun cancellingMediaEditRestoresStagedItemRemoval() = runBlocking {
        val dao = db.chronicleDao()
        val repository = ChronicleRepository(db, dao)
        val chronicle = repository.getOrCreate(ChronicleOwnerType.ACTIVE_FLOW, "media-edit-flow")
        val mediaMoment = ChronicleMomentEntity(
            id = "media-moment",
            chronicleId = chronicle.id,
            type = ChronicleMomentType.MEDIA,
            position = 0,
            createdAt = 1,
            updatedAt = 1,
        )
        dao.insertMoment(mediaMoment)
        dao.insertMedia(listOf(
            ChronicleMediaItemEntity("photo", mediaMoment.id, 0, "photo.jpg", "image/jpeg", createdAt = 1),
            ChronicleMediaItemEntity("video", mediaMoment.id, 1, "video.mp4", "video/mp4", createdAt = 2),
        ))
        val holder = ChronicleStateHolder(
            ChronicleOwnerType.ACTIVE_FLOW,
            "media-edit-flow",
            repository,
            this,
        )
        val moment = withTimeout(2_000) {
            holder.state.first { it.contentMoments.isNotEmpty() }.contentMoments.single()
        } as com.kingkharnivore.skillz.model.ui.ChronicleMomentUi.Media

        holder.beginMediaEdit(moment)
        holder.removeMediaItem("photo")
        assertEquals(listOf("video"), holder.state.value.editingMediaItems.map { it.id })
        assertEquals(listOf("photo", "video"), dao.media(mediaMoment.id).map { it.id })

        holder.cancelMediaEdit()
        assertEquals(null, holder.state.value.editingMediaId)
        assertEquals(listOf("photo", "video"), dao.media(mediaMoment.id).map { it.id })
        holder.close()
    }
}
