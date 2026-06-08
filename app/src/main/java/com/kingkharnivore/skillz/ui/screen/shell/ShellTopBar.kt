package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellTopBar(
    destination: ShellDestination,
    pearlBalance: Int,
    pearlBasinHasIndicator: Boolean,
    notificationCount: Int,
    onBack: () -> Unit,
    onPearls: () -> Unit,
    onNotifications: () -> Unit,
    onChest: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val title = when (destination) {
        ShellDestination.Heart -> stringResource(R.string.shell_title)
        ShellDestination.Focus -> stringResource(R.string.shell_room_focus_title)
        ShellDestination.Stillwater -> stringResource(R.string.shell_room_stillwater_title)
        ShellDestination.ShellChest -> stringResource(R.string.shell_chest_title)
        ShellDestination.Badges -> stringResource(R.string.shell_badges_title)
        ShellDestination.Notifications -> stringResource(R.string.shell_notifications_title)
        ShellDestination.VoyagePreview -> stringResource(R.string.shell_room_voyage_title)
        ShellDestination.TheBluePreview -> stringResource(R.string.shell_room_the_blue_title)
        ShellDestination.IdeaGrovePreview -> stringResource(R.string.shell_room_idea_title)
        ShellDestination.LookoutPreview -> stringResource(R.string.shell_room_lookout_title)
    }

    val pearlBalanceDescription = stringResource(R.string.shell_pearl_basin_chip_a11y, pearlBalance)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            navigationIconContentColor = scheme.primary,
            actionIconContentColor = scheme.primary
        ),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.shell_back_a11y)
                )
            }
        },
        actions = {
            AssistChip(
                onClick = onPearls,
                label = {
                    Text(stringResource(R.string.shell_pearl_balance, pearlBalance))
                },
                leadingIcon = {
                    ShellPearlMiniIcon(Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = scheme.surface,
                    labelColor = scheme.onSurface,
                    leadingIconContentColor = scheme.primary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (pearlBasinHasIndicator) shellIndicatorColor() else scheme.secondary.copy(alpha = 0.45f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                    role = Role.Button
                }
            )

            IconButton(onClick = onNotifications) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.shell_notifications_a11y, notificationCount)
                    )
                    if (notificationCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.secondary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = notificationCount.coerceAtMost(9).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onChest) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = stringResource(R.string.shell_chest_a11y)
                )
            }
        }
    )
}