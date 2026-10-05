package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel

@Composable
fun ArcSummaryContent(
    arc: ArcSummaryUiModel,
    calmMode: Boolean
) {
    val duration = formatDuration(arc.totalDurationMs)
    val findTitles = mapOf(
        ShellContentCatalog.FOCUS_MINNOW to stringResource(R.string.shell_find_minnow_title),
        ShellContentCatalog.FOCUS_SEAHORSE to stringResource(R.string.shell_find_seahorse_title),
        ShellContentCatalog.FOCUS_MANTA to stringResource(R.string.shell_find_manta_title),
        ShellContentCatalog.FOCUS_WHALE to stringResource(R.string.shell_find_whale_title),
        ShellContentCatalog.FOCUS_OCTOPUS to stringResource(R.string.shell_find_octopus_title),
        ShellContentCatalog.FOCUS_PEBBLE to stringResource(R.string.shell_find_pebble_title),
        ShellContentCatalog.TRINKET_SEA_GLASS_SHARD to stringResource(R.string.shell_find_sea_glass_title),
        ShellContentCatalog.TRINKET_GLIMMER to stringResource(R.string.shell_find_glimmer_title),
        ShellContentCatalog.FOCUS_LAMP to stringResource(R.string.shell_object_lamp_title),
        ShellContentCatalog.FOCUS_PERCH to stringResource(R.string.shell_object_perch_title),
        ShellContentCatalog.FOCUS_PEBBLES to stringResource(R.string.shell_object_pebbles_title),
        ShellContentCatalog.FOCUS_CURTAIN to stringResource(R.string.shell_object_curtain_title),
        ShellContentCatalog.FOCUS_BUBBLES to stringResource(R.string.shell_object_bubbles_title)
    )
    val rewardIds = arc.shellSummary.animals.map { it.id } +
        com.kingkharnivore.skillz.utils.shell.LandArcRewards.forFlowCount(arc.totalSessions).map { it.creatureId }
    val creatureTitles = rewardIds.distinct().mapNotNull { id ->
        com.kingkharnivore.skillz.utils.shell.CreatureCatalog.get(id)?.let { id to stringResource(it.titleRes) }
    }.toMap()
    val badgeTitles = mapOf(
        "badge_flow_10_min" to stringResource(R.string.shell_badge_flow_10_title),
        "badge_flow_30_min" to stringResource(R.string.shell_badge_flow_30_title),
        "badge_flow_60_min" to stringResource(R.string.shell_badge_flow_60_title),
        "badge_flow_120_min" to stringResource(R.string.shell_badge_flow_120_title)
    )
    val powerBadgeTitles = com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog.specs.associate { it.id to stringResource(it.titleRes) }
    val discoveryTitles = mapOf(
        "discovery_sea_glass_shard" to stringResource(R.string.shell_find_sea_glass_title),
        "discovery_glimmer" to stringResource(R.string.shell_find_glimmer_title),
        "discovery_octopus" to stringResource(R.string.shell_find_octopus_title),
        "discovery_pebble" to stringResource(R.string.shell_find_pebble_title)
    )
    // TODO(Movement Bonus): Arc-level Movement summary is intentionally deferred;
    // Flow reward and FlowCard movement displays are implemented in V1.
    val cards = buildArcSummaryRewardCards(
        arc = arc,
        calmMode = calmMode,
        text = rememberRewardRevealTextProvider(),
        durationText = duration,
        findTitle = { findTitles[it] ?: creatureTitles[it] },
        badgeTitle = { powerBadgeTitles[it] ?: badgeTitles[it] },
        discoveryTitle = { discoveryTitles[it] }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.arc_summary_completed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.arc_summary_totals_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )
        if (arc.landRewardsPending) {
            val packageLabel = com.kingkharnivore.skillz.utils.shell.LandArcRewards.forFlowCount(arc.totalSessions)
                .map { reward ->
                    val name = creatureTitles.getValue(reward.creatureId)
                    if (reward.quantity == 1) name else stringResource(R.string.land_reward_quantity, reward.quantity, name)
                }.joinToString(" + ")
            if (packageLabel.isNotEmpty()) {
                Text(packageLabel, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.land_rewards_pending), style = MaterialTheme.typography.bodySmall)
            }
        }
        RewardRevealDeck(cards = cards)
    }
}

@Composable
private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        stringResource(R.string.arc_summary_duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.arc_summary_duration_minutes, minutes)
    }
}
