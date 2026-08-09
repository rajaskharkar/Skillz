package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.shell.LookoutRepository
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveSourceFlow
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Materializes Objective occurrences from the persisted session ledger, independently of UI. */
@Singleton
class ObjectiveCompletionProcessor @Inject constructor(
    private val flowRepository: FlowRepository,
    private val lookoutRepository: LookoutRepository,
    private val calculator: ObjectiveProgressCalculator
) {
    /**
     * Replays persisted regular sessions through the idempotent completion transaction. Replaying
     * earlier sessions also reconciles a process death between session save and post-save rewards.
     */
    suspend fun processCompletedSession(savedSession: SessionEntity) {
        if (savedSession.isSoftMode) return
        if (lookoutRepository.getActiveObjectives().isEmpty()) return
        val sessions = flowRepository.getAllSessions().first()
            .filterNot { it.isSoftMode }
            .filter { it.endTime <= savedSession.endTime }
            .sortedWith(compareBy<SessionEntity> { it.endTime }.thenBy { it.id })
        val flows = sessions.map { it.toObjectiveSourceFlow() }

        sessions.forEach { event ->
            val objectives = lookoutRepository.getActiveObjectives()
            val completions = lookoutRepository.getCompletions()
            val result = calculator.calculate(
                objectives = objectives,
                flows = flows.filter { it.endTimeMs <= event.endTime },
                completions = completions,
                skippedCycles = lookoutRepository.getSkippedCycles(),
                now = Instant.ofEpochMilli(event.endTime),
                zoneId = ZoneId.systemDefault()
            )
            result.completionsToGrant.forEach { grant ->
                lookoutRepository.applyCompletionGrant(
                    grant.completion,
                    grant.newCurrentStreak,
                    grant.newMaxStreak,
                    grant.newTotalCompletions
                )
            }
            result.streakResets.forEach { lookoutRepository.resetStreak(it.objectiveId) }
        }
    }

    private fun SessionEntity.toObjectiveSourceFlow() = ObjectiveSourceFlow(
        id, tagId, startTime, endTime, durationMs, isSoftMode
    )
}
