package com.kingkharnivore.skillz.viewmodel.shell

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.data.repository.FlowRepository
import com.kingkharnivore.skillz.data.repository.JourneyRepository
import com.kingkharnivore.skillz.data.repository.lookout.LookoutRepository
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardModel
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.domain.lookout.ObjectiveKind
import com.kingkharnivore.skillz.domain.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.domain.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.domain.lookout.ObjectiveSourceFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
private const val LOOKOUT_TICK_MS = 60_000L

enum class LookoutMode { Objectives, Achievements }

enum class JourneyInputMode { Existing, New }

data class LookoutUiState(
    val isLoading: Boolean = true,
    val mode: LookoutMode = LookoutMode.Objectives,
    val selectedPeriod: ObjectivePeriod = ObjectivePeriod.Daily,
    val journeys: List<LookoutJourneyUiState> = emptyList(),
    val daily: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Daily),
    val weekly: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Weekly),
    val monthly: ObjectivePeriodUiState = ObjectivePeriodUiState(ObjectivePeriod.Monthly),
    val completedHistory: List<CompletedObjectiveHistoryGroupUiState> = emptyList(),
    val setObjectiveDialog: SetObjectiveDialogState? = null,
    val rewardDialog: ObjectiveRewardDialogState? = null,
    val removeDialog: ObjectiveRemoveDialogState? = null,
    val errorMessage: String? = null
)

data class LookoutJourneyUiState(val id: Long, val name: String)

data class ObjectivePeriodUiState(
    val period: ObjectivePeriod,
    val periodTitle: String = "",
    val summaryLabel: String = "",
    val inProgress: List<ObjectiveCardUiState> = emptyList(),
    val completed: List<ObjectiveCardUiState> = emptyList(),
    val upcoming: List<ObjectiveCardUiState> = emptyList()
)

data class ObjectiveCardUiState(
    val objectiveId: Long,
    val journeyId: Long,
    val journeyName: String,
    val periodLabel: String,
    val typeLabel: String,
    val progressPercent: Int,
    val progressLabel: String,
    val timeLeftLabel: String,
    val estimatedRewardLabel: String,
    val isRecurring: Boolean,
    val currentStreak: Int?,
    val maxStreak: Int?,
    val totalCompletions: Int?,
    val streakBonusLabel: String?,
    val rewardPearls: Int?,
    val completionId: Long?,
    val pearlsClaimed: Boolean,
    val state: ObjectiveCardState
)

data class CompletedObjectiveHistoryGroupUiState(
    val journeyName: String,
    val rows: List<CompletedObjectiveHistoryRowUiState>,
    val lastCompletedAt: Long
)

data class CompletedObjectiveHistoryRowUiState(
    val title: String,
    val summary: String,
    val lastCompletedLabel: String,
    val periodOrder: Int
)

private data class LookoutSourceData(
    val objectives: List<ObjectiveEntity>,
    val completions: List<ObjectiveCompletionEntity>,
    val skipped: List<ObjectiveSkippedCycleEntity>,
    val sessions: List<SessionEntity>,
    val journeys: List<TagEntity>
)

private data class TargetDurationFields(val hoursText: String, val minutesText: String)

private fun defaultTargetDurationFields(period: ObjectivePeriod): TargetDurationFields = when (period) {
    ObjectivePeriod.Daily -> TargetDurationFields(hoursText = "0", minutesText = "30")
    ObjectivePeriod.Weekly -> TargetDurationFields(hoursText = "5", minutesText = "0")
    ObjectivePeriod.Monthly -> TargetDurationFields(hoursText = "20", minutesText = "0")
}

data class SetObjectiveDialogState(
    val selectedJourneyId: Long? = null,
    val journeyText: String = "",
    val journeyInputMode: JourneyInputMode = JourneyInputMode.Existing,
    val startDate: LocalDate = LocalDate.now(),
    val period: ObjectivePeriod = ObjectivePeriod.Daily,
    val kind: ObjectiveKind = ObjectiveKind.OneTime,
    val targetHoursText: String = "0",
    val targetMinutesText: String = "30",
    val targetWasEdited: Boolean = false,
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
    @ApplicationContext private val context: Context,
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
    private val nowTick = MutableStateFlow(Instant.now())

    init {
        observeLookout()
        scheduleBoundaryRefresh()
    }

    fun selectPeriod(period: ObjectivePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun showObjectives() {
        _uiState.update { it.copy(mode = LookoutMode.Objectives) }
    }

    fun showAchievements() {
        _uiState.update { it.copy(mode = LookoutMode.Achievements) }
    }

    fun openSetObjective(period: ObjectivePeriod = _uiState.value.selectedPeriod) {
        val today = LocalDate.now()
        val targetDefaults = defaultTargetDurationFields(period)
        _uiState.update {
            it.copy(
                mode = LookoutMode.Objectives,
                setObjectiveDialog = SetObjectiveDialogState(
                    selectedJourneyId = null,
                    journeyText = "",
                    journeyInputMode = JourneyInputMode.Existing,
                    startDate = today,
                    period = period,
                    targetHoursText = targetDefaults.hoursText,
                    targetMinutesText = targetDefaults.minutesText
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
            state.copy(setObjectiveDialog = transform(current).copy(validationMessage = null))
        }
    }

    fun setDialogStartDate(startDate: LocalDate) = updateDialog { dialog ->
        dialog.copy(startDate = startDate)
    }

    fun saveObjective() = viewModelScope.launch {
        val dialog = _uiState.value.setObjectiveDialog ?: return@launch
        val journeyName = dialog.journeyText.trim()
        val targetHours = dialog.targetHoursText.toLongOrNull() ?: 0L
        val targetMinutes = dialog.targetMinutesText.toLongOrNull() ?: 0L
        val targetHasInvalidNumber =
            (dialog.targetHoursText.isNotBlank() && dialog.targetHoursText.toLongOrNull() == null) ||
                (dialog.targetMinutesText.isNotBlank() && dialog.targetMinutesText.toLongOrNull() == null)
        val totalTargetMinutes = targetHours * 60L + targetMinutes
        val matchedJourney = latestJourneys.firstOrNull { it.name.equals(journeyName, ignoreCase = true) }
        val journeyIdForDuplicateCheck = matchedJourney?.id ?: dialog.selectedJourneyId

        when {
            journeyName.isBlank() -> showDialogValidation(text(R.string.lookout_validation_choose_journey))
            targetHasInvalidNumber || targetHours < 0 || targetMinutes < 0 || totalTargetMinutes <= 0 -> showDialogValidation(text(R.string.lookout_validation_target_time))
            targetMinutes !in 0L..59L -> showDialogValidation(text(R.string.lookout_validation_minutes_range))
            dialog.startDate.isBefore(LocalDate.now()) -> showDialogValidation(text(R.string.lookout_validation_start_date))
            journeyIdForDuplicateCheck != null && hasDuplicateActiveObjective(journeyIdForDuplicateCheck, dialog.period) -> {
                val duplicateJourneyName = matchedJourney?.name ?: journeyName
                showDialogValidation(text(R.string.lookout_duplicate_objective, periodLabel(dialog.period), duplicateJourneyName))
            }
            else -> {
                val journeyId = matchedJourney?.id ?: journeyRepository.getOrCreateTagId(journeyName)
                val journeySnapshot = matchedJourney?.name ?: journeyName
                val zone = ZoneId.systemDefault()
                val startMs = dialog.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val now = System.currentTimeMillis()
                lookoutRepository.insertObjective(
                    ObjectiveEntity(
                        journeyId = journeyId,
                        journeyNameSnapshot = journeySnapshot,
                        periodType = dialog.period.storageValue,
                        objectiveType = dialog.kind.storageValue,
                        targetDurationMs = totalTargetMinutes * MILLIS_PER_MINUTE,
                        startAtMs = startMs,
                        weeklyBoundaryDay = if (dialog.period == ObjectivePeriod.Weekly) dialog.startDate.dayOfWeek.value else null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _uiState.update { it.copy(setObjectiveDialog = null) }
                _events.emit(text(R.string.lookout_objective_set, journeySnapshot))
            }
        }
    }

    fun claimReward(completionId: Long) = viewModelScope.launch {
        val completion = lookoutRepository.claimObjectivePearls(completionId) ?: return@launch
        val period = ObjectivePeriod.fromStorage(completion.periodType)
        val kind = ObjectiveKind.fromStorage(completion.objectiveType)
        val bonusPct = ((completion.streakMultiplier - 1.0) * 100).toInt()
        _uiState.update {
            it.copy(
                rewardDialog = ObjectiveRewardDialogState(
                    title = text(R.string.lookout_pearls_claimed_title),
                    body = text(R.string.lookout_objective_complete_body, completion.journeyNameSnapshot, periodLabel(period)),
                    pearls = text(R.string.lookout_pearls_delta, completion.finalRewardPearls),
                    badge = text(R.string.lookout_badge_reward, completion.journeyNameSnapshot, periodLabel(period)),
                    streakBonus = if (bonusPct > 0) text(R.string.lookout_streak_bonus_percent, bonusPct) else null,
                    currentStreak = if (kind == ObjectiveKind.Recurring) text(R.string.lookout_current_streak_value, completion.streakBeforeCompletion + 1) else null
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
            val sourceData = combine(
                lookoutRepository.observeObjectives(),
                lookoutRepository.observeCompletions(),
                lookoutRepository.observeSkippedCycles(),
                flowRepository.getAllSessions(),
                journeyRepository.getAllTags()
            ) { objectives, completions, skipped, sessions, journeys ->
                LookoutSourceData(objectives, completions, skipped, sessions, journeys)
            }

            combine(sourceData, nowTick) { data, now ->
                latestObjectives = data.objectives
                latestJourneys = data.journeys
                val flows = data.sessions.map { it.toObjectiveSourceFlow() }
                val result = calculator.calculate(data.objectives, flows, data.completions, data.skipped, now, ZoneId.systemDefault())

                // V1 evaluates Objective completions while The Lookout is active/open.
                // A future pass can move this into the post-Flow reward pipeline for immediate grants.
                result.completionsToGrant.forEach { grant ->
                    lookoutRepository.applyCompletionGrant(grant.completion, grant.newCurrentStreak, grant.newMaxStreak, grant.newTotalCompletions)
                }
                result.streakResets.forEach { lookoutRepository.resetStreak(it.objectiveId) }
                Triple(result.cards, data.journeys, data.completions) to now
            }.catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }.collect { (payload, now) ->
                val (cards, journeys, completions) = payload
                latestCards = cards
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        journeys = journeys.map { LookoutJourneyUiState(it.id, it.name) },
                        daily = cards.toPeriodState(ObjectivePeriod.Daily, now),
                        weekly = cards.toPeriodState(ObjectivePeriod.Weekly, now),
                        monthly = cards.toPeriodState(ObjectivePeriod.Monthly, now),
                        completedHistory = completions.toCompletedHistory(),
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun List<ObjectiveCardModel>.toPeriodState(period: ObjectivePeriod, now: Instant): ObjectivePeriodUiState {
        val periodCards = filter { it.period == period }
        val inProgress = periodCards.filter { it.state == ObjectiveCardState.InProgress }.sortedByDescending { it.progressPercent }.map { it.toUiState(now) }
        val completed = periodCards.filter { it.state == ObjectiveCardState.Completed }.sortedByDescending { it.completion?.completedAt ?: 0L }.map { it.toUiState(now) }
        val upcoming = periodCards.filter { it.state == ObjectiveCardState.Upcoming }.sortedBy { it.window.startMs }.map { it.toUiState(now) }
        val summary = when {
            inProgress.isNotEmpty() -> text(R.string.lookout_summary_in_progress, inProgress.size, completed.size)
            completed.isNotEmpty() -> text(R.string.lookout_summary_completed, completed.size)
            upcoming.isNotEmpty() -> text(R.string.lookout_summary_upcoming, upcoming.size)
            else -> text(R.string.lookout_no_period_objectives, periodLabel(period))
        }
        return ObjectivePeriodUiState(period, text(R.string.lookout_period_objectives_title, periodLabel(period)), summary, inProgress, completed, upcoming)
    }

    private fun ObjectiveCardModel.toUiState(now: Instant): ObjectiveCardUiState {
        val target = formatDuration(objective.targetDurationMs)
        val targetPearls = (objective.targetDurationMs / MILLIS_PER_MINUTE).coerceAtLeast(1)
        val progress = formatDuration(progressDurationMs)
        val bonusPct = completion?.let { ((it.streakMultiplier - 1.0) * 100).toInt() } ?: (effectiveCurrentStreak * 10)
        val nowMs = now.toEpochMilli()
        return ObjectiveCardUiState(
            objectiveId = objective.id,
            journeyId = objective.journeyId,
            journeyName = objective.journeyNameSnapshot,
            periodLabel = periodLabel(period),
            typeLabel = kindLabel(kind),
            progressPercent = progressPercent,
            progressLabel = if (state == ObjectiveCardState.Upcoming) text(R.string.lookout_target_duration, target) else text(R.string.lookout_progress_duration, progress, target),
            timeLeftLabel = when (state) {
                ObjectiveCardState.Upcoming -> text(R.string.lookout_starts_in, formatRemaining(window.startMs - nowMs))
                ObjectiveCardState.InProgress -> text(R.string.lookout_time_left, formatRemaining(window.endMs - nowMs))
                ObjectiveCardState.Completed -> if (kind == ObjectiveKind.Recurring) {
                    text(R.string.lookout_completed_next_reset, formatResetDay(window.endMs))
                } else {
                    text(R.string.lookout_completed)
                }
            },
            estimatedRewardLabel = completion?.let { text(R.string.lookout_pearls_delta, it.finalRewardPearls) } ?: text(R.string.lookout_pearls_value, targetPearls),
            isRecurring = kind == ObjectiveKind.Recurring,
            currentStreak = if (kind == ObjectiveKind.Recurring) effectiveCurrentStreak else null,
            maxStreak = if (kind == ObjectiveKind.Recurring) objective.maxStreak else null,
            totalCompletions = if (kind == ObjectiveKind.Recurring) objective.totalCompletions else null,
            streakBonusLabel = if (kind == ObjectiveKind.Recurring && bonusPct > 0) text(R.string.lookout_streak_bonus_percent, bonusPct) else null,
            rewardPearls = completion?.finalRewardPearls,
            completionId = completion?.id,
            pearlsClaimed = completion?.pearlsClaimed == true,
            state = state
        )
    }

    private fun List<ObjectiveCompletionEntity>.toCompletedHistory(): List<CompletedObjectiveHistoryGroupUiState> {
        val zone = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        return groupBy { it.journeyId to it.journeyNameSnapshot }
            .map { (journeyKey, journeyCompletions) ->
                val rows = journeyCompletions
                    .groupBy { it.periodType }
                    .map { (periodType, periodCompletions) ->
                        val period = ObjectivePeriod.fromStorage(periodType)
                        val count = periodCompletions.size
                        val claimedPearls = periodCompletions.filter { it.pearlsClaimed }.sumOf { it.finalRewardPearls }
                        val waitingPearls = periodCompletions.filterNot { it.pearlsClaimed }.sumOf { it.finalRewardPearls }
                        val lastCompleted = periodCompletions.maxOf { it.completedAt }
                        CompletedObjectiveHistoryRowUiState(
                            title = text(R.string.lookout_history_row_title, journeyKey.second, periodLabel(period)),
                            summary = if (waitingPearls > 0) {
                                text(R.string.lookout_history_row_summary_with_waiting, count, claimedPearls, waitingPearls)
                            } else {
                                text(R.string.lookout_history_row_summary_claimed, count, claimedPearls)
                            },
                            lastCompletedLabel = text(
                                R.string.lookout_history_last_completed,
                                dateFormatter.format(Instant.ofEpochMilli(lastCompleted).atZone(zone))
                            ),
                            periodOrder = period.ordinal
                        )
                    }
                    .sortedBy { it.periodOrder }
                CompletedObjectiveHistoryGroupUiState(
                    journeyName = journeyKey.second,
                    rows = rows,
                    lastCompletedAt = journeyCompletions.maxOf { it.completedAt }
                )
            }
            .sortedByDescending { it.lastCompletedAt }
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
                delay(LOOKOUT_TICK_MS)
                nowTick.value = Instant.now()
            }
        }
    }

    private fun SessionEntity.toObjectiveSourceFlow() = ObjectiveSourceFlow(id, tagId, startTime, endTime, durationMs, isSoftMode)

    private fun formatResetDay(endMs: Long): String =
        DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(Instant.ofEpochMilli(endMs).atZone(ZoneId.systemDefault()))

    private fun formatDuration(ms: Long): String {
        val minutes = (ms / MILLIS_PER_MINUTE).coerceAtLeast(0)
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) text(R.string.lookout_hours_short, h) else text(R.string.lookout_hours_minutes_short, h, m)
        } else text(R.string.lookout_minutes_short, minutes)
    }

    private fun formatRemaining(ms: Long): String {
        var minutes = (ms / MILLIS_PER_MINUTE).coerceAtLeast(0)
        val days = minutes / (24 * 60)
        minutes %= 24 * 60
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            days > 0 -> if (hours > 0) text(R.string.lookout_days_hours_short, days, hours) else text(R.string.lookout_days_short, days)
            hours > 0 -> if (mins > 0) text(R.string.lookout_hours_minutes_short, hours, mins) else text(R.string.lookout_hours_short, hours)
            else -> text(R.string.lookout_minutes_compact, mins)
        }
    }

    private fun periodLabel(period: ObjectivePeriod): String = when (period) {
        ObjectivePeriod.Daily -> text(R.string.lookout_period_daily)
        ObjectivePeriod.Weekly -> text(R.string.lookout_period_weekly)
        ObjectivePeriod.Monthly -> text(R.string.lookout_period_monthly)
    }

    private fun kindLabel(kind: ObjectiveKind): String = when (kind) {
        ObjectiveKind.OneTime -> text(R.string.lookout_type_one_time)
        ObjectiveKind.Recurring -> text(R.string.lookout_type_recurring)
    }

    private fun text(@StringRes resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
