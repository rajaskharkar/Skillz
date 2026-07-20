package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

@Composable
fun MasteryCelebrationScreen(
    event: MasteryCelebrationEventEntity,
    uiState: ShellUiState,
    onBegin: () -> Unit,
    onAdvance: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onComplete: (String) -> Unit,
    onPin: (String, String?) -> Unit
) {
    val stage = runCatching { CelebrationStage.valueOf(event.presentationStage) }
        .getOrDefault(CelebrationStage.FINAL_SUMMARY)
    val species = CreatureCatalog.get(event.speciesId)
    val creatureName = species?.displayName ?: stringResource(R.string.mastery_unknown_creature)
    val speciesBadgeId = "mastery_species_${event.speciesId}"
    val speciesBadge = uiState.badgeDashboard?.badges?.firstOrNull { it.badgeId == speciesBadgeId }
    val isSummary = stage == CelebrationStage.FINAL_SUMMARY
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
    var pinRequested by remember(event.eventId) { mutableStateOf(false) }

    LaunchedEffect(event.eventId) {
        if (event.lifecycleState == CelebrationLifecycle.PENDING.name) onBegin()
    }
    BackHandler {
        if (isSummary) onComplete(event.originDestination) else onSkip()
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                .semantics { contentDescription = "$creatureName reached Level 99. Mastery achieved." },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isSummary) TextButton(onClick = onSkip) { Text(stringResource(R.string.mastery_skip)) }
            }
            ShellObjectIcon(event.artworkKey, Modifier.size(132.dp))
            when (stage) {
                CelebrationStage.LEVEL_TRANSITION -> LevelTransitionStage()
                CelebrationStage.MASTERY_REVEAL -> MasteryRevealStage(creatureName, event.totalMasteries == 1)
                CelebrationStage.SPECIES_BADGE_REVEAL -> SpeciesBadgeStage(creatureName, event.speciesMasteryCount, speciesBadge)
                CelebrationStage.COLLECTION_IMPACT -> CollectionImpactStage(event)
                CelebrationStage.ADDITIONAL_ACHIEVEMENTS -> AdditionalAchievementsStage(event)
                CelebrationStage.FINAL_SUMMARY, CelebrationStage.COMPLETED -> FinalSummaryStage(event, creatureName, speciesBadge)
            }
            if (uiState.calmMode) Text(stringResource(R.string.mastery_calm_mode), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            if (isSummary) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        val pins = uiState.badgeDashboard?.badges?.filter { it.pinnedOrder != null }.orEmpty()
                        if (pins.size < 3) onPin(speciesBadgeId, null) else pinRequested = true
                    }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.mastery_pin_badge)) }
                    Button(onClick = { onComplete(event.originDestination) }, modifier = Modifier.weight(1f)) {
                        Text(returnLabel(event.originDestination))
                    }
                }
            } else Button(onClick = { onAdvance(reducedMotion) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.mastery_continue)) }
        }
    }
    if (pinRequested) {
        val pins = uiState.badgeDashboard?.badges?.filter { it.pinnedOrder != null }?.sortedBy { it.pinnedOrder }.orEmpty()
        AlertDialog(onDismissRequest = { pinRequested = false }, title = { Text(stringResource(R.string.mastery_replace_pin_title)) },
            text = { Column { pins.forEach { pin -> TextButton(onClick = { onPin(speciesBadgeId, pin.badgeId); pinRequested = false }) { Text(pin.badgeId.replace('_', ' ')) } } } },
            confirmButton = {}, dismissButton = { TextButton(onClick = { pinRequested = false }) { Text(stringResource(android.R.string.cancel)) } })
    }
}

@Composable private fun LevelTransitionStage() {
    Text(stringResource(R.string.mastery_level_transition), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
    Text(stringResource(R.string.mastery_journey_complete), style = MaterialTheme.typography.titleLarge)
}
@Composable private fun MasteryRevealStage(name: String, firstEver: Boolean) {
    Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.mastery_achieved), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.mastery_reached_99, name), style = MaterialTheme.typography.titleMedium)
    if (firstEver) Text(stringResource(R.string.mastery_permanent_record), textAlign = TextAlign.Center)
}
@Composable private fun SpeciesBadgeStage(name: String, count: Int, badge: BadgeProgressModel?) {
    if (badge != null) BadgeMedallion(badge, BadgeMedallionSize.Large)
    Text(stringResource(R.string.mastery_species_title, name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("×$count", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
    Text(pluralStringResource(R.plurals.mastery_species_support, count, count, name), textAlign = TextAlign.Center)
}
@Composable private fun CollectionImpactStage(event: MasteryCelebrationEventEntity) {
    Text(stringResource(R.string.mastery_collection_impact), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(event.regionId.removePrefix("blue_").removePrefix("collection_").replace('_', ' ').replaceFirstChar { it.titlecase() })
    Text(stringResource(R.string.mastery_collection_progress, event.regionalMastered, event.regionalTotal))
    if (event.regionalCompletionistEarned) Text(stringResource(R.string.mastery_region_complete), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    else Text(pluralStringResource(R.plurals.mastery_species_remaining, event.regionalTotal - event.regionalMastered, event.regionalTotal - event.regionalMastered))
}
@Composable private fun AdditionalAchievementsStage(event: MasteryCelebrationEventEntity) {
    Text(stringResource(R.string.mastery_additional), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    val ids = event.newlyEarnedBadgeIds.split(',').filter { it.isNotBlank() }.sortedBy(::significantAchievementOrder)
    if (ids.isEmpty()) Text(stringResource(R.string.mastery_counts_advanced))
    ids.forEach { id ->
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(id.replace('_', ' ').replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.Bold)
                when (id) {
                    "collection_all_waters_completionist" -> Text(stringResource(R.string.mastery_all_waters_complete))
                    "collection_the_blue_completionist" -> Text(stringResource(R.string.mastery_blue_complete))
                    else -> if (id.startsWith("blue_") && id.endsWith("_completionist")) Text(stringResource(R.string.mastery_region_complete))
                }
            }
        }
    }
    event.milestonesReached.split(',').filter { it.isNotBlank() }.forEach { milestone ->
        val id = milestone.substringBefore(':')
        val threshold = milestone.substringAfter(':')
        OutlinedCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mastery_milestone_reached, id.replace('_', ' '), threshold), Modifier.padding(16.dp))
        }
    }
}
@Composable private fun FinalSummaryStage(event: MasteryCelebrationEventEntity, name: String, badge: BadgeProgressModel?) {
    Text(stringResource(R.string.mastery_final_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.mastery_reached_99, name), style = MaterialTheme.typography.titleLarge)
    if (badge != null) BadgeMedallion(badge, BadgeMedallionSize.Medium)
    Text(stringResource(R.string.mastery_summary_species, event.speciesMasteryCount))
    Text(stringResource(R.string.mastery_summary_total, event.totalMasteries, event.uniqueMasteredSpecies))
    Text(stringResource(R.string.mastery_collection_progress, event.regionalMastered, event.regionalTotal))
    Text(stringResource(R.string.mastery_blue_progress, event.blueMastered, event.blueTotal))
    Text(stringResource(R.string.mastery_all_progress, event.allWatersMastered, event.allWatersTotal))
    val earned = event.newlyEarnedBadgeIds.split(',').filter { it.isNotBlank() }
    if (earned.isNotEmpty()) Text(stringResource(R.string.mastery_new_badges, earned.size))
    val milestones = event.milestonesReached.split(',').filter { it.isNotBlank() }
    if (milestones.isNotEmpty()) Text(stringResource(R.string.mastery_new_milestones, milestones.size))
}
@Composable private fun returnLabel(origin: String) = stringResource(when(origin) {
    "BLUE" -> R.string.mastery_return_blue
    "STILLWATER" -> R.string.mastery_return_stillwater
    else -> R.string.mastery_return_chest
})
