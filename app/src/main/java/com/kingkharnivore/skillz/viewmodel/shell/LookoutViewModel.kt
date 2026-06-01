package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.data.repository.lookout.LookoutRepository
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardModel
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.domain.lookout.ObjectiveKind
import com.kingkharnivore.skillz.domain.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.domain.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.domain.lookout.ObjectiveSourceFlow
import com.kingkharnivore.skillz.domain.lookout.ObjectiveWindow
import com.kingkharnivore.skillz.domain.lookout.millisUntilNextObjectiveBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_MINUTE = 60_000L

data class LookoutUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: ObjectivePeriod = ObjectivePeriod.Daily,
    val journeys: List<LookoutJourneyUiState> = emptyList(),
    val daily: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Daily),
    val weekly: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Weekly),
    val monthly: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Monthly),
    val setObjectiveDialog: SetObjectiveDialogState? = null,
    val rewardDialog: ObjectiveRewardDialogState? = null,
    val removeDialog: ObjectiveRemoveDialogState? = null,
    val errorMessage: String? = null
)

data class LookoutJourneyUiState(val id: Long, val name: String)

data class ObjectivePeriodUiState(
    val period: ObjectivePeriod,
    val periodTitle: String = "${period.label} Objectives",
    val summaryLabel: String = "No Objectives set.",
    val inProgress: List<ObjectiveCardUiState> = emptyList(),
    val completed: List<ObjectiveCardUiState> = emptyList(),
    val upcoming: List<ObjectiveCardUiState> = emptyList()
)

data class ObjectiveCardUiState(
    val objectiveId: Long,
    val journeyName: String,
    val periodLabel: String,
    val typeLabel: String,
    val progressPercent: Int,
    val progressLabel: String,
    val timeLeftLabel: String,
    val estimatedRewardLabel: String,
    val badgeLabel: String,
    val isRecurring: Boolean,
    val currentStreak: Int?,
    val maxStreak: Int?,
    val totalCompletions: Int?,
    val streakBonusLabel: String?,
    val rewardPearls: Int?,
    val state: ObjectiveCardState
)

data class SetObjectiveDialogState(
    val selectedJourneyId: Long? = null,
    val selectedJourneyName: String = "Choose Journey",
    val startDate: LocalDate = LocalDate.now(),
    val period: ObjectivePeriod = ObjectivePeriod.Daily,
    val kind: ObjectiveKind = ObjectiveKind.OneTime,
    val targetMinutesText: String = "30",
    val weeklyBoundaryDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val previewStarts: String = "",
    val previewEnds: String = "",
    val previewDuration: String = "",
    val validationMessage: String? = null
)

data class ObjectiveRewardDialogState(
    val title: String,
    val body: String,
    val pearls: String,
    val badge: String,
    val streakBonus: String?,
    val currentStreak: String?
)

data class ObjectiveRemoveDialogState(
    val objectiveId: Long,
    val isRecurring: Boolean,
    val period: ObjectivePeriod,
    val periodStartMs: Long,
    val periodEndMs: Long,
    val journeyName: String
)

@HiltViewModel
class LookoutViewModel @Inject constructor(
    private val flowRepository: FlowRepository,
    private val journeyRepository: JourneyRepository,
    private val lookoutRepository: LookoutRepository,
    private val calculator: ObjectiveProgressCalculator
) : ViewModel() {
    private val _uiState = MutableStateFlow(LookoutUiState())
    val uiState: StateFlow<LookoutUiState> = _uiState

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events

    private var latestObjectives: List<ObjectiveEntity> = emptyList()
    private var latestCards: List<ObjectiveCardModel> = emptyList()
    private var latestJourneys: List<TagEntity> = emptyList()
    private var refreshJob: Job? = null

    init {
        observeLookout()
        scheduleBoundaryRefresh()
    }

    fun selectPeriod(period: ObjectivePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun openSetObjective(period: ObjectivePeriod = _uiState.value.selectedPeriod) {
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                setObjectiveDialog = withPreview(
                    SetObjectiveDialogState(
                        selectedJourneyId = latestJourneys.firstOrNull()?.id,
                        selectedJourneyName = latestJourneys.firstOrNull()?.name ?: "Choose Journey",
                        startDate = today,
                        period = period,
                        weeklyBoundaryDay = today.dayOfWeek
                    )
                )
            )
        }
    }

    fun dismissSetObjective() = _uiState.update { it.copy(setObjectiveDialog = null) }
    fun dismissReward() = _uiState.update { it.copy(rewardDialog = null) }
    fun dismissRemove() = _uiState.update { it.copy(removeDialog = null) }

    fun updateDialog(transform: (SetObjectiveDialogState) -> SetObjectiveDialogState) {
        _uiState.update { state ->
            val current = state.setObjectiveDialog ?: return@update state
            state.copy(setObjectiveDialog = withPreview(transform(current).copy(validationMessage = null)))
        }
    }

    fun saveObjective() = viewModelScope.launch {
        val dialog = _uiState.value.setObjectiveDialog ?: return@launch
        val journeyId = dialog.selectedJourneyId
        val targetMinutes = dialog.targetMinutesText.toLongOrNull()
        when {
            journeyId == null -> showDialogValidation("Choose a Journey first.")
            targetMinutes == null || targetMinutes <= 0 -> showDialogValidation("Target time must be at least 1 minute.")
            hasDuplicateActiveObjective(journeyId, dialog.period) -> showDialogValidation("You already have a ${dialog.period.label} Objective for ${dialog.selectedJourneyName}.")
            else -> {
                val zone = ZoneId.systemDefault()
                val startMs = dialog.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val now = System.currentTimeMillis()
                lookoutRepository.insertObjective(
                    ObjectiveEntity(
                        journeyId = journeyId,
                        journeyNameSnapshot = dialog.selectedJourneyName,
                        periodType = dialog.period.storageValue,
                        objectiveType = dialog.kind.storageValue,
                        targetDurationMs = targetMinutes * MILLIS_PER_MINUTE,
                        startAtMs = startMs,
                        weeklyBoundaryDay = if (dialog.period == ObjectivePeriod.Weekly) dialog.weeklyBoundaryDay.value else null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _uiState.update { it.copy(setObjectiveDialog = null) }
                _events.emit("Objective set for ${dialog.selectedJourneyName}.")
            }
        }
    }

    fun showReward(objectiveId: Long) {
        val card = latestCards.firstOrNull { it.objective.id == objectiveId && it.completion != null } ?: return
        val completion = card.completion ?: return
        val bonusPct = ((completion.streakMultiplier - 1.0) * 100).toInt()
        _uiState.update {
            it.copy(
                rewardDialog = ObjectiveRewardDialogState(
                    title = "Objective Complete",
                    body = "You completed your ${completion.journeyNameSnapshot} ${card.period.label} Objective.",
                    pearls = "+${completion.finalRewardPearls} Pearls",
                    badge = "${completion.badgeLabelSnapshot} badge +1",
                    streakBonus = if (bonusPct > 0) "$bonusPct% streak bonus" else null,
                    currentStreak = if (card.kind == ObjectiveKind.Recurring) "Current streak: ${card.objective.currentStreak}" else null
                )
            )
        }
    }

    fun requestRemove(objectiveId: Long) {
        val card = latestCards.firstOrNull { it.objective.id == objectiveId } ?: return
        _uiState.update {
            it.copy(
                removeDialog = ObjectiveRemoveDialogState(
                    objectiveId = objectiveId,
                    isRecurring = card.kind == ObjectiveKind.Recurring,
                    period = card.period,
                    periodStartMs = card.window.startMs,
                    periodEndMs = card.window.endMs,
                    journeyName = card.objective.journeyNameSnapshot
                )
            )
        }
    }

    fun deleteOneTimeObjective() = viewModelScope.launch {
        val dialog = _uiState.value.removeDialog ?: return@launch
        lookoutRepository.archiveObjective(dialog.objectiveId)
        _uiState.update { it.copy(removeDialog = null) }
    }

    fun stopRecurringObjective() = viewModelScope.launch {
        val dialog = _uiState.value.removeDialog ?: return@launch
        lookoutRepository.archiveObjective(dialog.objectiveId)
        _uiState.update { it.copy(removeDialog = null) }
    }

    fun skipRecurringCycle() = viewModelScope.launch {
        val dialog = _uiState.value.removeDialog ?: return@launch
        lookoutRepository.skipCycle(dialog.objectiveId, dialog.periodStartMs, dialog.periodEndMs)
        _uiState.update { it.copy(removeDialog = null) }
    }

    private fun observeLookout() {
        viewModelScope.launch {
            combine(
                lookoutRepository.observeObjectives(),
                lookoutRepository.observeCompletions(),
                lookoutRepository.observeSkippedCycles(),
                flowRepository.getAllSessions(),
                journeyRepository.getAllTags()
            ) { objectives, completions, skipped, sessions, journeys ->
                latestObjectives = objectives
                latestJourneys = journeys
                val flows = sessions.map { it.toObjectiveSourceFlow() }
                val result = calculator.calculate(objectives, flows, completions, skipped, Instant.now(), ZoneId.systemDefault())
                result.completionsToGrant.forEach { grant ->
                    lookoutRepository.applyCompletionGrant(grant.completion, grant.newCurrentStreak, grant.newMaxStreak, grant.newTotalCompletions)
                }
                result.streakResets.forEach { lookoutRepository.resetStreak(it.objectiveId) }
                result.cards to journeys
            }.catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }.collect { (cards, journeys) ->
                latestCards = cards
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        journeys = journeys.map { LookoutJourneyUiState(it.id, it.name) },
                        daily = cards.toPeriodState(ObjectivePeriod.Daily),
                        weekly = cards.toPeriodState(ObjectivePeriod.Weekly),
                        monthly = cards.toPeriodState(ObjectivePeriod.Monthly),
                        errorMessage = null,
                        setObjectiveDialog = state.setObjectiveDialog?.let(::withPreview)
                    )
                }
            }
        }
    }

    private fun List<ObjectiveCardModel>.toPeriodState(period: ObjectivePeriod): ObjectivePeriodUiState {
        val periodCards = filter { it.period == period }
        val inProgress = periodCards.filter { it.state == ObjectiveCardState.InProgress }.sortedByDescending { it.progressPercent }.map { it.toUiState() }
        val completed = periodCards.filter { it.state == ObjectiveCardState.Completed }.sortedByDescending { it.completion?.completedAt ?: 0L }.map { it.toUiState() }
        val upcoming = periodCards.filter { it.state == ObjectiveCardState.Upcoming }.sortedBy { it.window.startMs }.map { it.toUiState() }
        val summary = when {
            inProgress.isNotEmpty() -> "${inProgress.size} in progress · ${completed.size} completed"
            completed.isNotEmpty() -> "${completed.size} completed this period"
            upcoming.isNotEmpty() -> "${upcoming.size} upcoming"
            else -> "No ${period.label} Objectives set."
        }
        return ObjectivePeriodUiState(period, "${period.label} Objectives", summary, inProgress, completed, upcoming)
    }

    private fun ObjectiveCardModel.toUiState(): ObjectiveCardUiState {
        val target = formatDuration(objective.targetDurationMs)
        val progress = formatDuration(progressDurationMs)
        val bonusPct = (objective.currentStreak * 10).coerceAtMost(100)
        return ObjectiveCardUiState(
            objectiveId = objective.id,
            journeyName = objective.journeyNameSnapshot,
            periodLabel = period.label,
            typeLabel = kind.label,
            progressPercent = progressPercent,
            progressLabel = if (state == ObjectiveCardState.Upcoming) "Target: $target" else "$progress / $target",
            timeLeftLabel = when (state) {
                ObjectiveCardState.Upcoming -> "Starts in: ${formatRemaining(window.startMs - System.currentTimeMillis())}"
                ObjectiveCardState.InProgress -> "Time left: ${formatRemaining(window.endMs - System.currentTimeMillis())}"
                ObjectiveCardState.Completed -> if (kind == ObjectiveKind.Recurring) "Completed for this cycle · Next reset: ${formatResetDay(window.endMs)}" else "Completed"
            },
            estimatedRewardLabel = completion?.let { "+${it.finalRewardPearls} Pearls" } ?: "Complete this Objective to earn Pearls equal to minutes completed",
            badgeLabel = "${objective.journeyNameSnapshot} ${period.label} Objective badge +1",
            isRecurring = kind == ObjectiveKind.Recurring,
            currentStreak = if (kind == ObjectiveKind.Recurring) objective.currentStreak else null,
            maxStreak = if (kind == ObjectiveKind.Recurring) objective.maxStreak else null,
            totalCompletions = if (kind == ObjectiveKind.Recurring) objective.totalCompletions else null,
            streakBonusLabel = if (kind == ObjectiveKind.Recurring) "Streak bonus: +$bonusPct%" else null,
            rewardPearls = completion?.finalRewardPearls,
            state = state
        )
    }

    private fun withPreview(dialog: SetObjectiveDialogState): SetObjectiveDialogState {
        val zone = ZoneId.systemDefault()
        val startMs = dialog.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val window = calculator.initialWindow(startMs, dialog.period, dialog.weeklyBoundaryDay.value, zone)
        val now = System.currentTimeMillis()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault())
        val timeLabel = if (now in window.startMs until window.endMs) {
            "Time left: ${formatRemaining(window.endMs - now)}"
        } else {
            "Time available: ${formatRemaining(window.endMs - window.startMs)}"
        }
        return dialog.copy(
            previewStarts = formatter.format(Instant.ofEpochMilli(window.startMs).atZone(zone)),
            previewEnds = formatter.format(Instant.ofEpochMilli(window.endMs).atZone(zone)),
            previewDuration = timeLabel
        )
    }

    private fun showDialogValidation(message: String) {
        _uiState.update { state ->
            state.copy(setObjectiveDialog = state.setObjectiveDialog?.copy(validationMessage = message))
        }
    }

    private fun hasDuplicateActiveObjective(journeyId: Long, period: ObjectivePeriod): Boolean {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        return latestObjectives.any { objective ->
            objective.journeyId == journeyId && objective.periodType == period.storageValue && !objective.isArchived &&
                (objective.objectiveType == ObjectiveKind.Recurring.storageValue || calculator.windowFor(objective, now, zone).endMs > now.toEpochMilli())
        }
    }

    private fun scheduleBoundaryRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(millisUntilNextObjectiveBoundary())
                _uiState.update { it.copy() }
            }
        }
    }

    private fun SessionEntity.toObjectiveSourceFlow() = ObjectiveSourceFlow(id, tagId, startTime, endTime, durationMs, isSoftMode)

    private fun formatResetDay(endMs: Long): String = Instant.ofEpochMilli(endMs).atZone(ZoneId.systemDefault()).dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }

    private fun formatDuration(ms: Long): String {
        val minutes = (ms / MILLIS_PER_MINUTE).coerceAtLeast(0)
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "${h}h" else "${h}h ${m}m"
        } else "${minutes} min"
    }

    private fun formatRemaining(ms: Long): String {
        var minutes = (ms / MILLIS_PER_MINUTE).coerceAtLeast(0)
        val days = minutes / (24 * 60)
        minutes %= 24 * 60
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
            hours > 0 -> if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            else -> "${mins}m"
        }
    }
}
