package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.SessionDao
import com.kingkharnivore.skillz.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {

    fun getSessionsForSkill(skillId: Long): Flow<List<SessionEntity>> {
        return sessionDao.getSessionsForSkill(skillId)
    }

    fun getTotalTimeForSkill(skillId: Long): Flow<Long?> {
        return sessionDao.getTotalTimeForSkill(skillId)
    }

    suspend fun addSession(
        skillId: Long,
        startTime: Long,
        endTime: Long,
        durationMs: Long,
        notes: String?
    ): Long {
        val session = SessionEntity(
            skillId = skillId,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs,
            notes = notes
        )
        return sessionDao.insertSession(session)
    }
}