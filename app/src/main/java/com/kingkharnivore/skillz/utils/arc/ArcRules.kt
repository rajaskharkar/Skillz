package com.kingkharnivore.skillz.utils.arc

object ArcRules {
    const val GRACE_WINDOW_MS = 5 * 60_000L
    const val PROGRESS_STEP_MS = 10 * 60_000L
    const val STEP = 0.1
    const val START_MULTIPLIER = 1.3

    const val PAUSE_BUDGET_EARLY_MS = 2 * 60_000L   // until flow 3
    const val PAUSE_BUDGET_LATE_MS  = 5 * 60_000L   // flow 3+
    const val PAUSE_BUDGET_ULTRA_MS = 10 * 60_000L // flow 10+
}