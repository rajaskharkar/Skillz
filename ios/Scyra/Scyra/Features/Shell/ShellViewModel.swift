import Combine
import Foundation

enum PendingShellDestination: Equatable {
    case chest(instanceID: String?, speciesID: String?)
    case badges(badgeID: String?, collectionID: String?, speciesID: String?)
    case blue(collectionID: String, speciesID: String?, opensBeyondBlue: Bool)
    case stillwater(collectionID: String, speciesID: String?)
}

struct ShellNavigationDispatch: Equatable {
    let room: ShellRoomRoute
    let pending: PendingShellDestination
}

enum ShellNavigationCoordinator {
    static func destination(for notification: ShellNotificationItem) -> PendingShellDestination {
        switch notification.kind {
        case .find: .chest(instanceID: notification.sourceID, speciesID: nil)
        case .badge: .badges(badgeID: notification.sourceID, collectionID: nil, speciesID: nil)
        }
    }

    static func dispatch(_ notification: ShellNotificationItem) -> ShellNavigationDispatch {
        let destination = destination(for: notification)
        let room: ShellRoomRoute = switch notification.kind {
        case .find: .chest
        case .badge: .badges
        }
        return .init(room: room, pending: destination)
    }

    static func dispatch(_ action: AchievementActionDestination) -> ShellNavigationDispatch? {
        switch action {
        case .flow, .arc:
            nil
        case .badgeDetails(let badgeID):
            .init(room: .badges, pending: .badges(badgeID: badgeID, collectionID: nil, speciesID: nil))
        case .collectionDetails(let collectionID, let speciesID):
            .init(room: .badges, pending: .badges(badgeID: nil, collectionID: collectionID, speciesID: speciesID))
        case .chestSpecies(let speciesID):
            .init(room: .chest, pending: .chest(instanceID: nil, speciesID: speciesID))
        case .blueRegion(let collectionID, let speciesID):
            .init(room: .theBlue, pending: .blue(collectionID: collectionID, speciesID: speciesID, opensBeyondBlue: false))
        case .stillwaterVessel(let collectionID, let speciesID):
            .init(room: .stillwater, pending: .stillwater(collectionID: collectionID, speciesID: speciesID))
        case .beyondBlue(let collectionID, let speciesID):
            .init(room: .theBlue, pending: .blue(collectionID: collectionID, speciesID: speciesID, opensBeyondBlue: true))
        }
    }
}

@MainActor
final class ShellViewModel: ObservableObject {
    @Published private(set) var pearlBalance = 0
    @Published private(set) var stillwaterDrops: Int64 = 0
    @Published private(set) var stillwaterLifetimeDrops: Int64 = 0
    @Published private(set) var stillwaterPerspective: StillwaterPerspective = .overview
    @Published private(set) var unlockedCreatureZones: Set<ShellDepthTier> = [.sunlitReef]
    @Published private(set) var stillwaterProgress: [StillwaterCollectionProgress] = []
    @Published private(set) var creatureCollectionProgress: [StillwaterCollectionProgress] = []
    @Published private(set) var creatureDiscoveries: [CreatureDiscoveryEvidence] = []
    @Published private(set) var creatureMasteries: [CreatureMasteryEvidence] = []
    @Published private(set) var lastEncounteredCreature: CreatureDefinition?
    @Published private(set) var pendingStillwaterConfirmation: StillwaterVessel?
    @Published private(set) var stillwaterReveal: StillwaterDrawResult?
    @Published private(set) var pendingMasteryCelebration: MasteryCelebration?
    @Published private(set) var trackedBadgeIDs: Set<String> = []
    @Published private(set) var instances: [ShellFindInstance] = []
    @Published private(set) var quantityStacks: [ShellFindStack] = []
    @Published private(set) var placements: [ShellPlacement] = []
    @Published private(set) var achievementDashboard = AchievementDashboard(badges: [], pinned: [], newCount: 0)
    @Published private(set) var notifications: [ShellNotificationItem] = []
    @Published private(set) var pendingDestination: PendingShellDestination?
    @Published private(set) var pinReplacement: (requested: String, current: [String])?
    @Published private(set) var errorMessage: String?
    @Published var chestSort: ChestSortOption = .level
    @Published var chestFilter: ChestFilterOption = .all
    @Published var achievementCategory: AchievementCategory = .all
    @Published var achievementSort: AchievementSort = .recommended

    private let repository: any ScyraRepository
    private var pendingNotificationID: String?

    init(repository: any ScyraRepository) {
        self.repository = repository
        refresh()
    }

    var chestStacks: [ChestInventoryStack] {
        chestStacks(filter: chestFilter)
    }

    var allChestStacks: [ChestInventoryStack] {
        chestStacks(filter: .all)
    }

    private func chestStacks(filter: ChestFilterOption) -> [ChestInventoryStack] {
        let placedInstanceIDs = Set(placements.map(\.instanceID))
        let activeChestCreatures = instances.filter {
            $0.isArchivedInChest &&
                !placedInstanceIDs.contains($0.id) &&
                CreatureCatalog.definition($0.findID) != nil
        }
        let mastery = Dictionary(uniqueKeysWithValues: achievementDashboard.badges.compactMap { badge in
            badge.badgeID.hasPrefix("mastery_species_")
                ? (String(badge.badgeID.dropFirst("mastery_species_".count)), badge.count)
                : nil
        })
        return ShellInventoryMapper.stacks(
            instances: activeChestCreatures,
            quantityStacks: [],
            masteryCounts: mastery,
            trackedSpeciesIDs: trackedSpeciesIDs,
            sort: chestSort,
            filter: filter
        )
    }

    var visibleAchievements: [AchievementProgress] {
        let filtered = achievementDashboard.badges.filter {
            achievementCategory == .all || $0.category == achievementCategory
        }
        return AchievementDashboardCalculator.sorted(filtered, by: achievementSort)
    }

    var allCollectionProgress: [StillwaterCollectionProgress] {
        var seen = Set<String>()
        return (creatureCollectionProgress + stillwaterProgress).filter { seen.insert($0.id).inserted }
    }

    var trackedSpeciesIDs: Set<String> {
        trackedBadgeIDs.reduce(into: Set<String>()) { result, badgeID in
            if badgeID.hasPrefix("mastery_species_") {
                result.insert(String(badgeID.dropFirst("mastery_species_".count)))
            } else if badgeID.hasPrefix("stillwater_") {
                result.formUnion(CreatureCatalog.stillwaterCreatures.map(\.id))
            } else if let collectionID = CollectionRosterCatalog.allCollectionIDs.first(where: { badgeID.hasPrefix("\($0)_") }) {
                result.formUnion(CollectionRosterCatalog.roster(for: collectionID))
            }
        }
    }

    func preferredInstanceID(for stack: ChestInventoryStack) -> String? {
        let placed = Set(placements.map(\.instanceID))
        return instances.filter { stack.instanceIDs.contains($0.id) }
            .sorted {
                if placed.contains($0.id) != placed.contains($1.id) { return !placed.contains($0.id) }
                if $0.acquiredAt != $1.acquiredAt { return $0.acquiredAt < $1.acquiredAt }
                return $0.id < $1.id
            }.first?.id
    }

    func refresh() {
        do {
            _ = try repository.reconcileShellCollections(version: 2, at: Date())
            pearlBalance = try repository.fetchPearlBalance()
            stillwaterDrops = try repository.fetchStillwaterBalance()
            stillwaterLifetimeDrops = try repository.fetchStillwaterLifetimeTotal()
            stillwaterPerspective = try repository.fetchStillwaterPerspective()
            unlockedCreatureZones = try repository.fetchUnlockedCreatureZones()
            stillwaterProgress = try repository.fetchStillwaterCollectionProgress()
            creatureCollectionProgress = try repository.fetchCreatureCollectionProgress()
            creatureDiscoveries = try repository.fetchCreatureDiscoveries()
            creatureMasteries = try repository.fetchCreatureMasteries()
            pendingMasteryCelebration = try repository.fetchPendingMasteryCelebration()
            trackedBadgeIDs = try repository.trackedAchievementIDs()
            instances = try repository.fetchShellFindInstances()
            quantityStacks = try repository.fetchShellFindStacks()
            placements = try repository.fetchShellPlacements(roomID: "FOCUS")
            achievementDashboard = try repository.fetchAchievementDashboard()
            notifications = try repository.fetchShellNotifications()
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func invite(_ findID: String) {
        perform { _ = try repository.invitePearlObjectToChest(findID: findID, at: Date()) }
    }

    func place(instanceID: String, slotID: String) {
        perform { try repository.placeShellFind(instanceID: instanceID, roomID: "FOCUS", slotID: slotID, at: Date()) }
    }

    func returnToChest(instanceID: String) {
        perform { try repository.returnShellFindToChest(instanceID: instanceID, at: Date()) }
    }

    func upgrade(instanceID: String) {
        perform { _ = try repository.upgradeShellFind(instanceID: instanceID, at: Date()) }
    }

    func growCreature(instanceID: String) {
        perform {
            guard let instance = try repository.fetchShellFindInstances().first(where: { $0.id == instanceID }) else {
                throw CreatureShellError.missingCreature
            }
            _ = try repository.growCreature(
                instanceID: instanceID,
                transactionID: "level_up:\(instanceID):\(max(1, instance.animalLevel) + 1)",
                at: Date()
            )
        }
    }

    func releaseCreature(instanceID: String) {
        perform { _ = try repository.releaseCreature(instanceID: instanceID, at: Date()) }
    }

    @discardableResult
    func releaseCreatures(instanceIDs: [String]) -> Bool {
        do {
            _ = try repository.releaseCreatures(instanceIDs: instanceIDs, at: Date())
            refresh()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func activeCreatureInstances(for creatureID: String) -> [ShellFindInstance] {
        instances.filter { $0.findID == creatureID }.sorted {
            if $0.animalLevel != $1.animalLevel { return $0.animalLevel > $1.animalLevel }
            if $0.acquiredAt != $1.acquiredAt { return $0.acquiredAt < $1.acquiredAt }
            return $0.id < $1.id
        }
    }

    func firstOpenFocusSlot(for creatureID: String) -> ShellSlotDefinition? {
        guard let definition = ShellContentCatalog.definition(creatureID) else { return nil }
        let occupied = Set(placements.map(\.slotID))
        return ShellContentCatalog.focusSlots.first {
            !occupied.contains($0.id) && ShellContentCatalog.isCompatible(slot: $0, find: definition)
        }
    }

    func firstRestingCreatureInstance(for creatureID: String) -> ShellFindInstance? {
        let displayed = Set(placements.map(\.instanceID))
        return activeCreatureInstances(for: creatureID).first {
            $0.isArchivedInChest && !displayed.contains($0.id)
        }
    }

    func quoteBeyondBlue(targetCreatureID: String, selectedInstanceIDs: [String]) throws -> CreaturePaymentQuote {
        try repository.quoteBeyondBlueEncounter(
            targetCreatureID: targetCreatureID,
            selectedInstanceIDs: selectedInstanceIDs
        )
    }

    @discardableResult
    func encounterBeyondBlue(targetCreatureID: String, selectedInstanceIDs: [String]) -> Bool {
        do {
            let instance = try repository.encounterBeyondBlue(
                targetCreatureID: targetCreatureID,
                selectedInstanceIDs: selectedInstanceIDs,
                at: Date()
            )
            lastEncounteredCreature = CreatureCatalog.definition(instance.findID)
            refresh()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func dismissEncounterReveal() { lastEncounteredCreature = nil }

    func requestStillwaterDraw(_ vessel: StillwaterVessel) {
        do {
            try repository.saveStillwaterPerspective(.init(vessel: vessel), at: Date())
            stillwaterPerspective = .init(vessel: vessel)
            guard unlockedCreatureZones.contains(vessel.zone) else { throw StillwaterError.locked }
            guard stillwaterDrops >= vessel.dropCost else {
                throw StillwaterError.insufficientDrops(required: vessel.dropCost, available: stillwaterDrops)
            }
            if vessel.requiresConfirmation {
                pendingStillwaterConfirmation = vessel
            } else {
                try drawStillwater(vessel)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func confirmStillwaterDraw() {
        guard let vessel = pendingStillwaterConfirmation else { return }
        pendingStillwaterConfirmation = nil
        do { try drawStillwater(vessel) }
        catch { errorMessage = error.localizedDescription; refresh() }
    }

    func dismissStillwaterConfirmation() { pendingStillwaterConfirmation = nil }
    func dismissStillwaterReveal() { stillwaterReveal = nil }
    func dismissError() { errorMessage = nil }

    func selectStillwaterPerspective(_ perspective: StillwaterPerspective) {
        do {
            try repository.saveStillwaterPerspective(perspective, at: Date())
            stillwaterPerspective = perspective
        } catch { errorMessage = error.localizedDescription }
    }

    func setBadgeTracked(_ badgeID: String, tracked: Bool) {
        perform { try repository.setAchievementTracked(badgeID, tracked: tracked, at: Date()) }
    }

    func acknowledgeMasteryCelebration() {
        guard let pendingMasteryCelebration else { return }
        perform { try repository.acknowledgeMasteryCelebration(id: pendingMasteryCelebration.id, at: Date()) }
    }

    func pin(_ badgeID: String, replacing replacementID: String? = nil) {
        do {
            switch try repository.pinAchievement(badgeID: badgeID, replacing: replacementID, at: Date()) {
            case .pinned, .alreadyPinned:
                pinReplacement = nil
            case .replacementRequired(let current):
                pinReplacement = (badgeID, current)
            }
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func unpin(_ badgeID: String) {
        perform { try repository.unpinAchievement(badgeID: badgeID) }
    }

    func dismissPinReplacement() { pinReplacement = nil }

    @discardableResult
    func openNotification(_ notification: ShellNotificationItem) -> ShellRoomRoute {
        let dispatch = ShellNavigationCoordinator.dispatch(notification)
        pendingNotificationID = notification.id
        pendingDestination = dispatch.pending
        return dispatch.room
    }

    func markAllNotificationsViewed() {
        perform { try repository.markAllShellNotificationsViewed(at: Date()) }
    }

    func consumePendingDestination(success: Bool) {
        let notificationID = pendingNotificationID
        pendingDestination = nil
        pendingNotificationID = nil
        guard success, let notificationID else { return }
        do {
            try repository.markShellNotificationViewed(id: notificationID, at: Date())
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func prepareChestFocus(instanceID: String?) {
        pendingNotificationID = nil
        pendingDestination = .chest(instanceID: instanceID, speciesID: nil)
    }

    @discardableResult
    func prepareNavigation(_ action: AchievementActionDestination) -> ShellRoomRoute? {
        pendingNotificationID = nil
        pendingDestination = nil
        guard let dispatch = ShellNavigationCoordinator.dispatch(action) else { return nil }
        pendingDestination = dispatch.pending
        return dispatch.room
    }

    func markBadgeViewed(_ badgeID: String) {
        perform { try repository.markShellNotificationViewed(id: "BADGE:\(badgeID)", at: Date()) }
    }

    func instance(id: String) -> ShellFindInstance? { instances.first { $0.id == id } }
    func placement(slotID: String) -> ShellPlacement? { placements.first { $0.slotID == slotID } }

    func isVesselUnlocked(_ vessel: StillwaterVessel) -> Bool { unlockedCreatureZones.contains(vessel.zone) }

    func progress(for collectionID: String) -> StillwaterCollectionProgress? {
        allCollectionProgress.first { $0.id == collectionID }
    }

    func achievementAction(for badge: AchievementProgress) -> AchievementActionDestination? {
        AchievementActionPolicy.destination(
            for: badge.badgeID,
            category: badge.category,
            ownedSpeciesIDs: Set(instances.map(\.findID)),
            discoveredSpeciesIDs: Set(creatureDiscoveries.map(\.speciesID))
        )
    }

    func collectionSpecies(for collectionID: String) -> [CollectionSpeciesProgress] {
        let discovered = Set(creatureDiscoveries.map(\.speciesID))
        let ownedBySpecies = Dictionary(grouping: instances, by: \.findID)
        let masteryBySpecies = Dictionary(grouping: creatureMasteries, by: \.speciesID)
        return CollectionRosterCatalog.roster(for: collectionID).compactMap { speciesID in
            guard CreatureCatalog.definition(speciesID) != nil else { return nil }
            let owned = ownedBySpecies[speciesID, default: []]
            let mastery = masteryBySpecies[speciesID, default: []]
            return CollectionSpeciesProgress(
                speciesID: speciesID,
                discovered: discovered.contains(speciesID),
                ownedCount: owned.count,
                highestLevel: owned.map(\.animalLevel).max(),
                currentLevel99Count: owned.count { $0.animalLevel >= CreatureEconomy.maxLevel },
                lifetimeMasteryCount: mastery.count,
                action: AchievementActionPolicy.speciesDestination(
                    speciesID: speciesID,
                    ownedSpeciesIDs: Set(ownedBySpecies.keys),
                    discoveredSpeciesIDs: discovered
                )
            )
        }
    }

    func ownedCreatureCount(_ creatureID: String) -> Int {
        instances.count { $0.findID == creatureID }
    }

    func isCreatureDiscovered(_ creatureID: String) -> Bool {
        creatureDiscoveries.contains { $0.speciesID == creatureID }
    }

    func masteryCount(_ creatureID: String) -> Int {
        creatureMasteries.count { $0.speciesID == creatureID }
    }

    func stillwaterDropsNeeded(for vessel: StillwaterVessel) -> Int64 {
        max(0, vessel.dropCost - stillwaterDrops)
    }

    func stillwaterProgressValue(for vessel: StillwaterVessel) -> Double {
        min(1, max(0, Double(stillwaterDrops) / Double(vessel.dropCost)))
    }

    private func drawStillwater(_ vessel: StillwaterVessel) throws {
        stillwaterReveal = try repository.drawFromStillwater(
            vessel: vessel,
            rarityRoll: Int.random(in: 0..<100),
            selectionRoll: Int.random(in: 0..<Int.max),
            at: Date()
        )
        refresh()
    }

    private func perform(_ action: () throws -> Void) {
        do {
            try action()
            refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
