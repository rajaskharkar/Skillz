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

@Singleton
class ShellRewardOrchestrator @Inject constructor(
    private val shellRepository: ShellRepository
) {
    suspend fun onSessionCompleted(session: SessionEntity): ShellRewardResult {
        val sourceId = session.id.toString()
        val minutes = (session.durationMs / MILLIS_PER_MINUTE).toInt().coerceAtLeast(0)
        return if (session.isSoftMode) {
            val units = minutes * 10L
            if (!shellRepository.addStillwater(units, "session", sourceId)) {
                return ShellRewardResult()
            }
            ShellRewardResult(stillwaterUnits = units)
        } else {
            val grantedFinds = mutableListOf<String>()
            val badges = mutableListOf<String>()
            val discoveries = mutableListOf<String>()

            if (!shellRepository.addPearls(session.scyraPoints, "flow_reward", "session", sourceId)) {
                return ShellRewardResult()
            }

            suspend fun thresholdReward(badgeId: String, findId: String) {
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
            }

            if (minutes >= 10) thresholdReward("badge_flow_10_min", ShellContentCatalog.FOCUS_GLOW_SHELL)
            if (minutes >= 30) thresholdReward("badge_flow_30_min", ShellContentCatalog.FOCUS_CURRENT_CONCH)
            if (minutes >= 60) thresholdReward("badge_flow_60_min", ShellContentCatalog.FOCUS_ANCHOR_CORAL)
            if (minutes >= 120) thresholdReward("badge_flow_120_min", ShellContentCatalog.FOCUS_ABYSS_LANTERNFISH)

            if (minutes >= 30 && shellRepository.grantDiscoveryOnce("discovery_threshold_seahorse", "session", sourceId) != null) {
                discoveries += "discovery_threshold_seahorse"
                grantedFinds += ShellContentCatalog.FOCUS_THRESHOLD_SEAHORSE
            }

            val previousEnd = shellRepository.lastRegularFlowBefore(session.endTime)
            if (previousEnd != null && session.endTime - previousEnd >= RETURN_GAP_MS &&
                shellRepository.grantDiscoveryOnce("discovery_return_turtle_stone", "session", sourceId) != null
            ) {
                discoveries += "discovery_return_turtle_stone"
                grantedFinds += ShellContentCatalog.FOCUS_RETURN_TURTLE_STONE
            }

            val count = shellRepository.regularFlowCount()
            if (discoveries.isEmpty() && count > 0 && count % 3 == 0) {
                val discoveryId = if ((count / 3) % 2 == 0) "discovery_pearl_cluster" else "discovery_sea_glass_shard"
                if (shellRepository.grantDiscoveryOnce(discoveryId, "session", sourceId) != null) discoveries += discoveryId
            }

            ShellRewardResult(session.scyraPoints, 0, grantedFinds.distinct(), badges.distinct(), discoveries.distinct())
        }
    }
}
