package com.kingkharnivore.skillz.ui.screen.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import com.kingkharnivore.skillz.domain.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.*
import kotlinx.coroutines.*
import kotlin.math.*


@Composable
internal fun ShellNotificationsScreen(uiState: ShellUiState) {
    val newFinds = uiState.finds.filter { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }
    val newStacks = uiState.stacks.filter { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }
    val newBadges = uiState.badges.filter { it.isNew }
    val newDiscoveries = uiState.discoveries.filter { it.isNew }
    val hasNotifications = newFinds.isNotEmpty() || newStacks.isNotEmpty() || newBadges.isNotEmpty() || newDiscoveries.isNotEmpty()

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

        items(newStacks, key = { it.findId }) { stack ->
            val def = ShellContentCatalog.find(stack.findId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = iconFor(def.category),
                title = stringResource(R.string.shell_chest_group_title, title, stack.quantity),
                body = stringResource(R.string.shell_notification_stack_body, stack.quantity)
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

        items(newDiscoveries, key = { it.userDiscoveryId }) { discovery ->
            val def = ShellContentCatalog.discovery(discovery.discoveryId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = Icons.Outlined.AutoStories,
                title = stringResource(R.string.shell_discovery_notification_title, title),
                body = stringResource(def.explanationRes)
            )
        }
    }
}

@Composable
internal fun ShellNotificationCard(
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