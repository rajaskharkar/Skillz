package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.runtime.saveable.Saver

sealed class ShellDestination(val savedKey: String) {
    data object TheRed : ShellDestination("red")
    data object Heart : ShellDestination("heart")
    data object Focus : ShellDestination("focus")
    data object TheGreen : ShellDestination("green")
    data object ShellChest : ShellDestination("chest")
    data object Badges : ShellDestination("badges")
    data object VoyagePreview : ShellDestination("voyage")
    data object TheBluePreview : ShellDestination("blue")
    data object IdeaGrovePreview : ShellDestination("idea")
    data object LookoutPreview : ShellDestination("lookout")

    companion object {
        val StateSaver = Saver<ShellDestination, String>(
            save = { it.savedKey },
            restore = { key ->
                listOf(TheRed, Heart, Focus, TheGreen, ShellChest, Badges, VoyagePreview,
                    TheBluePreview, IdeaGrovePreview, LookoutPreview).firstOrNull { it.savedKey == key } ?: Heart
            }
        )
    }
}
