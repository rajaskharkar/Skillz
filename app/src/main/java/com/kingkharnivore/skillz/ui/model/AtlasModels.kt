package com.kingkharnivore.skillz.ui.model

import kotlin.math.max

enum class BeamStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED_SUCCESS,
    COMPLETED_PARTIAL,
    MISSED
}

enum class ReadinessLevel(
    val displayLabel: String
) {
    DISTANT("DISTANT"),
    PLANNED("PLANNED"),
    LATER_TODAY("LATER TODAY"),
    COMING_UP("COMING UP"),
    ON_DECK("ON DECK"),
    APPROACHING("APPROACHING"),
    GET_READY("GET READY"),
    SOON("SOON"),
    PREP("PREP"),
    NOW("NOW"),
    ACTIVE("ACTIVE"),
    MISSED("MISSED"),
    EXPIRED("EXPIRED");
}

enum class AtlasViewMode { DAY, WEEK, MONTH }

data class AtlasUiState(
    val journeyFilter: JourneyFilter = JourneyFilter.All,
    val availableJourneys: List<JourneyChipUi> = emptyList(),
    val now: NowState = NowState(),
    val nowMs: Long = 0L,
    val viewMode: AtlasViewMode = AtlasViewMode.DAY,
    val selectedDayStartMs: Long = 0L,
    val day: AtlasDayUi = AtlasDayUi(0L, 0L),
    val week: AtlasWeekUi = AtlasWeekUi(0L, 0L),
    val month: AtlasMonthUi = AtlasMonthUi(0L, 0L),
    val beamsByDayStartMs: Map<Long, Int> = emptyMap(),
    val minSelectableDayStartMs: Long? = null,
)

data class AtlasDayUi(
    val dayStartMs: Long,
    val dayEndMs: Long,
    val beams: List<BeamBlockUi> = emptyList(),
    val beamsCount: Int = 0
)

data class AtlasWeekUi(
    val weekStartMs: Long,
    val weekEndMs: Long,
    val days: List<AtlasWeekDayUi> = emptyList(),
    val beamsCount: Int = 0,
    val activeDaysCount: Int = 0,
    val totalDurationMs: Long = 0L
)

data class AtlasWeekDayUi(
    val dayStartMs: Long,
    val dayEndMs: Long,
    val beams: List<BeamBlockUi> = emptyList(),
    val beamsCount: Int = 0,
    val totalDurationMs: Long = 0L
)

data class AtlasMonthUi(
    val monthStartMs: Long,
    val monthEndMs: Long,
    val cells: List<AtlasMonthCellUi> = emptyList(),
    val beamsCount: Int = 0,
    val activeDaysCount: Int = 0
)

data class AtlasMonthCellUi(
    val dayStartMs: Long,
    val isInCurrentMonth: Boolean,
    val beamsCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val previewColors: List<Int> = emptyList()
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
    val activeBeamProgress: Float? = null
) {
    val isBeamActive: Boolean get() = activeBeam != null
}

data class BeamBlockUi(
    val beamId: Long,
    val tagId: Long,
    val tagName: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val status: BeamStatus,
    val readiness: ReadinessLevel,
    val startMin: Int,
    val endMin: Int,
    val clippedTop: Boolean = false,
    val clippedBottom: Boolean = false,
    val completionRatio: Float = 0f,
    val journeyColorArgb: Int = 0
)

fun computeBeamStatus(nowMs: Long, startMs: Long, endMs: Long, completionRatio: Float): BeamStatus {
    if (nowMs < startMs) return BeamStatus.UPCOMING
    if (nowMs in startMs until endMs) return BeamStatus.ACTIVE
    val r = completionRatio.coerceIn(0f, 1f)
    val eps = 0.01f
    return when {
        r >= 1f - eps -> BeamStatus.COMPLETED_SUCCESS
        r > eps -> BeamStatus.COMPLETED_PARTIAL
        else -> BeamStatus.MISSED
    }
}

fun computeReadiness(
    nowMs: Long,
    startMs: Long,
    status: BeamStatus
): ReadinessLevel {
    if (status == BeamStatus.ACTIVE) return ReadinessLevel.ACTIVE
    if (status == BeamStatus.MISSED) return ReadinessLevel.MISSED
    if (status == BeamStatus.COMPLETED_SUCCESS || status == BeamStatus.COMPLETED_PARTIAL) {
        return ReadinessLevel.EXPIRED
    }

    val msUntilStart = (startMs - nowMs).coerceAtLeast(0L)
    val minutes = msUntilStart / 60_000L

    if (minutes <= 180) {
        return when {
            minutes <= 3 -> ReadinessLevel.NOW
            minutes <= 10 -> ReadinessLevel.PREP
            minutes <= 20 -> ReadinessLevel.SOON
            minutes <= 45 -> ReadinessLevel.GET_READY
            minutes <= 90 -> ReadinessLevel.APPROACHING
            else -> ReadinessLevel.ON_DECK
        }
    }

    val hours = msUntilStart / 3_600_000L
    return when {
        hours < 6 -> ReadinessLevel.COMING_UP
        hours < 12 -> ReadinessLevel.LATER_TODAY
        hours < 24 -> ReadinessLevel.PLANNED
        else -> ReadinessLevel.DISTANT
    }
}

fun progress(nowMs: Long, startMs: Long, endMs: Long): Float {
    val dur = max(1L, endMs - startMs)
    val elapsed = (nowMs - startMs).coerceIn(0L, dur)
    return elapsed.toFloat() / dur.toFloat()
}

fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean =
    aStart < bEnd && aEnd > bStart