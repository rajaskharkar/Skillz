package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType

/** One-owner, ordered, read-only renderer; future Moment renderers extend this dispatch. */
@Composable
fun ChronicleReader(moments: List<ChronicleMomentEntity>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        moments.sortedBy { it.position }.forEachIndexed { index, moment ->
            when (moment.type) {
                ChronicleMomentType.TEXT -> Text(
                    text = moment.text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (index < moments.lastIndex) HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = .22f)
            )
        }
    }
}
