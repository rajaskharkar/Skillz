package com.kingkharnivore.skillz.utils.arc

object ArcRules {
    const val GRACE_WINDOW_MS = 5 * 60_000L
    const val PROGRESS_STEP_MS = 10 * 60_000L
    const val STEP = 0.1
    const val START_MULTIPLIER = 1.3

    const val FRESH_PAUSE_WINDOW_MS = 3 * 60 * 60_000L
    const val LONG_PAUSE_THRESHOLD_MS = 24 * 60 * 60_000L
    const val EXTENDED_PAUSE_THRESHOLD_MS = 72 * 60 * 60_000L
    const val PAUSE_BUDGET_DEFAULT_MS = 5 * 60_000L
    const val PAUSE_BUDGET_LONG_MS = 30 * 60_000L
    const val PAUSE_BUDGET_EXTENDED_MS = 60 * 60_000L
}
