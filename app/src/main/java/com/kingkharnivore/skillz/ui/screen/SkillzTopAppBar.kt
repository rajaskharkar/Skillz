@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.theme.CaveatSemiBold

data class SkillzHomeNavState(
    val currentPage: Int,
    val onSelectPage: (Int) -> Unit
)

val LocalSkillzHomeNav = compositionLocalOf<SkillzHomeNavState?> { null }

@Composable
fun SkillzTopAppBar() {
    val title = when (BuildConfig.FLAVOR) {
        "aera" -> stringResource(R.string.home_app_name_aera)
        "scyra" -> stringResource(R.string.home_app_name_scyra)
        else -> stringResource(R.string.home_app_name_skillz)
    }
    val nav = LocalSkillzHomeNav.current

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = CaveatSemiBold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        actions = {
            if (nav != null) {
                HomeNavIcons(
                    selected = nav.currentPage,
                    onSelect = nav.onSelectPage
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun HomeNavIcons(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val atlasLabel = stringResource(R.string.home_nav_atlas)
    val storyLabel = stringResource(R.string.home_nav_story)
    val pathsLabel = stringResource(R.string.home_nav_paths)
    val notepadLabel = stringResource(R.string.home_nav_notepad)
    val helpLabel = stringResource(R.string.home_nav_help)
    val navBarLabel = stringResource(R.string.home_nav_bar)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(end = 6.dp)
            .semantics {
                contentDescription = navBarLabel
            }
    ) {
        NavIcon(
            selected = selected == 0,
            onClick = { onSelect(0) },
            contentDescription = atlasLabel,
            icon = {
                Icon(
                    Icons.Outlined.Map,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        NavIcon(
            selected = selected == 1,
            onClick = { onSelect(1) },
            contentDescription = storyLabel,
            icon = {
                Icon(
                    Icons.Outlined.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        NavIcon(
            selected = selected == 2,
            onClick = { onSelect(2) },
            contentDescription = pathsLabel,
            icon = {
                Icon(
                    Icons.Outlined.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        NavIcon(
            selected = selected == 3,
            onClick = { onSelect(3) },
            contentDescription = notepadLabel,
            icon = {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        NavIcon(
            selected = selected == 4,
            onClick = { onSelect(4) },
            contentDescription = helpLabel,
            icon = {
                Icon(
                    Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        )
    }
}

@Composable
private fun NavIcon(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    val selectedLabel = stringResource(R.string.home_nav_selected)
    val notSelectedLabel = stringResource(R.string.home_nav_not_selected)
    val stateLabel = stringResource(
        R.string.home_nav_tab_state,
        contentDescription,
        if (selected) selectedLabel else notSelectedLabel
    )

    val capsule = if (selected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    val alpha = if (selected) 1f else 0.72f
    val contentColor = LocalContentColor.current.copy(alpha = alpha)

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(capsule)
            .padding(2.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
                this.contentDescription = contentDescription
                stateDescription = stateLabel
            }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            icon()
        }
    }
}