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
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi

/** One-owner, ordered, read-only renderer; future Moment renderers extend this dispatch. */
@Composable
fun ChronicleReader(moments: List<ChronicleMomentUi>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        moments.sortedBy { it.position }.forEachIndexed { index, moment ->
            when (moment) {
                is ChronicleMomentUi.Text -> Text(
                    text = moment.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                is ChronicleMomentUi.Media -> ChronicleMediaMoment(moment.items)
                is ChronicleMomentUi.Audio -> ChronicleAudioMoment(
                    moment.displayName ?: stringResource(com.kingkharnivore.skillz.R.string.chronicle_audio),
                    moment.relativePath, moment.durationMs, moment.transcript, moment.transcriptEdited, moment.isAvailable)
                is ChronicleMomentUi.Voice -> ChronicleAudioMoment(
                    stringResource(com.kingkharnivore.skillz.R.string.chronicle_voice_note),
                    moment.relativePath, moment.durationMs, moment.transcript, moment.transcriptEdited, moment.isAvailable)
            }
            if (index < moments.lastIndex) HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = .22f)
            )
        }
    }
}
