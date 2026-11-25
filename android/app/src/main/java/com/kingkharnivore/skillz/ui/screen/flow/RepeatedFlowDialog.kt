package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.HorizonKind

@Composable
internal fun RepeatedFlowDialog(saving: Boolean, hasError: Boolean, onAnswer: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!saving) onAnswer(null) },
        title = { Text(stringResource(R.string.flow_repeat_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.flow_repeat_question))
                if (hasError) {
                    Text(stringResource(R.string.flow_repeat_error), color = MaterialTheme.colorScheme.error)
                }
                Button(
                    enabled = !saving,
                    onClick = { onAnswer(HorizonKind.HABIT) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.flow_repeat_add_habit)) }
                OutlinedButton(
                    enabled = !saving,
                    onClick = { onAnswer(HorizonKind.PLAN) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.flow_repeat_add_plan)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = { onAnswer(null) }) {
                Text(stringResource(R.string.flow_repeat_skip))
            }
        }
    )
}
