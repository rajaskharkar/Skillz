package com.kingkharnivore.skillz.ui.beams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.viewmodel.ScheduleBeamViewModel
import java.time.Instant
import java.time.ZoneId
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val now = remember { java.time.LocalTime.now() }
    val defaultHour = ui.selectedHour ?: now.hour
    val defaultMinute = ui.selectedMinute ?: now.minute

    val timePickerState = rememberTimePickerState(
        initialHour = defaultHour,
        initialMinute = defaultMinute,
        is24Hour = false
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.onTimePicked(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis
                        if (selected != null) vm.onDatePicked(selected)
                        showDatePicker = false
                    }
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Beam") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Text("←") }
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
            // HERO CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Lock in your Beam.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Show up during the scheduled window to earn Beam bonus points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }

            // DATE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(ui.selectedDateEpochMs?.let { "📅 ${formatDate(it)}" } ?: "📅 Pick date")
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (ui.selectedHour != null && ui.selectedMinute != null) {
                            "🕒 ${formatTime(ui.selectedHour, ui.selectedMinute)}"
                        } else {
                            "🕒 Pick time"
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Start: ${formatStartPreview(ui.selectedDateEpochMs, ui.selectedHour, ui.selectedMinute)}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }


            // DURATION (MMm SSs)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ui.minutesText,
                            onValueChange = vm::onMinutesChange,
                            label = { Text("MM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            supportingText = { Text("minutes") }
                        )
                        OutlinedTextField(
                            value = ui.secondsText,
                            onValueChange = vm::onSecondsChange,
                            label = { Text("SS") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            supportingText = { Text("seconds") }
                        )
                    }

                    Text(
                        text = "Format: 00m00s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            // TAG PICKER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Journey",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = ui.tagName,
                        onValueChange = vm::onTagNameChange,
                        label = { Text("Pick or create a tag") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(tags, key = { it.id }) { tag ->
                                AssistChip(
                                    onClick = { vm.onPickTag(tag.name) },
                                    label = { Text(tag.name) }
                                )
                            }
                        }
                    }
                }
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (ui.isSaving) "Scheduling..." else "Schedule Beam")
            }
        }
    }
}

private fun formatDate(epochMs: Long): String {
    val utcDate = Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d")
    return utcDate.format(fmt)
}

private fun formatTime(hour: Int?, minute: Int?): String {
    if (hour == null || minute == null) return "--:--"
    val t = java.time.LocalTime.of(hour, minute)
    val fmt = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    return t.format(fmt)
}

private fun formatStartPreview(
    selectedDateMillis: Long?,
    selectedHour: Int?,
    selectedMinute: Int?
): String {
    val zone = ZoneId.systemDefault()
    val now = java.time.ZonedDateTime.now(zone)

    val date = if (selectedDateMillis != null) {
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
    } else {
        now.toLocalDate()
    }

    val hour = selectedHour ?: now.hour
    val minute = selectedMinute ?: now.minute

    val start = java.time.ZonedDateTime.of(
        date,
        java.time.LocalTime.of(hour, minute, 0, 0),
        zone
    )

    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d • h:mm a")
    return start.format(fmt)
}

