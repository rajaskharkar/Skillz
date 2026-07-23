package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.CollectionProgress
import java.util.UUID

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

internal fun validateBlueSpeciesFocus(
    speciesId: String?,
    catalogSpeciesExists: Boolean,
    renderedSpeciesIds: Set<String>
): NavigationConsumptionResult = when {
    speciesId == null -> NavigationConsumptionResult.Consumed
    !catalogSpeciesExists -> NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND)
    speciesId !in renderedSpeciesIds -> NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE)
    else -> NavigationConsumptionResult.Consumed
}

internal fun validateBeyondBlueFocus(
    speciesExists: Boolean,
    isBeyondBlueSpecies: Boolean,
    belongsToCollection: Boolean
): NavigationConsumptionResult = when {
    !speciesExists -> NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND)
    !isBeyondBlueSpecies || !belongsToCollection ->
        NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE)
    else -> NavigationConsumptionResult.Consumed
}

internal fun validateCollectionSpeciesFocus(
    collection: CollectionProgress?,
    speciesId: String?
): NavigationConsumptionResult = when {
    collection == null -> NavigationConsumptionResult.Failed(NavigationFailureReason.COLLECTION_NOT_FOUND)
    speciesId == null -> NavigationConsumptionResult.Consumed
    collection.speciesStates.none { it.speciesId == speciesId } ->
        NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND)
    collection.speciesStates.any { it.speciesId == speciesId && it.secret && !it.discovered } ->
        NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE)
    else -> NavigationConsumptionResult.Consumed
}

sealed interface PendingShellNavigation {
    val notificationId: String?
    val requestId: String

    data class OpenBadge(val badgeId: String, override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()) : PendingShellNavigation
    data class OpenCollection(val collectionId: String, override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()) : PendingShellNavigation
    data class OpenChestSpecies(val speciesId: String, override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()) : PendingShellNavigation
    data class OpenBlueSpecies(
        val collectionId: String,
        val speciesId: String?,
        override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()
    ) : PendingShellNavigation
    data class OpenStillwaterSpecies(
        val collectionId: String,
        val speciesId: String?,
        override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()
    ) : PendingShellNavigation
    data class OpenBeyondBlue(
        val collectionId: String,
        val speciesId: String,
        override val notificationId: String? = null,
        override val requestId: String = UUID.randomUUID().toString()
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
