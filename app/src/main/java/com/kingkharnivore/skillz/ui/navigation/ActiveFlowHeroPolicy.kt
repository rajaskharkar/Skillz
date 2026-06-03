package com.kingkharnivore.skillz.ui.navigation

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity

internal fun shouldShowStoryActiveFlowHero(ongoingSession: OngoingSessionEntity?): Boolean =
    ongoingSession != null
