package com.kingkharnivore.skillz.utils.shell

import com.kingkharnivore.skillz.data.model.dao.shell.ShellRewardEventDao
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ShellRewardEventTypes
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.model.state.flow.ArcShellRewardCountUiModel
import com.kingkharnivore.skillz.model.state.flow.ArcShellRewardSummaryUiModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellRewardEventRecorder @Inject constructor(
    private val shellRewardEventDao: ShellRewardEventDao
) {
    suspend fun recordSessionRewards(session: SessionEntity, reward: ShellRewardResult) {
        val now = System.currentTimeMillis()
        val events = mutableListOf<ShellRewardEventEntity>()

        fun add(type: String, rewardId: String?, quantity: Long) {
            if (quantity <= 0L) return
            events += ShellRewardEventEntity(
                id = eventId(session.id, type, rewardId),
                sourceSessionId = session.id,
                arcId = session.arcId,
                rewardType = type,
                rewardId = rewardId,
                quantity = quantity,
                occurredAt = now
            )
        }

        if (session.isSoftMode) {
            add(ShellRewardEventTypes.STILLWATER_ADDED, null, reward.stillwaterUnits)
        } else {
            add(ShellRewardEventTypes.PEARLS_CARRIED, null, reward.pearlsEarned.toLong())
            reward.grantedFindIds.groupingBy { it }.eachCount().forEach { (findId, count) ->
                val type = when (ShellContentCatalog.find(findId)?.kind) {
                    ShellRewardKind.ANIMAL -> ShellRewardEventTypes.ANIMAL_GRANTED
                    ShellRewardKind.OBJECT -> ShellRewardEventTypes.OBJECT_GRANTED
                    ShellRewardKind.TRINKET -> ShellRewardEventTypes.TRINKET_GRANTED
                    else -> null
                }
                if (type != null) add(type, findId, count.toLong())
            }
            reward.discoveryIds.groupingBy { it }.eachCount().forEach { (discoveryId, count) ->
                add(ShellRewardEventTypes.DISCOVERY_RECORDED, discoveryId, count.toLong())
            }
            reward.badgeIds.groupingBy { it }.eachCount().forEach { (badgeId, count) ->
                add(ShellRewardEventTypes.BADGE_UPDATED, badgeId, count.toLong())
            }
        }

        if (events.isNotEmpty()) shellRewardEventDao.insertAll(events)
    }

    suspend fun summaryForArc(arcId: Long): ArcShellRewardSummaryUiModel =
        ShellRewardEventAggregator.aggregate(shellRewardEventDao.getEventsForArc(arcId))

    private fun eventId(sourceSessionId: Long, rewardType: String, rewardId: String?): String =
        "$sourceSessionId:$rewardType:${rewardId ?: NO_REWARD_ID}"

    private companion object {
        const val NO_REWARD_ID = "_"
    }
}

object ShellRewardEventAggregator {
    fun aggregate(events: List<ShellRewardEventEntity>): ArcShellRewardSummaryUiModel {
        fun countsFor(type: String): List<ArcShellRewardCountUiModel> = events
            .filter { it.rewardType == type && it.rewardId != null }
            .groupBy { it.rewardId.orEmpty() }
            .map { (id, grouped) -> ArcShellRewardCountUiModel(id, grouped.sumOf { it.quantity }.toInt()) }
            .filter { it.count > 0 }
            .sortedBy { it.id }

        val knownTypes = setOf(
            ShellRewardEventTypes.PEARLS_CARRIED,
            ShellRewardEventTypes.STILLWATER_ADDED,
            ShellRewardEventTypes.ANIMAL_GRANTED,
            ShellRewardEventTypes.OBJECT_GRANTED,
            ShellRewardEventTypes.TRINKET_GRANTED,
            ShellRewardEventTypes.DISCOVERY_RECORDED,
            ShellRewardEventTypes.BADGE_UPDATED
        )

        return ArcShellRewardSummaryUiModel(
            animals = countsFor(ShellRewardEventTypes.ANIMAL_GRANTED),
            objects = countsFor(ShellRewardEventTypes.OBJECT_GRANTED),
            trinkets = countsFor(ShellRewardEventTypes.TRINKET_GRANTED),
            badges = countsFor(ShellRewardEventTypes.BADGE_UPDATED),
            discoveries = countsFor(ShellRewardEventTypes.DISCOVERY_RECORDED),
            unknownRewards = events
                .filter { it.rewardType !in knownTypes || it.rewardId == null && it.rewardType !in setOf(ShellRewardEventTypes.PEARLS_CARRIED, ShellRewardEventTypes.STILLWATER_ADDED) }
                .groupBy { it.rewardId ?: it.rewardType }
                .map { (id, grouped) -> ArcShellRewardCountUiModel(id, grouped.sumOf { it.quantity }.toInt()) }
                .filter { it.count > 0 }
                .sortedBy { it.id },
            pearlsCarried = events.filter { it.rewardType == ShellRewardEventTypes.PEARLS_CARRIED }.sumOf { it.quantity }.toInt(),
            stillwaterAdded = events.filter { it.rewardType == ShellRewardEventTypes.STILLWATER_ADDED }.sumOf { it.quantity }
        )
    }
}
