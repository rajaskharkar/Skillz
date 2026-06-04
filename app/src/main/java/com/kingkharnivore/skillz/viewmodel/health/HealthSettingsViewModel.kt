package com.kingkharnivore.skillz.viewmodel.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import com.kingkharnivore.skillz.domain.health.HealthRefreshUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthSettingsUiState(
    val healthConnectAvailable: Boolean = false,
    val readStepsPermissionGranted: Boolean = false,
    val movementBonusEnabled: Boolean = false,
    val localMovementBonusEnabled: Boolean = false,
    val pendingRefreshableFlows: Boolean = false,
    val showDisableWarning: Boolean = false,
    val isBusy: Boolean = false
) {
    val toggleChecked: Boolean get() = healthConnectAvailable && readStepsPermissionGranted && movementBonusEnabled
    val toggleEnabled: Boolean get() = healthConnectAvailable && !isBusy
}

@HiltViewModel
class HealthSettingsViewModel @Inject constructor(
    private val settingsRepository: HealthSettingsRepository,
    private val permissionRepository: HealthPermissionRepository,
    private val flowHealthRepository: FlowHealthRepository,
    private val healthRefreshUseCase: HealthRefreshUseCase
) : ViewModel() {
    private val permissionGranted = MutableStateFlow(false)
    private val available = MutableStateFlow(false)
    private val pending = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(HealthSettingsUiState())
    val uiState: StateFlow<HealthSettingsUiState> = _uiState.asStateFlow()

    val readStepsPermission: String = permissionRepository.readStepsPermission

    init {
        viewModelScope.launch {
            combine(settingsRepository.settings, permissionGranted, available, pending) { settings, granted, isAvailable, hasPending ->
                HealthSettingsUiState(
                    healthConnectAvailable = isAvailable,
                    readStepsPermissionGranted = granted,
                    movementBonusEnabled = settings.movementBonusEnabled && granted && isAvailable,
                    localMovementBonusEnabled = settings.movementBonusEnabled,
                    pendingRefreshableFlows = hasPending,
                    showDisableWarning = _uiState.value.showDisableWarning,
                    isBusy = _uiState.value.isBusy
                )
            }.collect { _uiState.value = it }
        }
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            available.value = permissionRepository.isHealthConnectAvailable()
            permissionGranted.value = permissionRepository.isReadStepsGranted()
            pending.value = flowHealthRepository.hasPendingRefreshableSnapshots()
            if (available.value && permissionGranted.value) {
                healthRefreshUseCase.refreshForeground()
            }
        }
    }

    fun onPermissionResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            val granted = readStepsPermission in grantedPermissions || permissionRepository.isReadStepsGranted()
            permissionGranted.value = granted
            if (granted && available.value) {
                settingsRepository.setMovementBonusEnabled(true)
                healthRefreshUseCase.refreshForeground()
            }
        }
    }

    fun requestDisableOrDisableNow() {
        viewModelScope.launch {
            val hasPending = flowHealthRepository.hasPendingRefreshableSnapshots()
            pending.value = hasPending
            if (hasPending) {
                _uiState.update { it.copy(showDisableWarning = true) }
            } else {
                settingsRepository.setMovementBonusEnabled(false)
            }
        }
    }

    fun keepHealthOn() {
        _uiState.update { it.copy(showDisableWarning = false) }
    }

    fun disableAnyway() {
        viewModelScope.launch {
            flowHealthRepository.markRefreshableDisabled(System.currentTimeMillis())
            settingsRepository.setMovementBonusEnabled(false)
            pending.value = false
            _uiState.update { it.copy(showDisableWarning = false) }
        }
    }
}
