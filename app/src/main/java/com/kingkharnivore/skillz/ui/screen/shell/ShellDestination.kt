package com.kingkharnivore.skillz.ui.screen.shell

sealed class ShellDestination {
    data object Heart : ShellDestination()
    data object Focus : ShellDestination()
    data object Stillwater : ShellDestination()
    data object ShellChest : ShellDestination()
    data object Badges : ShellDestination()
    data object DiscoveryJournal : ShellDestination()
    data object Notifications : ShellDestination()
    data object VoyagePreview : ShellDestination()
    data object TheBluePreview : ShellDestination()
    data object IdeaGrovePreview : ShellDestination()
    data object LookoutPreview : ShellDestination()
}