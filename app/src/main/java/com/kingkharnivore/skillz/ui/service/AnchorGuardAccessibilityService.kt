package com.kingkharnivore.skillz.ui.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationManagerCompat
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.repository.AliveFlowRepository
import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.domain.anchor.AnchorMode
import com.kingkharnivore.skillz.domain.anchor.AnchorPermissionStatusProvider
import com.kingkharnivore.skillz.domain.anchor.AnchorRuntimeInput
import com.kingkharnivore.skillz.domain.anchor.AnchorRuntimePolicy
import com.kingkharnivore.skillz.domain.anchor.NeverAnchorPolicy
import com.kingkharnivore.skillz.ui.notification.AliveFlowNotificationFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnchorGuardAccessibilityService : AccessibilityService() {
    @Inject lateinit var aliveFlowRepository: AliveFlowRepository
    @Inject lateinit var anchorRepository: AnchorRepository
    @Inject lateinit var neverAnchorPolicy: NeverAnchorPolicy
    @Inject lateinit var permissionStatusProvider: AnchorPermissionStatusProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentEpisodePackage: String? = null
    private var lastGuardActionAtMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        serviceScope.launch {
            evaluateGuard(packageName)
        }
    }

    private suspend fun evaluateGuard(packageName: String) {
        val now = System.currentTimeMillis()
        val entity = aliveFlowRepository.getOngoingSessionNow()
        val settings = anchorRepository.settings.first()
        val anchored = anchorRepository.getAnchoredPackageSet()
        val input = AnchorRuntimeInput(
            entity = entity,
            now = now,
            mode = settings.mode,
            globallyEnabled = settings.enabled,
            selectedPackageCount = anchored.size,
            accessibilityEnabled = permissionStatusProvider.isGuardAccessibilityEnabled()
        )
        val eligible = AnchorRuntimePolicy.shouldRunGuard(input)
        val selected = packageName in anchored
        val safe = !neverAnchorPolicy.isNeverAnchored(packageName)
        if (BuildConfig.DEBUG) {
            Log.d(
                "AnchorGuard",
                "pkg=$packageName eligible=$eligible selected=$selected safe=$safe count=${anchored.size}"
            )
        }

        if (!eligible || !selected || !safe) {
            if (!selected || !safe || packageName == applicationContext.packageName) currentEpisodePackage = null
            return
        }

        if (currentEpisodePackage == packageName && now - lastGuardActionAtMs < GUARD_COOLDOWN_MS) {
            if (BuildConfig.DEBUG) Log.d("AnchorGuard", "cooldown pkg=$packageName")
            return
        }

        currentEpisodePackage = packageName
        lastGuardActionAtMs = now
        aliveFlowRepository.markAnchorNudge()
        performGlobalAction(GLOBAL_ACTION_BACK)
        if (BuildConfig.DEBUG) Log.d("AnchorGuard", "performed BACK for $packageName")
        delay(650L)
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (BuildConfig.DEBUG) Log.d("AnchorGuard", "performed HOME fallback for $packageName")
        publishGuardReturnNotification()
    }

    @SuppressLint("MissingPermission")
    private suspend fun publishGuardReturnNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val entity = aliveFlowRepository.getOngoingSessionNow() ?: return
        NotificationManagerCompat.from(this).notify(
            AliveFlowNotificationFactory.ANCHOR_GUARD_RETURN_NOTIFICATION_ID,
            AliveFlowNotificationFactory.buildAnchorGuardReturnNotification(this, entity)
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val GUARD_COOLDOWN_MS = 3_000L
    }
}
