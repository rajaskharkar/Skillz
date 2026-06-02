package com.kingkharnivore.skillz.domain.anchor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val neverAnchorPolicy: NeverAnchorPolicy
) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getRecentlyUsedApps(windowMs: Long, maxCount: Int): List<RecentApp> {
        if (!hasUsageAccess()) return emptyList()
        val now = System.currentTimeMillis()
        val start = (now - windowMs).coerceAtLeast(0L)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(start, now)
        val event = UsageEvents.Event()
        val lastUsed = linkedMapOf<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForeground = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (isForeground && !neverAnchorPolicy.isNeverAnchored(event.packageName)) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }
        return lastUsed.entries
            .sortedByDescending { it.value }
            .take(maxCount)
            .map { (pkg, at) -> RecentApp(pkg, displayName(pkg), at) }
    }

    fun getCurrentForegroundPackage(): String? {
        if (!hasUsageAccess()) return null
        val now = System.currentTimeMillis()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(now - 5 * 60_000L, now)
        val event = UsageEvents.Event()
        var current: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> current = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (current == event.packageName) current = null
                }
            }
        }
        return current
    }

    fun displayName(packageName: String): String {
        return runCatching {
            @Suppress("DEPRECATION")
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }
}
