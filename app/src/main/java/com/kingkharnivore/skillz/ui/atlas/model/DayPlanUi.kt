package com.kingkharnivore.skillz.ui.atlas.model

sealed class DaySegmentUi {

    data class Gap(
        val gapMinutes: Int,        // real minutes between beams
        val displayMinutes: Int     // compressed minutes to display
    ) : DaySegmentUi()

    data class Beam(
        val block: BeamBlockUi,
        val realMinutes: Int,       // true duration in minutes
        val displayMinutes: Int     // duration mapped to display scale (usually == realMinutes)
    ) : DaySegmentUi()
}

data class DayPlanUi(
    val dayStartMs: Long,
    val dayEndMs: Long,
    val segments: List<DaySegmentUi> = emptyList(),
    val anchors: List<DayAnchorUi> = emptyList(),      // for grid mapping
    val totalDisplayMinutes: Int = 24 * 60,
    val beamsCount: Int = 0
)

data class DayAnchorUi(
    val minuteOfDay: Int,   // 0..1440
    val displayMinute: Int  // 0..totalDisplayMinutes
)