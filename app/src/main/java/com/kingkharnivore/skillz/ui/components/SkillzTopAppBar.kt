@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.ui.theme.CaveatSemiBold

data class SkillzHomeNavState(
    val currentPage: Int,
    val onSelectPage: (Int) -> Unit
)

val LocalSkillzHomeNav = compositionLocalOf<SkillzHomeNavState?> { null }

@Composable
fun SkillzTopAppBar() {
    val title = when (BuildConfig.FLAVOR) {
        "aera" -> "Aera"
        "scyra" -> "Scyra"
        else -> "Skillz"
    }

    val nav = LocalSkillzHomeNav.current

    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = CaveatSemiBold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        actions = {
            // ✅ Icon-only section switching (no extra vertical real-estate).
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        NavIcon(
            selected = selected == 0,
            onClick = { onSelect(0) },
            contentDescription = "Atlas",
            icon = { Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(22.dp)) }
        )
        NavIcon(
            selected = selected == 1,
            onClick = { onSelect(1) },
            contentDescription = "Story",
            icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(22.dp)) }
        )
        NavIcon(
            selected = selected == 2,
            onClick = { onSelect(2) },
            contentDescription = "Notepad",
            icon = { Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(22.dp)) }
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
    // Sexy + minimal: selected gets a soft capsule behind it.
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
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            icon()
        }
    }
}
