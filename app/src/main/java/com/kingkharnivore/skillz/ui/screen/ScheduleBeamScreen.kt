@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.viewmodel.ScheduleBeamViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBeamScreen(
    vm: ScheduleBeamViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val tags by vm.tags.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWhenSheet by remember { mutableStateOf(false) }
    val whenSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomMinutes by remember { mutableStateOf(false) }

    val pickTimeLabel = stringResource(R.string.schedule_beam_pick_time)
    val setLabel = stringResource(R.string.common_set)
    val cancelLabel = stringResource(R.string.common_cancel)
    val customDurationLabel = stringResource(R.string.schedule_beam_custom_duration)
    val minutesLabel = stringResource(R.string.common_minutes)
    val whenLabel = stringResource(R.string.schedule_beam_when)
    val todayLabel = stringResource(R.string.schedule_beam_today)
    val tomorrowLabel = stringResource(R.string.schedule_beam_tomorrow)
    val pickDateLabel = stringResource(R.string.schedule_beam_pick_date)
    val timeLabel = stringResource(R.string.schedule_beam_time)
    val screenTitle = stringResource(R.string.schedule_beam_screen_title)
    val backLabel = stringResource(R.string.common_back)
    val journeyLabel = stringResource(R.string.schedule_beam_journey)
    val editLabel = stringResource(R.string.common_edit)
    val journeyHint = stringResource(R.string.schedule_beam_journey_hint)
    val journeyPlaceholder = stringResource(R.string.schedule_beam_journey_placeholder)
    val durationLabel = stringResource(R.string.schedule_beam_duration)
    val customLabel = stringResource(R.string.schedule_beam_custom)
    val pickDurationLabel = stringResource(R.string.schedule_beam_pick_duration)
    val lockingLabel = stringResource(R.string.schedule_beam_locking)
    val lockInLabel = stringResource(R.string.schedule_beam_lock_in)
    val whenSheetPaneTitle = stringResource(R.string.schedule_beam_a11y_when_sheet)
    val openDatePickerLabel = stringResource(R.string.schedule_beam_a11y_open_date_picker)
    val openTimePickerLabel = stringResource(R.string.schedule_beam_a11y_open_time_picker)
    val journeySuggestionsLabel = stringResource(R.string.schedule_beam_a11y_journey_suggestions)
    val whenFieldA11y = stringResource(R.string.schedule_beam_a11y_when_field)
    val customDurationA11y = stringResource(R.string.schedule_beam_a11y_custom_duration)

    val timePickerState = rememberTimePickerState(
        initialHour = ui.selectedHour ?: 12,
        initialMinute = ui.selectedMinute ?: 0,
        is24Hour = false
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(pickTimeLabel) },
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
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTimePicker = false
                    }
                ) {
                    Text(setLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = ui.selectedDateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            vm.onDatePicked(it)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(setLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(cancelLabel)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCustomMinutes) {
        AlertDialog(
            onDismissRequest = { showCustomMinutes = false },
            title = { Text(customDurationLabel) },
            text = {
                TextField(
                    value = ui.customMinutesText,
                    onValueChange = vm::onCustomMinutesChange,
                    singleLine = true,
                    label = { Text(minutesLabel) },
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
                TextButton(
                    onClick = {
                        vm.applyCustomMinutes()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCustomMinutes = false
                    }
                ) {
                    Text(setLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutes = false }) {
                    Text(cancelLabel)
                }
            }
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
                    .semantics { paneTitle = whenSheetPaneTitle }
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = whenLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = ui.datePreset == "today",
                        onClick = {
                            vm.pickToday()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text(todayLabel) }
                    )
                    FilterChip(
                        selected = ui.datePreset == "tomorrow",
                        onClick = {
                            vm.pickTomorrow()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text(tomorrowLabel) }
                    )

                    Spacer(Modifier.weight(1f))

                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(pickDateLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = openDatePickerLabel
                        }
                    )
                }

                TacticalRow(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    title = timeLabel,
                    value = formatTime(context, ui.selectedHour, ui.selectedMinute),
                    onClick = {
                        showTimePicker = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    contentDescription = openTimePickerLabel
                )

                Button(
                    onClick = {
                        showWhenSheet = false
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(setLabel)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = backLabel
                        )
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

            SectionLabel(whenLabel)

            TacticalSelectField(
                leading = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                text = formatWhenLabel(context, ui.selectedDateEpochMs, ui.selectedHour, ui.selectedMinute),
                onClick = {
                    showWhenSheet = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                contentDescription = whenFieldA11y
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
                        SectionLabel(journeyLabel)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = editLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    if (tags.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.semantics {
                                contentDescription = journeySuggestionsLabel
                            },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(tags, key = { it.id }) { tag ->
                                val selected = ui.tagName.equals(tag.name, ignoreCase = true)
                                val selectJourneyLabel =
                                    context.getString(R.string.schedule_beam_a11y_select_journey, tag.name)

                                AssistChip(
                                    onClick = {
                                        vm.onPickTag(tag.name)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    label = { Text(tag.name) },
                                    modifier = Modifier.semantics {
                                        contentDescription = selectJourneyLabel
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        labelColor = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        leadingIconContentColor = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        trailingIconContentColor = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    border = null
                                )
                            }
                        }

                        Text(
                            text = journeyHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    TextField(
                        value = ui.tagName,
                        onValueChange = vm::onTagNameChange,
                        placeholder = { Text(journeyPlaceholder) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (ui.isSaving) lockingLabel else lockInLabel)
            }
        }
    }
}

@Composable
private fun BeamHeroCard() {
    val heroTitle = stringResource(R.string.schedule_beam_hero_title)
    val heroBody = stringResource(R.string.schedule_beam_hero_body)
    val heroA11y = stringResource(R.string.schedule_beam_hero_a11y)

    val shape = RoundedCornerShape(26.dp)
    val c1 = MaterialTheme.colorScheme.secondary
    val c2 = MaterialTheme.colorScheme.tertiary
    val on = MaterialTheme.colorScheme.onSecondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = heroA11y
            },
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
                            Color.Transparent,
                            c2.copy(alpha = 0.35f),
                            c1.copy(alpha = 0.55f)
                        ),
                        radius = 900f
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
                    text = heroTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = on
                )
                Text(
                    text = heroBody,
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun TacticalSelectField(
    leading: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    contentDescription: String
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                stateDescription = text
            },
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
    onClick: () -> Unit,
    contentDescription: String
) {
    val combinedText = stringResource(R.string.schedule_beam_tactical_value, title, value)

    TacticalSelectField(
        leading = icon,
        text = combinedText,
        onClick = onClick,
        contentDescription = contentDescription
    )
}

@Composable
private fun DurationBlock(
    durationMinutes: Int?,
    onPickMinutes: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val durationLabel = stringResource(R.string.schedule_beam_duration)
    val customLabel = stringResource(R.string.schedule_beam_custom)
    val pickDurationLabel = stringResource(R.string.schedule_beam_pick_duration)
    val customDurationA11y = stringResource(R.string.schedule_beam_a11y_custom_duration)

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
            SectionLabel(durationLabel)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(options, key = { it }) { m ->
                    val selected = durationMinutes == m
                    val durationOptionA11y =
                        stringResource(R.string.schedule_beam_a11y_duration_option, m)

                    FilterChip(
                        selected = selected,
                        onClick = { onPickMinutes(m) },
                        label = { Text("${m}m") },
                        modifier = Modifier.semantics {
                            contentDescription = durationOptionA11y
                        },
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

            val customLineText = durationMinutes?.let {
                stringResource(R.string.schedule_beam_duration_set, it)
            } ?: stringResource(R.string.common_set)

            val customCombinedText =
                stringResource(R.string.schedule_beam_custom_value, customLabel, customLineText)

            TacticalSelectField(
                leading = { Icon(Icons.Default.Schedule, contentDescription = null) },
                text = customCombinedText,
                onClick = onCustomClick,
                contentDescription = customDurationA11y
            )

            if (durationMinutes == null) {
                Text(
                    text = pickDurationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}

private fun formatWhenLabel(
    context: Context,
    epochMs: Long?,
    hour: Int?,
    minute: Int?
): String {
    if (epochMs == null || hour == null || minute == null) {
        return context.getString(R.string.schedule_beam_pick_a_time)
    }

    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()
    val time = LocalTime.of(hour, minute)
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdhmma")
    val fmt = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return ZonedDateTime.of(date, time, ZoneId.systemDefault()).format(fmt)
}

private fun formatTime(
    context: Context,
    hour: Int?,
    minute: Int?
): String {
    if (hour == null || minute == null) {
        return context.getString(R.string.schedule_beam_time_empty)
    }

    val time = LocalTime.of(hour, minute)
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hmma")
    val fmt = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return time.format(fmt)
}