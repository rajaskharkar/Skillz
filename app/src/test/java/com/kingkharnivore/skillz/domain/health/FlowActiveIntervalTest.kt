package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.data.health.MovementReadResult
import com.kingkharnivore.skillz.utils.health.FlowActiveInterval
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalCodec
import com.kingkharnivore.skillz.utils.health.FlowActiveIntervalNormalizer
import com.kingkharnivore.skillz.utils.health.MovementBonusCalculator
import com.kingkharnivore.skillz.utils.health.MovementStepAggregator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowActiveIntervalTest {
    @Test fun normalizerMergesOverlappingAndAdjacentIntervalsAndIgnoresInvalid() {
        val normalized = FlowActiveIntervalNormalizer.normalize(
            listOf(
                FlowActiveInterval(30, 20),
                FlowActiveInterval(0, 10),
                FlowActiveInterval(10, 20),
                FlowActiveInterval(18, 25),
                FlowActiveInterval(40, 40),
                FlowActiveInterval(50, 60)
            )
        )

        assertEquals(
            listOf(
                FlowActiveInterval(0, 25),
                FlowActiveInterval(50, 60)
            ),
            normalized
        )
    }

    @Test fun codecRoundTripNormalizesIntervals() {
        val encoded = FlowActiveIntervalCodec.encode(
            listOf(
                FlowActiveInterval(100, 200),
                FlowActiveInterval(150, 250),
                FlowActiveInterval(500, 400)
            )
        )

        assertEquals(listOf(FlowActiveInterval(100, 250)), FlowActiveIntervalCodec.decode(encoded))
    }

    @Test fun stepAggregatorReadsOnlyActiveIntervalsNotCompressedRange() = runBlocking {
        val nineToNineThirty = FlowActiveInterval(9 * HOUR, 9 * HOUR + 30 * MINUTE)
        val noonToNoonThirty = FlowActiveInterval(12 * HOUR, 12 * HOUR + 30 * MINUTE)
        val queried = mutableListOf<FlowActiveInterval>()

        val result = MovementStepAggregator().readStepsAcrossActiveIntervals(
            intervals = listOf(nineToNineThirty, noonToNoonThirty)
        ) { interval ->
            queried += interval
            MovementReadResult.Success(250)
        }

        assertEquals(listOf(nineToNineThirty, noonToNoonThirty), queried)
        assertTrue(FlowActiveInterval(9 * HOUR, 12 * HOUR + 30 * MINUTE) !in queried)
        assertEquals(MovementReadResult.Success(500), result)
    }

    @Test fun pauseGapStepsDoNotCount() = runBlocking {
        val activeBeforePause = FlowActiveInterval(0, 30 * MINUTE)
        val pausedGap = FlowActiveInterval(30 * MINUTE, 3 * HOUR)
        val activeAfterPause = FlowActiveInterval(3 * HOUR, 3 * HOUR + 30 * MINUTE)

        val result = MovementStepAggregator().readStepsAcrossActiveIntervals(
            intervals = listOf(activeBeforePause, activeAfterPause)
        ) { interval ->
            when (interval) {
                activeBeforePause -> MovementReadResult.Success(100)
                activeAfterPause -> MovementReadResult.Success(100)
                pausedGap -> MovementReadResult.Success(10_000)
                else -> error("Unexpected interval $interval")
            }
        }

        val success = result as MovementReadResult.Success
        assertEquals(200, success.steps)
        assertEquals(8, MovementBonusCalculator().calculateMovementPoints(success.steps))
    }

    @Test fun restoredRunningFlowDoesNotDoubleCountSavedOpenInterval() {
        val savedWhileRunning = FlowActiveIntervalCodec.encode(listOf(
            FlowActiveInterval(
                0,
                10 * MINUTE
            )
        ))
        val restoredIntervals = FlowActiveIntervalCodec.decode(savedWhileRunning) + FlowActiveInterval(
            0,
            20 * MINUTE
        )

        assertEquals(listOf(FlowActiveInterval(0, 20 * MINUTE)), FlowActiveIntervalNormalizer.normalize(restoredIntervals))
    }

    @Test fun emptyActiveIntervalsDoNotQueryInvalidRanges() = runBlocking {
        var queryCount = 0
        val result = MovementStepAggregator().readStepsAcrossActiveIntervals(emptyList()) {
            queryCount++
            MovementReadResult.Success(100)
        }

        assertEquals(0, queryCount)
        assertEquals(MovementReadResult.NoData, result)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
