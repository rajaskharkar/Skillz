package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.model.AtlasDayUi
import com.kingkharnivore.skillz.ui.model.AtlasMonthCellUi
import com.kingkharnivore.skillz.ui.model.AtlasMonthUi
import com.kingkharnivore.skillz.ui.model.AtlasUiState
import com.kingkharnivore.skillz.ui.model.AtlasViewMode
import com.kingkharnivore.skillz.ui.model.AtlasWeekDayUi
import com.kingkharnivore.skillz.ui.model.AtlasWeekUi
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.model.BeamStatus
import com.kingkharnivore.skillz.ui.model.JourneyChipUi
import com.kingkharnivore.skillz.ui.model.JourneyFilter
import com.kingkharnivore.skillz.ui.model.NowState
import com.kingkharnivore.skillz.ui.model.ReadinessLevel
import com.kingkharnivore.skillz.ui.model.computeBeamStatus
import com.kingkharnivore.skillz.ui.model.computeReadiness
import com.kingkharnivore.skillz.ui.model.overlaps
import com.kingkharnivore.skillz.ui.model.progress
import com.kingkharnivore.skillz.ui.theme.ColdSteel
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
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@HiltViewModel
class AtlasViewModel @Inject constructor(
    private val beamRepository: BeamRepository,
    private val journeyRepository: JourneyRepository,
    private val sessionRepository: FlowRepository
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val journeyFilter = MutableStateFlow<JourneyFilter>(JourneyFilter.All)
    private val horizonHours = MutableStateFlow(8)
    private val horizonStartMs = MutableStateFlow(floorToHour(System.currentTimeMillis()))
    private val viewMode = MutableStateFlow(AtlasViewMode.DAY)
    private val selectedDayStartMs = MutableStateFlow(localDayStart(System.currentTimeMillis()))

    @Volatile
    private var minDayStartCache: Long? = null

    fun setViewMode(mode: AtlasViewMode) = viewMode.update { mode }

    fun selectDay(dayStartMs: Long) {
        selectedDayStartMs.update { dayStartMs }
    }

    fun shiftSelectedPeriod(delta: Long) {
        selectedDayStartMs.update { current ->
            when (viewMode.value) {
                AtlasViewMode.DAY -> {
                    val next = plusLocalDays(current, delta)
                    val min = minDayStartCache
                    if (min != null && next < min) min else next
                }
                AtlasViewMode.WEEK -> {
                    val next = plusLocalWeeks(current, delta)
                    val min = minDayStartCache?.let { localWeekStart(it) }
                    if (min != null && next < min) min else next
                }
                AtlasViewMode.MONTH -> {
                    val next = plusLocalMonths(current, delta)
                    val min = minDayStartCache?.let { localMonthStart(it) }
                    if (min != null && next < min) min else next
                }
            }
        }
    }

    fun goToToday() {
        selectedDayStartMs.update { localDayStart(System.currentTimeMillis()) }
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
            .combine(sessionRepository.getAllSessions()) { beams, sessions -> beams to sessions }
            .combine(tagsFlow) { (beams, sessions), tagData -> Triple(beams, sessions, tagData) }
            .combine(journeyFilter) { (beams, sessions, tagData), filter ->
                Quad(beams, sessions, tagData, filter)
            }
            .combine(horizonStartMs) { quad, start ->
                Penta(quad.a, quad.b, quad.c, quad.d, start)
            }
            .combine(horizonHours) { penta, hours ->
                Sexta(penta.a, penta.b, penta.c, penta.d, penta.e, hours)
            }
            .combine(viewMode) { sexta, mode ->
                Septa(sexta.a, sexta.b, sexta.c, sexta.d, sexta.e, sexta.f, mode)
            }
            .combine(selectedDayStartMs) { septa, dayStart ->
                Octa(septa.a, septa.b, septa.c, septa.d, septa.e, septa.f, septa.g, dayStart)
            }
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
        val rangeMinutes = hours * 60

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

        val minSelectableDay = filtered
            .flatMap { overlappingDayStarts(it.beam.startTime, it.beam.endTime) }
            .minOrNull()

        minDayStartCache = minSelectableDay

        val effectiveSelectedDayStartMs = when {
            minSelectableDay != null && selectedDayStartMs < minSelectableDay -> minSelectableDay
            else -> selectedDayStartMs
        }

        val activeRaw = filtered.firstOrNull { it.status == BeamStatus.ACTIVE }
        val nextRaw = filtered
            .filter { it.beam.startTime > nowMs }
            .minByOrNull { it.beam.startTime }

        val day = buildDay(
            filtered = filtered,
            dayStartMs = effectiveSelectedDayStartMs,
            nowMs = nowMs
        )

        val week = buildWeek(
            filtered = filtered,
            selectedDayStartMs = effectiveSelectedDayStartMs,
            nowMs = nowMs
        )

        val month = buildMonth(
            filtered = filtered,
            selectedDayStartMs = effectiveSelectedDayStartMs,
            nowMs = nowMs
        )

        val beamsByDayStartMs = buildBeamCountsByDay(filtered)

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
                activeBeamProgress = activeRaw?.let {
                    progress(nowMs, it.beam.startTime, it.beam.endTime)
                }
            ),
            nowMs = nowMs,
            viewMode = viewMode,
            selectedDayStartMs = effectiveSelectedDayStartMs,
            day = day,
            week = week,
            month = month,
            beamsByDayStartMs = beamsByDayStartMs,
            minSelectableDayStartMs = minSelectableDay,
        )
    }

    private fun buildDay(
        filtered: List<RawBeamBlock>,
        dayStartMs: Long,
        nowMs: Long
    ): AtlasDayUi {
        val dayEndMs = nextLocalDayStart(dayStartMs)
        val windowMinutes = ((dayEndMs - dayStartMs) / 60_000L).toInt().coerceAtLeast(1)

        val dayBeams = filtered
            .filter { raw -> overlaps(raw.beam.startTime, raw.beam.endTime, dayStartMs, dayEndMs) }
            .sortedBy { it.beam.startTime }
            .map { raw ->
                projectToWindow(
                    beam = raw.beam,
                    tagName = raw.tagName,
                    status = raw.status,
                    readiness = raw.readiness,
                    windowStartMs = dayStartMs,
                    windowMinutes = windowMinutes,
                    nowMs = nowMs,
                    completionRatio = raw.completionRatio
                ).copy(
                    clippedTop = raw.beam.startTime < dayStartMs,
                    clippedBottom = raw.beam.endTime > dayEndMs
                )
            }

        return AtlasDayUi(
            dayStartMs = dayStartMs,
            dayEndMs = dayEndMs,
            beams = dayBeams,
            beamsCount = dayBeams.size
        )
    }

    private fun buildWeek(
        filtered: List<RawBeamBlock>,
        selectedDayStartMs: Long,
        nowMs: Long
    ): AtlasWeekUi {
        val weekStartMs = localWeekStart(selectedDayStartMs)
        val weekEndMs = plusLocalDays(weekStartMs, 7)

        val days = (0 until 7).map { index ->
            val dayStartMs = plusLocalDays(weekStartMs, index.toLong())
            val dayEndMs = nextLocalDayStart(dayStartMs)
            val windowMinutes = ((dayEndMs - dayStartMs) / 60_000L).toInt().coerceAtLeast(1)

            val dayRaw = filtered
                .filter { raw -> overlaps(raw.beam.startTime, raw.beam.endTime, dayStartMs, dayEndMs) }
                .sortedBy { it.beam.startTime }

            val dayBeams = dayRaw.map { raw ->
                projectToWindow(
                    beam = raw.beam,
                    tagName = raw.tagName,
                    status = raw.status,
                    readiness = raw.readiness,
                    windowStartMs = dayStartMs,
                    windowMinutes = windowMinutes,
                    nowMs = nowMs,
                    completionRatio = raw.completionRatio
                ).copy(
                    clippedTop = raw.beam.startTime < dayStartMs,
                    clippedBottom = raw.beam.endTime > dayEndMs
                )
            }

            AtlasWeekDayUi(
                dayStartMs = dayStartMs,
                dayEndMs = dayEndMs,
                beams = dayBeams,
                beamsCount = dayBeams.size,
                totalDurationMs = dayRaw.sumOf {
                    overlapMs(it.beam.startTime, it.beam.endTime, dayStartMs, dayEndMs)
                }
            )
        }

        val weekDistinctBeamIds = filtered
            .filter { overlaps(it.beam.startTime, it.beam.endTime, weekStartMs, weekEndMs) }
            .map { it.beam.id }
            .distinct()

        return AtlasWeekUi(
            weekStartMs = weekStartMs,
            weekEndMs = weekEndMs,
            days = days,
            beamsCount = weekDistinctBeamIds.size,
            activeDaysCount = days.count { it.beamsCount > 0 },
            totalDurationMs = days.sumOf { it.totalDurationMs }
        )
    }

    private fun buildMonth(
        filtered: List<RawBeamBlock>,
        selectedDayStartMs: Long,
        nowMs: Long
    ): AtlasMonthUi {
        val monthStartMs = localMonthStart(selectedDayStartMs)
        val monthEndMs = nextLocalMonthStart(monthStartMs)

        val monthStartDate = Instant.ofEpochMilli(monthStartMs).atZone(zoneId).toLocalDate()
        val gridStartDate = monthStartDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val gridStartMs = gridStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val cells = (0 until 42).map { index ->
            val cellStartMs = plusLocalDays(gridStartMs, index.toLong())
            val cellEndMs = nextLocalDayStart(cellStartMs)
            val cellDate = Instant.ofEpochMilli(cellStartMs).atZone(zoneId).toLocalDate()

            val dayRaw = filtered
                .filter { raw -> overlaps(raw.beam.startTime, raw.beam.endTime, cellStartMs, cellEndMs) }
                .sortedBy { it.beam.startTime }

            val previewColors = dayRaw
                .map { raw ->
                    if (raw.beam.endTime <= nowMs) ColdSteel else colorForTagId(raw.beam.tagId)
                }
                .distinct()
                .take(3)

            AtlasMonthCellUi(
                dayStartMs = cellStartMs,
                isInCurrentMonth = cellDate.monthValue == monthStartDate.monthValue &&
                        cellDate.year == monthStartDate.year,
                beamsCount = dayRaw.map { it.beam.id }.distinct().size,
                totalDurationMs = dayRaw.sumOf {
                    overlapMs(it.beam.startTime, it.beam.endTime, cellStartMs, cellEndMs)
                },
                previewColors = previewColors
            )
        }

        val monthDistinctBeamIds = filtered
            .filter { overlaps(it.beam.startTime, it.beam.endTime, monthStartMs, monthEndMs) }
            .map { it.beam.id }
            .distinct()

        return AtlasMonthUi(
            monthStartMs = monthStartMs,
            monthEndMs = monthEndMs,
            cells = cells,
            beamsCount = monthDistinctBeamIds.size,
            activeDaysCount = cells.count { it.isInCurrentMonth && it.beamsCount > 0 }
        )
    }

    private fun buildBeamCountsByDay(
        filtered: List<RawBeamBlock>
    ): Map<Long, Int> {
        return filtered
            .flatMap { raw ->
                overlappingDayStarts(raw.beam.startTime, raw.beam.endTime)
                    .map { dayStart -> dayStart to raw.beam.id }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) -> ids.distinct().size }
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
        return projectToWindow(
            beam = beam,
            tagName = tagName,
            status = status,
            readiness = readiness,
            windowStartMs = horizonStartMs,
            windowMinutes = rangeMinutes,
            nowMs = nowMs,
            completionRatio = completionRatio
        )
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
        val clampedEnd = max(clampedStart + 1, endMinRaw.coerceIn(0, windowMinutes))

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

    private fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val s = maxOf(aStart, bStart)
        val e = minOf(aEnd, bEnd)
        return (e - s).coerceAtLeast(0L)
    }

    private fun localDayStart(ms: Long): Long {
        return Instant.ofEpochMilli(ms)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun nextLocalDayStart(dayStartMs: Long): Long {
        val date = Instant.ofEpochMilli(dayStartMs).atZone(zoneId).toLocalDate()
        return date.plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun plusLocalDays(dayStartMs: Long, deltaDays: Long): Long {
        val date = Instant.ofEpochMilli(dayStartMs).atZone(zoneId).toLocalDate()
        return date.plusDays(deltaDays)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun localWeekStart(ms: Long): Long {
        val date = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
        val weekStart = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun plusLocalWeeks(ms: Long, deltaWeeks: Long): Long {
        val date = Instant.ofEpochMilli(localWeekStart(ms)).atZone(zoneId).toLocalDate()
        return date.plusWeeks(deltaWeeks)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun localMonthStart(ms: Long): Long {
        val date = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate().withDayOfMonth(1)
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun nextLocalMonthStart(monthStartMs: Long): Long {
        val date = Instant.ofEpochMilli(monthStartMs).atZone(zoneId).toLocalDate().withDayOfMonth(1)
        return date.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun plusLocalMonths(ms: Long, deltaMonths: Long): Long {
        val date = Instant.ofEpochMilli(localMonthStart(ms)).atZone(zoneId).toLocalDate()
        return date.plusMonths(deltaMonths)
            .withDayOfMonth(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun overlappingDayStarts(startMs: Long, endMs: Long): List<Long> {
        val safeEndMs = maxOf(startMs + 1L, endMs)
        val lastIncludedMs = safeEndMs - 1L

        var date = Instant.ofEpochMilli(startMs).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(lastIncludedMs).atZone(zoneId).toLocalDate()

        val result = mutableListOf<Long>()
        while (!date.isAfter(endDate)) {
            result += date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            date = date.plusDays(1)
        }
        return result
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

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Penta<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private data class Sexta<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
private data class Septa<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)
private data class Octa<A, B, C, D, E, F, G, H>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H)

private fun floorToHour(ms: Long): Long {
    val hourMs = 60L * 60L * 1000L
    return ms - (ms % hourMs)
}