package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindStackEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.repository.shell.SHELL_CHEST_ROUTE
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.data.repository.shell.ShellNotificationType
import com.kingkharnivore.skillz.data.repository.shell.notificationId
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class ShellNotificationInlayMapperTest {
    @Test
    fun badgeCountIncludesOnlyUnviewedNotifications() {
        val state = ShellUiState(
            finds = listOf(
                find("find-new", viewedAt = null, acquiredAt = 20L),
                find("find-viewed", viewedAt = 50L, acquiredAt = 30L)
            ),
            badges = listOf(
                badge("badge_flow_10_min", viewedAt = null, earnedAt = 10L),
                badge("badge_flow_30_min", viewedAt = 60L, earnedAt = 40L)
            )
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(2, notifications.size)
        assertTrue(notifications.all { it.id != notificationId(ShellNotificationType.FIND, "find-viewed") })
        assertTrue(notifications.all { it.id != notificationId(ShellNotificationType.BADGE, "badge_flow_30_min") })
    }

    @Test
    fun obsoleteDiscoveryBadgeDoesNotProduceNotification() {
        val state = ShellUiState(badges = listOf(badge("badge_discovery", viewedAt = null)))

        assertTrue(unviewedShellNotifications(state).isEmpty())
    }

    @Test
    fun viewedNotificationsDisappearFromActiveInlay() {
        val before = ShellUiState(
            finds = listOf(find("find-new", viewedAt = null)),
            badges = listOf(badge("badge_flow_10_min", viewedAt = null))
        )
        val after = before.copy(
            finds = before.finds.map { it.copy(viewedAt = 123L) },
            badges = before.badges.map { it.copy(viewedAt = 123L) }
        )

        assertEquals(2, unviewedShellNotifications(before).size)
        assertTrue(unviewedShellNotifications(after).isEmpty())
    }

    @Test
    fun newestNotificationsAppearFirstAndExposeDeepLinkRoutes() {
        val state = ShellUiState(
            finds = listOf(find("find-new", acquiredAt = 20L)),
            badges = listOf(badge("badge_flow_10_min", earnedAt = 30L))
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(notificationId(ShellNotificationType.BADGE, "badge_flow_10_min"), notifications[0].id)
        assertNull(notifications[0].deepLinkRoute)
        assertEquals(BadgeActionDestination.BadgeDetails("badge_flow_10_min"), (notifications[0] as ShellNotificationInlayItem.Badge).destination)
        assertEquals(SHELL_CHEST_ROUTE, notifications[1].deepLinkRoute)
    }

    @Test
    fun deliveredAtLabelShowsOnlyTimeForTodayInUserTimezone() {
        val zoneId = ZoneId.of("America/New_York")
        val now = millis(2026, 6, 12, 22, 0, zoneId)
        val createdAt = millis(2026, 6, 12, 20, 42, zoneId)

        assertEquals(
            "8:42 PM",
            formatNotificationDeliveredAt(createdAt, now, zoneId, Locale.US)
        )
    }

    @Test
    fun deliveredAtLabelShowsMonthDayForEarlierDateInCurrentYear() {
        val zoneId = ZoneId.of("America/New_York")
        val now = millis(2026, 6, 12, 22, 0, zoneId)
        val createdAt = millis(2026, 6, 10, 20, 42, zoneId)

        assertEquals(
            "Jun 10",
            formatNotificationDeliveredAt(createdAt, now, zoneId, Locale.US)
        )
    }

    @Test
    fun deliveredAtLabelShowsMonthDayYearForPreviousYear() {
        val zoneId = ZoneId.of("America/New_York")
        val now = millis(2026, 6, 12, 22, 0, zoneId)
        val createdAt = millis(2025, 6, 10, 20, 42, zoneId)

        assertEquals(
            "Jun 10, 2025",
            formatNotificationDeliveredAt(createdAt, now, zoneId, Locale.US)
        )
    }

    @Test
    fun deliveredAtLabelUsesUserTimezoneToDecideToday() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val now = millis(2026, 6, 12, 1, 30, zoneId)
        val createdAt = millis(2026, 6, 12, 0, 15, zoneId)

        assertEquals(
            "12:15 AM",
            formatNotificationDeliveredAt(createdAt, now, zoneId, Locale.US)
        )
    }

    @Test
    fun enteringTheBlueCanClearIsNewWithoutClearingNotificationBadgeCount() {
        val state = ShellUiState(
            finds = listOf(find("blue-seen", isNew = false, viewedAt = null)),
            badges = emptyList()
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(1, notifications.size)
        assertEquals(notificationId(ShellNotificationType.FIND, "blue-seen"), notifications.single().id)
    }

    @Test
    fun retiredDiscoveryStackObjectAndTrinketRecordsDoNotAppearInInlay() {
        val state = ShellUiState(
            finds = listOf(
                find("creature", findId = ShellContentCatalog.FOCUS_MINNOW, viewedAt = null),
                find("object", findId = ShellContentCatalog.FOCUS_PEBBLE, viewedAt = null),
                find("trinket", findId = ShellContentCatalog.TRINKET_GLIMMER, viewedAt = null),
                find("released", findId = ShellContentCatalog.FOCUS_MINNOW, viewedAt = null, status = CreatureStatus.RELEASED)
            ),
            stacks = listOf(stack(ShellContentCatalog.TRINKET_GLIMMER)),
            discoveries = listOf(discovery("discovery_glimmer")),
            badges = emptyList()
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(listOf(notificationId(ShellNotificationType.FIND, "creature")), notifications.map { it.id })
    }

    // TODO: Add in-memory DAO/repository tests for mark-seen vs mark-viewed behavior
    // once the repository test fixture can construct the full Shell DAO graph.

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zoneId: ZoneId
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()

    private fun find(
        instanceId: String,
        findId: String = ShellContentCatalog.FOCUS_MINNOW,
        viewedAt: Long? = null,
        isNew: Boolean = viewedAt == null,
        acquiredAt: Long = 1L,
        status: String = CreatureStatus.ACTIVE
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = findId,
        acquiredAt = acquiredAt,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = null,
        customName = null,
        isNew = isNew,
        isArchivedInChest = true,
        viewedAt = viewedAt,
        creatureStatus = status
    )

    private fun badge(
        badgeId: String,
        viewedAt: Long? = null,
        earnedAt: Long = 1L
    ) = UserBadgeEntity(
        badgeId = badgeId,
        count = 1,
        firstEarnedAt = earnedAt,
        lastEarnedAt = earnedAt,
        isNew = viewedAt == null,
        viewedAt = viewedAt
    )

    private fun stack(findId: String) = UserShellFindStackEntity(
        findId = findId,
        quantity = 1,
        firstAcquiredAt = 1L,
        lastAcquiredAt = 1L,
        isNew = true,
        viewedAt = null
    )

    private fun discovery(discoveryId: String) = UserDiscoveryEntity(
        userDiscoveryId = "user-$discoveryId",
        discoveryId = discoveryId,
        discoveredAt = 1L,
        sourceType = "test",
        sourceId = null,
        grantedFindInstanceId = null,
        isNew = true,
        viewedAt = null
    )
}
