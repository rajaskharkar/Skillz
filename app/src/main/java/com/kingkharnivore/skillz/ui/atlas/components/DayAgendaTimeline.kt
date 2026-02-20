package com.kingkharnivore.skillz.ui.atlas.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.ui.atlas.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.atlas.model.DayPlanUi
import com.kingkharnivore.skillz.ui.atlas.model.DaySegmentUi
import com.kingkharnivore.skillz.utils.time.formatRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun DayAgendaTimeline(
    dayPlan: DayPlanUi,
    onAdvanceDay: (deltaDays: Long) -> Unit,
    onBeamClick: (BeamBlockUi) -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val railWidth = 72.dp
    val dpPerMin = 1.8.dp
    val sentinelHeight = 96.dp

    // Must match LazyColumn contentPadding:
    val outerStartPadding = 16.dp
    val outerEndPadding = 16.dp
    val listTopPadding = 10.dp
    val listBottomPadding = 10.dp

    var suppressDayAdvance by remember { mutableStateOf(false) }
    var lastAdvanceAtMs by remember { mutableStateOf(0L) }

    // Scroll to current time when day changes
    LaunchedEffect(dayPlan.dayStartMs) {
        val now = System.currentTimeMillis()
        val dayEnd = dayPlan.dayStartMs + 24 * 60 * 60 * 1000L

        if (now in dayPlan.dayStartMs until dayEnd) {
            val minute = ((now - dayPlan.dayStartMs) / 60_000L).toInt().coerceIn(0, 1440)
            val pxPerMin = with(density) { dpPerMin.toPx() }
            val yPx = (minute * pxPerMin).toInt()
            listState.scrollToItem(index = 1, scrollOffset = yPx)
        } else {
            listState.scrollToItem(index = 1, scrollOffset = 0)
        }
    }

    // Auto-advance during scroll (when you hit sentinel padding)
    // Auto-advance during scroll (when you hit sentinel padding)
    LaunchedEffect(dayPlan.dayStartMs) {
        val edgeThresholdPx = with(density) { 28.dp.toPx() }.toInt()
        val cooldownMs = 650L

        snapshotFlow { listState.layoutInfo }
            .collect { info ->
                if (!listState.isScrollInProgress) return@collect
                if (suppressDayAdvance) return@collect

                val total = info.totalItemsCount
                if (total <= 0) return@collect

                val now = System.currentTimeMillis()
                if (now - lastAdvanceAtMs < cooldownMs) return@collect

                val viewportStart = info.viewportStartOffset
                val viewportEnd = info.viewportEndOffset

                val first = info.visibleItemsInfo.firstOrNull()
                val last = info.visibleItemsInfo.lastOrNull()

                val topSentinelIndex = 0
                val bottomSentinelIndex = total - 1

                val isAtTopSentinel =
                    first?.index == topSentinelIndex &&
                            (first.offset - viewportStart) <= edgeThresholdPx

                val isAtBottomSentinel =
                    last?.index == bottomSentinelIndex &&
                            (viewportEnd - (last.offset + last.size)) <= edgeThresholdPx

                when {
                    isAtTopSentinel -> {
                        suppressDayAdvance = true
                        lastAdvanceAtMs = now
                        onAdvanceDay(-1)
                        delay(220)
                        suppressDayAdvance = false
                    }

                    isAtBottomSentinel -> {
                        suppressDayAdvance = true
                        lastAdvanceAtMs = now
                        onAdvanceDay(+1)
                        delay(220)
                        suppressDayAdvance = false
                    }
                }
            }
    }

    // Snap off a gap when scroll settles (so user doesn’t land inside dead space)
    LaunchedEffect(dayPlan.dayStartMs) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (inProgress) return@collect
            if (suppressDayAdvance) return@collect

            val first = listState.firstVisibleItemIndex
            if (first <= 0 || first > dayPlan.segments.size) return@collect

            val seg = dayPlan.segments[first - 1]
            if (seg is DaySegmentUi.Gap) {
                listState.animateScrollToItem(first + 1)
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds() // ✅ prevents background bleeding into other zones
    ) {

        // ✅ Background grid aligned to the same paddings as the list content
        DayTimeGridBackground(
            dayPlan = dayPlan,
            listState = listState,
            dpPerMin = dpPerMin,
            railWidth = railWidth,
            sentinelHeight = sentinelHeight,
            listTopPadding = listTopPadding,
            listBottomPadding = listBottomPadding,
            listOuterStartPadding = outerStartPadding
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = outerStartPadding + railWidth,
                end = outerEndPadding,
                top = listTopPadding,
                bottom = listBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Spacer(Modifier.height(sentinelHeight)) }

            items(dayPlan.segments) { segment ->
                when (segment) {
                    is DaySegmentUi.Gap -> {
                        Spacer(Modifier.height(segment.displayMinutes * dpPerMin))
                    }
                    is DaySegmentUi.Beam -> {
                        BeamCard(
                            b = segment.block,
                            h = (segment.displayMinutes * dpPerMin),
                            onBeamClick = onBeamClick
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(sentinelHeight)) }
        }
    }
}

/* ----------------------------- Beam card ----------------------------- */

@Composable
private fun BeamCard(
    b: BeamBlockUi,
    h: Dp,
    onBeamClick: (BeamBlockUi) -> Unit
) {
    val journeyColor = Color(b.journeyColorArgb)
    val bg = journeyColor.copy(alpha = 0.16f)
    val accent = journeyColor.copy(alpha = 0.9f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(h),
        colors = CardDefaults.cardColors(
            containerColor = bg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { onBeamClick(b) }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = b.tagName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)
                Text(
                    text = "${formatRange(b.startMs, b.endMs)} • ${mins}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatGap(mins: Int): String {
    val h = mins / 60
    val m = mins % 60
    return when {
        h <= 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/* ------------------------- Background grid ------------------------ */

@Composable
private fun DayTimeGridBackground(
    dayPlan: DayPlanUi,
    listState: LazyListState,
    dpPerMin: Dp,
    railWidth: Dp,
    sentinelHeight: Dp,
    listTopPadding: Dp,
    listBottomPadding: Dp,
    listOuterStartPadding: Dp
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Prefix sum of display minutes for segment starts (sentinels excluded)
    val prefix = remember(dayPlan.dayStartMs, dayPlan.segments) {
        val p = IntArray(dayPlan.segments.size + 1)
        var acc = 0
        for (i in dayPlan.segments.indices) {
            val seg = dayPlan.segments[i]
            val dm = when (seg) {
                is DaySegmentUi.Gap -> seg.displayMinutes
                is DaySegmentUi.Beam -> ((seg.block.endMs - seg.block.startMs) / 60_000L).toInt().coerceAtLeast(1)
            }
            acc += dm
            p[i + 1] = acc
        }
        p
    }

    // Real minute-of-day -> display minute (compressed) via anchors
    fun minuteToDisplay(minuteOfDay: Int): Int {
        val m = minuteOfDay.coerceIn(0, 1440)
        val a = dayPlan.anchors
        if (a.isEmpty()) return m

        var prev = a.first()
        for (i in 1 until a.size) {
            val next = a[i]
            if (m <= next.minuteOfDay) {
                val span = (next.minuteOfDay - prev.minuteOfDay).coerceAtLeast(1)
                val t = (m - prev.minuteOfDay).toFloat() / span.toFloat()
                return (prev.displayMinute + t * (next.displayMinute - prev.displayMinute)).toInt()
            }
            prev = next
        }
        return a.last().displayMinute
    }

    // Compute the display-minute at the top of the drawable timeline region
    val topDisplayMinute by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val firstVisible = info.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0

            // index 0 is top sentinel spacer
            if (firstVisible.index == 0) return@derivedStateOf 0

            // last index is bottom sentinel spacer
            if (firstVisible.index >= info.totalItemsCount - 1) {
                return@derivedStateOf dayPlan.totalDisplayMinutes
            }

            // segment index in dayPlan.segments (because of top sentinel)
            val segIndex = (firstVisible.index - 1).coerceIn(0, dayPlan.segments.lastIndex)
            val baseDisplay = prefix[segIndex]

            // How many "display minutes" does THIS segment represent?
            val segDisplayMinutes = when (val seg = dayPlan.segments[segIndex]) {
                is DaySegmentUi.Gap -> seg.displayMinutes
                is DaySegmentUi.Beam -> seg.displayMinutes
            }.coerceAtLeast(1)

            // How far into this item are we, based on actual rendered px?
            val pxIntoItem = listState.firstVisibleItemScrollOffset.toFloat().coerceAtLeast(0f)
            val itemPx = firstVisible.size.toFloat().coerceAtLeast(1f)

            val frac = (pxIntoItem / itemPx).coerceIn(0f, 1f)
            val intoMinutes = (frac * segDisplayMinutes).toInt()

            (baseDisplay + intoMinutes)
                .coerceIn(0, max(0, dayPlan.totalDisplayMinutes))
        }
    }

    // Compute the display-minute at the top of the drawable timeline region (pixel-accurate)
    val topDisplayMinuteFloat by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f

            // 0 = top sentinel spacer
            if (first.index == 0) return@derivedStateOf 0f

            // last = bottom sentinel spacer
            if (first.index >= info.totalItemsCount - 1) return@derivedStateOf dayPlan.totalDisplayMinutes.toFloat()

            val segIndex = (first.index - 1).coerceIn(0, dayPlan.segments.lastIndex)
            val baseDisplay = prefix[segIndex].toFloat()

            val segDisplayMinutes = when (val seg = dayPlan.segments[segIndex]) {
                is DaySegmentUi.Gap -> seg.displayMinutes
                is DaySegmentUi.Beam -> seg.displayMinutes
            }.coerceAtLeast(1)

            val pxIntoItem = listState.firstVisibleItemScrollOffset.toFloat().coerceAtLeast(0f)
            val itemPx = first.size.toFloat().coerceAtLeast(1f)

            val frac = (pxIntoItem / itemPx).coerceIn(0f, 1f)

            (baseDisplay + frac * segDisplayMinutes.toFloat())
                .coerceIn(0f, dayPlan.totalDisplayMinutes.toFloat())
        }
    }

    fun displayMinuteToViewportY(displayMinute: Int): Float? {
        val info = listState.layoutInfo
        val items = info.visibleItemsInfo
        if (items.isEmpty()) return null

        // Skip sentinels (0 and last)
        for (it in items) {
            if (it.index == 0) continue
            if (it.index >= info.totalItemsCount - 1) continue

            val segIndex = (it.index - 1).coerceIn(0, dayPlan.segments.lastIndex)
            val segStart = prefix[segIndex]
            val segMinutes = when (val seg = dayPlan.segments[segIndex]) {
                is DaySegmentUi.Gap -> seg.displayMinutes
                is DaySegmentUi.Beam -> seg.displayMinutes
            }.coerceAtLeast(1)

            val segEnd = segStart + segMinutes

            if (displayMinute in segStart..segEnd) {
                val frac = ((displayMinute - segStart).toFloat() / segMinutes.toFloat()).coerceIn(0f, 1f)
                return it.offset.toFloat() + frac * it.size.toFloat()
            }
        }

        return null
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val railPx = with(density) { railWidth.toPx() }
        val leftPadPx = with(density) { listOuterStartPadding.toPx() }

        val railRight = leftPadPx + railPx
        val dividerX = railRight + with(density) { 4.dp.toPx() }

        drawLine(
            color = cs.onSurface.copy(alpha = 0.08f),
            start = Offset(x = dividerX, y = 0f),
            end = Offset(x = dividerX, y = size.height),
            strokeWidth = 1f
        )

        val contentLeft = dividerX + with(density) { 8.dp.toPx() }
        val contentRight = size.width - with(density) { 16.dp.toPx() }

        val paint = Paint().apply {
            isAntiAlias = true
            color = cs.onSurface.copy(alpha = 0.55f).toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
        val labelX = railRight - with(density) { 10.dp.toPx() }

        // Hour lines every hour; labels every 2 hours
        for (h in 0..24) {
            val realMin = h * 60
            val dispMin = minuteToDisplay(realMin)

            val y = displayMinuteToViewportY(dispMin) ?: continue
            if (y < 0f || y > size.height) continue

            val isMajor = (h % 2 == 0)

            drawLine(
                color = cs.onSurface.copy(alpha = if (isMajor) 0.10f else 0.06f),
                start = Offset(x = contentLeft, y = y),
                end = Offset(x = contentRight, y = y),
                strokeWidth = if (isMajor) 1.2f else 1.0f
            )

            if (isMajor) {
                val label = when (h) {
                    0 -> "12 AM"
                    12 -> "12 PM"
                    24 -> "12 AM"
                    else -> {
                        val hh = h % 12
                        val shown = if (hh == 0) 12 else hh
                        "${shown} " + if (h < 12) "AM" else "PM"
                    }
                }

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    labelX,
                    y + paint.textSize * 0.35f,
                    paint
                )
            }
        }
    }
}

/* ----------------------------- Helpers ---------------------------- */

private fun <T> List<T>.indexOfFirstFrom(start: Int, predicate: (T) -> Boolean): Int {
    var i = start
    while (i in indices) {
        if (predicate(this[i])) return i
        i++
    }
    return -1
}

private fun <T> List<T>.indexOfLastBefore(start: Int, predicate: (T) -> Boolean): Int {
    var i = start
    while (i in indices) {
        if (predicate(this[i])) return i
        i--
    }
    return -1
}