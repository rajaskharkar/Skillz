package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun GrandTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelText = stringResource(R.string.flow_title_field_label)
    val placeholderText = stringResource(R.string.flow_title_field_placeholder)
    val fieldA11y = stringResource(R.string.flow_title_field_a11y)

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.tertiary
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = labelText,
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

        TextField(
            value = value,
            onValueChange = { onValueChange(it.take(60)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = fieldA11y
                },
            placeholder = {
                Text(
                    text = placeholderText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            shape = RoundedCornerShape(28.dp),
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
}