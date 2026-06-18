package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel

@Composable
fun SoftSessionRewardContent(r: FlowRewardUiModel) {
    val text = rememberRewardRevealTextProvider()
    RewardRevealDeck(
        cards = buildSoftRewardCards(r, text),
        modifier = Modifier.fillMaxWidth()
    )
}
