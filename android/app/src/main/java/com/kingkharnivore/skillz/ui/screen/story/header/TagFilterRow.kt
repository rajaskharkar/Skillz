package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@Composable
fun TagFilterRow(
    tags: List<TagUiModel>,
    selectedTagIds: Set<Long>,
    onTagToggled: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    if (tags.isEmpty()) return

    val observeJourneysLabel = if (selectedTagIds.isEmpty()) {
        stringResource(R.string.tag_filter_observe_journeys)
    } else {
        stringResource(R.string.tag_filter_observe_journeys_selected, selectedTagIds.size)
    }
    val rowA11yLabel = stringResource(R.string.tag_filter_row_a11y)
    val allLabel = stringResource(R.string.tag_filter_all)
    val allA11yLabel = stringResource(R.string.tag_filter_all_a11y)
    val selectedLabel = stringResource(R.string.tag_filter_selected)
    val notSelectedLabel = stringResource(R.string.tag_filter_not_selected)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = observeJourneysLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.semantics { heading() }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = rowA11yLabel
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            item {
                val allState = stringResource(
                    R.string.tag_filter_chip_state,
                    allA11yLabel,
                    if (selectedTagIds.isEmpty()) selectedLabel else notSelectedLabel
                )

                FilterChip(
                    selected = selectedTagIds.isEmpty(),
                    onClick = onClearAll,
                    label = {
                        Text(
                            text = allLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics {
                            contentDescription = allA11yLabel
                            stateDescription = allState
                        }
                )
            }

            items(
                items = tags,
                key = { it.id }
            ) { tag ->
                val isSelected = tag.id in selectedTagIds
                val chipState = stringResource(
                    R.string.tag_filter_chip_state,
                    tag.name,
                    if (isSelected) selectedLabel else notSelectedLabel
                )

                FilterChip(
                    selected = isSelected,
                    onClick = { onTagToggled(tag.id) },
                    label = {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics {
                            contentDescription = tag.name
                            stateDescription = chipState
                        }
                )
            }
        }
    }
}