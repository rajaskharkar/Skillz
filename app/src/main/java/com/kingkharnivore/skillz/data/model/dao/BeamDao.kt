package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeamDao {

    @Query("""
        SELECT * FROM beams
        WHERE startTime >= :startMs AND startTime < :endMs
        ORDER BY startTime ASC
    """)
    fun observeBeamsStartingBetween(startMs: Long, endMs: Long): Flow<List<BeamEntity>>

    @Query("""
        SELECT * FROM beams
        WHERE startTime < :endMs AND endTime > :startMs
        ORDER BY startTime ASC
    """)
    fun observeBeamsOverlappingWindow(startMs: Long, endMs: Long): Flow<List<BeamEntity>>

    @Query("""
        SELECT * FROM beams
        WHERE startTime <= :nowMs AND endTime > :nowMs
        LIMIT 1
    """)
    suspend fun getActiveBeam(nowMs: Long): BeamEntity?

    // Overlap check for creating a new beam
    @Query("""
        SELECT * FROM beams
        WHERE :candidateStart < endTime AND :candidateEnd > startTime
        LIMIT 1
    """)
    suspend fun findFirstOverlap(candidateStart: Long, candidateEnd: Long): BeamEntity?

    // Overlap check for updating existing beam (exclude itself)
    @Query("""
        SELECT * FROM beams
        WHERE id != :excludeId
          AND :candidateStart < endTime AND :candidateEnd > startTime
        LIMIT 1
    """)
    suspend fun findFirstOverlapExcludingId(
        excludeId: Long,
        candidateStart: Long,
        candidateEnd: Long
    ): BeamEntity?

    @Query("SELECT * FROM beams WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BeamEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(beam: BeamEntity): Long

    @Update
    suspend fun update(beam: BeamEntity)

    @Query("DELETE FROM beams WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM beams
        WHERE startTime < :endMs AND endTime > :startMs
        ORDER BY startTime ASC
    """)
    suspend fun getBeamsOverlappingWindow(startMs: Long, endMs: Long): List<BeamEntity>

    @Query("""
    SELECT * FROM beams
    WHERE endTime > :nowMs
    ORDER BY startTime ASC
    """)
    fun observeUpcomingBeams(nowMs: Long): Flow<List<BeamEntity>>

    @Query("""
    SELECT * FROM beams
    ORDER BY startTime ASC
    """)
    fun observeAllBeams(): Flow<List<BeamEntity>>

}
