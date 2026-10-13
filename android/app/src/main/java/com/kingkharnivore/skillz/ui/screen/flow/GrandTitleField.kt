package com.kingkharnivore.skillz.ui.screen.flow

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.flow.ActivityTitleSuggestion
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.ui.screen.paths.flowModeLabel

@Composable
fun GrandTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes labelRes: Int = R.string.flow_title_field_label,
    @StringRes placeholderRes: Int = R.string.flow_title_field_placeholder,
    @StringRes a11yRes: Int = R.string.flow_title_field_a11y,
    activitySuggestions: List<ActivityTitleSuggestion> = emptyList(),
    onActivitySelected: (ActivityTitleSuggestion) -> Unit = {}
) {
    var titleFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val labelText = stringResource(labelRes)
    val placeholderText = stringResource(placeholderRes)
    val fieldA11y = stringResource(a11yRes)

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
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
                .onFocusChanged { titleFocused = it.isFocused }
                .semantics {
                    contentDescription = fieldA11y
                },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            placeholder = {
                Text(
                    text = placeholderText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            ),
            colors = colors
        )

        if (titleFocused && activitySuggestions.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                activitySuggestions.forEach { activity ->
                    ListItem(
                        headlineContent = { Text(activity.title) },
                        supportingContent = {
                            Text(listOfNotNull(
                                stringResource(if (activity.kind == HorizonKind.PLAN) R.string.horizon_plan else R.string.horizon_habit),
                                stringResource(flowModeLabel(activity.mode)),
                                activity.tagName
                            ).joinToString(" · "))
                        },
                        modifier = Modifier.clickable {
                            onActivitySelected(activity)
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}
