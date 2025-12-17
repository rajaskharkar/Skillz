package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.OngoingSessionDao
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowSessionRepository @Inject constructor(
    private val ongoingSessionDao: OngoingSessionDao
){
    fun getOngoingSession(): Flow<OngoingSessionEntity?> =
        ongoingSessionDao.getOngoingSession()

    suspend fun saveOngoingSession(entity: OngoingSessionEntity) {
        ongoingSessionDao.upsert(entity)
    }

    suspend fun clearOngoingSession() {
        ongoingSessionDao.clear()
    }
}