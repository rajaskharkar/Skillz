package com.kingkharnivore.skillz.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.kingkharnivore.skillz.data.repository.FlowRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns the question and its post-completion destination across process recreation. */
internal class RepeatedFlowPromptState(
    private val savedState: SavedStateHandle,
    private val flows: FlowRepository,
    private val scope: CoroutineScope
) {
    val pendingSession = savedState.getStateFlow<Long?>(SESSION, null)
    private var needsRecovery = pendingSession.value != null
    private val _isWorking = MutableStateFlow(needsRecovery)
    val isWorking = _isWorking.asStateFlow()
    private val _hasError = MutableStateFlow(false)
    val hasError = _hasError.asStateFlow()

    fun queue(sessionId: Long, continueArc: Boolean) {
        savedState[CONTINUE_ARC] = continueArc
        savedState[SESSION] = sessionId
        _hasError.value = false
    }

    /** Restoring the destination must finish before an answer or navigation can consume it. */
    suspend fun restoreIfNeeded(restore: suspend (Long, Boolean) -> Unit): Boolean {
        if (!needsRecovery) return false
        try {
            recover(restore)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _hasError.value = true
        } finally {
            _isWorking.value = false
        }
        return true
    }

    private suspend fun recover(restore: suspend (Long, Boolean) -> Unit) {
        val sessionId = pendingSession.value ?: return
        restore(sessionId, savedState[CONTINUE_ARC] ?: false)
        // A transaction may have committed immediately before process death, or the saved
        // Flow/activity may have changed elsewhere. Do not restore an obsolete question.
        if (!flows.shouldOfferRepeatedFlow(sessionId)) clear()
        needsRecovery = false
    }

    fun answer(kind: String?, restore: suspend (Long, Boolean) -> Unit) {
        if (_isWorking.value || pendingSession.value == null) return
        _isWorking.value = true
        _hasError.value = false
        scope.launch {
            try {
                if (needsRecovery) recover(restore)
                val sessionId = pendingSession.value ?: return@launch
                if (kind != null && flows.shouldOfferRepeatedFlow(sessionId)) {
                    flows.createActivityFromRepeatedFlow(sessionId, kind)
                }
                clear()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _hasError.value = true
            } finally {
                _isWorking.value = false
            }
        }
    }

    private fun clear() {
        savedState[SESSION] = null
        savedState[CONTINUE_ARC] = null
    }

    private companion object {
        const val SESSION = "pendingRepeatedFlowSession"
        const val CONTINUE_ARC = "pendingRepeatedFlowContinueArc"
    }
}
