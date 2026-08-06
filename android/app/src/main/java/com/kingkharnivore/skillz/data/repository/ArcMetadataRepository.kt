package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.ArcMetadataDao
import com.kingkharnivore.skillz.data.model.entity.ArcMetadataEntity
import com.kingkharnivore.skillz.model.ArcMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ArcMetadataRepository @Inject constructor(private val dao: ArcMetadataDao) {
    fun observeAll(): Flow<Map<Long, ArcMetadata>> = dao.observeAll().map { rows ->
        rows.associate { it.arcId to it.toDomain() }
    }

    suspend fun get(arcId: Long): ArcMetadata? = dao.get(arcId)?.toDomain()

    suspend fun save(metadata: ArcMetadata) {
        require(!metadata.isEmpty)
        val now = System.currentTimeMillis()
        val createdAt = dao.get(metadata.arcId)?.createdAtEpochMillis ?: now
        dao.upsert(
            ArcMetadataEntity(
                arcId = metadata.arcId, title = metadata.title, summary = metadata.summary,
                outcome = metadata.outcome, highlight = metadata.highlight, nextStep = metadata.nextStep,
                createdAtEpochMillis = createdAt, updatedAtEpochMillis = now
            )
        )
    }

    suspend fun clear(arcId: Long) = dao.delete(arcId)

    private fun ArcMetadataEntity.toDomain() = ArcMetadata(arcId, title, summary, outcome, highlight, nextStep)
}
