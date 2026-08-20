package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi
import com.kingkharnivore.skillz.data.chronicle.ChroniclePlaybackController

/** One-owner, ordered, read-only renderer; future Moment renderers extend this dispatch. */
@Composable
fun ChronicleReader(moments: List<ChronicleMomentUi>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playback = remember(context) { ChroniclePlaybackController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(playback, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playback.release()
        }
    }
    val orderedMoments = remember(moments) { moments.sortedBy { it.position } }
    val browseMediaItems = remember(orderedMoments) {
        orderedMoments.filterIsInstance<ChronicleMomentUi.Media>().flatMap { it.items }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        orderedMoments.forEachIndexed { index, moment ->
            when (moment) {
                is ChronicleMomentUi.Text -> Text(
                    text = moment.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                is ChronicleMomentUi.Media -> ChronicleMediaMoment(
                    items = moment.items,
                    playback = playback,
                    browseItems = browseMediaItems,
                )
                is ChronicleMomentUi.Voice -> ChronicleAudioMoment(
                    sourceId = moment.id,
                    label = stringResource(com.kingkharnivore.skillz.R.string.chronicle_voice_note),
                    relativePath = moment.relativePath,
                    durationMs = moment.durationMs,
                    originalTranscript = moment.originalTranscript,
                    transcript = moment.transcript,
                    transcriptEdited = moment.transcriptEdited,
                    available = moment.isAvailable,
                    playback = playback,
                )
            }
            if (index < orderedMoments.lastIndex) HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = .22f)
            )
        }
    }
}
