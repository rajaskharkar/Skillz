package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val MILLIS_PER_MINUTE = 60_000L
private const val RETURN_GAP_MS = 7L * 24L * 60L * 60L * 1000L

data class ShellRewardResult(
    val pearlsEarned: Int = 0,
    val stillwaterUnits: Long = 0,
    val grantedFindIds: List<String> = emptyList(),
    val badgeIds: List<String> = emptyList(),
    val discoveryIds: List<String> = emptyList()
)

object ShellRewardPolicy {
    const val OCTOPUS_DISCOVERY_BADGE_COUNT = 3

    fun shouldDiscoverOctopus(minutes: Int, flow30BadgeCount: Int): Boolean =
        minutes >= 30 && flow30BadgeCount == OCTOPUS_DISCOVERY_BADGE_COUNT

    fun milestoneFindsForMinutes(minutes: Int): List<String> {
        val findIds = mutableListOf<String>()
        if (minutes >= 10) findIds += ShellContentCatalog.FOCUS_MINNOW
        if (minutes >= 30) findIds += ShellContentCatalog.FOCUS_SEAHORSE
        if (minutes >= 60) findIds += ShellContentCatalog.FOCUS_MANTA
        if (minutes >= 120) findIds += ShellContentCatalog.FOCUS_WHALE
        return findIds
    }
}

@Singleton
class ShellRewardOrchestrator @Inject constructor(
    private val shellRepository: ShellRepository,
    private val shellRewardEventRecorder: ShellRewardEventRecorder
) {
    suspend fun onSessionCompleted(session: SessionEntity): ShellRewardResult {
        val sourceId = session.id.toString()
        val minutes = (session.durationMs / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
        val result = if (session.isSoftMode) {
            val units = minutes * 10L
            if (!shellRepository.addStillwater(units, "session", sourceId)) {
                ShellRewardResult()
            } else {
                ShellRewardResult(stillwaterUnits = units)
            }
        } else {
            val grantedFinds = mutableListOf<String>()
            val badges = mutableListOf<String>()
            val discoveries = mutableListOf<String>()

            if (!shellRepository.addPearls(session.scyraPoints, "flow_reward", "session", sourceId)) {
                ShellRewardResult()
            } else {
                suspend fun thresholdReward(badgeId: String, findId: String): Int {
                    val badgeCount = shellRepository.incrementBadge(badgeId)
                    badges += badgeId
                    val granted = if (badgeCount == 1) {
                        shellRepository.grantFindOnce(findId, "session", sourceId)
                    } else if (badgeCount % 5 == 0) {
                        shellRepository.grantFindCopy(findId, "session", sourceId)
                    } else {
                        null
                    }
                    if (granted != null) grantedFinds += findId
                    return badgeCount
                }

                if (minutes >= 10) thresholdReward("badge_flow_10_min", ShellContentCatalog.FOCUS_MINNOW)
                val flow30BadgeCount = if (minutes >= 30) thresholdReward("badge_flow_30_min", ShellContentCatalog.FOCUS_SEAHORSE) else 0
                if (minutes >= 60) thresholdReward("badge_flow_60_min", ShellContentCatalog.FOCUS_MANTA)
                if (minutes >= 120) thresholdReward("badge_flow_120_min", ShellContentCatalog.FOCUS_WHALE)

                if (ShellRewardPolicy.shouldDiscoverOctopus(minutes, flow30BadgeCount) &&
                    shellRepository.grantDiscoveryOnce("discovery_octopus", "session", sourceId) != null
                ) {
                    discoveries += "discovery_octopus"
                    grantedFinds += ShellContentCatalog.FOCUS_OCTOPUS
                }

                val previousEnd = shellRepository.lastRegularFlowBefore(session.endTime)
                if (previousEnd != null && session.endTime - previousEnd >= RETURN_GAP_MS &&
                    shellRepository.grantDiscoveryOnce("discovery_pebble", "session", sourceId) != null
                ) {
                    discoveries += "discovery_pebble"
                    grantedFinds += ShellContentCatalog.FOCUS_PEBBLE
                }

                val count = shellRepository.regularFlowCount()
                if (discoveries.isEmpty() && count > 0 && count % 3 == 0) {
                    val discoveryId = if ((count / 3) % 2 == 0) "discovery_glimmer" else "discovery_sea_glass_shard"
                    if (shellRepository.grantDiscoveryOnce(discoveryId, "session", sourceId) != null) discoveries += discoveryId
                }

                ShellRewardResult(session.scyraPoints, 0, grantedFinds, badges, discoveries)
            }
        }
        shellRewardEventRecorder.recordSessionRewards(session, result)
        return result
    }
}
