package com.kingkharnivore.skillz.model.state

import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.Journey7dStatUiModel
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.viewmodel.TagUiModel


data class FlowListUiState(
    val isLoading: Boolean = true,
    val sessions: List<FlowListItemUiModel> = emptyList(),
    val chronicleItems: List<ChronicleUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList(),
    val selectedTagId: Long? = null,
    val totalDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val period: StoryPeriod = StoryPeriod.DAY,
    val anchorDayStartMs: Long = 0L,
    val currentScore: Int = 0,
    val currentSurgeScore: Int = 0,
    val topJourneysLast7d: List<Journey7dStatUiModel> = emptyList(),
    val firstSessionStartMs: Long? = null,
    val isCurrentPeriod: Boolean = true,
    val sagasInView: List<Journey7dStatUiModel> = emptyList(),
    val isViewJourneysOpen: Boolean = false,
    val viewJourneysTitle: String = "",
    val viewJourneysSessions: List<FlowListItemUiModel> = emptyList(),
    val showScoreUi: Boolean = false,
    val calmMode: Boolean = false
)