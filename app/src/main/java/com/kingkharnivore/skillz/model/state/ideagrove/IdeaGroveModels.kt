package com.kingkharnivore.skillz.model.state.ideagrove

enum class IdeaGroveItemType {
    RAW_PULSE,
    IDEA,
    INSIGHT,
    COMPLETED_IDEA
}

enum class IdeaGroveSort {
    Recents,
    Newest,
    Oldest,
    MostTime,
    LeastTime
}

data class IdeaGroveFlowUiModel(
    val sessionId: Long,
    val title: String,
    val description: String,
    val journeyName: String?,
    val durationMs: Long,
    val startTime: Long,
    val endTime: Long?
)

data class IdeaGroveItemUiModel(
    val pulseId: Long,
    val type: IdeaGroveItemType,
    val title: String,
    val description: String,
    val journeyName: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val groveStatus: String,
    val groveStatusChangedAt: Long?,
    val flowCount: Int,
    val totalFlowDurationMs: Long,
    val lastWorkedAt: Long?,
    val flows: List<IdeaGroveFlowUiModel>,
    val wasCapturedDuringFlow: Boolean
)

data class PulseLaunchContext(
    val pulseId: Long,
    val title: String,
    val description: String,
    val journeyName: String?
)

data class IdeaGroveUiState(
    val aliveItems: List<IdeaGroveItemUiModel> = emptyList(),
    val completedItems: List<IdeaGroveItemUiModel> = emptyList(),
    val totalPulseFlowDurationMs: Long = 0L,
    val totalPulseFlowCount: Int = 0,
    val completedPulseFlowDurationMs: Long = 0L,
    val completedPulseFlowCount: Int = 0,
    val aliveSort: IdeaGroveSort = IdeaGroveSort.Recents,
    val expandedPulseId: Long? = null,
    val pendingDeletePulseId: Long? = null,
    val isFlowRunning: Boolean = false,
    val isLoading: Boolean = true
)
