package com.kingkharnivore.skillz.viewmodel

import com.kingkharnivore.skillz.model.FlowMode
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.entity.*
import com.kingkharnivore.skillz.data.repository.*
import com.kingkharnivore.skillz.model.state.paths.*
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleReadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PathsViewModel @Inject constructor(
    private val flowRepository: FlowRepository,
    private val flowPlanRepository: FlowPlanRepository,
    private val journeyRepository: JourneyRepository,
    private val database: SkillzDatabase,
    private val chronicleRepository: ChronicleRepository,
    private val aliveFlowRepository: AliveFlowRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val selected = savedStateHandle.getStateFlow("horizonTab", PathsPrimaryTab.HABITS.name)
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private data class Data(val plans: List<FlowPlanEntity>, val memories: List<HorizonMemoryEntity>,
        val sessions: List<SessionEntity>, val tags: List<TagEntity>, val ongoing: OngoingSessionEntity?)
    private val data = combine(flowPlanRepository.observeAll(), database.horizonMemoryDao().observeAll(),
        database.sessionDao().getAllSessions(), journeyRepository.getAllTags(), aliveFlowRepository.getOngoingSession(), ::Data)
    val uiState: StateFlow<PathsUiState> = combine(data, selected, saving, error) { data, tab, saving, error ->
        val tags = data.tags.associate { it.id to it.name }
        val counts = data.memories.filter { it.kind == HorizonKind.HABIT }.groupBy { it.sourcePlanId }
        fun model(p: FlowPlanEntity) = FlowPlanListItemUiModel(p.id, p.title, p.tagId, tags[p.tagId].orEmpty(),
            p.isSoftMode, p.targetMinutes, p.launchWithSurge, p.pinned, p.launchCount, p.lastLaunchedAt,
            p.kind, counts[p.id].orEmpty().size, counts[p.id]?.maxOfOrNull { it.completedAt }, p.mode)
        PathsUiState(isLoading = false, isSaving = saving, errorMessage = error,
            selectedPrimaryTab = runCatching { PathsPrimaryTab.valueOf(tab) }.getOrDefault(PathsPrimaryTab.HABITS),
            flowPlans = data.plans.filter { !it.archived && it.completedAt == null }.map(::model),
            dreamFlowPlans = data.plans.filter { it.archived && it.completedAt == null }.map(::model),
            memories = groupHorizonMemories(data.memories, data.sessions, tags),
            tags = data.tags.map { TagUiModel(it.id, it.name) }, ongoing = data.ongoing)
    }.catch { emit(PathsUiState(isLoading = false, errorMessage = it.message)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PathsUiState())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val arcPlans = database.arcPlanDao().getActiveArcPlans().flatMapLatest { arcs ->
        if (arcs.isEmpty()) flowOf(emptyList<com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel>())
        else combine(arcs.map { arc -> database.arcPlanDao().getStepsForArcPlan(arc.id).map { steps ->
            com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel(arc.id, arc.title, arc.isInStudio,
                arc.launchCount, arc.lastLaunchedAt, steps.map { step ->
                    com.kingkharnivore.skillz.model.ui.ArcPlanStepPreviewUiModel(step.titleSnapshot,
                        step.targetMinutesSnapshot, step.isSoftModeSnapshot, step.launchWithSurgeSnapshot, step.mode)
                })
        } }) { it.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteArcPlan(id: Long) = mutate { database.arcPlanDao().deleteArcPlanById(id) }

    fun createHistoricalChronicle(type: String, key: String) = ChronicleReadState(type, key, chronicleRepository, viewModelScope)
    fun onPrimaryTabSelected(tab: PathsPrimaryTab) { savedStateHandle["horizonTab"] = tab.name }
    fun clearError() { error.value = null }
    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { error.value = null; block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message }
        }
    }
    fun saveActivity(id: Long?, title: String, tagName: String, kind: String, mode: FlowMode,
        minutes: Int?, surge: Boolean, onSaved: () -> Unit) {
        if (saving.value) return
        saving.value = true
        mutate {
            try {
                require(title.isNotBlank())
                require(kind == HorizonKind.HABIT || kind == HorizonKind.PLAN)
                require(!surge || (mode != FlowMode.SOFT && minutes != null && minutes > 0))
                val tagId = tagName.trim().takeIf(String::isNotEmpty)?.let { journeyRepository.getOrCreateTagId(it) }
                if (id == null) flowPlanRepository.createFlowPlan(title, tagId, mode == FlowMode.SOFT, minutes, surge, kind, mode)
                else {
                    val old = checkNotNull(flowPlanRepository.getFlowPlanById(id))
                    check(old.completedAt == null)
                    val ongoing = aliveFlowRepository.getOngoingSession().first()
                    check(ongoing?.originPlanId != id) { "Finish or leave the current Flow before editing this activity." }
                    flowPlanRepository.updateFlowPlan(old.copy(title = title.trim(), tagId = tagId, kind = kind,
                        isSoftMode = mode == FlowMode.SOFT, mode = mode, targetMinutes = minutes, launchWithSurge = surge))
                }
                onPrimaryTabSelected(if (kind == HorizonKind.HABIT) PathsPrimaryTab.HABITS else PathsPrimaryTab.PLANS)
                onSaved()
            } finally { saving.value = false }
        }
    }
    fun archive(id: Long, archived: Boolean) = mutate { flowPlanRepository.setArchived(id, archived) }
    fun pin(id: Long, pinned: Boolean) = mutate { flowPlanRepository.setPinned(id, pinned) }
    fun delete(id: Long) = mutate {
        check(aliveFlowRepository.getOngoingSession().first()?.originPlanId != id) { "Finish or leave the current Flow before deleting this activity." }
        flowPlanRepository.deleteFlowPlanById(id)
    }
    fun removeMemory(flow: MemoryFlowUi) = mutate {
        flowRepository.removeMemoryFromActivity(flow.receipt.flowInstanceId, flow.receipt.sourcePlanId)
    }
    fun launched(id: Long) = mutate { flowPlanRepository.markLaunched(id) }
    fun planAgain(memory: MemoryGroupUi, onSaved: () -> Unit) {
        if (saving.value) return
        saving.value = true
        mutate {
        try {
        val old = flowPlanRepository.getFlowPlanById(memory.sourcePlanId)
        val receipt = memory.latest.receipt
        val tagId = (old?.tagId) ?: receipt.tagName.takeIf(String::isNotBlank)?.let { journeyRepository.getOrCreateTagId(it) }
        flowPlanRepository.createFlowPlan(old?.title ?: memory.title, tagId, old?.isSoftMode ?: (receipt.mode == com.kingkharnivore.skillz.model.FlowMode.SOFT),
            old?.targetMinutes ?: receipt.surgePlannedMs?.div(60_000L)?.toInt(),
            old?.launchWithSurge ?: (receipt.surgePlannedMs != null), HorizonKind.PLAN, old?.mode ?: receipt.mode)
        onPrimaryTabSelected(PathsPrimaryTab.PLANS)
        onSaved()
        } finally { saving.value = false }
        }
    }
}

internal fun groupHorizonMemories(receipts: List<HorizonMemoryEntity>, sessions: List<SessionEntity>, tags: Map<Long, String>): List<MemoryGroupUi> {
    val byId = sessions.associateBy { it.id }
    return receipts.groupBy { it.kind to it.sourcePlanId }.map { (key, values) ->
        val ordered = values.sortedWith(compareByDescending<HorizonMemoryEntity> { it.completedAt }.thenByDescending { it.flowInstanceId })
        MemoryGroupUi(key.second, key.first, ordered.first().activityTitle, ordered.map { receipt ->
            val session = byId[receipt.sessionId]
            MemoryFlowUi(receipt, session, session?.let { tags[it.tagId] } ?: receipt.tagName)
        })
    }.sortedWith(compareByDescending<MemoryGroupUi> { it.latest.receipt.completedAt }.thenBy { it.key })
}
