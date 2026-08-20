package com.kingkharnivore.skillz.ui.screen.chronicle

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.chronicle.ChroniclePlaybackController
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
    var requestedMicAction by remember { mutableStateOf<PendingMicAction?>(null) }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = requestedMicAction
        requestedMicAction = null
        if (granted && action != null) {
            if (action == PendingMicAction.DICTATION) holder.startDictation() else holder.startVoice()
        } else if (!granted) holder.microphonePermissionDenied()
    }
    val invokeMic = { action: PendingMicAction ->
        playback.stop()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (action == PendingMicAction.DICTATION) holder.startDictation() else holder.startVoice()
        } else {
            requestedMicAction = action
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    var failedImports by remember { mutableIntStateOf(0) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        holder.importMedia(uris) { failedImports = it }
    }
    var cameraMode by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedCameraMode by remember { mutableStateOf<ChronicleCameraMode?>(null) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val galleryWriteGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && galleryWriteGranted) cameraMode = requestedCameraMode?.name
        else cameraPermissionDenied = true
        requestedCameraMode = null
    }
    val openCamera = { mode: ChronicleCameraMode ->
        playback.stop()
        failedImports = 0
        cameraPermissionDenied = false
        val needsCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        val needsLegacyGalleryWrite = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (!needsCamera && !needsLegacyGalleryWrite) {
            cameraMode = mode.name
        } else {
            requestedCameraMode = mode
            cameraPermission.launch(buildList {
                if (needsCamera) add(Manifest.permission.CAMERA)
                if (needsLegacyGalleryWrite) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }.toTypedArray())
        }
    }
    var deleteTarget by remember { mutableStateOf<ChronicleMomentUi?>(null) }
    var showRemoveEditingMedia by remember { mutableStateOf(false) }
    val moveUpLabel = stringResource(R.string.chronicle_move_up)
    val moveDownLabel = stringResource(R.string.chronicle_move_down)
    val removeLabel = stringResource(R.string.chronicle_remove_action)
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val gestureScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val momentRemovedMessage = stringResource(R.string.chronicle_moment_removed)
    val undoLabel = stringResource(R.string.common_undo)
    var dragPointerY by remember { mutableFloatStateOf(0f) }
    val displayedMoments = remember(state.contentMoments, state.stagedOrder, state.pendingDeletion?.id) {
        val ordered = if (state.stagedOrder.isEmpty()) state.contentMoments else {
            val byId = state.contentMoments.associateBy { it.id }
            state.stagedOrder.mapNotNull(byId::get)
        }
        ordered.filterNot { it.id == state.pendingDeletion?.id }
    }
    val browseMediaItems = remember(displayedMoments) {
        displayedMoments.filterIsInstance<ChronicleMomentUi.Media>().flatMap { it.items }
    }
    LaunchedEffect(state.pendingDeletion?.id) {
        val pending = state.pendingDeletion ?: return@LaunchedEffect
        when (snackbarHostState.showSnackbar(
            message = momentRemovedMessage,
            actionLabel = undoLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )) {
            SnackbarResult.ActionPerformed -> holder.undoPendingDelete(pending.id)
            SnackbarResult.Dismissed -> holder.commitPendingDelete(pending.id)
        }
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().imePadding(),
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
                if (state.editingTranscriptId == moment.id && moment is ChronicleMomentUi.Voice) {
                    ChronicleTranscriptEditor(
                        value = state.editingTranscriptField,
                        onValueChange = holder::editTranscript,
                        onCancel = holder::cancelTranscriptEdit,
                        onDone = holder::finishTranscriptEdit,
                        enabled = !state.isCommitting,
                    )
                } else if (state.editingMediaId == moment.id && moment is ChronicleMomentUi.Media) {
                    ChronicleMediaEditor(
                        items = state.editingMediaItems,
                        onMove = holder::moveMediaItem,
                        onRemove = { id ->
                            if (state.editingMediaItems.size == 1) showRemoveEditingMedia = true
                            else holder.removeMediaItem(id)
                        },
                        onCancel = holder::cancelMediaEdit,
                        onDone = holder::finishMediaEdit,
                        enabled = !state.isCommitting,
                    )
                } else if (state.editingId == moment.id && entity != null) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField(value = state.editingField, onValueChange = holder::editText,
                                enabled = state.microphone !is ChronicleMicrophoneState.Dictating,
                                modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { deleteTarget = moment }, enabled = !state.isCommitting) {
                                Text(stringResource(R.string.chronicle_remove))
                            }
                                TextButton(onClick = holder::cancelEdit, enabled = !state.isCommitting) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                                Button(onClick = holder::finishEdit,
                                    enabled = state.editingText.isNotBlank() && !state.isCommitting) {
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
                            customActions = if (state.pendingDeletion == null) listOf(
                                CustomAccessibilityAction(moveUpLabel) { holder.move(moment, -1); true },
                                CustomAccessibilityAction(moveDownLabel) { holder.move(moment, 1); true },
                                CustomAccessibilityAction(removeLabel) { deleteTarget = moment; true }
                            ) else emptyList()
                        }.combinedClickable(onClick = { if (moment is ChronicleMomentUi.Text) entity?.let(holder::beginEdit) }, onLongClick = null)
                            .pointerInput(moment.id, state.editingId, state.pendingDeletion?.id) {
                                if (state.editingId == null && state.pendingDeletion == null && entity != null) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            holder.startDrag(moment)
                                            val item = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == moment.id }
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
                            }
                            .padding(vertical = 8.dp)) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.DragHandle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .38f),
                                    modifier = Modifier.size(20.dp),
                                )
                                MomentActionMenu(
                                    enabled = state.pendingDeletion == null,
                                    canMoveEarlier = index > 0,
                                    canMoveLater = index < displayedMoments.lastIndex,
                                    editLabel = when (moment) {
                                        is ChronicleMomentUi.Text -> stringResource(R.string.chronicle_edit_text_moment)
                                        is ChronicleMomentUi.Media -> stringResource(R.string.chronicle_edit_media)
                                        is ChronicleMomentUi.Voice -> moment.transcript?.let {
                                            stringResource(R.string.chronicle_edit_transcript)
                                        }
                                    },
                                    onEdit = when (moment) {
                                        is ChronicleMomentUi.Text -> entity?.let { { holder.beginEdit(it) } }
                                        is ChronicleMomentUi.Media -> { { holder.beginMediaEdit(moment) } }
                                        is ChronicleMomentUi.Voice -> moment.transcript?.let {
                                            { holder.beginTranscriptEdit(moment) }
                                        }
                                    },
                                    onMoveEarlier = { holder.move(moment, -1) },
                                    onMoveLater = { holder.move(moment, 1) },
                                    onRemove = { deleteTarget = moment },
                                )
                            }
                            when (moment) {
                                is ChronicleMomentUi.Text -> Text(moment.text, style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
                                is ChronicleMomentUi.Media -> ChronicleMediaMoment(
                                    items = moment.items,
                                    playback = playback,
                                    browseItems = browseMediaItems,
                                )
                                is ChronicleMomentUi.Voice -> ChronicleAudioMoment(
                                    sourceId = moment.id,
                                    label = stringResource(R.string.chronicle_voice_note),
                                    relativePath = moment.relativePath,
                                    durationMs = moment.durationMs,
                                    originalTranscript = moment.originalTranscript,
                                    transcript = moment.transcript,
                                    transcriptEdited = moment.transcriptEdited,
                                    available = moment.isAvailable,
                                    playback = playback,
                                    transcriptionState = state.transcriptions[moment.id],
                                    showTranscriptionControls = true,
                                    transcriptionSupported = state.transcriptionSupported,
                                    onTranscribe = { holder.startTranscription(moment) },
                                    onCancelTranscription = { holder.cancelTranscription(moment.id) },
                                    onEditTranscript = { holder.beginTranscriptEdit(moment) },
                                )
                            }
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
                    val captureEnabled = !state.isCommitting &&
                        state.microphone is ChronicleMicrophoneState.Idle &&
                        state.editingId == null && state.editingTranscriptId == null
                    val micEnabled = !state.isCommitting &&
                        state.microphone is ChronicleMicrophoneState.Idle &&
                        state.editingMediaId == null && state.editingTranscriptId == null
                    val micGestureVisible = micEnabled ||
                        state.microphone is ChronicleMicrophoneState.RecordingVoice
                    TextField(value = state.draftField, onValueChange = holder::setDraft,
                        enabled = !state.hasActiveEditor && !state.isCommitting &&
                            state.microphone !is ChronicleMicrophoneState.Dictating,
                        placeholder = { Text(stringResource(R.string.chronicle_write_placeholder)) },
                        modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            failedImports = 0
                            gallery.launch(arrayOf("image/*", "video/*"))
                        }, enabled = captureEnabled) {
                            Icon(Icons.Outlined.Collections, stringResource(R.string.chronicle_gallery))
                        }
                        IconButton(onClick = { openCamera(ChronicleCameraMode.PHOTO) }, enabled = captureEnabled) {
                            Icon(Icons.Outlined.PhotoCamera, stringResource(R.string.chronicle_camera))
                        }
                        IconButton(onClick = { openCamera(ChronicleCameraMode.VIDEO) }, enabled = captureEnabled) {
                            Icon(Icons.Outlined.Videocam, stringResource(R.string.chronicle_video))
                        }
                        if (micGestureVisible) {
                            val micArbiter = remember { MicGestureArbiter() }
                            val viewConfiguration = LocalViewConfiguration.current
                            val micHaptics = LocalHapticFeedback.current
                            val dictateLabel = stringResource(R.string.chronicle_start_dictation)
                            val voiceLabel = stringResource(R.string.chronicle_start_voice_recording)
                            Box(
                                modifier = Modifier.size(48.dp).semantics {
                                    role = Role.Button
                                    contentDescription = dictateLabel
                                    onClick(label = dictateLabel) { invokeMic(PendingMicAction.DICTATION); true }
                                customActions = listOf(
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
                                        playback.stop()
                                        if (permitted) holder.startVoice() else {
                                            requestedMicAction = PendingMicAction.VOICE
                                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        val up = waitForUpOrCancellation()
                                        if (permitted) {
                                            if (up != null &&
                                                micArbiter.pointerUp() == MicGestureArbiter.Result.FINISH_VOICE) {
                                                holder.finishVoice()
                                            } else {
                                                micArbiter.cancel()
                                                holder.discardVoice()
                                            }
                                        } else {
                                            micArbiter.cancel()
                                        }
                                    }
                                }
                            },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.MicNone, contentDescription = null)
                            }
                        } else if (state.microphone is ChronicleMicrophoneState.Idle) {
                            IconButton(onClick = {}, enabled = false) {
                                Icon(Icons.Outlined.MicNone, stringResource(R.string.chronicle_start_dictation))
                            }
                        }
                    }
                    if (state.microphone is ChronicleMicrophoneState.RecordingVoice) {
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
                    } else if (state.microphone is ChronicleMicrophoneState.Dictating) {
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
                    Button(onClick = { holder.add() }, enabled = state.draft.isNotBlank() && !state.hasActiveEditor &&
                        !state.isCommitting &&
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
                    if (cameraPermissionDenied) Text(stringResource(R.string.chronicle_camera_permission_denied),
                        color = MaterialTheme.colorScheme.onBackground)
                    if (failedImports > 0) Text(stringResource(R.string.chronicle_media_failed, failedImports),
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        )
    }
    deleteTarget?.let { moment ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.chronicle_remove_title), color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = { TextButton(onClick = {
                holder.cancelEdit()
                holder.requestDelete(moment)
                deleteTarget = null
            }) { Text(stringResource(R.string.chronicle_remove)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } })
    }
    if (showRemoveEditingMedia) {
        AlertDialog(
            onDismissRequest = { showRemoveEditingMedia = false },
            title = { Text(stringResource(R.string.chronicle_remove_media_moment_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveEditingMedia = false
                    val moment = state.contentMoments.firstOrNull { it.id == state.editingMediaId }
                    holder.cancelMediaEdit()
                    if (moment != null) holder.requestDelete(moment)
                }) { Text(stringResource(R.string.chronicle_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveEditingMedia = false }) {
                    Text(stringResource(R.string.chronicle_keep_editing))
                }
            },
        )
    }
    cameraMode?.let { savedMode ->
        ChronicleCamera(
            initialMode = ChronicleCameraMode.valueOf(savedMode),
            createOutput = holder::createCameraStagingFile,
            onCaptured = { uri, complete ->
                holder.importMedia(listOf(uri)) {
                    failedImports = it
                    if (it == 0) holder.discardCapture(uri)
                    complete(it == 0)
                }
            },
            onClose = { cameraMode = null },
        )
    }
}

@Composable
private fun MomentActionMenu(
    enabled: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    editLabel: String?,
    onEdit: (() -> Unit)?,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.chronicle_moment_actions))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (editLabel != null && onEdit != null) {
                DropdownMenuItem(
                    text = { Text(editLabel) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = { expanded = false; onEdit() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chronicle_move_up)) },
                leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null) },
                enabled = canMoveEarlier,
                onClick = { expanded = false; onMoveEarlier() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chronicle_move_down)) },
                leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                enabled = canMoveLater,
                onClick = { expanded = false; onMoveLater() },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chronicle_remove_action)) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = { expanded = false; onRemove() },
            )
        }
    }
}

@Composable
internal fun ChronicleAudioMoment(
    sourceId: String,
    label: String,
    relativePath: String?,
    durationMs: Long?,
    originalTranscript: String?,
    transcript: String?,
    transcriptEdited: Boolean,
    available: Boolean,
    playback: ChroniclePlaybackController,
    transcriptionState: ChronicleTranscriptionState? = null,
    showTranscriptionControls: Boolean = false,
    transcriptionSupported: Boolean = false,
    onTranscribe: (() -> Unit)? = null,
    onCancelTranscription: (() -> Unit)? = null,
    onEditTranscript: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val isActive = playbackState.activeSourceId == sourceId
    val resolvedDuration = if (isActive && playbackState.durationMs > 0L) playbackState.durationMs else durationMs ?: 0L
    val resolvedPosition = if (isActive) playbackState.positionMs.coerceAtMost(resolvedDuration.coerceAtLeast(0L)) else 0L
    val file = remember(relativePath) { relativePath?.let { File(context.filesDir, it) } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
        if (available && file?.isFile == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playback.toggle(sourceId, file) }) {
                    Icon(
                        if (isActive && playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            if (isActive && playbackState.isPlaying) R.string.chronicle_pause else R.string.chronicle_play
                        ),
                    )
                }
                Slider(
                    value = resolvedPosition.toFloat(),
                    onValueChange = { playback.seekTo(sourceId, it.toLong()) },
                    valueRange = 0f..resolvedDuration.coerceAtLeast(1L).toFloat(),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "${formatDuration(resolvedPosition)} / ${formatDuration(resolvedDuration)}",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
            )
            if (isActive && playbackState.hasError) {
                Text(stringResource(R.string.chronicle_playback_failed), color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Text(stringResource(R.string.chronicle_voice_unavailable), color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            stringResource(R.string.chronicle_original_transcript),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
        )
        val originalDisplay = when (transcriptionState) {
            is ChronicleTranscriptionState.Transcribing ->
                transcriptionState.partial.takeIf(String::isNotBlank) ?: originalTranscript
            else -> originalTranscript
        }
        originalDisplay?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }
        when (transcriptionState) {
            is ChronicleTranscriptionState.Transcribing -> {
                Text(stringResource(R.string.chronicle_transcribing), color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { onCancelTranscription?.invoke() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
            ChronicleTranscriptionState.Failed -> {
                Text(stringResource(R.string.chronicle_transcription_failed), color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { onTranscribe?.invoke() }) { Text(stringResource(R.string.common_retry)) }
            }
            null -> if (showTranscriptionControls && originalTranscript == null) {
                if (transcriptionSupported) {
                    TextButton(onClick = { onTranscribe?.invoke() }) {
                        Text(stringResource(R.string.chronicle_transcribe))
                    }
                } else {
                    Text(stringResource(R.string.chronicle_transcription_unavailable), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        if (transcriptEdited) {
            Text(stringResource(R.string.chronicle_edited_transcript), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium)
            transcript?.let { Text(it, color = MaterialTheme.colorScheme.onBackground) }
            if (showTranscriptionControls && onEditTranscript != null) {
                TextButton(onClick = onEditTranscript) { Text(stringResource(R.string.chronicle_edit_transcript)) }
            }
        } else if (originalTranscript != null && showTranscriptionControls && onEditTranscript != null) {
            TextButton(onClick = onEditTranscript) { Text(stringResource(R.string.chronicle_edit_transcript)) }
        }
    }
}

@Composable
private fun ChronicleTranscriptEditor(
    value: androidx.compose.ui.text.input.TextFieldValue,
    onValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    enabled: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.chronicle_edit_transcript), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium)
            TextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = chronicleTextFieldColors(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = enabled) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = onDone, enabled = enabled) { Text(stringResource(R.string.common_done)) }
            }
        }
    }
}

@Composable
internal fun ChronicleMediaMoment(
    items: List<ChronicleMediaItemUi>,
    playback: ChroniclePlaybackController,
    browseItems: List<ChronicleMediaItemUi> = items,
) {
    var viewerIndex by rememberSaveable { mutableIntStateOf(-1) }
    val visible = items.take(4)
    val columns = if (visible.size == 1) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visible.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { item ->
                    val visibleIndex = visible.indexOf(item)
                    MediaThumbnail(item, Modifier.weight(1f),
                        overflow = (items.size - visible.size).takeIf { it > 0 && visibleIndex == visible.lastIndex }) {
                        viewerIndex = browseItems.indexOfFirst { candidate -> candidate.id == item.id }
                    }
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (viewerIndex in browseItems.indices) {
        ChronicleMediaViewer(
            browseItems,
            viewerIndex,
            playback = playback,
            onIndexChanged = { viewerIndex = it },
            onClose = { playback.stop(); viewerIndex = -1 },
        )
    }
}

@Composable
private fun ChronicleMediaEditor(
    items: List<com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity>,
    onMove: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    enabled: Boolean,
) {
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.chronicle_editing_media),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val ui = ChronicleMediaItemUi(
                        id = item.id,
                        position = item.position,
                        relativePath = item.localPath,
                        mimeType = item.mimeType,
                        durationMs = item.durationMs,
                        width = item.width,
                        height = item.height,
                        isAvailable = File(context.filesDir, item.localPath).isFile,
                        createdAt = item.createdAt,
                    )
                    Box(Modifier.width(176.dp)) {
                        MediaThumbnail(
                            item = ui,
                            modifier = Modifier.fillMaxWidth(),
                            overflow = null,
                            aspectRatio = 1f,
                            onOpen = null,
                        )
                        FilledTonalIconButton(
                            onClick = { onRemove(item.id) },
                            enabled = enabled,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                stringResource(
                                    if (item.mimeType.startsWith("video/")) R.string.chronicle_remove_video
                                    else R.string.chronicle_remove_photo
                                ),
                            )
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onMove(item.id, -1) }, enabled = enabled && index > 0) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                        stringResource(R.string.chronicle_move_media_earlier))
                                }
                                Text(
                                    stringResource(R.string.chronicle_media_position, index + 1, items.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(onClick = { onMove(item.id, 1) },
                                    enabled = enabled && index < items.lastIndex) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        stringResource(R.string.chronicle_move_media_later))
                                }
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.chronicle_media_reorder_hint),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.chronicle_media_edit_capture_hint),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = enabled) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = onDone, enabled = enabled && items.isNotEmpty()) { Text(stringResource(R.string.common_done)) }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(
    item: ChronicleMediaItemUi,
    modifier: Modifier,
    overflow: Int?,
    aspectRatio: Float = if (item.position == 0) 1.35f else 1f,
    onOpen: ((ChronicleMediaItemUi) -> Unit)?,
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.relativePath) {
        value = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, item.relativePath)
            ChronicleThumbnailLoader.load(file, item.mimeType)?.asImageBitmap()
        }
    }
    val interactionModifier = if (onOpen == null) modifier else modifier.clickable { onOpen(item) }
    Surface(interactionModifier.aspectRatio(aspectRatio),
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
    playback: ChroniclePlaybackController,
    onIndexChanged: (Int) -> Unit,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = selectedIndex, pageCount = { items.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.settledPage) {
        onIndexChanged(pagerState.settledPage)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> items[page].id },
                    userScrollEnabled = items.size > 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ChronicleMediaViewerPage(
                        item = items[page],
                        playback = playback,
                        active = page == pagerState.settledPage,
                    )
                }
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.chronicle_close)) }
                    Text(stringResource(R.string.chronicle_media_position, pagerState.currentPage + 1, items.size),
                        color = MaterialTheme.colorScheme.onBackground)
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledTonalIconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        enabled = pagerState.currentPage > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.chronicle_previous))
                    }
                    if (items.size > 1) {
                        Text(
                            stringResource(R.string.chronicle_swipe_media_hint),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        enabled = pagerState.currentPage < items.lastIndex,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.chronicle_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChronicleMediaViewerPage(
    item: ChronicleMediaItemUi,
    playback: ChroniclePlaybackController,
    active: Boolean,
) {
    val context = LocalContext.current
    val file = remember(item.relativePath) { File(context.filesDir, item.relativePath) }
    var videoTexture by remember(item.id) { mutableStateOf<TextureView?>(null) }
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val isActiveSource = playbackState.activeSourceId == item.id
    val resolvedDuration = if (isActiveSource && playbackState.durationMs > 0L) {
        playbackState.durationMs
    } else {
        item.durationMs ?: 0L
    }
    val resolvedPosition = if (isActiveSource) {
        playbackState.positionMs.coerceAtMost(resolvedDuration.coerceAtLeast(0L))
    } else {
        0L
    }
    LaunchedEffect(active, item.id, file.path, item.mimeType) {
        if (active && item.mimeType.startsWith("video/") && file.isFile) {
            playback.prepare(item.id, file, item.mimeType)
        }
        else if (active) playback.stop()
    }
    DisposableEffect(active, item.id, playback, videoTexture) {
        videoTexture?.takeIf { active }?.let(playback::bindVideo)
        onDispose {
            videoTexture?.let(playback::unbindVideo)
            if (active) playback.stop()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!file.isFile) {
            Text(stringResource(R.string.chronicle_media_unavailable), color = MaterialTheme.colorScheme.onBackground)
        } else if (item.mimeType.startsWith("video/") && active) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { viewContext ->
                        TextureView(viewContext).also { view ->
                            videoTexture = view
                        }
                    },
                    update = { view ->
                        videoTexture = view
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 80.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        if (isActiveSource && playbackState.hasError) {
                            Text(
                                stringResource(R.string.chronicle_video_playback_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { playback.toggle(item.id, file, item.mimeType) },
                            ) {
                                Icon(
                                    if (isActiveSource && playbackState.isPlaying) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = stringResource(
                                        if (isActiveSource && playbackState.isPlaying) {
                                            R.string.chronicle_pause
                                        } else {
                                            R.string.chronicle_play
                                        }
                                    ),
                                )
                            }
                            Slider(
                                value = resolvedPosition.toFloat(),
                                onValueChange = { playback.seekTo(item.id, it.toLong()) },
                                valueRange = 0f..resolvedDuration.coerceAtLeast(1L).toFloat(),
                                enabled = isActiveSource && resolvedDuration > 0L,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${formatDuration(resolvedPosition)} / ${formatDuration(resolvedDuration)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        } else {
            val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file.path) {
                value = withContext(Dispatchers.IO) {
                    ChronicleThumbnailLoader.load(file, item.mimeType)?.asImageBitmap()
                }
            }
            image?.let {
                Image(
                    it,
                    contentDescription = stringResource(R.string.chronicle_open_media),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
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
