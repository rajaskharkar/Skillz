package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.BeamEntity
import com.kingkharnivore.skillz.data.repository.BeamRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.ui.atlas.model.*
import com.kingkharnivore.skillz.ui.theme.ColdSteel
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
            .combine(nowTicker) { penta, nowMs ->
                buildUiState(
                    beams = penta.a,
                    tagData = penta.b,
                    filter = penta.c,
                    horizonStart = penta.d,
                    horizonHours = penta.e,
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

        // NOW state from ALL filtered beams (not just horizon)
        val activeRaw = filtered.firstOrNull { it.status == BeamStatus.ACTIVE }
        val nextRaw = filtered
            .filter { it.beam.startTime > nowMs }
            .minByOrNull { it.beam.startTime }

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

private fun floorToHour(ms: Long): Long {
    val hourMs = 60L * 60L * 1000L
    return ms - (ms % hourMs)
}
