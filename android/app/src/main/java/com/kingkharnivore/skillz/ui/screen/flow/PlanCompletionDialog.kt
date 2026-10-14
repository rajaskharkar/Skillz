package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R

@Composable
internal fun PlanCompletionDialog(saving: Boolean, error: String?, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.flow_plan_completion_question)) },
        text = error?.let { message -> { Text(message, color = MaterialTheme.colorScheme.error) } },
        confirmButton = {
            TextButton(enabled = !saving, onClick = { onAnswer(true) }) {
                Text(stringResource(R.string.flow_plan_completion_yes))
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = { onAnswer(false) }) {
                Text(stringResource(R.string.flow_plan_completion_no))
            }
        }
    )
}
