package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.TagEntity

@Composable
fun JourneyLean(
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    val journeyLabel = pluralStringResource(
        id = R.plurals.flow_journey_label,
        count = tags.size.coerceAtLeast(1),
        tags.size.coerceAtLeast(1)
    )
    val cleanTags = remember(tags) { tags.filter { it.name.isNotBlank() } }

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
            .padding(bottom = 10.dp)
            .semantics {
                contentDescription = journeyA11y
            }
    )

    if (cleanTags.isNotEmpty()) {
        val stringResource = stringResource(R.string.flow_journey_suggestions_a11y)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = stringResource
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(items = cleanTags, key = { it.id }) { tag ->
                val selected = tag.name == tagName
                val stringResourceTabs = stringResource(
                    R.string.flow_journey_chip_a11y,
                    tag.name
                )
                val tabSelected = stringResource(R.string.segmented_tab_selected)
                val tabNotSelected = stringResource(R.string.segmented_tab_not_selected)
                AssistChip(
                    onClick = { onTagClicked(tag) },
                    label = { Text(tag.name) },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = stringResourceTabs
                        stateDescription = if (selected) {
                            tabSelected
                        } else {
                            tabNotSelected
                        }
                    }
                )
            }
        }
    }

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
        onValueChange = onTagNameChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = journeyNameA11y
            },
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
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp
        ),
        colors = colors
    )
}