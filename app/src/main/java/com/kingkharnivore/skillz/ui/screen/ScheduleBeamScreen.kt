package com.kingkharnivore.skillz.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.viewmodel.ScheduleBeamViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBeamScreen(
    vm: ScheduleBeamViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val tags by vm.tags.collectAsState()

    val haptics = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWhenSheet by remember { mutableStateOf(false) }
    val whenSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomMinutes by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = ui.selectedHour ?: 12,
        initialMinute = ui.selectedMinute ?: 0,
        is24Hour = false
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.onTimePicked(timePickerState.hour, timePickerState.minute)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = ui.selectedDateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        vm.onDatePicked(it)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    showDatePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showCustomMinutes) {
        AlertDialog(
            onDismissRequest = { showCustomMinutes = false },
            title = { Text("Custom duration") },
            text = {
                TextField(
                    value = ui.customMinutesText,
                    onValueChange = vm::onCustomMinutesChange,
                    singleLine = true,
                    label = { Text("Minutes") },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.applyCustomMinutes()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCustomMinutes = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showCustomMinutes = false }) { Text("Cancel") } }
        )
    }

    if (showWhenSheet) {
        ModalBottomSheet(
            sheetState = whenSheetState,
            onDismissRequest = { showWhenSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("When", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = ui.datePreset == "today",
                        onClick = {
                            vm.pickToday()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = ui.datePreset == "tomorrow",
                        onClick = {
                            vm.pickTomorrow()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text("Tomorrow") }
                    )

                    Spacer(Modifier.weight(1f))

                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text("Pick date") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                    )
                }

                TacticalRow(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    title = "Time",
                    value = formatTime(ui.selectedHour, ui.selectedMinute),
                    onClick = {
                        showTimePicker = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )

                Button(
                    onClick = {
                        showWhenSheet = false
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Set") }
            }
        }
    }

    // --------- SCREEN ----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Beam ⭐") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BeamHeroCard()

            SectionLabel("When")
            TacticalSelectField(
                leading = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                text = formatWhenLabel(ui.selectedDateEpochMs, ui.selectedHour, ui.selectedMinute),
                onClick = {
                    showWhenSheet = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Journey")
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    if (tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(tags, key = { it.id }) { tag ->
                                val selected = ui.tagName.equals(tag.name, ignoreCase = true)

                                AssistChip(
                                    onClick = {
                                        vm.onPickTag(tag.name)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    label = { Text(tag.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,

                                        labelColor = if (selected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,

                                        leadingIconContentColor = if (selected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,

                                        trailingIconContentColor = if (selected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = null
                                )
                            }
                        }

                        Text(
                            text = "Tap a journey to select, or type to create one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    TextField(
                        value = ui.tagName,
                        onValueChange = vm::onTagNameChange,
                        placeholder = { Text("Type a journey name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // DURATION (chips + custom row)
            DurationBlock(
                durationMinutes = ui.durationMinutes,
                onPickMinutes = {
                    vm.setDurationMinutes(it)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onCustomClick = {
                    showCustomMinutes = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )

            if (ui.error != null) {
                Text(
                    text = ui.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.schedule(onDone) },
                enabled = !ui.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (ui.isSaving) "Locking..." else "Lock in your Beam")
            }
        }
    }
}

@Composable
private fun BeamHeroCard() {
    val shape = RoundedCornerShape(26.dp)

    val c1 = MaterialTheme.colorScheme.secondary
    val c2 = MaterialTheme.colorScheme.tertiary
    val on = MaterialTheme.colorScheme.onSecondary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        color = c1
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c1)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,                 // center stays clean
                            c2.copy(alpha = 0.35f),             // mid-edge tint
                            c1.copy(alpha = 0.55f)              // corners darker
                        ),
                        radius = 900f                          // large radius = edge-focused
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .fillMaxWidth()
                    .background(on.copy(alpha = 0.22f))
            )

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = on.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Lock in your Beam.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = on
                )
                Text(
                    text = "Show up during this window to earn bonus points.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = on.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    )
}

@Composable
private fun TacticalSelectField(
    leading: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = borderColor, shape = shape),
        shape = shape,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun TacticalRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    TacticalSelectField(
        leading = icon,
        text = "$title • $value",
        onClick = onClick
    )
}

@Composable
private fun DurationBlock(
    durationMinutes: Int?,
    onPickMinutes: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val options = listOf(5, 15, 20, 30, 45, 60)
    val selectedContainer = MaterialTheme.colorScheme.primary
    val selectedLabel = MaterialTheme.colorScheme.onPrimary
    val unselectedContainer = MaterialTheme.colorScheme.surfaceVariant
    val unselectedLabel = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Duration")

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(options, key = { it }) { m ->
                    val selected = durationMinutes == m

                    FilterChip(
                        selected = selected,
                        onClick = { onPickMinutes(m) },
                        label = { Text("${m}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = unselectedContainer,
                            labelColor = unselectedLabel,
                            iconColor = unselectedLabel,
                            selectedContainerColor = selectedContainer,
                            selectedLabelColor = selectedLabel,
                            selectedLeadingIconColor = selectedLabel,
                            selectedTrailingIconColor = selectedLabel
                        ),
                        border = null
                    )
                }
            }

            val customLineText = durationMinutes?.let { "${it}m set" } ?: "Set"

            TacticalSelectField(
                leading = { Icon(Icons.Default.Schedule, contentDescription = null) },
                text = "Custom • $customLineText",
                onClick = onCustomClick
            )

            if (durationMinutes == null) {
                Text(
                    text = "Pick a duration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

private fun formatWhenLabel(epochMs: Long?, hour: Int?, minute: Int?): String {
    if (epochMs == null || hour == null || minute == null) return "Pick a time"
    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()
    val time = LocalTime.of(hour, minute)
    val fmt = DateTimeFormatter.ofPattern("MMM d • h:mm a")
    return ZonedDateTime.of(date, time, ZoneId.systemDefault()).format(fmt)
}

private fun formatTime(hour: Int?, minute: Int?): String {
    if (hour == null || minute == null) return "--:--"
    val t = LocalTime.of(hour, minute)
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    return t.format(fmt)
}
