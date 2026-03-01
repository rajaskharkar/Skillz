package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Surge (Option A: two-row):
 * - Wrap-content pill so it can truly sit on the right.
 * - Discreet when OFF.
 * - Toggle disabled once Flow is entered OR locked.
 * - Countdown while Flow active.
 * - Long-press opens dialog.
 */
@Composable
fun SurgeMiniControl(
    modifier: Modifier = Modifier,
    isInFlow: Boolean,
    elapsedMs: Long,
    locked: Boolean,
    isSurgeOn: Boolean,
    plannedMs: Long?,
    minutesInline: String,
    onMinutesChange: (String) -> Unit,
    onCommit: () -> Unit,
    onToggleOff: () -> Unit,
    onLongPress: () -> Unit,
    calmMode: Boolean = false
) {
    var pendingOn by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isSurgeOn) {
        if (isSurgeOn) pendingOn = false
        if (!isSurgeOn && !pendingOn) isEditing = false
    }

    val effectiveOn = isSurgeOn || pendingOn
    val toggleEnabled = !isInFlow && !locked

    val planned = plannedMs
    val remainingMs = if (planned != null) (planned - elapsedMs).coerceAtLeast(0L) else null
    val completed = isInFlow && planned != null && remainingMs == 0L

    LaunchedEffect(effectiveOn, plannedMs, toggleEnabled) {
        if (effectiveOn && toggleEnabled && plannedMs == null) isEditing = true
    }

    // ✅ Auto-revert if user toggled Surge ON but never set minutes
    LaunchedEffect(isEditing, plannedMs, pendingOn) {
        if (!isEditing && pendingOn && plannedMs == null) {
            pendingOn = false
            onToggleOff()
        }
    }

    val isOff = !effectiveOn

    val outerShape = RoundedCornerShape(if (isOff) 999.dp else 18.dp)
    val contentHPad = if (isOff) 10.dp else 12.dp
    val contentVPad = if (isOff) 6.dp else 10.dp
    val rowSpacing = if (isOff) 6.dp else 10.dp

    // Calm Mode: neutral stroke/container
    val stroke = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        if (isOff) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    val containerColor = if (calmMode) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (effectiveOn) 0.22f else 0.14f)
    } else {
        when {
            completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            effectiveOn -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        }
    }

    // Calm Mode: less “surge” tint on switch when off
    val offTrack = if (calmMode)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    else
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)

    val offThumb = MaterialTheme.colorScheme.onSurface.copy(alpha = if (calmMode) 0.35f else 0.45f)

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .combinedClickable(
                onClick = {
                    if (effectiveOn && toggleEnabled && !isInFlow) isEditing = true
                },
                onLongClick = { onLongPress() }
            )
            .border(1.dp, stroke, outerShape),
        shape = outerShape,
        tonalElevation = 1.dp,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = contentHPad, vertical = contentVPad),
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Calm Mode: optional to remove emoji (keeps it calmer)
                if (!calmMode) {
                    Text("⚡")
                    Spacer(Modifier.width(6.dp))
                }

                Text(
                    text = "Surge",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isOff) FontWeight.Medium else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isOff) 0.70f else 1f)
                )

                Spacer(Modifier.width(10.dp))

                val statusText = when {
                    completed -> "Complete"
                    planned != null && elapsedMs > 0L && remainingMs != null -> formatMsAsMmSs(remainingMs)
                    planned != null -> "${planned / 60_000L} min"
                    else -> ""
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Switch(
                    checked = effectiveOn,
                    onCheckedChange = { checked ->
                        if (!toggleEnabled) return@Switch

                        if (checked) {
                            pendingOn = true
                            if (plannedMs == null) isEditing = true
                        } else {
                            pendingOn = false
                            isEditing = false
                            onToggleOff()
                        }
                    },
                    enabled = toggleEnabled,
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = offTrack,
                        uncheckedThumbColor = offThumb
                    )
                )
            }

            AnimatedVisibility(visible = effectiveOn && toggleEnabled && isEditing && !isInFlow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minutesInline,
                        onValueChange = { raw ->
                            onMinutesChange(raw.filter(Char::isDigit).take(3))
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "mins",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    val minsValid = minutesInline.toIntOrNull()?.let { it > 0 } == true

                    val setColor = when {
                        calmMode -> MaterialTheme.colorScheme.surfaceVariant
                        minsValid -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Surface(
                        onClick = {
                            if (minsValid) {
                                onCommit()
                                isEditing = false
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        tonalElevation = if (minsValid && !calmMode) 2.dp else 0.dp,
                        color = setColor
                    ) {
                        Text(
                            text = "Set",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                minsValid && !calmMode -> MaterialTheme.colorScheme.primary
                                minsValid && calmMode -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            }
                        )
                    }
                }
            }
        }
    }
}