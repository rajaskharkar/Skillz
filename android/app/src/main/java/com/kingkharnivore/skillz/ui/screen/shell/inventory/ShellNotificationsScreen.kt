package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.repository.shell.SHELL_BADGES_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.SHELL_CHEST_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.ShellNotificationType
import com.kingkharnivore.skillz.data.repository.shell.notificationId
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import com.kingkharnivore.skillz.domain.achievement.BadgeVisibilityContext
import com.kingkharnivore.skillz.domain.achievement.BadgeVisibilityEvaluator
import com.kingkharnivore.skillz.ui.screen.shell.ux.isActiveChestCreature
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.domain.lookout.ObjectiveBadgePresentationMetadata
import com.kingkharnivore.skillz.domain.lookout.objectiveBadgePresentationMetadata
import com.kingkharnivore.skillz.ui.screen.shell.NavigationConsumptionResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

sealed interface ShellNotificationInlayItem {
    val id: String
    val createdAt: Long
    val deepLinkRoute: String?

    data class Find(
        override val id: String,
        val findId: String,
        val instanceId: String,
        override val createdAt: Long,
        override val deepLinkRoute: String? = SHELL_CHEST_ROUTE
    ) : ShellNotificationInlayItem

    data class Badge(
        override val id: String,
        val badgeId: String,
        val count: Int,
        override val createdAt: Long,
        override val deepLinkRoute: String? = null,
        val destination: BadgeActionDestination = BadgeActionDestination.BadgeDetails(badgeId)
    ) : ShellNotificationInlayItem
}

fun unviewedShellNotifications(uiState: ShellUiState): List<ShellNotificationInlayItem> = buildList {
    val visibilityContext = BadgeVisibilityContext(
        // Legacy user_discovery IDs are not authoritative creature species IDs.
        discoveredSpeciesIds = emptySet(),
        earnedBadgeIds = uiState.badges.mapTo(mutableSetOf()) { it.badgeId },
        historicallyMasteredSpeciesIds = emptySet()
    )
    uiState.finds
        .filter { it.viewedAt == null && isActiveChestCreature(it) }
        .forEach { find ->
            add(
                ShellNotificationInlayItem.Find(
                    id = notificationId(ShellNotificationType.FIND, find.instanceId),
                    findId = find.findId,
                    instanceId = find.instanceId,
                    createdAt = find.acquiredAt
                )
            )
        }

    uiState.badges
        .filter { badge -> badge.viewedAt == null && BadgeVisibilityEvaluator.isVisible(
            BadgeDefinitionResolver.resolve(badge.badgeId), visibilityContext
        ) }
        .forEach { badge ->
            add(
                ShellNotificationInlayItem.Badge(
                    id = notificationId(ShellNotificationType.BADGE, badge.badgeId),
                    badgeId = badge.badgeId,
                    count = badge.count,
                    createdAt = badge.lastEarnedAt,
                    destination = BadgeActionDestination.BadgeDetails(badge.badgeId)
                )
            )
        }
}.sortedByDescending { it.createdAt }

@Composable
fun NotificationInlayOverlay(
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onMarkNotificationViewed: (String) -> Unit,
    onMarkAllViewed: () -> Unit,
    onFindDestination: (ShellNotificationInlayItem.Find) -> NavigationConsumptionResult,
    onBadgeDestination: (ShellNotificationInlayItem.Badge) -> NavigationConsumptionResult,
    modifier: Modifier = Modifier
) {
    val notifications = unviewedShellNotifications(uiState)
    val objectiveMetadata = remember(uiState.objectiveCompletions) {
        objectiveBadgePresentationMetadata(uiState.objectiveCompletions)
    }
    val backgroundInteraction = remember { MutableInteractionSource() }
    val inlayDescription = stringResource(R.string.notifications_inlay_a11y)

    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = backgroundInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .then(if (maxWidth < 600.dp) Modifier.fillMaxWidth() else Modifier.width(380.dp))
                .heightIn(max = 420.dp)
                .align(Alignment.TopEnd)
                .semantics { contentDescription = inlayDescription }
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 16.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shell_notifications_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.shell_notifications_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = onMarkAllViewed) {
                            Text(stringResource(R.string.shell_notifications_mark_viewed))
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    NotificationEmptyState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationInlayRow(
                                notification = notification,
                                objectiveMetadata = (notification as? ShellNotificationInlayItem.Badge)
                                    ?.let { objectiveMetadata[it.badgeId] },
                                onMarkViewed = { onMarkNotificationViewed(notification.id) },
                                onClick = {
                                    when (notification) {
                                        is ShellNotificationInlayItem.Badge -> {
                                            onBadgeDestination(notification)
                                        }
                                        is ShellNotificationInlayItem.Find -> onFindDestination(notification)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationEmptyState() {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        headlineContent = { Text(stringResource(R.string.shell_notifications_inlay_empty_title)) },
        supportingContent = { Text(stringResource(R.string.shell_notifications_inlay_empty_body)) },
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
    )
}

@Composable
private fun NotificationInlayRow(
    notification: ShellNotificationInlayItem,
    objectiveMetadata: ObjectiveBadgePresentationMetadata?,
    onMarkViewed: () -> Unit,
    onClick: () -> Unit
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val title: String
    val body: String
    val icon: ImageVector

    val deliveredAt = formatNotificationDeliveredAt(notification.createdAt)

    when (notification) {
        is ShellNotificationInlayItem.Find -> {
            val def = ShellContentCatalog.find(notification.findId)
            title = def?.let { notificationTitleFor(it) } ?: stringResource(R.string.shell_notification_fallback_title)
            body = def?.let { notificationBodyFor(it) } ?: stringResource(R.string.shell_notification_fallback_body)
            icon = def?.let { iconFor(it.category) } ?: Icons.Outlined.Notifications
        }
        is ShellNotificationInlayItem.Badge -> {
            val presentation = resolveBadgePresentation(notification.badgeId, objectiveMetadata)
            val badgeTitle = presentation.title
            title = stringResource(R.string.shell_badge_notification_title, badgeTitle)
            body = stringResource(
                R.string.shell_badge_notification_body,
                notification.count,
                presentation.description
            )
            icon = when (presentation.artworkKind) {
                BadgeArtworkKind.SPECIES_MASTERY -> Icons.Outlined.Pets
                BadgeArtworkKind.COLLECTOR -> Icons.Outlined.FilterVintage
                BadgeArtworkKind.CURATOR -> Icons.Outlined.CheckCircle
                BadgeArtworkKind.COMPLETIONIST -> Icons.Outlined.EmojiEvents
                BadgeArtworkKind.OBJECTIVE -> Icons.Outlined.EmojiEvents
                else -> Icons.Outlined.MilitaryTech
            }
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = title }
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
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(body)
                    Text(
                        text = deliveredAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingContent = {
                IconButton(onClick = onMarkViewed) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.shell_notifications_mark_notification_viewed_a11y)
                    )
                }
            }
        )
    }
}

internal fun formatNotificationDeliveredAt(
    createdAt: Long,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val deliveredAt = Instant.ofEpochMilli(createdAt).atZone(zoneId)
    val nowAtZone = Instant.ofEpochMilli(now).atZone(zoneId)
    return when {
        deliveredAt.toLocalDate() == nowAtZone.toLocalDate() -> deliveredAt.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        )
        deliveredAt.year == nowAtZone.year -> deliveredAt.format(
            DateTimeFormatter.ofPattern("MMM d", locale)
        )
        else -> deliveredAt.format(
            DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
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
