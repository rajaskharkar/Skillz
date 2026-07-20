package com.kingkharnivore.skillz.ui.screen.shell.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeMedallionCountTest {
    @Test fun exactCountsRemainExactThrough999() {
        assertEquals("1", compactCount(1))
        assertEquals("2", compactCount(2))
        assertEquals("91", compactCount(91))
        assertEquals("999", compactCount(999))
    }

    @Test fun largeCountsUseCompactPresentation() {
        val result = compactCount(1_200)
        assert(result.isNotBlank())
        assert(result != "1200")
    }
}
