package com.kingkharnivore.skillz.ui.screen.shell.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTabTest {
    @Test fun badgeScreenHasFourTabsWithShowcaseFirst() {
        assertEquals(
            listOf(BadgesTab.SHOWCASE, BadgesTab.BADGE_BOOK, BadgesTab.WITHIN_REACH, BadgesTab.PROGRESS),
            BadgesTab.entries
        )
    }

    @Test fun searchBelongsOnlyToBadgeBook() {
        BadgesTab.entries.forEach { tab ->
            if (tab == BadgesTab.BADGE_BOOK) assertTrue(tab.showsBadgeBookControls)
            else assertFalse(tab.showsBadgeBookControls)
        }
    }
}
