package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.CelebrationLifecycle
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.MasteryCelebrationStep
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellAnimalCanvasIcon
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

private val CelebrationHorizontalPadding = 24.dp
private val MajorSpacing = 24.dp
private val SectionSpacing = 24.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MasteryCelebrationScreen(
    event: MasteryCelebrationEventEntity,
    uiState: ShellUiState,
    onBegin: () -> Unit,
    onAdvance: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onComplete: (String) -> Unit,
    onPin: (String, String?) -> Unit,
    onDismissPinReplacement: () -> Unit,
    onUnpin: (String) -> Unit,
    onTrack: (String) -> Unit,
    onUntrack: (String) -> Unit,
    onNavigate: (BadgeActionDestination) -> Unit
) {
    val stage = runCatching { CelebrationStage.valueOf(event.presentationStage) }
        .getOrDefault(CelebrationStage.FINAL_SUMMARY)
    val step = MasteryCelebrationStep.from(stage)
    val presentationState = remember(event) { MasteryCelebrationUiStateMapper.map(event) }
    val species = CreatureCatalog.get(event.speciesId)
    val creatureName = species?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) }
        ?: stringResource(R.string.mastery_unknown_creature)
    val celebrationDescription = stringResource(R.string.mastery_celebration_a11y, creatureName)
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    val pagerState = rememberPagerState(initialPage = step.pageIndex, pageCount = { 3 })
    var viewingBadgeId by remember(event.eventId) { mutableStateOf<String?>(null) }

    LaunchedEffect(event.eventId) {
        if (event.lifecycleState == CelebrationLifecycle.PENDING.name) onBegin()
    }
    LaunchedEffect(step, reducedMotion) {
        if (reducedMotion) pagerState.scrollToPage(step.pageIndex)
        else pagerState.animateScrollToPage(step.pageIndex)
    }
    BackHandler(enabled = true) {
        if (step != MasteryCelebrationStep.LEVEL_99) onPrevious()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = CelebrationHorizontalPadding)
                .semantics { contentDescription = celebrationDescription }
                .testTag("mastery-celebration")
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> Level99Page(presentationState, creatureName, reducedMotion)
                    1 -> MasteryPage(presentationState, creatureName)
                    else -> MasteryProgressPage(
                        state = presentationState,
                        creatureName = creatureName,
                        onAchievementClick = { badgeId ->
                            if (uiState.badgeDashboard?.badges?.any { it.badgeId == badgeId } == true) {
                                viewingBadgeId = badgeId
                            }
                        }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            MasteryFooter(
                step = step,
                onPrevious = onPrevious,
                onNext = { onAdvance(reducedMotion) },
                onDone = { onComplete(event.originDestination) }
            )
        }
    }

    PinReplacementDialog(uiState, onPin, onDismissPinReplacement)
    viewingBadgeId?.let { id ->
        uiState.badgeDashboard?.badges?.firstOrNull { it.badgeId == id }?.let { badge ->
            BadgeDetailsSheet(
                b = badge,
                dismiss = { viewingBadgeId = null },
                pin = { if (badge.pinnedOrder != null) onUnpin(id) else onPin(id, null) },
                track = { if (badge.tracked) onUntrack(id) else onTrack(id) },
                action = { onNavigate(badge.action) }
            )
        }
    }
}

@Composable
private fun Level99Page(
    state: MasteryCelebrationUiState,
    creatureName: String,
    reducedMotion: Boolean
) {
    CenteredCelebrationPage("mastery-page-1") {
        ShellObjectIcon(state.artworkKey, Modifier.size(160.dp))
        Spacer(Modifier.height(MajorSpacing))
        LevelTransition(state.previousLevel, state.newLevel, reducedMotion)
        Spacer(Modifier.height(MajorSpacing))
        Text(
            text = stringResource(R.string.mastery_level_99_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.mastery_reached_99_sentence, creatureName),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.mastery_journey_complete),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LevelTransition(previousLevel: Int, newLevel: Int, reducedMotion: Boolean) {
    var revealed by remember(previousLevel, newLevel) { mutableStateOf(reducedMotion) }
    LaunchedEffect(previousLevel, newLevel, reducedMotion) {
        if (!reducedMotion) delay(140)
        revealed = true
    }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 420),
        label = "level-99-reveal"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = NumberFormat.getIntegerInstance().format(previousLevel),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "→",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = NumberFormat.getIntegerInstance().format(newLevel),
            modifier = Modifier.graphicsLayer {
                alpha = reveal
                scaleX = 0.9f + (0.1f * reveal)
                scaleY = 0.9f + (0.1f * reveal)
            },
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MasteryPage(state: MasteryCelebrationUiState, creatureName: String) {
    CenteredCelebrationPage("mastery-page-2") {
        ShellObjectIcon(state.artworkKey, Modifier.size(136.dp))
        Spacer(Modifier.height(MajorSpacing))
        Text(
            text = stringResource(
                R.string.mastery_mastered_title,
                creatureName.uppercase(Locale.getDefault())
            ),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.mastery_species_title, creatureName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MajorSpacing))
        MasterySeal()
        Spacer(Modifier.height(16.dp))
        MasteryCountTransition(state.speciesMasteries, prefix = "×", hero = true)
        Spacer(Modifier.height(12.dp))
        Text(
            text = masterySupportText(state.speciesMasteries.current, creatureName),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.mastery_permanent_record_short),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        state.speciesMilestone?.let { milestone ->
            Spacer(Modifier.height(MajorSpacing))
            MilestonePill(creatureName, milestone)
        }
    }
}

@Composable
private fun MasterySeal() {
    val primary = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.mastery_seal_a11y)
    Box(
        Modifier.size(92.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(primary.copy(alpha = 0.10f))
            drawCircle(primary, style = Stroke(width = 2.dp.toPx()))
            drawCircle(
                primary.copy(alpha = 0.55f),
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 1.dp.toPx())
            )
            val waveStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(primary, 200f, 140f, false, style = waveStroke)
            drawArc(primary.copy(alpha = 0.55f), 20f, 140f, false, style = waveStroke)
        }
        Text(
            text = "99",
            style = MaterialTheme.typography.headlineMedium,
            color = primary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MilestonePill(creatureName: String, milestone: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.mastery_milestone_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.mastery_milestone_species, creatureName, milestone),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasteryProgressPage(
    state: MasteryCelebrationUiState,
    creatureName: String,
    onAchievementClick: (String) -> Unit
) {
    CenteredCelebrationPage("mastery-page-3") {
        Text(
            text = stringResource(R.string.mastery_progress_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MajorSpacing))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.mastery_species_masteries, creatureName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                MasteryCountTransition(state.speciesMasteries, hero = true)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressStatCard(
                label = stringResource(R.string.mastery_total_masteries),
                change = state.totalMasteries,
                modifier = Modifier.weight(1f)
            )
            ProgressStatCard(
                label = stringResource(R.string.mastery_unique_species),
                change = state.uniqueSpecies,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(SectionSpacing))
        SectionLabel(stringResource(R.string.mastery_collection_progress_title))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.collections.fastForEach { collection -> CollectionProgressRow(collection) }
        }
        if (state.newlyEarnedBadgeIds.isNotEmpty()) {
            Spacer(Modifier.height(SectionSpacing))
            SectionLabel(stringResource(R.string.mastery_new_achievements_title))
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - 16.dp) / 2
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag("mastery-achievement-grid"),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.newlyEarnedBadgeIds.fastForEach { badgeId ->
                        NewAchievementCard(
                            badgeId = badgeId,
                            modifier = Modifier.width(cardWidth),
                            onClick = { onAchievementClick(badgeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressStatCard(
    label: String,
    change: MasteryCountChange,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            MasteryCountTransition(change)
        }
    }
}

@Composable
private fun CollectionProgressRow(collection: MasteryCollectionChange) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = collectionDisplayName(collection.collectionId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MasteryCountTransition(collection.progress)
                Text(
                    text = " / ${NumberFormat.getIntegerInstance().format(collection.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MasteryCountTransition(
    change: MasteryCountChange,
    prefix: String = "",
    hero: Boolean = false
) {
    val numberStyle = if (hero) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge
    val formattedCurrent = "$prefix${NumberFormat.getIntegerInstance().format(change.current)}"
    if (!change.changed || change.previous == 0) {
        Text(
            text = formattedCurrent,
            style = numberStyle,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(
            text = "$prefix${NumberFormat.getIntegerInstance().format(change.previous)}",
            style = if (hero) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "→",
            modifier = Modifier.padding(horizontal = if (hero) 12.dp else 6.dp),
            style = if (hero) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formattedCurrent,
            style = numberStyle,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NewAchievementCard(
    badgeId: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val presentation = resolveBadgePresentation(badgeId)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            AchievementSeal(presentation)
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.badge_new),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AchievementSeal(presentation: BadgePresentation) {
    Surface(
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (presentation.artworkKind == BadgeArtworkKind.SPECIES_MASTERY && presentation.creatureIconKey != null) {
                ShellAnimalCanvasIcon(
                    presentation.creatureIconKey,
                    Modifier.fillMaxSize().padding(6.dp)
                )
                Text(
                    text = "99",
                    modifier = Modifier.align(Alignment.TopEnd).padding(3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            } else {
                val icon = when (presentation.artworkKind) {
                    BadgeArtworkKind.COLLECTOR -> Icons.Outlined.TravelExplore
                    BadgeArtworkKind.CURATOR -> Icons.Outlined.Inventory2
                    BadgeArtworkKind.COMPLETIONIST -> Icons.Outlined.WorkspacePremium
                    BadgeArtworkKind.FLOW_DURATION,
                    BadgeArtworkKind.ACTIVITY -> Icons.Outlined.Bolt
                    BadgeArtworkKind.OBJECTIVE -> Icons.Outlined.TrackChanges
                    BadgeArtworkKind.SPECIAL -> Icons.Outlined.Stars
                    BadgeArtworkKind.MASTERY,
                    BadgeArtworkKind.SPECIES_MASTERY -> Icons.Outlined.AutoAwesome
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CenteredCelebrationPage(
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(testTag),
        contentPadding = PaddingValues(vertical = MajorSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun MasteryFooter(
    step: MasteryCelebrationStep,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (step != MasteryCelebrationStep.LEVEL_99) {
                TextButton(onClick = onPrevious, modifier = Modifier.testTag("mastery-previous")) {
                    Text(stringResource(R.string.mastery_previous))
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.mastery_step_indicator, step.pageIndex + 1, 3),
                modifier = Modifier.testTag("mastery-step-indicator"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (step == MasteryCelebrationStep.PROGRESS) {
                Button(onClick = onDone, modifier = Modifier.testTag("mastery-done")) {
                    Text(stringResource(R.string.mastery_done))
                }
            } else {
                Button(onClick = onNext, modifier = Modifier.testTag("mastery-next")) {
                    Text(stringResource(R.string.mastery_next))
                }
            }
        }
    }
}

@Composable
private fun masterySupportText(count: Int, creatureName: String): String {
    if (count <= 1) return stringResource(R.string.mastery_first_species_support, creatureName)
    val pluralName = if (Locale.getDefault().language == Locale.ENGLISH.language) {
        pluralizeEnglishCreatureName(creatureName)
    } else creatureName
    return when (count) {
        2 -> stringResource(R.string.mastery_second_species_support, pluralName)
        3 -> stringResource(R.string.mastery_third_species_support, pluralName)
        else -> stringResource(R.string.mastery_many_species_support, count, pluralName)
    }
}

internal fun pluralizeEnglishCreatureName(name: String): String {
    val lower = name.lowercase(Locale.US)
    return when {
        lower.endsWith("fish") -> name
        lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("s") ||
            lower.endsWith("x") || lower.endsWith("z") -> "${name}es"
        lower.endsWith("y") && lower.length > 1 && lower[lower.lastIndex - 1] !in "aeiou" ->
            "${name.dropLast(1)}ies"
        else -> "${name}s"
    }
}
