package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R

@Composable
fun SegmentedIconTab(
    selected: Boolean,
    onClick: () -> Unit,
    selectedBg: Color,
    selectedFg: Color,
    unselectedFg: Color,
    icon: ImageVector,
    contentDescription: String
) {
    val selectedLabel = stringResource(R.string.segmented_tab_selected)
    val notSelectedLabel = stringResource(R.string.segmented_tab_not_selected)
    val stateLabel = stringResource(
        R.string.segmented_tab_state,
        contentDescription,
        if (selected) selectedLabel else notSelectedLabel
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Tab
            this.selected = selected
            this.contentDescription = contentDescription
            stateDescription = stateLabel
        },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) selectedBg else Color.Transparent,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) selectedFg else unselectedFg
            )
        }
    }
}