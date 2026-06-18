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
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.ui.health.MovementBonusRewardBlock

@Composable
fun SessionRewardContent(
    r: FlowRewardUiModel,
    calmMode: Boolean
) {
    val text = rememberRewardRevealTextProvider()
    val findTitles = ShellContentCatalog.finds.associate { it.findId to stringResource(it.titleRes) }
    val badgeTitles = ShellContentCatalog.badges.associate { it.badgeId to stringResource(it.titleRes) }
    val discoveryTitles = ShellContentCatalog.discoveries.associate { it.discoveryId to stringResource(it.titleRes) }
    val cards = buildSessionRewardCards(
        reward = r,
        calmMode = calmMode,
        text = text,
        findTitle = { findId -> findTitles[findId] },
        badgeTitle = { badgeId -> badgeTitles[badgeId] },
        discoveryTitle = { discoveryId -> discoveryTitles[discoveryId] }
    )
    val titleText = stringResource(
        if (r.surgePoints > 0) R.string.session_reward_title_surge_completed else R.string.session_reward_title_flow_completed
    )

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
                text = stringResource(R.string.session_reward_subtitle_logged_story),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )
        }
        MovementBonusRewardBlock(
            steps = r.movementSteps ?: 0L,
            movementPoints = r.movementPoints
        )
        RewardRevealDeck(cards = cards)
    }
}
