package com.kingkharnivore.skillz.ui.screen.chronicle

import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity
import com.kingkharnivore.skillz.data.repository.ChronicleRepository
import com.kingkharnivore.skillz.model.ui.ChronicleMomentUi
import android.net.Uri
import java.io.File
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.kingkharnivore.skillz.data.chronicle.LiveDictationEngine
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import java.util.concurrent.ConcurrentHashMap

data class ChronicleUiState(
    val chronicleId: String? = null,
    val draft: String = "",
    val draftField: TextFieldValue = TextFieldValue(),
    val moments: List<ChronicleMomentEntity> = emptyList(),
    val contentMoments: List<ChronicleMomentUi> = emptyList(),
    val editingId: String? = null,
    val editingText: String = "",
    val editingField: TextFieldValue = TextFieldValue(),
    val editingMediaId: String? = null,
    val editingMediaOriginal: List<ChronicleMediaItemEntity> = emptyList(),
    val editingMediaItems: List<ChronicleMediaItemEntity> = emptyList(),
    val editingMediaStaged: List<ChronicleMediaItemEntity> = emptyList(),
    val editingTranscriptId: String? = null,
    val editingTranscriptField: TextFieldValue = TextFieldValue(),
    val transcriptions: Map<String, ChronicleTranscriptionState> = emptyMap(),
    val transcriptionSupported: Boolean = false,
    val isCommitting: Boolean = false,
    val hasError: Boolean = false,
    val pendingDeletion: ChronicleMomentUi? = null,
    val draggingId: String? = null,
    val stagedOrder: List<String> = emptyList(),
    val microphone: ChronicleMicrophoneState = ChronicleMicrophoneState.Idle,
    val speechUnavailable: Boolean = false,
    val microphoneUnavailable: Boolean = false
) {
    val hasActiveEditor: Boolean get() = editingId != null || editingMediaId != null || editingTranscriptId != null
    val blocksCompletion: Boolean get() = hasActiveEditor || isCommitting ||
        microphone !is ChronicleMicrophoneState.Idle
    val blocksPager: Boolean get() = microphone !is ChronicleMicrophoneState.Idle
}

sealed interface ChronicleMicrophoneState {
    data object Idle : ChronicleMicrophoneState
    data object Dictating : ChronicleMicrophoneState
    data class RecordingVoice(val elapsedMs: Long = 0, val amplitudes: List<Float> = emptyList()) : ChronicleMicrophoneState
}

sealed interface ChronicleTranscriptionState {
    data class Transcribing(val partial: String = "") : ChronicleTranscriptionState
    data object Failed : ChronicleTranscriptionState
}

class ChronicleStateHolder(
    private val ownerType: String,
    private val ownerKey: String,
    private val repository: ChronicleRepository,
    parentScope: CoroutineScope
) {
    private enum class Lifecycle { ACTIVE, PREPARING, CLOSED }
    private val holderJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + holderJob)
    private val _state = MutableStateFlow(ChronicleUiState())
    val state: StateFlow<ChronicleUiState> = _state.asStateFlow()
    private var draftJob: Job? = null
    private var draftGeneration = 0L
    private var syncedGeneration = 0L
    @Volatile private var lifecycle = Lifecycle.ACTIVE
    private val acceptingMutations: Boolean get() = lifecycle == Lifecycle.ACTIVE
    private var accumulatedDragPx = 0f
    private var pendingDeleteJob: Job? = null
    private var voiceTicker: Job? = null
    private var voiceStartPending = false
    private var finishVoiceWhenStarted = false
    private var discardVoiceWhenStarted = false
    private var dictationSession: DictationTextSession? = null
    private var dictationTargetsEditor = false
    private var dictationOperation = 0L
    private var dictationFinishPending = false
    private val transcriptionJobs = ConcurrentHashMap<String, Job>()

    init {
        _state.update { it.copy(transcriptionSupported = repository.isTranscriptionSupported()) }
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
                        draftField = if (draftGeneration == syncedGeneration) {
                            TextFieldValue(persisted, TextRange(persisted.length))
                        } else current.draftField,
                        moments = snapshot.moments
                    ) }
                }
        }
        scope.launch {
            repository.observeContent(ownerType, ownerKey)
                .catch { _state.update { state -> state.copy(hasError = true) } }
                .collectLatest { snapshot ->
                    _state.update { it.copy(contentMoments = snapshot.moments) }
                }
        }
    }

    fun importMedia(sources: List<Uri>, onResult: (Int) -> Unit = {}) {
        if (!acceptingMutations || sources.isEmpty()) {
            onResult(sources.size)
            return
        }
        _state.update { it.copy(isCommitting = true, hasError = false) }
        scope.launch {
            val mediaEditId = _state.value.editingMediaId
            if (mediaEditId == null) {
                val result = runCatching { repository.importMedia(ownerType, ownerKey, sources) }
                result.onSuccess { imported ->
                    if (imported.momentId == null) _state.update { it.copy(hasError = true) }
                }.onFailure { _state.update { it.copy(hasError = true) } }
                onResult(result.getOrNull()?.failedCount ?: sources.size)
            } else {
                val result = runCatching { repository.stageMedia(ownerType, ownerKey, sources) }
                result.onSuccess { staged ->
                    if (acceptingMutations && _state.value.editingMediaId == mediaEditId) {
                        _state.update { current ->
                            val additions = staged.items.map { it.copy(momentId = mediaEditId) }
                            current.copy(
                                editingMediaItems = (current.editingMediaItems + additions)
                                    .mapIndexed { index, item -> item.copy(position = index) },
                                editingMediaStaged = current.editingMediaStaged + additions,
                            )
                        }
                    } else repository.discardStagedMedia(staged.items)
                }.onFailure { _state.update { it.copy(hasError = true) } }
                onResult(result.getOrNull()?.failedCount ?: sources.size)
            }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun createCameraStagingFile(video: Boolean): File? =
        if (acceptingMutations) runCatching { repository.createCameraStagingFile(video) }.getOrNull() else null

    fun discardCapture(uri: Uri) {
        scope.launch { repository.discardCapture(uri) }
    }

    fun startVoice() {
        if (!acceptingMutations || voiceStartPending || _state.value.microphone !is ChronicleMicrophoneState.Idle || _state.value.isCommitting) return
        voiceStartPending = true
        scope.launch {
            runCatching { repository.startVoice(ownerType, ownerKey) }
                .onSuccess {
                    voiceStartPending = false
                    val startedAt = System.currentTimeMillis()
                    _state.update { it.copy(microphone = ChronicleMicrophoneState.RecordingVoice(), hasError = false,
                        microphoneUnavailable = false) }
                    if (discardVoiceWhenStarted) {
                        discardVoiceWhenStarted = false
                        discardVoice()
                        return@onSuccess
                    }
                    voiceTicker = scope.launch {
                        while (_state.value.microphone is ChronicleMicrophoneState.RecordingVoice) {
                            val amplitude = (repository.voiceAmplitude() / 32767f).coerceIn(0f, 1f)
                            _state.update { current ->
                                val recording = current.microphone as? ChronicleMicrophoneState.RecordingVoice
                                current.copy(microphone = recording?.copy(
                                    elapsedMs = System.currentTimeMillis() - startedAt,
                                    amplitudes = (recording.amplitudes + amplitude).takeLast(48)
                                ) ?: ChronicleMicrophoneState.Idle)
                            }
                            delay(100)
                        }
                    }
                    if (finishVoiceWhenStarted) {
                        finishVoiceWhenStarted = false
                        finishVoice()
                    }
                }
                .onFailure {
                    voiceStartPending = false
                    finishVoiceWhenStarted = false
                    discardVoiceWhenStarted = false
                    _state.update { it.copy(hasError = true) }
                }
        }
    }

    fun microphonePermissionDenied() {
        _state.update { it.copy(microphoneUnavailable = true) }
    }

    fun finishVoice() {
        if (voiceStartPending) {
            finishVoiceWhenStarted = true
            return
        }
        if (_state.value.microphone !is ChronicleMicrophoneState.RecordingVoice) return
        voiceTicker?.cancel()
        _state.update { it.copy(microphone = ChronicleMicrophoneState.Idle, isCommitting = true) }
        scope.launch {
            runCatching { repository.finishVoice(ownerType, ownerKey) }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun discardVoice() {
        if (voiceStartPending) {
            discardVoiceWhenStarted = true
            finishVoiceWhenStarted = false
            return
        }
        finishVoiceWhenStarted = false
        voiceTicker?.cancel()
        repository.discardVoice()
        _state.update { it.copy(microphone = ChronicleMicrophoneState.Idle) }
    }

    fun startDictation() {
        val current = _state.value
        if (!acceptingMutations || voiceStartPending || current.microphone !is ChronicleMicrophoneState.Idle ||
            current.isCommitting || current.editingMediaId != null || current.editingTranscriptId != null) return
        if (!repository.isDictationAvailable()) {
            _state.update { it.copy(speechUnavailable = true) }
            return
        }
        val editor = _state.value.editingId != null
        val original = if (editor) _state.value.editingField else _state.value.draftField
        if (!editor) {
            draftJob?.cancel()
            ++draftGeneration
        }
        val session = DictationTextSession(original)
        val operation = ++dictationOperation
        dictationSession = session
        dictationTargetsEditor = editor
        dictationFinishPending = false
        _state.update { it.copy(microphone = ChronicleMicrophoneState.Dictating, speechUnavailable = false,
            microphoneUnavailable = false) }
        val started = runCatching { repository.startDictation(object : LiveDictationEngine.Listener {
            override fun onPartial(text: String) = applyDictation(operation, session.partial(text))
            override fun onFinal(text: String) {
                applyDictation(operation, session.partial(text))
                finishDictation(fromEngine = true)
            }
            override fun onError() {
                if (operation == dictationOperation) cancelDictation(showUnavailable = true)
            }
        }) }.getOrDefault(false)
        if (!started && operation == dictationOperation) cancelDictation(showUnavailable = true)
    }

    private fun applyDictation(operation: Long, value: TextFieldValue) {
        if (operation != dictationOperation || _state.value.microphone !is ChronicleMicrophoneState.Dictating) return
        if (dictationTargetsEditor) _state.update { it.copy(editingText = value.text, editingField = value) }
        else _state.update { it.copy(draft = value.text, draftField = value) }
    }

    fun finishDictation(fromEngine: Boolean = false) {
        if (_state.value.microphone !is ChronicleMicrophoneState.Dictating) return
        if (!fromEngine) {
            if (dictationFinishPending) return
            dictationFinishPending = true
            repository.finishDictation()
            return
        }
        dictationSession?.finish()
        ++dictationOperation
        dictationFinishPending = false
        dictationSession = null
        _state.update { it.copy(microphone = ChronicleMicrophoneState.Idle) }
        if (!dictationTargetsEditor) setDraft(_state.value.draftField)
    }

    fun cancelDictation(showUnavailable: Boolean = false) {
        val session = dictationSession ?: return
        ++dictationOperation
        dictationFinishPending = false
        repository.cancelDictation()
        val original = session.cancel()
        if (dictationTargetsEditor) _state.update { it.copy(editingText = original.text, editingField = original,
            microphone = ChronicleMicrophoneState.Idle, speechUnavailable = showUnavailable) }
        else {
            _state.update { it.copy(draft = original.text, draftField = original,
                microphone = ChronicleMicrophoneState.Idle, speechUnavailable = showUnavailable) }
            setDraft(original)
        }
        dictationSession = null
    }

    fun setDraft(value: String) {
        setDraft(TextFieldValue(value, TextRange(value.length)))
    }
    fun setDraft(value: TextFieldValue) {
        if (!acceptingMutations) return
        ++draftGeneration
        _state.update { it.copy(draft = value.text, draftField = value, hasError = false) }
        draftJob?.cancel()
        draftJob = scope.launch {
            delay(250)
            runCatching { repository.setDraft(ownerType, ownerKey, value.text) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun add(onSuccess: (() -> Unit)? = null) {
        if (!acceptingMutations || _state.value.isCommitting ||
            _state.value.hasActiveEditor || _state.value.microphone !is ChronicleMicrophoneState.Idle ||
            _state.value.draft.isBlank()) return
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
                    _state.update { state -> state.copy(draft = "", draftField = TextFieldValue()) }
                }
                onSuccess?.invoke()
            }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun beginEdit(moment: ChronicleMomentEntity) {
        val current = _state.value
        if (!acceptingMutations || current.hasActiveEditor || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        _state.update { it.copy(
            editingId = moment.id,
            editingText = moment.text.orEmpty(),
            editingField = TextFieldValue(moment.text.orEmpty(), TextRange(moment.text.orEmpty().length)),
        ) }
    }
    fun editText(value: String) {
        editText(TextFieldValue(value, TextRange(value.length)))
    }
    fun editText(value: TextFieldValue) {
        if (acceptingMutations && _state.value.microphone !is ChronicleMicrophoneState.Dictating)
            _state.update { it.copy(editingText = value.text, editingField = value) }
    }
    fun cancelEdit() = _state.update { it.copy(editingId = null, editingText = "", editingField = TextFieldValue()) }

    fun beginMediaEdit(moment: ChronicleMomentUi.Media) {
        val current = _state.value
        if (!acceptingMutations || current.hasActiveEditor || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        val rows = moment.items.map { item ->
            ChronicleMediaItemEntity(
                id = item.id,
                momentId = moment.id,
                position = item.position,
                localPath = item.relativePath,
                mimeType = item.mimeType,
                durationMs = item.durationMs,
                width = item.width,
                height = item.height,
                createdAt = item.createdAt,
            )
        }
        _state.update { it.copy(
            editingMediaId = moment.id,
            editingMediaOriginal = rows,
            editingMediaItems = rows,
            editingMediaStaged = emptyList(),
        ) }
    }

    fun moveMediaItem(id: String, delta: Int) {
        _state.update { current ->
            val items = current.editingMediaItems.toMutableList()
            val from = items.indexOfFirst { it.id == id }
            val to = (from + delta).coerceIn(0, items.lastIndex)
            if (from < 0 || from == to) current else current.copy(
                editingMediaItems = items.apply { add(to, removeAt(from)) }
                    .mapIndexed { index, item -> item.copy(position = index) }
            )
        }
    }

    fun removeMediaItem(id: String) {
        _state.update { current ->
            if (current.editingMediaItems.size <= 1) current else current.copy(
                editingMediaItems = current.editingMediaItems.filterNot { it.id == id }
                    .mapIndexed { index, item -> item.copy(position = index) }
            )
        }
    }

    fun cancelMediaEdit() {
        val current = _state.value
        val additions = current.editingMediaStaged
        _state.update { it.copy(
            editingMediaId = null,
            editingMediaOriginal = emptyList(),
            editingMediaItems = emptyList(),
            editingMediaStaged = emptyList(),
        ) }
        if (additions.isNotEmpty()) scope.launch { repository.discardStagedMedia(additions) }
    }

    fun finishMediaEdit() {
        val current = _state.value
        val momentId = current.editingMediaId ?: return
        if (!acceptingMutations || current.editingMediaItems.isEmpty() || current.isCommitting) return
        val finalItems = current.editingMediaItems.mapIndexed { index, item ->
            item.copy(momentId = momentId, position = index)
        }
        val finalIds = finalItems.mapTo(mutableSetOf()) { it.id }
        val unusedStaged = current.editingMediaStaged.filterNot { it.id in finalIds }
        _state.update { it.copy(isCommitting = true, hasError = false) }
        scope.launch {
            runCatching { repository.replaceMedia(ownerType, ownerKey, momentId, finalItems) }
                .onSuccess {
                    repository.discardStagedMedia(unusedStaged)
                    _state.update { it.copy(
                        editingMediaId = null,
                        editingMediaOriginal = emptyList(),
                        editingMediaItems = emptyList(),
                        editingMediaStaged = emptyList(),
                    ) }
                }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }

    fun removeEditingMediaMoment() {
        val current = _state.value
        val momentId = current.editingMediaId ?: return
        val entity = current.moments.firstOrNull { it.id == momentId } ?: return
        val additions = current.editingMediaStaged
        _state.update { it.copy(
            editingMediaId = null,
            editingMediaOriginal = emptyList(),
            editingMediaItems = emptyList(),
            editingMediaStaged = emptyList(),
            isCommitting = true,
        ) }
        scope.launch {
            runCatching {
                repository.discardStagedMedia(additions)
                repository.deleteMoment(ownerType, ownerKey, entity)
            }.onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }
    fun finishEdit() {
        val current = _state.value
        if (!acceptingMutations || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        val moment = current.moments.firstOrNull { it.id == current.editingId } ?: return
        val value = current.editingText
        if (value.isBlank()) return
        _state.update { it.copy(isCommitting = true, hasError = false) }
        scope.launch {
            runCatching { repository.updateText(ownerType, ownerKey, moment, value) }
                .onSuccess { cancelEdit() }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }
    fun delete(moment: ChronicleMomentEntity) {
        if (!acceptingMutations) return
        cancelTranscription(moment.id)
        scope.launch {
            runCatching { repository.deleteMoment(ownerType, ownerKey, moment) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }
    fun delete(moment: ChronicleMomentUi) {
        _state.value.moments.firstOrNull { it.id == moment.id }?.let(::delete)
    }

    /**
     * Optimistically hides a Moment while leaving its database rows and files intact long enough
     * for the user to undo. A holder-owned timeout guarantees that leaving the page still commits
     * the explicit removal request.
     */
    fun requestDelete(moment: ChronicleMomentUi) {
        val current = _state.value
        if (!acceptingMutations || current.isCommitting) return
        current.pendingDeletion?.let { commitPendingDelete(it.id) }
        cancelTranscription(moment.id)
        pendingDeleteJob?.cancel()
        _state.update { it.copy(pendingDeletion = moment, hasError = false) }
        pendingDeleteJob = scope.launch {
            delay(PENDING_DELETE_FALLBACK_MS)
            commitPendingDelete(moment.id)
        }
    }

    fun undoPendingDelete(momentId: String) {
        if (_state.value.pendingDeletion?.id != momentId) return
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _state.update { it.copy(pendingDeletion = null) }
    }

    fun commitPendingDelete(momentId: String) {
        val pending = _state.value.pendingDeletion?.takeIf { it.id == momentId } ?: return
        val entity = _state.value.moments.firstOrNull { it.id == pending.id }
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _state.update { it.copy(pendingDeletion = null) }
        if (entity != null) delete(entity)
    }

    fun startTranscription(moment: ChronicleMomentUi.Voice) {
        if (!acceptingMutations || !moment.isAvailable ||
            !repository.isTranscriptionSupported() || transcriptionJobs.containsKey(moment.id)) return
        _state.update { current ->
            current.copy(transcriptions = current.transcriptions +
                (moment.id to ChronicleTranscriptionState.Transcribing()))
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val saved = repository.transcribeVoice(ownerType, ownerKey, moment.id) { partial ->
                    _state.update { current ->
                        if (transcriptionJobs.containsKey(moment.id)) current.copy(
                            transcriptions = current.transcriptions +
                                (moment.id to ChronicleTranscriptionState.Transcribing(partial))
                        ) else current
                    }
                }
                _state.update { current -> current.copy(
                    transcriptions = if (saved) current.transcriptions - moment.id
                    else current.transcriptions + (moment.id to ChronicleTranscriptionState.Failed)
                ) }
            } catch (cancelled: CancellationException) {
                _state.update { current -> current.copy(transcriptions = current.transcriptions - moment.id) }
                throw cancelled
            } catch (_: Exception) {
                _state.update { current -> current.copy(
                    transcriptions = current.transcriptions + (moment.id to ChronicleTranscriptionState.Failed)
                ) }
            } finally {
                transcriptionJobs.remove(moment.id)
            }
        }
        transcriptionJobs[moment.id] = job
        job.start()
    }

    fun cancelTranscription(momentId: String) {
        transcriptionJobs.remove(momentId)?.cancel()
        _state.update { current -> current.copy(transcriptions = current.transcriptions - momentId) }
    }

    fun beginTranscriptEdit(moment: ChronicleMomentUi.Voice) {
        val current = _state.value
        val editableTranscript = if (moment.transcriptEdited) moment.transcript
            else moment.originalTranscript ?: moment.transcript
        if (!acceptingMutations || editableTranscript == null || current.hasActiveEditor || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        cancelTranscription(moment.id)
        _state.update { it.copy(
            editingTranscriptId = moment.id,
            editingTranscriptField = TextFieldValue(editableTranscript, TextRange(editableTranscript.length)),
        ) }
    }

    fun editTranscript(value: TextFieldValue) {
        if (acceptingMutations) _state.update { it.copy(editingTranscriptField = value) }
    }

    fun cancelTranscriptEdit() {
        _state.update { it.copy(editingTranscriptId = null, editingTranscriptField = TextFieldValue()) }
    }

    fun finishTranscriptEdit() {
        val current = _state.value
        val momentId = current.editingTranscriptId ?: return
        if (!acceptingMutations || current.isCommitting) return
        val value = current.editingTranscriptField.text.trim().takeIf(String::isNotEmpty)
        _state.update { it.copy(isCommitting = true) }
        scope.launch {
            runCatching { repository.updateTranscript(ownerType, ownerKey, momentId, value, manuallyEdited = true) }
                .onSuccess { cancelTranscriptEdit() }
                .onFailure { _state.update { it.copy(hasError = true) } }
            _state.update { it.copy(isCommitting = false) }
        }
    }
    fun move(moment: ChronicleMomentEntity, delta: Int) {
        val current = _state.value
        if (!acceptingMutations || current.hasActiveEditor || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        val items = _state.value.moments.toMutableList()
        val from = items.indexOfFirst { it.id == moment.id }
        val to = (from + delta).coerceIn(0, items.lastIndex)
        if (from < 0 || from == to) return
        items.add(to, items.removeAt(from))
        scope.launch {
            runCatching { repository.reorder(ownerType, ownerKey, moment.chronicleId, items.map { it.id }) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }
    fun move(moment: ChronicleMomentUi, delta: Int) {
        _state.value.moments.firstOrNull { it.id == moment.id }?.let { move(it, delta) }
    }
    fun startDrag(moment: ChronicleMomentEntity) {
        val current = _state.value
        if (!acceptingMutations || current.hasActiveEditor || current.isCommitting ||
            current.microphone !is ChronicleMicrophoneState.Idle) return
        _state.update {
        accumulatedDragPx = 0f
        if (it.hasActiveEditor) it else it.copy(
            draggingId = moment.id,
            stagedOrder = it.moments.map(ChronicleMomentEntity::id)
        )
        }
    }
    fun startDrag(moment: ChronicleMomentUi) {
        _state.value.moments.firstOrNull { it.id == moment.id }?.let(::startDrag)
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
        _state.update { it.copy(draft = "", draftField = TextFieldValue()) }
        scope.launch {
            runCatching { repository.setDraft(ownerType, ownerKey, "") }
                .onSuccess { onSuccess?.invoke() }
                .onFailure {
                    _state.update { it.copy(draft = previous,
                        draftField = TextFieldValue(previous, TextRange(previous.length)), hasError = true) }
                }
        }
    }

    /** Re-enables a prepared holder only when the durable owner was not finalized. */
    fun resumeAfterPreCommitFailure() {
        if (holderJob.isActive && lifecycle == Lifecycle.PREPARING) lifecycle = Lifecycle.ACTIVE
    }

    /** Permanently closes a holder after its owner promotion has durably committed. */
    fun finalizeTransition() {
        lifecycle = Lifecycle.CLOSED
        draftJob?.cancel()
    }

    /** Flushes the newest local value, then permanently prevents this holder from writing. */
    fun quiesce(onReady: () -> Unit) {
        if (!acceptingMutations) return
        lifecycle = Lifecycle.PREPARING
        val value = _state.value.draft
        val generation = draftGeneration
        val pendingDelete = _state.value.pendingDeletion
            ?.let { pending -> _state.value.moments.firstOrNull { it.id == pending.id } }
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _state.update { it.copy(pendingDeletion = null) }
        draftJob?.cancel()
        scope.launch {
            val result = runCatching {
                if (pendingDelete != null) repository.deleteMoment(ownerType, ownerKey, pendingDelete)
                if (generation != syncedGeneration) repository.setDraft(ownerType, ownerKey, value)
            }
            if (result.isSuccess) {
                onReady()
            } else {
                lifecycle = Lifecycle.ACTIVE
                _state.update { it.copy(hasError = true) }
            }
        }
    }

    fun discardAndQuiesce(onReady: () -> Unit) {
        if (!acceptingMutations) return
        lifecycle = Lifecycle.PREPARING
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _state.update { it.copy(pendingDeletion = null) }
        draftJob?.cancel()
        scope.launch {
            runCatching { repository.discard(ownerType, ownerKey) }
                .onSuccess { onReady() }
                .onFailure {
                    lifecycle = Lifecycle.ACTIVE
                    _state.update { it.copy(hasError = true) }
                }
        }
    }

    fun close() {
        lifecycle = Lifecycle.CLOSED
        cancelDictation()
        discardVoice()
        val current = _state.value
        val additions = current.editingMediaStaged
        val pendingDelete = current.pendingDeletion
            ?.let { pending -> current.moments.firstOrNull { it.id == pending.id } }
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        transcriptionJobs.values.forEach(Job::cancel)
        transcriptionJobs.clear()
        if (additions.isEmpty() && pendingDelete == null) holderJob.cancel()
        else scope.launch {
            repository.discardStagedMedia(additions)
            if (pendingDelete != null) {
                runCatching { repository.deleteMoment(ownerType, ownerKey, pendingDelete) }
            }
            holderJob.cancel()
        }
    }

    private companion object {
        const val PENDING_DELETE_FALLBACK_MS = 30_000L
    }
}
