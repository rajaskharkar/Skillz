package com.kingkharnivore.skillz.ui.screen.story.chronicle

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PulseCard(
    pulse: PulseListItemUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLongPress: () -> Unit,
    onDeletePulse: () -> Unit,
    nested: Boolean = false,
    parentContextTitle: String? = null,
    parentContextTagName: String = "",
    parentContextIsSoftMode: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val containerColor = if (nested) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val contentColor = if (nested) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    val metaBg = if (nested) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(if (nested) 16.dp else 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = metaBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PsychologyAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor
                            )

                            if (pulse.tagName.isNotBlank()) {
                                Text(
                                    text = pulse.tagName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor
                                )
                            }
                        }
                    }

                    if (!nested && parentContextTitle != null) {
                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (parentContextIsSoftMode) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                            } else {
                                metaBg
                            }
                        ) {
                            Text(
                                text = buildString {
                                    append("Captured during: ")
                                    if (parentContextTagName.isNotBlank()) {
                                        append(parentContextTagName)
                                        append(" • ")
                                    }
                                    append(parentContextTitle)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = pulse.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }

                if (isExpanded) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = contentColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.14f))
            Spacer(Modifier.height(10.dp))

            Text(
                text = pulse.description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formatPulseTime(pulse.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete entry?") },
            text = { Text("This will permanently delete this entry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePulse()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatPulseTime(timeMs: Long): String {
    return SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(timeMs))
}