package com.kingkharnivore.skillz.ui.screen.shell.rooms.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.isTraceInProgress
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun FocusRoomScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedExercise by remember { mutableStateOf<FocusGuidedExercise?>(null) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var elapsedInCurrentStep by remember { mutableIntStateOf(0) }
    var totalElapsedSeconds by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    var voiceEnabled by rememberSaveable { mutableStateOf(true) }
    var voiceReady by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }

    val voiceGuide = remember {
        FocusExerciseVoiceGuide(
            context = context.applicationContext,
            onReadyChanged = { ready ->
                voiceReady = ready
            },
            onError = { message ->
                voiceMessage = message
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceGuide.shutdown()
        }
    }

    val currentStep = selectedExercise?.steps?.getOrNull(currentStepIndex)

    fun clearPlayer() {
        voiceGuide.stop()
        selectedExercise = null
        currentStepIndex = 0
        elapsedInCurrentStep = 0
        totalElapsedSeconds = 0
        isPlaying = false
        isCompleted = false
        hasStarted = false
    }

    fun startExercise(exercise: FocusGuidedExercise) {
        voiceGuide.stop()
        selectedExercise = exercise
        currentStepIndex = 0
        elapsedInCurrentStep = 0
        totalElapsedSeconds = 0
        isCompleted = false
        isPlaying = false
        hasStarted = false
        voiceMessage = null
    }

    fun beginSelectedExercise() {
        val exercise = selectedExercise ?: return
        voiceGuide.stop()
        currentStepIndex = 0
        elapsedInCurrentStep = 0
        totalElapsedSeconds = 0
        isCompleted = false
        hasStarted = true
        isPlaying = true
        voiceMessage = null
    }

    fun restartExercise() {
        val exercise = selectedExercise ?: return
        voiceGuide.stop()
        currentStepIndex = 0
        elapsedInCurrentStep = 0
        totalElapsedSeconds = 0
        isCompleted = false
        hasStarted = true
        isPlaying = true
        voiceMessage = null
    }

    fun completeExercise() {
        voiceGuide.stop()
        isPlaying = false
        isCompleted = true
    }

    fun pauseExercise() {
        voiceGuide.stop()
        isPlaying = false
    }

    fun resumeExercise() {
        if (!isCompleted && selectedExercise != null && hasStarted) {
            isPlaying = true
        }
    }

    LaunchedEffect(
        selectedExercise?.id,
        currentStepIndex,
        hasStarted,
        isPlaying,
        isCompleted,
        voiceEnabled,
        voiceReady
    ) {
        val step = currentStep
        if (
            selectedExercise != null &&
            step != null &&
            hasStarted &&
            isPlaying &&
            !isCompleted &&
            voiceEnabled &&
            voiceReady
        ) {
            voiceGuide.speak(step.spokenText)
        }
    }

    LaunchedEffect(
        selectedExercise?.id,
        currentStepIndex,
        hasStarted,
        isPlaying,
        isCompleted
    ) {
        val exercise = selectedExercise ?: return@LaunchedEffect
        val step = exercise.steps.getOrNull(currentStepIndex) ?: return@LaunchedEffect

        if (!hasStarted || !isPlaying || isCompleted) return@LaunchedEffect

        while (
            isActive &&
            hasStarted &&
            isPlaying &&
            !isCompleted &&
            elapsedInCurrentStep < step.durationSeconds
        ) {
            delay(1_000)
            elapsedInCurrentStep += 1
            totalElapsedSeconds = exercise.steps
                .take(currentStepIndex)
                .sumOf { it.durationSeconds } + elapsedInCurrentStep
        }

        if (!isActive || !hasStarted || !isPlaying || isCompleted) return@LaunchedEffect

        if (elapsedInCurrentStep >= step.durationSeconds) {
            if (currentStepIndex >= exercise.steps.lastIndex) {
                totalElapsedSeconds = exercise.durationSeconds
                completeExercise()
            } else {
                currentStepIndex += 1
                elapsedInCurrentStep = 0
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        val backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surface
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            val exercise = selectedExercise

            if (exercise == null) {
                FocusRoomListContent(
                    voiceReady = voiceReady,
                    voiceMessage = voiceMessage,
                    onStartExercise = ::startExercise
                )
            } else {
                if (isCompleted) {
                    FocusExerciseCompletionContent(
                        exercise = exercise,
                        onReturnToFocusRoom = ::clearPlayer,
                        onReplay = ::restartExercise
                    )
                } else {
                    FocusExercisePlayerContent(
                        exercise = exercise,
                        currentStepIndex = currentStepIndex,
                        elapsedInCurrentStep = elapsedInCurrentStep,
                        totalElapsedSeconds = totalElapsedSeconds,
                        hasStarted = hasStarted,
                        isPlaying = isPlaying,
                        voiceEnabled = voiceEnabled,
                        voiceReady = voiceReady,
                        voiceMessage = voiceMessage,
                        onVoiceEnabledChanged = { enabled ->
                            voiceEnabled = enabled
                            if (!enabled) {
                                voiceGuide.stop()
                            }
                        },
                        onStart = ::beginSelectedExercise,
                        onPause = ::pauseExercise,
                        onResume = ::resumeExercise,
                        onRestart = ::restartExercise,
                        onEnd = ::clearPlayer
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusRoomListContent(
    voiceReady: Boolean,
    voiceMessage: String?,
    onStartExercise: (FocusGuidedExercise) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Focus Room. Let your attention settle."
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FocusRoomHeader()
        }

        item {
            FocusRoomHeroCard()
        }

        items(
            items = FocusRoomOriginalExercises.exercises,
            key = { it.id.name }
        ) { exercise ->
            FocusExerciseCard(
                exercise = exercise,
                onStart = { onStartExercise(exercise) }
            )
        }

        item {
            FocusRoomPhilosophyCard()
        }

        if (!voiceReady && voiceMessage != null) {
            item {
                VoiceUnavailableCard(message = voiceMessage)
            }
        }
    }
}

@Composable
private fun FocusRoomHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Focus Room",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Let your attention settle.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FocusRoomHeroCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.primary)
                .padding(22.dp)
        ) {
            FocusRoomAmbientVisualOnPrimary(
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.70f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Let your attention settle.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = "Choose a short guided exercise. Scyra will talk you through breath, body, or attention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                )

                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Guided mindfulness only",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusRoomAmbientVisualOnPrimary(
    modifier: Modifier = Modifier
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Canvas(
        modifier = modifier
            .size(132.dp)
            .semantics {
                contentDescription = "A calm glowing shell ripple."
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2.4f

        drawCircle(
            color = onPrimary.copy(alpha = 0.12f),
            radius = maxRadius,
            center = center
        )

        drawCircle(
            color = onPrimary.copy(alpha = 0.32f),
            radius = maxRadius * 0.72f,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )

        drawCircle(
            color = onPrimary.copy(alpha = 0.18f),
            radius = maxRadius * 0.38f,
            center = center
        )

        drawCircle(
            color = onPrimary.copy(alpha = 0.42f),
            radius = maxRadius * 0.18f,
            center = center
        )
    }
}

@Composable
private fun FocusRoomAmbientVisual(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .size(132.dp)
            .semantics {
                contentDescription = "A calm glowing shell ripple."
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2.4f

        drawCircle(
            color = primary.copy(alpha = 0.16f),
            radius = maxRadius,
            center = center
        )

        drawCircle(
            color = primary.copy(alpha = 0.32f),
            radius = maxRadius * 0.72f,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )

        drawCircle(
            color = surface.copy(alpha = 0.64f),
            radius = maxRadius * 0.38f,
            center = center
        )

        drawCircle(
            color = primary.copy(alpha = 0.34f),
            radius = maxRadius * 0.18f,
            center = center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusExerciseCard(
    exercise: FocusGuidedExercise,
    onStart: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusExerciseGlyph(exerciseId = exercise.id)

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "${formatDurationLabel(exercise.durationSeconds)} · Guided",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = exercise.bestFor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusPurposeChip(exercise = exercise)
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun FocusExerciseGlyph(
    exerciseId: FocusExerciseId
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                color = primaryContainer.copy(alpha = 0.72f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(34.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.6f

            when (exerciseId) {
                FocusExerciseId.THREE_POINT_GROUNDING -> {
                    drawCircle(primary.copy(alpha = 0.9f), radius = radius * 0.22f, center = center.copy(x = center.x, y = center.y - radius * 0.8f))
                    drawCircle(primary.copy(alpha = 0.75f), radius = radius * 0.22f, center = center.copy(x = center.x - radius * 0.72f, y = center.y + radius * 0.45f))
                    drawCircle(primary.copy(alpha = 0.75f), radius = radius * 0.22f, center = center.copy(x = center.x + radius * 0.72f, y = center.y + radius * 0.45f))
                }

                FocusExerciseId.BOX_BREATHING -> {
                    drawRoundRect(
                        color = primary.copy(alpha = 0.85f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                FocusExerciseId.MINI_BODY_SCAN -> {
                    drawCircle(primary.copy(alpha = 0.85f), radius = radius * 0.28f, center = center.copy(y = center.y - radius * 0.72f))
                    drawLine(
                        color = primary.copy(alpha = 0.85f),
                        start = center.copy(y = center.y - radius * 0.28f),
                        end = center.copy(y = center.y + radius * 0.9f),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = primary.copy(alpha = 0.72f),
                        start = center.copy(x = center.x - radius * 0.62f, y = center.y + radius * 0.04f),
                        end = center.copy(x = center.x + radius * 0.62f, y = center.y + radius * 0.04f),
                        strokeWidth = 4.dp.toPx()
                    )
                }

                FocusExerciseId.FOUR_SEVEN_EIGHT_BREATHING -> {
                    drawCircle(
                        color = primary.copy(alpha = 0.7f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.34f),
                        radius = radius * 0.58f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                FocusExerciseId.FIVE_SENSES_RESET -> {
                    repeat(5) { index ->
                        val angle = Math.toRadians((index * 72.0) - 90.0)
                        val x = center.x + kotlin.math.cos(angle).toFloat() * radius * 0.72f
                        val y = center.y + kotlin.math.sin(angle).toFloat() * radius * 0.72f
                        drawCircle(
                            color = primary.copy(alpha = 0.82f),
                            radius = radius * 0.18f,
                            center = Offset(x, y)
                        )
                    }
                    drawCircle(
                        color = primary.copy(alpha = 0.28f),
                        radius = radius * 0.24f,
                        center = center
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusPurposeChip(
    exercise: FocusGuidedExercise
) {
    val label = when (exercise.id) {
        FocusExerciseId.THREE_POINT_GROUNDING -> "Return"
        FocusExerciseId.BOX_BREATHING -> "Steady"
        FocusExerciseId.MINI_BODY_SCAN -> "Soften"
        FocusExerciseId.FOUR_SEVEN_EIGHT_BREATHING -> "Deepen"
        FocusExerciseId.FIVE_SENSES_RESET -> "Reset"
    }

    FilterChip(
        selected = false,
        onClick = {},
        label = {
            Text(label)
        }
    )
}

@Composable
private fun FocusRoomPhilosophyCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No points. No pressure.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Focus Room is here to help you settle. These exercises do not affect Scyra Points, Pearls, creatures, Stillwater, or stats.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun VoiceUnavailableCard(
    message: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Voice guidance unavailable",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "$message You can still follow every exercise on screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun FocusExercisePlayerContent(
    exercise: FocusGuidedExercise,
    currentStepIndex: Int,
    elapsedInCurrentStep: Int,
    totalElapsedSeconds: Int,
    hasStarted: Boolean,
    isPlaying: Boolean,
    voiceEnabled: Boolean,
    voiceReady: Boolean,
    voiceMessage: String?,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit
) {
    val currentStep = exercise.steps.getOrNull(currentStepIndex)
    val totalDuration = exercise.durationSeconds.coerceAtLeast(1)
    val progress = (totalElapsedSeconds.toFloat() / totalDuration.toFloat())
        .coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "${exercise.title}. Guided exercise player."
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            PlayerHeader(
                exercise = exercise,
                currentStepIndex = currentStepIndex,
                totalSteps = exercise.steps.size,
                onEnd = onEnd
            )
        }

        if (!hasStarted) {
            item {
                ExerciseReadyCard(
                    exercise = exercise,
                    voiceEnabled = voiceEnabled,
                    voiceReady = voiceReady,
                    onStart = onStart
                )
            }

            item {
                VoiceControlCard(
                    voiceEnabled = voiceEnabled,
                    voiceReady = voiceReady,
                    voiceMessage = voiceMessage,
                    onVoiceEnabledChanged = onVoiceEnabledChanged
                )
            }

            item {
                OutlinedButton(
                    onClick = onEnd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Focus Room")
                }
            }
        } else {
            item {
                if (currentStep != null) {
                    GuidedPromptCard(
                        exercise = exercise,
                        step = currentStep,
                        currentStepIndex = currentStepIndex,
                        elapsedInCurrentStep = elapsedInCurrentStep,
                        totalElapsedSeconds = totalElapsedSeconds,
                        progress = progress
                    )
                }
            }

            item {
                VoiceControlCard(
                    voiceEnabled = voiceEnabled,
                    voiceReady = voiceReady,
                    voiceMessage = voiceMessage,
                    onVoiceEnabledChanged = onVoiceEnabledChanged
                )
            }

            item {
                PlayerControls(
                    isPlaying = isPlaying,
                    onPause = onPause,
                    onResume = onResume,
                    onRestart = onRestart,
                    onEnd = onEnd
                )
            }
        }
    }
}

@Composable
private fun ExerciseReadyCard(
    exercise: FocusGuidedExercise,
    voiceEnabled: Boolean,
    voiceReady: Boolean,
    onStart: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StillnessVisual(
                step = FocusGuidedExerciseStep(
                    spokenText = "",
                    displayText = "",
                    durationSeconds = 1,
                    visualState = FocusExerciseVisualState.STILL
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            Text(
                text = "Ready when you are",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = contentColor.copy(alpha = 0.14f),
                contentColor = contentColor
            ) {
                Text(
                    text = if (voiceEnabled && voiceReady) {
                        "Voice guidance is on"
                    } else if (voiceEnabled) {
                        "Voice guidance is on, waiting for device voice"
                    } else {
                        "Text-only guidance is on"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor,
                    contentColor = containerColor
                )
            ) {
                Text(
                    text = "Start guided exercise",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    exercise: FocusGuidedExercise,
    currentStepIndex: Int,
    totalSteps: Int,
    onEnd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Step ${currentStepIndex + 1} of $totalSteps · ${formatDurationLabel(exercise.durationSeconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onEnd) {
            Text("End")
        }
    }
}

@Composable
private fun GuidedPromptCard(
    exercise: FocusGuidedExercise,
    step: FocusGuidedExerciseStep,
    currentStepIndex: Int,
    elapsedInCurrentStep: Int,
    totalElapsedSeconds: Int,
    progress: Float
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    val phaseRemainingSeconds = (step.durationSeconds - elapsedInCurrentStep)
        .coerceIn(0, step.durationSeconds)

    val shouldShowPhaseTimer =
        isBreathingExercise(exercise.id) && step.type.isBreathPhase()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExerciseVisual(
                step = step,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            AnimatedContent(
                targetState = step.displayText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(160))
                },
                label = "FocusExercisePrompt"
            ) { prompt ->
                Text(
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    text = prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }

            if (shouldShowPhaseTimer) {
                PhaseCountdownPill(
                    remainingSeconds = phaseRemainingSeconds,
                    totalSeconds = step.durationSeconds
                )
            }

            Text(
                text = stepSupportingText(exercise.id, step.type),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.84f),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.22f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatClock(totalElapsedSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.82f)
                    )

                    Text(
                        text = "-${formatClock((exercise.durationSeconds - totalElapsedSeconds).coerceAtLeast(0))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.82f)
                    )
                }
            }

            Text(
                text = "Step ${currentStepIndex + 1} of ${exercise.steps.size}",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.70f)
            )
        }
    }
}

@Composable
private fun PhaseCountdownPill(
    remainingSeconds: Int,
    totalSeconds: Int
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSecondary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = remainingSeconds.coerceAtLeast(0).toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "sec in this phase",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.78f)
            )
        }
    }
}

private fun isBreathingExercise(id: FocusExerciseId): Boolean {
    return id == FocusExerciseId.BOX_BREATHING ||
            id == FocusExerciseId.FOUR_SEVEN_EIGHT_BREATHING
}

private fun FocusExerciseStepType.isBreathPhase(): Boolean {
    return this == FocusExerciseStepType.BREATH_IN ||
            this == FocusExerciseStepType.HOLD ||
            this == FocusExerciseStepType.BREATH_OUT
}

@Composable
private fun ExerciseVisual(
    step: FocusGuidedExerciseStep,
    modifier: Modifier = Modifier
) {
    when (step.visualState) {
        FocusExerciseVisualState.EXPAND,
        FocusExerciseVisualState.FULL,
        FocusExerciseVisualState.CONTRACT,
        FocusExerciseVisualState.SMALL -> {
            BreathingVisual(
                step = step,
                modifier = modifier
            )
        }

        FocusExerciseVisualState.GROUNDING,
        FocusExerciseVisualState.BODY,
        FocusExerciseVisualState.STILL -> {
            StillnessVisual(
                step = step,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BreathingVisual(
    step: FocusGuidedExerciseStep,
    modifier: Modifier = Modifier
) {
    val targetScale = when (step.visualState) {
        FocusExerciseVisualState.EXPAND -> 1.18f
        FocusExerciseVisualState.FULL -> 1.18f
        FocusExerciseVisualState.CONTRACT -> 0.78f
        FocusExerciseVisualState.SMALL -> 0.78f
        else -> 1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = (step.durationSeconds * 1_000).coerceAtLeast(300)
        ),
        label = "BreathingScale"
    )

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(170.dp)
                .scale(animatedScale)
                .semantics {
                    contentDescription = "Breathing visual for ${step.displayText}."
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.5f

            drawCircle(
                color = primary.copy(alpha = 0.18f),
                radius = radius,
                center = center
            )

            drawCircle(
                color = primary.copy(alpha = 0.48f),
                radius = radius * 0.76f,
                center = center,
                style = Stroke(width = 5.dp.toPx())
            )

            drawCircle(
                color = surface.copy(alpha = 0.48f),
                radius = radius * 0.42f,
                center = center
            )

            drawCircle(
                color = primary.copy(alpha = 0.38f),
                radius = radius * 0.20f,
                center = center
            )
        }
    }
}

@Composable
private fun StillnessVisual(
    step: FocusGuidedExerciseStep,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface

    val pulseTarget = when (step.visualState) {
        FocusExerciseVisualState.BODY -> 1.06f
        FocusExerciseVisualState.GROUNDING -> 1.03f
        else -> 1.0f
    }

    val pulseScale by animateFloatAsState(
        targetValue = pulseTarget,
        animationSpec = tween(durationMillis = 1_200),
        label = "StillnessPulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
                .semantics {
                    contentDescription = "Calm stillness visual."
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2.35f

            drawCircle(
                color = primary.copy(alpha = 0.12f),
                radius = radius,
                center = center
            )

            drawCircle(
                color = tertiary.copy(alpha = 0.14f),
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            drawCircle(
                color = primary.copy(alpha = 0.30f),
                radius = radius * 0.48f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            drawCircle(
                color = surface.copy(alpha = 0.42f),
                radius = radius * 0.28f,
                center = center
            )
        }
    }
}

@Composable
private fun VoiceControlCard(
    voiceEnabled: Boolean,
    voiceReady: Boolean,
    voiceMessage: String?,
    onVoiceEnabledChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Voice guidance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = when {
                            voiceEnabled && voiceReady -> "On. Scyra will gently guide each step."
                            voiceEnabled && !voiceReady -> "On, but your device voice engine is still waking up."
                            else -> "Off. You can follow the text prompts on screen."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                    )
                }

                Switch(
                    checked = voiceEnabled,
                    enabled = true,
                    onCheckedChange = onVoiceEnabledChanged
                )
            }

            if (voiceEnabled && !voiceReady && voiceMessage != null) {
                Text(
                    text = voiceMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                if (isPlaying) {
                    onPause()
                } else {
                    onResume()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isPlaying) "Pause" else "Resume")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Text("Restart")
            }

            OutlinedButton(
                onClick = onEnd,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("End")
            }
        }
    }
}

@Composable
private fun FocusExerciseCompletionContent(
    exercise: FocusGuidedExercise,
    onReturnToFocusRoom: () -> Unit,
    onReplay: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(containerColor)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = contentColor.copy(alpha = 0.16f),
                        contentColor = contentColor
                    ) {
                        Text(
                            text = "Complete",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                    }

                    Text(
                        text = "Exercise complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )

                    Text(
                        text = "Let the stillness come with you.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor.copy(alpha = 0.88f)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = contentColor.copy(alpha = 0.12f),
                    contentColor = contentColor
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "You completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.78f)
                        )

                        Text(
                            text = exercise.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )

                        Text(
                            text = formatDurationLabel(exercise.durationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.76f)
                        )
                    }
                }

                Text(
                    text = "No points. No pressure. Just a moment returned to yourself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.82f)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReturnToFocusRoom,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = containerColor
                        )
                    ) {
                        Text(
                            text = "Return to Focus Room",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor
                        )
                    ) {
                        Text(
                            text = "Replay exercise",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun stepSupportingText(
    exerciseId: FocusExerciseId,
    stepType: FocusExerciseStepType
): String {
    return when {
        stepType == FocusExerciseStepType.BREATH_IN -> "Let the breath arrive."
        stepType == FocusExerciseStepType.HOLD -> "Hold gently. Do not force."
        stepType == FocusExerciseStepType.BREATH_OUT -> "Let the breath leave slowly."
        exerciseId == FocusExerciseId.THREE_POINT_GROUNDING -> "Return through one simple point at a time."
        exerciseId == FocusExerciseId.MINI_BODY_SCAN -> "Notice the body without needing to change everything."
        exerciseId == FocusExerciseId.FIVE_SENSES_RESET -> "Let the senses bring you back to the room."
        else -> "Stay gentle. Return when ready."
    }
}

private fun formatDurationLabel(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return when {
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}