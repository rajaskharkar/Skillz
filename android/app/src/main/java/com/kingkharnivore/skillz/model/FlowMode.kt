package com.kingkharnivore.skillz.model

import androidx.room.TypeConverter

/** Stable integer encoding preserves the released Flow/Soft Room columns without rebuilding rows. */
enum class FlowMode(val storageId: Int, val timeMultiplier: Double) {
    FLOW(0, 1.0), SOFT(1, 0.0), POWER(2, 1.5);
    companion object { fun fromSoft(soft: Boolean) = if (soft) SOFT else FLOW }
}

class FlowModeConverter {
    @TypeConverter fun encode(mode: FlowMode): Int = mode.storageId
    @TypeConverter fun decode(value: Int): FlowMode = FlowMode.entries.first { it.storageId == value }
}
