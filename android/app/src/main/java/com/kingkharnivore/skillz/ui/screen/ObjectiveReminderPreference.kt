package com.kingkharnivore.skillz.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import kotlinx.coroutines.launch

@Composable
internal fun ObjectiveReminderPreference(minutes: Int, onSave: suspend (Int) -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().clickable { input = minutes.toString(); failed = false; show = true }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.help_objective_reminder_title), style = MaterialTheme.typography.titleSmall)
            Text(if (minutes == 0) stringResource(R.string.help_reminder_at_target) else stringResource(R.string.help_reminder_minutes_before, minutes))
        }
    }
    if (show) AlertDialog(
        onDismissRequest = { if (!saving) show = false },
        title = { Text(stringResource(R.string.help_objective_reminder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.help_objective_reminder_body))
                OutlinedTextField(value = input, onValueChange = { input = it; failed = false },
                    label = { Text(stringResource(R.string.help_reminder_minutes_label)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !saving,
                    isError = input.isNotEmpty() && (input.toIntOrNull()?.let { it >= 0 } != true))
                if (failed) Text(stringResource(R.string.help_reminder_save_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(enabled = !saving && input.toIntOrNull()?.let { it >= 0 } == true, onClick = {
            scope.launch {
                saving = true
                try { onSave(input.toInt()); show = false }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { failed = true }
                finally { saving = false }
            }
        }) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = { show = false }) { Text(stringResource(R.string.common_cancel)) } }
    )
}
