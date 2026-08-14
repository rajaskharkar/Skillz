package com.kingkharnivore.skillz.data.repository

import androidx.room.withTransaction
import android.net.Uri
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity
import com.kingkharnivore.skillz.data.chronicle.ChronicleFileStore
import com.kingkharnivore.skillz.model.ui.ChronicleMediaItemUi
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ChronicleRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val dao: ChronicleDao,
    private val fileStore: ChronicleFileStore? = null
) {
    private data class Owner(val type: String, val key: String)
    private val ownerMutexes = ConcurrentHashMap<Owner, Mutex>()
    private val finalizedOwners = ConcurrentHashMap.newKeySet<Owner>()
    private fun mutex(owner: Owner) = ownerMutexes.getOrPut(owner) { Mutex() }
    private suspend fun isDurablyFinalized(owner: Owner): Boolean = when (owner.type) {
        ChronicleOwnerType.ACTIVE_FLOW -> database.sessionDao().findCreatedSession(owner.key) != null
        ChronicleOwnerType.PULSE_DRAFT -> database.pulseDao().findCreatedPulse(owner.key) != null
        else -> false
    }
    private suspend fun requireMutable(owner: Owner) {
        check(owner !in finalizedOwners && !isDurablyFinalized(owner)) {
            "Chronicle owner is finalized"
        }
    }
    data class Snapshot(val chronicle: ChronicleEntity?, val moments: List<ChronicleMomentEntity>)
    data class ContentSnapshot(val chronicle: ChronicleEntity?, val moments: List<ChronicleMomentUi>)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(ownerType: String, ownerKey: String): Flow<Snapshot> =
        dao.observe(ownerType, ownerKey).transformLatest { chronicle ->
            if (chronicle == null) emit(Snapshot(null, emptyList()))
            else dao.observeMoments(chronicle.id).collect { emit(Snapshot(chronicle, it)) }
        }

    fun observeSummaries() = dao.observeSummaries()

    data class MediaImportResult(val momentId: String?, val failedCount: Int)

    fun createCaptureOutput(video: Boolean): Uri =
        fileStore?.createCaptureOutput(video) ?: error("File storage unavailable")

    suspend fun discardCapture(uri: Uri) = fileStore?.discardCapture(uri)

    suspend fun importMedia(ownerType: String, ownerKey: String, sources: List<Uri>): MediaImportResult {
        if (sources.isEmpty()) return MediaImportResult(null, 0)
        val owner = Owner(ownerType, ownerKey)
        val chronicle = mutex(owner).withLock {
            requireMutable(owner)
            getOrCreateUnlocked(ownerType, ownerKey)
        }
        val imported = sources.map { source ->
            runCatching { fileStore?.importMedia(chronicle.id, source) ?: error("File storage unavailable") }
                .getOrNull()
        }
        val successful = imported.filterNotNull()
        if (successful.isEmpty()) return MediaImportResult(null, sources.size)
        val now = System.currentTimeMillis()
        val rows = successful.mapIndexed { index, stored ->
            ChronicleMediaItemEntity(UUID.randomUUID().toString(), "", index, stored.relativePath,
                stored.mimeType, stored.durationMs, stored.width, stored.height, createdAt = now)
        }
        return MediaImportResult(addMedia(ownerType, ownerKey, rows), sources.size - successful.size)
    }

    suspend fun importAudio(ownerType: String, ownerKey: String, source: Uri): String {
        val owner = Owner(ownerType, ownerKey)
        val chronicle = mutex(owner).withLock {
            requireMutable(owner)
            getOrCreateUnlocked(ownerType, ownerKey)
        }
        val stored = fileStore?.importAudio(chronicle.id, source) ?: error("File storage unavailable")
        return try {
            mutex(owner).withLock {
                requireMutable(owner)
                database.withTransaction {
                    check(dao.find(ownerType, ownerKey)?.id == chronicle.id)
                    val now = System.currentTimeMillis()
                    val id = UUID.randomUUID().toString()
                    dao.insertMoment(ChronicleMomentEntity(id, chronicle.id, ChronicleMomentType.AUDIO,
                        dao.moments(chronicle.id).size, audioPath = stored.relativePath,
                        displayName = stored.displayName, mimeType = stored.mimeType, durationMs = stored.durationMs,
                        createdAt = now, updatedAt = now))
                    id
                }
            }
        } catch (failure: Exception) {
            fileStore?.deleteIfOwned(stored.relativePath)
            throw failure
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeContent(ownerType: String, ownerKey: String): Flow<ContentSnapshot> =
        dao.observe(ownerType, ownerKey).transformLatest { chronicle ->
            if (chronicle == null) emit(ContentSnapshot(null, emptyList()))
            else combine(dao.observeMoments(chronicle.id), dao.observeMedia(chronicle.id)) { moments, media ->
                val byMoment = media.groupBy { it.momentId }
                ContentSnapshot(chronicle, moments.map { it.toUi(byMoment[it.id].orEmpty()) })
            }.collect(::emit)
        }
    private suspend fun getOrCreateUnlocked(ownerType: String, ownerKey: String): ChronicleEntity =
        database.withTransaction {
            dao.find(ownerType, ownerKey) ?: run {
                val now = System.currentTimeMillis()
                ChronicleEntity(UUID.randomUUID().toString(), ownerType, ownerKey, "", now, now)
                    .also { dao.insertChronicle(it) }
            }
        }

    suspend fun getOrCreate(ownerType: String, ownerKey: String): ChronicleEntity {
        val owner = Owner(ownerType, ownerKey)
        return mutex(owner).withLock {
            requireMutable(owner)
            getOrCreateUnlocked(ownerType, ownerKey)
        }
    }

    suspend fun setDraft(ownerType: String, ownerKey: String, text: String) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            requireMutable(owner)
            val chronicle = getOrCreateUnlocked(ownerType, ownerKey)
            dao.updateChronicle(chronicle.copy(draftText = text, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addText(ownerType: String, ownerKey: String, text: String): String? {
        if (text.isBlank()) return null
        val owner = Owner(ownerType, ownerKey)
        return mutex(owner).withLock {
            requireMutable(owner)
            database.withTransaction {
            val chronicle = getOrCreateUnlocked(ownerType, ownerKey)
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            dao.insertMoment(
                ChronicleMomentEntity(
                    id = id,
                    chronicleId = chronicle.id,
                    type = ChronicleMomentType.TEXT,
                    position = dao.moments(chronicle.id).size,
                    text = text,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.compareAndSetDraft(chronicle.id, text, "", now)
            id
            }
        }
    }

    suspend fun addMedia(
        ownerType: String,
        ownerKey: String,
        items: List<ChronicleMediaItemEntity>
    ): String {
        require(items.isNotEmpty())
        val owner = Owner(ownerType, ownerKey)
        return try {
            mutex(owner).withLock {
                requireMutable(owner)
                database.withTransaction {
                    val chronicle = getOrCreateUnlocked(ownerType, ownerKey)
                    val now = System.currentTimeMillis()
                    val momentId = UUID.randomUUID().toString()
                    dao.insertMoment(ChronicleMomentEntity(momentId, chronicle.id, ChronicleMomentType.MEDIA,
                        dao.moments(chronicle.id).size, createdAt = now, updatedAt = now))
                    dao.insertMedia(items.mapIndexed { index, item -> item.copy(momentId = momentId, position = index) })
                    momentId
                }
            }
        } catch (failure: Exception) {
            cleanupOwnedPaths(items.flatMap { listOfNotNull(it.localPath, it.thumbnailPath) })
            throw failure
        }
    }

    suspend fun replaceMedia(ownerType: String, ownerKey: String, momentId: String, items: List<ChronicleMediaItemEntity>) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            requireMutable(owner)
            val chronicle = dao.find(ownerType, ownerKey) ?: error("Chronicle owner is unavailable")
            check(dao.moments(chronicle.id).any { it.id == momentId && it.type == ChronicleMomentType.MEDIA })
            val previousPaths = dao.media(momentId)
                .flatMap { listOfNotNull(it.localPath, it.thumbnailPath) }
                .toSet()
            try {
                dao.replaceMedia(momentId, items)
            } catch (failure: Exception) {
                val prior = previousPaths
                cleanupOwnedPaths(items.flatMap { listOfNotNull(it.localPath, it.thumbnailPath) }
                    .filterNot(prior::contains))
                throw failure
            }
            val finalPaths = items.flatMap { listOfNotNull(it.localPath, it.thumbnailPath) }.toSet()
            (previousPaths - finalPaths).forEach { path ->
                if (dao.mediaPathReferenceCount(path) == 0) fileStore?.deleteIfOwned(path)
            }
        }
    }

    suspend fun updateText(ownerType: String, ownerKey: String, moment: ChronicleMomentEntity, text: String) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            requireMutable(owner)
            require(moment.type == ChronicleMomentType.TEXT && text.isNotBlank())
            check(dao.find(ownerType, ownerKey)?.id == moment.chronicleId)
            dao.updateMoment(moment.copy(text = text, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMoment(ownerType: String, ownerKey: String, moment: ChronicleMomentEntity) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            requireMutable(owner)
            check(dao.find(ownerType, ownerKey)?.id == moment.chronicleId)
            val ownedPaths = buildList {
                addAll(dao.media(moment.id).flatMap { listOfNotNull(it.localPath, it.thumbnailPath) })
                moment.audioPath?.let(::add)
            }.distinct()
            dao.deleteMomentAndNormalize(moment, System.currentTimeMillis())
            ownedPaths.forEach { path ->
                val stillReferenced = dao.mediaPathReferenceCount(path) > 0 ||
                    dao.audioPathReferenceCount(path) > 0
                if (!stillReferenced) fileStore?.deleteIfOwned(path)
            }
        }
    }

    suspend fun reorder(ownerType: String, ownerKey: String, chronicleId: String, ids: List<String>) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            requireMutable(owner)
            check(dao.find(ownerType, ownerKey)?.id == chronicleId)
            dao.reorderMoments(chronicleId, ids, System.currentTimeMillis())
        }
    }

    suspend fun discard(ownerType: String, ownerKey: String) {
        val owner = Owner(ownerType, ownerKey)
        mutex(owner).withLock {
            val chronicleId = dao.find(ownerType, ownerKey)?.id
            dao.delete(ownerType, ownerKey)
            if (chronicleId != null) fileStore?.deleteChronicle(chronicleId)
            finalizedOwners += owner
        }
    }

    /** Cleans files only after a caller has durably removed the Chronicle rows. */
    suspend fun cleanupDeletedChronicle(chronicleId: String) {
        fileStore?.deleteChronicle(chronicleId)
    }

    private suspend fun cleanupOwnedPaths(paths: List<String>) = withContext(NonCancellable) {
        paths.distinct().forEach { fileStore?.deleteIfOwned(it) }
    }

    /** Serializes the last draft mutation with an atomic owner save/promotion. */
    suspend fun <T> finalizeOwner(ownerType: String, ownerKey: String, block: suspend () -> T): T {
        val owner = Owner(ownerType, ownerKey)
        return mutex(owner).withLock {
            block().also { finalizedOwners += owner }
        }
    }

    suspend fun promoteActiveFlow(flowInstanceId: String, sessionId: Long) =
        dao.promote(
            ChronicleOwnerType.ACTIVE_FLOW,
            flowInstanceId,
            ChronicleOwnerType.SESSION,
            sessionId.toString(),
            System.currentTimeMillis()
        )

    private fun ChronicleMomentEntity.toUi(media: List<ChronicleMediaItemEntity>): ChronicleMomentUi = when (type) {
        ChronicleMomentType.TEXT -> ChronicleMomentUi.Text(id, chronicleId, position, text.orEmpty())
        ChronicleMomentType.MEDIA -> ChronicleMomentUi.Media(id, chronicleId, position, media.sortedBy { it.position }.map { item ->
            ChronicleMediaItemUi(item.id, item.position, item.localPath, item.mimeType, item.durationMs,
                item.width, item.height, fileStore?.resolve(item.localPath)?.isFile != false)
        })
        ChronicleMomentType.VOICE -> ChronicleMomentUi.Voice(id, chronicleId, position, audioPath, durationMs,
            transcript, transcriptEdited, audioPath?.let { fileStore?.resolve(it)?.isFile } == true)
        ChronicleMomentType.AUDIO -> ChronicleMomentUi.Audio(id, chronicleId, position, audioPath, displayName,
            mimeType, durationMs, transcript, transcriptEdited,
            audioPath?.let { fileStore?.resolve(it)?.isFile } == true)
        else -> error("Unsupported Chronicle Moment type: $type")
    }
}
