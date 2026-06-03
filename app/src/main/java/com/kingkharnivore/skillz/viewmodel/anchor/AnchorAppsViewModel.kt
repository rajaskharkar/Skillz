package com.kingkharnivore.skillz.viewmodel.anchor

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.domain.anchor.AnchorableApp
import com.kingkharnivore.skillz.domain.anchor.CuratedAnchorAppCatalog
import com.kingkharnivore.skillz.domain.anchor.RecentAppsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnchoredAppUiModel(val packageName: String, val displayName: String, val isInstalled: Boolean = true)
data class AnchorableAppUiModel(
    val packageName: String,
    val displayName: String,
    val label: String,
    val available: Boolean = true,
    val alreadyAnchored: Boolean = false
)

data class AnchorAppsUiState(
    val usageAccessGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = true,
    val anchoredApps: List<AnchoredAppUiModel> = emptyList(),
    val recentlyUsedApps: List<AnchorableAppUiModel> = emptyList(),
    val suggestedApps: List<AnchorableAppUiModel> = emptyList(),
    val expandedTo30Days: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val debugDiagnostics: List<String> = emptyList()
)

sealed interface AnchorAppsAction {
    data object RequestUsageAccess : AnchorAppsAction
    data object RefreshRecentlyUsedApps : AnchorAppsAction
    data object ExpandTo30Days : AnchorAppsAction
    data class AnchorApp(val packageName: String) : AnchorAppsAction
    data class RemoveApp(val packageName: String) : AnchorAppsAction
}

@HiltViewModel
class AnchorAppsViewModel @Inject constructor(
    application: Application,
    private val anchorRepository: AnchorRepository,
    private val recentAppsProvider: RecentAppsProvider
) : AndroidViewModel(application) {
    private val expanded = MutableStateFlow(false)
    private val loading = MutableStateFlow(false)
    private val recentApps = MutableStateFlow<List<AnchorableAppUiModel>>(emptyList())
    private val error = MutableStateFlow<String?>(null)

    val uiState = combine(
        anchorRepository.anchoredApps,
        expanded,
        loading,
        recentApps,
        error
    ) { anchored, isExpanded, isLoading, recent, err ->
        val anchoredPackages = anchored.mapTo(mutableSetOf()) { it.packageName }
        AnchorAppsUiState(
            usageAccessGranted = recentAppsProvider.hasUsageAccess(),
            notificationPermissionGranted = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled(),
            anchoredApps = anchored.map { AnchoredAppUiModel(it.packageName, if (isInstalled(it.packageName)) it.displayName else "App not found", isInstalled(it.packageName)) },
            recentlyUsedApps = recent.filterNot { it.packageName in anchoredPackages },
            suggestedApps = commonDistractionApps(anchoredPackages),
            expandedTo30Days = isExpanded,
            isLoading = isLoading,
            errorMessage = err,
            debugDiagnostics = debugDiagnostics(recent, anchoredPackages)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnchorAppsUiState())

    init { refresh() }

    fun onAction(action: AnchorAppsAction) {
        when (action) {
            AnchorAppsAction.RefreshRecentlyUsedApps -> refresh()
            AnchorAppsAction.ExpandTo30Days -> { expanded.value = true; refresh() }
            is AnchorAppsAction.AnchorApp -> add(action.packageName)
            is AnchorAppsAction.RemoveApp -> viewModelScope.launch { anchorRepository.removeAnchoredApp(action.packageName) }
            AnchorAppsAction.RequestUsageAccess -> Unit
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            val days = if (expanded.value) 30L else 14L
            val max = if (expanded.value) 100 else 40
            recentApps.value = recentAppsProvider.getRecentlyUsedApps(days * 24 * 60 * 60 * 1000L, max)
                .map { AnchorableAppUiModel(it.packageName, it.displayName, "Detected recently") }
            error.value = null
            loading.value = false
        }
    }

    private fun add(packageName: String) {
        viewModelScope.launch {
            val app = recentApps.value.firstOrNull { it.packageName == packageName }
                ?: commonApp(packageName)
                ?: AnchorableAppUiModel(packageName, recentAppsProvider.displayName(packageName), "Common distraction")
            anchorRepository.addAnchoredApp(AnchorableApp(app.packageName, app.displayName))
        }
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        getApplication<Application>().packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    private fun commonDistractionApps(
        anchoredPackages: Set<String>
    ): List<AnchorableAppUiModel> = CuratedAnchorAppCatalog.apps.map { app ->
        AnchorableAppUiModel(
            packageName = app.packageName,
            displayName = recentAppsProvider.displayName(app.packageName).ifBlank { app.displayName },
            label = "Common distraction",
            available = true,
            alreadyAnchored = app.packageName in anchoredPackages
        )
    }

    private fun commonApp(packageName: String): AnchorableAppUiModel? = CuratedAnchorAppCatalog.apps
        .firstOrNull { it.packageName == packageName }
        ?.let { AnchorableAppUiModel(it.packageName, it.displayName, "Common distraction", available = true) }

    private fun debugDiagnostics(recent: List<AnchorableAppUiModel>, anchoredPackages: Set<String>): List<String> {
        if (!BuildConfig.DEBUG) return emptyList()
        return listOf(
            "Usage Access: ${recentAppsProvider.hasUsageAccess()}",
            "Detected packages: ${recent.joinToString { it.packageName }.ifBlank { "none" }}",
            "Last foreground package: ${recentAppsProvider.getCurrentForegroundPackage() ?: "unknown"}",
            "Selected anchored packages: ${anchoredPackages.joinToString().ifBlank { "none" }}"
        )
    }

}
