package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.ShellDestination
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import java.text.NumberFormat

@Composable
fun BadgesScreen(
    uiState: ShellUiState,
    onPin: (String, String?) -> Unit,
    onUnpin: (String) -> Unit,
    onMovePin: (String, Int) -> Unit,
    onTrack: (String) -> Unit,
    onUntrack: (String) -> Unit,
    onCategory: (BadgeUiCategory) -> Unit,
    onSort: (BadgeSort) -> Unit,
    onAcknowledgeBackfill: (Int) -> Unit,
    onNavigate: (ShellDestination) -> Unit
) {
    val dashboard = uiState.badgeDashboard
    var query by rememberSaveable { mutableStateOf("") }
    val category = uiState.badgeCategory
    val sort = uiState.badgeSort
    var details by remember { mutableStateOf<BadgeProgressModel?>(null) }
    var replacement by remember { mutableStateOf<BadgeProgressModel?>(null) }
    if (dashboard == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val visible = remember(dashboard.badges, query, category, sort) {
        dashboard.badges.filter { badge ->
            (category == BadgeUiCategory.ALL || badge.category == category) &&
                (query.isBlank() || badgeSearchText(badge).contains(query.trim(), ignoreCase = true))
        }.sortedWith(badgeComparator(sort))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item("header") {
            RoomHeader(R.string.shell_badges_title, R.string.badges_hub_body)
            Text("${dashboard.uniqueEarned} earned · ${dashboard.totalMasteries} Masteries · ${dashboard.completedCollections} completed collections",
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search badges") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Clear, "Clear search") } })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeMenu("Category: ${category.name.lowercase().replace('_',' ')}", BadgeUiCategory.values().toList(), { it.name.lowercase().replace('_',' ') }, onCategory)
                BadgeMenu("Sort: ${sort.name.lowercase().replace('_',' ')}", BadgeSort.values().toList(), { it.name.lowercase().replace('_',' ') }, onSort)
            }
        }
        item("showcase-title") { SectionTitle("Your Showcase", "Pin the achievements that mean the most to you.") }
        item("showcase") {
            val pins = dashboard.badges.filter { it.pinnedOrder != null }.sortedBy { it.pinnedOrder }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val badge = pins.getOrNull(index)
                    Surface(Modifier.weight(1f).heightIn(min = 156.dp), shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        if (badge == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Empty slot", style = MaterialTheme.typography.labelMedium) }
                        else Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BadgeMedallion(badge, BadgeMedallionSize.Large, onClick = { details = badge })
                            Text(badgeTitle(badge), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                            Row { IconButton({ onMovePin(badge.badgeId, -1) }, enabled = index > 0) { Icon(Icons.Outlined.ChevronLeft, "Move left") }; IconButton({ onMovePin(badge.badgeId, 1) }, enabled = index < pins.lastIndex) { Icon(Icons.Outlined.ChevronRight, "Move right") } }
                        }
                    }
                }
            }
        }
        item("reach-title") { SectionTitle("Within Reach", "Personalized next steps, with tracked goals first.") }
        if (dashboard.recommendations.isEmpty()) item("no-reach") { EmptyCard("Keep exploring—your next reachable goal will appear here.") }
        items(dashboard.recommendations, key = { "reach:${it.badgeId}" }) { badge ->
            ProgressBadgeRow(badge, { details = badge }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate) })
        }
        val recent = dashboard.badges.filter { it.earned && it.lastAdvancedAt != null }.sortedByDescending { it.lastAdvancedAt }.take(5)
        if (recent.isNotEmpty()) {
            item("recent-title") { SectionTitle("Recently Earned and Updated", "New and advanced achievements.") }
            items(recent, key = { "recent:${it.badgeId}" }) { BadgeGridRow(it, { details = it }, { requestPin(it, dashboard, onPin) { replacement = it } }) }
        }
        item("earned-title") { SectionTitle("Your Badges", "${visible.count { it.earned }} unique badges shown") }
        val earned = visible.filter { it.earned }
        if (earned.isEmpty()) item("empty-earned") { EmptyCard(if (query.isBlank()) "Complete a Flow or explore The Blue to earn your first badge." else "No badges match “$query”.") }
        items(earned, key = { "earned:${it.badgeId}" }) { BadgeGridRow(it, { details = it }, { requestPin(it, dashboard, onPin) { replacement = it } }) }
        item("collections-title") { SectionTitle("Collection Progress", "Lifetime discovery and Mastery survive release.") }
        items(dashboard.collections, key = { it.collectionId }) { CollectionCard(it) }
        item("book-title") { SectionTitle("Badge Book", "Earned and locked progression goals.") }
        items(visible, key = { "book:${it.badgeId}" }) { badge -> ProgressBadgeRow(badge, { details = badge }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate) }) }
    }
    details?.let { badge -> BadgeDetailsDialog(badge, { details = null }, {
        if (badge.pinnedOrder != null) onUnpin(badge.badgeId) else requestPin(badge, dashboard, onPin) { replacement = badge }
    }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }) }
    replacement?.let { requested ->
        AlertDialog(onDismissRequest = { replacement = null }, title = { Text("Replace a showcase badge?") },
            text = { Column { Text("Choose which pinned badge to replace."); dashboard.badges.filter { it.pinnedOrder != null }.sortedBy { it.pinnedOrder }.forEach { pin -> TextButton({ onPin(requested.badgeId, pin.badgeId); replacement = null }) { Text(badgeTitle(pin)) } } } },
            confirmButton = {}, dismissButton = { TextButton({ replacement = null }) { Text("Cancel") } })
    }
    uiState.backfillSummary?.let { summary ->
        AlertDialog(onDismissRequest = { onAcknowledgeBackfill(summary.version) },
            title = { Text("Your Badge Collection Has Grown") },
            text = { Text("We recognized ${summary.discoveredCount} creature discoveries, ${summary.masteryCount} Creature Masteries, and ${summary.completionCount} collection completions from your existing progress.") },
            confirmButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text("View New Badges") } },
            dismissButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text("Continue") } })
    }
}

private fun requestPin(badge: BadgeProgressModel, dashboard: BadgeDashboard, onPin: (String, String?) -> Unit, full: () -> Unit) {
    if (badge.pinnedOrder != null) return
    if (dashboard.badges.count { it.pinnedOrder != null } >= 3) full() else onPin(badge.badgeId, null)
}
private fun badgeSearchText(b: BadgeProgressModel) = listOf(b.badgeId, b.category.name, b.collectionProgress?.collectionId, AchievementBadgeCatalog.byId[b.badgeId]?.speciesId?.let { CreatureCatalog.get(it)?.displayName }).joinToString(" ")
private fun badgeComparator(sort: BadgeSort): Comparator<BadgeProgressModel> = when(sort) {
    BadgeSort.RECOMMENDED -> compareBy<BadgeProgressModel> { it.pinnedOrder ?: 99 }.thenByDescending { it.earned }.thenBy { it.remaining }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_EARNED -> compareByDescending<BadgeProgressModel> { it.firstEarnedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_ADVANCED -> compareByDescending<BadgeProgressModel> { it.lastAdvancedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.HIGHEST_COUNT -> compareByDescending<BadgeProgressModel> { it.count }.thenBy { it.badgeId }
    BadgeSort.CLOSEST_MILESTONE -> compareBy<BadgeProgressModel> { it.remaining }.thenBy { it.badgeId }
    BadgeSort.ALPHABETICAL -> compareBy { it.badgeId }
}

@Composable private fun badgeTitle(badge: BadgeProgressModel): String {
    ShellContentCatalog.badge(badge.badgeId)?.let { return androidx.compose.ui.res.stringResource(it.titleRes) }
    AchievementBadgeCatalog.byId[badge.badgeId]?.speciesId?.let { return "${CreatureCatalog.get(it)?.displayName ?: "Creature"} Mastery" }
    val collection = badge.collectionProgress?.collectionId?.removePrefix("blue_")?.removePrefix("collection_")?.replace('_',' ')?.replaceFirstChar { it.titlecase() }
    if (collection != null) return "$collection ${badge.badgeId.substringAfterLast('_').replaceFirstChar { it.titlecase() }}"
    return badge.badgeId.replace('_',' ').replaceFirstChar { it.titlecase() }
}

enum class BadgeMedallionSize { Small, Medium, Large }
@Composable fun BadgeMedallion(badge: BadgeProgressModel, size: BadgeMedallionSize = BadgeMedallionSize.Medium, onClick: () -> Unit = {}) {
    val diameter = when(size) { BadgeMedallionSize.Small -> 56.dp; BadgeMedallionSize.Medium -> 72.dp; BadgeMedallionSize.Large -> 88.dp }
    val title = badgeTitle(badge); val exact = NumberFormat.getIntegerInstance().format(badge.count)
    val semantics = "$title badge. ${if (badge.earned) "Earned" else "Locked"}. Completed $exact times. ${badge.remaining} remaining.${if (badge.pinnedOrder != null) " Pinned." else ""}${if (badge.tracked) " Tracked." else ""}"
    Box(Modifier.size(diameter + 18.dp).semantics(mergeDescendants = true) { contentDescription = semantics }.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { if (badge.target == 0) 0f else badge.progress.toFloat()/badge.target }, Modifier.size(diameter + 8.dp), strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
        Surface(Modifier.size(diameter).clip(CircleShape), shape = CircleShape, color = if (badge.earned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) { Icon(if (badge.earned) Icons.Outlined.MilitaryTech else Icons.Outlined.Lock, null, Modifier.size(diameter/2)); Surface(Modifier.align(Alignment.BottomCenter), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface) { Text("×${compactCount(badge.count)}", Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1) } }
        }
        if (badge.pinnedOrder != null) Icon(Icons.Outlined.PushPin, null, Modifier.align(Alignment.TopEnd).size(18.dp))
        if (badge.tracked) Icon(Icons.Outlined.TrackChanges, null, Modifier.align(Alignment.TopStart).size(18.dp))
    }
}
internal fun compactCount(count: Int): String = if (count <= 999) count.toString() else NumberFormat.getCompactNumberInstance().format(count)

@Composable private fun BadgeGridRow(badge: BadgeProgressModel, open: () -> Unit, pin: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = open)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); Text("Exact count: ${NumberFormat.getIntegerInstance().format(badge.count)}", style = MaterialTheme.typography.bodySmall) }; IconButton(pin) { Icon(if (badge.pinnedOrder != null) Icons.Outlined.PushPin else Icons.Outlined.AddCircleOutline, if (badge.pinnedOrder != null) "Pinned" else "Pin") } } } }
@Composable private fun ProgressBadgeRow(badge: BadgeProgressModel, open: () -> Unit, track: () -> Unit, action: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); Text("${badge.progress} of ${badge.target} · ${badge.remaining} remaining", style = MaterialTheme.typography.bodySmall); LinearProgressIndicator({ if (badge.target == 0) 0f else badge.progress.toFloat()/badge.target }, Modifier.fillMaxWidth().semantics { progressBarRangeInfo = ProgressBarRangeInfo(badge.progress.toFloat(), 0f..badge.target.toFloat()) }) }; IconButton(track) { Icon(if (badge.tracked) Icons.Outlined.TrackChanges else Icons.Outlined.AddTask, if (badge.tracked) "Stop tracking" else "Track") }; IconButton(action) { Icon(Icons.Outlined.ArrowForward, "Open progression action") } } } }
@Composable private fun CollectionCard(p: CollectionProgress) { OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(p.collectionId.removePrefix("blue_").removePrefix("collection_").replace('_',' ').replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.Bold); Text("${p.discoveredSpeciesCount} of ${p.totalParticipatingSpecies} discovered"); Text("${p.masteredSpeciesCount} of ${p.totalParticipatingSpecies} mastered"); Text(listOfNotNull(if(p.collectorEarned) "Collector achieved" else null, if(p.completionistEarned) "Completionist achieved" else null).joinToString(" · "), color = MaterialTheme.colorScheme.primary) } } }
@Composable private fun BadgeDetailsDialog(b: BadgeProgressModel, dismiss: () -> Unit, pin: () -> Unit, track: () -> Unit) { AlertDialog(onDismissRequest = dismiss, icon = { BadgeMedallion(b, BadgeMedallionSize.Large) }, title = { Text(badgeTitle(b)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Exact count: ${NumberFormat.getIntegerInstance().format(b.count)}"); Text(if(b.earned) "Earned" else "Locked"); Text("Progress: ${b.progress} of ${b.target}. ${b.remaining} remaining."); b.milestone.currentThreshold?.let { Text("Current milestone: $it") }; b.milestone.nextThreshold?.let { Text("Next milestone: $it") }; b.collectionProgress?.let { Text("Discovered ${it.discoveredSpeciesCount}; owned ${it.currentlyOwnedSpeciesCount}; mastered ${it.masteredSpeciesCount}.") } } }, confirmButton = { TextButton(pin) { Text(if(b.pinnedOrder != null) "Unpin" else "Pin") } }, dismissButton = { TextButton(track) { Text(if(b.tracked) "Stop tracking" else "Track") } }) }
@Composable private fun SectionTitle(title: String, body: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun EmptyCard(text: String) { OutlinedCard(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(18.dp)) } }
@Composable private fun <T> BadgeMenu(label: String, values: List<T>, text: (T)->String, selected: (T)->Unit) { var open by remember { mutableStateOf(false) }; Box { OutlinedButton({open=true}) { Text(label, maxLines=1, overflow=TextOverflow.Ellipsis) }; DropdownMenu(open, {open=false}) { values.forEach { DropdownMenuItem({Text(text(it))}, { selected(it); open=false }) } } } }
private fun navigateFor(b: BadgeProgressModel, navigate: (ShellDestination)->Unit) { navigate(when(b.action) { "chest" -> ShellDestination.ShellChest; "stillwater" -> ShellDestination.Stillwater; else -> ShellDestination.TheBluePreview }) }
