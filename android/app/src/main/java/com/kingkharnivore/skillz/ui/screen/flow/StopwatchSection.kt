package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import com.kingkharnivore.skillz.viewmodel.FlowViewModel

@Composable
fun StopwatchSection(
    state: StopwatchState,
    viewModel: FlowViewModel,
    showScoreUi: Boolean,
    calmMode: Boolean,
    timerText: String
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    val resetText = stringResource(R.string.stopwatch_reset)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowTimer(timerText = timerText, calmMode = calmMode)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val threshold = 2 * 60_000L
                    if (state.elapsedMs >= threshold) showResetConfirm = true
                    else viewModel.resetStopwatch()
                },
                enabled = state.elapsedMs > 0L && !state.isRunning
            ) {
                Text(resetText)
            }
        }

        // If later you add score UI near timer, it must be gated like this:
        // if (showScoreUi && !calmMode) { ... }

        if (showResetConfirm) {
            val minutes = (state.elapsedMs / 60_000L).toInt()
            val resetBody = pluralStringResource(
                R.plurals.stopwatch_reset_confirm_body,
                minutes,
                minutes
            )

            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text(stringResource(R.string.stopwatch_reset_confirm_title)) },
                text = { Text(resetBody) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            viewModel.resetStopwatch()
                        }
                    ) {
                        Text(stringResource(R.string.stopwatch_reset_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

/** The same timer presentation is used for elapsed Flow time and signed Surge time. */
@Composable
internal fun FlowTimer(timerText: String, calmMode: Boolean) {
    val timerA11y = stringResource(R.string.stopwatch_timer_a11y, timerText)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (calmMode) R.string.stopwatch_title_calm else R.string.stopwatch_title_in_flow
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (calmMode) 0.55f else 1f)
        )
        Text(
            text = timerText,
            style = if (calmMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (calmMode) 0.78f else 1f),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = timerA11y }
        )
    }
}
