package com.kingkharnivore.skillz.viewmodel.anchor

import android.app.Application
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.domain.anchor.PhoneDownMode
import com.kingkharnivore.skillz.domain.anchor.RecentAppsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnchorSettingsUiState(
    val anchorEnabled: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
    val anchoredAppCount: Int = 0,
    val phoneDownMode: PhoneDownMode = PhoneDownMode.OFF
)

@HiltViewModel
class AnchorSettingsViewModel @Inject constructor(
    application: Application,
    private val anchorRepository: AnchorRepository,
    private val recentAppsProvider: RecentAppsProvider
) : AndroidViewModel(application) {
    val uiState = combine(anchorRepository.settings, anchorRepository.anchoredApps) { settings, apps ->
        AnchorSettingsUiState(
            anchorEnabled = settings.enabled,
            usageAccessGranted = recentAppsProvider.hasUsageAccess(),
            notificationPermissionGranted = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled(),
            anchoredAppCount = apps.size,
            phoneDownMode = settings.phoneDownMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnchorSettingsUiState())

    fun setAnchorEnabled(enabled: Boolean) {
        viewModelScope.launch { anchorRepository.setEnabled(enabled) }
    }
}
