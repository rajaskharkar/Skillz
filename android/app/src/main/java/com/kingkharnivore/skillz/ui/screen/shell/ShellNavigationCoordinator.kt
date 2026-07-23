package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination

enum class NavigationFailureReason {
    BADGE_NOT_FOUND,
    COLLECTION_NOT_FOUND,
    SPECIES_NOT_FOUND,
    DESTINATION_UNAVAILABLE,
    UNSUPPORTED_REQUEST
}

sealed interface NavigationConsumptionResult {
    data object Pending : NavigationConsumptionResult
    data object Consumed : NavigationConsumptionResult
    data class Failed(val reason: NavigationFailureReason) : NavigationConsumptionResult
}

object NotificationAcknowledgementPolicy {
    fun shouldMarkViewed(result: NavigationConsumptionResult): Boolean =
        result == NavigationConsumptionResult.Consumed
}

sealed interface PendingShellNavigation {
    val notificationId: String?

    data class OpenBadge(val badgeId: String, override val notificationId: String? = null) : PendingShellNavigation
    data class OpenCollection(val collectionId: String, override val notificationId: String? = null) : PendingShellNavigation
    data class OpenChestSpecies(val speciesId: String, override val notificationId: String? = null) : PendingShellNavigation
    data class OpenBlueSpecies(
        val collectionId: String,
        val speciesId: String?,
        override val notificationId: String? = null
    ) : PendingShellNavigation
    data class OpenStillwaterSpecies(
        val collectionId: String,
        val speciesId: String?,
        override val notificationId: String? = null
    ) : PendingShellNavigation
    data class OpenBeyondBlue(
        val collectionId: String,
        val speciesId: String,
        override val notificationId: String? = null
    ) : PendingShellNavigation
}

data class NavigationDispatch(
    val destination: ShellDestination,
    val pending: PendingShellNavigation
)

/** The only mapping from badge-domain destinations to Shell child requests. */
object ShellNavigationCoordinator {
    fun dispatch(action: BadgeActionDestination, notificationId: String? = null): NavigationDispatch? = when (action) {
        is BadgeActionDestination.BadgeDetails -> NavigationDispatch(
            ShellDestination.Badges, PendingShellNavigation.OpenBadge(action.badgeId, notificationId)
        )
        is BadgeActionDestination.CollectionDetails -> NavigationDispatch(
            ShellDestination.Badges, PendingShellNavigation.OpenCollection(action.collectionId, notificationId)
        )
        is BadgeActionDestination.ChestSpecies -> NavigationDispatch(
            ShellDestination.ShellChest, PendingShellNavigation.OpenChestSpecies(action.speciesId, notificationId)
        )
        is BadgeActionDestination.BlueRegion -> NavigationDispatch(
            ShellDestination.TheBluePreview,
            PendingShellNavigation.OpenBlueSpecies(action.collectionId, action.speciesId, notificationId)
        )
        is BadgeActionDestination.StillwaterVessel -> NavigationDispatch(
            ShellDestination.Stillwater,
            PendingShellNavigation.OpenStillwaterSpecies(action.collectionId, action.speciesId, notificationId)
        )
        is BadgeActionDestination.BeyondBlue -> NavigationDispatch(
            ShellDestination.TheBluePreview,
            PendingShellNavigation.OpenBeyondBlue(action.collectionId, action.speciesId, notificationId)
        )
        BadgeActionDestination.Flow, BadgeActionDestination.Arc, BadgeActionDestination.MovementInfo -> null
    }
}
