package com.kingkharnivore.skillz.ui.screen.story.chronicle

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R

@Composable
fun BeamBonusChip(
    bonusPoints: Int
) {
    val compactPoints = stringResource(R.string.beam_bonus_chip_points_compact, bonusPoints)
    val a11yLabel = stringResource(R.string.beam_bonus_chip_a11y, bonusPoints)

    Surface(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = a11yLabel
        },
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "★",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = compactPoints,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}