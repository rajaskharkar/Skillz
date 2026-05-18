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

    fun milestoneFindsForMinutes(minutes: Int): List<String> =
        CreatureEconomy.creaturesForRegularFlowMinutes(minutes).flatMap { reward ->
            List(reward.quantity) { reward.creatureId }
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
                CreatureEconomy.creaturesForRegularFlowMinutes(minutes).forEach { reward ->
                    repeat(reward.quantity) {
                        val granted = shellRepository.grantFindCopy(reward.creatureId, "session", sourceId)
                        grantedFinds += granted.findId
                    }
                }

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

                ShellRewardResult(session.scyraPoints, 0, grantedFinds, badges, discoveries)
            }
        }
        shellRewardEventRecorder.recordSessionRewards(session, result)
        return result
    }
}
