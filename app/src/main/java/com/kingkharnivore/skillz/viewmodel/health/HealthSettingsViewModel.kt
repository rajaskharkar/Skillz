package com.kingkharnivore.skillz.viewmodel.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
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
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val rawHealthConnectSdkStatus: Int? = null,
    val readStepsPermissionGranted: Boolean = false,
    val localMovementBonusEnabled: Boolean = false,
    val pendingRefreshableFlows: Boolean = false,
    val showDisableWarning: Boolean = false,
    val isBusy: Boolean = false,
    val userMessage: String? = null
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
    private val rawSdkStatus = MutableStateFlow<Int?>(null)
    private val pending = MutableStateFlow(false)
    private val userMessage = MutableStateFlow<String?>(null)
    private val isBusy = MutableStateFlow(false)

    private val _showDisableWarning = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(HealthSettingsUiState())
    val uiState: StateFlow<HealthSettingsUiState> = _uiState.asStateFlow()

    val readStepsPermission: String = permissionRepository.readStepsPermission

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                permissionGranted,
                availability,
                rawSdkStatus,
                pending,
                userMessage,
                isBusy,
                _showDisableWarning
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val settings = values[0] as com.kingkharnivore.skillz.data.repository.health.HealthSettings
                val granted = values[1] as Boolean
                val currentAvailability = values[2] as HealthConnectAvailability
                val rawStatus = values[3] as Int?
                val hasPending = values[4] as Boolean
                val message = values[5] as String?
                val busy = values[6] as Boolean
                val showWarning = values[7] as Boolean

                HealthSettingsUiState(
                    healthConnectAvailability = currentAvailability,
                    rawHealthConnectSdkStatus = rawStatus,
                    readStepsPermissionGranted = granted,
                    localMovementBonusEnabled = settings.movementBonusEnabled,
                    pendingRefreshableFlows = hasPending,
                    showDisableWarning = showWarning,
                    isBusy = busy,
                    userMessage = message
                )
            }.collect { state ->
                Log.d(TAG, "Health UI state=$state")
                _uiState.value = state
            }
        }

        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            refreshStateInternal(clearMessage = false)
        }
    }

    fun enableMovementBonusIfPermissionGranted() {
        viewModelScope.launch {
            isBusy.value = true
            try {
                refreshStateInternal(clearMessage = true)

                val currentAvailability = availability.value
                val granted = permissionGranted.value

                if (currentAvailability == HealthConnectAvailability.AVAILABLE && granted) {
                    settingsRepository.setMovementBonusEnabled(true)
                    userMessage.value = null
                    healthRefreshUseCase.refreshForeground()
                } else {
                    settingsRepository.setMovementBonusEnabled(false)
                    userMessage.value = when (currentAvailability) {
                        HealthConnectAvailability.AVAILABLE ->
                            "Health permission was not granted."

                        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
                            "Health Connect needs to be installed or updated."

                        HealthConnectAvailability.UNAVAILABLE ->
                            "Health Connect is not available on this device."
                    }
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun onPermissionLaunchAttempt(packageName: String) {
        Log.d(
            TAG,
            "Connect Health clicked. package=$packageName availability=${availability.value} rawSdkStatus=${rawSdkStatus.value} permission=$readStepsPermission granted=${permissionGranted.value}"
        )
        userMessage.value = null
    }

    fun onPermissionLaunchFailed(t: Throwable) {
        Log.w(TAG, "Could not open Health Connect permissions", t)
        userMessage.value = "Could not open Health Connect permissions."
    }

    fun onPermissionResult(grantedPermissions: Set<String>) {
        Log.d(TAG, "Health permission result=$grantedPermissions")

        viewModelScope.launch {
            isBusy.value = true
            try {
                val currentAvailability = permissionRepository.availability()
                val rawStatus = permissionRepository.rawSdkStatus()
                val alreadyGranted = permissionRepository.isReadStepsGranted()
                val grantedFromResult = readStepsPermission in grantedPermissions
                val granted = grantedFromResult || alreadyGranted

                Log.d(
                    TAG,
                    "Permission result processed. " +
                            "readStepsPermission=$readStepsPermission " +
                            "grantedFromResult=$grantedFromResult " +
                            "alreadyGranted=$alreadyGranted " +
                            "finalGranted=$granted " +
                            "availability=$currentAvailability " +
                            "rawSdkStatus=$rawStatus"
                )

                availability.value = currentAvailability
                rawSdkStatus.value = rawStatus
                permissionGranted.value = granted
                pending.value = flowHealthRepository.hasPendingRefreshableSnapshots()

                if (granted && currentAvailability == HealthConnectAvailability.AVAILABLE) {
                    settingsRepository.setMovementBonusEnabled(true)
                    userMessage.value = null
                    healthRefreshUseCase.refreshForeground()
                } else {
                    settingsRepository.setMovementBonusEnabled(false)
                    userMessage.value = "Health permission was not granted."
                }
            } finally {
                isBusy.value = false
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
            Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
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
                userMessage.value = "Could not open Health Connect in Play Store."
            }
        } catch (marketError: Throwable) {
            Log.w(TAG, "Could not open Health Connect in Play Store", marketError)
            userMessage.value = "Could not open Health Connect in Play Store."
        }
    }

    fun requestDisableOrDisableNow() {
        viewModelScope.launch {
            val hasPending = flowHealthRepository.hasPendingRefreshableSnapshots()
            pending.value = hasPending

            if (hasPending) {
                _showDisableWarning.value = true
            } else {
                settingsRepository.setMovementBonusEnabled(false)
                userMessage.value = null
            }
        }
    }

    fun keepHealthOn() {
        _showDisableWarning.value = false
    }

    fun disableAnyway() {
        viewModelScope.launch {
            flowHealthRepository.markRefreshableDisabled(System.currentTimeMillis())
            settingsRepository.setMovementBonusEnabled(false)
            pending.value = false
            userMessage.value = null
            _showDisableWarning.value = false
        }
    }

    private suspend fun refreshStateInternal(clearMessage: Boolean) {
        val currentAvailability = permissionRepository.availability()
        val rawStatus = permissionRepository.rawSdkStatus()
        val granted = permissionRepository.isReadStepsGranted()
        val hasPending = flowHealthRepository.hasPendingRefreshableSnapshots()

        Log.d(
            TAG,
            "refreshState availability=$currentAvailability rawSdkStatus=$rawStatus READ_STEPS granted=$granted permission=$readStepsPermission"
        )

        availability.value = currentAvailability
        rawSdkStatus.value = rawStatus
        permissionGranted.value = granted
        pending.value = hasPending

        if (clearMessage) {
            userMessage.value = null
        }

        if (currentAvailability == HealthConnectAvailability.AVAILABLE && granted) {
            runCatching {
                healthRefreshUseCase.refreshForeground()
            }.onFailure {
                Log.w(TAG, "Foreground health refresh failed", it)
            }
        }
    }

    private companion object {
        const val TAG = "HealthSettings"
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
}