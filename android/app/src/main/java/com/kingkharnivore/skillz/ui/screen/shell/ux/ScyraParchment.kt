package com.kingkharnivore.skillz.ui.screen.shell.ux

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/** Warm parchment panel in light mode and the theme's warm dark parchment in dark mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScyraParchmentSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        content = content
    )
}
