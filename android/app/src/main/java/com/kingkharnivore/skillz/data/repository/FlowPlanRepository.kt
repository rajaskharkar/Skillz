package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.FlowPlanDao
import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.model.FlowMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowPlanRepository @Inject constructor(
    private val flowPlanDao: FlowPlanDao
) {

    fun observeAll() = flowPlanDao.observeAll()

    fun getActiveFlowPlans(): Flow<List<FlowPlanEntity>> =
        flowPlanDao.getActiveFlowPlans()

    fun getArchivedFlowPlans(): Flow<List<FlowPlanEntity>> =
        flowPlanDao.getArchivedFlowPlans()

    suspend fun getFlowPlanById(id: Long): FlowPlanEntity? =
        flowPlanDao.getFlowPlanById(id)

    suspend fun createFlowPlan(
        title: String,
        tagId: Long?,
        isSoftMode: Boolean,
        targetMinutes: Int?,
        launchWithSurge: Boolean,
        kind: String = com.kingkharnivore.skillz.data.model.entity.HorizonKind.HABIT,
        mode: FlowMode = FlowMode.fromSoft(isSoftMode)
    ): Long {
        val normalizedTargetMinutes = targetMinutes?.takeIf { it > 0 }
        val normalizedLaunchWithSurge =
            mode != FlowMode.SOFT && normalizedTargetMinutes != null && launchWithSurge

        return flowPlanDao.insertFlowPlan(
            FlowPlanEntity(
                kind = kind,
                title = title.trim(),
                tagId = tagId,
                isSoftMode = mode == FlowMode.SOFT,
                mode = mode,
                targetMinutes = normalizedTargetMinutes,
                launchWithSurge = normalizedLaunchWithSurge
            )
        )
    }

    suspend fun updateFlowPlan(plan: FlowPlanEntity) {
        val normalizedTargetMinutes = plan.targetMinutes?.takeIf { it > 0 }
        val normalizedLaunchWithSurge =
            plan.mode != FlowMode.SOFT && normalizedTargetMinutes != null && plan.launchWithSurge

        check(flowPlanDao.updateDetails(plan.id, plan.title.trim(), plan.tagId, plan.kind,
            plan.mode == FlowMode.SOFT, plan.mode, normalizedTargetMinutes, normalizedLaunchWithSurge,
            System.currentTimeMillis()) == 1) { "This activity has already been completed or removed." }
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