package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.flow.formatMsAsMmSs
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel

@Composable
fun SessionRewardContent(
    r: FlowRewardUiModel,
    isAera: Boolean,
    calmMode: Boolean
) {
    val showBeamUi = r.beamBonusPoints > 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (r.surgePoints > 0) "Surge completed." else "Flow completed.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Logged into your story.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )
        }

        if (calmMode || isAera) {
            RewardCard(title = "Session details", subtitle = "Your Story in Time") {
                MetricLine("Total time", "${r.minutes} min", MetricTone.Neutral)
                if (showBeamUi) {
                    MetricLine("Time in Beam ⭐", formatMsAsMmSs(r.beamEligibleMs), MetricTone.Glow)
                }
            }
            return
        }

        RewardChipRowV2(
            isAera = isAera,
            totalMinutes = r.minutes,
            totalScyra = r.finalScyraPoints,
            beamBonusPoints = r.beamBonusPoints,
            showBeamUi = showBeamUi,
            surgePoints = r.surgePoints
        )

        RewardTotalCard(
            title = "Total Scyra Score",
            value = r.finalScyraPoints,
            footnote = "This Flow"
        )

        RewardCard(title = "Breakdown", subtitle = "How your score was built") {
            HighlightMetric("Base Scyra score", "+${r.baseScyraPoints}")

            val hasAnyBonus =
                r.tenMinuteBonuses > 0 || r.thirtyMinuteBonuses > 0 || r.sixtyMinuteBonuses > 0

            if (hasAnyBonus) {
                DividerSoft()
                Text(
                    text = "Time bonuses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                if (r.tenMinuteBonuses > 0) BonusLine("10-minute bonus", r.tenMinuteBonuses, 5)
                if (r.thirtyMinuteBonuses > 0) BonusLine("30-minute bonus", r.thirtyMinuteBonuses, 15)
                if (r.sixtyMinuteBonuses > 0) BonusLine("60-minute bonus", r.sixtyMinuteBonuses, 50)
            }

            if (showBeamUi) {
                DividerSoft()
                Text(
                    text = "Beam ⭐",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                MetricLine("Time in Beam", formatMsAsMmSs(r.beamEligibleMs), MetricTone.Glow)

                MetricLine(
                    "Beam multiplier",
                    r.beamMultiplier?.let { "×${"%.2f".format(it)}" } ?: "—",
                    tone = if (r.beamMultiplier != null) MetricTone.Glow else MetricTone.Muted
                )

                HighlightMetric("Beam points gained", "+${r.beamBonusPoints}", glow = true)
            }

            val showArcUi =
                (r.arcIndexInArc ?: 0) >= 2 && (r.arcBonusPoints > 0 || r.arcMultiplierUsed != null)

            if (showArcUi) {
                DividerSoft()
                Text(
                    text = "Arc 🔥",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                MetricLine(
                    label = "Arc multiplier used",
                    value = r.arcMultiplierUsed?.let { "×${"%.1f".format(it)}" } ?: "—",
                    tone = if (r.arcMultiplierUsed != null) MetricTone.Glow else MetricTone.Muted
                )

                HighlightMetric("Arc points gained", "+${r.arcBonusPoints}", glow = true)

                if (r.arcDidLevelUp) {
                    Text(
                        text = "Multiplier grew by +0.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}