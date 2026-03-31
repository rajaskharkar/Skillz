package com.kingkharnivore.skillz.ui.screen.story.chronicle

import android.text.format.DateFormat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
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

    val context = LocalContext.current

    val deleteEntryLabel = stringResource(R.string.pulse_card_delete_entry)
    val editEntryLabel = stringResource(R.string.pulse_card_edit_entry)
    val expandLabel = stringResource(R.string.pulse_card_expand)
    val collapseLabel = stringResource(R.string.pulse_card_collapse)
    val openDetailsLabel = stringResource(R.string.pulse_card_open_details)
    val pulseTypeLabel = stringResource(R.string.pulse_card_type)
    val expandedStateLabel = stringResource(R.string.a11y_expanded)
    val collapsedStateLabel = stringResource(R.string.a11y_collapsed)

    val stateLabel = if (isExpanded) expandedStateLabel else collapsedStateLabel
    val toggleLabel = if (isExpanded) collapseLabel else expandLabel

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

    val parentContextText = if (!nested && parentContextTitle != null) {
        if (parentContextTagName.isNotBlank()) {
            stringResource(
                R.string.pulse_card_captured_during_with_tag,
                parentContextTagName,
                parentContextTitle
            )
        } else {
            stringResource(
                R.string.pulse_card_captured_during_without_tag,
                parentContextTitle
            )
        }
    } else {
        null
    }

    val createdAtText = formatPulseTime(
        context = context,
        timeMs = pulse.createdAt
    )

    val cardContentDescription = buildString {
        if (pulse.title.isNotBlank()) {
            append(pulse.title)
            append(", ")
        } else {
            append(pulseTypeLabel)
            append(", ")
        }

        if (pulse.tagName.isNotBlank()) {
            append(pulse.tagName)
            append(", ")
        }

        append(pulse.description)

        if (parentContextText != null) {
            append(", ")
            append(parentContextText)
        }

        append(", ")
        append(createdAtText)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = cardContentDescription
                stateDescription = stateLabel
                onLongClick(label = editEntryLabel) {
                    onLongPress()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(label = toggleLabel) {
                        onToggleExpand()
                        true
                    },
                    CustomAccessibilityAction(label = openDetailsLabel) {
                        onToggleExpand()
                        true
                    }
                )
            }
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

                    if (parentContextText != null) {
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
                                text = parentContextText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor
                            )
                        }
                    }

                    if (pulse.title.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = pulse.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                }

                if (isExpanded) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = deleteEntryLabel,
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
                text = createdAtText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.pulse_card_delete_dialog_title)) },
            text = { Text(stringResource(R.string.pulse_card_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePulse()
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private fun formatPulseTime(
    context: android.content.Context,
    timeMs: Long
): String {
    val skeleton = "MMMdhmma"
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
    return java.text.SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMs))
}