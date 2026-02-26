package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.model.DayPlanUi
import com.kingkharnivore.skillz.ui.model.DaySegmentUi
import kotlinx.coroutines.delay

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

    // ✅ Prevent first-layout/first-measure weirdness from triggering paging
    var pagingArmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(350)
        pagingArmed = true
    }

    // ✅ Track scroll direction (finger space delta)
    // available.y > 0  => finger moving down
    // available.y < 0  => finger moving up
    var lastScrollDeltaY by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                lastScrollDeltaY = available.y
                return Offset.Zero
            }
        }
    }

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
    LaunchedEffect(dayPlan.dayStartMs) {
        val edgeThresholdPx = with(density) { 28.dp.toPx() }.toInt()
        val cooldownMs = 650L

        snapshotFlow { listState.layoutInfo }
            .collect { info ->
                if (!pagingArmed) return@collect
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

                // ✅ Direction gating:
                // - top sentinel should only trigger when the user is pulling DOWN at the top
                // - bottom sentinel should only trigger when the user is pulling UP at the bottom
                val pulledDown = lastScrollDeltaY > 0f
                val pulledUp = lastScrollDeltaY < 0f

                when {
                    isAtTopSentinel && pulledDown -> {
                        suppressDayAdvance = true
                        lastAdvanceAtMs = now // ✅ FIX: cooldown actually works
                        onAdvanceDay(-1)
                        delay(220)
                        suppressDayAdvance = false
                    }

                    isAtBottomSentinel && pulledUp -> {
                        suppressDayAdvance = true
                        lastAdvanceAtMs = now // ✅ FIX: cooldown actually works
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
            .clipToBounds()
            .nestedScroll(nestedScrollConnection) // ✅ direction tracking + fixes first-scroll paging
    ) {
        // Background grid aligned to the same paddings as the list content
        DayTimeGridBackground(
            dayPlan = dayPlan,
            listState = listState,
            railWidth = railWidth,
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