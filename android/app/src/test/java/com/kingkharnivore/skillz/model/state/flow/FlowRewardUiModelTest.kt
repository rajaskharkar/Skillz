package com.kingkharnivore.skillz.model.state.flow

import org.junit.Assert.assertTrue
import org.junit.Test

class FlowRewardUiModelTest {
    @Test
    fun softRewardRemainsSelfDescribingWhenDraftModeChanges() {
        val completedReward = FlowRewardUiModel(
            minutes = 12,
            baseScyraPoints = 0,
            tenMinuteBonuses = 0,
            thirtyMinuteBonuses = 0,
            sixtyMinuteBonuses = 0,
            finalScyraPoints = 0,
            surgePoints = 0,
            isSoftSession = true
        )

        // Reward rendering reads this immutable completion metadata, not FlowUiState.
        assertTrue(completedReward.isSoftSession)
    }
}
