package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.flow.RewardRevealAnimationStyle
import com.kingkharnivore.skillz.model.state.flow.RewardRevealCardType
import com.kingkharnivore.skillz.model.state.flow.RewardRevealCardUiModel
import kotlin.math.roundToInt

@Composable
fun RewardRevealDeck(
    cards: List<RewardRevealCardUiModel>,
    modifier: Modifier = Modifier
) {
    val fallbackTitle = stringResource(R.string.reward_card_shell_recorded_title)
    val fallbackBody = stringResource(R.string.reward_card_shell_recorded_body)
    val safeCards = if (cards.isEmpty()) {
        listOf(
            RewardRevealCardUiModel(
                id = "empty",
                type = RewardRevealCardType.EMPTY_SHELL_MEANING,
                title = fallbackTitle,
                body = fallbackBody,
                contentDescription = fallbackBody
            )
        )
    } else {
        cards
    }
    val pagerState = rememberPagerState(pageCount = { safeCards.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            pageSpacing = 10.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val card = safeCards[page]
            val selected = pagerState.currentPage == page
            val animatedAlpha by animateFloatAsState(if (selected) 1f else 0.72f, label = "reward-card-alpha")
            val animatedScale by animateFloatAsState(if (selected) 1f else 0.96f, label = "reward-card-scale")
            val offsetY by animateFloatAsState(if (selected) 0f else 10f, label = "reward-card-offset")
            val pageText = stringResource(R.string.reward_card_page_announcement, page + 1, safeCards.size)
            RewardRevealCard(
                card = card,
                pageDescription = "$pageText. ${card.contentDescription}",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .alpha(animatedAlpha)
                    .scale(animatedScale)
                    .offset { IntOffset(0, offsetY.roundToInt()) }
            )
        }

        Text(
            text = stringResource(R.string.reward_card_page_count, pagerState.currentPage + 1, safeCards.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )
        Row(
            modifier = Modifier.clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            safeCards.forEachIndexed { index, _ ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (selected) 18.dp else 6.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun RewardRevealCard(
    card: RewardRevealCardUiModel,
    pageDescription: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics { contentDescription = pageDescription },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RewardIcon(card)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                card.chip?.let { RewardRevealChip(it) }
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                card.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            card.body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )
            }
            Spacer(Modifier.weight(1f))
            card.destinationHint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RewardIcon(card: RewardRevealCardUiModel) {
    val icon = iconFor(card)
    val pulse by animateFloatAsState(
        targetValue = when (card.animationStyle) {
            RewardRevealAnimationStyle.NONE -> 1f
            else -> 1.08f
        },
        label = "reward-icon-pulse"
    )
    Surface(
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = when (card.type) {
            RewardRevealCardType.SCORE_BREAKDOWN,
            RewardRevealCardType.ARC_SCORE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        },
        contentColor = when (card.type) {
            RewardRevealCardType.SCORE_BREAKDOWN,
            RewardRevealCardType.ARC_SCORE -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .scale(pulse)
            )
        }
    }
}

@Composable
private fun RewardRevealChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun iconFor(card: RewardRevealCardUiModel): ImageVector = when (card.type) {
    RewardRevealCardType.ANIMAL,
    RewardRevealCardType.ARC_ANIMALS -> Icons.Outlined.Pets
    RewardRevealCardType.OBJECT,
    RewardRevealCardType.TRINKET,
    RewardRevealCardType.ARC_OBJECTS,
    RewardRevealCardType.ARC_TRINKETS -> Icons.Outlined.Inventory2
    RewardRevealCardType.STILLWATER_RESULT,
    RewardRevealCardType.STILLWATER_PERSPECTIVE -> Icons.Outlined.WaterDrop
    RewardRevealCardType.SOFT_RULE -> Icons.Outlined.Spa
    RewardRevealCardType.BADGE,
    RewardRevealCardType.ARC_BADGES -> Icons.Outlined.EmojiEvents
    else -> Icons.Outlined.AutoAwesome
}
