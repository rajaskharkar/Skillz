package com.kingkharnivore.skillz.viewmodel.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kingkharnivore.skillz.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import com.kingkharnivore.skillz.utils.health.HealthRefreshUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HealthUserMessage {
    data object HealthPermissionNotGranted : HealthUserMessage
    data object CouldNotOpenHealthConnectPermissions : HealthUserMessage
    data object CouldNotOpenHealthConnectInPlayStore : HealthUserMessage
}

data class HealthSettingsUiState(
    val healthConnectAvailability: HealthConnectAvailability =
        HealthConnectAvailability.UNAVAILABLE,
    val readStepsPermissionGranted: Boolean = false,
    val localMovementBonusEnabled: Boolean = false,
    val pendingRefreshableFlows: Boolean = false,
    val showDisableWarning: Boolean = false,
    val isBusy: Boolean = false,
    val userMessage: HealthUserMessage? = null
) {
    val healthConnectAvailable: Boolean
        get() = healthConnectAvailability == HealthConnectAvailability.AVAILABLE

    val providerUpdateRequired: Boolean
        get() = healthConnectAvailability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED

    val movementBonusEnabled: Boolean
        get() = healthConnectAvailable && readStepsPermissionGranted && localMovementBonusEnabled

    val toggleChecked: Boolean
        get() = movementBonusEnabled
}

@HiltViewModel
class HealthSettingsViewModel @Inject constructor(
    private val settingsRepository: HealthSettingsRepository,
    private val permissionRepository: HealthPermissionRepository,
    private val flowHealthRepository: FlowHealthRepository,
    private val healthRefreshUseCase: HealthRefreshUseCase
) : ViewModel() {
    private val permissionGranted = MutableStateFlow(false)
    private val availability = MutableStateFlow(HealthConnectAvailability.UNAVAILABLE)
    private val pending = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<HealthUserMessage?>(null)
    private val _uiState = MutableStateFlow(HealthSettingsUiState())
    val uiState: StateFlow<HealthSettingsUiState> = _uiState.asStateFlow()

    val readStepsPermission: String = permissionRepository.readStepsPermission

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                permissionGranted,
                availability,
                pending,
                userMessage
            ) { settings, granted, currentAvailability, hasPending, message ->
                HealthSettingsUiState(
                    healthConnectAvailability = currentAvailability,
                    readStepsPermissionGranted = granted,
                    localMovementBonusEnabled = settings.movementBonusEnabled,
                    pendingRefreshableFlows = hasPending,
                    showDisableWarning = _uiState.value.showDisableWarning,
                    isBusy = _uiState.value.isBusy,
                    userMessage = message
                )
            }.collect { _uiState.value = it }
        }
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            val currentAvailability = permissionRepository.availability()
            val granted = permissionRepository.isReadStepsGranted()
            logDebug("HealthConnect availability=$currentAvailability")
            logDebug("READ_STEPS granted=$granted")
            availability.value = currentAvailability
            permissionGranted.value = granted
            pending.value = flowHealthRepository.hasPendingRefreshableSnapshots()
            if (currentAvailability == HealthConnectAvailability.AVAILABLE && granted) {
                healthRefreshUseCase.refreshForeground()
            }
        }
    }

    fun enableMovementBonusIfPermissionGranted() {
        viewModelScope.launch {
            val currentAvailability = permissionRepository.availability()
            val granted = permissionRepository.isReadStepsGranted()
            availability.value = currentAvailability
            permissionGranted.value = granted
            if (currentAvailability == HealthConnectAvailability.AVAILABLE && granted) {
                settingsRepository.setMovementBonusEnabled(true)
                userMessage.value = null
                healthRefreshUseCase.refreshForeground()
            } else {
                userMessage.value = if (currentAvailability
                    == HealthConnectAvailability.AVAILABLE) {
                    HealthUserMessage.HealthPermissionNotGranted
                } else {
                    null
                }
            }
        }
    }

    fun onPermissionLaunchFailed(t: Throwable) {
        Log.w(TAG, "Could not open Health Connect permissions", t)
        userMessage.value = HealthUserMessage.CouldNotOpenHealthConnectPermissions
    }

    fun onPermissionResult(grantedPermissions: Set<String>) {
        logDebug("Health permission result=$grantedPermissions")
        viewModelScope.launch {
            val currentAvailability = permissionRepository.availability()
            val granted = readStepsPermission in grantedPermissions
                    || permissionRepository.isReadStepsGranted()
            availability.value = currentAvailability
            permissionGranted.value = granted
            if (granted && currentAvailability == HealthConnectAvailability.AVAILABLE) {
                settingsRepository.setMovementBonusEnabled(true)
                userMessage.value = null
                healthRefreshUseCase.refreshForeground()
            } else {
                userMessage.value = HealthUserMessage.HealthPermissionNotGranted
            }
        }
    }

    fun openHealthConnectInstallOrUpdate(context: Context) {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE"
            )
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(marketIntent)
            userMessage.value = null
        } catch (marketError: ActivityNotFoundException) {
            try {
                context.startActivity(webIntent)
                userMessage.value = null
            } catch (webError: Throwable) {
                Log.w(TAG, "Could not open Health Connect in Play Store", webError)
                userMessage.value = HealthUserMessage.CouldNotOpenHealthConnectInPlayStore
            }
        } catch (marketError: Throwable) {
            Log.w(TAG, "Could not open Health Connect in Play Store", marketError)
            userMessage.value = HealthUserMessage.CouldNotOpenHealthConnectInPlayStore
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
                userMessage.value = null
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
            userMessage.value = null
            _uiState.update { it.copy(showDisableWarning = false) }
        }
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private companion object {
        const val TAG = "HealthSettings"
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
}
