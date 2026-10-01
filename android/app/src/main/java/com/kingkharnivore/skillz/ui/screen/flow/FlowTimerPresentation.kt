package com.kingkharnivore.skillz.ui.screen.flow

import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import kotlin.math.abs

/** Display only: the authoritative stopwatch and persisted Surge target are never modified. */
internal val FlowUiState.mainTimerText: String
    get() {
        val elapsedSeconds = stopwatch.elapsedMs / 1000L
        val targetMs = surgePlannedMs
        // Quantize elapsed time just as the normal Flow timer does. This gives zero a full
        // second at the target, even when ticker updates include fractional milliseconds.
        val displaySeconds = if (isSurgeOn && targetMs != null) {
            targetMs / 1000L - elapsedSeconds
        } else {
            elapsedSeconds
        }
        val sign = if (displaySeconds < 0L) "-" else ""
        return sign + formatFlowSeconds(abs(displaySeconds))
    }

internal val FlowUiState.hasReachedSurgeTarget: Boolean
    get() = isInFlowMode && surgePlannedMs?.let { stopwatch.elapsedMs >= it } == true

private fun formatFlowSeconds(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}
