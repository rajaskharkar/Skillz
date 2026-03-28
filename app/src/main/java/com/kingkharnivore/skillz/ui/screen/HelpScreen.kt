package com.kingkharnivore.skillz.ui.screen.help

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.FlowListUiState
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@Composable
fun HelpScreen(
    uiState: FlowListUiState,
    onToggleShowScoreUi: (Boolean) -> Unit,
    onToggleCalmMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember { helpPages() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Help",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
        )

        Spacer(Modifier.height(2.dp))

        PreferenceToggleRow(
            title = "Keep Score Visible",
            description = "Show score totals in Story, and score labels in cards.",
            checked = uiState.showScoreUi,
            onCheckedChange = onToggleShowScoreUi
        )

        PreferenceToggleRow(
            title = "Calm Mode",
            description = "Hides Arc information and timer in Flow.",
            checked = uiState.calmMode,
            onCheckedChange = onToggleCalmMode
        )

        HelpConceptCarousel(
            pages = pages,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpConceptCarousel(
    pages: List<HelpPage>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 26.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageOffset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)

            val scale = 1f - (pageOffset * 0.06f)
            val alpha = 1f - (pageOffset * 0.18f)

            HelpInfoCard(
                page = pages[page],
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                }
                            )
                            .size(if (selected) 10.dp else 8.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Swipe • ${pagerState.currentPage + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun HelpCardKicker(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun HelpInfoCard(
    page: HelpPage,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 245.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpCardKicker(
                iconRes = page.iconRes,
                label = page.kicker
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
            )

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.60f)
            ) {
                Text(
                    text = page.tip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

private data class HelpPage(
    @DrawableRes val iconRes: Int,
    val kicker: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val tip: String
)

private fun helpPages(): List<HelpPage> = listOf(
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = "Flow",
        title = "A Flow is one focused session.",
        subtitle = "Start a Flow when you want to intentionally spend time on something that matters.",
        body = "A Flow tracks your time, keeps the session alive in the background, and lets you add notes so the work becomes part of your Story. Every Flow is a clean, deliberate chapter of effort.",
        tip = "Use Flow for deep work, practice, study, writing, workouts, or anything you want to do with intention."
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = "Arc",
        title = "Arcs reward continuity.",
        subtitle = "An Arc forms when you continue your momentum across consecutive Flows.",
        body = "Keep going and your Arc grows. After each completed Flow of 10 minutes or more, the Arc multiplier increases by +0.1. You have a short grace window between Flows, so continuing quickly helps preserve the chain.",
        tip = "Think of an Arc as momentum across sessions. Save Flow, continue, and keep the chain alive."
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = "Beam",
        title = "Beams are planned boost windows.",
        subtitle = "Set up a Beam when you know you want a protected pocket of time for focused effort.",
        body = "When your Flow overlaps with a Beam, that overlapping time receives a score boost. Beams help you turn planned time into higher-value time, especially when you want structure around important work.",
        tip = "Use Beams for study blocks, training sessions, writing time, or any window you want to treat as special."
    ),
    HelpPage(
        iconRes = R.drawable.scyra_turtle,
        kicker = "Surge",
        title = "Surge rewards precision.",
        subtitle = "Set a target duration before you begin and try to finish close to it.",
        body = "Surge is for sessions where you want to commit to a specific amount of time. The closer your actual session is to the planned duration, the better the reward. It adds a satisfying sense of control and intentional execution.",
        tip = "Use Surge when you want a clear mission, such as 20 minutes of reading, 45 minutes of coding, or 30 minutes of practice."
    )
)