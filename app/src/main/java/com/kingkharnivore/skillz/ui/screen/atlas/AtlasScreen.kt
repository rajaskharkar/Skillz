package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kingkharnivore.skillz.ui.model.AtlasUiState
import com.kingkharnivore.skillz.ui.model.AtlasViewMode
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.utils.time.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy")
private val WEEK_RANGE_FMT = DateTimeFormatter.ofPattern("MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasScreen(
    uiState: AtlasUiState,
    onStartFlow: (String) -> Unit,
    onSelectMode: (AtlasViewMode) -> Unit,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onScheduleBeamClick: () -> Unit,
) {
    val visibleBeams: List<BeamBlockUi> = when (uiState.viewMode) {
        AtlasViewMode.DAY -> uiState.day.beams
        AtlasViewMode.WEEK -> uiState.week.days.flatMap { it.beams }
        AtlasViewMode.MONTH -> emptyList()
    }

    var selectedBeamId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedBeam = visibleBeams.firstOrNull { it.beamId == selectedBeamId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.selectedDayStartMs, uiState.viewMode, uiState.journeyFilter) {
        selectedBeamId = null
    }

    LaunchedEffect(selectedBeamId, selectedBeam) {
        if (selectedBeamId != null && selectedBeam == null) {
            selectedBeamId = null
        }
    }

    if (selectedBeam != null) {
        val journeyColor = Color(selectedBeam.journeyColorArgb)
        val sheetBase = journeyColor.copy(alpha = 0.88f)
        val onJourney = Color.White

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { selectedBeamId = null },
            containerColor = sheetBase,
            contentColor = onJourney,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = onJourney.copy(alpha = 0.45f)
                )
            }
        ) {
            BeamDetailsSheetContent(
                b = selectedBeam,
                onClose = { selectedBeamId = null }
            )
        }
    }

    val headerData = rememberAtlasHeaderData(uiState)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageScrollState = rememberScrollState()
        val screenHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
        ) {
            NowZone(
                now = uiState.now,
                onStartFlow = onStartFlow,
                onScheduleBeamClick = onScheduleBeamClick
            )

            AtlasHeader(
                mode = uiState.viewMode,
                titleText = headerData.title,
                subtitleText = headerData.subtitle,
                canGoPrev = headerData.canGoPrev,
                showTodayButton = headerData.showTodayButton,
                onSelectMode = onSelectMode,
                onPrev = onPrevPeriod,
                onNext = onNextPeriod,
                onToday = onToday
            )

            when (uiState.viewMode) {
                AtlasViewMode.DAY -> {
                    AtlasDayStrip(
                        selectedDayStartMs = uiState.selectedDayStartMs,
                        nowMs = uiState.nowMs,
                        minSelectableDayStartMs = uiState.minSelectableDayStartMs,
                        beamsByDayStartMs = uiState.beamsByDayStartMs,
                        onSelectDay = onSelectDay
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                    ) {
                        AtlasDayPlanner(
                            day = uiState.day,
                            nowMs = uiState.nowMs,
                            onBeamClick = { beam -> selectedBeamId = beam.beamId },
                            onScheduleBeamClick = onScheduleBeamClick
                        )
                    }
                }

                AtlasViewMode.WEEK -> {
                    AtlasWeekBoard(
                        week = uiState.week,
                        selectedDayStartMs = uiState.selectedDayStartMs,
                        nowMs = uiState.nowMs,
                        onOpenDay = { dayStart ->
                            onSelectDay(dayStart)
                            onSelectMode(AtlasViewMode.DAY)
                        },
                        onBeamClick = { beam -> selectedBeamId = beam.beamId }
                    )
                }

                AtlasViewMode.MONTH -> {
                    AtlasMonthGrid(
                        month = uiState.month,
                        selectedDayStartMs = uiState.selectedDayStartMs,
                        nowMs = uiState.nowMs,
                        onDayClick = { dayStart ->
                            onSelectDay(dayStart)
                            onSelectMode(AtlasViewMode.DAY)
                        }
                    )
                }
            }
        }
    }
}

private data class AtlasHeaderData(
    val title: String,
    val subtitle: String,
    val canGoPrev: Boolean,
    val showTodayButton: Boolean
)

@Composable
private fun rememberAtlasHeaderData(uiState: AtlasUiState): AtlasHeaderData {
    val zone = ZoneId.systemDefault()
    val todayStart = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    return when (uiState.viewMode) {
        AtlasViewMode.DAY -> {
            val title = if (uiState.day.dayStartMs > 0L) {
                Instant.ofEpochMilli(uiState.day.dayStartMs).atZone(zone).format(DAY_FMT)
            } else {
                "Atlas"
            }

            val subtitle = if (uiState.day.beamsCount == 1) {
                "1 beam"
            } else {
                "${uiState.day.beamsCount} beams"
            }

            AtlasHeaderData(
                title = title,
                subtitle = subtitle,
                canGoPrev = uiState.minSelectableDayStartMs?.let {
                    uiState.selectedDayStartMs > it
                } ?: true,
                showTodayButton = uiState.selectedDayStartMs != todayStart
            )
        }

        AtlasViewMode.WEEK -> {
            val weekStart = Instant.ofEpochMilli(uiState.week.weekStartMs).atZone(zone)
            val weekEnd = Instant.ofEpochMilli(uiState.week.weekEndMs - 1L).atZone(zone)
            val title = "${weekStart.format(WEEK_RANGE_FMT)} – ${weekEnd.format(WEEK_RANGE_FMT)}"
            val subtitle = buildString {
                append(if (uiState.week.beamsCount == 1) "1 beam" else "${uiState.week.beamsCount} beams")
                append(" • ")
                append(if (uiState.week.activeDaysCount == 1) "1 active day" else "${uiState.week.activeDaysCount} active days")
                if (uiState.week.totalDurationMs > 0L) {
                    append(" • ")
                    append(formatDuration(uiState.week.totalDurationMs))
                }
            }

            val todayWeekStart = localWeekStartForUi(todayStart)
            val minWeekStart = uiState.minSelectableDayStartMs?.let { localWeekStartForUi(it) }

            AtlasHeaderData(
                title = title,
                subtitle = subtitle,
                canGoPrev = minWeekStart?.let { uiState.week.weekStartMs > it } ?: true,
                showTodayButton = uiState.week.weekStartMs != todayWeekStart
            )
        }

        AtlasViewMode.MONTH -> {
            val monthStart = Instant.ofEpochMilli(uiState.month.monthStartMs).atZone(zone)
            val title = monthStart.format(MONTH_FMT)
            val subtitle = buildString {
                append(if (uiState.month.beamsCount == 1) "1 beam" else "${uiState.month.beamsCount} beams")
                append(" • ")
                append(if (uiState.month.activeDaysCount == 1) "1 active day" else "${uiState.month.activeDaysCount} active days")
            }

            val todayMonthStart = localMonthStartForUi(todayStart)
            val minMonthStart = uiState.minSelectableDayStartMs?.let { localMonthStartForUi(it) }

            AtlasHeaderData(
                title = title,
                subtitle = subtitle,
                canGoPrev = minMonthStart?.let { uiState.month.monthStartMs > it } ?: true,
                showTodayButton = uiState.month.monthStartMs != todayMonthStart
            )
        }
    }
}

private fun localWeekStartForUi(ms: Long): Long {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val start = date.with(TemporalAdjusters.previousOrSame(first))
    return start.atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun localMonthStartForUi(ms: Long): Long {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().withDayOfMonth(1)
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}