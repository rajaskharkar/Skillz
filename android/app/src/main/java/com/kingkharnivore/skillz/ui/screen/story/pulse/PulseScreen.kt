@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.story.pulse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.chronicle.ChroniclePage
import com.kingkharnivore.skillz.ui.screen.chronicle.ChroniclePagerSelector
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.kingkharnivore.skillz.ui.screen.flow.GrandTitleField
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@Composable
fun PulseScreen(
    viewModel: StoryViewModel,
    isFlowStateActive: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var attachToCurrentFlow by rememberSaveable { mutableStateOf(isFlowStateActive) }

    val screenTitle = stringResource(R.string.pulse_screen_title)
    val backLabel = stringResource(R.string.common_back)
    val cancelLabel = stringResource(R.string.common_cancel)
    val savePulseLabel = stringResource(R.string.pulse_screen_save_pulse)
    val attachTitle = stringResource(R.string.pulse_screen_attach_to_current_flow)
    val attachEnabledText = stringResource(R.string.pulse_screen_attach_enabled)
    val attachDisabledText = stringResource(R.string.pulse_screen_attach_disabled)
    val attachSwitchLabel = stringResource(R.string.pulse_a11y_attach_switch)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val pagerScope = rememberCoroutineScope()
    val cancelPulse = { viewModel.cancelPulseDraft(onCancel) }
    val chronicleState by viewModel.pulseChronicle.state.collectAsState()
    val chronicleRequiresAttention = chronicleState.blocksCompletion
    val restoredCreatedPulseId by viewModel.restoredCreatedPulseId.collectAsState()
    var showDraftPrompt by remember { mutableStateOf(false) }
    fun savePulse() = viewModel.pulseChronicle.quiesce {
        viewModel.createPulseFromStory(title, tagName, attachToCurrentFlow, onDone)
    }
    LaunchedEffect(restoredCreatedPulseId) {
        if (restoredCreatedPulseId != null) onDone()
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
                    IconButton(onClick = cancelPulse) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = backLabel
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ChroniclePagerSelector(
                selectedPage = pagerState.currentPage,
                primaryIcon = Icons.Outlined.PsychologyAlt,
                primaryLabel = screenTitle,
                primaryContentDescription = screenTitle,
                chronicleLabel = stringResource(R.string.chronicle_title),
                chronicleContentDescription = stringResource(R.string.chronicle_title),
                canLeaveChronicle = !chronicleRequiresAttention,
                onPageSelected = { page -> pagerScope.launch { pagerState.animateScrollToPage(page) } },
            )
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = !chronicleRequiresAttention) { page ->
                if (page == 1) { ChroniclePage(viewModel.pulseChronicle); return@HorizontalPager }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulseHeroCard()

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    GrandTitleField(
                        value = title,
                        onValueChange = { title = it },
                        labelRes = R.string.pulse_title_field_label,
                        placeholderRes = R.string.pulse_title_field_placeholder,
                        a11yRes = R.string.pulse_title_field_a11y
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    PulseJourneyLean(
                        tags = uiState.tags,
                        tagName = tagName,
                        onTagClicked = { tag -> tagName = tag.name },
                        onTagNameChange = { tagName = it }
                    )
                }
            }

            if (isFlowStateActive) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (attachToCurrentFlow) {
                                    attachEnabledText
                                } else {
                                    attachDisabledText
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            )
                        }

                        Switch(
                            checked = attachToCurrentFlow,
                            onCheckedChange = { attachToCurrentFlow = it },
                            modifier = Modifier.semantics {
                                contentDescription = attachSwitchLabel
                                stateDescription = if (attachToCurrentFlow) {
                                    attachEnabledText
                                } else {
                                    attachDisabledText
                                }
                                role = Role.Switch
                            }
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = cancelPulse,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(cancelLabel)
                }

                Button(
                    onClick = {
                        if (chronicleState.blocksCompletion || chronicleState.draft.isNotBlank()) showDraftPrompt = true
                        else savePulse()
                    },
                    enabled = title.isNotBlank() || chronicleState.moments.isNotEmpty() || chronicleState.draft.isNotBlank(),
                    modifier = Modifier.weight(1.25f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(savePulseLabel)
                }
            }
            }
        }
        }
    }
    if (showDraftPrompt) AlertDialog(
        onDismissRequest = { showDraftPrompt = false },
        title = { Text(stringResource(if (chronicleState.editingId != null) R.string.chronicle_finish_edit else R.string.chronicle_unfinished)) },
        confirmButton = { if (chronicleState.editingId == null) TextButton(onClick = {
            viewModel.pulseChronicle.add { showDraftPrompt = false; savePulse() }
        }) { Text(stringResource(R.string.chronicle_add_moment)) } },
        dismissButton = { Row {
            if (chronicleState.editingId == null) TextButton(onClick = {
                viewModel.pulseChronicle.discardDraft { showDraftPrompt = false; savePulse() }
            }) { Text(stringResource(R.string.chronicle_discard)) }
            TextButton(onClick = { showDraftPrompt = false }) { Text(cancelLabel) }
        } }
    )
}

@Composable
private fun PulseHeroCard() {
    val heroTitle = stringResource(R.string.pulse_hero_title)
    val heroBody = stringResource(R.string.pulse_hero_body)
    val heroA11y = stringResource(R.string.pulse_a11y_pulse_hero)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = heroA11y
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PsychologyAlt,
                    contentDescription = null
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = heroTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = heroBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun PulseJourneyLean(
    tags: List<TagUiModel>,
    tagName: String,
    onTagClicked: (TagUiModel) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    val journeyLabel = stringResource(
        if (tags.size > 1) R.string.pulse_journey_plural else R.string.pulse_journey_singular
    )
    val placeholderLabel = stringResource(R.string.pulse_journey_placeholder)
    val tagSuggestionsLabel = stringResource(R.string.pulse_a11y_tag_suggestions)

    Text(
        text = journeyLabel,
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .semantics { heading() }
    )

    val cleanTags = remember(tags) { tags.filter { it.name.isNotBlank() } }

    if (cleanTags.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = tagSuggestionsLabel
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(items = cleanTags, key = { it.id }) { tag ->
                val selectJourneyLabel =
                    stringResource(R.string.pulse_a11y_select_journey, tag.name)

                AssistChip(
                    onClick = { onTagClicked(tag) },
                    label = { Text(tag.name) },
                    modifier = Modifier.semantics {
                        contentDescription = selectJourneyLabel
                    }
                )
            }
        }
    }

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    TextField(
        value = tagName,
        onValueChange = onTagNameChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholderLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
            )
        },
        shape = RoundedCornerShape(999.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp
        ),
        colors = colors
    )
}
