package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet

@Composable internal fun BadgeBookCollectionCard(collection: BadgeBookCollection, badges: List<BadgeProgressModel>, onOpen: () -> Unit) {
    val byId = badges.associateBy { it.badgeId }
    val earned = BadgeBookCollections.earnedMembers(collection, byId)
    ElevatedCard(Modifier.fillMaxWidth().testTag(collection.id).clickable(onClick = onOpen), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                byId[collection.completionBadgeId]?.let { BadgeMedallion(it, BadgeMedallionSize.Small, onOpen) }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(collection.titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(when {
                        collection.difficulty <= 20 -> R.string.book_difficulty_start
                        collection.difficulty <= 55 -> R.string.book_difficulty_build
                        collection.difficulty <= 70 -> R.string.book_difficulty_long
                        else -> R.string.book_difficulty_ultimate
                    }), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(pluralStringResource(R.plurals.book_member_progress, collection.memberIds.size, earned, collection.memberIds.size), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { earned.toFloat() / collection.memberIds.size }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.book_completion_reward, resolveBadgePresentation(collection.completionBadgeId).title),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun BadgeBookCollectionSheet(collection: BadgeBookCollection, badges: List<BadgeProgressModel>, dismiss: () -> Unit, openBadge: (BadgeProgressModel) -> Unit) {
    val byId = badges.associateBy { it.badgeId }
    ScyraParchmentSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().testTag("book-collection-members"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(collection.titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.book_collection_rules), modifier = Modifier.padding(top = 8.dp))
                Text(pluralStringResource(R.plurals.book_member_progress, collection.memberIds.size, BadgeBookCollections.earnedMembers(collection, byId), collection.memberIds.size), modifier = Modifier.padding(top = 8.dp))
            }
            item {
                byId[collection.completionBadgeId]?.let { award ->
                    OutlinedButton(onClick = { openBadge(award) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.book_completion_reward, resolveBadgePresentation(award.badgeId).title))
                    }
                }
            }
            items(collection.memberIds, key = { it }) { id ->
                val badge = byId[id]
                OutlinedCard(Modifier.fillMaxWidth().testTag("book-member-$id").then(if (badge != null) Modifier.clickable { openBadge(badge) } else Modifier)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        badge?.let { BadgeMedallion(it, BadgeMedallionSize.Small) { openBadge(it) } }
                        Column(Modifier.weight(1f)) {
                            Text(resolveBadgePresentation(id).title, fontWeight = FontWeight.SemiBold)
                            Text(resolveBadgePresentation(id).description, style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(if (badge?.everEarned == true) R.string.badge_earned else R.string.badge_locked), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
