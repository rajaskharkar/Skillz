package com.kingkharnivore.skillz.ui.navigation

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.domain.anchor.hasMeaningfulActiveFlow

internal fun shouldShowStoryActiveFlowHero(
    ongoingSession: OngoingSessionEntity?,
    now: Long = System.currentTimeMillis()
): Boolean = hasMeaningfulActiveFlow(ongoingSession, now)
