package com.kingkharnivore.skillz.ui.screen.story.chronicle

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun FlowCard(
    session: FlowListItemUiModel,
    isExpanded: Boolean,
    showScoreUi: Boolean,
    calmMode: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteSession: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val showSurgeStat = session.isSurge && session.surgePoints > 0
    val isBeamed = session.beamBonusPoints > 0

    val baseContainer = if (session.isSurge) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                onToggleExpand()
                onClick()
            },
            onLongClick = onLongPress
        )

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.tagName,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!calmMode && isBeamed) {
                        BeamBonusChip(bonusPoints = session.beamBonusPoints)
                        Spacer(Modifier.height(6.dp))
                    }

                    if (!calmMode && showSurgeStat) {
                        val dark = isSystemInDarkTheme()

                        val surgeTint = lerp(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.secondary,
                            if (dark) 0.75f else 0.45f
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚡ +${session.surgePoints}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = surgeTint
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                    }

                    if (isExpanded) {
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete session"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            Spacer(modifier = Modifier.height(10.dp))

            if (session.description.isNotBlank()) {
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Duration: ${formatDuration(session.durationMs)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (showScoreUi) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scyra Score: ${session.score}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (calmMode) 0.70f else 0.85f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete flow?")
            },
            text = {
                Text("This will permanently delete this flow.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSession()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}