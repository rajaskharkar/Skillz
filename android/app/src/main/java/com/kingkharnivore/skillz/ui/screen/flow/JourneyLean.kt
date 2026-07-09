package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
private const val MaxJourneySuggestions = 7

@Composable
fun JourneyLean(
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    val cleanTags = remember(tags) { cleanAndDeduplicateTags(tags) }
    val journeyLabel = pluralStringResource(
        id = R.plurals.flow_journey_label,
        count = cleanTags.size.coerceAtLeast(1),
        cleanTags.size.coerceAtLeast(1)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JourneyLabel(journeyLabel = journeyLabel)

        MostUsedJourneyChips(
            tags = cleanTags.take(MaxJourneyChips),
            selectedTagName = tagName,
            onTagSelected = { tag ->
                onTagClicked(tag)
                if (tagName != tag.name) {
                    onTagNameChange(tag.name)
                }
            }
        )

        JourneyAutocompleteField(
            tags = cleanTags,
            tagName = tagName,
            onTagClicked = onTagClicked,
            onTagNameChange = onTagNameChange
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
    tags: List<TagEntity>,
    selectedTagName: String,
    onTagSelected: (TagEntity) -> Unit
) {
    if (tags.isEmpty()) return

    val suggestionsA11y = stringResource(R.string.flow_journey_suggestions_a11y)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = suggestionsA11y },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        items(items = tags, key = { normalizeJourneyName(it.name) }) { tag ->
            val selected = normalizeJourneyName(tag.name) == normalizeJourneyName(selectedTagName)
            val chipA11y = stringResource(R.string.flow_journey_chip_a11y, tag.name)
            val selectedA11y = stringResource(R.string.segmented_tab_selected)
            val notSelectedA11y = stringResource(R.string.segmented_tab_not_selected)

            FilterChip(
                selected = selected,
                onClick = { onTagSelected(tag) },
                label = {
                    Text(
                        text = tag.name,
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
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val trimmedInput = tagName.trim()
    val suggestions by remember(tags, tagName) {
        derivedStateOf { filterJourneySuggestions(tags, tagName, MaxJourneySuggestions) }
    }
    val exactMatch = remember(tags, tagName) { findExactJourneyMatch(tags, tagName) }
    val canCreate = trimmedInput.isNotEmpty() && exactMatch == null
    val shouldShowMenu = expanded && hasFocus && (suggestions.isNotEmpty() || canCreate)

    fun closeMenu() {
        expanded = false
        focusManager.clearFocus()
    }

    fun selectExisting(tag: TagEntity) {
        onTagClicked(tag)
        if (tagName != tag.name) {
            onTagNameChange(tag.name)
        }
        closeMenu()
    }

    fun useTypedJourney() {
        when {
            trimmedInput.isEmpty() -> closeMenu()
            exactMatch != null -> selectExisting(exactMatch)
            else -> {
                onTagNameChange(trimmedInput)
                closeMenu()
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        val colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
        val journeyNameA11y = stringResource(R.string.flow_journey_input_a11y)

        TextField(
            value = tagName,
            onValueChange = {
                onTagNameChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    hasFocus = state.isFocused
                    expanded = state.isFocused
                }
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
            keyboardActions = KeyboardActions(onDone = { useTypedJourney() }),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            ),
            colors = colors
        )

        JourneySuggestionsMenu(
            expanded = shouldShowMenu,
            suggestions = suggestions,
            createName = trimmedInput.takeIf { canCreate },
            onDismiss = { expanded = false },
            onTagSelected = ::selectExisting,
            onCreateSelected = {
                onTagNameChange(it)
                closeMenu()
            }
        )
    }
}

@Composable
private fun JourneySuggestionsMenu(
    expanded: Boolean,
    suggestions: List<TagEntity>,
    createName: String?,
    onDismiss: () -> Unit,
    onTagSelected: (TagEntity) -> Unit,
    onCreateSelected: (String) -> Unit
) {
    val suggestionsA11y = stringResource(R.string.flow_journey_suggestions_a11y)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 320.dp)
            .semantics { contentDescription = suggestionsA11y }
    ) {
        suggestions.forEach { tag ->
            JourneySuggestionRow(
                tag = tag,
                onClick = { onTagSelected(tag) }
            )
        }
        createName?.let { name ->
            CreateJourneyRow(
                journeyName = name,
                onClick = { onCreateSelected(name) }
            )
        }
    }
}

@Composable
private fun JourneySuggestionRow(tag: TagEntity, onClick: () -> Unit) {
    val selectA11y = stringResource(R.string.flow_journey_select_a11y, tag.name)
    DropdownMenuItem(
        text = {
            Text(
                text = tag.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Button
            contentDescription = selectA11y
        }
    )
}

@Composable
private fun CreateJourneyRow(journeyName: String, onClick: () -> Unit) {
    val createText = stringResource(R.string.flow_journey_create_row, journeyName)
    val createA11y = stringResource(R.string.flow_journey_create_a11y, journeyName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = createA11y }
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

private fun normalizeJourneyName(value: String): String = value.trim().lowercase(Locale.ROOT)

private fun cleanAndDeduplicateTags(tags: List<TagEntity>): List<TagEntity> = tags
    .asSequence()
    .filter { it.name.isNotBlank() }
    .distinctBy { normalizeJourneyName(it.name) }
    .toList()

private fun filterJourneySuggestions(tags: List<TagEntity>, query: String, limit: Int): List<TagEntity> {
    val normalizedQuery = normalizeJourneyName(query)
    if (normalizedQuery.isEmpty()) return tags.take(limit)

    return tags
        .asSequence()
        .mapNotNull { tag ->
            val normalizedName = normalizeJourneyName(tag.name)
            val rank = when {
                normalizedName == normalizedQuery -> 0
                normalizedName.startsWith(normalizedQuery) -> 1
                normalizedName.contains(normalizedQuery) -> 2
                else -> null
            }
            rank?.let { it to tag }
        }
        .sortedBy { it.first }
        .map { it.second }
        .take(limit)
        .toList()
}

private fun findExactJourneyMatch(tags: List<TagEntity>, query: String): TagEntity? {
    val normalizedQuery = normalizeJourneyName(query)
    if (normalizedQuery.isEmpty()) return null
    return tags.firstOrNull { normalizeJourneyName(it.name) == normalizedQuery }
}
