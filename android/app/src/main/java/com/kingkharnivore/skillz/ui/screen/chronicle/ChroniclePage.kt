package com.kingkharnivore.skillz.ui.screen.chronicle

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.ChronicleMediaItemUi
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class PendingMicAction { DICTATION, VOICE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChroniclePage(holder: ChronicleStateHolder, modifier: Modifier = Modifier) {
    val state by holder.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var stopPlaybackSignal by remember { mutableIntStateOf(0) }
    var pendingMicAction by remember { mutableStateOf<PendingMicAction?>(null) }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) when (pendingMicAction) {
            PendingMicAction.DICTATION -> holder.startDictation()
            PendingMicAction.VOICE -> holder.startVoice()
            null -> Unit
        } else holder.microphonePermissionDenied()
        pendingMicAction = null
    }
    val invokeMic = { action: PendingMicAction ->
        stopPlaybackSignal++
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (action == PendingMicAction.DICTATION) holder.startDictation() else holder.startVoice()
        } else {
            pendingMicAction = action
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    var failedImports by remember { mutableIntStateOf(0) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        holder.importMedia(uris) { failedImports = it }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(holder::importAudio)
    }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingVideo by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingPhoto?.let(Uri::parse)?.let { uri ->
            if (success) holder.importMedia(listOf(uri)) { failedImports = it; holder.discardCapture(uri) }
            else holder.discardCapture(uri)
        }
        pendingPhoto = null
    }
    val videoCapture = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        pendingVideo?.let(Uri::parse)?.let { uri ->
            if (success) holder.importMedia(listOf(uri)) { failedImports = it; holder.discardCapture(uri) }
            else holder.discardCapture(uri)
        }
        pendingVideo = null
    }
    var deleteTarget by remember { mutableStateOf<ChronicleMomentUi?>(null) }
    val moveUpLabel = stringResource(R.string.chronicle_move_up)
    val moveDownLabel = stringResource(R.string.chronicle_move_down)
    val removeLabel = stringResource(R.string.chronicle_remove_action)
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val gestureScope = rememberCoroutineScope()
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    val displayedMoments = remember(state.contentMoments, state.stagedOrder) {
        if (state.stagedOrder.isEmpty()) state.contentMoments else {
            val byId = state.contentMoments.associateBy { it.id }
            state.stagedOrder.mapNotNull(byId::get)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (displayedMoments.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.chronicle_empty_title), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.chronicle_empty_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }
        itemsIndexed(displayedMoments, key = { _, item -> item.id }) { index, moment ->
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                val entity = state.moments.firstOrNull { it.id == moment.id }
                if (state.editingId == moment.id && entity != null) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField(value = state.editingField, onValueChange = holder::editText,
                                enabled = state.microphone !is ChronicleMicrophoneState.Dictating,
                                modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { deleteTarget = moment }) { Text(stringResource(R.string.chronicle_remove)) }
                                TextButton(onClick = holder::cancelEdit) { Text(stringResource(R.string.common_cancel)) }
                                Button(onClick = holder::finishEdit, enabled = state.editingText.isNotBlank()) {
                                    Text(stringResource(R.string.common_done))
                                }
                            }
                        }
                    }
                } else {
                    val isDragging = state.draggingId == moment.id
                    Surface(
                        color = if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = if (isDragging) 6.dp else 0.dp
                    ) { Box(Modifier.fillMaxWidth().semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(moveUpLabel) { holder.move(moment, -1); true },
                                CustomAccessibilityAction(moveDownLabel) { holder.move(moment, 1); true },
                                CustomAccessibilityAction(removeLabel) { deleteTarget = moment; true }
                            )
                        }.combinedClickable(onClick = { if (moment is ChronicleMomentUi.Text) entity?.let(holder::beginEdit) }, onLongClick = null)
                            .pointerInput(moment.id, state.editingId) {
                                if (state.editingId == null && entity != null) detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        holder.startDrag(moment)
                                        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == moment.id }
                                        dragPointerY = (item?.offset ?: 0) + offset.y
                                    },
                                    onDragEnd = holder::finishDrag,
                                    onDragCancel = holder::cancelDrag,
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragPointerY += amount.y
                                        val items = listState.layoutInfo.visibleItemsInfo
                                        val target = items.minByOrNull { item ->
                                            kotlin.math.abs(dragPointerY - (item.offset + item.size / 2f))
                                        }
                                        (target?.key as? String)?.let(holder::dragToId)
                                        val viewport = listState.layoutInfo
                                        val edge = 64f
                                        val scroll = when {
                                            dragPointerY < viewport.viewportStartOffset + edge -> -18f
                                            dragPointerY > viewport.viewportEndOffset - edge -> 18f
                                            else -> 0f
                                        }
                                        if (scroll != 0f) {
                                            gestureScope.launch { listState.scrollBy(scroll) }
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 8.dp)) {
                        when (moment) {
                            is ChronicleMomentUi.Text -> Text(moment.text, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
                            is ChronicleMomentUi.Media -> ChronicleMediaMoment(moment.items, stopPlaybackSignal)
                            is ChronicleMomentUi.Audio -> ChronicleAudioMoment(
                                label = moment.displayName ?: stringResource(R.string.chronicle_audio),
                                relativePath = moment.relativePath,
                                durationMs = moment.durationMs,
                                transcript = moment.transcript,
                                transcriptEdited = moment.transcriptEdited,
                                available = moment.isAvailable,
                                stopPlaybackSignal = stopPlaybackSignal
                            )
                            is ChronicleMomentUi.Voice -> ChronicleAudioMoment(
                                label = stringResource(R.string.chronicle_voice_note),
                                relativePath = moment.relativePath,
                                durationMs = moment.durationMs,
                                transcript = moment.transcript,
                                transcriptEdited = moment.transcriptEdited,
                                available = moment.isAvailable,
                                stopPlaybackSignal = stopPlaybackSignal
                            )
                        }
                    } }
                }
                if (index < displayedMoments.lastIndex) {
                    val destinationIndex = state.stagedOrder.indexOf(state.draggingId)
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(
                        alpha = if (destinationIndex == index && state.draggingId != null) .65f else .22f))
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = state.draftField, onValueChange = holder::setDraft,
                        enabled = state.editingId == null && !state.isCommitting &&
                            state.microphone !is ChronicleMicrophoneState.Dictating,
                        placeholder = { Text(stringResource(R.string.chronicle_write_placeholder)) },
                        modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            failedImports = 0
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }, modifier = Modifier.weight(1f),
                            enabled = !state.isCommitting && state.microphone is ChronicleMicrophoneState.Idle) {
                            Text(stringResource(R.string.chronicle_gallery)) }
                        TextButton(onClick = {
                            holder.createCaptureOutput(video = false)?.let { uri ->
                                pendingPhoto = uri.toString()
                                camera.launch(uri)
                            }
                        }, modifier = Modifier.weight(1f), enabled = !state.isCommitting &&
                            state.microphone is ChronicleMicrophoneState.Idle) {
                            Text(stringResource(R.string.chronicle_camera))
                        }
                        TextButton(onClick = {
                            holder.createCaptureOutput(video = true)?.let { uri ->
                                pendingVideo = uri.toString()
                                videoCapture.launch(uri)
                            }
                        }, modifier = Modifier.weight(1f), enabled = !state.isCommitting &&
                            state.microphone is ChronicleMicrophoneState.Idle) {
                            Text(stringResource(R.string.chronicle_video))
                        }
                    }
                    TextButton(onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth(), enabled = !state.isCommitting &&
                            state.microphone is ChronicleMicrophoneState.Idle) {
                        Text(stringResource(R.string.chronicle_choose_audio))
                    }
                    if (state.microphone is ChronicleMicrophoneState.Idle) {
                        val micArbiter = remember { MicGestureArbiter() }
                        val viewConfiguration = LocalViewConfiguration.current
                        val micHaptics = LocalHapticFeedback.current
                        val dictateLabel = stringResource(R.string.chronicle_start_dictation)
                        val voiceLabel = stringResource(R.string.chronicle_start_voice_recording)
                        TextButton(
                            onClick = { invokeMic(PendingMicAction.DICTATION) },
                            modifier = Modifier.fillMaxWidth().semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(dictateLabel) { invokeMic(PendingMicAction.DICTATION); true },
                                    CustomAccessibilityAction(voiceLabel) { invokeMic(PendingMicAction.VOICE); true }
                                )
                            }.pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    micArbiter.pointerDown()
                                    val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        waitForUpOrCancellation()
                                    }
                                    if (released != null) {
                                        if (micArbiter.pointerUp() == MicGestureArbiter.Result.START_DICTATION) {
                                            invokeMic(PendingMicAction.DICTATION)
                                        }
                                    } else if (micArbiter.longPressThreshold() == MicGestureArbiter.Result.START_VOICE) {
                                        micHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val permitted = ContextCompat.checkSelfPermission(context,
                                            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        stopPlaybackSignal++
                                        if (permitted) holder.startVoice() else {
                                            pendingMicAction = null
                                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        val up = waitForUpOrCancellation()
                                        if (permitted && up != null &&
                                            micArbiter.pointerUp() == MicGestureArbiter.Result.FINISH_VOICE) {
                                            holder.finishVoice()
                                        } else {
                                            micArbiter.cancel()
                                            holder.discardVoice()
                                        }
                                    }
                                }
                            },
                            enabled = !state.isCommitting
                        ) { Text(stringResource(R.string.chronicle_mic)) }
                    } else if (state.microphone is ChronicleMicrophoneState.RecordingVoice) {
                        val recording = state.microphone as ChronicleMicrophoneState.RecordingVoice
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.chronicle_recording_time, formatDuration(recording.elapsedMs)),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom) {
                                recording.amplitudes.forEach { amplitude ->
                                    Box(Modifier.weight(1f).height((4 + amplitude * 28).dp)
                                        .fillMaxWidth().padding(horizontal = .5.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = holder::discardVoice) {
                                    Text(stringResource(R.string.chronicle_discard_recording))
                                }
                                Button(onClick = holder::finishVoice) {
                                    Text(stringResource(R.string.chronicle_finish_recording))
                                }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.chronicle_listening), color = MaterialTheme.colorScheme.onBackground)
                            Row {
                                TextButton(onClick = { holder.cancelDictation() }) {
                                    Text(stringResource(R.string.chronicle_cancel_dictation))
                                }
                                Button(onClick = { holder.finishDictation() }) {
                                    Text(stringResource(R.string.chronicle_finish_dictation))
                                }
                            }
                        }
                    }
                    Button(onClick = { holder.add() }, enabled = state.draft.isNotBlank() && !state.isCommitting &&
                        state.microphone is ChronicleMicrophoneState.Idle,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = .25f),
                            disabledContentColor = MaterialTheme.colorScheme.onSecondary), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.chronicle_add))
                    }
                    if (state.hasError) Text(stringResource(R.string.chronicle_save_error), color = MaterialTheme.colorScheme.secondary)
                    if (state.speechUnavailable) Text(stringResource(R.string.chronicle_speech_unavailable),
                        color = MaterialTheme.colorScheme.onBackground)
                    if (state.microphoneUnavailable) Text(stringResource(R.string.chronicle_microphone_unavailable),
                        color = MaterialTheme.colorScheme.onBackground)
                    if (failedImports > 0) Text(stringResource(R.string.chronicle_media_failed, failedImports),
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
    deleteTarget?.let { moment ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.chronicle_remove_title), color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = { TextButton(onClick = { holder.delete(moment); holder.cancelEdit(); deleteTarget = null }) { Text(stringResource(R.string.chronicle_remove)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } })
    }
}

@Composable
internal fun ChronicleAudioMoment(
    label: String,
    relativePath: String?,
    durationMs: Long?,
    transcript: String?,
    transcriptEdited: Boolean,
    available: Boolean,
    stopPlaybackSignal: Int = 0
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(stopPlaybackSignal) { playing = false }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
        durationMs?.let { Text(formatDuration(it), color = MaterialTheme.colorScheme.onBackground) }
        if (available && relativePath != null) {
            TextButton(onClick = { playing = true }) { Text(stringResource(R.string.chronicle_play)) }
        } else {
            Text(stringResource(R.string.chronicle_audio_unavailable), color = MaterialTheme.colorScheme.onBackground)
        }
        transcript?.let {
            Text(if (transcriptEdited) stringResource(R.string.chronicle_transcript_edited) else
                stringResource(R.string.chronicle_transcript), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium)
            Text(it, color = MaterialTheme.colorScheme.onBackground)
        }
    }
    if (playing && relativePath != null) {
        val file = remember(relativePath) { File(context.filesDir, relativePath) }
        Dialog(onDismissRequest = { playing = false }) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, color = MaterialTheme.colorScheme.onBackground)
                    AndroidView(factory = { viewContext ->
                        VideoView(viewContext).apply {
                            val controls = MediaController(viewContext)
                            controls.setAnchorView(this)
                            setMediaController(controls)
                            setVideoPath(file.path)
                            setOnPreparedListener { start() }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(96.dp))
                    TextButton(onClick = { playing = false }) { Text(stringResource(R.string.chronicle_close)) }
                }
            }
        }
    }
}

@Composable
internal fun ChronicleMediaMoment(items: List<ChronicleMediaItemUi>, stopPlaybackSignal: Int = 0) {
    var viewerIndex by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(stopPlaybackSignal) { viewerIndex = -1 }
    val visible = items.take(4)
    val columns = if (visible.size == 1) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visible.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { item ->
                    val visibleIndex = visible.indexOf(item)
                    MediaThumbnail(item, Modifier.weight(1f),
                        overflow = (items.size - visible.size).takeIf { it > 0 && visibleIndex == visible.lastIndex }) {
                        viewerIndex = items.indexOf(item)
                    }
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (viewerIndex in items.indices) {
        ChronicleMediaViewer(items, viewerIndex, onIndexChanged = { viewerIndex = it }, onClose = { viewerIndex = -1 })
    }
}

@Composable
private fun MediaThumbnail(
    item: ChronicleMediaItemUi,
    modifier: Modifier,
    overflow: Int?,
    onOpen: (ChronicleMediaItemUi) -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.relativePath) {
        value = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, item.relativePath)
            ChronicleThumbnailLoader.load(file, item.mimeType)?.asImageBitmap()
        }
    }
    Surface(modifier.aspectRatio(if (item.position == 0) 1.35f else 1f).clickable { onOpen(item) },
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = .12f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap!!, contentDescription = stringResource(R.string.chronicle_open_media),
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(stringResource(R.string.chronicle_media_unavailable), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall)
            if (item.mimeType.startsWith("video/")) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.chronicle_video),
                    tint = MaterialTheme.colorScheme.onBackground)
                item.durationMs?.let { Text(formatDuration(it), color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) }
            }
            overflow?.let { Text("+$it", color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun ChronicleMediaViewer(
    items: List<ChronicleMediaItemUi>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val item = items[selectedIndex]
    val file = remember(item.relativePath) { File(context.filesDir, item.relativePath) }
    var activeVideoView by remember(item.id) { mutableStateOf<VideoView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(item.id, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) activeVideoView?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activeVideoView?.stopPlayback()
            activeVideoView = null
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!file.isFile) {
                    Text(stringResource(R.string.chronicle_media_unavailable), color = MaterialTheme.colorScheme.onBackground)
                } else if (item.mimeType.startsWith("video/")) {
                    AndroidView(
                        factory = { viewContext ->
                            VideoView(viewContext).apply {
                                activeVideoView = this
                                val controls = MediaController(viewContext)
                                controls.setAnchorView(this)
                                setMediaController(controls)
                                setVideoPath(file.path)
                                setOnPreparedListener { it.isLooping = false; start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file.path) {
                        value = ChronicleThumbnailLoader.load(file, item.mimeType)?.asImageBitmap()
                    }
                    image?.let { Image(it, contentDescription = stringResource(R.string.chronicle_open_media),
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                }
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.chronicle_close)) }
                    Text(stringResource(R.string.chronicle_media_position, selectedIndex + 1, items.size),
                        color = MaterialTheme.colorScheme.onBackground)
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onIndexChanged(selectedIndex - 1) }, enabled = selectedIndex > 0) {
                        Text(stringResource(R.string.chronicle_previous))
                    }
                    TextButton(onClick = { onIndexChanged(selectedIndex + 1) }, enabled = selectedIndex < items.lastIndex) {
                        Text(stringResource(R.string.chronicle_next))
                    }
                }
            }
        }
    }
}

@Composable private fun chronicleTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    disabledTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = MaterialTheme.colorScheme.secondary, unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .35f),
    disabledIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .20f),
    focusedPlaceholderColor = MaterialTheme.colorScheme.secondary, unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
    disabledPlaceholderColor = MaterialTheme.colorScheme.secondary.copy(alpha = .55f)
)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
