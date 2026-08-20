package com.kingkharnivore.skillz.ui.screen.chronicle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.chronicle.ChronicleImageTransform
import com.kingkharnivore.skillz.data.chronicle.normalizeChronicleQuarterTurns
import com.kingkharnivore.skillz.data.chronicle.normalizeChronicleRotationDegrees
import com.kingkharnivore.skillz.data.chronicle.persistChronicleQuarterTurns
import com.kingkharnivore.skillz.data.chronicle.readChronicleImageTransform
import com.kingkharnivore.skillz.data.chronicle.transformChronicleBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ChronicleCameraMode { PHOTO, VIDEO }

private data class PendingCapture(
    val file: File,
    val mode: ChronicleCameraMode,
    val rotationQuarterTurns: Int = 0,
)

/** One Scyra-owned CameraX surface with explicit review and save semantics. */
@Composable
fun ChronicleCamera(
    initialMode: ChronicleCameraMode,
    createOutput: (video: Boolean) -> File?,
    onCaptured: (Uri, (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCaptured by rememberUpdatedState(onCaptured)
    val currentClose by rememberUpdatedState(onClose)
    val cameraScope = rememberCoroutineScope()
    var modeName by rememberSaveable { mutableStateOf(initialMode.name) }
    val mode = ChronicleCameraMode.valueOf(modeName)
    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var bindAttempt by remember { mutableIntStateOf(0) }
    var cameraReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var discardRecording by remember { mutableStateOf(false) }
    var closeAfterFinalize by remember { mutableStateOf(false) }
    var photoInFlight by remember { mutableStateOf(false) }
    var acceptingResults by remember { mutableStateOf(true) }
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }
    var savingCapture by remember { mutableStateOf(false) }
    var captureHandedOff by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(Recorder.Builder().build()) }

    val latestRecording by rememberUpdatedState(activeRecording)
    val latestRecordingFile by rememberUpdatedState(recordingFile)
    val latestPendingCapture by rememberUpdatedState(pendingCapture)
    val latestCaptureHandedOff by rememberUpdatedState(captureHandedOff)

    // Bind only the active capture use case. Binding preview, photo, and video at
    // once exceeds the stream combinations supported by some physical cameras.
    DisposableEffect(lifecycleOwner, lensFacing, mode, bindAttempt, previewView) {
        cameraReady = false
        error = false
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        future.addListener({
            if (disposed || !acceptingResults) return@addListener
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                provider.unbindAll()
                if (mode == ChronicleCameraMode.PHOTO) {
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                } else {
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
                }
                cameraReady = true
            }.onFailure {
                cameraReady = false
                if (bindAttempt < MAX_AUTO_BIND_RETRIES) {
                    previewView.postDelayed({
                        if (!disposed && acceptingResults) bindAttempt++
                    }, CAMERA_RETRY_DELAY_MS)
                } else {
                    error = true
                }
            }
        }, executor)
        onDispose {
            disposed = true
            cameraReady = false
            if (future.isDone) runCatching { future.get().unbindAll() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            acceptingResults = false
            latestPendingCapture?.file?.takeUnless { latestCaptureHandedOff }?.delete()
            if (latestRecording != null) {
                discardRecording = true
                closeAfterFinalize = false
                latestRecording?.stop()
            } else {
                latestRecordingFile?.delete()
            }
        }
    }

    LaunchedEffect(activeRecording, recordingStartedAt) {
        while (activeRecording != null) {
            elapsedMs = System.currentTimeMillis() - recordingStartedAt
            delay(200L)
        }
    }

    fun closeCamera() {
        if (savingCapture) return
        if (activeRecording != null) {
            discardRecording = true
            closeAfterFinalize = true
            activeRecording?.stop()
        } else {
            pendingCapture?.file?.delete()
            pendingCapture = null
            acceptingResults = false
            currentClose()
        }
    }

    fun takePhoto() {
        if (photoInFlight || !acceptingResults || !cameraReady || pendingCapture != null) return
        val output = createOutput(false) ?: run { error = true; return }
        photoInFlight = true
        error = false
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(output).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    photoInFlight = false
                    if (acceptingResults && output.isFile && output.length() > 0L) {
                        pendingCapture = PendingCapture(output, ChronicleCameraMode.PHOTO)
                    } else {
                        output.delete()
                        if (acceptingResults) error = true
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    photoInFlight = false
                    output.delete()
                    if (acceptingResults) {
                        error = true
                        bindAttempt++
                    }
                }
            },
        )
    }

    fun toggleVideo() {
        activeRecording?.let { recording ->
            recording.stop()
            return
        }
        if (!cameraReady || pendingCapture != null || !acceptingResults) return
        val output = createOutput(true) ?: run { error = true; return }
        recordingFile = output
        discardRecording = false
        closeAfterFinalize = false
        error = false
        val pending = videoCapture.output.prepareRecording(
            context,
            FileOutputOptions.Builder(output).build(),
        )
        val withAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val prepared = if (withAudio) pending.withAudioEnabled() else pending
        recordingStartedAt = System.currentTimeMillis()
        elapsedMs = 0L
        activeRecording = prepared.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> Unit
                is VideoRecordEvent.Finalize -> {
                    val file = recordingFile
                    activeRecording = null
                    recordingFile = null
                    val valid = !event.hasError() && file?.isFile == true && file.length() > 0L
                    if (discardRecording || !acceptingResults || !valid) {
                        file?.delete()
                        if (!discardRecording && acceptingResults) {
                            error = true
                            bindAttempt++
                        }
                    } else {
                        pendingCapture = PendingCapture(checkNotNull(file), ChronicleCameraMode.VIDEO)
                    }
                    discardRecording = false
                    if (closeAfterFinalize) {
                        closeAfterFinalize = false
                        acceptingResults = false
                        currentClose()
                    }
                }
                else -> Unit
            }
        }
    }

    fun retake() {
        pendingCapture?.file?.delete()
        pendingCapture = null
        savingCapture = false
        error = false
    }

    fun rotatePhoto(quarterTurnDelta: Int) {
        val capture = pendingCapture ?: return
        if (capture.mode != ChronicleCameraMode.PHOTO || savingCapture) return
        pendingCapture = capture.copy(
            rotationQuarterTurns = normalizeChronicleQuarterTurns(
                capture.rotationQuarterTurns + quarterTurnDelta
            )
        )
        error = false
    }

    fun saveCapture() {
        val capture = pendingCapture ?: return
        if (savingCapture) return
        savingCapture = true
        error = false
        cameraScope.launch {
            val readyCapture = if (
                capture.mode == ChronicleCameraMode.PHOTO && capture.rotationQuarterTurns != 0
            ) {
                val rotated = runCatching {
                    withContext(Dispatchers.IO) {
                        persistChronicleQuarterTurns(capture.file, capture.rotationQuarterTurns)
                    }
                }.isSuccess
                if (!rotated) {
                    savingCapture = false
                    error = true
                    return@launch
                }
                capture.copy(rotationQuarterTurns = 0).also { pendingCapture = it }
            } else {
                capture
            }
            currentCaptured(Uri.fromFile(readyCapture.file)) { saved ->
                cameraScope.launch {
                    if (saved) {
                        captureHandedOff = true
                        pendingCapture = null
                        savingCapture = false
                        acceptingResults = false
                        currentClose()
                    } else {
                        savingCapture = false
                        error = true
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = ::closeCamera,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                val review = pendingCapture
                if (review == null) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                } else {
                    ChronicleCaptureReview(review, context)
                }

                if (review != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = .68f))
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(
                                if (review.mode == ChronicleCameraMode.VIDEO) {
                                    R.string.chronicle_video_ready
                                } else {
                                    R.string.chronicle_photo_ready
                                }
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (review.mode == ChronicleCameraMode.PHOTO) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { rotatePhoto(-1) },
                                    enabled = !savingCapture,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Outlined.RotateLeft,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.chronicle_rotate_left))
                                }
                                OutlinedButton(
                                    onClick = { rotatePhoto(1) },
                                    enabled = !savingCapture,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Outlined.RotateRight,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.chronicle_rotate_right))
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = ::retake,
                                enabled = !savingCapture,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.chronicle_retake))
                            }
                            Button(
                                onClick = ::saveCapture,
                                enabled = !savingCapture,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    stringResource(
                                        if (savingCapture) R.string.chronicle_saving_capture
                                        else R.string.chronicle_save_capture
                                    )
                                )
                            }
                        }
                        if (error) {
                            Text(stringResource(R.string.chronicle_capture_save_failed), color = Color.White)
                        }
                    }
                } else {
                    ChronicleCameraControls(
                        mode = mode,
                        cameraReady = cameraReady,
                        error = error,
                        photoInFlight = photoInFlight,
                        activeRecording = activeRecording,
                        elapsedMs = elapsedMs,
                        onModeChanged = { modeName = it.name },
                        onFlip = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        onCapture = { if (mode == ChronicleCameraMode.PHOTO) takePhoto() else toggleVideo() },
                        onClose = ::closeCamera,
                        onRetry = { error = false; bindAttempt++ },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChronicleCameraControls(
    mode: ChronicleCameraMode,
    cameraReady: Boolean,
    error: Boolean,
    photoInFlight: Boolean,
    activeRecording: Recording?,
    elapsedMs: Long,
    onModeChanged: (ChronicleCameraMode) -> Unit,
    onFlip: () -> Unit,
    onCapture: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .58f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (activeRecording != null) {
            Text(
                stringResource(R.string.chronicle_camera_recording, formatChronicleDuration(elapsedMs)),
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .12f)) {
            Row(Modifier.padding(3.dp)) {
                ChronicleCameraMode.entries.forEach { candidate ->
                    TextButton(
                        onClick = { onModeChanged(candidate) },
                        enabled = activeRecording == null && !photoInFlight,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (mode == candidate) Color.Black else Color.White,
                            containerColor = if (mode == candidate) Color.White else Color.Transparent,
                        ),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text(
                            stringResource(
                                if (candidate == ChronicleCameraMode.PHOTO) R.string.chronicle_photo
                                else R.string.chronicle_video
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onFlip,
                enabled = cameraReady && activeRecording == null && !photoInFlight,
            ) {
                Icon(
                    Icons.Outlined.Cameraswitch,
                    stringResource(R.string.chronicle_flip_camera),
                    tint = Color.White,
                )
            }
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = if (activeRecording != null) MaterialTheme.colorScheme.secondary else Color.White,
                onClick = onCapture,
                enabled = cameraReady && !error && !photoInFlight,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!cameraReady && !error) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Color.Black)
                    } else {
                        Icon(
                            imageVector = when {
                                activeRecording != null -> Icons.Default.Stop
                                mode == ChronicleCameraMode.PHOTO -> Icons.Outlined.PhotoCamera
                                else -> Icons.Outlined.Videocam
                            },
                            contentDescription = stringResource(
                                when {
                                    activeRecording != null -> R.string.chronicle_stop_video
                                    mode == ChronicleCameraMode.PHOTO -> R.string.chronicle_take_photo
                                    else -> R.string.chronicle_start_video
                                }
                            ),
                            tint = if (activeRecording != null) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                Color.Black
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.chronicle_close_camera),
                    tint = Color.White,
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.chronicle_camera_retry), color = Color.White)
            }
        }
    }
}

@Composable
private fun ChronicleCaptureReview(capture: PendingCapture, context: android.content.Context) {
    if (capture.mode == ChronicleCameraMode.PHOTO) {
        val bitmap = remember(
            capture.file.path,
            capture.file.lastModified(),
            capture.rotationQuarterTurns,
        ) {
            decodeChroniclePreview(capture.file, capture.rotationQuarterTurns)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        val videoView = remember(capture.file.path) {
            VideoView(context).apply {
                setVideoPath(capture.file.path)
                setOnPreparedListener { player ->
                    player.isLooping = true
                    start()
                }
            }
        }
        DisposableEffect(videoView) {
            videoView.start()
            onDispose { videoView.stopPlayback() }
        }
        AndroidView(factory = { videoView }, modifier = Modifier.fillMaxSize())
    }
}

private fun decodeChroniclePreview(
    file: File,
    rotationQuarterTurns: Int,
): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_REVIEW_EDGE_PX ||
        bounds.outHeight / sampleSize > MAX_REVIEW_EDGE_PX
    ) {
        sampleSize *= 2
    }
    val bitmap = BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null
    val current = readChronicleImageTransform(file)
    return transformChronicleBitmap(
        bitmap,
        ChronicleImageTransform(
            rotationDegrees = normalizeChronicleRotationDegrees(
                current.rotationDegrees + rotationQuarterTurns * 90
            ),
            flippedHorizontally = current.flippedHorizontally,
        )
    )
}

internal fun formatChronicleDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private const val MAX_AUTO_BIND_RETRIES = 2
private const val CAMERA_RETRY_DELAY_MS = 350L
private const val MAX_REVIEW_EDGE_PX = 1_440
