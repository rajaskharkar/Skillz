package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures

data class PresenceAccounting(
    val owned: Int,
    var representedDirect: Int = 0,
    var representedCohort: Int = 0,
    var representedHabitat: Int = 0
) {
    val representedTotal: Int get() = representedDirect + representedCohort + representedHabitat
    val remaining: Int get() = (owned - representedTotal).coerceAtLeast(0)
}
