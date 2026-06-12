package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindStackEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.repository.shell.SHELL_BADGES_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.SHELL_CHEST_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.ShellNotificationType
import com.kingkharnivore.skillz.data.repository.shell.notificationId
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun markTheBlueAnimalsSeenClearsOnlyIsNewAndKeepsViewedAtNull() {
        val daoSource = File("app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaos.kt").readText()
        val repositorySource = File("app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/ShellRepository.kt").readText()

        assertTrue(daoSource.contains("UPDATE user_shell_find_instance SET isNew = 0 WHERE findId IN (:findIds)"))
        assertFalse(daoSource.contains("SET isNew = 0, viewedAt = COALESCE(viewedAt, :viewedAt) WHERE findId IN (:findIds)"))
        assertTrue(repositorySource.contains("findInstanceDao.markFindIdsSeen(animalFindIds)"))
    }

    @Test
    fun markNotificationViewedSetsViewedAtThroughNotificationSpecificDaoMethods() {
        val daoSource = File("app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaos.kt").readText()
        val repositorySource = File("app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/ShellRepository.kt").readText()

        assertTrue(daoSource.contains("viewedAt = COALESCE(viewedAt, :viewedAt) WHERE instanceId = :instanceId"))
        assertTrue(daoSource.contains("viewedAt = COALESCE(viewedAt, :viewedAt) WHERE badgeId = :badgeId"))
        assertTrue(repositorySource.contains("fun markNotificationViewed(notificationId: String)"))
        assertTrue(repositorySource.contains("findInstanceDao.markViewed(notificationId.substringAfter(':'), now)"))
        assertTrue(repositorySource.contains("badgeDao.markViewed(notificationId.substringAfter(':'), now)"))
    }

    @Test
    fun markAllNotificationsViewedSetsViewedAtForAllUnviewedNotificationSources() {
        val daoSource = File("app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaos.kt").readText()
        val repositorySource = File("app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/ShellRepository.kt").readText()

        assertTrue(daoSource.contains("viewedAt = COALESCE(viewedAt, :viewedAt) WHERE viewedAt IS NULL"))
        assertTrue(daoSource.contains("WHERE viewedAt IS NULL AND findId IN (:findIds)"))
        assertTrue(repositorySource.contains("fun markAllNotificationsViewed()"))
        assertTrue(repositorySource.contains("findInstanceDao.markFindIdsViewed(ShellContentCatalog.allAnimalFindIds.toList(), now)"))
        assertTrue(repositorySource.contains("badgeDao.markAllViewed(now)"))
    }

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
