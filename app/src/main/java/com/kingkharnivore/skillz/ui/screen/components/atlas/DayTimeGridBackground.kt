package com.kingkharnivore.skillz.ui.screen.components.atlas

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.atlas.model.DayPlanUi
import com.kingkharnivore.skillz.ui.atlas.model.DaySegmentUi
import kotlin.math.max

@Composable
fun DayTimeGridBackground(
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