package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import java.util.Locale

private const val MaxJourneyChips = 8

@Composable
fun JourneyLean(
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    val journeyOptions = remember(tags) { buildJourneyOptions(tags) }
    var chipSelectionVersion by remember { mutableStateOf(0) }
    val journeyLabel = pluralStringResource(
        id = R.plurals.flow_journey_label,
        count = journeyOptions.size.coerceAtLeast(1),
        journeyOptions.size.coerceAtLeast(1)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JourneyLabel(journeyLabel = journeyLabel)

        MostUsedJourneyChips(
            options = journeyOptions.take(MaxJourneyChips),
            selectedTagName = tagName,
            onTagSelected = { option ->
                onTagClicked(option.tag)
                if (tagName != option.displayName) {
                    onTagNameChange(option.displayName)
                }
                chipSelectionVersion += 1
            }
        )

        JourneyAutocompleteField(
            options = journeyOptions,
            tagName = tagName,
            onTagClicked = onTagClicked,
            onTagNameChange = onTagNameChange,
            externalSelectionVersion = chipSelectionVersion
        )
    }
}

@Composable
private fun JourneyLabel(journeyLabel: String) {
    val journeyA11y = stringResource(R.string.flow_journey_section_a11y, journeyLabel)

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
            .semantics { contentDescription = journeyA11y }
    )
}

@Composable
private fun MostUsedJourneyChips(
    options: List<JourneyOption>,
    selectedTagName: String,
    onTagSelected: (JourneyOption) -> Unit
) {
    if (options.isEmpty()) return

    val suggestionsA11y = stringResource(R.string.flow_journey_suggestions_a11y)
    val selectedJourneyName = remember(selectedTagName) { normalizeJourneyName(selectedTagName) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = suggestionsA11y },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        items(items = options, key = { it.normalizedName }) { option ->
            val selected = option.normalizedName == selectedJourneyName
            val chipA11y = stringResource(R.string.flow_journey_chip_a11y, option.displayName)
            val selectedA11y = stringResource(R.string.segmented_tab_selected)
            val notSelectedA11y = stringResource(R.string.segmented_tab_not_selected)

            FilterChip(
                selected = selected,
                onClick = { onTagSelected(option) },
                label = {
                    Text(
                        text = option.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .widthIn(max = 156.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = chipA11y
                        stateDescription = if (selected) selectedA11y else notSelectedA11y
                    }
            )
        }
    }
}

@Composable
private fun JourneyAutocompleteField(
    options: List<JourneyOption>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit,
    externalSelectionVersion: Int
) {
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    val trimmedInput = remember(tagName) { tagName.trim() }
    val suggestions = remember(options, tagName) {
        filterJourneySuggestions(options, tagName)
    }
    val exactMatch = remember(options, tagName) { findExactJourneyMatch(options, tagName) }
    val canCreate = trimmedInput.isNotEmpty() && exactMatch == null
    val showSuggestions = fieldFocused && suggestionsExpanded && (suggestions.isNotEmpty() || canCreate)

    LaunchedEffect(externalSelectionVersion) {
        if (externalSelectionVersion > 0) {
            suggestionsExpanded = false
        }
    }

    fun collapseSuggestions() {
        suggestionsExpanded = false
    }

    fun selectExisting(option: JourneyOption) {
        onTagClicked(option.tag)
        if (tagName != option.displayName) {
            onTagNameChange(option.displayName)
        }
        collapseSuggestions()
    }

    fun useTypedJourney() {
        when {
            trimmedInput.isEmpty() -> collapseSuggestions()
            exactMatch != null -> {
                onTagClicked(exactMatch.tag)
                if (tagName != exactMatch.displayName) {
                    onTagNameChange(exactMatch.displayName)
                }
                collapseSuggestions()
            }
            else -> {
                onTagNameChange(trimmedInput)
                collapseSuggestions()
            }
        }
        focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        JourneyTextField(
            tagName = tagName,
            onTagNameChange = {
                onTagNameChange(it)
                suggestionsExpanded = true
            },
            onDone = ::useTypedJourney,
            onFocusChanged = { isFocused ->
                fieldFocused = isFocused
                suggestionsExpanded = isFocused
            }
        )

        JourneySuggestionsSurface(
            visible = showSuggestions,
            suggestions = suggestions,
            createName = trimmedInput.takeIf { canCreate },
            onTagSelected = ::selectExisting,
            onCreateSelected = { name ->
                onTagNameChange(name)
                collapseSuggestions()
            }
        )
    }
}

@Composable
private fun JourneyTextField(
    tagName: String,
    onTagNameChange: (String) -> Unit,
    onDone: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    val journeyNameA11y = stringResource(R.string.flow_journey_input_a11y)

    CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
        TextField(
            value = tagName,
            onValueChange = onTagNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                .semantics { contentDescription = journeyNameA11y },
            placeholder = {
                Text(
                    text = stringResource(R.string.flow_journey_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            shape = RoundedCornerShape(999.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            ),
            colors = colors
        )
    }
}

@Composable
private fun JourneySuggestionsSurface(
    visible: Boolean,
    suggestions: List<JourneyOption>,
    createName: String?,
    onTagSelected: (JourneyOption) -> Unit,
    onCreateSelected: (String) -> Unit
) {
    if (!visible) return

    val suggestionsA11y = stringResource(R.string.flow_journey_suggestions_a11y)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .heightIn(max = 280.dp)
            .semantics { contentDescription = suggestionsA11y },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(items = suggestions, key = { it.normalizedName }) { option ->
                JourneySuggestionRow(
                    option = option,
                    onClick = { onTagSelected(option) }
                )
            }
            createName?.let { name ->
                item(key = "create-${normalizeJourneyName(name)}") {
                    CreateJourneyRow(
                        journeyName = name,
                        onClick = { onCreateSelected(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneySuggestionRow(option: JourneyOption, onClick: () -> Unit) {
    val selectA11y = stringResource(R.string.flow_journey_select_a11y, option.displayName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = selectA11y
            }
    ) {
        Text(
            text = option.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CreateJourneyRow(journeyName: String, onClick: () -> Unit) {
    val createText = stringResource(R.string.flow_journey_create_row, journeyName)
    val createA11y = stringResource(R.string.flow_journey_create_a11y, journeyName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = createA11y
            }
    ) {
        Text(
            text = createText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private data class JourneyOption(
    val tag: TagEntity,
    val displayName: String,
    val normalizedName: String
)

private object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit

    override fun hide() = Unit
}

private fun normalizeJourneyName(value: String): String = value.trim().lowercase(Locale.ROOT)

private fun buildJourneyOptions(tags: List<TagEntity>): List<JourneyOption> = tags
    .asSequence()
    .filter { it.name.isNotBlank() }
    .map { tag ->
        JourneyOption(
            tag = tag,
            displayName = tag.name.trim(),
            normalizedName = normalizeJourneyName(tag.name)
        )
    }
    .distinctBy { it.normalizedName }
    .toList()

private fun filterJourneySuggestions(
    options: List<JourneyOption>,
    query: String
): List<JourneyOption> {
    val normalizedQuery = normalizeJourneyName(query)
    if (normalizedQuery.isEmpty()) return options

    val exactMatches = mutableListOf<JourneyOption>()
    val prefixMatches = mutableListOf<JourneyOption>()
    val containsMatches = mutableListOf<JourneyOption>()

    options.forEach { option ->
        when {
            option.normalizedName == normalizedQuery -> exactMatches.add(option)
            option.normalizedName.startsWith(normalizedQuery) -> prefixMatches.add(option)
            option.normalizedName.contains(normalizedQuery) -> containsMatches.add(option)
        }
    }

    return buildList {
        addAll(exactMatches)
        addAll(prefixMatches)
        addAll(containsMatches)
    }
}

private fun findExactJourneyMatch(options: List<JourneyOption>, query: String): JourneyOption? {
    val normalizedQuery = normalizeJourneyName(query)
    if (normalizedQuery.isEmpty()) return null
    return options.firstOrNull { it.normalizedName == normalizedQuery }
}
