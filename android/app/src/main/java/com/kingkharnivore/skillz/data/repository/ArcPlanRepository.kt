package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.ArcPlanDao
import com.kingkharnivore.skillz.data.model.entity.ArcPlanEntity
import com.kingkharnivore.skillz.data.model.entity.ArcPlanStepEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArcPlanRepository @Inject constructor(
    private val arcPlanDao: ArcPlanDao
) {

    fun getActiveArcPlans(): Flow<List<ArcPlanEntity>> =
        arcPlanDao.getActiveArcPlans()

    fun getArchivedArcPlans(): Flow<List<ArcPlanEntity>> =
        arcPlanDao.getArchivedArcPlans()

    fun getStudioArcPlans(): Flow<List<ArcPlanEntity>> =
        arcPlanDao.getStudioArcPlans()

    fun getStepsForArcPlan(arcPlanId: Long): Flow<List<ArcPlanStepEntity>> =
        arcPlanDao.getStepsForArcPlan(arcPlanId)

    suspend fun getArcPlanById(id: Long): ArcPlanEntity? =
        arcPlanDao.getArcPlanById(id)

    suspend fun getStepsForArcPlanOnce(arcPlanId: Long): List<ArcPlanStepEntity> =
        arcPlanDao.getStepsForArcPlanOnce(arcPlanId)

    suspend fun createArcPlan(
        title: String,
        recurrenceType: String = ArcPlanEntity.RECURRENCE_ONE_TIME,
        recurrenceDaysCsv: String = ""
    ): Long {
        return arcPlanDao.insertArcPlan(
            ArcPlanEntity(
                title = title.trim(),
                recurrenceType = recurrenceType,
                recurrenceDaysCsv = recurrenceDaysCsv
            )
        )
    }

    suspend fun createArcPlanWithSteps(
        title: String,
        steps: List<ArcPlanStepEntity>,
        isInStudio: Boolean = false,
        recurrenceType: String = ArcPlanEntity.RECURRENCE_ONE_TIME,
        recurrenceDaysCsv: String = ""
    ): Long {
        val arcPlanId = arcPlanDao.insertArcPlan(
            ArcPlanEntity(
                title = title.trim(),
                isInStudio = isInStudio,
                recurrenceType = recurrenceType,
                recurrenceDaysCsv = recurrenceDaysCsv
            )
        )

        arcPlanDao.replaceAllSteps(
            arcPlanId = arcPlanId,
            newSteps = steps.mapIndexed { index, step ->
                step.copy(
                    arcPlanId = arcPlanId,
                    orderIndex = index
                )
            }
        )

        return arcPlanId
    }

    suspend fun updateArcPlan(plan: ArcPlanEntity) {
        arcPlanDao.updateArcPlan(
            plan.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun addStep(step: ArcPlanStepEntity): Long =
        arcPlanDao.insertArcPlanStep(step)

    suspend fun updateStep(step: ArcPlanStepEntity) {
        arcPlanDao.updateArcPlanStep(
            step.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun replaceAllSteps(
        arcPlanId: Long,
        steps: List<ArcPlanStepEntity>
    ) {
        arcPlanDao.replaceAllSteps(arcPlanId, steps)
    }

    suspend fun deleteStep(stepId: Long) {
        arcPlanDao.deleteArcPlanStepById(stepId)
    }

    suspend fun setInStudio(id: Long, isInStudio: Boolean) {
        arcPlanDao.setInStudio(id, isInStudio)
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        arcPlanDao.setArchived(id, archived)
    }

    suspend fun markLaunched(id: Long) {
        arcPlanDao.markLaunched(id)
    }

    suspend fun deleteArcPlanById(id: Long) {
        arcPlanDao.deleteArcPlanById(id)
    }
}