package com.kingkharnivore.skillz.domain.anchor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class AnchorDetectionPolicyTest {
    private fun input() = AnchorDetectionInput(
        hasActiveFlow = true,
        flowRunning = true,
        flowPaused = false,
        globallyEnabled = true,
        enabledForFlow = false,
        disabledForFlow = false,
        anchorPaused = false,
        inBreak = false,
        usageAccessGranted = true,
        currentPackage = "com.example.social",
        anchoredPackages = setOf("com.example.social"),
        neverAnchorPackages = emptySet()
    )

    @Test fun nudgesOnlyWhenAllPreconditionsPass() {
        assertTrue(AnchorDetectionPolicy.shouldNudge(input()))
        assertFalse(AnchorDetectionPolicy.shouldNudge(input().copy(usageAccessGranted = false)))
        assertFalse(AnchorDetectionPolicy.shouldNudge(input().copy(anchorPaused = true)))
        assertFalse(AnchorDetectionPolicy.shouldNudge(input().copy(inBreak = true)))
        assertFalse(AnchorDetectionPolicy.shouldNudge(input().copy(disabledForFlow = true)))
        assertFalse(AnchorDetectionPolicy.shouldNudge(input().copy(anchoredPackages = emptySet())))
    }

    @Test fun essentialPackagesAreIgnoredEvenIfSaved() {
        assertFalse(
            AnchorDetectionPolicy.shouldNudge(
                input().copy(
                    currentPackage = "com.android.settings",
                    anchoredPackages = setOf("com.android.settings"),
                    neverAnchorPackages = setOf("com.android.settings")
                )
            )
        )
    }

    @Test fun anchorDoesNotChangeScoreCalculations() {
        val baseline = ScoreCalculator.breakdownFromDuration(45 * 60_000L)
        val withAnchorMetadataPresent = ScoreCalculator.breakdownFromDuration(45 * 60_000L)
        assertEquals(baseline, withAnchorMetadataPresent)
        assertEquals(ScoreCalculator.surgePoints(25 * 60_000L, 25 * 60_000L), ScoreCalculator.surgePoints(25 * 60_000L, 25 * 60_000L))
    }

    @Test fun countsOneEpisodePerAnchoredAppOpening() {
        val counter = AnchorEpisodeCounter()
        val anchored = setOf("com.example.social")
        assertTrue(counter.shouldCountEpisode("com.example.social", anchored))
        assertFalse(counter.shouldCountEpisode("com.example.social", anchored))
        assertFalse(counter.shouldCountEpisode("com.example.other", anchored))
        assertTrue(counter.shouldCountEpisode("com.example.social", anchored))
    }
}
