package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.chronicle.PulseCard

@Composable
fun FlowPulseDetails(
    sessionId: Long,
    childPulses: List<PulseListItemUiModel>,
    onCreatePulse: (Long, String, String, String) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onEditPulse: (PulseListItemUiModel) -> Unit
) {
    var pulseTitle by rememberSaveable(sessionId) { mutableStateOf("") }
    var pulseDescription by rememberSaveable(sessionId) { mutableStateOf("") }
    var pulseTagName by rememberSaveable(sessionId) { mutableStateOf("") }
    var showPulseComposer by rememberSaveable(sessionId) { mutableStateOf(false) }
    val pulsesTitle = stringResource(R.string.flow_details_pulses_title)
    val pulsesSubtitle = stringResource(R.string.flow_details_pulses_subtitle)
    val noPulsesText = stringResource(R.string.flow_details_no_pulses)
    val hidePulseComposerText = stringResource(R.string.flow_details_hide_pulse_composer)
    val addPulseText = stringResource(R.string.flow_details_add_pulse)
    val pulseTitleLabel = stringResource(R.string.flow_details_pulse_title_label)
    val journeyOptionalLabel = stringResource(R.string.flow_details_journey_optional_label)
    val pulseDescriptionLabel = stringResource(R.string.flow_details_pulse_description_label)
    val savePulseText = stringResource(R.string.flow_details_save_pulse)
    val pulseCountA11y = stringResource(R.string.flow_details_pulse_count_a11y, childPulses.size)
    val togglePulseComposerA11y = stringResource(R.string.flow_details_toggle_pulse_composer_a11y)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = pulsesTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = pulsesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            Text(
                text = childPulses.size.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics {
                    contentDescription = pulseCountA11y
                }
            )
        }

        if (childPulses.isEmpty()) {
            Text(
                text = noPulsesText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                childPulses.forEach { pulse ->
                    PulseCard(
                        pulse = pulse,
                        isExpanded = false,
                        onToggleExpand = { onEditPulse(pulse) },
                        onLongPress = { onEditPulse(pulse) },
                        onDeletePulse = { onDeletePulse(pulse.pulseId) },
                        nested = true
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showPulseComposer = !showPulseComposer },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = togglePulseComposerA11y
                }
        ) {
            Text(if (showPulseComposer) hidePulseComposerText else addPulseText)
        }

        if (showPulseComposer) {
            OutlinedTextField(
                value = pulseTitle,
                onValueChange = { pulseTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(pulseTitleLabel) },
                singleLine = true
            )

            OutlinedTextField(
                value = pulseTagName,
                onValueChange = { pulseTagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(journeyOptionalLabel) },
                placeholder = { Text(journeyOptionalLabel) },
                singleLine = true,
            )

            OutlinedTextField(
                value = pulseDescription,
                onValueChange = { pulseDescription = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text(pulseDescriptionLabel) }
            )

            Button(
                enabled = pulseTitle.isNotBlank() && pulseDescription.isNotBlank(),
                onClick = {
                    onCreatePulse(
                        sessionId,
                        pulseTitle,
                        pulseDescription,
                        pulseTagName
                    )
                    pulseTitle = ""
                    pulseDescription = ""
                    pulseTagName = ""
                    showPulseComposer = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(savePulseText)
            }
        }
    }

}
