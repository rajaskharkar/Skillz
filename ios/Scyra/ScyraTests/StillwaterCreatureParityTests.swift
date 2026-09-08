import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct StillwaterDomainParityTests {
    @Test func canonicalVesselsCatalogWeightsAndReleaseValuesMatchAndroid() throws {
        #expect(StillwaterVessel.allCases.map(\.dropCost) == [15_000, 25_000, 45_000, 75_000])
        #expect(StillwaterVessel.fishbowl.requiresConfirmation == false)
        #expect(StillwaterVessel.aquarium.requiresConfirmation == false)
        #expect(StillwaterVessel.pond.requiresConfirmation)
        #expect(StillwaterVessel.lake.requiresConfirmation)
        #expect(StillwaterCatalog.creatures.count == 32)
        #expect(StillwaterVessel.allCases.allSatisfy { StillwaterCatalog.creatures(for: $0).count == 8 })
        #expect(Set(StillwaterCatalog.creatures.map(\.id)).count == 32)
        #expect(CreatureCatalog.all.count == 71)
        #expect(CreatureCatalog.flowEarned.count == 4)
        #expect(CreatureCatalog.beyondBlue.count == 35)
        #expect(CreatureCatalog.stillwaterCreatures.count == 32)

        #expect(StillwaterCatalog.rarity(for: 0) == .common)
        #expect(StillwaterCatalog.rarity(for: 59) == .common)
        #expect(StillwaterCatalog.rarity(for: 60) == .uncommon)
        #expect(StillwaterCatalog.rarity(for: 89) == .uncommon)
        #expect(StillwaterCatalog.rarity(for: 90) == .rare)
        #expect(StillwaterCatalog.rarity(for: 97) == .rare)
        #expect(StillwaterCatalog.rarity(for: 98) == .mythic)
        #expect(StillwaterCatalog.rarity(for: 99) == .mythic)
        #expect(StillwaterCatalog.roll(vessel: .fishbowl, rarityRoll: 0, selectionRoll: 2).id == "stillwater_clam")
        #expect(StillwaterCatalog.roll(vessel: .aquarium, rarityRoll: 98, selectionRoll: 0).id == "stillwater_nautilus")

        #expect(CreatureEconomy.releaseValuePearls("stillwater_clam") == 125)
        #expect(CreatureEconomy.releaseValuePearls("stillwater_lionfish") == 208)
        #expect(CreatureEconomy.releaseValuePearls("stillwater_barracuda") == 375)
        #expect(CreatureEconomy.releaseValuePearls("stillwater_coelacanth") == 625)
    }

    @Test func historicalNonStillwaterFindsUnlockEveryShallowerVessel() {
        let now = Date(timeIntervalSince1970: 100)
        let stillwaterAtDepth = instance("sw", "stillwater_coelacanth", at: now)
        #expect(CreatureUnlockPolicy.unlockedZones(from: [stillwaterAtDepth]) == [.sunlitReef])
        let deep = instance("deep", ShellRewardCatalog.focusManta, at: now)
        #expect(CreatureUnlockPolicy.unlockedZones(from: [stillwaterAtDepth, deep]) == [.sunlitReef, .deeperReef, .openBlue])
        // Status is intentionally external to the historical row: releasing the
        // Manta must never revoke a depth that was already reached.
        #expect(CreatureUnlockPolicy.unlockedZones(from: [deep]).contains(.openBlue))
    }

    @Test func growthCurveAndMasteryTiersMatchCanonicalThresholds() {
        #expect(CreatureEconomy.masteryTier(level: 9) == nil)
        #expect(CreatureEconomy.masteryTier(level: 10) == .seasoned)
        #expect(CreatureEconomy.masteryTier(level: 25) == .proven)
        #expect(CreatureEconomy.masteryTier(level: 50) == .veteran)
        #expect(CreatureEconomy.masteryTier(level: 75) == .ascendant)
        #expect(CreatureEconomy.masteryTier(level: 99) == .mastered)
        let id = ShellRewardCatalog.focusMinnow
        #expect(CreatureEconomy.releaseValuePearls(id, level: 10) > CreatureEconomy.releaseValuePearls(id, level: 1))
        #expect(CreatureEconomy.releaseValuePearls(id, level: 99) > CreatureEconomy.releaseValuePearls(id, level: 75))
        #expect(CreatureEconomy.growthCostPearls(id, currentLevel: 98) > CreatureEconomy.growthCostPearls(id, currentLevel: 1))
    }

    private func instance(_ id: String, _ findID: String, at date: Date) -> ShellFindInstance {
        .init(id: id, findID: findID, acquiredAt: date, sourceType: "test", sourceID: nil,
              currentUpgradeStageID: nil, isNew: false, isArchivedInChest: true,
              viewedAt: date, animalLevel: 1, lastActivityAt: date)
    }
}

@MainActor
struct StillwaterTransactionParityTests {
    @Test func drawAtomicallyDebitsDropsGrantsV4CopyAndRecordsEvidence() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let date = Date(timeIntervalSince1970: 1_000)
        container.mainContext.insert(StillwaterLedgerModel(entry: .init(
            id: "seed", units: 15_000, sourceType: "test", sourceID: nil, createdAt: date
        )))
        try container.mainContext.save()

        let result = try repository.drawFromStillwater(
            vessel: .fishbowl, rarityRoll: 0, selectionRoll: 2, at: date.addingTimeInterval(1)
        )
        #expect(result.creature.id == "stillwater_clam")
        #expect(result.rarity == .common)
        #expect(result.wasFirstDiscovery)
        #expect(result.remainingDrops == 0)
        #expect(try repository.fetchStillwaterBalance() == 0)
        #expect(try repository.fetchStillwaterLifetimeTotal() == 15_000)
        let copy = try #require(repository.fetchShellFindInstances().single)
        #expect(copy.id == result.instance.id)
        #expect(copy.sourceType == "stillwater")
        #expect(copy.sourceID == StillwaterVessel.fishbowl.rawValue)
        #expect(try repository.creatureStatus(instanceID: copy.id) == .active)
        #expect(try repository.fetchCreatureDiscoveries().map(\.speciesID) == ["stillwater_clam"])
        let dashboard = try repository.fetchAchievementDashboard()
        #expect(dashboard.badges.first { $0.badgeID == "stillwater_first_catch" }?.count == 1)
        #expect(dashboard.badges.first { $0.badgeID == "stillwater_variety" }?.count == 1)
    }

    @Test func invalidDrawRollsBackWithoutAFindDebitOrDiscovery() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        container.mainContext.insert(StillwaterLedgerModel(entry: .init(
            id: "seed", units: 25_000, sourceType: "test", sourceID: nil, createdAt: .now
        )))
        try container.mainContext.save()

        #expect(throws: StillwaterError.locked) {
            try repository.drawFromStillwater(vessel: .aquarium, rarityRoll: 98, selectionRoll: 0, at: .now)
        }
        #expect(try repository.fetchStillwaterBalance() == 25_000)
        #expect(try repository.fetchShellFindInstances().isEmpty)
        #expect(try repository.fetchCreatureDiscoveries().isEmpty)
        #expect(try repository.fetchStillwaterLedger().count == 1)
    }

    @Test func perspectivePersistsAndSpendableNeverChangesLifetimeTotal() throws {
        let repository = SwiftDataFlowRepository(container: try ScyraPersistenceFactory.makeContainer(inMemory: true))
        #expect(try repository.fetchStillwaterPerspective() == .overview)
        try repository.saveStillwaterPerspective(.lake, at: Date(timeIntervalSince1970: 10))
        #expect(try repository.fetchStillwaterPerspective() == .lake)
    }

    @Test func level99MasteryIsImmutableIdempotentAndSurvivesRelease() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let date = Date(timeIntervalSince1970: 2_000)
        let creature = ShellFindInstance(
            id: "almost-master", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 98, lastActivityAt: date
        )
        container.mainContext.insert(ShellFindInstanceModel(instance: creature))
        container.mainContext.insert(CreatureLifecycleModel(instanceID: creature.id, status: .active, updatedAt: date))
        container.mainContext.insert(PearlLedgerModel(entry: .init(
            id: "seed", delta: 1_000_000, reason: "test", sourceType: "test", sourceID: nil,
            createdAt: date, note: nil
        )))
        try container.mainContext.save()

        let first = try repository.growCreature(instanceID: creature.id, transactionID: "grow-99", at: date.addingTimeInterval(1))
        let balanceAfterFirst = try repository.fetchPearlBalance()
        let retry = try repository.growCreature(instanceID: creature.id, transactionID: "grow-99", at: date.addingTimeInterval(2))
        #expect(first.resultingLevel == 99)
        #expect(first.recordedMastery)
        #expect(retry == first)
        #expect(try repository.fetchPearlBalance() == balanceAfterFirst)
        #expect(try repository.fetchCreatureMasteries().count == 1)
        #expect(try repository.fetchPendingMasteryCelebration()?.speciesID == ShellRewardCatalog.focusMinnow)

        let payout = try repository.releaseCreature(instanceID: creature.id, at: date.addingTimeInterval(3))
        #expect(payout > 0)
        #expect(try repository.creatureStatus(instanceID: creature.id) == .released)
        #expect(try repository.fetchShellFindInstances().isEmpty)
        #expect(try repository.fetchCreatureMasteries().count == 1)
        #expect(try repository.fetchCreatureDiscoveries().single?.speciesID == ShellRewardCatalog.focusMinnow)
    }

    @Test func levelAwareBulkReleaseCommitsAllCopiesAndPearlsTogether() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let date = Date(timeIntervalSince1970: 2_200)
        let low = ShellFindInstance(
            id: "low", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: false, viewedAt: date, animalLevel: 1, lastActivityAt: date
        )
        let high = ShellFindInstance(
            id: "high", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 25, lastActivityAt: date
        )
        let keep = ShellFindInstance(
            id: "keep", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 50, lastActivityAt: date
        )
        for creature in [low, high, keep] {
            container.mainContext.insert(ShellFindInstanceModel(instance: creature))
            container.mainContext.insert(CreatureLifecycleModel(instanceID: creature.id, status: .active, updatedAt: date))
        }
        container.mainContext.insert(ShellPlacementModel(placement: .init(
            id: "placed", roomID: "FOCUS", slotID: "creature_perch_left", instanceID: low.id, placedAt: date
        )))
        try container.mainContext.save()

        let expected = CreatureEconomy.releaseValuePearls(low.findID, level: low.animalLevel)
            + CreatureEconomy.releaseValuePearls(high.findID, level: high.animalLevel)
        let payout = try repository.releaseCreatures(instanceIDs: [low.id, high.id, low.id], at: date.addingTimeInterval(1))

        #expect(payout == expected)
        #expect(try repository.fetchPearlBalance() == expected)
        #expect(try repository.creatureStatus(instanceID: low.id) == .released)
        #expect(try repository.creatureStatus(instanceID: high.id) == .released)
        #expect(try repository.creatureStatus(instanceID: keep.id) == .active)
        #expect(try repository.fetchShellFindInstances().map(\.id) == [keep.id])
        #expect(try repository.fetchShellPlacements(roomID: "FOCUS").isEmpty)
    }

    @Test func invalidBulkReleaseRollsBackEverySelectedCreature() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let date = Date(timeIntervalSince1970: 2_300)
        let creature = ShellFindInstance(
            id: "stay-active", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 8, lastActivityAt: date
        )
        container.mainContext.insert(ShellFindInstanceModel(instance: creature))
        container.mainContext.insert(CreatureLifecycleModel(instanceID: creature.id, status: .active, updatedAt: date))
        try container.mainContext.save()

        #expect(throws: CreatureShellError.missingCreature) {
            try repository.releaseCreatures(instanceIDs: [creature.id, "missing"], at: date.addingTimeInterval(1))
        }
        #expect(try repository.creatureStatus(instanceID: creature.id) == .active)
        #expect(try repository.fetchShellFindInstances().map(\.id) == [creature.id])
        #expect(try repository.fetchPearlBalance() == 0)
    }

    @Test func beyondBlueTradeAndPearlPaymentCommitTogether() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let date = Date(timeIntervalSince1970: 2_500)
        let minnow = ShellFindInstance(
            id: "trade-minnow", findID: ShellRewardCatalog.focusMinnow, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 40, lastActivityAt: date
        )
        container.mainContext.insert(ShellFindInstanceModel(instance: minnow))
        container.mainContext.insert(CreatureLifecycleModel(instanceID: minnow.id, status: .active, updatedAt: date))
        container.mainContext.insert(PearlLedgerModel(entry: .init(
            id: "seed", delta: 100, reason: "test", sourceType: "test", sourceID: nil,
            createdAt: date, note: nil
        )))
        try container.mainContext.save()

        let quote = try repository.quoteBeyondBlueEncounter(
            targetCreatureID: "creature_clownfish", selectedInstanceIDs: [minnow.id]
        )
        #expect(quote.targetRequirementMinutes == 30)
        #expect(quote.selectedCreatureMinutes == 10, "Trade contribution is species time, not animal level")
        #expect(quote.pearlCostForRemaining == 40)
        #expect(quote.canEncounter)

        let encountered = try repository.encounterBeyondBlue(
            targetCreatureID: "creature_clownfish", selectedInstanceIDs: [minnow.id], at: date.addingTimeInterval(1)
        )
        #expect(encountered.findID == "creature_clownfish")
        #expect(try repository.creatureStatus(instanceID: minnow.id) == .usedBeyondBlue)
        #expect(try repository.creatureStatus(instanceID: encountered.id) == .active)
        #expect(try repository.fetchPearlBalance() == 60)
        #expect(try repository.fetchShellFindInstances().map(\.findID) == ["creature_clownfish"])
        #expect(try repository.fetchCreatureDiscoveries().contains { $0.speciesID == "creature_clownfish" })
    }

    @Test func insufficientBeyondBluePaymentDoesNotConsumeTradeSelection() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let creature = ShellFindInstance(
            id: "keep-me", findID: ShellRewardCatalog.focusMinnow, acquiredAt: .now,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: .now, animalLevel: 1, lastActivityAt: .now
        )
        container.mainContext.insert(ShellFindInstanceModel(instance: creature))
        container.mainContext.insert(CreatureLifecycleModel(instanceID: creature.id, status: .active, updatedAt: .now))
        try container.mainContext.save()

        #expect(throws: CreatureShellError.insufficientPearls(required: 40, available: 0)) {
            try repository.encounterBeyondBlue(
                targetCreatureID: "creature_clownfish", selectedInstanceIDs: [creature.id], at: .now
            )
        }
        #expect(try repository.creatureStatus(instanceID: creature.id) == .active)
        #expect(try repository.fetchShellFindInstances().map(\.id) == [creature.id])
        #expect(try repository.fetchPearlBalance() == 0)
    }

    @Test func fullStillwaterRosterWritesCollectorAndCuratorEditionEvidence() throws {
        let repository = InMemoryFlowRepository()
        let date = Date(timeIntervalSince1970: 3_000)
        repository.stillwaterLedger = [.init(id: "seed", units: 2_000_000, sourceType: "test", sourceID: nil, createdAt: date)]
        repository.shellFindInstances.append(.init(
            id: "whale", findID: ShellRewardCatalog.focusWhale, acquiredAt: date,
            sourceType: "session", sourceID: "flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: date, animalLevel: 1, lastActivityAt: date
        ))
        for (offset, entry) in StillwaterCatalog.creatures.enumerated() {
            let pool = StillwaterCatalog.creatures(for: entry.vessel).filter { $0.rarity == entry.rarity }
            let selection = try #require(pool.firstIndex(of: entry))
            let rarityRoll = switch entry.rarity { case .common: 0; case .uncommon: 60; case .rare: 90; case .mythic: 98 }
            _ = try repository.drawFromStillwater(
                vessel: entry.vessel, rarityRoll: rarityRoll, selectionRoll: selection,
                at: date.addingTimeInterval(Double(offset + 1))
            )
        }
        let overall = try #require(repository.fetchStillwaterCollectionProgress().first { $0.id == "collection_stillwater" })
        #expect(overall.total == 32)
        #expect(overall.discovered == 32)
        #expect(overall.owned == 32)
        #expect(overall.collectorEarned)
        #expect(overall.curatorEarned)
        #expect(!overall.completionistEarned)
        #expect(repository.fetchAchievementDashboard().badges.first { $0.badgeID == "stillwater_variety" }?.count == 32)

        let released = try #require(repository.fetchShellFindInstances().first { $0.findID.hasPrefix("stillwater_") })
        _ = try repository.releaseCreature(instanceID: released.id, at: date.addingTimeInterval(100))
        let afterRelease = try #require(repository.fetchStillwaterCollectionProgress().first { $0.id == "collection_stillwater" })
        #expect(afterRelease.owned == 31)
        #expect(afterRelease.curatorEarned, "Historical completion evidence must survive release")
        #expect(afterRelease.discovered == 32)
    }

    @Test func historicalLevel99FindBackfillsEvidenceOnceWithoutNewAlerts() throws {
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        let repository = SwiftDataFlowRepository(container: container)
        let acquiredAt = Date(timeIntervalSince1970: 4_000)
        let masteredAt = acquiredAt.addingTimeInterval(9_900)
        let legacy = ShellFindInstance(
            id: "legacy-master", findID: ShellRewardCatalog.focusMinnow, acquiredAt: acquiredAt,
            sourceType: "session", sourceID: "old-flow", currentUpgradeStageID: nil,
            isNew: false, isArchivedInChest: true, viewedAt: acquiredAt,
            animalLevel: CreatureEconomy.maxLevel, lastActivityAt: masteredAt
        )
        container.mainContext.insert(ShellFindInstanceModel(instance: legacy))
        try container.mainContext.save()

        let first = try repository.reconcileShellCollections(version: 2, at: masteredAt.addingTimeInterval(1))
        let retry = try repository.reconcileShellCollections(version: 2, at: masteredAt.addingTimeInterval(2))
        #expect(!first.alreadyCompleted)
        #expect(retry.alreadyCompleted)

        let discovery = try #require(repository.fetchCreatureDiscoveries().single)
        #expect(discovery.speciesID == legacy.findID)
        #expect(discovery.firstDiscoveredAt == acquiredAt)
        #expect(discovery.timestampConfidence == "ESTIMATED_FROM_ACQUISITION")
        let mastery = try #require(repository.fetchCreatureMasteries().single)
        #expect(mastery.instanceID == legacy.id)
        #expect(mastery.achievedAt == masteredAt)
        #expect(mastery.timestampConfidence == "ESTIMATED_FROM_ACQUISITION")
        #expect(try repository.fetchPendingMasteryCelebration() == nil)
        #expect(try repository.fetchShellNotifications().isEmpty)
        #expect(try repository.fetchAchievementDashboard().badges
            .filter { $0.badgeID == "creature_first_discovery" || $0.badgeID == "creature_first_mastery" }
            .allSatisfy { !$0.isNew })
    }

    @Test func v4StoreLightweightMigratesToCreatureEvidenceSchema() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("scyra-shell-v5-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Scyra.store")
        do {
            let schema = Schema(versionedSchema: ScyraSchemaV4.self)
            let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
            let container = try ModelContainer(for: schema, configurations: [configuration])
            container.mainContext.insert(StillwaterLedgerModel(entry: .init(
                id: "legacy", units: 150, sourceType: "session", sourceID: "flow", createdAt: .now
            )))
            try container.mainContext.save()
        }
        let schema = Schema(versionedSchema: ScyraSchemaV5.self)
        let configuration = ModelConfiguration("Scyra", schema: schema, url: storeURL)
        let upgraded = try ModelContainer(for: schema, migrationPlan: ScyraMigrationPlan.self, configurations: [configuration])
        let repository = SwiftDataFlowRepository(container: upgraded)
        #expect(try repository.fetchStillwaterBalance() == 150)
        #expect(try repository.fetchStillwaterPerspective() == .overview)
        #expect(try repository.fetchCreatureDiscoveries().isEmpty)
    }
}

private extension Array {
    var single: Element? { count == 1 ? first : nil }
}
