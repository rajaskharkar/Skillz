package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.BeamDao
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class BeamError(message: String) : RuntimeException(message) {
    class Overlap(val conflicting: BeamEntity) : BeamError("Beam overlaps existing beam")
    class InvalidTime(message: String) : BeamError(message)
}

@Singleton
class BeamRepository @Inject constructor(
    private val beamDao: BeamDao
) {
    fun observeBeamsOverlappingWindow(startMs: Long, endMs: Long): Flow<List<BeamEntity>> =
        beamDao.observeBeamsOverlappingWindow(startMs, endMs)

    suspend fun getActiveBeam(nowMs: Long): BeamEntity? =
        beamDao.getActiveBeam(nowMs)

    suspend fun scheduleBeam(
        tagId: Long,
        startTime: Long,
        durationMs: Long
    ): Long {
        if (durationMs <= 0L) throw BeamError.InvalidTime("Duration must be > 0")
        val endTime = startTime + durationMs
        if (endTime <= startTime) throw BeamError.InvalidTime("End time must be after start; check duration")
        // If beam ends in the past entirely, reject (prevents nonsense)
        val now = System.currentTimeMillis()
        if (endTime <= now - 60_000L) { // allow slight past tolerance if you want
            throw BeamError.InvalidTime("Beam ends in the past")
        }
        val overlap = beamDao.findFirstOverlap(startTime, endTime)
        if (overlap != null) throw BeamError.Overlap(overlap)
        return beamDao.insert(
            BeamEntity(
                tagId = tagId,
                startTime = startTime,
                endTime = endTime,
                durationMs = durationMs
            )
        )
    }

    suspend fun updateBeam(
        beamId: Long,
        tagId: Long,
        startTime: Long,
        durationMs: Long
    ) {
        if (durationMs <= 0L) throw BeamError.InvalidTime("Duration must be > 0")
        val endTime = startTime + durationMs
        if (endTime <= startTime) throw BeamError.InvalidTime("End time must be after start")
        val overlap = beamDao.findFirstOverlapExcludingId(beamId, startTime, endTime)
        if (overlap != null) throw BeamError.Overlap(overlap)
        val existing = beamDao.getById(beamId) ?: return
        beamDao.update(
            existing.copy(
                tagId = tagId,
                startTime = startTime,
                endTime = endTime,
                durationMs = durationMs
            )
        )
    }

    suspend fun deleteBeam(beamId: Long) {
        beamDao.deleteById(beamId)
    }

    suspend fun getBeamsOverlappingWindow(startMs: Long, endMs: Long): List<BeamEntity> =
        beamDao.getBeamsOverlappingWindow(startMs, endMs)

    fun observeUpcomingBeams(nowMs: Long): Flow<List<BeamEntity>> =
        beamDao.observeUpcomingBeams(nowMs)

    fun observeAllBeams(): Flow<List<BeamEntity>> = beamDao.observeAllBeams()
}
