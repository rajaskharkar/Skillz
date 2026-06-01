package com.kingkharnivore.skillz.model.state.ideagrove

enum class IdeaGroveItemType {
    RAW_PULSE,
    IDEA,
    INSIGHT,
    COMPLETED_IDEA
}

enum class IdeaGroveSort {
    Newest,
    Oldest,
    MostTime,
    LeastTime
}

data class IdeaGroveFlowUiModel(
    val sessionId: Long,
    val title: String,
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
    val aliveTotalDurationMs: Long = 0L,
    val aliveFlowCount: Int = 0,
    val completedTotalDurationMs: Long = 0L,
    val completedFlowCount: Int = 0,
    val aliveSort: IdeaGroveSort = IdeaGroveSort.Newest,
    val expandedPulseId: Long? = null,
    val isFlowRunning: Boolean = false,
    val isLoading: Boolean = true
)
