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
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel

@Composable
fun SessionRewardContent(
    r: FlowRewardUiModel,
    isAera: Boolean,
    calmMode: Boolean
) {
    val titleText = stringResource(
        if (r.surgePoints > 0) {
            R.string.session_reward_title_surge_completed
        } else {
            R.string.session_reward_title_flow_completed
        }
    )
    val subtitleText = stringResource(R.string.session_reward_subtitle_logged_story)
    val sessionDetailsTitle = stringResource(R.string.session_reward_card_session_details)
    val storyInTimeSubtitle = stringResource(R.string.session_reward_card_story_in_time)
    val totalTimeLabel = stringResource(R.string.session_reward_total_time)
    val minutesText = stringResource(R.string.session_reward_minutes_value, r.minutes)
    val totalScoreTitle = stringResource(R.string.session_reward_total_score_title)
    val totalScoreFootnote = stringResource(R.string.session_reward_total_score_footnote)
    val breakdownTitle = stringResource(R.string.session_reward_breakdown_title)
    val breakdownSubtitle = stringResource(R.string.session_reward_breakdown_subtitle)
    val baseScoreLabel = stringResource(R.string.session_reward_base_score)
    val baseScoreValue = stringResource(R.string.session_reward_points_value, r.baseScyraPoints)
    val timeBonusesTitle = stringResource(R.string.session_reward_time_bonuses)
    val tenMinuteBonusLabel = stringResource(R.string.session_reward_10_min_bonus)
    val thirtyMinuteBonusLabel = stringResource(R.string.session_reward_30_min_bonus)
    val sixtyMinuteBonusLabel = stringResource(R.string.session_reward_60_min_bonus)
    val arcSectionTitle = stringResource(R.string.session_reward_arc_section)
    val arcMultiplierUsedLabel = stringResource(R.string.session_reward_arc_multiplier_used)
    val arcPointsGainedLabel = stringResource(R.string.session_reward_arc_points_gained)
    val arcPointsGainedValue = stringResource(R.string.session_reward_points_value, r.arcBonusPoints)
    val arcLevelUpText = stringResource(R.string.session_reward_arc_level_up)
    val dashText = stringResource(R.string.session_reward_dash)

    val arcMultiplierText = r.arcMultiplierUsed?.let {
        stringResource(R.string.session_reward_multiplier_1dp, it)
    } ?: dashText

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )
        }

        if (calmMode || isAera) {
            RewardCard(
                title = sessionDetailsTitle,
                subtitle = storyInTimeSubtitle
            ) {
                MetricLine(totalTimeLabel, minutesText, MetricTone.Neutral)
            }
            return
        }

        RewardChipRowV2(
            isAera = isAera,
            totalMinutes = r.minutes,
            totalScyra = r.finalScyraPoints,
            surgePoints = r.surgePoints
        )

        RewardTotalCard(
            title = totalScoreTitle,
            value = r.finalScyraPoints,
            footnote = totalScoreFootnote
        )

        RewardCard(
            title = breakdownTitle,
            subtitle = breakdownSubtitle
        ) {
            HighlightMetric(baseScoreLabel, baseScoreValue)

            val hasAnyBonus =
                r.tenMinuteBonuses > 0 || r.thirtyMinuteBonuses > 0 || r.sixtyMinuteBonuses > 0

            if (hasAnyBonus) {
                DividerSoft()
                Text(
                    text = timeBonusesTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                if (r.tenMinuteBonuses > 0) {
                    BonusLine(tenMinuteBonusLabel, r.tenMinuteBonuses, 5)
                }
                if (r.thirtyMinuteBonuses > 0) {
                    BonusLine(thirtyMinuteBonusLabel, r.thirtyMinuteBonuses, 15)
                }
                if (r.sixtyMinuteBonuses > 0) {
                    BonusLine(sixtyMinuteBonusLabel, r.sixtyMinuteBonuses, 50)
                }
            }

            val showArcUi =
                (r.arcIndexInArc ?: 0) >= 2 &&
                        (r.arcBonusPoints > 0 || r.arcMultiplierUsed != null)

            if (showArcUi) {
                DividerSoft()
                Text(
                    text = arcSectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                MetricLine(
                    label = arcMultiplierUsedLabel,
                    value = arcMultiplierText,
                    tone = if (r.arcMultiplierUsed != null) MetricTone.Glow else MetricTone.Muted
                )

                HighlightMetric(arcPointsGainedLabel, arcPointsGainedValue, glow = true)

                if (r.arcDidLevelUp) {
                    Text(
                        text = arcLevelUpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}