package com.kingkharnivore.skillz.viewmodel.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.repository.shell.ShellRepository
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.domain.achievement.MasteryEvidenceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RedUiState(val pebbles: Int = 0, val pearls: Int = 0, val creatures: List<UserShellFindInstanceEntity> = emptyList(), val masteries: Map<String, Int> = emptyMap())

@HiltViewModel
class RedViewModel @Inject constructor(private val repository: ShellRepository, private val savedState: SavedStateHandle) : ViewModel() {
    val lastEra = savedState.getStateFlow("red_last_era", com.kingkharnivore.skillz.utils.shell.RedEra.TRIASSIC.name)
    fun selectEra(era: com.kingkharnivore.skillz.utils.shell.RedEra) { savedState["red_last_era"] = era.name }
    val state = combine(repository.observePebbleBalance(), repository.observePearlBalance(), repository.observeOwnedFinds(), repository.observeCreatureMasteries(), repository.observeBadgeCountFloors()) { pebbles, pearls, creatures, masteries, floors ->
        RedUiState(pebbles, pearls, creatures, MasteryEvidenceCalculator.bySpecies(masteries, floors).mapValues { it.value.effectiveLifetimeCount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedUiState())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()
    fun clearMessage() { _message.value = null }
    fun purchase(id: String, transactionId: String) = mutate { repository.purchaseRedCreature(id, transactionId) }
    fun grow(instanceId: String, level: Int) = mutate { repository.growCreature(instanceId, "level_up:$instanceId:${level + 1}", "RED") }
    fun release(instanceId: String) = mutate { repository.releaseCreature(instanceId) }
    private fun mutate(block: suspend () -> Any) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { block(); _message.value = R.string.red_action_success }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _message.value = R.string.red_action_failed }
            finally { _busy.value = false }
        }
    }
}
