package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

private data class ObjectiveBadgeGroup(
    val journeyId: Long,
    val journeyName: String,
    val badges: List<ObjectiveBadgeRow>
)

private data class ObjectiveBadgeRow(val period: String, val count: Int)

@Composable
fun BadgesScreen(uiState: ShellUiState) {
    val objectiveGroups = uiState.badges.objectiveBadgeGroups(uiState.objectiveCompletions)
    val catalogBadges = uiState.badges.filterNot { it.badgeId.startsWith("objective_badge_") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_badges_title,
                body = R.string.shell_badges_body
            )
        }

        if (objectiveGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Objective Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(objectiveGroups, key = { it.journeyId }) { group ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.journeyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        group.badges.forEach { row ->
                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                headlineContent = { Text("${row.period} Objective x${row.count}") },
                                supportingContent = { Text("Completed ${row.period.lowercase()} Objectives for ${group.journeyName}.") }
                            )
                        }
                    }
                }
            }
        }

        items(catalogBadges) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            val badgeDescription = stringResource(R.string.shell_badge_a11y, title, badge.count)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = badgeDescription
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.shell_badge_row_title, title, badge.count)) },
                    supportingContent = {
                        Text(stringResource(def.descriptionRes))
                    }
                )
            }
        }
    }
}

private fun List<UserBadgeEntity>.objectiveBadgeGroups(completions: List<ObjectiveCompletionEntity>): List<ObjectiveBadgeGroup> {
    val journeyNameByBadge = completions
        .groupBy { it.badgeKey }
        .mapValues { (_, rows) -> rows.maxByOrNull { it.completedAt }?.journeyNameSnapshot.orEmpty() }
    return asSequence()
        .mapNotNull { badge ->
            val parts = badge.badgeId.removePrefix("objective_badge_").split("_")
            if (!badge.badgeId.startsWith("objective_badge_") || parts.size < 2) return@mapNotNull null
            val period = parts.last()
            val journeyId = parts.dropLast(1).joinToString("_").toLongOrNull() ?: return@mapNotNull null
            ObjectiveBadgeGroup(
                journeyId = journeyId,
                journeyName = journeyNameByBadge[badge.badgeId].takeUnless { it.isNullOrBlank() } ?: "Journey $journeyId",
                badges = listOf(ObjectiveBadgeRow(period.replaceFirstChar { it.titlecase() }, badge.count))
            )
        }
        .groupBy { it.journeyId to it.journeyName }
        .map { (key, groups) ->
            ObjectiveBadgeGroup(
                journeyId = key.first,
                journeyName = key.second,
                badges = groups.flatMap { it.badges }
                    .sortedBy { listOf("Daily", "Weekly", "Monthly").indexOf(it.period).let { index -> if (index < 0) Int.MAX_VALUE else index } }
            )
        }
        .sortedBy { it.journeyName.lowercase() }
}
