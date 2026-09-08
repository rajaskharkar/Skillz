import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct ShellContentCatalogParityTests {
    @Test func canonicalPearlObjectsAndReservedSlotMatchAndroidV1() {
        #expect(ShellContentCatalog.pearlObjects.count == 5)
        #expect(ShellContentCatalog.definition(ShellContentCatalog.focusLamp)?.pearlCost == 80)
        #expect(ShellContentCatalog.definition(ShellContentCatalog.focusPerch)?.pearlCost == 120)
        #expect(ShellContentCatalog.definition(ShellContentCatalog.focusPebbles)?.pearlCost == 60)
        let reserved = ShellContentCatalog.focusSlots.first { $0.id == "surge_current_nook" }
        #expect(reserved?.type == .surgeCurrent)
        #expect(ShellContentCatalog.finds.allSatisfy { find in
            guard let reserved else { return false }
            return !ShellContentCatalog.isCompatible(slot: reserved, find: find)
        })
    }

    @Test func slotCompatibilityUsesBothCategoryAndRewardKind() throws {
        let perch = try #require(ShellContentCatalog.focusSlots.first { $0.id == "creature_perch_left" })
        let shelf = try #require(ShellContentCatalog.focusSlots.first { $0.id == "left_reef_shelf" })
        let minnow = try #require(ShellContentCatalog.definition(ShellRewardCatalog.focusMinnow))
        let bubbles = try #require(ShellContentCatalog.definition(ShellContentCatalog.focusBubbles))
        let objectPerch = try #require(ShellContentCatalog.definition(ShellContentCatalog.focusPerch))
        #expect(ShellContentCatalog.isCompatible(slot: perch, find: minnow))
        #expect(!ShellContentCatalog.isCompatible(slot: perch, find: bubbles))
        #expect(ShellContentCatalog.isCompatible(slot: perch, find: objectPerch))
        #expect(!ShellContentCatalog.isCompatible(slot: shelf, find: minnow))
    }

    @Test func inventoryGroupsCopiesAndImplementsAndroidOrderingAndFilters() {
        let old = Date(timeIntervalSince1970: 100)
        let recent = Date(timeIntervalSince1970: 900)
        let values = [
            instance("m1", ShellRewardCatalog.focusMinnow, level: 3, acquiredAt: old),
            instance("m2", ShellRewardCatalog.focusMinnow, level: 3, acquiredAt: recent),
            instance("w1", ShellRewardCatalog.focusWhale, level: 1, acquiredAt: Date(timeIntervalSince1970: 500))
        ]
        let byLevel = ShellInventoryMapper.stacks(instances: values, quantityStacks: [], sort: .level)
        #expect(byLevel.map { "\($0.findID):\($0.level):\($0.count)" } == [
            "\(ShellRewardCatalog.focusMinnow):3:2",
            "\(ShellRewardCatalog.focusWhale):1:1"
        ])
        let newest = ShellInventoryMapper.stacks(instances: values, quantityStacks: [], sort: .newestArrival)
        #expect(newest.first?.findID == ShellRewardCatalog.focusMinnow)
        let greatBlue = ShellInventoryMapper.stacks(instances: values, quantityStacks: [], filter: .greatBlue)
        #expect(greatBlue.map(\.findID) == [ShellRewardCatalog.focusWhale])
        let trinket = ShellFindStack(findID: ShellContentCatalog.glimmer, quantity: 4, firstAcquiredAt: old, lastAcquiredAt: recent, isNew: true, viewedAt: nil)
        let stacked = ShellInventoryMapper.stacks(instances: [], quantityStacks: [trinket], filter: .trinkets)
        #expect(stacked.single?.count == 4)
        #expect(stacked.single?.level == 0)

        let stillwater = [
            instance("shrimp", "stillwater_shrimp", level: 1, acquiredAt: old),
            instance("nautilus", "stillwater_nautilus", level: 1, acquiredAt: recent)
        ]
        #expect(ShellInventoryMapper.stacks(instances: stillwater, quantityStacks: [], filter: .fishbowl).map(\.findID) == ["stillwater_shrimp"])
        #expect(ShellInventoryMapper.stacks(instances: stillwater, quantityStacks: [], filter: .aquarium).map(\.findID) == ["stillwater_nautilus"])
        #expect(ShellInventoryMapper.stacks(instances: stillwater, quantityStacks: [], filter: .pond).isEmpty)
        #expect(ShellInventoryMapper.stacks(instances: stillwater, quantityStacks: [], filter: .lake).isEmpty)
    }

    @Test func collectionCatalogUsesAndroidBlueThenStillwaterThenAggregateOrder() {
        #expect(Array(CollectionRosterCatalog.allCollectionIDs.prefix(4)) == [
            "blue_sunlit_reef", "blue_deeper_reef", "blue_open_blue", "blue_great_blue"
        ])
        #expect(Array(CollectionRosterCatalog.allCollectionIDs.dropFirst(4).prefix(4)) == [
            "stillwater_fishbowl", "stillwater_aquarium", "stillwater_pond", "stillwater_lake"
        ])
        #expect(Array(CollectionRosterCatalog.allCollectionIDs.suffix(3)) == [
            "collection_stillwater", "collection_the_blue", "collection_all_waters"
        ])
    }

    private func instance(_ id: String, _ findID: String, level: Int, acquiredAt: Date) -> ShellFindInstance {
        ShellFindInstance(id: id, findID: findID, acquiredAt: acquiredAt, sourceType: "test", sourceID: nil, currentUpgradeStageID: nil, isNew: false, isArchivedInChest: true, viewedAt: nil, animalLevel: level, lastActivityAt: acquiredAt)
    }
}

@MainActor
struct ShellCreatureUsabilityTests {
    @Test func newlyAcquiredCreatureAppearsInChestAndCanMoveToFocus() throws {
        let repository = InMemoryFlowRepository()
        let creature = ShellFindInstance(
            id: "new-minnow", findID: ShellRewardCatalog.focusMinnow, acquiredAt: .now,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: .now
        )
        repository.shellFindInstances = [creature]

        let viewModel = ShellViewModel(repository: repository)
        #expect(viewModel.allChestStacks.single?.instanceIDs == [creature.id])
        #expect(viewModel.firstRestingCreatureInstance(for: creature.findID)?.id == creature.id)
        let slot = try #require(viewModel.firstOpenFocusSlot(for: creature.findID))

        viewModel.place(instanceID: creature.id, slotID: slot.id)

        #expect(viewModel.placements.single?.instanceID == creature.id)
        #expect(viewModel.firstRestingCreatureInstance(for: creature.findID) == nil)
        #expect(viewModel.allChestStacks.isEmpty)
    }
}

@MainActor
struct ShellCollectionTransactionParityTests {
    @Test func flowCommitMaterializesPerCopyInventoryAndRetryIsIdempotent() throws {
        let repository = SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true))
        let flowInstanceID = UUID()
        let session = collectionSession(flowInstanceID: flowInstanceID, durationMs: 9_000_000, points: 482)
        _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        _ = try repository.commit(session: collectionSession(flowInstanceID: flowInstanceID, durationMs: 9_000_000, points: 999), activeArc: nil, recentlyEndedArc: nil)

        let instances = try repository.fetchShellFindInstances()
        #expect(instances.count == 3)
        #expect(Dictionary(grouping: instances, by: \.findID).mapValues(\.count) == [
            ShellRewardCatalog.focusWhale: 2,
            ShellRewardCatalog.focusSeahorse: 1
        ])
        #expect(try repository.fetchShellNotifications().filter { $0.kind == .find }.count == 3)
    }

    @Test func historicalBackfillHasDurableReceiptCountFloorsAndNoFalseNewState() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let sessionID = UUID()
        let acquired = Date(timeIntervalSince1970: 100)
        container.mainContext.insert(ShellFindGrantModel(id: "legacy-copy", findID: ShellRewardCatalog.focusMinnow, sourceSessionID: sessionID, acquiredAt: acquired))
        container.mainContext.insert(ShellRewardEventModel(event: ShellRewardEvent(
            id: "legacy-badge", sourceSessionID: sessionID, arcID: nil, type: .badgeUpdated,
            rewardID: ShellRewardCatalog.badgeFlow10, quantity: 5, occurredAt: acquired
        )))
        try container.mainContext.save()

        let first = try repository.reconcileShellCollections(version: 7, at: Date(timeIntervalSince1970: 200))
        let second = try repository.reconcileShellCollections(version: 7, at: Date(timeIntervalSince1970: 300))
        #expect(!first.alreadyCompleted)
        #expect(second.alreadyCompleted)
        #expect(try repository.fetchShellFindInstances().count == 1)
        #expect(try repository.fetchShellFindInstances().single?.isNew == false)
        let badge = try #require(repository.fetchAchievementDashboard().badges.first { $0.badgeID == ShellRewardCatalog.badgeFlow10 })
        #expect(badge.count == 5)
        #expect(!badge.isNew)
    }

    @Test func placementSwapsAtomicallyAndInvalidPlacementLeavesExistingNookUntouched() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let first = instance("first", ShellRewardCatalog.focusMinnow)
        let second = instance("second", ShellRewardCatalog.focusSeahorse)
        container.mainContext.insert(ShellFindInstanceModel(instance: first))
        container.mainContext.insert(ShellFindInstanceModel(instance: second))
        try container.mainContext.save()

        try repository.placeShellFind(instanceID: first.id, roomID: "FOCUS", slotID: "creature_perch_left", at: Date(timeIntervalSince1970: 10))
        try repository.placeShellFind(instanceID: second.id, roomID: "FOCUS", slotID: "creature_perch_left", at: Date(timeIntervalSince1970: 20))
        #expect(try repository.fetchShellPlacements(roomID: "FOCUS").single?.instanceID == second.id)
        #expect(try repository.fetchShellFindInstances().first { $0.id == first.id }?.isArchivedInChest == true)

        #expect(throws: ShellCollectionError.incompatibleSlot) {
            try repository.placeShellFind(instanceID: second.id, roomID: "FOCUS", slotID: "left_reef_shelf", at: Date(timeIntervalSince1970: 30))
        }
        #expect(try repository.fetchShellPlacements(roomID: "FOCUS").single?.instanceID == second.id)
    }

    @Test func objectUpgradeDebitsPearlsAndPersistsAuditWithoutPartialFailure() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        container.mainContext.insert(PearlLedgerModel(entry: PearlLedgerEntry(id: "seed", delta: 100, reason: "test", sourceType: "test", sourceID: nil, createdAt: .now, note: nil)))
        let pebble = ShellFindInstance(id: "pebble", findID: ShellContentCatalog.focusPebble, acquiredAt: .now, sourceType: "test", sourceID: nil, currentUpgradeStageID: "focus_pebble_base", isNew: false, isArchivedInChest: true, viewedAt: .now, animalLevel: 1, lastActivityAt: .now)
        container.mainContext.insert(ShellFindInstanceModel(instance: pebble))
        try container.mainContext.save()

        let upgrade = try repository.upgradeShellFind(instanceID: pebble.id, at: Date(timeIntervalSince1970: 500))
        #expect(upgrade.toStageID == "focus_pebble_polished")
        #expect(upgrade.pearlCost == 60)
        #expect(try repository.fetchPearlBalance() == 40)
        #expect(try repository.fetchShellFindUpgrades(instanceID: pebble.id).single == upgrade)
        #expect(try repository.fetchShellFindInstances().single?.currentUpgradeStageID == "focus_pebble_polished")
    }

    @Test func inviteFailureDoesNotCreateInventoryOrLedgerDebt() throws {
        let repository = SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true))
        #expect(throws: ShellCollectionError.insufficientPearls(required: 80, available: 0)) {
            _ = try repository.invitePearlObjectToChest(findID: ShellContentCatalog.focusLamp, at: .now)
        }
        #expect(try repository.fetchShellFindInstances().isEmpty)
        #expect(try repository.fetchPearlBalance() == 0)
    }

    @Test func swiftDataPinsReplaceAndPersistAsOneNormalizedTransaction() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let definitions = AchievementCatalog.definitions
        for definition in definitions {
            container.mainContext.insert(ShellBadgeModel(badgeID: definition.id, count: 1, earnedAt: .now))
        }
        try container.mainContext.save()
        for definition in definitions.prefix(3) {
            #expect(try repository.pinAchievement(badgeID: definition.id, replacing: nil, at: .now) == .pinned)
        }
        #expect(try repository.pinAchievement(badgeID: definitions[3].id, replacing: definitions[1].id, at: .now) == .pinned)

        let recreated = SwiftDataFlowRepository(container: container)
        let pins = try recreated.fetchAchievementDashboard().pinned
        #expect(pins.map(\.pinOrder) == [0, 1, 2])
        #expect(pins.map(\.badgeID) == [definitions[0].id, definitions[3].id, definitions[2].id])
    }

    private func instance(_ id: String, _ findID: String) -> ShellFindInstance {
        ShellFindInstance(id: id, findID: findID, acquiredAt: .now, sourceType: "test", sourceID: nil, currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: .now)
    }
}

@MainActor
struct AchievementDashboardParityTests {
    @Test func countFloorCannotReduceReliableHistoricalCount() throws {
        let repository = InMemoryFlowRepository()
        let date = Date(timeIntervalSince1970: 100)
        repository.shellBadges[ShellRewardCatalog.badgeFlow10] = ShellBadge(badgeID: ShellRewardCatalog.badgeFlow10, count: 2, firstEarnedAt: date, lastEarnedAt: date, isNew: true)
        repository.setAchievementCountFloor(badgeID: ShellRewardCatalog.badgeFlow10, minimumCount: 7, source: "legacy", at: date)
        repository.setAchievementCountFloor(badgeID: ShellRewardCatalog.badgeFlow10, minimumCount: 3, source: "stale", at: date)
        let badge = try #require(repository.fetchAchievementDashboard().badges.first { $0.badgeID == ShellRewardCatalog.badgeFlow10 })
        #expect(badge.count == 7)
        #expect(badge.milestone.currentThreshold == 5)
        #expect(badge.milestone.nextThreshold == 10)
        #expect(badge.remaining == 3)
    }

    @Test func threePinLimitRequiresExplicitReplacementAndNormalizesOrder() throws {
        let repository = InMemoryFlowRepository()
        let earned = AchievementCatalog.definitions
        for definition in earned {
            repository.shellBadges[definition.id] = ShellBadge(badgeID: definition.id, count: 1, firstEarnedAt: .now, lastEarnedAt: .now, isNew: true)
        }
        for definition in earned.prefix(3) { #expect(try repository.pinAchievement(badgeID: definition.id, replacing: nil, at: .now) == .pinned) }
        #expect(try repository.pinAchievement(badgeID: earned[3].id, replacing: nil, at: .now) == .replacementRequired(Array(earned.prefix(3).map(\.id))))
        #expect(try repository.pinAchievement(badgeID: earned[3].id, replacing: earned[1].id, at: .now) == .pinned)
        let pinned = repository.fetchAchievementDashboard().pinned
        #expect(pinned.count == 3)
        #expect(pinned.map(\.pinOrder) == [0, 1, 2])
        #expect(pinned.map(\.badgeID).contains(earned[3].id))
    }

    @Test func notificationAcknowledgementClearsOnlyExactSource() throws {
        let repository = InMemoryFlowRepository()
        let date = Date(timeIntervalSince1970: 50)
        repository.shellFindInstances = [
            ShellFindInstance(id: "a", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date, sourceType: "test", sourceID: nil, currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date),
            ShellFindInstance(id: "b", findID: ShellRewardCatalog.focusSeahorse, acquiredAt: date, sourceType: "test", sourceID: nil, currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date)
        ]
        repository.markShellNotificationViewed(id: "FIND:a", at: .now)
        #expect(repository.fetchShellNotifications().map(\.id) == ["FIND:b"])
    }

    @Test func coordinatorPreservesExactNotificationIdentity() {
        let find = ShellNotificationItem(id: "FIND:instance-7", kind: .find, sourceID: "instance-7", title: "Minnow", detail: "New", occurredAt: .now)
        let badge = ShellNotificationItem(id: "BADGE:badge-7", kind: .badge, sourceID: "badge-7", title: "Badge", detail: "New", occurredAt: .now)
        #expect(ShellNavigationCoordinator.destination(for: find) == .chest(instanceID: "instance-7", speciesID: nil))
        #expect(ShellNavigationCoordinator.destination(for: badge) == .badges(badgeID: "badge-7", collectionID: nil, speciesID: nil))
        #expect(ShellNavigationCoordinator.dispatch(find).room == .chest)
        #expect(ShellNavigationCoordinator.dispatch(badge).room == .badges)
    }

    @Test func notificationIsAcknowledgedOnlyAfterSuccessfulDestinationConsumption() throws {
        let repository = InMemoryFlowRepository()
        let acquiredAt = Date(timeIntervalSince1970: 80)
        repository.shellFindInstances = [
            ShellFindInstance(
                id: "notification-find", findID: ShellRewardCatalog.focusMinnow,
                acquiredAt: acquiredAt, sourceType: "test", sourceID: nil,
                currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true,
                viewedAt: nil, animalLevel: 1, lastActivityAt: acquiredAt
            )
        ]
        let viewModel = ShellViewModel(repository: repository)
        let notification = try #require(viewModel.notifications.first)

        #expect(viewModel.openNotification(notification) == .chest)
        #expect(repository.fetchShellNotifications().map(\.id) == [notification.id])
        viewModel.consumePendingDestination(success: false)
        #expect(repository.fetchShellNotifications().map(\.id) == [notification.id])
        #expect(viewModel.pendingDestination == nil)

        #expect(viewModel.openNotification(notification) == .chest)
        viewModel.consumePendingDestination(success: true)
        #expect(repository.fetchShellNotifications().isEmpty)
        #expect(viewModel.pendingDestination == nil)
    }

    @Test func unrelatedShellNavigationCannotAcknowledgeAQueuedNotification() throws {
        let repository = InMemoryFlowRepository()
        let acquiredAt = Date(timeIntervalSince1970: 81)
        repository.shellFindInstances = [
            ShellFindInstance(
                id: "queued-find", findID: ShellRewardCatalog.focusMinnow,
                acquiredAt: acquiredAt, sourceType: "test", sourceID: nil,
                currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true,
                viewedAt: nil, animalLevel: 1, lastActivityAt: acquiredAt
            )
        ]
        let viewModel = ShellViewModel(repository: repository)
        let notification = try #require(viewModel.notifications.first)

        _ = viewModel.openNotification(notification)
        _ = viewModel.prepareNavigation(.stillwaterVessel(collectionID: "stillwater_fishbowl"))
        viewModel.consumePendingDestination(success: true)

        #expect(repository.fetchShellNotifications().map(\.id) == [notification.id])
    }

    @Test func collectionActionPolicyMatchesAndroidAcquisitionDestinations() {
        let minnow = ShellRewardCatalog.focusMinnow
        let beyond = "creature_clownfish"
        let stillwater = "stillwater_shrimp"
        #expect(AchievementActionPolicy.speciesDestination(
            speciesID: minnow, ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .blueRegion(collectionID: "blue_sunlit_reef", speciesID: minnow))
        #expect(AchievementActionPolicy.speciesDestination(
            speciesID: beyond, ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .beyondBlue(collectionID: "blue_sunlit_reef", speciesID: beyond))
        #expect(AchievementActionPolicy.speciesDestination(
            speciesID: stillwater, ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .stillwaterVessel(collectionID: "stillwater_fishbowl", speciesID: stillwater))
        #expect(AchievementActionPolicy.speciesDestination(
            speciesID: beyond, ownedSpeciesIDs: [beyond], discoveredSpeciesIDs: [beyond]
        ) == .chestSpecies(speciesID: beyond))
        #expect(AchievementActionPolicy.destination(
            for: ShellRewardCatalog.badgeFlow30, category: .flow,
            ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .flow)
        #expect(AchievementActionPolicy.destination(
            for: "blue_sunlit_reef_completionist", category: .collections,
            ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .collectionDetails(collectionID: "blue_sunlit_reef"))
        #expect(AchievementActionPolicy.destination(
            for: "legacy_arc_badge", category: .arc,
            ownedSpeciesIDs: [], discoveredSpeciesIDs: []
        ) == .arc)
    }

    @Test func shellCoordinatorCarriesCollectionAndSpeciesIdentityToChildRoom() {
        let speciesID = "creature_clownfish"
        #expect(ShellNavigationCoordinator.dispatch(.collectionDetails(
            collectionID: "blue_sunlit_reef", speciesID: speciesID
        )) == ShellNavigationDispatch(
            room: .badges,
            pending: .badges(badgeID: nil, collectionID: "blue_sunlit_reef", speciesID: speciesID)
        ))
        #expect(ShellNavigationCoordinator.dispatch(.beyondBlue(
            collectionID: "blue_sunlit_reef", speciesID: speciesID
        )) == ShellNavigationDispatch(
            room: .theBlue,
            pending: .blue(collectionID: "blue_sunlit_reef", speciesID: speciesID, opensBeyondBlue: true)
        ))
        #expect(ShellNavigationCoordinator.dispatch(.stillwaterVessel(
            collectionID: "stillwater_fishbowl", speciesID: "stillwater_shrimp"
        ))?.pending == .stillwater(collectionID: "stillwater_fishbowl", speciesID: "stillwater_shrimp"))
        #expect(ShellNavigationCoordinator.dispatch(.chestSpecies(speciesID: speciesID))?.pending ==
            .chest(instanceID: nil, speciesID: speciesID))
    }

    @Test func collectionDetailsExposeEverySpeciesOnceWithUsableActions() throws {
        let repository = InMemoryFlowRepository()
        repository.shellFindInstances = [
            ShellFindInstance(
                id: "owned-minnow", findID: ShellRewardCatalog.focusMinnow, acquiredAt: .now,
                sourceType: "test", sourceID: nil, currentUpgradeStageID: nil,
                isNew: false, isArchivedInChest: true, viewedAt: .now,
                animalLevel: 12, lastActivityAt: .now
            )
        ]
        repository.recordCreatureDiscoveryInMemoryIfNeeded(
            speciesID: ShellRewardCatalog.focusMinnow,
            instanceID: "owned-minnow",
            discoveredAt: .now
        )
        let viewModel = ShellViewModel(repository: repository)

        #expect(viewModel.allCollectionProgress.map(\.id).count == Set(viewModel.allCollectionProgress.map(\.id)).count)
        let sunlit = viewModel.collectionSpecies(for: "blue_sunlit_reef")
        let minnow = try #require(sunlit.first { $0.speciesID == ShellRewardCatalog.focusMinnow })
        let clownfish = try #require(sunlit.first { $0.speciesID == "creature_clownfish" })
        #expect(minnow.ownedCount == 1)
        #expect(minnow.highestLevel == 12)
        #expect(minnow.action == .chestSpecies(speciesID: ShellRewardCatalog.focusMinnow))
        #expect(clownfish.action == .beyondBlue(collectionID: "blue_sunlit_reef", speciesID: "creature_clownfish"))
    }

    @Test func shellViewModelCoordinatesBalancesInventoryAndNewCounts() {
        let repository = InMemoryFlowRepository()
        repository.pearlLedger = [PearlLedgerEntry(id: "seed", delta: 25, reason: "test", sourceType: "test", sourceID: nil, createdAt: .now, note: nil)]
        repository.shellBadges[ShellRewardCatalog.badgeFlow10] = ShellBadge(badgeID: ShellRewardCatalog.badgeFlow10, count: 1, firstEarnedAt: .now, lastEarnedAt: .now, isNew: true)
        let viewModel = ShellViewModel(repository: repository)
        #expect(viewModel.pearlBalance == 25)
        #expect(viewModel.achievementDashboard.newCount == 1)
        viewModel.pin(ShellRewardCatalog.badgeFlow10)
        #expect(viewModel.achievementDashboard.pinned.map(\.badgeID) == [ShellRewardCatalog.badgeFlow10])
    }
}

@MainActor
struct ShellCollectionMigrationParityTests {
    @Test func versionThreeStoreLightweightMigratesToShellCollectionSchema() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("scyra-shell-v4-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Scyra.store")
        do {
            let schema = Schema(versionedSchema: ScyraSchemaV3.self)
            let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
            let container = try ModelContainer(for: schema, configurations: [configuration])
            container.mainContext.insert(PearlLedgerModel(entry: PearlLedgerEntry(id: "legacy", delta: 17, reason: "flow_reward", sourceType: "session", sourceID: "old", createdAt: .now, note: nil)))
            try container.mainContext.save()
        }

        let schema = Schema(versionedSchema: ScyraSchemaV4.self)
        let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
        let upgraded = try ModelContainer(for: schema, migrationPlan: ScyraMigrationPlan.self, configurations: [configuration])
        let repository = SwiftDataFlowRepository(container: upgraded)
        #expect(try repository.fetchPearlBalance() == 17)
        _ = try repository.reconcileShellCollections(version: 1, at: .now)
        #expect(try repository.fetchShellFindInstances().isEmpty)
    }
}

@MainActor
private func collectionSession(flowInstanceID: UUID, durationMs: Int64, points: Int) -> FlowSession {
    let end = Date(timeIntervalSince1970: 10_000)
    return FlowSession(id: UUID(), flowInstanceID: flowInstanceID, title: "Chest", description: "", journeyName: "Scyra", startTime: end.addingTimeInterval(-Double(durationMs) / 1_000), endTime: end, durationMs: durationMs, surgePlannedMs: nil, surgePoints: 0, scyraPoints: points, isSoftMode: false, arcID: nil, arcIndex: nil, arcMultiplierUsed: nil, arcBonusPoints: 0, createdAt: end)
}

private extension Array {
    var single: Element? { count == 1 ? first : nil }
}
