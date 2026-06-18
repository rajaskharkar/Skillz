package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R

@Composable
fun ChronicleField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val labelText = stringResource(R.string.chronicle_field_label)
    val placeholderText = stringResource(R.string.chronicle_field_placeholder)
    val fieldA11y = stringResource(
        R.string.chronicle_field_a11y,
        if (value.isBlank()) placeholderText else value
    )

    Text(
        text = labelText,
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 1.dp,
        color = Color.Transparent
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = fieldA11y
                }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(28.dp)
                ),
            placeholder = {
                Text(
                    text = placeholderText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Thin,
                letterSpacing = 0.2.sp,
                lineHeight = 30.sp
            ),
            minLines = 6,
            shape = RoundedCornerShape(28.dp),
            colors = colors
        )
    }
}