package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

/** The shared, accessible page selector used by live Flow and Pulse Chronicles. */
@Composable
fun ChroniclePagerSelector(
    selectedPage: Int,
    primaryIcon: ImageVector,
    primaryLabel: String,
    primaryContentDescription: String,
    chronicleLabel: String,
    chronicleContentDescription: String,
    canLeaveChronicle: Boolean,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).widthIn(min = 280.dp, max = 360.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .08f),
        ) {
            Row(
                Modifier.padding(3.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ChroniclePagerSegment(
                    selected = selectedPage == 0,
                    enabled = selectedPage != 1 || canLeaveChronicle,
                    icon = primaryIcon,
                    label = primaryLabel,
                    description = primaryContentDescription,
                    onClick = { onPageSelected(0) },
                )
                ChroniclePagerSegment(
                    selected = selectedPage == 1,
                    enabled = true,
                    icon = Icons.Outlined.AutoStories,
                    label = chronicleLabel,
                    description = chronicleContentDescription,
                    onClick = { onPageSelected(1) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.ChroniclePagerSegment(
    selected: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        label = "chronicle pager container",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        label = "chronicle pager content",
    )
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            ),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Text(
                label,
                modifier = Modifier.padding(start = 7.dp),
                maxLines = 2,
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 0.6.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
