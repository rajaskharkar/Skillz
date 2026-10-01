import Foundation
import SwiftData
import Testing
@testable import Scyra

@MainActor
struct LandExplorationParityTests {
    @Test func realmCatalogsAndChildRoutesMatchAndroidWithoutInventedBrowsingLocks() throws {
        #expect(BlueRealmPolicy.zones(in: .land) == [.goldenFields, .ancientWoods, .openSands, .highPeaks, .greatWild])
        #expect(BlueRealmPolicy.zones(in: .sea).count == 4)
        let land = ShellDepthTier.landZones.flatMap { BlueRealmPolicy.encounters(in: $0) }
        #expect(land.count == 63)
        #expect(land.filter { $0.sourceType == .beyondBlue }.count == 58)
        #expect(land.filter { $0.sourceType == .arcEarned }.count == 5)
        #expect(!land.contains { $0.sourceType == .restorativeLand })
        #expect(BlueRealmPolicy.encounters(in: .goldenFields).first?.id == "creature_chicken")
        #expect(BlueRealmPolicy.encounters(in: .greatWild).last?.id == "creature_lion")
        for zone in ShellDepthTier.allCases {
            #expect(BlueRealmPolicy.zone(collectionID: BlueRealmPolicy.collectionID(for: zone)) == zone)
        }
        #expect(BlueRealmPolicy.collectionID(for: .goldenFields) == "blue_golden_fields")
        #expect(BlueRealmPolicy.zone(collectionID: "stillwater_pasture") == nil)
        let action = try #require(AchievementActionPolicy.speciesDestination(speciesID: "creature_lion", ownedSpeciesIDs: [], discoveredSpeciesIDs: []))
        #expect(action == .beyondBlue(collectionID: "blue_great_wild", speciesID: "creature_lion"))
        #expect(ShellNavigationCoordinator.dispatch(action)?.room == .theBlue)
        #expect(AchievementActionPolicy.speciesDestination(speciesID: "creature_chicken", ownedSpeciesIDs: [], discoveredSpeciesIDs: []) == .arc)
    }

    @Test func realmBackNavigationReturnsToChooserBeforeLeavingTheBlue() {
        let shell = ShellViewModel(repository: InMemoryFlowRepository())
        #expect(shell.blueRealm == nil)
        #expect(!shell.returnToBlueRealmSelector())
        shell.selectBlueRealm(.land)
        #expect(shell.blueRealm == .land)
        #expect(shell.returnToBlueRealmSelector())
        #expect(shell.blueRealm == nil)
        shell.selectBlueRealm(.sea)
        #expect(shell.returnToBlueRealmSelector())
        #expect(!shell.returnToBlueRealmSelector())
    }

    @Test func tradeStacksCrossRealmsPreserveLevelsAndSelectOldestCopies() throws {
        let values = [instance("new", "creature_chicken", time: 20), instance("old", "creature_chicken", time: 10),
                      instance("mastered", "creature_chicken", level: 99), instance("sea", "focus_minnow"),
                      instance("not-animal", "not-a-species")]
        let stacks = BeyondBlueTradeStack.make(from: values)
        #expect(stacks.count == 3)
        let chicken = try #require(stacks.first { $0.creature.id == "creature_chicken" && $0.level == 1 })
        #expect(chicken.instances.map(\.id) == ["old", "new"])
        #expect(chicken.valueMinutes == 15)
        #expect(stacks.first { $0.level == 99 }?.valueMinutes == 15)
        #expect(BeyondBlueTradeStack.selectedIDs(stacks: stacks, counts: [chicken.id: 1]) == ["old"])
        #expect(BeyondBlueTradeStack.selectedIDs(stacks: stacks, counts: [chicken.id: 999]) == ["old", "new"])
        #expect(BeyondBlueTradeStack.selectedIDs(stacks: stacks, counts: [chicken.id: -1, "stale": 5]).isEmpty)
    }

    @Test(arguments: [false, true])
    func landPurchaseCrossRealmTradeAndFailureAreAtomic(swiftData: Bool) throws {
        let date = Date(timeIntervalSince1970: 100_000)
        let whale = instance("whale", "focus_whale")
        let tiger = instance("tiger", "creature_tiger", level: 99)
        let repository = try repository(swiftData: swiftData, pearls: 1_000, instances: [whale, tiger])
        try repository.placeShellFind(instanceID: whale.id, roomID: "FOCUS", slotID: "center_focus_nook", at: date)
        let quote = try repository.quoteBeyondBlueEncounter(targetCreatureID: "creature_duck", selectedInstanceIDs: [whale.id])
        #expect(quote.pearlCostForRemaining == 0)
        #expect(quote.selectedCreatureMinutes == 120)
        let duck = try repository.encounterBeyondBlue(targetCreatureID: "creature_duck", selectedInstanceIDs: [whale.id], at: date)
        #expect(try repository.creatureStatus(instanceID: whale.id) == .usedBeyondBlue)
        #expect(try repository.fetchShellPlacements(roomID: "FOCUS").isEmpty)
        #expect(try repository.fetchPearlBalance() == 1_000)
        #expect(duck.animalLevel == 1 && duck.isNew)
        #expect(try repository.fetchCreatureDiscoveries().contains { $0.speciesID == "creature_duck" })
        // A consumed selection cannot be replayed; no second grant or debit.
        #expect(throws: CreatureShellError.self) {
            _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_duck", selectedInstanceIDs: [whale.id], at: date)
        }
        #expect(try repository.fetchShellFindInstances().filter { $0.findID == "creature_duck" }.count == 1)
        // Land creatures can pay for Sea creatures. Level does not inflate value.
        let seaQuote = try repository.quoteBeyondBlueEncounter(targetCreatureID: "creature_clownfish", selectedInstanceIDs: [tiger.id])
        #expect(seaQuote.selectedCreatureMinutes == 360)
        #expect(seaQuote.pearlReturnForOverpay == 330)
        _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_clownfish", selectedInstanceIDs: [tiger.id], at: date)
        #expect(try repository.fetchPearlBalance() == 1_330)
        _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_turkey", selectedInstanceIDs: [], at: date)
        #expect(try repository.fetchPearlBalance() == 1_030)
        let history = try repository.fetchCreatureLifetimeCounts()
        #expect(history[whale.findID] == .init(encountered: 1, released: 0, traded: 1))
        #expect(history[tiger.findID] == .init(encountered: 1, released: 0, traded: 1))
        #expect(history[duck.findID] == .init(encountered: 1, released: 0, traded: 0))
        #expect(throws: CreatureShellError.insufficientPearls(required: 5_760, available: 1_030)) {
            _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_lion", selectedInstanceIDs: [duck.id], at: date)
        }
        #expect(try repository.creatureStatus(instanceID: duck.id) == .active)
        #expect(try repository.fetchPearlBalance() == 1_030)
        _ = try repository.releaseCreature(instanceID: duck.id, at: date)
        #expect(try repository.fetchCreatureLifetimeCounts()[duck.findID] == .init(encountered: 1, released: 1, traded: 0))
        #expect(throws: CreatureShellError.self) {
            _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_lion", selectedInstanceIDs: [duck.id], at: date)
        }
        #expect(try repository.creatureStatus(instanceID: duck.id) == .released)
        #expect(try repository.fetchPearlBalance() == 1_270)
        #expect(throws: CreatureShellError.self) {
            _ = try repository.encounterBeyondBlue(targetCreatureID: "creature_chicken", selectedInstanceIDs: [], at: date)
        }
        #expect(try repository.fetchPearlBalance() == 1_270)
    }

    @Test func shellRefreshKeepsRealmSelectionAndExposesEncounteredLandCreature() throws {
        let repository = try repository(swiftData: true, pearls: 300, instances: [])
        let shell = ShellViewModel(repository: repository)
        shell.selectBlueRealm(.land)
        #expect(shell.encounterBeyondBlue(targetCreatureID: "creature_duck", selectedInstanceIDs: []))
        #expect(shell.blueRealm == .land)
        #expect(shell.pearlBalance == 60)
        #expect(shell.lastEncounteredCreature?.id == "creature_duck")
        shell.chestFilter = .land
        #expect(shell.chestStacks.map(\.findID) == ["creature_duck"])
        #expect(!shell.encounterBeyondBlue(targetCreatureID: "creature_lion", selectedInstanceIDs: []))
        #expect(shell.errorMessage != nil)
        #expect(shell.instances.count == 1)
    }

    private func repository(swiftData: Bool, pearls: Int, instances: [ShellFindInstance]) throws -> any ScyraRepository {
        let ledger = PearlLedgerEntry(id: "seed", delta: pearls, reason: "test", sourceType: "test", sourceID: nil, createdAt: .now, note: nil)
        if !swiftData {
            let repository = InMemoryFlowRepository()
            repository.pearlLedger = [ledger]
            repository.shellFindInstances = instances
            return repository
        }
        let container = try ScyraPersistenceFactory.makeContainer(inMemory: true)
        container.mainContext.insert(PearlLedgerModel(entry: ledger))
        for value in instances {
            container.mainContext.insert(ShellFindInstanceModel(instance: value))
            container.mainContext.insert(CreatureLifecycleModel(instanceID: value.id, status: .active, updatedAt: .now))
        }
        try container.mainContext.save()
        return SwiftDataFlowRepository(container: container)
    }

    private func instance(_ id: String, _ species: String, level: Int = 1, time: Double = 0) -> ShellFindInstance {
        .init(id: id, findID: species, acquiredAt: Date(timeIntervalSince1970: time), sourceType: "test", sourceID: nil,
              currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true, viewedAt: nil,
              animalLevel: level, lastActivityAt: Date(timeIntervalSince1970: time))
    }
}
