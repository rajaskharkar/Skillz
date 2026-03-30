package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowPlanRepository @Inject constructor(
    private val flowPlanDao: FlowPlanDao
) {

    fun getActiveFlowPlans(): Flow<List<FlowPlanEntity>> =
        flowPlanDao.getActiveFlowPlans()

    fun getArchivedFlowPlans(): Flow<List<FlowPlanEntity>> =
        flowPlanDao.getArchivedFlowPlans()

    suspend fun getFlowPlanById(id: Long): FlowPlanEntity? =
        flowPlanDao.getFlowPlanById(id)

    suspend fun createFlowPlan(
        title: String,
        tagId: Long?,
        isSoftMode: Boolean
    ): Long {
        return flowPlanDao.insertFlowPlan(
            FlowPlanEntity(
                title = title.trim(),
                tagId = tagId,
                isSoftMode = isSoftMode
            )
        )
    }

    suspend fun updateFlowPlan(plan: FlowPlanEntity) {
        flowPlanDao.updateFlowPlan(
            plan.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        flowPlanDao.setPinned(id = id, pinned = pinned)
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        flowPlanDao.setArchived(id = id, archived = archived)
    }

    suspend fun markLaunched(id: Long) {
        flowPlanDao.markLaunched(id = id)
    }

    suspend fun deleteFlowPlanById(id: Long) {
        flowPlanDao.deleteFlowPlanById(id)
    }
}