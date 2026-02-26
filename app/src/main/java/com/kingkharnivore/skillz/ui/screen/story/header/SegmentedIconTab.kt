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
import androidx.compose.ui.unit.dp

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
    Surface(
        onClick = onClick,
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
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = if (selected) selectedFg else unselectedFg
            )
        }
    }
}