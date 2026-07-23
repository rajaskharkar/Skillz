package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.CollectionCatalog
import com.kingkharnivore.skillz.domain.achievement.CollectionProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellNavigationCoordinatorTest {
    @Test fun exactBadgeAndCollectionRequestsRetainTheirIds() {
        val badge = ShellNavigationCoordinator.dispatch(
            BadgeActionDestination.BadgeDetails("badge-7"), "notification-1"
        )?.pending as PendingShellNavigation.OpenBadge
        assertEquals("badge-7", badge.badgeId)
        assertEquals("notification-1", badge.notificationId)
        val collection = ShellNavigationCoordinator.dispatch(
            BadgeActionDestination.CollectionDetails("blue_open_blue")
        )?.pending as PendingShellNavigation.OpenCollection
        assertEquals("blue_open_blue", collection.collectionId)
    }

    @Test fun speciesIdentitySurvivesEveryRoomDispatch() {
        val chest = ShellNavigationCoordinator.dispatch(BadgeActionDestination.ChestSpecies("minnow"))?.pending
            as PendingShellNavigation.OpenChestSpecies
        assertEquals("minnow", chest.speciesId)
        val blue = ShellNavigationCoordinator.dispatch(
            BadgeActionDestination.BlueRegion("blue_great_blue", "whale")
        )?.pending as PendingShellNavigation.OpenBlueSpecies
        assertEquals("blue_great_blue", blue.collectionId)
        assertEquals("whale", blue.speciesId)
        val stillwater = ShellNavigationCoordinator.dispatch(
            BadgeActionDestination.StillwaterVessel("stillwater_lake", "koi")
        )?.pending as PendingShellNavigation.OpenStillwaterSpecies
        assertEquals("stillwater_lake", stillwater.collectionId)
        assertEquals("koi", stillwater.speciesId)
        val beyond = ShellNavigationCoordinator.dispatch(
            BadgeActionDestination.BeyondBlue("blue_great_blue", "anglerfish")
        )?.pending as PendingShellNavigation.OpenBeyondBlue
        assertEquals("blue_great_blue", beyond.collectionId)
        assertEquals("anglerfish", beyond.speciesId)
        assertEquals(false, blue.requestId == beyond.requestId)
    }

    @Test fun externalDestinationsAreNotFalselyReportedAsChildRequests() {
        assertNull(ShellNavigationCoordinator.dispatch(BadgeActionDestination.Flow))
        assertNull(ShellNavigationCoordinator.dispatch(BadgeActionDestination.Arc))
        assertNull(ShellNavigationCoordinator.dispatch(BadgeActionDestination.MovementInfo))
    }

    @Test fun onlyExactChildConsumptionAcknowledgesANotification() {
        assertEquals(false, NotificationAcknowledgementPolicy.shouldMarkViewed(NavigationConsumptionResult.Pending))
        assertEquals(false, NotificationAcknowledgementPolicy.shouldMarkViewed(
            NavigationConsumptionResult.Failed(NavigationFailureReason.BADGE_NOT_FOUND)
        ))
        assertEquals(true, NotificationAcknowledgementPolicy.shouldMarkViewed(NavigationConsumptionResult.Consumed))
    }

    @Test fun blueFocusRequiresExactRenderedSpecies() {
        assertEquals(NavigationConsumptionResult.Consumed,
            validateBlueSpeciesFocus(null, catalogSpeciesExists = false, renderedSpeciesIds = emptySet()))
        assertEquals(NavigationConsumptionResult.Consumed,
            validateBlueSpeciesFocus("owned", catalogSpeciesExists = true, renderedSpeciesIds = setOf("owned")))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND),
            validateBlueSpeciesFocus("missing", catalogSpeciesExists = false, renderedSpeciesIds = emptySet()))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE),
            validateBlueSpeciesFocus("unowned", catalogSpeciesExists = true, renderedSpeciesIds = emptySet()))
    }

    @Test fun beyondBlueFocusRequiresMatchingBeyondSpeciesAndCollection() {
        assertEquals(NavigationConsumptionResult.Consumed,
            validateBeyondBlueFocus(speciesExists = true, isBeyondBlueSpecies = true, belongsToCollection = true))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE),
            validateBeyondBlueFocus(speciesExists = true, isBeyondBlueSpecies = true, belongsToCollection = false))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND),
            validateBeyondBlueFocus(speciesExists = false, isBeyondBlueSpecies = false, belongsToCollection = false))
    }

    @Test fun collectionFocusRejectsWrongSpeciesAndAcceptsVesselOnlyRequest() {
        val definition = CollectionCatalog.collections.first()
        val progress = CollectionProgressCalculator.calculate(definition, emptySet(), emptyMap(), emptySet())
        assertEquals(NavigationConsumptionResult.Consumed, validateCollectionSpeciesFocus(progress, null))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND),
            validateCollectionSpeciesFocus(progress, "not-in-this-collection"))
        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.COLLECTION_NOT_FOUND),
            validateCollectionSpeciesFocus(null, null))
    }

    @Test fun collectionFocusDoesNotExposeUndiscoveredSecretSpecies() {
        val definition = CollectionCatalog.collections.first()
        val base = CollectionProgressCalculator.calculate(definition, emptySet(), emptyMap(), emptySet())
        val secretId = base.speciesStates.first().speciesId
        val progress = base.copy(speciesStates = base.speciesStates.map {
            if (it.speciesId == secretId) it.copy(secret = true, discovered = false) else it
        })

        assertEquals(NavigationConsumptionResult.Failed(NavigationFailureReason.DESTINATION_UNAVAILABLE),
            validateCollectionSpeciesFocus(progress, secretId))
    }
}
