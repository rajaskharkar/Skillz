package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.ActiveArcRunDao
import com.kingkharnivore.skillz.data.model.entity.ActiveArcRunEntity
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import com.kingkharnivore.skillz.utils.arc.ArcPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveArcRunRepository @Inject constructor(
    private val dao: ActiveArcRunDao,
    private val arcPrefs: ArcPrefs
) {

    fun getActiveArcRun(): Flow<ActiveArcRunEntity?> =
        dao.getActiveArcRun()

    suspend fun getActiveArcRunOnce(): ActiveArcRunEntity? =
        dao.getActiveArcRun().first()

    suspend fun startRun(
        arcPlanId: Long,
        arcTitle: String,
        currentStepIndex: Int,
        totalSteps: Int,
        currentStepTitle: String,
        currentTagName: String,
        currentIsSoftMode: Boolean
    ) {
        val now = System.currentTimeMillis()
        arcPrefs.clearPlannedFlowHandoff()
        dao.upsert(
            ActiveArcRunEntity(
                arcPlanId = arcPlanId,
                arcTitle = arcTitle,
                currentStepIndex = currentStepIndex,
                totalSteps = totalSteps,
                currentStepTitle = currentStepTitle,
                currentTagName = currentTagName,
                currentIsSoftMode = currentIsSoftMode
            )
        )
        arcPrefs.save(
            ArcRuntimeState(
                arcId = now,
                isPending = true,
                multiplier = ArcRuntimeState.BASE_MULTIPLIER,
                progressMs = 0L,
                lastSessionEndTimeMs = now,
                sessionCountInArc = 0
            )
        )
    }

    suspend fun updateCurrentStep(
        currentStepIndex: Int,
        currentStepTitle: String,
        currentTagName: String,
        currentIsSoftMode: Boolean
    ) {
        dao.updateCurrentStep(
            currentStepIndex = currentStepIndex,
            currentStepTitle = currentStepTitle,
            currentTagName = currentTagName,
            currentIsSoftMode = currentIsSoftMode
        )
    }

    suspend fun clear() {
        dao.clear()
    }
}
