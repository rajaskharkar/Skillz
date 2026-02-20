package com.kingkharnivore.skillz.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun NotepadScreen(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // ✅ One local TextFieldValue that preserves selection/cursor correctly
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }

    // ✅ Only sync when external `text` changed NOT due to this field’s typing.
    // This prevents cursor being forced to the end after every keystroke/tap.
    LaunchedEffect(text) {
        if (text != fieldValue.text) {
            val newSelection = fieldValue.selection
                .let { sel ->
                    val clampedStart = sel.start.coerceIn(0, text.length)
                    val clampedEnd = sel.end.coerceIn(0, text.length)
                    TextRange(clampedStart, clampedEnd)
                }

            fieldValue = fieldValue.copy(
                text = text,
                selection = newSelection
            )
        }
    }

    // Optional: keep your scroll-to-bottom behavior (but only when user is already near bottom)
    val maxScroll by remember { derivedStateOf { scrollState.maxValue } }
    LaunchedEffect(maxScroll) {
        // If you *always* scroll to bottom, it can fight editing earlier lines.
        // This keeps behavior but is less annoying:
        val nearBottom = (scrollState.value >= (maxScroll - 80))
        if (nearBottom) scrollState.scrollTo(maxScroll)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val rawText = newValue.text

                // Auto-capitalize ONLY the first character of the first line
                val updatedText =
                    if (rawText.isNotEmpty() && rawText.first().isLowerCase()) {
                        rawText.replaceFirstChar { it.uppercase() }
                    } else {
                        rawText
                    }

                // If we changed the first char, adjust selection so cursor doesn't jump weirdly
                val updatedSelection =
                    if (updatedText != rawText) {
                        val delta = updatedText.length - rawText.length
                        TextRange(
                            start = (newValue.selection.start + delta).coerceAtLeast(0),
                            end = (newValue.selection.end + delta).coerceAtLeast(0)
                        )
                    } else {
                        newValue.selection
                    }

                fieldValue = newValue.copy(
                    text = updatedText,
                    selection = updatedSelection
                )

                onTextChange(updatedText)
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (fieldValue.text.isEmpty()) {
                    Text(
                        text = "SkratchPad — For all ideas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                innerTextField()
            }
        )
    }
}
