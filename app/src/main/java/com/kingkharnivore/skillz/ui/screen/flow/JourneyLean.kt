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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.data.model.entity.TagEntity

@Composable
fun JourneyLean(
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    Text(
        text = if (tags.size > 1) "Journeys" else "Journey",
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )

    val cleanTags = remember(tags) { tags.filter { it.name.isNotBlank() } }
    if (cleanTags.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(items = cleanTags, key = { it.id }) { tag ->
                AssistChip(
                    onClick = { onTagClicked(tag) },
                    label = { Text(tag.name) }
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
        placeholder = { Text(
            text = "Start a new journey…",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)) },
        shape = RoundedCornerShape(999.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp
        ),
        colors = colors
    )
}