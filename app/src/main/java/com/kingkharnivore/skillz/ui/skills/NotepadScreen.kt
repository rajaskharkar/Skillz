package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Local state so we control cursor position
    var fieldValue by remember(text) {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length) // cursor at end
            )
        )
    }

    // Keep local field in sync with external `text`
    LaunchedEffect(text) {
        fieldValue = fieldValue.copy(
            text = text,
            selection = TextRange(text.length)
        )
    }

    // 1) Scroll to bottom whenever text changes
    LaunchedEffect(fieldValue.text) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    // 2) Also scroll to bottom whenever the max scroll range changes
    //    (e.g., when keyboard appears and layout height shrinks)
    val maxScroll by remember {
        derivedStateOf { scrollState.maxValue }
    }
    LaunchedEffect(maxScroll) {
        scrollState.scrollTo(maxScroll)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skillz") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()      // lift above keyboard
                .padding(16.dp)
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    // always keep cursor at end
                    fieldValue = newValue.copy(
                        selection = TextRange(newValue.text.length)
                    )
                    onTextChange(newValue.text)
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
}



