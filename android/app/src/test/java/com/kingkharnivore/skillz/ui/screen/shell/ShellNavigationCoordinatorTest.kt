package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellNavigationCoordinatorTest {
    @Test fun exactBadgeAndCollectionRequestsRetainTheirIds() {
        assertEquals(
            PendingShellNavigation.OpenBadge("badge-7", "notification-1"),
            ShellNavigationCoordinator.dispatch(
                BadgeActionDestination.BadgeDetails("badge-7"), "notification-1"
            )?.pending
        )
        assertEquals(
            PendingShellNavigation.OpenCollection("blue_open_blue"),
            ShellNavigationCoordinator.dispatch(
                BadgeActionDestination.CollectionDetails("blue_open_blue")
            )?.pending
        )
    }

    @Test fun speciesIdentitySurvivesEveryRoomDispatch() {
        assertEquals(
            PendingShellNavigation.OpenChestSpecies("minnow"),
            ShellNavigationCoordinator.dispatch(BadgeActionDestination.ChestSpecies("minnow"))?.pending
        )
        assertEquals(
            PendingShellNavigation.OpenBlueSpecies("blue_great_blue", "whale"),
            ShellNavigationCoordinator.dispatch(
                BadgeActionDestination.BlueRegion("blue_great_blue", "whale")
            )?.pending
        )
        assertEquals(
            PendingShellNavigation.OpenStillwaterSpecies("stillwater_lake", "koi"),
            ShellNavigationCoordinator.dispatch(
                BadgeActionDestination.StillwaterVessel("stillwater_lake", "koi")
            )?.pending
        )
        assertEquals(
            PendingShellNavigation.OpenBeyondBlue("blue_great_blue", "anglerfish"),
            ShellNavigationCoordinator.dispatch(
                BadgeActionDestination.BeyondBlue("blue_great_blue", "anglerfish")
            )?.pending
        )
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
}
