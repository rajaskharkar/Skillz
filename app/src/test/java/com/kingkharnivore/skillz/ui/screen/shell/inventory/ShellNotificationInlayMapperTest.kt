package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.repository.shell.SHELL_BADGES_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.SHELL_CHEST_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.ShellNotificationType
import com.kingkharnivore.skillz.data.repository.shell.notificationId
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellNotificationInlayMapperTest {
    @Test
    fun badgeCountIncludesOnlyUnviewedNotifications() {
        val state = ShellUiState(
            finds = listOf(
                find("find-new", viewedAt = null, acquiredAt = 20L),
                find("find-viewed", viewedAt = 50L, acquiredAt = 30L)
            ),
            badges = listOf(
                badge("badge-new", viewedAt = null, earnedAt = 10L),
                badge("badge-viewed", viewedAt = 60L, earnedAt = 40L)
            )
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(2, notifications.size)
        assertTrue(notifications.all { it.id != notificationId(ShellNotificationType.FIND, "find-viewed") })
        assertTrue(notifications.all { it.id != notificationId(ShellNotificationType.BADGE, "badge-viewed") })
    }

    @Test
    fun viewedNotificationsDisappearFromActiveInlay() {
        val before = ShellUiState(
            finds = listOf(find("find-new", viewedAt = null)),
            badges = listOf(badge("badge-new", viewedAt = null))
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
            badges = listOf(badge("badge-new", earnedAt = 30L))
        )

        val notifications = unviewedShellNotifications(state)

        assertEquals(notificationId(ShellNotificationType.BADGE, "badge-new"), notifications[0].id)
        assertEquals(SHELL_BADGES_ROUTE, notifications[0].deepLinkRoute)
        assertEquals(SHELL_CHEST_ROUTE, notifications[1].deepLinkRoute)
    }

    private fun find(
        instanceId: String,
        viewedAt: Long? = null,
        acquiredAt: Long = 1L
    ) = UserShellFindInstanceEntity(
        instanceId = instanceId,
        findId = ShellContentCatalog.FOCUS_MINNOW,
        acquiredAt = acquiredAt,
        sourceType = "test",
        sourceId = null,
        currentUpgradeStageId = null,
        customName = null,
        isNew = viewedAt == null,
        isArchivedInChest = true,
        viewedAt = viewedAt,
        creatureStatus = CreatureStatus.ACTIVE
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
}
