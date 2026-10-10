package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraRoomTabRow
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.AchievementInitializationState
import com.kingkharnivore.skillz.ui.screen.shell.NavigationConsumptionResult
import com.kingkharnivore.skillz.ui.screen.shell.NavigationFailureReason
import com.kingkharnivore.skillz.ui.screen.shell.PendingShellNavigation
import java.text.NumberFormat
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.CompositionLocalProvider

enum class BadgesTab { SHOWCASE, BADGE_BOOK, WITHIN_REACH, PROGRESS;
    val showsBadgeBookControls: Boolean get() = this == BADGE_BOOK
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    onOpenArc: () -> Unit,
    onRetryInitialization: () -> Unit = {},
    pendingNavigation: PendingShellNavigation? = null,
    onNavigationResult: (String, NavigationConsumptionResult) -> Unit = { _, _ -> },
    initialTab: BadgesTab = BadgesTab.SHOWCASE,
    initialBrowseCollections: Boolean = true
) {
    val dashboard = uiState.badgeDashboard
    var browseCollections by rememberSaveable { mutableStateOf(initialBrowseCollections) }
    var query by rememberSaveable { mutableStateOf("") }
    var earnedCategory by rememberSaveable { mutableStateOf(BadgeUiCategory.ALL) }
    var earnedSort by rememberSaveable { mutableStateOf(BadgeSort.ALPHABETICAL) }
    val category = uiState.badgeCategory
    val sort = uiState.badgeSort
    var detailsBadgeId by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionDetailsId by rememberSaveable { mutableStateOf<String?>(null) }
    fun openBadge(badge: BadgeProgressModel) { detailsBadgeId = badge.badgeId; onBadgeViewed(badge.badgeId) }
    if (dashboard == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val details = detailsBadgeId?.let { id -> dashboard.badges.firstOrNull { it.badgeId == id } }
    val bookCollectionDetails = collectionDetailsId?.let(BadgeBookCollections.byId::get)
    val collectionDetails = collectionDetailsId?.let { id -> dashboard.collections.firstOrNull { it.collectionId == id } }
    LaunchedEffect(detailsBadgeId, details) { if (detailsBadgeId != null && details == null) detailsBadgeId = null }
    LaunchedEffect(collectionDetailsId, collectionDetails) {
        if (collectionDetailsId != null && collectionDetails == null && bookCollectionDetails == null) collectionDetailsId = null
    }
    val objectiveMetadata = remember(uiState.objectiveCompletions) {
        com.kingkharnivore.skillz.domain.lookout.objectiveBadgePresentationMetadata(uiState.objectiveCompletions)
    }
    val presentations = dashboard.badges.associate { badge ->
        badge.badgeId to resolveBadgePresentation(badge.badgeId, objectiveMetadata[badge.badgeId])
    }
    val localizedCreatureNames = dashboard.badges.associate { badge ->
        val creature = BadgeDefinitionResolver.resolve(badge.badgeId).speciesId?.let(CreatureCatalog::get)
        badge.badgeId to (creature?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) }.orEmpty())
    }
    val categoryLabels = mapOf(
        BadgeUiCategory.GREEN to stringResource(R.string.green_title),
        BadgeUiCategory.ALL to stringResource(R.string.badge_category_all), BadgeUiCategory.FLOW to stringResource(R.string.badge_category_flow),
        BadgeUiCategory.ARC to stringResource(R.string.badge_category_arc), BadgeUiCategory.CREATURES to stringResource(R.string.badge_category_creatures),
        BadgeUiCategory.MASTERY to stringResource(R.string.badge_category_mastery), BadgeUiCategory.COLLECTIONS to stringResource(R.string.badge_category_collections),
        BadgeUiCategory.STILLWATER to stringResource(R.string.badge_category_stillwater), BadgeUiCategory.MOVEMENT to stringResource(R.string.badge_category_movement),
        BadgeUiCategory.SURGE to stringResource(R.string.badge_category_surge), BadgeUiCategory.OBJECTIVES to stringResource(R.string.badge_category_objectives),
        BadgeUiCategory.SPECIAL to stringResource(R.string.badge_category_special)
    )
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val collator = remember(locale) { java.text.Collator.getInstance(locale) }
    val sortLabels = mapOf(
        BadgeSort.RECOMMENDED to stringResource(R.string.badge_sort_recommended), BadgeSort.RECENTLY_EARNED to stringResource(R.string.badge_sort_recent_earned),
        BadgeSort.RECENTLY_ADVANCED to stringResource(R.string.badge_sort_recent_advanced), BadgeSort.HIGHEST_COUNT to stringResource(R.string.badge_sort_highest_count),
        BadgeSort.CLOSEST_MILESTONE to stringResource(R.string.badge_sort_closest), BadgeSort.ALPHABETICAL to stringResource(R.string.badge_sort_alphabetical)
    )
    val availableCategories = remember(dashboard.badges) {
        listOf(BadgeUiCategory.ALL) + dashboard.badges.map { it.category }.distinct()
    }
    val earnedBadges = remember(dashboard.badges) { dashboard.badges.filter { it.earned } }
    val earnedCategories = remember(earnedBadges) {
        listOf(BadgeUiCategory.ALL) + earnedBadges.map { it.category }.distinct()
    }
    val visibleEarnedBadges = remember(earnedBadges, presentations, earnedCategory, earnedSort, collator) {
        earnedBadges.filter { earnedCategory == BadgeUiCategory.ALL || it.category == earnedCategory }
            .sortedWith(badgeComparator(earnedSort, presentations, collator))
    }
    val visible = remember(dashboard.badges, presentations, localizedCreatureNames, categoryLabels, query, category, sort, collator) {
        dashboard.badges.filter { badge ->
            (category == BadgeUiCategory.ALL || badge.category == category) &&
                (query.isBlank() || badgeSearchText(presentations.getValue(badge.badgeId), categoryLabels.getValue(badge.category), localizedCreatureNames[badge.badgeId].orEmpty()).contains(query.trim(), ignoreCase = true))
        }.sortedWith(badgeComparator(sort, presentations, collator))
    }
    val requestedInitialTab = when (val request = pendingNavigation) {
        is PendingShellNavigation.OpenCollection -> if (pendingNavigation.collectionId in BadgeBookCollections.byId) BadgesTab.BADGE_BOOK else BadgesTab.PROGRESS
        is PendingShellNavigation.OpenBadge -> dashboard.badges.firstOrNull { it.badgeId == request.badgeId }?.let {
            when {
                it.tracked -> BadgesTab.BADGE_BOOK
                it.everEarned || it.recentlyUpdated -> BadgesTab.PROGRESS
                else -> BadgesTab.BADGE_BOOK
            }
        } ?: BadgesTab.SHOWCASE
        else -> initialTab
    }
    var selectedTab by rememberSaveable { mutableStateOf(requestedInitialTab) }
    val tabs = BadgesTab.entries
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal, pageCount = { tabs.size })
    val tabScope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) { selectedTab = tabs[pagerState.currentPage] }
    val tabLabels = mapOf(
        BadgesTab.SHOWCASE to stringResource(R.string.badges_tab_showcase),
        BadgesTab.BADGE_BOOK to stringResource(R.string.badges_tab_book),
        BadgesTab.WITHIN_REACH to stringResource(R.string.badges_tab_reach),
        BadgesTab.PROGRESS to stringResource(R.string.badges_tab_progress)
    )
    val showcaseListState = rememberLazyListState()
    val bookListState = rememberLazyListState()
    val reachListState = rememberLazyListState()
    val progressListState = rememberLazyListState()
    val tabListStates = listOf(showcaseListState, bookListState, reachListState, progressListState)
    LaunchedEffect(pendingNavigation, dashboard, uiState.achievementInitializationState) {
        when (val request = pendingNavigation) {
            is PendingShellNavigation.OpenBadge -> {
                val requested = dashboard.badges.firstOrNull { it.badgeId == request.badgeId }
                if (requested == null) {
                    if (uiState.achievementInitializationState !is AchievementInitializationState.Running &&
                        uiState.achievementInitializationState !is AchievementInitializationState.NotStarted
                    ) onNavigationResult(request.requestId, NavigationConsumptionResult.Failed(NavigationFailureReason.BADGE_NOT_FOUND))
                } else {
                    val targetTab = when {
                        requested.tracked -> BadgesTab.BADGE_BOOK
                        requested.everEarned || requested.recentlyUpdated -> BadgesTab.PROGRESS
                        else -> BadgesTab.BADGE_BOOK
                    }
                    selectedTab = targetTab
                    pagerState.scrollToPage(targetTab.ordinal)
                    detailsBadgeId = requested.badgeId
                    withFrameNanos { }
                    onBadgeViewed(requested.badgeId)
                    onNavigationResult(request.requestId, NavigationConsumptionResult.Consumed)
                }
            }
            is PendingShellNavigation.OpenCollection -> {
                val requested = dashboard.collections.firstOrNull { it.collectionId == request.collectionId }
                val bookCollection = BadgeBookCollections.byId[request.collectionId]
                if (requested == null && bookCollection == null) {
                    if (uiState.achievementInitializationState !is AchievementInitializationState.Running &&
                        uiState.achievementInitializationState !is AchievementInitializationState.NotStarted
                    ) onNavigationResult(request.requestId, NavigationConsumptionResult.Failed(NavigationFailureReason.COLLECTION_NOT_FOUND))
                } else {
                    selectedTab = if (bookCollection != null) BadgesTab.BADGE_BOOK else BadgesTab.PROGRESS
                    if (bookCollection != null) browseCollections = true
                    pagerState.scrollToPage(selectedTab.ordinal)
                    collectionDetailsId = request.collectionId
                    withFrameNanos { }
                    onNavigationResult(request.requestId, NavigationConsumptionResult.Consumed)
                }
            }
            else -> Unit
        }
    }
    CompositionLocalProvider(LocalObjectiveBadgePresentationMetadata provides objectiveMetadata) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoomHeader(R.string.shell_badges_title, R.string.badges_hub_body)
            Text(pluralStringResource(R.plurals.badges_summary, dashboard.completedCollections, dashboard.uniqueEarned, dashboard.totalMasteries, dashboard.completedCollections),
                style = MaterialTheme.typography.titleMedium)
            if (uiState.achievementInitializationState is AchievementInitializationState.Failed) {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.achievement_backfill_retry_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.achievement_backfill_retry_body), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRetryInitialization) {
                            Text(stringResource(R.string.achievement_backfill_retry_action))
                        }
                    }
                }
            }
        }
        Box(Modifier.padding(horizontal = 16.dp)) {
            ScyraRoomTabRow(
                tabs = tabs.map(tabLabels::getValue), selectedIndex = pagerState.currentPage,
                onSelected = { page -> tabScope.launch { pagerState.animateScrollToPage(page) } }, evenlyDistributed = false,
                accessibilityLabel = { index, title, selected ->
                    stringResource(R.string.badges_tab_a11y, title,
                        if (selected) stringResource(R.string.badges_tab_selected) else "", index + 1, tabs.size)
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
        val galleryColumns = ((maxWidth - 32.dp) / (120.dp * LocalDensity.current.fontScale)).toInt().coerceIn(1, 5)
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        LazyColumn(state = tabListStates[page], contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when (tabs[page]) {
            BadgesTab.SHOWCASE -> {
                item("showcase-title") { SectionTitle(stringResource(R.string.badges_showcase_title), stringResource(R.string.badges_showcase_body)) }
                val pins = dashboard.badges.filter { it.earned && it.pinnedOrder != null }.sortedBy { it.pinnedOrder }
                if (pins.isEmpty()) item("showcase-empty") { EmptyCard(stringResource(R.string.badges_showcase_empty)) }
                badgeGalleryRows(pins, galleryColumns, "pinned", framed = true, onOpen = ::openBadge)
                item("yours-title") { SectionTitle(stringResource(R.string.badges_yours_title), "") }
                item("yours-controls") {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgeMenu(stringResource(R.string.badges_category_selected, categoryLabels.getValue(earnedCategory)),
                            earnedCategories, { categoryLabels.getValue(it) }, { earnedCategory = it })
                        BadgeMenu(stringResource(R.string.badges_sort_selected, sortLabels.getValue(earnedSort)),
                            BadgeSort.entries, { sortLabels.getValue(it) }, { earnedSort = it })
                    }
                }
                if (earnedBadges.isEmpty()) item("yours-empty") { EmptyCard(stringResource(R.string.badges_no_earned)) }
                else if (visibleEarnedBadges.isEmpty()) item("yours-filter-empty") {
                    OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.badges_category_empty))
                            TextButton({ earnedCategory = BadgeUiCategory.ALL }) { Text(stringResource(R.string.badges_reset_filters)) }
                        }
                    }
                }
                badgeGalleryRows(visibleEarnedBadges, galleryColumns, "earned", framed = false, onOpen = ::openBadge)
            }
            BadgesTab.BADGE_BOOK -> {
                if (!browseCollections) item("book-title") { SectionTitle(stringResource(R.string.badges_book_title),
                    stringResource(if (category == BadgeUiCategory.GREEN) R.string.green_badges_explainer else R.string.badges_book_body)) }
                item("book-browse") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(browseCollections, { browseCollections = true }, label = { Text(stringResource(R.string.book_collections)) })
                        FilterChip(!browseCollections, { browseCollections = false }, label = { Text(stringResource(R.string.book_all_badges)) })
                    }
                }
                if (!browseCollections) {
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
                            { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetailsId = id } })
                    }
                } else if (dashboard.badges.any { it.tracked }) {
                    item("book-tracked-shortcut") { TextButton(onClick = { browseCollections = false }) { Text(stringResource(R.string.badges_tracked_title)) } }
                }
                if (browseCollections) {
                    item("book-collections-intro") { Text(stringResource(R.string.book_collections_body), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(BadgeBookCollections.collections, key = { it.id }) { collection ->
                        BadgeBookCollectionCard(collection, dashboard.badges) { collectionDetailsId = collection.id }
                    }
                } else {
                    item("book-controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text(stringResource(R.string.badges_search)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Clear, stringResource(R.string.badges_clear_search)) } })
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeMenu(stringResource(R.string.badges_category_selected, categoryLabels.getValue(category)), availableCategories, { categoryLabels.getValue(it) }, onCategory)
                                BadgeMenu(stringResource(R.string.badges_sort_selected, sortLabels.getValue(sort)), BadgeSort.entries, { sortLabels.getValue(it) }, onSort)
                            }
                        }
                    }
                    if (visible.isEmpty()) item("book-empty") {
                        val message = when {
                            dashboard.badges.isEmpty() -> stringResource(R.string.badges_book_empty)
                            query.isNotBlank() -> stringResource(R.string.badges_no_search_results, query)
                            else -> stringResource(R.string.badges_category_empty)
                        }
                        OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(message)
                                if (query.isNotBlank() || category != BadgeUiCategory.ALL) TextButton({ query = ""; onCategory(BadgeUiCategory.ALL) }) { Text(stringResource(R.string.badges_reset_filters)) }
                            }
                        }
                    }
                    items(visible, key = { "book:${it.badgeId}" }) { badge -> ProgressBadgeRow(badge, { openBadge(badge) }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetailsId = id } }) }
                }
            }
            BadgesTab.WITHIN_REACH -> {
                item("reach-title") { SectionTitle(stringResource(R.string.badges_within_reach_title), stringResource(R.string.badges_within_reach_body)) }
                if (dashboard.recommendations.isEmpty()) item("no-reach") { EmptyCard(stringResource(R.string.badges_no_recommendations)) }
                items(dashboard.recommendations.take(3), key = { "reach:${it.badgeId}" }) { badge ->
                    ProgressBadgeRow(badge, { openBadge(badge) }, { onTrack(badge.badgeId) }, { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { openBadge(badge) }) { id -> collectionDetailsId = id } })
                }
            }
            BadgesTab.PROGRESS -> {
                val recent = dashboard.badges.filter { it.earned && it.lastAdvancedAt != null }.sortedByDescending { it.lastAdvancedAt }.take(5)
                if (recent.isNotEmpty()) {
                    item("recent-title") { SectionTitle(stringResource(R.string.badges_recent_title), stringResource(R.string.badges_recent_body)) }
                    items(recent, key = { "recent:${it.badgeId}" }) { badge -> BadgeGridRow(badge, { openBadge(badge) }, { if (badge.pinnedOrder != null) onUnpin(badge.badgeId) else onPin(badge.badgeId, null) }) }
                    item("progress-divider") { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                }
                item("collections-title") { SectionTitle(stringResource(R.string.badges_collections_title), stringResource(R.string.badges_collections_body)) }
                items(dashboard.collections, key = { it.collectionId }) { CollectionCard(it) { collectionDetailsId = it.collectionId } }
            }
        }
    }
    }
    }
    }
    details?.let { badge -> BadgeDetailsSheet(badge, { detailsBadgeId = null }, {
        if (badge.pinnedOrder != null) onUnpin(badge.badgeId) else onPin(badge.badgeId, null)
    }, { if (badge.tracked) onUntrack(badge.badgeId) else onTrack(badge.badgeId) },
        { navigateFor(badge, onNavigate, onOpenFlow, onOpenArc, { detailsBadgeId = null }) { id ->
            collectionDetailsId = id
            detailsBadgeId = null
        } }) }
    collectionDetails?.let {
        CollectionDetailsSheet(
            p = it,
            dismiss = { collectionDetailsId = null },
            onSpeciesAction = { action -> collectionSpeciesDestination(action)?.let(onNavigate) }
        )
    }
    bookCollectionDetails?.let { collection ->
        BadgeBookCollectionSheet(collection, dashboard.badges, { collectionDetailsId = null }) { badge ->
            collectionDetailsId = null
            openBadge(badge)
        }
    }
    PinReplacementDialog(uiState, onPin, onDismissPinReplacement)
    uiState.backfillSummary?.let { summary ->
        AlertDialog(onDismissRequest = { onAcknowledgeBackfill(summary.version) }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.badges_backfill_title)) },
            text = { Text(stringResource(R.string.badges_backfill_body, summary.discoveredCount, summary.masteryCount, summary.completionCount)) },
            confirmButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text(stringResource(R.string.badges_view_new)) } },
            dismissButton = { TextButton({ onAcknowledgeBackfill(summary.version) }) { Text(stringResource(R.string.mastery_continue)) } })
    }
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
private fun badgeComparator(sort: BadgeSort, presentations: Map<String, BadgePresentation>, collator: java.text.Collator): Comparator<BadgeProgressModel> = when(sort) {
    BadgeSort.RECOMMENDED -> compareBy<BadgeProgressModel> { it.pinnedOrder ?: Int.MAX_VALUE }.thenByDescending { it.earned }.thenBy { it.remaining }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_EARNED -> compareByDescending<BadgeProgressModel> { it.firstEarnedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.RECENTLY_ADVANCED -> compareByDescending<BadgeProgressModel> { it.lastAdvancedAt ?: Long.MIN_VALUE }.thenBy { it.badgeId }
    BadgeSort.HIGHEST_COUNT -> compareByDescending<BadgeProgressModel> { it.count }.thenBy { it.badgeId }
    BadgeSort.CLOSEST_MILESTONE -> compareBy<BadgeProgressModel> { it.remaining }.thenBy { it.badgeId }
    BadgeSort.ALPHABETICAL -> Comparator { left, right ->
        collator.compare(
            presentations[left.badgeId]?.title.orEmpty(),
            presentations[right.badgeId]?.title.orEmpty()
        ).takeIf { it != 0 } ?: left.badgeId.compareTo(right.badgeId)
    }
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
    val isRed = com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog.byId.containsKey(badge.badgeId)
    val background = badgeMedallionBackground(badge)
    val accent = badgeMedallionAccent(badge,background)
    val scheme = MaterialTheme.colorScheme
    val artwork: @Composable () -> Unit = {
        MaterialTheme(colorScheme=if(isRed) scheme.copy(tertiary=accent) else scheme.copy(primary=accent)) {
            BadgeCoreArtwork(presentation,diameter)
        }
    }
    val title = presentation.title; val exact = localizedBadgeCount(badge.count)
    val semantics = if (badge.countType == BadgeCountType.ONE_TIME) stringResource(
        R.string.badge_one_time_a11y, title,
        if (badge.earned) stringResource(R.string.badge_earned) else stringResource(R.string.badge_locked),
        badge.remaining, if (badge.pinnedOrder != null) stringResource(R.string.badge_pinned_a11y) else "",
        if (badge.tracked) stringResource(R.string.badge_tracked_a11y) else ""
    ) else stringResource(R.string.badge_count_a11y, title,
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
        CircularProgressIndicator(progress = { ringProgress }, Modifier.size(diameter + 8.dp), color = accent, strokeWidth = 4.dp, strokeCap = StrokeCap.Round)
        Surface(Modifier.size(diameter).clip(CircleShape), shape = CircleShape,
            color = background,
            border = if (badge.earned) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            shadowElevation = if (badge.earned) 2.dp else 0.dp) {
            Box(contentAlignment = Alignment.Center) {
                if (isRed) androidx.compose.foundation.layout.Box(Modifier.then(if(badge.earned) Modifier else Modifier.alpha(.35f))) { artwork() }
                else if (badge.earned) artwork()
                else Icon(Icons.Outlined.Lock, null, Modifier.size(diameter * .45f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (badge.earned && badge.count > 0 && badge.countType == BadgeCountType.REPEATABLE) {
                    Surface(Modifier.align(Alignment.BottomCenter), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.inverseSurface) { Text(stringResource(R.string.badge_count_plate, localizedBadgeCount(badge.count)), Modifier.padding(horizontal = 7.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
        }
        if (badge.pinnedOrder != null) Icon(Icons.Outlined.PushPin, null, Modifier.align(Alignment.TopEnd).size(18.dp))
        if (badge.tracked) Icon(Icons.Outlined.TrackChanges, null, Modifier.align(Alignment.TopStart).size(18.dp))
    }
}
@Composable
internal fun badgeMedallionBackground(badge: BadgeProgressModel): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    val base = if (badge.earned) scheme.surface else scheme.surfaceVariant
    if (badge.countType != BadgeCountType.ONE_TIME) return base
    val tint = if (RedBadgeCatalog.byId.containsKey(badge.badgeId)) scheme.tertiary else scheme.primary
    return androidx.compose.ui.graphics.lerp(base, tint, if (badge.earned) .12f else .06f)
}

@Composable
internal fun badgeMedallionAccent(badge: BadgeProgressModel, background: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    val accent = if (RedBadgeCatalog.byId.containsKey(badge.badgeId)) scheme.tertiary else scheme.primary
    if (badge.countType != BadgeCountType.ONE_TIME || !badge.earned) return accent
    val backdrop = background.luminance()
    val target = if (backdrop > accent.luminance()) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
    // Preserve the accent hue while keeping earned symbols legible on the new tint.
    for (step in 0..10) {
        val candidate = androidx.compose.ui.graphics.lerp(accent,target,step / 10f)
        val light = candidate.luminance()
        if ((maxOf(light,backdrop)+.05f)/(minOf(light,backdrop)+.05f) >= 3f) return candidate
    }
    return target
}

internal fun compactCount(count: Int, locale: java.util.Locale = java.util.Locale.getDefault()): String {
    return NumberFormat.getIntegerInstance(locale).format(count)
}

internal fun showsMilestoneProgress(badge: BadgeProgressModel): Boolean =
    badge.countType == BadgeCountType.REPEATABLE && !badge.terminal && badge.nextMilestoneTarget != null
internal fun showsObjectiveProgress(badge: BadgeProgressModel): Boolean =
    badge.countType == BadgeCountType.ONE_TIME && badge.objectiveTarget > 0 &&
        (!badge.everEarned || badge.currentRosterComplete == false)

internal enum class BadgeProgressPresentationState { EARNED, OBJECTIVE, RESTORATION, MILESTONE, EXHAUSTED }
internal fun badgeProgressPresentationState(badge: BadgeProgressModel): BadgeProgressPresentationState = when {
    badge.everEarned && badge.currentRosterComplete == false -> BadgeProgressPresentationState.RESTORATION
    badge.countType == BadgeCountType.ONE_TIME && !badge.everEarned -> BadgeProgressPresentationState.OBJECTIVE
    badge.terminal && badge.everEarned -> BadgeProgressPresentationState.EARNED
    badge.countType == BadgeCountType.REPEATABLE && badge.nextMilestoneTarget == null -> BadgeProgressPresentationState.EXHAUSTED
    else -> BadgeProgressPresentationState.MILESTONE
}
@Composable private fun BadgeGridRow(badge: BadgeProgressModel, open: () -> Unit, pin: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = open), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); if (badge.countType == BadgeCountType.REPEATABLE) Text(stringResource(R.string.badge_exact_count, localizedBadgeCount(badge.count)), style = MaterialTheme.typography.bodySmall); if (badge.newlyEarned) Text(stringResource(R.string.badge_new), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) else if (badge.recentlyUpdated) Text(stringResource(R.string.badge_updated), color = MaterialTheme.colorScheme.primary) }; IconButton(pin) { Icon(if (badge.pinnedOrder != null) Icons.Outlined.PushPin else Icons.Outlined.AddCircleOutline, if (badge.pinnedOrder != null) stringResource(R.string.badge_unpin) else stringResource(R.string.badge_pin)) } } } }
@Composable private fun ProgressBadgeRow(badge: BadgeProgressModel, open: () -> Unit, track: () -> Unit, action: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { BadgeMedallion(badge, BadgeMedallionSize.Small, open); Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(badgeTitle(badge), fontWeight = FontWeight.Bold); Text(resolveBadgePresentation(badge.badgeId).description, style = MaterialTheme.typography.bodySmall); Text(when (badgeProgressPresentationState(badge)) {
    BadgeProgressPresentationState.EARNED -> stringResource(R.string.badge_earned)
    BadgeProgressPresentationState.OBJECTIVE -> badgeObjectiveProgressText(badge.badgeId, badge.currentProgress, badge.objectiveTarget, badge.remaining)
    BadgeProgressPresentationState.RESTORATION -> stringResource(R.string.badge_current_roster_progress, badge.currentProgress, badge.objectiveTarget)
    BadgeProgressPresentationState.EXHAUSTED -> stringResource(R.string.badge_all_milestones_reached)
    BadgeProgressPresentationState.MILESTONE -> recommendationText(badge)
}, style = MaterialTheme.typography.bodySmall, color = if (badge.everEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); if (showsMilestoneProgress(badge) || showsObjectiveProgress(badge)) LinearProgressIndicator({ (badge.progress.toFloat()/badge.target).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().semantics { progressBarRangeInfo = ProgressBarRangeInfo(badge.progress.toFloat().coerceAtMost(badge.target.toFloat()), 0f..badge.target.toFloat()) }) }; Column(horizontalAlignment = Alignment.End) { if (badge.tracked || badge.canTrack) TextButton(track) { Text(if (badge.tracked) stringResource(R.string.badge_untrack) else stringResource(R.string.badge_track)) }; if (badge.canNavigate) TextButton(action) { Text(badgeActionLabel(badge.action)) } } } } }
@Composable private fun CollectionCard(p: CollectionProgress, onClick: () -> Unit) { OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(collectionDisplayName(p.collectionId), fontWeight = FontWeight.Bold); Text(stringResource(R.string.collection_discovered_progress, p.discoveredSpeciesCount, p.totalParticipatingSpecies)); Text(stringResource(R.string.collection_owned_progress, p.currentlyOwnedSpeciesCount, p.totalParticipatingSpecies)); Text(stringResource(R.string.collection_mastered_progress, p.masteredSpeciesCount, p.totalCompletionistSpecies)); Text(listOfNotNull(if(p.collectorEarned) stringResource(R.string.badge_state_collector) else null, if(p.curatorEarned) stringResource(R.string.badge_state_curator) else null, if(p.completionistEarned) stringResource(R.string.badge_state_completionist) else null).joinToString(" · "), color = MaterialTheme.colorScheme.primary) } } }
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable internal fun BadgeDetailsSheet(b: BadgeProgressModel, dismiss: () -> Unit, pin: () -> Unit, track: () -> Unit, action: () -> Unit) { ScyraParchmentSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { val presentation = resolveBadgePresentation(b.badgeId); BadgeMedallion(b, BadgeMedallionSize.Large); Text(presentation.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(presentation.description); b.specialProgress?.let { SpecialBadgeChecklist(it) }; if (b.countType == BadgeCountType.REPEATABLE) Text(stringResource(R.string.badge_exact_count, localizedBadgeCount(b.count))); Text(if(b.earned) stringResource(R.string.badge_earned) else stringResource(R.string.badge_locked)); if (showsMilestoneProgress(b) || showsObjectiveProgress(b)) Text(badgeObjectiveProgressText(b.badgeId, b.progress, b.target, b.remaining)); if (b.everEarned && b.currentRosterComplete == false) Text(stringResource(R.string.badge_current_roster_progress, b.currentProgress, b.objectiveTarget)); if (b.countType == BadgeCountType.REPEATABLE && b.nextMilestoneTarget == null) Text(stringResource(R.string.badge_all_milestones_reached)); b.disabledReason?.let { Text(badgeDisabledText(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (showsMilestoneProgress(b)) b.milestone.nextThreshold?.let { next -> b.milestone.currentThreshold?.let { Text(stringResource(R.string.badge_current_milestone, it)) }; Text(stringResource(R.string.badge_next_milestone, next)) }; if (b.everEarned) Text(badgeEarnedDateText(b)); b.lastAdvancedAt?.takeIf { it != b.firstEarnedAt }?.let { Text(stringResource(R.string.badge_last_advanced, formatBadgeDate(it))) }; if (b.tracked || b.canTrack) Text(stringResource(R.string.badge_tracking_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { if (b.earned && b.pinnable) OutlinedButton(pin) { Text(if(b.pinnedOrder != null) stringResource(R.string.badge_unpin) else stringResource(R.string.badge_pin)) }; if (b.tracked || b.canTrack) OutlinedButton(track) { Text(if(b.tracked) stringResource(R.string.badge_untrack) else stringResource(R.string.badge_track)) }; if (b.canNavigate) Button(action) { Text(badgeActionLabel(b.action)) } }; Spacer(Modifier.height(24.dp)) } } }

@Composable private fun badgeEarnedDateText(badge: BadgeProgressModel): String = when (badge.timestampConfidence) {
    AchievementTimestampConfidence.EXACT -> badge.firstEarnedAt?.let {
        stringResource(R.string.badge_first_earned, formatBadgeDate(it))
    } ?: stringResource(R.string.badge_earned_date_unavailable)
    AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION -> badge.firstEarnedAt?.let {
        stringResource(R.string.badge_earned_date_estimated, formatBadgeDate(it))
    } ?: stringResource(R.string.badge_earned_date_unavailable)
    AchievementTimestampConfidence.UNKNOWN -> stringResource(R.string.badge_earned_date_unavailable)
}
@Composable private fun SpecialBadgeChecklist(progress: SpecialBadgeProgress) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        progress.requirements.forEach { requirement ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (requirement.complete) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (requirement.complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(collectionDisplayName(requirement.collectionId), modifier = Modifier.weight(1f))
                Text(if (requirement.complete) stringResource(R.string.badge_earned) else stringResource(R.string.badge_locked))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CollectionDetailsSheet(
    p: CollectionProgress,
    dismiss: () -> Unit,
    onSpeciesAction: ((CollectionSpeciesAction) -> Unit)? = null,
    initialFocusSpeciesId: String? = null,
    initialFocusRequestId: String? = null,
    onFocusResult: ((NavigationConsumptionResult) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var handledFocusRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(initialFocusSpeciesId, initialFocusRequestId, p.speciesStates) {
        val requestedId = initialFocusSpeciesId ?: return@LaunchedEffect
        val requestKey = initialFocusRequestId ?: requestedId
        if (handledFocusRequestId == requestKey) return@LaunchedEffect
        val index = p.speciesStates.indexOfFirst { it.speciesId == requestedId }
        if (index < 0) {
            handledFocusRequestId = requestKey
            onFocusResult?.invoke(NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND))
            return@LaunchedEffect
        }
        val state = p.speciesStates[index]
        if (state.secret && !state.discovered) {
            handledFocusRequestId = requestKey
            onFocusResult?.invoke(NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE))
            return@LaunchedEffect
        }
        listState.scrollToItem(index + 2)
        withFrameNanos { }
        handledFocusRequestId = requestKey
        onFocusResult?.invoke(NavigationConsumptionResult.Consumed)
    }
    ScyraParchmentSheet(onDismissRequest = dismiss) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item("title") { Text(collectionDisplayName(p.collectionId), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            item("summary") { Text(stringResource(R.string.collection_detail_summary, p.discoveredSpeciesCount, p.currentlyOwnedSpeciesCount, p.masteredSpeciesCount, p.totalParticipatingSpecies)) }
            p.historicalCompletions.entries.sortedBy { it.key.ordinal }.forEach { (requirement, evidence) ->
                item("history-${requirement.name}") {
                    Column {
                        Text(stringResource(when (requirement) {
                            BadgeRequirement.COLLECTOR -> R.string.badge_state_collector
                            BadgeRequirement.CURATOR -> R.string.badge_state_curator
                            BadgeRequirement.COMPLETIONIST -> R.string.badge_state_completionist
                            BadgeRequirement.EXACT_COUNT -> R.string.badge_earned
                        }), fontWeight = FontWeight.SemiBold)
                        Text(when (evidence.timestampConfidence) {
                            AchievementTimestampConfidence.EXACT -> evidence.completedAt?.let { stringResource(R.string.badge_first_earned, formatBadgeDate(it)) } ?: stringResource(R.string.badge_earned_date_unavailable)
                            AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION -> evidence.completedAt?.let { stringResource(R.string.badge_earned_date_estimated, formatBadgeDate(it)) } ?: stringResource(R.string.badge_earned_date_unavailable)
                            AchievementTimestampConfidence.UNKNOWN -> stringResource(R.string.badge_earned_date_unavailable)
                        })
                    }
                }
            }
            items(p.speciesStates, key = { it.speciesId }) { state ->
                CollectionSpeciesRow(state, onSpeciesAction, focused = state.speciesId == initialFocusSpeciesId)
            }
            item("spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
@Composable private fun CollectionSpeciesList(p: CollectionProgress) { p.speciesStates.forEach { CollectionSpeciesRow(it, null) } }
@Composable private fun CollectionSpeciesRow(state: CollectionSpeciesProgress, onAction: ((CollectionSpeciesAction) -> Unit)?, focused: Boolean = false) { val creature = CreatureCatalog.get(state.speciesId); val hidden = state.secret && !state.discovered; val actionable = state.action !is CollectionSpeciesAction.None && onAction != null; val rowModifier = if (actionable) Modifier.clickable { onAction?.invoke(state.action) } else Modifier; ListItem(modifier = rowModifier, colors = ListItemDefaults.colors(containerColor = if (focused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface), leadingContent = { ShellObjectIcon(if(hidden) "unknown_creature" else creature?.staticIconKey ?: "unknown_creature", Modifier.size(48.dp)) }, headlineContent = { Text(if(hidden) stringResource(R.string.collection_secret_species) else creature?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) } ?: stringResource(R.string.badge_creature_fallback)) }, trailingContent = { if (actionable) Icon(Icons.Outlined.ArrowForward, stringResource(R.string.collection_species_open_action)) }, supportingContent = { Column { Text(when { state.mastered -> stringResource(R.string.collection_species_mastered_count, state.lifetimeMasteryCount, state.currentLevel99Count); state.ownedCount > 0 -> stringResource(R.string.collection_species_owned, state.ownedCount, state.highestLevel ?: 1, 99 - (state.highestLevel ?: 1)); state.discovered -> stringResource(R.string.collection_species_discovered_not_owned); else -> stringResource(R.string.collection_species_undiscovered) }); if (state.mastered) Text(when (state.timestampConfidence) { AchievementTimestampConfidence.EXACT -> state.firstMasteryAt?.let { stringResource(R.string.mastery_date_exact, formatBadgeDate(it)) } ?: stringResource(R.string.mastery_date_unknown); AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION -> state.firstMasteryAt?.let { stringResource(R.string.mastery_date_estimated, formatBadgeDate(it)) } ?: stringResource(R.string.mastery_date_unknown); AchievementTimestampConfidence.UNKNOWN -> stringResource(R.string.mastery_date_unknown) }, style = MaterialTheme.typography.bodySmall) } }) }
internal fun collectionSpeciesDestination(action: CollectionSpeciesAction): BadgeActionDestination? = when (action) {
    is CollectionSpeciesAction.ViewInChest -> BadgeActionDestination.ChestSpecies(action.speciesId)
    is CollectionSpeciesAction.OpenBlueRegion -> BadgeActionDestination.BlueRegion(action.collectionId, action.speciesId)
    is CollectionSpeciesAction.OpenBeyondBlue -> BadgeActionDestination.BeyondBlue(action.collectionId, action.speciesId)
    is CollectionSpeciesAction.OpenStillwaterVessel -> BadgeActionDestination.StillwaterVessel(action.collectionId, action.speciesId)
    CollectionSpeciesAction.None -> null
}
@Composable private fun formatBadgeDate(timestamp: Long): String = java.text.DateFormat.getDateInstance(
    java.text.DateFormat.MEDIUM, androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
).format(java.util.Date(timestamp))

@Composable private fun localizedBadgeCount(count: Int): String =
    compactCount(count, androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
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
    is BadgeActionDestination.Green -> R.string.green_view_plant
    is BadgeActionDestination.ChestSpecies -> if (action.speciesId in com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog.byId) R.string.red_badge_open_creature else R.string.badge_action_view_chest
    is BadgeActionDestination.BlueRegion -> R.string.badge_action_open_blue
    is BadgeActionDestination.StillwaterVessel -> R.string.badge_action_open_stillwater
    else -> R.string.badge_open_action
})
@Composable internal fun recommendationText(badge: BadgeProgressModel): String = when {
    badge.badgeId in RedBadgeCatalog.byId -> badgeObjectiveProgressText(badge.badgeId, badge.progress, badge.target, badge.remaining)
    badge.highestCreatureLevel != null -> stringResource(R.string.badge_next_mastery_step, badge.highestCreatureLevel, (99 - badge.highestCreatureLevel).coerceAtLeast(0))
    BadgeDefinitionResolver.resolve(badge.badgeId).requirement == BadgeRequirement.COLLECTOR -> stringResource(R.string.badge_next_discovery_step, badge.remaining)
    BadgeDefinitionResolver.resolve(badge.badgeId).requirement == BadgeRequirement.COMPLETIONIST -> stringResource(R.string.badge_next_completionist_step, badge.remaining)
    badge.category == BadgeUiCategory.FLOW -> stringResource(R.string.badge_next_flow_step, badge.remaining)
    else -> badgeObjectiveProgressText(badge.badgeId, badge.progress, badge.target, badge.remaining)
}
@Composable private fun SectionTitle(title: String, body: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (body.isNotBlank()) Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
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
    is BadgeActionDestination.ChestSpecies, is BadgeActionDestination.BlueRegion,
    is BadgeActionDestination.Green, is BadgeActionDestination.StillwaterVessel, is BadgeActionDestination.BeyondBlue -> navigate(badge.action)
    is BadgeActionDestination.CollectionDetails -> showCollection(badge.action.collectionId)
    BadgeActionDestination.MovementInfo -> navigate(badge.action)
    is BadgeActionDestination.BadgeDetails -> showDetails()
}

@Composable
internal fun badgeObjectiveProgressText(badgeId: String, progress: Int, target: Int, remaining: Int): String {
    val spec = RedBadgeCatalog.byId[badgeId]
    return when {
        badgeId in com.kingkharnivore.skillz.domain.green.GreenBadgeEvaluator.byId -> stringResource(R.string.green_badge_progress_units, progress, target, remaining)
        badgeId in setOf("power_pressure", "power_bedrock", "power_unyielding") ->
            stringResource(R.string.power_badge_time_progress, progress / 60, progress % 60, target / 60)
        spec?.species?.isNotEmpty() == true -> pluralStringResource(
            if (spec.mastery) R.plurals.red_badge_mastery_progress else R.plurals.red_badge_collection_progress,
            target, progress.coerceAtMost(target), target)
        badgeId in setOf("power_spark", "power_resolve") -> pluralStringResource(R.plurals.power_badge_session_progress, target, progress, target)
        badgeId == "power_strata" -> pluralStringResource(R.plurals.power_badge_journey_progress, target, progress, target)
        else -> stringResource(R.string.badge_progress_remaining, progress, target, remaining)
    }
}
