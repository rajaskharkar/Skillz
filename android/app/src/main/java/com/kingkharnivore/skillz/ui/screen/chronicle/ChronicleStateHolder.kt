package com.kingkharnivore.skillz.ui.screen.chronicle

import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

data class ChronicleUiState(
    val chronicleId: String? = null,
    val draft: String = "",
    val moments: List<ChronicleMomentEntity> = emptyList(),
    val editingId: String? = null,
    val editingText: String = "",
    val isCommitting: Boolean = false,
    val hasError: Boolean = false,
    val draggingId: String? = null,
    val stagedOrder: List<String> = emptyList()
) {
    val blocksCompletion: Boolean get() = editingId != null || isCommitting
}

class ChronicleStateHolder(
    private val ownerType: String,
    private val ownerKey: String,
    private val repository: ChronicleRepository,
    parentScope: CoroutineScope
) {
    private val holderJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + holderJob)
    private val _state = MutableStateFlow(ChronicleUiState())
    val state: StateFlow<ChronicleUiState> = _state.asStateFlow()
    private var draftJob: Job? = null
    private var draftGeneration = 0L
    private var syncedGeneration = 0L
    @Volatile private var acceptingMutations = true
    private var accumulatedDragPx = 0f

    init {
        scope.launch {
            repository.observe(ownerType, ownerKey)
                .catch { _state.update { state -> state.copy(hasError = true) } }
                .collectLatest { snapshot ->
                    _state.update { current ->
                      val persisted = snapshot.chronicle?.draftText.orEmpty()
                      if (draftGeneration != syncedGeneration && persisted == current.draft) {
                          syncedGeneration = draftGeneration
                      }
                      current.copy(
                        chronicleId = snapshot.chronicle?.id,
                        draft = if (draftGeneration == syncedGeneration) {
                            persisted
                        } else current.draft,
                        moments = snapshot.moments
                    ) }
                }
        }
    }

    fun setDraft(value: String) {
        if (!acceptingMutations) return
        ++draftGeneration
        _state.update { it.copy(draft = value, hasError = false) }
        draftJob?.cancel()
        draftJob = scope.launch {
            delay(250)
            runCatching { repository.setDraft(ownerType, ownerKey, value) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun add(onSuccess: (() -> Unit)? = null) {
        if (!acceptingMutations || _state.value.isCommitting || _state.value.draft.isBlank()) return
        val captured = _state.value.draft
        val capturedGeneration = draftGeneration
        val pendingDraftWrite = draftJob
        _state.update { it.copy(isCommitting = true, hasError = false) }
        scope.launch {
            runCatching {
                pendingDraftWrite?.join()
                repository.addText(ownerType, ownerKey, captured)
            }.onSuccess {
                if (draftGeneration == capturedGeneration) {
                    syncedGeneration = capturedGeneration
                    _state.update { state -> state.copy(draft = "") }
                }
                onSuccess?.invoke()
            }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun beginEdit(moment: ChronicleMomentEntity) =
        if (acceptingMutations) _state.update { it.copy(editingId = moment.id, editingText = moment.text.orEmpty()) } else Unit
    fun editText(value: String) = _state.update { it.copy(editingText = value) }
    fun cancelEdit() = _state.update { it.copy(editingId = null, editingText = "") }
    fun finishEdit() {
        if (!acceptingMutations) return
        val moment = _state.value.moments.firstOrNull { it.id == _state.value.editingId } ?: return
        val value = _state.value.editingText
        if (value.isBlank()) return
        scope.launch { runCatching { repository.updateText(ownerType, ownerKey, moment, value) }.onSuccess { cancelEdit() } }
    }
    fun delete(moment: ChronicleMomentEntity) {
        if (!acceptingMutations) return
        scope.launch { repository.deleteMoment(ownerType, ownerKey, moment) }
    }
    fun move(moment: ChronicleMomentEntity, delta: Int) {
        if (!acceptingMutations) return
        val items = _state.value.moments.toMutableList()
        val from = items.indexOfFirst { it.id == moment.id }
        val to = (from + delta).coerceIn(0, items.lastIndex)
        if (from < 0 || from == to) return
        items.add(to, items.removeAt(from))
        scope.launch { repository.reorder(ownerType, ownerKey, moment.chronicleId, items.map { it.id }) }
    }
    fun startDrag(moment: ChronicleMomentEntity) = _state.update {
        accumulatedDragPx = 0f
        if (it.editingId != null) it else it.copy(
            draggingId = moment.id,
            stagedOrder = it.moments.map(ChronicleMomentEntity::id)
        )
    }
    fun dragByPixels(deltaPx: Float, itemExtentPx: Float) {
        if (_state.value.draggingId == null || itemExtentPx <= 0f) return
        accumulatedDragPx += deltaPx
        while (accumulatedDragPx >= itemExtentPx) {
            dragBy(1); accumulatedDragPx -= itemExtentPx
        }
        while (accumulatedDragPx <= -itemExtentPx) {
            dragBy(-1); accumulatedDragPx += itemExtentPx
        }
    }
    fun dragBy(delta: Int) = _state.update { current ->
        val ids = current.stagedOrder.toMutableList()
        val from = ids.indexOf(current.draggingId)
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        if (from < 0 || from == to) current else current.copy(
            stagedOrder = ids.apply { add(to, removeAt(from)) }
        )
    }
    fun dragTo(targetIndex: Int) = _state.update { current ->
        val ids = current.stagedOrder.toMutableList()
        val from = ids.indexOf(current.draggingId)
        val to = targetIndex.coerceIn(0, ids.lastIndex)
        if (from < 0 || from == to) current else current.copy(
            stagedOrder = ids.apply { add(to, removeAt(from)) }
        )
    }
    fun dragToId(targetId: String) {
        val target = _state.value.stagedOrder.indexOf(targetId)
        if (target >= 0) dragTo(target)
    }
    fun finishDrag() {
        val current = _state.value
        val chronicleId = current.chronicleId
        val order = current.stagedOrder
        accumulatedDragPx = 0f
        _state.update { it.copy(draggingId = null, stagedOrder = emptyList()) }
        if (chronicleId != null && order.isNotEmpty() && order != current.moments.map { it.id }) {
            scope.launch {
                runCatching { repository.reorder(ownerType, ownerKey, chronicleId, order) }
                    .onFailure { _state.update { it.copy(hasError = true) } }
            }
        }
    }
    fun cancelDrag() {
        accumulatedDragPx = 0f
        _state.update { it.copy(draggingId = null, stagedOrder = emptyList()) }
    }
    fun discardDraft(onSuccess: (() -> Unit)? = null) {
        draftJob?.cancel()
        val previous = _state.value.draft
        ++draftGeneration
        _state.update { it.copy(draft = "") }
        scope.launch {
            runCatching { repository.setDraft(ownerType, ownerKey, "") }
                .onSuccess { onSuccess?.invoke() }
                .onFailure {
                    _state.update { it.copy(draft = previous, hasError = true) }
                }
        }
    }

    /** Re-enables a prepared holder only when the durable owner was not finalized. */
    fun resumeAfterPreCommitFailure() {
        if (holderJob.isActive) acceptingMutations = true
    }

    /** Flushes the newest local value, then permanently prevents this holder from writing. */
    fun quiesce(onReady: () -> Unit) {
        if (!acceptingMutations) return
        acceptingMutations = false
        val value = _state.value.draft
        val generation = draftGeneration
        draftJob?.cancel()
        scope.launch {
            val result = runCatching {
                if (generation != syncedGeneration) repository.setDraft(ownerType, ownerKey, value)
            }
            if (result.isSuccess) {
                onReady()
            } else {
                acceptingMutations = true
                _state.update { it.copy(hasError = true) }
            }
        }
    }

    fun discardAndQuiesce(onReady: () -> Unit) {
        if (!acceptingMutations) return
        acceptingMutations = false
        draftJob?.cancel()
        scope.launch {
            runCatching { repository.discard(ownerType, ownerKey) }
                .onSuccess { onReady() }
                .onFailure {
                    acceptingMutations = true
                    _state.update { it.copy(hasError = true) }
                }
        }
    }

    fun close() {
        acceptingMutations = false
        holderJob.cancel()
    }
}
