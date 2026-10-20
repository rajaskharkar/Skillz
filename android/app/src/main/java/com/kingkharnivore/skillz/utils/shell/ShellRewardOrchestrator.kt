package com.kingkharnivore.skillz.utils.shell

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.SessionShellRewardEntity
import com.kingkharnivore.skillz.data.model.entity.shell.PebbleLedgerEntity
import com.kingkharnivore.skillz.data.model.entity.shell.isArcAnimalGrant
import com.kingkharnivore.skillz.model.FlowMode

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCompletionProcessor
import javax.inject.Inject
import javax.inject.Singleton

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_SECOND = 1_000L

data class ShellRewardResult(
    val pearlsEarned: Int = 0,
    val stillwaterUnits: Long = 0,
    val grantedFindIds: List<String> = emptyList(),
    val badgeIds: List<String> = emptyList(),
    val discoveryIds: List<String> = emptyList(),
    val pebblesEarned: Int = 0
)

object ShellRewardPolicy {
    /** Flow duration grants sea creatures; land Arc rewards are handled separately. */
    fun milestoneFindsForMinutes(
        minutes: Int,
        mode: FlowMode = FlowMode.FLOW
    ): List<String> = CreatureEconomy.creaturesForRegularFlowMinutes(minutes, isSoftFlow = mode == FlowMode.SOFT)
        .flatMap { reward -> List(reward.quantity) { reward.creatureId } }
}

@Singleton
class ShellRewardOrchestrator @Inject constructor(
    private val db: SkillzDatabase,
    private val shellRepository: ShellRepository,
    private val shellRewardEventRecorder: ShellRewardEventRecorder,
    private val objectiveCompletionProcessor: ObjectiveCompletionProcessor
) {
    suspend fun retryPending() {
        db.powerRewardDao().pending(System.currentTimeMillis() - 60_000L).forEach { id ->
            try {
                val session = db.sessionDao().getSessionById(id)
                if (session == null) db.powerRewardDao().discardPending(id)
                else onSessionCompleted(session)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // A failing receipt must not starve later sessions, including the next batch.
                db.powerRewardDao().defer(id, System.currentTimeMillis())
                android.util.Log.w("ShellRewardRetry", "Reward for session $id remains pending", failure)
            }
        }
    }

    suspend fun onSessionCompleted(completedSession: SessionEntity): ShellRewardResult {
      val result = db.withTransaction {
        // A delayed Health sync may have committed while this callback waited for Room.
        // Read the authoritative score under the same transaction as the currency grant.
        val session = db.sessionDao().getSessionById(completedSession.id) ?: return@withTransaction ShellRewardResult()
        val ledger = db.powerRewardDao()
        val receipt = ledger.reward(session.id)
        if (receipt?.completed == true) {
            val events = db.shellRewardEventDao().getEventsForSession(session.id).filterNot { it.isArcAnimalGrant }
            return@withTransaction ShellRewardResult(
                pearlsEarned = receipt.pearls, pebblesEarned = receipt.pebbles,
                stillwaterUnits = events.filter { it.rewardType == "STILLWATER_ADDED" }.sumOf { it.quantity },
                badgeIds = events.filter { it.rewardType == "BADGE_UPDATED" }.mapNotNull { it.rewardId },
                discoveryIds = events.filter { it.rewardType == "DISCOVERY_RECORDED" }.mapNotNull { it.rewardId },
                grantedFindIds = events.filter { it.rewardType == "ANIMAL_GRANTED" }.flatMap { event -> List(event.quantity.toInt()) { requireNotNull(event.rewardId) } }
            )
        }

        val badgesBefore = db.userBadgeDao().getAll().filter { it.count > 0 }.map { it.badgeId }.toSet()
        val sourceId = session.id.toString()
        val minutes = (session.durationMs / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
        val result = if (session.isSoftMode) {
            val durationSeconds = (session.durationMs / MILLIS_PER_SECOND).coerceAtLeast(0L)
            val drops = calculateDropsForSoftFlow(durationSeconds)
            if (!shellRepository.addStillwater(drops, "session", sourceId)) {
                ShellRewardResult()
            } else {
                ShellRewardResult(stillwaterUnits = drops)
            }
        } else {
            val grantedFinds = mutableListOf<String>()
            val badges = mutableListOf<String>()
            val discoveries = mutableListOf<String>()

            if (!shellRepository.addPearls(session.scyraPoints, "flow_reward", "session", sourceId)) {
                ShellRewardResult()
            } else {
                if (minutes >= 10) {
                    shellRepository.incrementBadge("badge_flow_10_min")
                    badges += "badge_flow_10_min"
                }
                if (minutes >= 30) {
                    shellRepository.incrementBadge("badge_flow_30_min")
                    badges += "badge_flow_30_min"
                }
                if (minutes >= 60) {
                    shellRepository.incrementBadge("badge_flow_60_min")
                    badges += "badge_flow_60_min"
                }
                if (minutes >= 120) {
                    shellRepository.incrementBadge("badge_flow_120_min")
                    badges += "badge_flow_120_min"
                }

                // Include this Flow's milestone badges when reconciling badge-book collections.
                val findIds = ShellRewardPolicy.milestoneFindsForMinutes(minutes, session.mode)
                grantedFinds += shellRepository.grantFindCopies(findIds, "session", sourceId).map { it.findId }
                ShellRewardResult(session.scyraPoints, 0, grantedFinds, badges, discoveries)
            }
        }
        // Mirror the actual authoritative Pearl grant, never a separate duration formula.
        val pebbles = if (session.mode == FlowMode.POWER) result.pearlsEarned else 0
        if (pebbles > 0) ledger.insert(PebbleLedgerEntity("power:${session.id}", pebbles, session.id.toString(), System.currentTimeMillis()))
        // A creature batch already reconciled session and collection badges atomically.
        if (result.grantedFindIds.isEmpty()) shellRepository.reconcilePowerBadges()
        val newBadges = db.userBadgeDao().getAll().filter {
            it.count > 0 && it.badgeId !in badgesBefore
        }.map { it.badgeId }
        val rewarded = result.copy(pebblesEarned = pebbles, badgeIds = (result.badgeIds + newBadges).distinct())
        shellRewardEventRecorder.recordSessionRewards(session, rewarded)
        ledger.enqueue(SessionShellRewardEntity(sessionId = session.id, queuedAt = System.currentTimeMillis()))
        ledger.update(SessionShellRewardEntity(session.id, true, result.pearlsEarned, pebbles, receipt?.queuedAt ?: System.currentTimeMillis()))
        rewarded
      }
      // Objective reconciliation has its own mutex: never acquire it inside a Room transaction.
      objectiveCompletionProcessor.processCompletedSession(completedSession)
      return result
    }
}
