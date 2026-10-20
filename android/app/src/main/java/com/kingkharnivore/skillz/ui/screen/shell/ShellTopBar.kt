package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellTopBar(
    destination: ShellDestination,
    pearlBalance: Int,
    pearlBasinHasIndicator: Boolean,
    notificationCount: Int,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
    blueRealm: com.kingkharnivore.skillz.utils.shell.CreatureRealm? = null,
    pebbleBalance: Int = 0,
    dropBalance: Long = 0
) {
    val scheme = MaterialTheme.colorScheme

    val title = when (destination) {
        ShellDestination.TheRed -> stringResource(R.string.red_title)
        ShellDestination.Heart -> stringResource(R.string.shell_title)
        ShellDestination.Focus -> stringResource(R.string.shell_room_focus_title)
        ShellDestination.TheGreen -> stringResource(R.string.shell_room_stillwater_title)
        ShellDestination.ShellChest -> stringResource(R.string.shell_chest_title)
        ShellDestination.Badges -> stringResource(R.string.shell_badges_title)
        ShellDestination.VoyagePreview -> stringResource(R.string.shell_room_voyage_title)
        ShellDestination.TheBluePreview -> blueRealm?.let { stringResource(if (it == com.kingkharnivore.skillz.utils.shell.CreatureRealm.SEA) R.string.land_realm_sea else R.string.land_realm_land) } ?: stringResource(R.string.shell_room_the_blue_title)
        ShellDestination.IdeaGrovePreview -> stringResource(R.string.shell_room_idea_title)
        ShellDestination.LookoutPreview -> stringResource(R.string.shell_room_lookout_title)
    }

    Column(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal))) {
        TopAppBar(
            windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = scheme.surface,
                titleContentColor = scheme.onSurface,
                navigationIconContentColor = scheme.primary,
                actionIconContentColor = scheme.primary
            ),
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            },
            actions = {
                TextButton(onClick = onNotifications, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.shell_notifications_a11y, notificationCount)
                    )
                    if (notificationCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Badge(
                            containerColor = scheme.secondary,
                            contentColor = scheme.onSecondary,
                            modifier = Modifier.clearAndSetSemantics {}
                        ) {
                            Text(notificationCount.coerceAtMost(9).toString(),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, if (pearlBasinHasIndicator) shellIndicatorColor() else scheme.outlineVariant.copy(alpha = .5f)),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            ShellBalances(pearlBalance, pebbleBalance, dropBalance, Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        }
    }
}
