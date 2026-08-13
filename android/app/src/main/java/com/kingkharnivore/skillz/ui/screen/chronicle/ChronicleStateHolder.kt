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
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ChronicleUiState())
    val state: StateFlow<ChronicleUiState> = _state.asStateFlow()
    private var draftJob: Job? = null

    init {
        scope.launch {
            repository.observe(ownerType, ownerKey)
                .catch { _state.update { state -> state.copy(hasError = true) } }
                .collectLatest { snapshot ->
                    _state.update { it.copy(
                        chronicleId = snapshot.chronicle?.id,
                        draft = snapshot.chronicle?.draftText.orEmpty(),
                        moments = snapshot.moments
                    ) }
                }
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value, hasError = false) }
        draftJob?.cancel()
        draftJob = scope.launch {
            delay(250)
            runCatching { repository.setDraft(ownerType, ownerKey, value) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun add(onSuccess: (() -> Unit)? = null) {
        if (_state.value.isCommitting || _state.value.draft.isBlank()) return
        val captured = _state.value.draft
        val pendingDraftWrite = draftJob
        _state.update { it.copy(isCommitting = true, hasError = false) }
        scope.launch {
            runCatching {
                pendingDraftWrite?.join()
                repository.addText(ownerType, ownerKey, captured)
            }.onSuccess { onSuccess?.invoke() }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun beginEdit(moment: ChronicleMomentEntity) =
        _state.update { it.copy(editingId = moment.id, editingText = moment.text.orEmpty()) }
    fun editText(value: String) = _state.update { it.copy(editingText = value) }
    fun cancelEdit() = _state.update { it.copy(editingId = null, editingText = "") }
    fun finishEdit() {
        val moment = _state.value.moments.firstOrNull { it.id == _state.value.editingId } ?: return
        val value = _state.value.editingText
        if (value.isBlank()) return
        scope.launch { runCatching { repository.updateText(moment, value) }.onSuccess { cancelEdit() } }
    }
    fun delete(moment: ChronicleMomentEntity) = scope.launch { repository.deleteMoment(moment) }
    fun move(moment: ChronicleMomentEntity, delta: Int) {
        val items = _state.value.moments.toMutableList()
        val from = items.indexOfFirst { it.id == moment.id }
        val to = (from + delta).coerceIn(0, items.lastIndex)
        if (from < 0 || from == to) return
        items.add(to, items.removeAt(from))
        scope.launch { repository.reorder(moment.chronicleId, items.map { it.id }) }
    }
    fun startDrag(moment: ChronicleMomentEntity) = _state.update {
        if (it.editingId != null) it else it.copy(
            draggingId = moment.id,
            stagedOrder = it.moments.map(ChronicleMomentEntity::id)
        )
    }
    fun dragBy(delta: Int) = _state.update { current ->
        val ids = current.stagedOrder.toMutableList()
        val from = ids.indexOf(current.draggingId)
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        if (from < 0 || from == to) current else current.copy(
            stagedOrder = ids.apply { add(to, removeAt(from)) }
        )
    }
    fun finishDrag() {
        val current = _state.value
        val chronicleId = current.chronicleId
        val order = current.stagedOrder
        _state.update { it.copy(draggingId = null, stagedOrder = emptyList()) }
        if (chronicleId != null && order.isNotEmpty() && order != current.moments.map { it.id }) {
            scope.launch {
                runCatching { repository.reorder(chronicleId, order) }
                    .onFailure { _state.update { it.copy(hasError = true) } }
            }
        }
    }
    fun discardDraft(onSuccess: (() -> Unit)? = null) {
        draftJob?.cancel()
        scope.launch { repository.setDraft(ownerType, ownerKey, ""); onSuccess?.invoke() }
    }
}
