package com.kingkharnivore.skillz.domain.anchor

enum class PhoneDownMode { OFF }

data class AnchorSettings(
    val enabled: Boolean = false,
    val phoneDownMode: PhoneDownMode = PhoneDownMode.OFF
)

data class AnchoredApp(
    val packageName: String,
    val displayName: String,
    val iconCacheKey: String? = null,
    val addedAt: Long,
    val lastSeenAt: Long? = null
)

data class AnchorableApp(
    val packageName: String,
    val displayName: String,
    val iconCacheKey: String? = null,
    val lastUsedAt: Long? = null
)

data class RecentApp(
    val packageName: String,
    val displayName: String,
    val lastUsedAt: Long
)

data class AnchorFlowState(
    val globallyEnabled: Boolean = false,
    val configured: Boolean = false,
    val usageAccessGranted: Boolean = false,
    val anchoredAppCount: Int = 0,
    val enabledForThisFlow: Boolean = false,
    val paused: Boolean = false,
    val inBreak: Boolean = false,
    val breakRemainingMs: Long = 0L,
    val distractionAttemptCount: Int = 0,
    val setupMessage: String? = null,
    val showReturnPanel: Boolean = false,
    val usageAccessRevoked: Boolean = false
)
