package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.atlas.model.*
import com.kingkharnivore.skillz.ui.theme.ColdSteel
import com.kingkharnivore.skillz.utils.time.dayStartPlusDays
import com.kingkharnivore.skillz.utils.time.floorToDay
import com.kingkharnivore.skillz.viewmodel.atlas.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class AtlasViewModel @Inject constructor(
    private val beamRepository: BeamRepository,
    private val journeyRepository: JourneyRepository
) : ViewModel() {

    private val journeyFilter = MutableStateFlow<JourneyFilter>(JourneyFilter.All)

    private val horizonHours = MutableStateFlow(8) // default: next 8 hours
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

    fun setJourneyFilter(filter: JourneyFilter) = journeyFilter.update { filter }

    fun setHorizonHours(hours: Int) {
        val clamped = hours.coerceIn(2, 12)
        horizonHours.update { clamped }
    }

    fun shiftHorizonByHours(deltaHours: Int) {
        val delta = deltaHours * 60L * 60L * 1000L
        horizonStartMs.update { it + delta }
    }

    fun resetHorizonToNow() {
        horizonStartMs.update { floorToHour(System.currentTimeMillis()) }
    }

    fun jumpToNextBeam() {
        // shift horizonStart to next beam start (rounded to hour is optional; here we snap to beam start)
        viewModelScope.launch {
            // we can compute from latest snapshot of beams via state if you want,
            // but simplest: let UI call this with nextBeam.startMs later.
            // For now, no-op unless you wire a parameter.
        }
    }

    private val nowTicker: Flow<Long> =
        tickerFlow(periodMs = 1000L).onStart { emit(System.currentTimeMillis()) }

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
            .combine(tagsFlow) { beams, tagData -> beams to tagData }
            .combine(journeyFilter) { (beams, tagData), filter -> Triple(beams, tagData, filter) }
            .combine(horizonStartMs) { (beams, tagData, filter), start -> Quad(beams, tagData, filter, start) }
            .combine(horizonHours) { quad, hours -> Penta(quad.a, quad.b, quad.c, quad.d, hours) }
            .combine(viewMode) { penta, mode -> Sexta(penta.a, penta.b, penta.c, penta.d, penta.e, mode) }
            .combine(selectedDayStartMs) { sexta, dayStart -> Septa(sexta.a, sexta.b, sexta.c, sexta.d, sexta.e, sexta.f, dayStart) }
            .combine(nowTicker) { septa, nowMs ->
                buildUiState(
                    beams = septa.a,
                    tagData = septa.b,
                    filter = septa.c,
                    horizonStart = septa.d,
                    horizonHours = septa.e,
                    viewMode = septa.f,
                    selectedDayStartMs = septa.g,
                    nowMs = nowMs
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AtlasUiState())

    private fun buildUiState(
        beams: List<BeamEntity>,
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

        // ✅ Adaptive "history" buffer:
        // - 2h view: show NO past buffer (otherwise you lose the future entirely)
        // - 4h/6h/8h/12h: show up to 2h of past for context
        val pastBufferHours = minOf(2, maxOf(0, hours - 2))

        val start = horizonStart - pastBufferHours * 60L * 60L * 1000L
        val end = start + hours * 60L * 60L * 1000L
        val rangeMinutes = hours * 60

        // Build raw blocks
        val allBlocks = beams.map { beam ->
            val status = computeBeamStatus(nowMs, beam.startTime, beam.endTime, completionRatio = 0f)
            val readiness = computeReadiness(nowMs, beam.startTime, status)
            RawBeamBlock(
                beam = beam,
                tagName = tagMap[beam.tagId] ?: "Unknown",
                status = status,
                readiness = readiness
            )
        }

        val filtered = when (filter) {
            JourneyFilter.All -> allBlocks
            is JourneyFilter.Only -> allBlocks.filter { it.beam.tagId == filter.tagId }
        }

        val minSelectableDay = filtered
            .minOfOrNull { floorToDay(it.beam.startTime) }

        // update cache so shiftSelectedDay can clamp
        minDayStartCache = minSelectableDay

        // NOW state from ALL filtered beams (not just horizon)
        val activeRaw = filtered.firstOrNull { it.status == BeamStatus.ACTIVE }
        val nextRaw = filtered
            .filter { it.beam.startTime > nowMs }
            .minByOrNull { it.beam.startTime }

        val dayPlan = buildDayPlan(
            filtered = filtered,
            dayStartMs = selectedDayStartMs,
            nowMs = nowMs
        )

        // Horizon shows only beams that overlap the horizon range
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
                    nowMs = nowMs
                )
            }

        val ticks = buildHorizonTicks(start, hours)

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
                        nowMs = nowMs
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
                        nowMs = nowMs
                    )
                },
                activeBeamRemainingMs = activeRaw?.let { max(0L, it.beam.endTime - nowMs) },
                activeBeamProgress = activeRaw?.let { progress(nowMs, it.beam.startTime, it.beam.endTime) }
            ),
            viewMode = viewMode,
            selectedDayStartMs = selectedDayStartMs,
            dayPlan = dayPlan,
            minSelectableDayStartMs = minSelectableDay,
            horizon = HorizonState(
                startMs = start,
                hours = hours,
                nowMs = nowMs
            ),
            timeline = HorizonTimelineModel(
                blocks = horizonBlocks,
                ticks = ticks
            ),
            aftermath = AftermathModel(completed = emptyList()) // later
        )
    }

    private fun buildDayPlan(
        filtered: List<RawBeamBlock>,
        dayStartMs: Long,
        nowMs: Long
    ): DayPlanUi {
        val dayEndMs = dayStartMs + 24L * 60L * 60L * 1000L

        // ✅ include beams that overlap the day (handles cross-midnight)
        val dayRaw = filtered
            .filter { raw -> overlaps(raw.beam.startTime, raw.beam.endTime, dayStartMs, dayEndMs) }
            .sortedBy { it.beam.startTime }

        // Project into BeamBlockUi (keeps colors/status/readiness)
        val dayBlocks = dayRaw.map { raw ->
            projectToWindow(
                beam = raw.beam,
                tagName = raw.tagName,
                status = raw.status,
                readiness = raw.readiness,
                windowStartMs = dayStartMs,
                windowMinutes = 1440,
                nowMs = nowMs
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
            // ✅ CLIP beam to this day for segment sizing / mapping
            val beamStartClamped = maxOf(b.startMs, dayStartMs)
            val beamEndClamped = minOf(b.endMs, dayEndMs)

            // ----- GAP before beam -----
            if (beamStartClamped > cursorMs) {
                val gapMin = ((beamStartClamped - cursorMs) / 60_000L).toInt().coerceAtLeast(1)
                val dispMin = gapDisplayMinutes(gapMin)

                val realStartMin = ((cursorMs - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val realEndMin = ((beamStartClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)

                addAnchor(realStartMin, displayCursorMin)
                displayCursorMin += dispMin
                addAnchor(realEndMin, displayCursorMin)

                segments += DaySegmentUi.Gap(
                    gapMinutes = gapMin,
                    displayMinutes = dispMin
                )
            }

            // ----- BEAM segment -----
            run {
                val realStartMin = ((beamStartClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val realEndMin = ((beamEndClamped - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
                val beamMin = (realEndMin - realStartMin).coerceAtLeast(1)

                addAnchor(realStartMin, displayCursorMin)
                displayCursorMin += beamMin
                addAnchor(realEndMin, displayCursorMin)

                // ✅ IMPORTANT: segment carries displayMinutes
                segments += DaySegmentUi.Beam(
                    block = b.copy(
                        // keep the original times in BeamBlockUi (for details sheet),
                        // BUT clipped flags should reflect this day's view
                        clippedTop = b.startMs < dayStartMs,
                        clippedBottom = b.endMs > dayEndMs
                    ),
                    realMinutes = beamMin,
                    displayMinutes = beamMin
                )

                cursorMs = maxOf(cursorMs, beamEndClamped)
            }
        }

        // ----- Tail gap to end of day -----
        if (cursorMs < dayEndMs) {
            val gapMin = ((dayEndMs - cursorMs) / 60_000L).toInt().coerceAtLeast(1)
            val dispMin = gapDisplayMinutes(gapMin)

            val realStartMin = ((cursorMs - dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)

            addAnchor(realStartMin, displayCursorMin)
            displayCursorMin += dispMin
            addAnchor(1440, displayCursorMin)

            segments += DaySegmentUi.Gap(
                gapMinutes = gapMin,
                displayMinutes = dispMin
            )
        } else {
            // ensure day end anchor exists
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

    // Keep short gaps honest; crush long gaps.
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
        nowMs: Long
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
            completionRatio = 0f,
            journeyColorArgb = journeyColorArgb
        )
    }

    private data class RawBeamBlock(
        val beam: BeamEntity,
        val tagName: String,
        val status: BeamStatus,
        val readiness: ReadinessLevel
    )

    private fun projectIntoHorizon(
        beam: BeamEntity,
        tagName: String,
        status: BeamStatus,
        readiness: ReadinessLevel,
        horizonStartMs: Long,
        rangeMinutes: Int,
        nowMs: Long
    ): BeamBlockUi {
        val startMinRaw = ((beam.startTime - horizonStartMs) / 60_000L).toInt()
        val endMinRaw = ((beam.endTime - horizonStartMs) / 60_000L).toInt()

        val clippedTop = startMinRaw < 0
        val clippedBottom = endMinRaw > rangeMinutes

        val clampedStart = startMinRaw.coerceIn(0, rangeMinutes)
        val clampedEnd = max(clampedStart + 1, endMinRaw.coerceIn(0, rangeMinutes))

        // Past beams always Cold Steel (ARGB int)
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
            completionRatio = 0f,
            journeyColorArgb = journeyColorArgb
        )
    }

    private fun buildHorizonTicks(horizonStartMs: Long, hours: Int): List<HorizonTickUi> {
        val zone = ZoneId.systemDefault()
        val hourFmt = DateTimeFormatter.ofPattern("h a") // "5 PM"
        val start = Instant.ofEpochMilli(horizonStartMs).atZone(zone)

        val totalMinutes = hours * 60
        val step = 15

        val ticks = mutableListOf<HorizonTickUi>()
        var m = 0
        while (m <= totalMinutes) {
            val isHour = (m % 60 == 0)
            val label = if (isHour) start.plusMinutes(m.toLong()).format(hourFmt) else ""

            ticks += HorizonTickUi(
                minuteFromStart = m,
                label = label,
                isMajor = isHour
            )
            m += step
        }
        return ticks
    }

    private val ATLAS_JOURNEY_PALETTE: List<Int> = listOf(
        0xFF8B1E1E.toInt(), // Crimson Ink (strong Gryffindor)
        0xFF3A5F8C.toInt(), // Scholar Blue (Ravenclaw, readable on black)
        0xFF2F8F86.toInt(), // Verdigris Teal (clean, modern Slytherin)
        0xFF6F9E91.toInt(), // Tempered Sage (never disappears)
        0xFFD1B45A.toInt(), // Warm Antique Gold (glows on dark)
        0xFFCC8A3E.toInt(), // Burnished Bronze (high contrast)
        0xFF7A4A32.toInt(), // Leather Umber (rich, readable)
        0xFF8C6AA8.toInt(), // Arcane Amethyst (pops on black)
        0xFF3E8F6B.toInt()  // Forest Emerald (deep but alive)
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

private data class Sexta<A,B,C,D,E,F>(val a:A,val b:B,val c:C,val d:D,val e:E,val f:F)
private data class Septa<A,B,C,D,E,F,G>(val a:A,val b:B,val c:C,val d:D,val e:E,val f:F,val g:G)

private fun floorToHour(ms: Long): Long {
    val hourMs = 60L * 60L * 1000L
    return ms - (ms % hourMs)
}
