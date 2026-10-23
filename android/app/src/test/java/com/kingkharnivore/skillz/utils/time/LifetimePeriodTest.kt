package com.kingkharnivore.skillz.utils.time

import org.junit.Assert.assertEquals
import org.junit.Test

class LifetimePeriodTest {
    @Test fun lifetimeHasNoDateNavigationOrTimeBoundary() {
        val period = StoryPeriod.LIFETIME
        assertEquals(0L, TimeWindowUtils.normalizeAnchor(System.currentTimeMillis(), period))
        assertEquals(0L, TimeWindowUtils.shiftAnchor(0, period, -1))
        assertEquals(0L, TimeWindowUtils.shiftAnchor(0, period, 1))
        assertEquals(TimeWindow(Long.MIN_VALUE, Long.MAX_VALUE), TimeWindowUtils.windowFor(0, period))
        assertEquals(0L, TimeWindowUtils.clampToFirstAndToday(0, period, null))
    }
}
