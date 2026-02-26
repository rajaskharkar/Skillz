package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.model.AtlasUiState
import com.kingkharnivore.skillz.ui.model.AtlasViewMode
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.model.BeamStatus
import com.kingkharnivore.skillz.ui.model.DayAnchorUi
import com.kingkharnivore.skillz.ui.model.DayPlanUi
import com.kingkharnivore.skillz.ui.model.DaySegmentUi
import com.kingkharnivore.skillz.ui.model.JourneyChipUi
import com.kingkharnivore.skillz.ui.model.JourneyFilter
import com.kingkharnivore.skillz.ui.model.NowState
import com.kingkharnivore.skillz.ui.model.ReadinessLevel
import com.kingkharnivore.skillz.ui.model.computeBeamStatus
import com.kingkharnivore.skillz.ui.model.computeReadiness
import com.kingkharnivore.skillz.ui.model.overlaps
import com.kingkharnivore.skillz.ui.model.progress
import com.kingkharnivore.skillz.ui.theme.ColdSteel
import com.kingkharnivore.skillz.utils.time.dayStartPlusDays
import com.kingkharnivore.skillz.utils.time.floorToDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class AtlasViewModel @Inject constructor(
    private val beamRepository: BeamRepository,
    private val journeyRepository: JourneyRepository,
    private val sessionRepository: FlowRepository
) : ViewModel() {

    private val journeyFilter = MutableStateFlow<JourneyFilter>(JourneyFilter.All)

    private val horizonHours = MutableStateFlow(8)
    private val horizonStartMs = MutableStateFlow(floorToHour(System.currentTimeMillis()))

    private val viewMode = MutableStateFlow(AtlasViewMode.DAY)
    private val selectedDayStartMs = MutableStateFlow(floorToDay(System.currentTimeMillis()))

    fun setViewMode(mode: AtlasViewMode) = viewMode.update { mode }

    @Volatile private var minDayStartCache: Long? = null

    fun shiftSelectedDay(deltaDays: Long) {
        selectedDayStartMs.update { current ->
            val next = dayStartPlusDays(current, deltaDays)
            val min = minDayStartCache
            if (min != null && next < min) min else next
        }
    }

    fun goToToday() {
        selectedDayStartMs.update { floorToDay(System.currentTimeMillis()) }
        viewMode.update { AtlasViewMode.DAY }
    }

    private val nowTicker: Flow<Long> =
        tickerFlow(periodMs = 1000L).onStart { emit(System.currentTimeMillis()) }

    private fun tickerFlow(periodMs: Long = 60_000L): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }

    private val tagsFlow: Flow<Pair<Map<Long, String>, List<JourneyChipUi>>> =
        journeyRepository.getAllTags()
            .map { tags ->
                val map = tags.associateBy({ it.id }, { it.name })
                val chips = tags
                    .map { JourneyChipUi(it.id, it.name) }
                    .sortedBy { it.name.lowercase() }
                map to chips
            }

    val uiState: StateFlow<AtlasUiState> =
        beamRepository.observeAllBeams()
            .combine(sessionRepository.getAllSessions()) { beams, sessions -> beams to sessions } // ✅
            .combine(tagsFlow) { (beams, sessions), tagData -> Triple(beams, sessions, tagData) }
            .combine(journeyFilter) { (beams, sessions, tagData), filter -> Quad(beams, sessions, tagData, filter) }
            .combine(horizonStartMs) { quad, start -> Penta(quad.a, quad.b, quad.c, quad.d, start) }
            .combine(horizonHours) { penta, hours -> Sexta(penta.a, penta.b, penta.c, penta.d, penta.e, hours) }
            .combine(viewMode) { sexta, mode -> Septa(sexta.a, sexta.b, sexta.c, sexta.d, sexta.e, sexta.f, mode) }
            .combine(selectedDayStartMs) { septa, dayStart -> Octa(septa.a, septa.b, septa.c, septa.d, septa.e, septa.f, septa.g, dayStart) }
            .combine(nowTicker) { octa, nowMs ->
                buildUiState(
                    beams = octa.a,
                    sessions = octa.b,
                    tagData = octa.c,
                    filter = octa.d,
                    horizonStart = octa.e,
                    horizonHours = octa.f,
                    viewMode = octa.g,
                    selectedDayStartMs = octa.h,
                    nowMs = nowMs
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AtlasUiState())

    private fun buildUiState(
        beams: List<BeamEntity>,
        sessions: List<SessionEntity>,
        tagData: Pair<Map<Long, String>, List<JourneyChipUi>>,
        filter: JourneyFilter,
        horizonStart: Long,
        horizonHours: Int,
        viewMode: AtlasViewMode,
        selectedDayStartMs: Long,
        nowMs: Long
    ): AtlasUiState {
        val (tagMap, tagChips) = tagData
        val hours = horizonHours.coerceIn(2, 12)

        val pastBufferHours = minOf(2, maxOf(0, hours - 2))
        val start = horizonStart - pastBufferHours * 60L * 60L * 1000L
        val end = start + hours * 60L * 60L * 1000L
        val rangeMinutes = hours * 60

        // ✅ index sessions by beamId when present (fast path)
        val sessionsByBeamId =
            sessions
                .filter { it.beamId != null }
                .groupBy { it.beamId!! }

        val allBlocks = beams.map { beam ->
            val tagName = tagMap[beam.tagId] ?: "Unknown"

            val eligibleMs = sessionsByBeamId[beam.id]
                .orEmpty()
                .sumOf { it.beamEligibleMs }

            val completionRatio =
                (eligibleMs.toFloat() / beam.durationMs.toFloat())
                    .coerceIn(0f, 1f)

            val status = computeBeamStatus(
                nowMs = nowMs,
                startMs = beam.startTime,
                endMs = beam.endTime,
                completionRatio = completionRatio
            )

            val readiness = computeReadiness(
                nowMs = nowMs,
                startMs = beam.startTime,
                status = status
            )

            RawBeamBlock(
                beam = beam,
                tagName = tagName,
                status = status,
                readiness = readiness,
                completionRatio = completionRatio
            )
        }

        val filtered = when (filter) {
            JourneyFilter.All -> allBlocks
            is JourneyFilter.Only -> allBlocks.filter { it.beam.tagId == filter.tagId }
        }

        val minSelectableDay = filtered.minOfOrNull { floorToDay(it.beam.startTime) }
        minDayStartCache = minSelectableDay

        val activeRaw = filtered.firstOrNull { it.status == BeamStatus.ACTIVE }
        val nextRaw = filtered
            .filter { it.beam.startTime > nowMs }
            .minByOrNull { it.beam.startTime }

        val dayPlan = buildDayPlan(filtered = filtered, dayStartMs = selectedDayStartMs, nowMs = nowMs)

        val horizonBlocks = filtered
            .filter { overlaps(it.beam.startTime, it.beam.endTime, start, end) }
            .sortedBy { it.beam.startTime }
            .map { raw ->
                projectIntoHorizon(
                    beam = raw.beam,
                    tagName = raw.tagName,
                    status = raw.status,
                    readiness = raw.readiness,
                    horizonStartMs = start,
                    rangeMinutes = rangeMinutes,
                    nowMs = nowMs,
                    completionRatio = raw.completionRatio
                )
            }

        return AtlasUiState(
            journeyFilter = filter,
            availableJourneys = tagChips,
            now = NowState(
                activeBeam = activeRaw?.let {
                    projectIntoHorizon(
                        beam = it.beam,
                        tagName = it.tagName,
                        status = it.status,
                        readiness = it.readiness,
                        horizonStartMs = start,
                        rangeMinutes = rangeMinutes,
                        nowMs = nowMs,
                        completionRatio = it.completionRatio
                    )
                },
                nextBeam = nextRaw?.let {
                    projectIntoHorizon(
                        beam = it.beam,
                        tagName = it.tagName,
                        status = it.status,
                        readiness = it.readiness,
                        horizonStartMs = start,
                        rangeMinutes = rangeMinutes,
                        nowMs = nowMs,
                        completionRatio = it.completionRatio
                    )
                },
                activeBeamRemainingMs = activeRaw?.let { max(0L, it.beam.endTime - nowMs) },
                activeBeamProgress = activeRaw?.let { progress(nowMs, it.beam.startTime, it.beam.endTime) }
            ),
            viewMode = viewMode,
            selectedDayStartMs = selectedDayStartMs,
            dayPlan = dayPlan,
            minSelectableDayStartMs = minSelectableDay,
        )
    }

    /**
     * ✅ Completion ratio logic:
     * 1) Fast path: sessions with beamId == this beam.id, sum beamEligibleMs
     * 2) Fallback: if no sessions are linked, optionally compute overlap-based completion
     */
    private fun computeCompletionRatio(
        beam: BeamEntity,
        sessionsByBeamId: Map<Long, List<SessionEntity>>,
        allSessions: List<SessionEntity>
    ): Float {
        val beamDur = (beam.endTime - beam.startTime).coerceAtLeast(1L)

        val linked = sessionsByBeamId[beam.id].orEmpty()
        if (linked.isNotEmpty()) {
            val eligible = linked.sumOf { it.beamEligibleMs.coerceAtLeast(0L) }
                .coerceAtMost(beamDur)
            return eligible.toFloat() / beamDur.toFloat()
        }

        // Optional fallback: overlap-based for sessions without beamId (safe default)
        // If you DON'T want fallback, just return 0f here.
        val overlapMs = allSessions
            .asSequence()
            .filter { overlaps(it.startTime, it.endTime, beam.startTime, beam.endTime) }
            .sumOf { overlapMs(it.startTime, it.endTime, beam.startTime, beam.endTime) }
            .coerceAtMost(beamDur)

        return overlapMs.toFloat() / beamDur.toFloat()
    }

    private fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val s = maxOf(aStart, bStart)
        val e = minOf(aEnd, bEnd)
        return (e - s).coerceAtLeast(0L)
    }

    private fun buildDayPlan(
        filtered: List<RawBeamBlock>,
        dayStartMs: Long,
        nowMs: Long
    ): DayPlanUi {
        val dayEndMs = dayStartMs + 24L * 60L * 60L * 1000L

        val dayRaw = filtered
            .filter { raw -> overlaps(raw.beam.startTime, raw.beam.endTime, dayStartMs, dayEndMs) }
            .sortedBy { it.beam.startTime }

        val dayBlocks = dayRaw.map { raw ->
            projectToWindow(
                beam = raw.beam,
                tagName = raw.tagName,
                status = raw.status,
                readiness = raw.readiness,
                windowStartMs = dayStartMs,
                windowMinutes = 1440,
                nowMs = nowMs,
                completionRatio = raw.completionRatio
            )
        }.sortedBy { it.startMs }

        val segments = mutableListOf<DaySegmentUi>()
        val anchors = mutableListOf<DayAnchorUi>()
        var displayCursorMin = 0

        fun addAnchor(realMinute: Int, displayMinute: Int) {
            val rm = realMinute.coerceIn(0, 1440)
            val dm = displayMinute.coerceAtLeast(0)
            if (anchors.lastOrNull()?.minuteOfDay == rm) {
                anchors[anchors.lastIndex] = DayAnchorUi(rm, dm)
            } else {
                anchors += DayAnchorUi(rm, dm)
            }
        }

        addAnchor(0, 0)
        var cursorMs = dayStartMs

        for (b in dayBlocks) {
            val beamStartClamped = maxOf(b.startMs, dayStartMs)
            val beamEndClamped = minOf(b.endMs, dayEndMs)

            if (beamStartClamped > cursorMs) {
                val gapMin = ((beamStartClamped - cursorMs) / 60_000L).toInt().coerceAtLeast(1)
                val dispMin = gapDisplayMinutes(gapMin)

                val realStartMin = ((cursorMs - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val realEndMin = ((beamStartClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)

                addAnchor(realStartMin, displayCursorMin)
                displayCursorMin += dispMin
                addAnchor(realEndMin, displayCursorMin)

                segments += DaySegmentUi.Gap(gapMinutes = gapMin, displayMinutes = dispMin)
            }

            run {
                val realStartMin = ((beamStartClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val realEndMin = ((beamEndClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val beamMin = (realEndMin - realStartMin).coerceAtLeast(1)

                addAnchor(realStartMin, displayCursorMin)
                displayCursorMin += beamMin
                addAnchor(realEndMin, displayCursorMin)

                segments += DaySegmentUi.Beam(
                    block = b.copy(
                        clippedTop = b.startMs < dayStartMs,
                        clippedBottom = b.endMs > dayEndMs
                    ),
                    realMinutes = beamMin,
                    displayMinutes = beamMin
                )

                cursorMs = maxOf(cursorMs, beamEndClamped)
            }
        }

        if (cursorMs < dayEndMs) {
            val gapMin = ((dayEndMs - cursorMs) / 60_000L).toInt().coerceAtLeast(1)
            val dispMin = gapDisplayMinutes(gapMin)

            val realStartMin = ((cursorMs - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)

            addAnchor(realStartMin, displayCursorMin)
            displayCursorMin += dispMin
            addAnchor(1440, displayCursorMin)

            segments += DaySegmentUi.Gap(gapMinutes = gapMin, displayMinutes = dispMin)
        } else {
            if (anchors.lastOrNull()?.minuteOfDay != 1440) addAnchor(1440, displayCursorMin)
        }

        return DayPlanUi(
            dayStartMs = dayStartMs,
            dayEndMs = dayEndMs,
            segments = segments,
            anchors = anchors,
            totalDisplayMinutes = displayCursorMin.coerceAtLeast(1),
            beamsCount = dayBlocks.size
        )
    }

    private fun gapDisplayMinutes(gapMin: Int): Int {
        val head = minOf(gapMin, 30)
        val tail = (gapMin - head).coerceAtLeast(0)
        val collapsedTail = (tail * 0.15f).toInt()
        return (head + collapsedTail).coerceAtLeast(8)
    }

    private fun projectToWindow(
        beam: BeamEntity,
        tagName: String,
        status: BeamStatus,
        readiness: ReadinessLevel,
        windowStartMs: Long,
        windowMinutes: Int,
        nowMs: Long,
        completionRatio: Float
    ): BeamBlockUi {
        val startMinRaw = ((beam.startTime - windowStartMs) / 60_000L).toInt()
        val endMinRaw = ((beam.endTime - windowStartMs) / 60_000L).toInt()

        val clippedTop = startMinRaw < 0
        val clippedBottom = endMinRaw > windowMinutes

        val clampedStart = startMinRaw.coerceIn(0, windowMinutes)
        val clampedEnd = kotlin.math.max(clampedStart + 1, endMinRaw.coerceIn(0, windowMinutes))

        val isPast = beam.endTime <= nowMs
        val journeyColorArgb = if (isPast) ColdSteel else colorForTagId(beam.tagId)

        return BeamBlockUi(
            beamId = beam.id,
            tagId = beam.tagId,
            tagName = tagName,
            startMs = beam.startTime,
            endMs = beam.endTime,
            durationMs = beam.durationMs,
            status = status,
            readiness = readiness,
            startMin = clampedStart,
            endMin = clampedEnd,
            clippedTop = clippedTop,
            clippedBottom = clippedBottom,
            completionRatio = completionRatio,
            journeyColorArgb = journeyColorArgb
        )
    }

    private data class RawBeamBlock(
        val beam: BeamEntity,
        val tagName: String,
        val status: BeamStatus,
        val readiness: ReadinessLevel,
        val completionRatio: Float
    )

    private fun projectIntoHorizon(
        beam: BeamEntity,
        tagName: String,
        status: BeamStatus,
        readiness: ReadinessLevel,
        horizonStartMs: Long,
        rangeMinutes: Int,
        nowMs: Long,
        completionRatio: Float
    ): BeamBlockUi {
        val startMinRaw = ((beam.startTime - horizonStartMs) / 60_000L).toInt()
        val endMinRaw = ((beam.endTime - horizonStartMs) / 60_000L).toInt()

        val clippedTop = startMinRaw < 0
        val clippedBottom = endMinRaw > rangeMinutes

        val clampedStart = startMinRaw.coerceIn(0, rangeMinutes)
        val clampedEnd = max(clampedStart + 1, endMinRaw.coerceIn(0, rangeMinutes))

        val isPast = beam.endTime <= nowMs
        val journeyColorArgb = if (isPast) ColdSteel else colorForTagId(beam.tagId)

        return BeamBlockUi(
            beamId = beam.id,
            tagId = beam.tagId,
            tagName = tagName,
            startMs = beam.startTime,
            endMs = beam.endTime,
            durationMs = beam.durationMs,
            status = status,
            readiness = readiness,
            startMin = clampedStart,
            endMin = clampedEnd,
            clippedTop = clippedTop,
            clippedBottom = clippedBottom,
            completionRatio = completionRatio,
            journeyColorArgb = journeyColorArgb
        )
    }

    private val ATLAS_JOURNEY_PALETTE: List<Int> = listOf(
        0xFF8B1E1E.toInt(),
        0xFF3A5F8C.toInt(),
        0xFF2F8F86.toInt(),
        0xFF6F9E91.toInt(),
        0xFFD1B45A.toInt(),
        0xFFCC8A3E.toInt(),
        0xFF7A4A32.toInt(),
        0xFF8C6AA8.toInt(),
        0xFF3E8F6B.toInt()
    )

    private fun colorForTagId(tagId: Long): Int {
        val palette = ATLAS_JOURNEY_PALETTE
        val idx = abs((tagId % palette.size).toInt())
        return palette[idx]
    }
}

/** helpers for nested combine typing */
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Penta<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private data class Sexta<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
private data class Septa<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)
private data class Octa<A, B, C, D, E, F, G, H>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H)

private fun floorToHour(ms: Long): Long {
    val hourMs = 60L * 60L * 1000L
    return ms - (ms % hourMs)
}