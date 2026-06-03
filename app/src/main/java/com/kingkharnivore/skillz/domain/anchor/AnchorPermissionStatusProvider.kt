package com.kingkharnivore.skillz.domain.anchor

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnchorPermissionStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isGuardAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context.packageName, "com.kingkharnivore.skillz.ui.service.AnchorGuardAccessibilityService").flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
