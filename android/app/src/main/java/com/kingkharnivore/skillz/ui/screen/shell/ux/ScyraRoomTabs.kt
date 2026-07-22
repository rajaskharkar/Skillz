package com.kingkharnivore.skillz.ui.screen.shell.ux

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared room tab treatment used by Idea Grove and achievement surfaces. */
@Composable
fun ScyraRoomTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    accessibilityLabel: @Composable (index: Int, title: String, selected: Boolean) -> String,
    evenlyDistributed: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(selectedIndex, evenlyDistributed, tabs.size) {
        if (!evenlyDistributed && tabs.isNotEmpty()) {
            lazyListState.animateScrollToItem(selectedIndex.coerceIn(tabs.indices))
        }
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        if (evenlyDistributed) {
            Row(
                Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    ScyraRoomTab(
                        title = title,
                        selected = selectedIndex == index,
                        accessibilityLabel = accessibilityLabel(index, title, selectedIndex == index),
                        onClick = { onSelected(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(tabs, key = { index, title -> "$index:$title" }) { index, title ->
                    ScyraRoomTab(
                        title = title,
                        selected = selectedIndex == index,
                        accessibilityLabel = accessibilityLabel(index, title, selectedIndex == index),
                        onClick = { onSelected(index) },
                        modifier = Modifier.widthIn(min = 112.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScyraRoomTab(
    title: String,
    selected: Boolean,
    accessibilityLabel: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) scheme.surface else scheme.primary.copy(alpha = 0f),
        label = "scyra_room_tab_background"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        label = "scyra_room_tab_text"
    )
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick).semantics {
            contentDescription = accessibilityLabel
            role = Role.Tab
            this.selected = selected
        }
    ) {
        Box(
            Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
