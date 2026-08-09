package com.kingkharnivore.skillz.domain.lookout

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.shell.ObjectiveProcessedSessionDao
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveProcessedSessionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveSourceFlow
import com.kingkharnivore.skillz.data.repository.shell.LookoutRepository
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveKind
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Durable, UI-independent materialization of Objective occurrences from saved sessions. */
@Singleton
class ObjectiveCompletionProcessor @Inject constructor(
    private val database: SkillzDatabase,
    private val sessionDao: SessionDao,
    private val processedSessionDao: ObjectiveProcessedSessionDao,
    private val lookoutRepository: LookoutRepository,
    private val calculator: ObjectiveProgressCalculator
) {
    suspend fun createObjectiveAndReconcile(objective: ObjectiveEntity): Long = database.withTransaction {
        val objectiveId = lookoutRepository.insertObjective(objective)
        reconcileObjectiveInTransaction(objective.copy(id = objectiveId), Instant.now(), ZoneId.systemDefault())
        objectiveId
    }

    suspend fun reconcileNewObjective(objectiveId: Long): Boolean = database.withTransaction {
        val objective = lookoutRepository.getObjective(objectiveId) ?: return@withTransaction false
        reconcileObjectiveInTransaction(objective, Instant.now(), ZoneId.systemDefault())
    }

    suspend fun processCompletedSession(savedSession: SessionEntity) {
        if (savedSession.isSoftMode) return
        reconcileUnprocessedSessions()
    }

    /** Startup and hot-path entry point; work scales with unprocessed sessions, not lifetime history. */
    suspend fun reconcileUnprocessedSessions(): Int {
        val pending = processedSessionDao.getUnprocessedRegularSessions()
        if (pending.isEmpty()) return 0
        val zoneId = ZoneId.systemDefault()
        pending.forEachIndexed { index, session ->
            processOne(
                session, zoneId,
                if (index == pending.lastIndex) Instant.now() else Instant.ofEpochMilli(session.endTime),
                rebuildAllStats = index == pending.lastIndex
            )
        }
        return pending.size
    }

    private suspend fun processOne(
        session: SessionEntity,
        zoneId: ZoneId,
        statsAsOf: Instant,
        rebuildAllStats: Boolean
    ) = database.withTransaction {
        val activeObjectives = lookoutRepository.getActiveObjectives()
        val objectives = activeObjectives.filter { it.journeyId == session.tagId }
        val skipped = lookoutRepository.getSkippedCycles()
        val completions = lookoutRepository.getCompletions().toMutableList()
        val eventTime = Instant.ofEpochMilli(session.endTime)
        val completedObjectiveIds = mutableSetOf<Long>()

        objectives.forEach { objective ->
            val window = calculator.windowFor(objective, eventTime, zoneId)
            val alreadyCompleted = completions.any {
                it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs
            }
            val wasSkipped = skipped.any {
                it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs
            }
            if (!alreadyCompleted && !wasSkipped && session.endTime in window.startMs until window.endMs) {
                val windowSessions = sessionDao.getRegularSessionsForObjectiveWindow(
                    objective.journeyId, window.startMs, window.endMs, session.endTime, session.id
                )
                calculator.completionEvidence(
                    objective, windowSessions.map { it.toObjectiveSourceFlow() }, window
                )?.let { evidence ->
                    val streakBefore = RecurringObjectiveStatsCalculator.streakBefore(
                        objective, completions, skipped, window.startMs
                    )
                    val grant = calculator.buildGrant(
                        objective,
                        ObjectiveKind.fromStorage(objective.objectiveType),
                        window,
                        evidence.achievedDurationMs,
                        evidence.completedAtMs,
                        streakBefore
                    )
                    if (lookoutRepository.applyCompletionGrant(grant.completion, null, null, null)) {
                        completions += grant.completion
                        completedObjectiveIds += objective.id
                    }
                }
            }

        }
        val statsObjectives = if (rebuildAllStats) activeObjectives else objectives.filter { it.id in completedObjectiveIds }
        statsObjectives.forEach { objective ->
            if (ObjectiveKind.fromStorage(objective.objectiveType) == ObjectiveKind.Recurring) {
                val stats = RecurringObjectiveStatsCalculator.derive(
                    objective, completions, skipped, statsAsOf, zoneId, calculator
                )
                lookoutRepository.updateRecurringStats(objective.id, stats, session.endTime)
            }
        }
        processedSessionDao.markProcessed(ObjectiveProcessedSessionEntity(session.id, System.currentTimeMillis()))
    }

    private suspend fun reconcileObjectiveInTransaction(
        objective: ObjectiveEntity,
        now: Instant,
        zoneId: ZoneId
    ): Boolean {
        val window = calculator.windowFor(objective, now, zoneId)
        val skipped = lookoutRepository.getSkippedCycles()
        if (skipped.any {
                it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs
            }) return false
        val completions = lookoutRepository.getCompletions().toMutableList()
        if (completions.any {
                it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs
            }) return false
        val sessions = sessionDao.getRegularSessionsForObjectiveWindow(
            objective.journeyId, window.startMs, window.endMs, now.toEpochMilli(), Long.MAX_VALUE
        )
        val evidence = calculator.completionEvidence(
            objective, sessions.map { it.toObjectiveSourceFlow() }, window
        ) ?: return false
        val streakBefore = RecurringObjectiveStatsCalculator.streakBefore(
            objective, completions, skipped, window.startMs
        )
        val grant = calculator.buildGrant(
            objective, ObjectiveKind.fromStorage(objective.objectiveType), window,
            evidence.achievedDurationMs, evidence.completedAtMs, streakBefore
        )
        if (!lookoutRepository.applyCompletionGrant(grant.completion, null, null, null)) return false
        completions += grant.completion
        if (ObjectiveKind.fromStorage(objective.objectiveType) == ObjectiveKind.Recurring) {
            val stats = RecurringObjectiveStatsCalculator.derive(
                objective, completions, skipped, now, zoneId, calculator
            )
            lookoutRepository.updateRecurringStats(objective.id, stats, evidence.completedAtMs)
        }
        return true
    }

    private fun SessionEntity.toObjectiveSourceFlow() = ObjectiveSourceFlow(
        id, tagId, startTime, endTime, durationMs, isSoftMode
    )
}
