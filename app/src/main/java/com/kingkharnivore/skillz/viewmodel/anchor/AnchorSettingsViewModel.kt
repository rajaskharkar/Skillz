package com.kingkharnivore.skillz.viewmodel.anchor

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.domain.anchor.AnchorMode
import com.kingkharnivore.skillz.domain.anchor.AnchorPermissionStatusProvider
import com.kingkharnivore.skillz.domain.anchor.PhoneDownMode
import com.kingkharnivore.skillz.domain.anchor.RecentAppsProvider
import com.kingkharnivore.skillz.domain.anchor.hasMeaningfulActiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnchorSettingsUiState(
    val anchorEnabled: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
    val guardAccessibilityGranted: Boolean = false,
    val installedAppCount: Int = 0,
    val anchoredAppCount: Int = 0,
    val phoneDownMode: PhoneDownMode = PhoneDownMode.OFF,
    val mode: AnchorMode = AnchorMode.GUIDE,
    val guardAvailable: Boolean = false,
    val activeDuringCurrentFlow: Boolean = false
)

@HiltViewModel
class AnchorSettingsViewModel @Inject constructor(
    application: Application,
    private val anchorRepository: AnchorRepository,
    private val recentAppsProvider: RecentAppsProvider,
    private val permissionStatusProvider: AnchorPermissionStatusProvider,
    private val aliveFlowRepository: AliveFlowRepository
) : AndroidViewModel(application) {
    private val refreshTick = MutableStateFlow(0)

    val uiState = combine(anchorRepository.settings, anchorRepository.anchoredApps, aliveFlowRepository.getOngoingSession(), refreshTick) { settings, apps, ongoing, _ ->
        AnchorSettingsUiState(
            anchorEnabled = settings.enabled,
            usageAccessGranted = recentAppsProvider.hasUsageAccess(),
            notificationPermissionGranted = NotificationManagerCompat.from(getApplication<Application>()).areNotificationsEnabled(),
            guardAccessibilityGranted = permissionStatusProvider.isGuardAccessibilityEnabled(),
            installedAppCount = recentAppsProvider.getInstalledLaunchableApps().size,
            anchoredAppCount = apps.size,
            phoneDownMode = settings.phoneDownMode,
            mode = settings.mode,
            guardAvailable = permissionStatusProvider.isGuardAccessibilityEnabled(),
            activeDuringCurrentFlow = hasMeaningfulActiveFlow(ongoing, System.currentTimeMillis()) && (ongoing?.anchorEnabledForFlow == true || settings.enabled)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnchorSettingsUiState())

    fun refresh() {
        refreshTick.value += 1
    }

    fun setAnchorEnabled(enabled: Boolean) {
        viewModelScope.launch { anchorRepository.setEnabled(enabled) }
    }

    fun setMode(mode: AnchorMode) {
        viewModelScope.launch { anchorRepository.setMode(mode) }
    }
}
