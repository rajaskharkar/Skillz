package com.kingkharnivore.skillz.utils.arc

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventTypes
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.utils.shell.LandArcRewards
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** The final close is after the existing continuation window, not the resumable summary. */
@Singleton
class ArcLandRewardFinalizer @Inject constructor(
    private val db: SkillzDatabase,
    private val shell: ShellRepository,
    private val prefs: ArcPrefs
) {
    fun observeReward(arcId: Long) = db.arcLandRewardDao().observe(arcId)

    suspend fun isPending(arcId: Long): Boolean = com.kingkharnivore.skillz.BuildConfig.LAND_ENABLED &&
        db.arcLandRewardDao().get(arcId)?.let { it.finalizedAt == null } == true

    private val lifecycleMutex = Mutex()
    private var startingArcId: Long? = null

    // Capturing the Flow-start timestamp and reserving its Arc must serialize with expiry.
    suspend fun startFlow(block: suspend () -> ArcFlowStart): ArcFlowStart = lifecycleMutex.withLock {
        block().also { startingArcId = it.arc?.arcId }
    }

    suspend fun flowEnded() = lifecycleMutex.withLock { startingArcId = null }

    suspend fun finalizeExpired(now: Long = System.currentTimeMillis()) = lifecycleMutex.withLock {
        if (!com.kingkharnivore.skillz.BuildConfig.LAND_ENABLED) return@withLock
        val ongoingArc = db.ongoingSessionDao().getOngoingSession().first()?.arcId
        val active = prefs.loadActive()
        val recent = prefs.loadRecentlyEnded()
        db.withTransaction {
            db.arcLandRewardDao().pending(now - ArcRules.GRACE_WINDOW_MS).forEach { pending ->
                if (pending.arcId == startingArcId || pending.arcId == ongoingArc) return@forEach
                // Arc may have refreshed its grace window without creating another Flow.
                val runtime = listOfNotNull(active, recent).filter { it.arcId == pending.arcId }
                if (runtime.any { ArcContinuationResolver.isWithinContinuationWindow(it, now) }) return@forEach
                val sessions = db.sessionDao().getSessionsForArc(pending.arcId)
                val finalCount = sessions.size
                val last = sessions.maxByOrNull { it.endTime }
                if (last != null && now - last.endTime <= ArcRules.GRACE_WINDOW_MS) return@forEach
                val rewards = LandArcRewards.forFlowCount(finalCount)
                rewards.forEach { reward ->
                    repeat(reward.quantity) {
                        shell.grantFindCopy(reward.creatureId, "arc", pending.arcId.toString())
                    }
                    db.shellRewardEventDao().insertAll(listOf(ShellRewardEventEntity(
                        id = "arc_land:${pending.arcId}:${reward.creatureId}",
                        sourceSessionId = last?.id ?: pending.finalSessionId,
                        arcId = pending.arcId, rewardType = ShellRewardEventTypes.ANIMAL_GRANTED,
                        rewardId = reward.creatureId, quantity = reward.quantity.toLong(), occurredAt = now
                    )))
                }
                db.arcLandRewardDao().save(pending.copy(flowCount = finalCount, finalizedAt = now))
            }
        }
    }
}
