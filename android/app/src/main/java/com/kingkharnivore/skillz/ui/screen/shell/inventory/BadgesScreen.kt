package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import java.text.NumberFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesScreen(
    uiState: ShellUiState,
    onPin: (String, String?) -> Unit,
    onDismissPinReplacement: () -> Unit,
    onUnpin: (String) -> Unit,
    onTrack: (String) -> Unit,
    onUntrack: (String) -> Unit,
    onCategory: (BadgeUiCategory) -> Unit,
    onSort: (BadgeSort) -> Unit,
    onBadgeViewed: (String) -> Unit,
    onAcknowledgeBackfill: (Int) -> Unit,
    onNavigate: (BadgeActionDestination) -> Unit,
    onOpenFlow: () -> Unit,
    onOpenArc: () -> Unit
) {
    val dashboard = uiState.badgeDashboard
    var query by rememberSaveable { mutableStateOf("") }
    val category = uiState.badgeCategory
    val sort = uiState.badgeSort
    var details by remember { mutableStateOf<BadgeProgressModel?>(null) }
    var collectionDetails by remember { mutableStateOf<CollectionProgress?>(null) }
    fun openBadge(badge: BadgeProgressModel) { details = badge; onBadgeViewed(badge.badgeId) }
    if (dashboard == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val presentations = dashboard.badges.associate { it.badgeId to resolveBadgePresentation(it.badgeId) }
    val localizedCreatureNames = dashboard.badges.associate { badge ->
        val creature = BadgeDefinitionResolver.resolve(badge.badgeId).speciesId?.let(CreatureCatalog::get)
        badge.badgeId to (creature?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) }.orEmpty())
    }
    val categoryLabels = mapOf(
        BadgeUiCategory.ALL to stringResource(R.string.badge_category_all), BadgeUiCategory.FLOW to stringResource(R.string.badge_category_flow),
        BadgeUiCategory.ARC to stringResource(R.string.badge_category_arc), BadgeUiCategory.CREATURES to stringResource(R.string.badge_category_creatures),
        BadgeUiCategory.MASTERY to stringResource(R.string.badge_category_mastery), BadgeUiCategory.COLLECTIONS to stringResource(R.string.badge_category_collections),
        BadgeUiCategory.STILLWATER to stringResource(R.string.badge_category_stillwater), BadgeUiCategory.MOVEMENT to stringResource(R.string.badge_category_movement),
        BadgeUiCategory.SURGE to stringResource(R.string.badge_category_surge), BadgeUiCategory.SPECIAL to stringResource(R.string.badge_category_special)
    )
    val sortLabels = mapOf(
        BadgeSort.RECOMMENDED to stringResource(R.string.badge_sort_recommended), BadgeSort.RECENTLY_EARNED to stringResource(R.string.badge_sort_recent_earned),
        BadgeSort.RECENTLY_ADVANCED to stringResource(R.string.badge_sort_recent_advanced), BadgeSort.HIGHEST_COUNT to stringResource(R.string.badge_sort_highest_count),
        BadgeSort.CLOSEST_MILESTONE to stringResource(R.string.badge_sort_closest), BadgeSort.ALPHABETICAL to stringResource(R.string.badge_sort_alphabetical)
    )
    val availableCategories = remember(dashboard.badges) {
        listOf(BadgeUiCategory.ALL) + dashboard.badges.map { it.category }.distinct()
    }
    val visible = remember(dashboard.badges, presentations, localizedCreatureNames, categoryLabels, query, category, sort) {
        dashboard.badges.filter { badge ->
            (category == BadgeUiCategory.ALL || badge.category == category) &&
                (query.isBlank() || badgeSearchText(presentations.getValue(badge.badgeId), categoryLabels.getValue(badge.category), localizedCreatureNames[badge.badgeId].orEmpty()).contains(query.trim(), ignoreCase = true))
        }.sortedWith(badgeComparator(sort, presentations))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item("header") {
            RoomHeader(R.string.shell_badges_title, R.string.badges_hub_body)
            Text(pluralStringResource(R.plurals.badges_summary, dashboard.completedCollections, dashboard.uniqueEarned, dashboard.totalMasteries, dashboard.completedCollections),
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.badges_search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Clear, stringResource(R.string.badges_clear_search)) } })
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeMenu(stringResource(R.string.badges_category_selected, categoryLabels.getValue(category)), availableCategories, { categoryLabels.getValue(it) }, onCategory)
                BadgeMenu(stringResource(R.string.badges_sort_selected, sortLabels.getValue(sort)), BadgeSort.values().toList(), { sortLabels.getValue(it) }, onSort)
            }
        }
        item("showcase-title") { SectionTitle(stringResource(R.string.badges_showcase_title), stringResource(R.string.badges_showcase_body)) }
        item("showcase") {
            val pins = dashboard.badges.filter { it.pinnedOrder != null }.sortedBy { it.pinnedOrder }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val badge = pins.getOrNull(index)
                    Surface(Modifier.weight(1f).heightIn(min = 132.dp), shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        if (badge == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.badges_empty_slot), style = MaterialTheme.typography.labelMedium) }
                        else Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BadgeMedallion(badge, BadgeMedallionSize.Large, onClick = { openBadge(badge) })
                            Text(badgeTitle(badge), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        item("tracked-title") { SectionTitle(stringResource(R.string.badges_tracked_title), stringResource(R.string.badges_tracked_body)) }
        val trackedBadges = dashboard.badges.filter { it.tracked }
        if (trackedBadges.isEmpty()) item("no-tracked") {
            OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.badges_no_tracked_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.badges_no_tracked_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(trackedBadges, key = { "tracked:${it.badgeId}" }) { badge ->
            ProgressBadgeRow(badge, { openBadge(badge) }, { onUntrack(badge.badgeId) },
                { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetails = dashboard.collections.firstOrNull { it.collectionId == id } } })
        }
        item("reach-title") { SectionTitle(stringResource(R.string.badges_within_reach_title), stringResource(R.string.badges_within_reach_body)) }
        if (dashboard.recommendations.isEmpty()) item("no-reach") { EmptyCard(stringResource(R.string.badges_no_recommendations)) }
        items(dashboard.recommendations, key = { "reach:${it.badgeId}" }) { badge ->
            ProgressBadgeRow(badge, { openBadge(badge) }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetails = dashboard.collections.firstOrNull { it.collectionId == id } } })
        }
        val recent = dashboard.badges.filter { it.earned && it.lastAdvancedAt != null }.sortedByDescending { it.lastAdvancedAt }.take(5)
        if (recent.isNotEmpty()) {
            item("recent-title") { SectionTitle(stringResource(R.string.badges_recent_title), stringResource(R.string.badges_recent_body)) }
            items(recent, key = { "recent:${it.badgeId}" }) { badge -> BadgeGridRow(badge, { openBadge(badge) }, {
                if (badge.pinnedOrder != null) onUnpin(badge.badgeId) else onPin(badge.badgeId, null)
            }) }
        }
        item("collections-title") { SectionTitle(stringResource(R.string.badges_collections_title), stringResource(R.string.badges_collections_body)) }
        items(dashboard.collections, key = { it.collectionId }) { CollectionCard(it) { collectionDetails = it } }
        item("book-title") { SectionTitle(stringResource(R.string.badges_book_title), stringResource(R.string.badges_book_body)) }
        items(visible, key = { "book:${it.badgeId}" }) { badge -> ProgressBadgeRow(badge, { openBadge(badge) }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetails = dashboard.collections.firstOrNull { it.collectionId == id } } }) }
    }
    details?.let { badge -> BadgeDetailsSheet(badge, { details = null }, {
        if (badge.pinnedOrder != null) onUnpin(badge.badgeId) else onPin(badge.badgeId, null)
    }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) },
        { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { details = null }) { id -> collectionDetails = dashboard.collections.firstOrNull { it.collectionId == id }; details = null } }) }
    collectionDetails?.let { CollectionDetailsSheet(it, { collectionDetails = null }) { action ->
        collectionSpeciesDestination(action)?.let(onNavigate)
    } }
    PinReplacementDialog(uiState, onPin, onDismissPinReplacement)
    uiState.backfillSummary?.let { summary ->
        AlertDialog(onDismissRequest = { onAcknowledgeBackfill(summary.version) }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.badges_backfill_title)) },
            text = { Text(stringResource(R.string.badges_backfill_body, summary.discoveredCount, summary.masteryCount, summary.completionCount)) },
            confirmButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text(stringResource(R.string.badges_view_new)) } },
            dismissButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text(stringResource(R.string.mastery_continue)) } })
    }
}

@Composable internal fun PinReplacementDialog(uiState: ShellUiState, onPin: (String, String?) -> Unit, onDismiss: () -> Unit) {
    val replacement = uiState.pinReplacement ?: return
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.badges_replace_title)) },
        text = { Column { Text(stringResource(R.string.badges_replace_body)); replacement.pinnedBadgeIds.forEach { pinnedId ->
            TextButton({ onPin(replacement.requestedBadgeId, pinnedId) }) { Text(resolveBadgePresentation(pinnedId).title) }
        } } }, confirmButton = {}, dismissButton = { TextButton(onDismiss) { Text(stringResource(android.R.string.cancel)) } })
}
private fun badgeSearchText(presentation: BadgePresentation, category: String, localizedCreatureName: String) =
    listOf(presentation.title, presentation.description, category, localizedCreatureName).joinToString(" ")
private fun badgeComparator(sort: BadgeSort, presentations: Map<String, BadgePresentation>): Comparator<BadgeProgressModel> = when(sort) {
    BadgeSort.RECOMMENDED -> compareBy<BadgeProgressModel> { it.pinnedOrder ?: 99 }.thenByDescending { it.earned }.thenBy { it.remaining }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_EARNED -> compareByDescending<BadgeProgressModel> { it.firstEarnedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_ADVANCED -> compareByDescending<BadgeProgressModel> { it.lastAdvancedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.HIGHEST_COUNT -> compareByDescending<BadgeProgressModel> { it.count }.thenBy { it.badgeId }
    BadgeSort.CLOSEST_MILESTONE -> compareBy<BadgeProgressModel> { it.remaining }.thenBy { it.badgeId }
    BadgeSort.ALPHABETICAL -> compareBy<BadgeProgressModel> { presentations[it.badgeId]?.title.orEmpty() }.thenBy { it.badgeId }
}

@Composable private fun badgeTitle(badge: BadgeProgressModel): String {
    return resolveBadgePresentation(badge.badgeId).title
}

enum class BadgeMedallionSize { Small, Medium, Large }
sealed interface BadgeMedallionState {
    data object Earned : BadgeMedallionState
    data class LockedWithProgress(val progress: Int, val target: Int) : BadgeMedallionState
    data object Locked : BadgeMedallionState
}

internal fun badgeMedallionState(badge: BadgeProgressModel): BadgeMedallionState = when {
    badge.earned -> BadgeMedallionState.Earned
    badge.target > 0 && badge.progress > 0 -> BadgeMedallionState.LockedWithProgress(badge.progress, badge.target)
    else -> BadgeMedallionState.Locked
}

@Composable fun BadgeMedallion(badge: BadgeProgressModel, size: BadgeMedallionSize = BadgeMedallionSize.Medium, onClick: (() -> Unit)? = null) {
    val diameter = when(size) { BadgeMedallionSize.Small -> 56.dp; BadgeMedallionSize.Medium -> 72.dp; BadgeMedallionSize.Large -> 88.dp }
    val presentation = resolveBadgePresentation(badge.badgeId)
    val title = presentation.title; val exact = NumberFormat.getIntegerInstance().format(badge.count)
    val semantics = stringResource(R.string.badge_count_a11y, title,
        if (badge.earned) stringResource(R.string.badge_earned) else stringResource(R.string.badge_locked), exact,
        badge.remaining, if (badge.pinnedOrder != null) stringResource(R.string.badge_pinned_a11y) else "",
        if (badge.tracked) stringResource(R.string.badge_tracked_a11y) else "")
    val interactionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val medallionState = badgeMedallionState(badge)
    val ringProgress = when (medallionState) {
        BadgeMedallionState.Earned -> 1f
        is BadgeMedallionState.LockedWithProgress -> (medallionState.progress.toFloat() / medallionState.target).coerceIn(0f, 1f)
        BadgeMedallionState.Locked -> 0f
    }
    Box(Modifier.size(diameter + 18.dp).semantics(mergeDescendants = true) { contentDescription = semantics }.then(interactionModifier), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { ringProgress }, Modifier.size(diameter + 8.dp), strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
        Surface(Modifier.size(diameter).clip(CircleShape), shape = CircleShape,
            color = if (badge.earned) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            border = if (badge.earned) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            shadowElevation = if (badge.earned) 2.dp else 0.dp) {
            Box(contentAlignment = Alignment.Center) {
                if (!badge.earned) Icon(Icons.Outlined.Lock, null, Modifier.size(diameter/2)) else when (presentation.artworkKind) {
                    BadgeArtworkKind.FLOW_DURATION -> Text(presentation.centerLabel.orEmpty(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    BadgeArtworkKind.SPECIES_MASTERY -> Box(contentAlignment = Alignment.Center) { presentation.creatureIconKey?.let { ShellObjectIcon(it, Modifier.size(diameter * 0.62f)) }; Text("99", Modifier.align(Alignment.TopEnd).padding(5.dp), fontWeight = FontWeight.Black) }
                    BadgeArtworkKind.COLLECTOR -> CollectionBadgeArtwork(presentation, Icons.Outlined.TravelExplore, diameter)
                    BadgeArtworkKind.CURATOR -> CollectionBadgeArtwork(presentation, Icons.Outlined.Inventory2, diameter)
                    BadgeArtworkKind.COMPLETIONIST -> CollectionBadgeArtwork(presentation, Icons.Outlined.WorkspacePremium, diameter)
                    BadgeArtworkKind.MASTERY -> Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(diameter/2))
                    BadgeArtworkKind.ACTIVITY -> Icon(Icons.Outlined.Bolt, null, Modifier.size(diameter/2))
                    BadgeArtworkKind.SPECIAL -> Icon(Icons.Outlined.Stars, null, Modifier.size(diameter/2))
                }
                if (badge.count > 0 && !(badge.goalType == com.kingkharnivore.skillz.domain.achievement.BadgeGoalType.ONE_TIME && badge.count == 1)) {
                    Surface(Modifier.align(Alignment.BottomCenter), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface) { Text(stringResource(R.string.badge_count_plate, compactCount(badge.count)), Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
        }
        if (badge.pinnedOrder != null) Icon(Icons.Outlined.PushPin, null, Modifier.align(Alignment.TopEnd).size(18.dp))
        if (badge.tracked) Icon(Icons.Outlined.TrackChanges, null, Modifier.align(Alignment.TopStart).size(18.dp))
    }
}
@Composable private fun CollectionBadgeArtwork(presentation: BadgePresentation, icon: androidx.compose.ui.graphics.vector.ImageVector, diameter: androidx.compose.ui.unit.Dp) {
    Box(contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(diameter / 2))
        val crestColor = MaterialTheme.colorScheme.primary
        Canvas(Modifier.align(Alignment.TopCenter).size(diameter * 0.3f)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.11f)
            when (presentation.collectionIdentity) {
                CollectionArtworkIdentity.SUNLIT_REEF -> drawCircle(crestColor, size.minDimension * 0.28f)
                CollectionArtworkIdentity.DEEPER_REEF -> repeat(3) { drawLine(crestColor, androidx.compose.ui.geometry.Offset(0f, size.height * (it + 1) / 4), androidx.compose.ui.geometry.Offset(size.width, size.height * (it + 1) / 4), strokeWidth = stroke.width) }
                CollectionArtworkIdentity.OPEN_BLUE -> drawArc(crestColor, 0f, 180f, false, style = stroke)
                CollectionArtworkIdentity.GREAT_BLUE -> drawCircle(crestColor, size.minDimension * 0.4f, style = stroke)
                CollectionArtworkIdentity.FISHBOWL -> drawArc(crestColor, 0f, 180f, false, style = stroke)
                CollectionArtworkIdentity.AQUARIUM -> drawRect(crestColor, style = stroke)
                CollectionArtworkIdentity.POND -> repeat(2) { ring -> drawOval(crestColor.copy(alpha = 1f - ring * .3f), topLeft = androidx.compose.ui.geometry.Offset(ring * 4f, ring * 4f), size = androidx.compose.ui.geometry.Size(size.width - ring * 8f, size.height - ring * 8f), style = stroke) }
                CollectionArtworkIdentity.LAKE -> drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, size.height); lineTo(size.width * .5f, 0f); lineTo(size.width, size.height) }, crestColor, style = stroke)
                CollectionArtworkIdentity.THE_BLUE -> drawCircle(crestColor, size.minDimension * .42f, style = stroke)
                CollectionArtworkIdentity.STILLWATER -> drawCircle(crestColor, size.minDimension * .22f)
                CollectionArtworkIdentity.ALL_WATERS -> { drawCircle(crestColor, size.minDimension * .4f, style = stroke); drawCircle(crestColor, size.minDimension * .16f) }
                null -> Unit
            }
        }
    }
}
internal fun compactCount(count: Int): String {
    if (count <= 999) return count.toString()
    val (divisor, suffix) = when {
        count >= 1_000_000_000 -> 1_000_000_000.0 to "B"
        count >= 1_000_000 -> 1_000_000.0 to "M"
        else -> 1_000.0 to "K"
    }
    return NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
        .format(count / divisor) + suffix
}

@Composable private fun BadgeGridRow(badge: BadgeProgressModel, open: () -> Unit, pin: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = open), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); Text(stringResource(R.string.badge_exact_count, NumberFormat.getIntegerInstance().format(badge.count)), style = MaterialTheme.typography.bodySmall); if (badge.newlyEarned) Text(stringResource(R.string.badge_new), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) else if (badge.recentlyUpdated) Text(stringResource(R.string.badge_updated), color = MaterialTheme.colorScheme.primary) }; IconButton(pin) { Icon(if (badge.pinnedOrder != null) Icons.Outlined.PushPin else Icons.Outlined.AddCircleOutline, if (badge.pinnedOrder != null) stringResource(R.string.badge_unpin) else stringResource(R.string.badge_pin)) } } } }
@Composable private fun ProgressBadgeRow(badge: BadgeProgressModel, open: () -> Unit, track: () -> Unit, action: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); Text(resolveBadgePresentation(badge.badgeId).description, style = MaterialTheme.typography.bodySmall); Text(recommendationText(badge), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (badge.target > 0) LinearProgressIndicator({ (badge.progress.toFloat()/badge.target).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().semantics { progressBarRangeInfo = ProgressBarRangeInfo(badge.progress.toFloat().coerceAtMost(badge.target.toFloat()), 0f..badge.target.toFloat()) }) }; Column(horizontalAlignment = Alignment.End) { if (badge.tracked || badge.canTrack) TextButton(track) { Text(if (badge.tracked) stringResource(R.string.badge_untrack) else stringResource(R.string.badge_track)) }; if (badge.canNavigate) TextButton(action) { Text(badgeActionLabel(badge.action)) } } } } }
@Composable private fun CollectionCard(p: CollectionProgress, onClick: () -> Unit) { OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(collectionDisplayName(p.collectionId), fontWeight = FontWeight.Bold); Text(stringResource(R.string.collection_discovered_progress, p.discoveredSpeciesCount, p.totalParticipatingSpecies)); Text(stringResource(R.string.collection_owned_progress, p.currentlyOwnedSpeciesCount, p.totalParticipatingSpecies)); Text(stringResource(R.string.collection_mastered_progress, p.masteredSpeciesCount, p.totalCompletionistSpecies)); Text(listOfNotNull(if(p.collectorEarned) stringResource(R.string.badge_state_collector) else null, if(p.curatorEarned) stringResource(R.string.badge_state_curator) else null, if(p.completionistEarned) stringResource(R.string.badge_state_completionist) else null).joinToString(" · "), color = MaterialTheme.colorScheme.primary) } } }
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable internal fun BadgeDetailsSheet(b: BadgeProgressModel, dismiss: () -> Unit, pin: () -> Unit, track: () -> Unit, action: () -> Unit) { ScyraParchmentSheet(onDismissRequest = dismiss) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { val presentation = resolveBadgePresentation(b.badgeId); BadgeMedallion(b, BadgeMedallionSize.Large); Text(presentation.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(presentation.description); Text(stringResource(R.string.badge_exact_count, NumberFormat.getIntegerInstance().format(b.count))); Text(if(b.earned) stringResource(R.string.badge_earned) else stringResource(R.string.badge_locked)); if (b.target > 0) Text(stringResource(R.string.badge_progress_remaining, b.progress, b.target, b.remaining)); b.disabledReason?.let { Text(badgeDisabledText(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }; b.milestone.nextThreshold?.let { next -> b.milestone.currentThreshold?.let { Text(stringResource(R.string.badge_current_milestone, it)) }; Text(stringResource(R.string.badge_next_milestone, next)) }; b.firstEarnedAt?.let { Text(stringResource(R.string.badge_first_earned, formatBadgeDate(it))) }; b.lastAdvancedAt?.let { Text(stringResource(R.string.badge_last_advanced, formatBadgeDate(it))) }; if (b.tracked || b.canTrack) Text(stringResource(R.string.badge_tracking_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { if (b.earned && b.pinnable) OutlinedButton(pin) { Text(if(b.pinnedOrder != null) stringResource(R.string.badge_unpin) else stringResource(R.string.badge_pin)) }; if (b.tracked || b.canTrack) OutlinedButton(track) { Text(if(b.tracked) stringResource(R.string.badge_untrack) else stringResource(R.string.badge_track)) }; if (b.canNavigate) Button(action) { Text(badgeActionLabel(b.action)) } }; Spacer(Modifier.height(24.dp)) } } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CollectionDetailsSheet(p: CollectionProgress, dismiss: () -> Unit, onSpeciesAction: ((CollectionSpeciesAction) -> Unit)? = null) { ScyraParchmentSheet(onDismissRequest = dismiss) { LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text(collectionDisplayName(p.collectionId), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; item { Text(stringResource(R.string.collection_detail_summary, p.discoveredSpeciesCount, p.currentlyOwnedSpeciesCount, p.masteredSpeciesCount, p.totalParticipatingSpecies)) }; items(p.speciesStates, key = { it.speciesId }) { CollectionSpeciesRow(it, onSpeciesAction) }; item { Spacer(Modifier.height(24.dp)) } } } }
@Composable private fun CollectionSpeciesList(p: CollectionProgress) { p.speciesStates.forEach { CollectionSpeciesRow(it, null) } }
@Composable private fun CollectionSpeciesRow(state: CollectionSpeciesProgress, onAction: ((CollectionSpeciesAction) -> Unit)?) { val creature = CreatureCatalog.get(state.speciesId); val hidden = state.secret && !state.discovered; val actionable = state.action !is CollectionSpeciesAction.None && onAction != null; val rowModifier = if (actionable) Modifier.clickable { onAction?.invoke(state.action) } else Modifier; ListItem(modifier = rowModifier, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface), leadingContent = { ShellObjectIcon(if(hidden) "unknown_creature" else creature?.staticIconKey ?: "unknown_creature", Modifier.size(48.dp)) }, headlineContent = { Text(if(hidden) stringResource(R.string.collection_secret_species) else creature?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) } ?: stringResource(R.string.badge_creature_fallback)) }, trailingContent = { if (actionable) Icon(Icons.Outlined.ArrowForward, stringResource(R.string.collection_species_open_action)) }, supportingContent = { Column { Text(when { state.mastered -> stringResource(R.string.collection_species_mastered_count, state.lifetimeMasteryCount, state.currentLevel99Count); state.ownedCount > 0 -> stringResource(R.string.collection_species_owned, state.ownedCount, state.highestLevel ?: 1, 99 - (state.highestLevel ?: 1)); state.discovered -> stringResource(R.string.collection_species_discovered_not_owned); else -> stringResource(R.string.collection_species_undiscovered) }); if (state.mastered) Text(when (state.timestampConfidence) { MasteryTimestampConfidence.EXACT -> state.firstMasteryAt?.let { stringResource(R.string.mastery_date_exact, formatBadgeDate(it)) } ?: stringResource(R.string.mastery_date_unknown); MasteryTimestampConfidence.ESTIMATED_FROM_ACQUISITION -> state.firstMasteryAt?.let { stringResource(R.string.mastery_date_estimated, formatBadgeDate(it)) } ?: stringResource(R.string.mastery_date_unknown); MasteryTimestampConfidence.UNKNOWN -> stringResource(R.string.mastery_date_unknown) }, style = MaterialTheme.typography.bodySmall) } }) }
internal fun collectionSpeciesDestination(action: CollectionSpeciesAction): BadgeActionDestination? = when (action) {
    is CollectionSpeciesAction.ViewInChest -> BadgeActionDestination.Chest(action.speciesId)
    is CollectionSpeciesAction.OpenBlueRegion -> BadgeActionDestination.Blue(action.collectionId)
    is CollectionSpeciesAction.OpenBeyondBlue -> BadgeActionDestination.Blue(action.collectionId)
    is CollectionSpeciesAction.OpenStillwaterVessel -> BadgeActionDestination.Stillwater(action.collectionId)
    CollectionSpeciesAction.None -> null
}
private fun formatBadgeDate(timestamp: Long): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(timestamp))
@Composable private fun badgeDisabledText(reason: BadgeDisabledReason): String = stringResource(when (reason) {
    BadgeDisabledReason.COMPLETE -> R.string.badge_disabled_complete
    BadgeDisabledReason.NO_NEXT_MILESTONE -> R.string.badge_disabled_no_next
    BadgeDisabledReason.CREATURE_NOT_OWNED -> R.string.badge_disabled_not_owned
    BadgeDisabledReason.REGION_LOCKED -> R.string.badge_disabled_region_locked
    BadgeDisabledReason.VESSEL_LOCKED -> R.string.badge_disabled_vessel_locked
    BadgeDisabledReason.EMPTY_ROSTER -> R.string.badge_disabled_empty_roster
    BadgeDisabledReason.UNSUPPORTED_DESTINATION -> R.string.badge_disabled_historical
})
@Composable private fun badgeActionLabel(action: BadgeActionDestination): String = stringResource(when (action) {
    is BadgeActionDestination.Chest -> R.string.badge_action_view_chest
    is BadgeActionDestination.Blue -> R.string.badge_action_open_blue
    is BadgeActionDestination.Stillwater -> R.string.badge_action_open_stillwater
    else -> R.string.badge_open_action
})
@Composable private fun recommendationText(badge: BadgeProgressModel): String = when {
    badge.highestCreatureLevel != null -> stringResource(R.string.badge_next_mastery_step, badge.highestCreatureLevel, (99 - badge.highestCreatureLevel).coerceAtLeast(0))
    BadgeDefinitionResolver.resolve(badge.badgeId).requirement == BadgeRequirement.COLLECTOR -> stringResource(R.string.badge_next_discovery_step, badge.remaining)
    BadgeDefinitionResolver.resolve(badge.badgeId).requirement == BadgeRequirement.COMPLETIONIST -> stringResource(R.string.badge_next_completionist_step, badge.remaining)
    badge.category == BadgeUiCategory.FLOW -> stringResource(R.string.badge_next_flow_step, badge.remaining)
    else -> stringResource(R.string.badge_progress_remaining, badge.progress, badge.target, badge.remaining)
}
@Composable private fun SectionTitle(title: String, body: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun EmptyCard(text: String) { OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Text(text, Modifier.padding(18.dp)) } }
@Composable private fun <T> BadgeMenu(label: String, values: List<T>, text: (T)->String, selected: (T)->Unit) { var open by remember { mutableStateOf(false) }; Box { OutlinedButton({open=true}) { Text(label) }; DropdownMenu(open, {open=false}, containerColor = MaterialTheme.colorScheme.surface) { values.forEach { DropdownMenuItem({Text(text(it))}, { selected(it); open=false }) } } } }
private fun navigateFor(
    badge: BadgeProgressModel,
    navigate: (BadgeActionDestination) -> Unit,
    openFlow: () -> Unit,
    openArc: () -> Unit,
    showDetails: () -> Unit,
    showCollection: (String) -> Unit
) = when (badge.action) {
    BadgeActionDestination.Flow -> openFlow()
    BadgeActionDestination.Arc -> openArc()
    is BadgeActionDestination.Chest, is BadgeActionDestination.Blue, is BadgeActionDestination.Stillwater -> navigate(badge.action)
    BadgeActionDestination.BlueCollection -> showCollection("collection_the_blue")
    BadgeActionDestination.StillwaterCollection -> showCollection("collection_stillwater")
    BadgeActionDestination.AllWatersCollection -> showCollection("collection_all_waters")
    BadgeActionDestination.MovementInfo -> navigate(badge.action)
    BadgeActionDestination.BadgeDetails -> showDetails()
}
