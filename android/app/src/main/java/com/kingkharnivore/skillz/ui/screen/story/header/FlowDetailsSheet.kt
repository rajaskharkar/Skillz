package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.SessionEditState
import com.kingkharnivore.skillz.ui.screen.story.chronicle.PulseCard
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowDetailsSheet(
    editState: SessionEditState,
    tags: List<TagUiModel>,
    childPulses: List<PulseListItemUiModel>,
    onCreatePulse: (sessionId: Long, title: String, description: String, tagName: String) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onEditPulse: (PulseListItemUiModel) -> Unit,
    chronicleMoments: List<com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity>
) {
    val session = editState.editingSession.value ?: return

    var pulseTitle by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var pulseDescription by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var pulseTagName by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var showPulseComposer by rememberSaveable(session.sessionId) { mutableStateOf(false) }

    val paneTitleText = stringResource(R.string.flow_details_sheet_pane_title)
    val flowTypeText = stringResource(
        if (session.isSoftMode) {
            R.string.flow_details_type_soft_flow
        } else {
            R.string.flow_details_type_flow
        }
    )
    val closeText = stringResource(R.string.common_close)
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

    val flowSummaryA11y = if (session.tagName.isNotBlank()) {
        stringResource(
            R.string.flow_details_flow_summary_with_tag_a11y,
            flowTypeText,
            session.title,
            session.tagName
        )
    } else {
        stringResource(
            R.string.flow_details_flow_summary_a11y,
            flowTypeText,
            session.title
        )
    }

    val suggestionsText = remember(tags) {
        tags.map { it.name }
            .filter { it.isNotBlank() }
            .take(6)
            .joinToString(" • ")
    }


    ModalBottomSheet(
        onDismissRequest = { editState.stopEditing() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { this.paneTitle = paneTitleText }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = flowSummaryA11y
                    },
                shape = RoundedCornerShape(20.dp),
                color = if (session.isSoftMode) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (session.isSoftMode) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = flowTypeText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (session.tagName.isNotBlank()) {
                        Text(
                            text = session.tagName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (chronicleMoments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(stringResource(R.string.chronicle_title), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleReader(chronicleMoments)
                }
                HorizontalDivider()
            }

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
                                onToggleExpand = {},
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
                        supportingText = {
                            if (suggestionsText.isNotBlank()) {
                                Text(
                                    stringResource(
                                        R.string.flow_details_suggestions,
                                        suggestionsText
                                    )
                                )
                            }
                        }
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
                                session.sessionId,
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

            Spacer(Modifier.height(18.dp))
        }
    }
}
