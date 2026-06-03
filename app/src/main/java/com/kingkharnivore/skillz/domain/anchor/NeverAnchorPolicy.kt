package com.kingkharnivore.skillz.domain.anchor

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Telephony
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeverAnchorPolicy @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val staticNeverAnchorPackages = setOf(
        "android",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.emergency",
        "com.google.android.apps.safetycenter",
        "com.android.server.telecom",
        "com.android.providers.telephony",
        "com.android.settings",
        "com.google.android.settings",
        "com.android.systemui",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.providers.settings",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage"
    )

    fun isNeverAnchored(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == context.packageName) return true
        if (packageName in staticNeverAnchorPackages) return true
        if (packageName == defaultDialerPackage()) return true
        if (packageName == defaultSmsPackage()) return true
        if (packageName in launcherPackages()) return true
        return isSystemCorePackage(packageName)
    }

    private fun defaultDialerPackage(): String? = runCatching {
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
    }.getOrNull()

    private fun defaultSmsPackage(): String? = runCatching {
        Telephony.Sms.getDefaultSmsPackage(context)
    }.getOrNull()

    private fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(intent, 0)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName }
    }

    private fun isSystemCorePackage(packageName: String): Boolean {
        val appInfo = runCatching {
            context.packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: return false
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val looksCore = packageName.startsWith("com.android.providers.") ||
                packageName.startsWith("com.android.permission") ||
                packageName.startsWith("com.google.android.gms") ||
                packageName.startsWith("com.google.android.gsf") ||
                packageName.startsWith("com.samsung.android.providers.")
        return isSystem && looksCore
    }
}
