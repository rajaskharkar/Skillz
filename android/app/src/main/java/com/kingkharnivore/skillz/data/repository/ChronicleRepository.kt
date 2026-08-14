package com.kingkharnivore.skillz.data.repository

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class ChronicleRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val dao: ChronicleDao
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(ownerType: String, ownerKey: String): Flow<Snapshot> =
        dao.observe(ownerType, ownerKey).transformLatest { chronicle ->
            if (chronicle == null) emit(Snapshot(null, emptyList()))
            else dao.observeMoments(chronicle.id).collect { emit(Snapshot(chronicle, it)) }
        }

    fun observeSummaries() = dao.observeSummaries()
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
            dao.deleteMomentAndNormalize(moment, System.currentTimeMillis())
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
            dao.delete(ownerType, ownerKey)
            finalizedOwners += owner
        }
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
}
