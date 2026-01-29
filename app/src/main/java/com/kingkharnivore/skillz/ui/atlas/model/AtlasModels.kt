package com.kingkharnivore.skillz.ui.atlas.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

enum class BeamStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED_SUCCESS,
    COMPLETED_PARTIAL,
    MISSED
}

enum class ReadinessLevel {
    FAR, SOON, NEAR, IMMINENT, ACTIVE
}

data class AtlasUiState(
    val journeyFilter: JourneyFilter = JourneyFilter.All,
    val availableJourneys: List<JourneyChipUi> = emptyList(),

    val now: NowState = NowState(),
    val horizon: HorizonState = HorizonState(),
    val timeline: HorizonTimelineModel = HorizonTimelineModel(),
    val aftermath: AftermathModel = AftermathModel()
)

/**
 * Horizon = the “time window” we are looking at.
 * Example: startMs=nowRoundedToHour, hours=8 => now..+8h.
 */
data class HorizonState(
    val startMs: Long = 0L,
    val hours: Int = 8,
    val nowMs: Long = 0L
) {
    val endMs: Long get() = startMs + hours * 60L * 60L * 1000L
    val rangeMinutes: Int get() = hours * 60

    fun title(): String {
        if (startMs <= 0L) return "Horizon"
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ofPattern("h:mm a")
        val s = Instant.ofEpochMilli(startMs).atZone(zone).format(fmt)
        val e = Instant.ofEpochMilli(endMs).atZone(zone).format(fmt)
        return "$s → $e"
    }
}

data class HorizonTimelineModel(
    val blocks: List<BeamBlockUi> = emptyList(),
    val ticks: List<HorizonTickUi> = emptyList()
)

data class HorizonTickUi(
    val minuteFromStart: Int, // 0..rangeMinutes
    val label: String,        // "5 PM"
    val isMajor: Boolean
)

data class JourneyChipUi(
    val tagId: Long,
    val name: String
)

sealed class JourneyFilter {
    data object All : JourneyFilter()
    data class Only(val tagId: Long) : JourneyFilter()
}

data class NowState(
    val activeBeam: BeamBlockUi? = null,
    val nextBeam: BeamBlockUi? = null,
    val activeBeamRemainingMs: Long? = null,
    val activeBeamProgress: Float? = null // 0..1
) {
    val isBeamActive: Boolean get() = activeBeam != null
}

data class AftermathModel(
    val completed: List<CompletedBeamUi> = emptyList()
)

data class CompletedBeamUi(
    val beam: BeamBlockUi,
    val flows: List<FlowInBeamUi>
)

data class FlowInBeamUi(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val title: String? = null
)

/**
 * BeamBlockUi.startMin/endMin are minutes relative to Horizon.startMs (NOT a day).
 */
data class BeamBlockUi(
    val beamId: Long,
    val tagId: Long,
    val tagName: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val status: BeamStatus,
    val readiness: ReadinessLevel,

    // minutes relative to Horizon start
    val startMin: Int,
    val endMin: Int,

    // clipping (beam extends outside the visible horizon)
    val clippedTop: Boolean = false,
    val clippedBottom: Boolean = false,

    val completionRatio: Float = 0f
)

fun computeBeamStatus(
    nowMs: Long,
    startMs: Long,
    endMs: Long,
    completionRatio: Float
): BeamStatus =
    when {
        nowMs in startMs until endMs -> BeamStatus.ACTIVE
        nowMs < startMs -> BeamStatus.UPCOMING
        completionRatio <= 0f -> BeamStatus.MISSED
        completionRatio >= 1f -> BeamStatus.COMPLETED_SUCCESS
        else -> BeamStatus.COMPLETED_PARTIAL
    }

fun computeReadiness(
    nowMs: Long,
    startMs: Long,
    status: BeamStatus
): ReadinessLevel {
    if (status == BeamStatus.ACTIVE) return ReadinessLevel.ACTIVE
    val diff = startMs - nowMs
    return when {
        diff <= 60 * 60 * 1000L -> ReadinessLevel.IMMINENT
        diff <= 6 * 60 * 60 * 1000L -> ReadinessLevel.NEAR
        diff <= 24 * 60 * 60 * 1000L -> ReadinessLevel.SOON
        else -> ReadinessLevel.FAR
    }
}

fun progress(nowMs: Long, startMs: Long, endMs: Long): Float {
    val dur = max(1L, endMs - startMs)
    val elapsed = (nowMs - startMs).coerceIn(0L, dur)
    return elapsed.toFloat() / dur.toFloat()
}

fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean =
    aStart < bEnd && aEnd > bStart
