package com.kingkharnivore.skillz.ui.screen.paths

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.reminders.ActivityReminder
import com.kingkharnivore.skillz.domain.reminders.ActivityReminderScheduler
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Calendar

internal val ActivityReminderSaver = listSaver<ActivityReminder, Any>(
    save = { listOf(it.enabled, it.frequency, it.minuteOfDay, it.weekDays, it.monthDays, it.oneTimeDateEpochDay ?: Long.MIN_VALUE, it.oneTimeConsumedAt ?: Long.MIN_VALUE) },
    restore = { ActivityReminder(it[0] as Boolean, it[1] as String, it[2] as Int, it[3] as Int, it[4] as Long,
        (it.getOrNull(5) as? Long)?.takeUnless { value -> value == Long.MIN_VALUE },
        (it.getOrNull(6) as? Long)?.takeUnless { value -> value == Long.MIN_VALUE }) }
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivityReminderSection(reminder: ActivityReminder, saving: Boolean, previous: ActivityReminder? = null, onChange: (ActivityReminder) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun updateReminder(value: ActivityReminder) {
        val sameSchedule = value.frequency == reminder.frequency && value.minuteOfDay == reminder.minuteOfDay &&
            value.oneTimeDateEpochDay == reminder.oneTimeDateEpochDay
        onChange(if (sameSchedule) value else value.copy(oneTimeConsumedAt = null))
    }
    fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(ActivityReminderScheduler.CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    var allowed by remember { mutableStateOf(notificationsAllowed()) }
    var precise by remember { mutableStateOf(exactAllowed()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = notificationsAllowed() }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { allowed = notificationsAllowed(); precise = exactAllowed() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    HorizontalDivider()
    val heading = stringResource(R.string.activity_reminder_heading)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(heading, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(reminder.enabled, onCheckedChange = {
            updateReminder(reminder.copy(enabled = it))
            if (it && !allowed && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }, enabled = !saving, modifier = Modifier.semantics { contentDescription = heading })
    }
    if (!reminder.enabled) return

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ActivityReminder.ONE_TIME to R.string.activity_reminder_one_time,
            ActivityReminder.DAILY to R.string.activity_reminder_daily,
            ActivityReminder.WEEKLY to R.string.activity_reminder_weekly,
            ActivityReminder.MONTHLY to R.string.activity_reminder_monthly).forEach { (value, label) ->
            FilterChip(reminder.frequency == value, {
                val date = if (value == ActivityReminder.ONE_TIME && reminder.oneTimeDateEpochDay == null) {
                    val today = LocalDate.now()
                    val candidate = reminder.copy(oneTimeDateEpochDay = today.toEpochDay()).oneTimeInstant(ZoneId.systemDefault())
                    (if (candidate != null && candidate > Instant.now()) today else today.plusDays(1)).toEpochDay()
                } else reminder.oneTimeDateEpochDay
                updateReminder(reminder.copy(frequency = value, oneTimeDateEpochDay = date))
            },
                enabled = !saving, label = { Text(stringResource(label)) })
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    if (reminder.frequency == ActivityReminder.ONE_TIME) {
        val date = reminder.oneTimeDateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                updateReminder(reminder.copy(oneTimeDateEpochDay = LocalDate.of(year, month + 1, day).toEpochDay()))
            }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.show()
        }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.activity_reminder_date, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))))
        }
    }
    val time = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60); set(Calendar.MINUTE, reminder.minuteOfDay % 60) }
    OutlinedButton(onClick = {
        TimePickerDialog(context, { _, hour, minute -> updateReminder(reminder.copy(minuteOfDay = hour * 60 + minute)) },
            reminder.minuteOfDay / 60, reminder.minuteOfDay % 60, DateFormat.is24HourFormat(context)).show()
    }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.activity_reminder_time, DateFormat.getTimeFormat(context).format(time.time)))
    }
    if (reminder.frequency == ActivityReminder.WEEKLY) {
        Text(stringResource(R.string.activity_reminder_weekdays), style = MaterialTheme.typography.labelLarge)
        val first = WeekFields.of(locale).firstDayOfWeek
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0L..6L).forEach { offset ->
                val day = first.plus(offset)
                val bit = 1 shl (day.value - 1)
                val fullName = day.getDisplayName(TextStyle.FULL, locale)
                FilterChip(reminder.weekDays and bit != 0, { updateReminder(reminder.copy(weekDays = reminder.weekDays xor bit)) },
                    enabled = !saving, label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    modifier = Modifier.semantics { contentDescription = fullName })
            }
        }
    }
    if (reminder.frequency == ActivityReminder.MONTHLY) {
        Text(stringResource(R.string.activity_reminder_month_dates), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to R.string.activity_reminder_first, 0 to R.string.activity_reminder_last).forEach { (day, label) ->
                val bit = 1L shl day
                FilterChip(reminder.monthDays and bit != 0L, { updateReminder(reminder.copy(monthDays = reminder.monthDays xor bit)) },
                    enabled = !saving, label = { Text(stringResource(label)) })
            }
        }
        Text(stringResource(R.string.activity_reminder_custom_dates), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..31).forEach { day ->
                val bit = 1L shl day
                val label = stringResource(R.string.activity_reminder_day, day)
                FilterChip(reminder.monthDays and bit != 0L, { updateReminder(reminder.copy(monthDays = reminder.monthDays xor bit)) },
                    enabled = !saving, label = { Text(String.format(locale, "%d", day)) },
                    modifier = Modifier.semantics { contentDescription = label })
            }
        }
        Text(stringResource(R.string.activity_reminder_short_month), style = MaterialTheme.typography.bodySmall)
    }
    if (!reminder.canSave(previous)) Text(stringResource(
        if (reminder.frequency == ActivityReminder.ONE_TIME) R.string.activity_reminder_future_date else R.string.activity_reminder_select_day
    ), color = MaterialTheme.colorScheme.error)
    else if (reminder.isPastOneTime()) Text(stringResource(R.string.activity_reminder_one_time_past_hint), style = MaterialTheme.typography.bodySmall)
    if (!allowed) {
        Text(stringResource(R.string.activity_reminder_permission), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, enabled = !saving) { Text(stringResource(R.string.activity_reminder_settings)) }
    }
    if (!precise && Build.VERSION.SDK_INT >= 31) {
        Text(stringResource(R.string.activity_reminder_timing), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }, enabled = !saving) { Text(stringResource(R.string.activity_reminder_exact_settings)) }
    }
}

/** The card shows every selected day, and explicitly distinguishes disabled and archived reminders. */
@Composable
internal fun activityReminderSummary(reminder: ActivityReminder, archived: Boolean = false): String {
    if (!reminder.enabled || !reminder.isValid()) return stringResource(R.string.activity_reminder_off)
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, reminder.minuteOfDay / 60)
        set(Calendar.MINUTE, reminder.minuteOfDay % 60)
    }
    val schedule = buildList {
        add(stringResource(when (reminder.frequency) {
            ActivityReminder.ONE_TIME -> R.string.activity_reminder_one_time
            ActivityReminder.WEEKLY -> R.string.activity_reminder_weekly
            ActivityReminder.MONTHLY -> R.string.activity_reminder_monthly
            else -> R.string.activity_reminder_daily
        }))
        if (reminder.frequency == ActivityReminder.ONE_TIME) {
            add(LocalDate.ofEpochDay(requireNotNull(reminder.oneTimeDateEpochDay))
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
        }
        if (reminder.frequency == ActivityReminder.WEEKLY) {
            val first = WeekFields.of(locale).firstDayOfWeek
            add((0L..6L).map { first.plus(it) }
                .filter { reminder.weekDays and (1 shl (it.value - 1)) != 0 }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) })
        }
        if (reminder.frequency == ActivityReminder.MONTHLY) {
            add(buildList {
                if (reminder.monthDays and 2L != 0L) add(stringResource(R.string.activity_reminder_first))
                (2..31).filter { reminder.monthDays and (1L shl it) != 0L }
                    .forEach { add(String.format(locale, "%d", it)) }
                if (reminder.monthDays and 1L != 0L) add(stringResource(R.string.activity_reminder_last))
            }.joinToString(", "))
        }
        add(DateFormat.getTimeFormat(context).format(time.time))
    }.joinToString(" · ")
    return stringResource(when {
        reminder.isPastOneTime() -> R.string.activity_reminder_past_summary
        archived -> R.string.activity_reminder_paused_summary
        else -> R.string.activity_reminder_summary
    }, schedule)
}
