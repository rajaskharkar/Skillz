package com.kingkharnivore.skillz.viewmodel.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
import com.kingkharnivore.skillz.data.health.PhoneStepEstimateTracker
import com.kingkharnivore.skillz.data.repository.health.FlowHealthRepository
import com.kingkharnivore.skillz.data.repository.health.HealthPermissionRepository
import com.kingkharnivore.skillz.data.repository.health.HealthSettingsRepository
import com.kingkharnivore.skillz.domain.health.HealthRefreshUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthSettingsUiState(
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val rawHealthConnectSdkStatus: Int? = null,
    val readStepsPermissionGranted: Boolean = false,
    val phoneStepSensorAvailable: Boolean = false,
    val activityRecognitionPermissionGranted: Boolean = false,
    val activityRecognitionPermissionDenied: Boolean = false,
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

    val healthConnectSourceReady: Boolean
        get() = healthConnectAvailable && readStepsPermissionGranted

    val phoneStepSourceReady: Boolean
        get() = phoneStepSensorAvailable && activityRecognitionPermissionGranted

    val movementBonusEnabled: Boolean
        get() = localMovementBonusEnabled && (healthConnectSourceReady || phoneStepSourceReady)

    val toggleChecked: Boolean
        get() = localMovementBonusEnabled
}

sealed interface HealthSetupEvent {
    data object RequestHealthConnectPermission : HealthSetupEvent
    data object RequestActivityRecognitionPermission : HealthSetupEvent
}

@HiltViewModel
class HealthSettingsViewModel @Inject constructor(
    private val settingsRepository: HealthSettingsRepository,
    private val permissionRepository: HealthPermissionRepository,
    private val flowHealthRepository: FlowHealthRepository,
    private val healthRefreshUseCase: HealthRefreshUseCase,
    private val phoneStepEstimateTracker: PhoneStepEstimateTracker
) : ViewModel() {

    private val permissionGranted = MutableStateFlow(false)
    private val availability = MutableStateFlow(HealthConnectAvailability.UNAVAILABLE)
    private val rawSdkStatus = MutableStateFlow<Int?>(null)
    private val pending = MutableStateFlow(false)
    private val phoneStepSensorAvailable = MutableStateFlow(false)
    private val activityRecognitionPermissionGranted = MutableStateFlow(false)
    private val activityRecognitionPermissionDenied = MutableStateFlow(false)
    private val setupInProgress = MutableStateFlow(false)
    private var setupHealthPermissionRequested = false
    private var setupActivityPermissionRequested = false
    private val userMessage = MutableStateFlow<String?>(null)
    private val isBusy = MutableStateFlow(false)

    private val _showDisableWarning = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(HealthSettingsUiState())
    val uiState: StateFlow<HealthSettingsUiState> = _uiState.asStateFlow()

    private val _setupEvents = MutableSharedFlow<HealthSetupEvent>(extraBufferCapacity = 1)
    val setupEvents: SharedFlow<HealthSetupEvent> = _setupEvents.asSharedFlow()

    val readStepsPermission: String = permissionRepository.readStepsPermission

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                permissionGranted,
                availability,
                rawSdkStatus,
                pending,
                phoneStepSensorAvailable,
                activityRecognitionPermissionGranted,
                activityRecognitionPermissionDenied,
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
                val phoneAvailable = values[5] as Boolean
                val activityGranted = values[6] as Boolean
                val activityDenied = values[7] as Boolean
                val message = values[8] as String?
                val busy = values[9] as Boolean
                val showWarning = values[10] as Boolean

                HealthSettingsUiState(
                    healthConnectAvailability = currentAvailability,
                    rawHealthConnectSdkStatus = rawStatus,
                    readStepsPermissionGranted = granted,
                    phoneStepSensorAvailable = phoneAvailable,
                    activityRecognitionPermissionGranted = activityGranted,
                    activityRecognitionPermissionDenied = activityDenied,
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

    fun onHealthToggleChanged(enabled: Boolean) {
        if (enabled) {
            startMovementBonusSetup()
        } else {
            requestDisableOrDisableNow()
        }
    }

    fun startMovementBonusSetup() {
        viewModelScope.launch {
            isBusy.value = true
            setupInProgress.value = true
            setupHealthPermissionRequested = false
            setupActivityPermissionRequested = false
            userMessage.value = null
            try {
                refreshStateInternal(clearMessage = true)
                continueSetupOrFinalize()
            } finally {
                isBusy.value = false
            }
        }
    }

    fun requestHealthConnectFromSecondary() {
        viewModelScope.launch {
            userMessage.value = null
            _setupEvents.emit(HealthSetupEvent.RequestHealthConnectPermission)
        }
    }

    fun requestActivityRecognitionFromSecondary() {
        viewModelScope.launch {
            userMessage.value = null
            _setupEvents.emit(HealthSetupEvent.RequestActivityRecognitionPermission)
        }
    }

    fun onPermissionLaunchAttempt(packageName: String) {
        Log.d(
            TAG,
            "Health permission launch. package=$packageName availability=${availability.value} rawSdkStatus=${rawSdkStatus.value} permission=$readStepsPermission granted=${permissionGranted.value}"
        )
        userMessage.value = null
    }

    fun onPermissionLaunchFailed(t: Throwable) {
        Log.w(TAG, "Could not open Health Connect permissions", t)
        viewModelScope.launch {
            if (setupInProgress.value) {
                continueSetupOrFinalize()
            } else {
                userMessage.value = "Could not open Health Connect permissions."
            }
        }
    }

    fun onPermissionResult(grantedPermissions: Set<String>) {
        Log.d(TAG, "Health permission result=$grantedPermissions")
        viewModelScope.launch {
            isBusy.value = true
            try {
                refreshHealthPermissionState(grantedPermissions)
                if (setupInProgress.value) {
                    continueSetupOrFinalize()
                } else {
                    finalizeMovementBonusIfAnySourceAvailable(allowDisableIfNoSource = false)
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    fun onActivityRecognitionPermissionResult(granted: Boolean) {
        Log.d(TAG, "Activity Recognition permission result=$granted")
        viewModelScope.launch {
            isBusy.value = true
            try {
                activityRecognitionPermissionGranted.value = phoneStepEstimateTracker.hasRuntimePermission() || granted
                activityRecognitionPermissionDenied.value = !activityRecognitionPermissionGranted.value
                if (setupInProgress.value) {
                    continueSetupOrFinalize()
                } else {
                    finalizeMovementBonusIfAnySourceAvailable(allowDisableIfNoSource = false)
                }
            } finally {
                isBusy.value = false
            }
        }
    }

    private suspend fun continueSetupOrFinalize() {
        refreshStateInternal(clearMessage = false)
        when {
            availability.value == HealthConnectAvailability.AVAILABLE && !permissionGranted.value && !setupHealthPermissionRequested -> {
                setupHealthPermissionRequested = true
                _setupEvents.emit(HealthSetupEvent.RequestHealthConnectPermission)
            }
            phoneStepSensorAvailable.value && !activityRecognitionPermissionGranted.value && !setupActivityPermissionRequested -> {
                setupActivityPermissionRequested = true
                _setupEvents.emit(HealthSetupEvent.RequestActivityRecognitionPermission)
            }
            else -> finalizeMovementBonusIfAnySourceAvailable(allowDisableIfNoSource = true)
        }
    }

    private suspend fun refreshHealthPermissionState(grantedPermissions: Set<String>) {
        val currentAvailability = permissionRepository.availability()
        val rawStatus = permissionRepository.rawSdkStatus()
        val alreadyGranted = permissionRepository.isReadStepsGranted()
        val grantedFromResult = readStepsPermission in grantedPermissions
        val granted = grantedFromResult || alreadyGranted

        Log.d(
            TAG,
            "Permission result processed. readStepsPermission=$readStepsPermission grantedFromResult=$grantedFromResult alreadyGranted=$alreadyGranted finalGranted=$granted availability=$currentAvailability rawSdkStatus=$rawStatus"
        )

        availability.value = currentAvailability
        rawSdkStatus.value = rawStatus
        permissionGranted.value = granted
        pending.value = flowHealthRepository.hasPendingRefreshableSnapshots()
        refreshPhoneSourceState()
    }

    private suspend fun finalizeMovementBonusIfAnySourceAvailable(allowDisableIfNoSource: Boolean) {
        val healthReady = availability.value == HealthConnectAvailability.AVAILABLE && permissionGranted.value
        val phoneReady = phoneStepSensorAvailable.value && activityRecognitionPermissionGranted.value
        if (healthReady || phoneReady) {
            settingsRepository.setMovementBonusEnabled(true)
            setupInProgress.value = false
            setupHealthPermissionRequested = false
            setupActivityPermissionRequested = false
            userMessage.value = when {
                healthReady && phoneReady -> null
                healthReady -> "Movement Bonus is on. Phone step estimate is off."
                phoneReady -> "Movement Bonus is on. Health Connect is not connected."
                else -> null
            }
            runCatching { healthRefreshUseCase.refreshForeground(force = true) }
                .onFailure { Log.w(TAG, "Foreground health refresh failed", it) }
        } else if (allowDisableIfNoSource) {
            settingsRepository.setMovementBonusEnabled(false)
            setupInProgress.value = false
            setupHealthPermissionRequested = false
            setupActivityPermissionRequested = false
            userMessage.value = "Movement Bonus needs Health Connect or phone step access to track steps."
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
                phoneStepEstimateTracker.stopTracking()
                settingsRepository.setMovementBonusEnabled(false)
                setupInProgress.value = false
                setupHealthPermissionRequested = false
                setupActivityPermissionRequested = false
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
            setupInProgress.value = false
            setupHealthPermissionRequested = false
            setupActivityPermissionRequested = false
            phoneStepEstimateTracker.stopTracking()
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
        refreshPhoneSourceState()

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
                healthRefreshUseCase.refreshForeground(force = true)
            }.onFailure {
                Log.w(TAG, "Foreground health refresh failed", it)
            }
        }
    }

    private fun refreshPhoneSourceState() {
        val sensorAvailable = phoneStepEstimateTracker.isSensorAvailable()
        val activityGranted = phoneStepEstimateTracker.hasRuntimePermission()
        phoneStepSensorAvailable.value = sensorAvailable
        activityRecognitionPermissionGranted.value = activityGranted
        if (activityGranted) {
            activityRecognitionPermissionDenied.value = false
        }
    }

    private companion object {
        const val TAG = "HealthSettings"
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
}