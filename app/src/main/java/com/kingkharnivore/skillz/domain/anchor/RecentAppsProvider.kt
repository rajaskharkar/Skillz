package com.kingkharnivore.skillz.domain.anchor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
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
        val fromEvents = getRecentlyDetectedAppsFromEvents(start, now, maxCount)
        if (fromEvents.isNotEmpty()) return fromEvents
        return getRecentlyDetectedAppsFromStats(start, now, maxCount)
    }

    private fun getRecentlyDetectedAppsFromEvents(start: Long, now: Long, maxCount: Int): List<RecentApp> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(start, now)
        val event = UsageEvents.Event()
        val lastForegroundByPackage = linkedMapOf<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastForegroundByPackage[event.packageName] = event.timeStamp
            }
        }
        return lastForegroundByPackage.entries
            .asSequence()
            .filter { (pkg, _) -> !neverAnchorPolicy.isNeverAnchored(pkg) }
            .sortedByDescending { it.value }
            .take(maxCount)
            .map { (pkg, lastUsedAt) -> RecentApp(pkg, displayName(pkg), lastUsedAt) }
            .toList()
    }

    private fun getRecentlyDetectedAppsFromStats(start: Long, now: Long, maxCount: Int): List<RecentApp> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(INTERVAL_BEST, start, now) ?: emptyList()
        return stats
            .asSequence()
            .filter { it.lastTimeUsed > 0L }
            .filterNot { neverAnchorPolicy.isNeverAnchored(it.packageName) }
            .sortedByDescending { it.lastTimeUsed }
            .distinctBy { it.packageName }
            .take(maxCount)
            .map { RecentApp(it.packageName, displayName(it.packageName), it.lastTimeUsed) }
            .toList()
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
        return curatedDisplayName(packageName) ?: runCatching {
            @Suppress("DEPRECATION")
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    fun curatedDisplayName(packageName: String): String? = CuratedAnchorAppCatalog.apps
        .firstOrNull { it.packageName == packageName }
        ?.displayName
}

data class CuratedAnchorApp(
    val displayName: String,
    val packageName: String
)

object CuratedAnchorAppCatalog {
    val apps = listOf(
        CuratedAnchorApp("Instagram", "com.instagram.android"),
        CuratedAnchorApp("WhatsApp", "com.whatsapp"),
        CuratedAnchorApp("WhatsApp Business", "com.whatsapp.w4b"),
        CuratedAnchorApp("Reddit", "com.reddit.frontpage"),
        CuratedAnchorApp("YouTube", "com.google.android.youtube"),
        CuratedAnchorApp("TikTok", "com.zhiliaoapp.musically"),
        CuratedAnchorApp("Snapchat", "com.snapchat.android"),
        CuratedAnchorApp("Discord", "com.discord"),
        CuratedAnchorApp("Facebook", "com.facebook.katana"),
        CuratedAnchorApp("Messenger", "com.facebook.orca"),
        CuratedAnchorApp("X", "com.twitter.android"),
        CuratedAnchorApp("Chrome", "com.android.chrome"),
        CuratedAnchorApp("Netflix", "com.netflix.mediaclient"),
        CuratedAnchorApp("Prime Video", "com.amazon.avod.thirdpartyclient")
    )
}
