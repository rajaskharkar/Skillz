package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.viewmodel.ReminderFlowPrompt

@Composable
internal fun ReminderFlowDialog(prompt: ReminderFlowPrompt, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val habit = prompt.kind == HorizonKind.HABIT
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.reminder_launch_title, prompt.title)) },
        text = { Text(stringResource(
            if (!prompt.hasActiveFlow) R.string.reminder_launch_replace_draft
            else if (habit) R.string.reminder_launch_switch_habit else R.string.reminder_launch_switch_plan,
            prompt.currentTitle
        )) },
        confirmButton = { TextButton(onClick = onConfirm) {
            Text(stringResource(if (habit) R.string.reminder_launch_confirm_habit else R.string.reminder_launch_confirm_plan))
        } },
        dismissButton = { TextButton(onClick = onCancel) {
            Text(stringResource(if (prompt.hasActiveFlow) R.string.reminder_launch_keep_flow else R.string.reminder_launch_keep_draft))
        } }
    )
}
