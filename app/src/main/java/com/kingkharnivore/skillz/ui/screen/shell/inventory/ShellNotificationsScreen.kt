package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.isActiveChestCreature
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

@Composable
fun ShellNotificationsScreen(uiState: ShellUiState) {
    val newFinds = uiState.finds.filter { it.isNew && isActiveChestCreature(it) }
    val newBadges = uiState.badges.filter { it.isNew }
    val hasNotifications = newFinds.isNotEmpty() || newBadges.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_notifications_title,
                body = R.string.shell_notifications_body
            )
        }

        if (!hasNotifications) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.shell_notifications_empty_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_notifications_empty_body)) }
                    )
                }
            }
        }

        items(newFinds, key = { it.instanceId }) { find ->
            val def = ShellContentCatalog.find(find.findId) ?: return@items
            ShellNotificationCard(
                icon = iconFor(def.category),
                title = notificationTitleFor(def),
                body = notificationBodyFor(def)
            )
        }


        items(newBadges, key = { it.badgeId }) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = Icons.Outlined.MilitaryTech,
                title = stringResource(R.string.shell_badge_notification_title, title),
                body = stringResource(R.string.shell_badge_notification_body, badge.count, stringResource(def.descriptionRes))
            )
        }
    }
}

@Composable
private fun ShellNotificationCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.semantics {
            contentDescription = title
        }
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            headlineContent = { Text(title) },
            supportingContent = { Text(body) }
        )
    }
}

@Composable
private fun notificationTitleFor(def: ShellFindDefinition): String {
    val title = stringResource(def.titleRes)
    return stringResource(R.string.shell_notification_title_encountered, title)
}

@Composable
private fun notificationBodyFor(def: ShellFindDefinition): String {
    val depth = depthLabel(def.depthTier)
    return depth?.let { stringResource(R.string.shell_notification_depth_body, it, stringResource(def.descriptionRes)) }
        ?: stringResource(def.descriptionRes)
}

@Composable
private fun depthLabel(depth: ShellDepthTier?): String? = when (depth) {
    ShellDepthTier.REEF -> stringResource(R.string.shell_depth_reef)
    ShellDepthTier.DEEPER_REEF -> stringResource(R.string.shell_depth_deeper_reef)
    ShellDepthTier.OPEN_BLUE -> stringResource(R.string.shell_depth_open_blue)
    ShellDepthTier.DEEP_OCEAN -> stringResource(R.string.shell_depth_deep_ocean)
    null -> null
}

private fun iconFor(category: ShellFindCategory): ImageVector = when (category) {
    ShellFindCategory.CREATURES -> Icons.Outlined.Pets
    ShellFindCategory.SHELLS -> Icons.Outlined.Spa
    ShellFindCategory.CORAL -> Icons.Outlined.FilterVintage
    ShellFindCategory.PLANTS -> Icons.Outlined.Grass
    ShellFindCategory.TROPHIES -> Icons.Outlined.EmojiEvents
    ShellFindCategory.TRINKETS -> Icons.Outlined.EmojiEvents
    ShellFindCategory.DISCOVERIES -> Icons.Outlined.EmojiEvents
}