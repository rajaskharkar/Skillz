package com.kingkharnivore.skillz.ui.screen.atlas.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.model.AtlasDayUi
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.screen.atlas.BeamCard
import com.kingkharnivore.skillz.utils.time.formatRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged

private data class PlacedBeamUi(
    val beam: BeamBlockUi,
    val laneIndex: Int,
    val laneCount: Int
)

private data class HourMark(
    val label: String,
    val minuteOffset: Int
)

private data class BeamRenderSpec(
    val beam: BeamBlockUi,
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp
)

private data class BeamHitRect(
    val beam: BeamBlockUi,
    val leftPx: Float,
    val topPx: Float,
    val rightPx: Float,
    val bottomPx: Float
) {
    val areaPx: Float get() = (rightPx - leftPx) * (bottomPx - topPx)
}

@Composable
fun AtlasDayPlanner(
    day: AtlasDayUi,
    nowMs: Long,
    onBeamClick: (BeamBlockUi) -> Unit,
    modifier: Modifier = Modifier,
    onScrollOffsetChanged: (Int) -> Unit = {},
    onInitialScrollAnchorResolved: (Int) -> Unit = {},
    onScheduleBeamClick: () -> Unit = {}
) {
    var hourHeightDp by rememberSaveable { mutableStateOf(76f) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { onScrollOffsetChanged(it) }
    }

    val totalMinutes = remember(day.dayStartMs, day.dayEndMs) {
        ((day.dayEndMs - day.dayStartMs) / 60_000L).toInt().coerceAtLeast(1)
    }
    val hourHeight = hourHeightDp.dp
    val totalHeight = hourHeight * (totalMinutes / 60f)
    val placedBeams = remember(day.beams) { placeBeamsForDay(day.beams) }
    val hourMarks = remember(day.dayStartMs, day.dayEndMs) {
        buildHourMarks(day.dayStartMs, day.dayEndMs)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeightPx = constraints.maxHeight.toFloat()
        var previousHourHeightDp by remember { mutableFloatStateOf(hourHeightDp) }

        LaunchedEffect(day.dayStartMs) {
            val targetMinute = when {
                nowMs in day.dayStartMs until day.dayEndMs -> minuteOffset(day.dayStartMs, nowMs)
                day.beams.isNotEmpty() -> minuteOffset(day.dayStartMs, day.beams.first().startMs).coerceAtLeast(0)
                else -> 0
            }

            val pxPerMinute = with(density) { hourHeight.toPx() / 60f }
            val targetPx = (targetMinute * pxPerMinute - viewportHeightPx * 0.25f)
                .roundToInt()
                .coerceAtLeast(0)

            scrollState.scrollTo(targetPx)
            onInitialScrollAnchorResolved(scrollState.value)
        }

        LaunchedEffect(hourHeightDp) {
            val oldHourHeightDp = previousHourHeightDp
            val newHourHeightDp = hourHeightDp
            if (oldHourHeightDp == newHourHeightDp) return@LaunchedEffect

            val oldPxPerMinute = with(density) { oldHourHeightDp.dp.toPx() / 60f }
            val newPxPerMinute = with(density) { newHourHeightDp.dp.toPx() / 60f }

            val currentCenterMinute = ((scrollState.value + viewportHeightPx / 2f) / oldPxPerMinute)
                .coerceIn(0f, totalMinutes.toFloat())

            val targetPx = (currentCenterMinute * newPxPerMinute - viewportHeightPx * 0.25f)
                .roundToInt()
                .coerceAtLeast(0)

            previousHourHeightDp = newHourHeightDp
            scrollState.scrollTo(targetPx)
        }

        Box(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .verticalScroll(scrollState)
            ) {
                AtlasTimeRail(
                    hourMarks = hourMarks,
                    totalHeight = totalHeight,
                    hourHeight = hourHeight,
                    modifier = Modifier.width(72.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 10.dp)
                ) {
                    AtlasDayGrid(
                        totalMinutes = totalMinutes,
                        modifier = Modifier.fillMaxSize()
                    )

                    AtlasBeamLayer(
                        day = day,
                        hourHeight = hourHeight,
                        placedBeams = placedBeams,
                        onBeamClick = onBeamClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    AtlasNowLine(
                        day = day,
                        nowMs = nowMs,
                        hourHeight = hourHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            AtlasZoomControls(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onZoomIn = {
                    hourHeightDp = (hourHeightDp + 12f).coerceAtMost(132f)
                },
                onZoomOut = {
                    hourHeightDp = (hourHeightDp - 12f).coerceAtLeast(56f)
                },
                onScheduleBeamClick = onScheduleBeamClick
            )
        }
    }
}

@Composable
private fun AtlasTimeRail(
    hourMarks: List<HourMark>,
    totalHeight: Dp,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(totalHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        hourMarks.forEach { mark ->
            Text(
                text = mark.label,
                modifier = Modifier.offset(y = hourHeight * (mark.minuteOffset / 60f)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun AtlasDayGrid(
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val major = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val minor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Canvas(modifier = modifier) {
        for (minute in 0..totalMinutes step 30) {
            val y = size.height * (minute / totalMinutes.toFloat())
            drawLine(
                color = if (minute % 60 == 0) major else minor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (minute % 60 == 0) 1.2f else 1f
            )
        }
    }
}

@Composable
private fun AtlasBeamLayer(
    day: AtlasDayUi,
    hourHeight: Dp,
    placedBeams: List<PlacedBeamUi>,
    onBeamClick: (BeamBlockUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val sidePadding = 8.dp
        val laneGap = 6.dp
        val usableWidth = maxWidth - sidePadding * 2

        val renderSpecs = placedBeams.map { placed ->
            val beam = placed.beam
            val startMs = maxOf(beam.startMs, day.dayStartMs)
            val endMs = minOf(beam.endMs, day.dayEndMs)

            val startMinute = minuteOffset(day.dayStartMs, startMs).coerceAtLeast(0)
            val durationMinutes = ((endMs - startMs) / 60_000L).toInt().coerceAtLeast(1)

            val y = hourHeight * (startMinute / 60f)
            val height = hourHeight * (durationMinutes / 60f)
            val width = (usableWidth - laneGap * (placed.laneCount - 1)) / placed.laneCount
            val x = sidePadding + (width + laneGap) * placed.laneIndex

            BeamRenderSpec(
                beam = beam,
                x = x,
                y = y,
                width = width,
                height = height
            )
        }

        val hitRects = with(density) {
            renderSpecs.map { spec ->
                BeamHitRect(
                    beam = spec.beam,
                    leftPx = spec.x.toPx(),
                    topPx = spec.y.toPx(),
                    rightPx = spec.x.toPx() + spec.width.toPx(),
                    bottomPx = spec.y.toPx() + spec.height.toPx()
                )
            }
        }
        val hitSlopPx = with(density) { 18.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hitRects) {
                    detectTapGestures { position ->
                        findBeamAtTap(position, hitRects, hitSlopPx)?.let(onBeamClick)
                    }
                }
        ) {
            renderSpecs.forEach { spec ->
                val beamA11y = stringResource(
                    R.string.atlas_day_planner_beam_card_a11y,
                    spec.beam.tagName,
                    formatRange(spec.beam.startMs, spec.beam.endMs)
                )

                BeamCard(
                    b = spec.beam,
                    h = spec.height,
                    onBeamClick = onBeamClick,
                    modifier = Modifier
                        .offset(x = spec.x, y = spec.y)
                        .width(spec.width)
                        .semantics {
                            role = Role.Button
                            contentDescription = beamA11y
                            onClick {
                                onBeamClick(spec.beam)
                                true
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun AtlasNowLine(
    day: AtlasDayUi,
    nowMs: Long,
    hourHeight: Dp,
    modifier: Modifier = Modifier
) {
    if (nowMs !in day.dayStartMs until day.dayEndMs) return

    val minute = minuteOffset(day.dayStartMs, nowMs).coerceAtLeast(0)
    val y = hourHeight * (minute / 60f)

    Box(
        modifier = modifier
            .offset(y = y)
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun AtlasZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val zoomOutA11y = stringResource(R.string.atlas_zoom_out_a11y)
    val zoomInA11y = stringResource(R.string.atlas_zoom_in_a11y)
    val scheduleBeamA11y = stringResource(R.string.atlas_zoom_schedule_beam_a11y)

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = cs.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallFloatingActionButton(
                onClick = onZoomOut,
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
                modifier = Modifier.semantics {
                    contentDescription = zoomOutA11y
                }
            ) {
                Text(stringResource(R.string.atlas_day_planner_loading_hour_label_minus))
            }

            Box(Modifier.width(8.dp))

            SmallFloatingActionButton(
                onClick = onZoomIn,
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
                modifier = Modifier.semantics {
                    contentDescription = zoomInA11y
                }
            ) {
                Text(stringResource(R.string.atlas_day_planner_loading_hour_label_plus))
            }

            Box(Modifier.width(12.dp))

            SmallFloatingActionButton(
                onClick = onScheduleBeamClick,
                containerColor = cs.secondary,
                contentColor = cs.onSecondary,
                modifier = Modifier.semantics {
                    contentDescription = scheduleBeamA11y
                }
            ) {
                Text(stringResource(R.string.atlas_day_planner_schedule_beam_icon))
            }
        }
    }
}

private fun placeBeamsForDay(beams: List<BeamBlockUi>): List<PlacedBeamUi> {
    if (beams.isEmpty()) return emptyList()

    val sorted = beams.sortedWith(compareBy<BeamBlockUi> { it.startMs }.thenBy { it.endMs })
    val clusters = mutableListOf<List<BeamBlockUi>>()

    var cluster = mutableListOf<BeamBlockUi>()
    var clusterEnd = Long.MIN_VALUE

    for (beam in sorted) {
        if (cluster.isEmpty()) {
            cluster += beam
            clusterEnd = beam.endMs
        } else if (beam.startMs < clusterEnd) {
            cluster += beam
            clusterEnd = maxOf(clusterEnd, beam.endMs)
        } else {
            clusters += cluster.toList()
            cluster = mutableListOf(beam)
            clusterEnd = beam.endMs
        }
    }
    if (cluster.isNotEmpty()) clusters += cluster.toList()

    return clusters.flatMap { group ->
        val active = mutableListOf<Pair<BeamBlockUi, Int>>()
        val placed = mutableListOf<Pair<BeamBlockUi, Int>>()
        var maxLanes = 1

        group.forEach { beam ->
            active.removeAll { it.first.endMs <= beam.startMs }
            val used = active.map { it.second }.toSet()
            val lane = generateSequence(0) { it + 1 }.first { it !in used }

            active += beam to lane
            placed += beam to lane
            maxLanes = maxOf(maxLanes, active.size)
        }

        placed.map { (beam, lane) ->
            PlacedBeamUi(
                beam = beam,
                laneIndex = lane,
                laneCount = maxLanes
            )
        }
    }
}

private fun findBeamAtTap(
    position: Offset,
    rects: List<BeamHitRect>,
    hitSlopPx: Float
): BeamBlockUi? {
    val direct = rects.filter {
        position.x in it.leftPx..it.rightPx &&
                position.y in it.topPx..it.bottomPx
    }

    if (direct.isNotEmpty()) {
        return direct.minByOrNull { it.areaPx }?.beam
    }

    return rects
        .mapNotNull { rect ->
            val dx = when {
                position.x < rect.leftPx -> rect.leftPx - position.x
                position.x > rect.rightPx -> position.x - rect.rightPx
                else -> 0f
            }
            val dy = when {
                position.y < rect.topPx -> rect.topPx - position.y
                position.y > rect.bottomPx -> position.y - rect.bottomPx
                else -> 0f
            }

            val distance = hypot(dx, dy)
            if (distance <= hitSlopPx) rect to distance else null
        }
        .minWithOrNull(
            compareBy<Pair<BeamHitRect, Float>> { it.second }
                .thenBy { it.first.areaPx }
        )
        ?.first
        ?.beam
}

private fun buildHourMarks(dayStartMs: Long, dayEndMs: Long): List<HourMark> {
    val zone = ZoneId.systemDefault()
    val basicFormatter = DateTimeFormatter.ofPattern("h a")
    val dstFormatter = DateTimeFormatter.ofPattern("h a z")

    var cursor = Instant.ofEpochMilli(dayStartMs).atZone(zone)
    val end = Instant.ofEpochMilli(dayEndMs).atZone(zone)

    val probe = mutableListOf<Pair<String, Int>>()
    while (cursor.isBefore(end)) {
        probe += cursor.format(basicFormatter) to minuteOffset(dayStartMs, cursor.toInstant().toEpochMilli())
        cursor = cursor.plusHours(1)
    }

    val hasDuplicates = probe.groupingBy { it.first }.eachCount().any { it.value > 1 }
    val useDstFormatter = hasDuplicates || (dayEndMs - dayStartMs) != 24L * 60L * 60L * 1000L
    val formatter = if (useDstFormatter) dstFormatter else basicFormatter

    cursor = Instant.ofEpochMilli(dayStartMs).atZone(zone)
    val result = mutableListOf<HourMark>()
    while (cursor.isBefore(end)) {
        val ms = cursor.toInstant().toEpochMilli()
        result += HourMark(
            label = cursor.format(formatter),
            minuteOffset = minuteOffset(dayStartMs, ms).coerceAtLeast(0)
        )
        cursor = cursor.plusHours(1)
    }
    return result
}

private fun minuteOffset(dayStartMs: Long, pointMs: Long): Int {
    return ((pointMs - dayStartMs) / 60_000L).toInt()
}