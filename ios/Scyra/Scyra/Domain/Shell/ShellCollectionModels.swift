import Foundation

enum ShellFindCategory: String, CaseIterable, Codable, Sendable {
    case creatures, shells, coral, plants, trophies, trinkets, discoveries
}

enum ShellRewardKind: String, Codable, Sendable {
    case animal, object, trinket, discovery
}

enum ShellDepthTier: String, CaseIterable, Codable, Sendable {
    case sunlitReef, deeperReef, openBlue, greatBlue

    var title: String {
        switch self {
        case .sunlitReef: "Sunlit Reef"
        case .deeperReef: "Deeper Reef"
        case .openBlue: "Open Blue"
        case .greatBlue: "Great Blue"
        }
    }
}

enum ShellSlotType: String, Codable, Sendable {
    case reefShelf, shellWall, creaturePerch, coralBed, tidepoolEdge
    case currentPath, surgeCurrent, centerpiece, memoryNook
}

struct ShellFindDefinition: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let description: String
    let category: ShellFindCategory
    let kind: ShellRewardKind
    let systemImage: String
    let placeable: Bool
    let upgradeable: Bool
    let stackable: Bool
    let acceptedSlotTypes: Set<ShellSlotType>
    let pearlCost: Int?
    let isPearlObject: Bool
    let depth: ShellDepthTier?
}

struct ShellSlotDefinition: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let type: ShellSlotType
    let acceptedCategories: Set<ShellFindCategory>
}

struct ShellFindUpgradeDefinition: Identifiable, Equatable, Sendable {
    let id: String
    let findID: String
    let order: Int
    let title: String
    let pearlCost: Int
    let verb: String
}

enum ShellContentCatalog {
    static let focusOctopus = "focus_octopus"
    static let focusPebble = "focus_pebble"
    static let seaGlass = "trinket_sea_glass_shard"
    static let glimmer = "trinket_glimmer"
    static let focusLamp = "focus_lamp"
    static let focusPerch = "focus_perch"
    static let focusPebbles = "focus_pebbles"
    static let focusCurtain = "focus_curtain"
    static let focusBubbles = "focus_bubbles"

    private static let animalSlots: Set<ShellSlotType> = [.creaturePerch, .tidepoolEdge]

    private static let legacyFinds: [ShellFindDefinition] = [
        .init(id: ShellRewardCatalog.focusMinnow, title: "Minnow", description: "Encountered in the Sunlit Reef after a regular Flow lasting 10 minutes or more.", category: .creatures, kind: .animal, systemImage: "fish.fill", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: animalSlots, pearlCost: nil, isPearlObject: false, depth: .sunlitReef),
        .init(id: ShellRewardCatalog.focusSeahorse, title: "Seahorse", description: "Encountered in the Deeper Reef after a regular Flow lasting 30 minutes or more.", category: .creatures, kind: .animal, systemImage: "fish.fill", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: animalSlots, pearlCost: nil, isPearlObject: false, depth: .deeperReef),
        .init(id: ShellRewardCatalog.focusManta, title: "Manta", description: "Encountered in the Open Blue after a regular Flow lasting 60 minutes or more.", category: .creatures, kind: .animal, systemImage: "water.waves", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: [.currentPath, .centerpiece, .tidepoolEdge], pearlCost: nil, isPearlObject: false, depth: .openBlue),
        .init(id: ShellRewardCatalog.focusWhale, title: "Whale", description: "Encountered in the Great Blue after a regular Flow lasting 2 hours or more.", category: .creatures, kind: .animal, systemImage: "water.waves", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: [.currentPath, .centerpiece], pearlCost: nil, isPearlObject: false, depth: .greatBlue),
        .init(id: focusOctopus, title: "Octopus", description: "Encountered beyond The Blue and now hiding in the Deeper Reef.", category: .creatures, kind: .animal, systemImage: "aqi.medium", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: [.creaturePerch, .memoryNook], pearlCost: nil, isPearlObject: false, depth: .deeperReef),
        .init(id: focusPebble, title: "Pebble", description: "Found after you returned to regular Flow.", category: .coral, kind: .object, systemImage: "circle.fill", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: [.memoryNook, .reefShelf], pearlCost: nil, isPearlObject: false, depth: nil),
        .init(id: seaGlass, title: "Seaglass", description: "Found through regular Flow activity.", category: .trinkets, kind: .trinket, systemImage: "diamond.fill", placeable: false, upgradeable: false, stackable: true, acceptedSlotTypes: [], pearlCost: nil, isPearlObject: false, depth: nil),
        .init(id: glimmer, title: "Glimmers", description: "Found through regular Flow activity.", category: .trinkets, kind: .trinket, systemImage: "sparkles", placeable: false, upgradeable: false, stackable: true, acceptedSlotTypes: [], pearlCost: nil, isPearlObject: false, depth: nil),
        .init(id: focusLamp, title: "Glow Coral", description: "Creature · Light. Invited with Pearls to brighten a quiet nook.", category: .coral, kind: .object, systemImage: "lamp.table.fill", placeable: true, upgradeable: true, stackable: false, acceptedSlotTypes: [.shellWall, .coralBed, .centerpiece], pearlCost: 80, isPearlObject: true, depth: nil),
        .init(id: focusPerch, title: "Perch", description: "Invited with Pearls as a resting object.", category: .creatures, kind: .object, systemImage: "tree.fill", placeable: true, upgradeable: false, stackable: false, acceptedSlotTypes: [.creaturePerch, .memoryNook], pearlCost: 120, isPearlObject: true, depth: nil),
        .init(id: focusPebbles, title: "Pebbles", description: "Invited with Pearls for a reef shelf.", category: .coral, kind: .object, systemImage: "circle.grid.3x3.fill", placeable: true, upgradeable: false, stackable: false, acceptedSlotTypes: [.reefShelf, .memoryNook], pearlCost: 60, isPearlObject: true, depth: nil),
        .init(id: focusCurtain, title: "Curtain", description: "Invited with Pearls for the edge of the room.", category: .plants, kind: .object, systemImage: "leaf.fill", placeable: true, upgradeable: false, stackable: false, acceptedSlotTypes: [.coralBed, .shellWall], pearlCost: 140, isPearlObject: true, depth: nil),
        .init(id: focusBubbles, title: "Bubbles", description: "Invited with Pearls for a current-like nook.", category: .coral, kind: .object, systemImage: "circle.dotted", placeable: true, upgradeable: false, stackable: false, acceptedSlotTypes: [.currentPath, .centerpiece, .memoryNook], pearlCost: 100, isPearlObject: true, depth: nil)
    ]

    /// Android's complete CreatureCatalog is also the authoritative Chest catalog.
    /// Keep the pre-existing definitions where they contain richer placement rules,
    /// then add every remaining Flow, Beyond Blue, and Stillwater creature.
    static let finds: [ShellFindDefinition] = {
        let legacyIDs = Set(legacyFinds.map(\.id))
        let creatures = CreatureCatalog.all.compactMap { creature -> ShellFindDefinition? in
            guard !legacyIDs.contains(creature.id) else { return nil }
            let acceptedSlots: Set<ShellSlotType> = switch creature.zone {
            case .sunlitReef, .deeperReef: [.creaturePerch, .tidepoolEdge]
            case .openBlue: [.currentPath, .centerpiece, .tidepoolEdge]
            case .greatBlue: [.currentPath, .centerpiece]
            }
            let source = switch creature.sourceType {
            case .flowEarned: "Encountered through regular Flow."
            case .beyondBlue: "Encountered beyond The Blue."
            case .stillwater: "Drawn from a quiet vessel in Stillwater."
            }
            return .init(
                id: creature.id,
                title: creature.displayName,
                description: "\(source) \(creature.zone.title).",
                category: .creatures,
                kind: .animal,
                systemImage: creature.systemImage,
                placeable: true,
                upgradeable: true,
                stackable: false,
                acceptedSlotTypes: acceptedSlots,
                pearlCost: nil,
                isPearlObject: false,
                depth: creature.zone
            )
        }
        return legacyFinds + creatures
    }()

    static let focusSlots: [ShellSlotDefinition] = [
        .init(id: "left_reef_shelf", title: "Left Reef Shelf", type: .reefShelf, acceptedCategories: [.shells, .trinkets, .coral]),
        .init(id: "right_reef_shelf", title: "Right Reef Shelf", type: .reefShelf, acceptedCategories: [.shells, .trinkets, .coral]),
        .init(id: "shell_wall_nook", title: "Shell Wall Nook", type: .shellWall, acceptedCategories: [.shells, .coral, .plants]),
        .init(id: "coral_bed", title: "Coral Bed", type: .coralBed, acceptedCategories: [.coral, .plants]),
        .init(id: "creature_perch_left", title: "Left Creature Perch", type: .creaturePerch, acceptedCategories: [.creatures]),
        .init(id: "creature_perch_right", title: "Right Creature Perch", type: .creaturePerch, acceptedCategories: [.creatures]),
        .init(id: "center_focus_nook", title: "Center Focus Nook", type: .centerpiece, acceptedCategories: [.shells, .coral, .creatures, .trophies, .trinkets]),
        .init(id: "surge_current_nook", title: "Surge Current Nook", type: .surgeCurrent, acceptedCategories: [.trophies]),
        .init(id: "memory_nook", title: "Memory Nook", type: .memoryNook, acceptedCategories: [.shells, .trinkets, .discoveries, .creatures])
    ]

    static let upgrades: [ShellFindUpgradeDefinition] = [
        .init(id: "focus_pebble_base", findID: focusPebble, order: 0, title: "Base", pearlCost: 0, verb: "Polish"),
        .init(id: "focus_pebble_polished", findID: focusPebble, order: 1, title: "Polished", pearlCost: 60, verb: "Polish"),
        .init(id: "focus_pebble_tidemarked", findID: focusPebble, order: 2, title: "Tidemarked", pearlCost: 140, verb: "Polish"),
        .init(id: "focus_pebble_moonlit", findID: focusPebble, order: 3, title: "Moonlit", pearlCost: 260, verb: "Awaken"),
        .init(id: "focus_lamp_base", findID: focusLamp, order: 0, title: "Base", pearlCost: 0, verb: "Brighten"),
        .init(id: "focus_lamp_bright", findID: focusLamp, order: 1, title: "Bright", pearlCost: 80, verb: "Brighten"),
        .init(id: "focus_lamp_moonlit", findID: focusLamp, order: 2, title: "Moonlit", pearlCost: 160, verb: "Awaken")
    ]

    static let byID = Dictionary(uniqueKeysWithValues: finds.map { ($0.id, $0) })
    static let pearlObjects = finds.filter(\.isPearlObject)

    static func definition(_ id: String) -> ShellFindDefinition? { byID[id] }
    static func upgrades(for findID: String) -> [ShellFindUpgradeDefinition] {
        upgrades.filter { $0.findID == findID }.sorted { $0.order < $1.order }
    }
    static func nextUpgrade(findID: String, currentStageID: String?) -> ShellFindUpgradeDefinition? {
        let stages = upgrades(for: findID)
        let index = stages.firstIndex { $0.id == currentStageID } ?? 0
        return stages.indices.contains(index + 1) ? stages[index + 1] : nil
    }
    static func isCompatible(slot: ShellSlotDefinition, find: ShellFindDefinition) -> Bool {
        guard slot.type != .surgeCurrent,
              find.acceptedSlotTypes.contains(slot.type),
              slot.acceptedCategories.contains(find.category) else { return false }
        switch slot.type {
        case .creaturePerch: return find.kind == .animal || find.id == focusPerch
        case .currentPath: return find.kind == .animal || find.id == focusBubbles
        case .tidepoolEdge: return find.kind == .animal
        case .memoryNook: return [.object, .trinket, .discovery].contains(find.kind) || find.id == focusOctopus
        case .reefShelf: return [.object, .trinket, .discovery].contains(find.kind)
        case .shellWall, .coralBed: return find.kind == .object
        case .centerpiece: return [.animal, .object].contains(find.kind)
        case .surgeCurrent: return false
        }
    }
}

struct ShellFindInstance: Identifiable, Equatable, Sendable {
    let id: String
    let findID: String
    let acquiredAt: Date
    let sourceType: String
    let sourceID: String?
    var currentUpgradeStageID: String?
    var isNew: Bool
    var isArchivedInChest: Bool
    var viewedAt: Date?
    var animalLevel: Int
    var lastActivityAt: Date
}

struct ShellFindStack: Identifiable, Equatable, Sendable {
    var id: String { findID }
    let findID: String
    var quantity: Int
    let firstAcquiredAt: Date
    var lastAcquiredAt: Date
    var isNew: Bool
    var viewedAt: Date?
}

struct ShellPlacement: Identifiable, Equatable, Sendable {
    let id: String
    let roomID: String
    let slotID: String
    let instanceID: String
    let placedAt: Date
}

struct ShellFindUpgrade: Identifiable, Equatable, Sendable {
    let id: String
    let instanceID: String
    let fromStageID: String?
    let toStageID: String
    let pearlCost: Int
    let upgradedAt: Date
}

enum ChestSortOption: String, CaseIterable, Sendable {
    case level, recent, newestArrival, oldestArrival, alphabetical, value, count, closestToMastery, speciesMasteryCount

    var title: String {
        switch self {
        case .level: "Level"
        case .recent: "Recent"
        case .newestArrival: "Newest arrival"
        case .oldestArrival: "Oldest arrival"
        case .alphabetical: "Alphabetical"
        case .value: "Value"
        case .count: "Count"
        case .closestToMastery: "Closest to mastery"
        case .speciesMasteryCount: "Species mastery count"
        }
    }
}

enum ChestFilterOption: String, CaseIterable, Sendable {
    case all, creatures, objects, trinkets, closestToMastery, mastered, notMastered
    case neededForTrackedBadges
    case sunlitReef, deeperReef, openBlue, greatBlue
    case fishbowl, aquarium, pond, lake

    var title: String {
        switch self {
        case .all: "All"
        case .creatures: "Creatures"
        case .objects: "Objects"
        case .trinkets: "Trinkets"
        case .closestToMastery: "Closest to mastery"
        case .mastered: "Mastered"
        case .notMastered: "Not mastered"
        case .neededForTrackedBadges: "Needed for tracked badges"
        case .sunlitReef: "Sunlit Reef"
        case .deeperReef: "Deeper Reef"
        case .openBlue: "Open Blue"
        case .greatBlue: "Great Blue"
        case .fishbowl: "Fishbowl"
        case .aquarium: "Aquarium"
        case .pond: "Pond"
        case .lake: "Lake"
        }
    }
}

struct ChestInventoryStack: Identifiable, Equatable, Sendable {
    var id: String { "\(findID)-\(level)" }
    let findID: String
    let title: String
    let level: Int
    let count: Int
    let instanceIDs: [String]
    let category: ShellFindCategory
    let kind: ShellRewardKind
    let depth: ShellDepthTier?
    let systemImage: String
    let newestAcquiredAt: Date
    let oldestAcquiredAt: Date
    let recentActivityAt: Date
    let totalValuePearls: Int
    let speciesMasteryCount: Int
    let isNew: Bool
}

enum ShellInventoryMapper {
    static func stacks(
        instances: [ShellFindInstance],
        quantityStacks: [ShellFindStack],
        masteryCounts: [String: Int] = [:],
        trackedSpeciesIDs: Set<String> = [],
        sort: ChestSortOption = .level,
        filter: ChestFilterOption = .all
    ) -> [ChestInventoryStack] {
        var result = Dictionary(grouping: instances, by: { "\($0.findID)-\($0.animalLevel)" }).compactMap { _, values -> ChestInventoryStack? in
            guard let first = values.first, let definition = ShellContentCatalog.definition(first.findID) else { return nil }
            return ChestInventoryStack(
                findID: first.findID,
                title: definition.title,
                level: max(1, first.animalLevel),
                count: values.count,
                instanceIDs: values.map(\.id).sorted(),
                category: definition.category,
                kind: definition.kind,
                depth: definition.depth,
                systemImage: definition.systemImage,
                newestAcquiredAt: values.map(\.acquiredAt).max() ?? first.acquiredAt,
                oldestAcquiredAt: values.map(\.acquiredAt).min() ?? first.acquiredAt,
                recentActivityAt: values.map { max($0.acquiredAt, $0.lastActivityAt) }.max() ?? first.acquiredAt,
                totalValuePearls: releaseValue(findID: first.findID, level: first.animalLevel) * values.count,
                speciesMasteryCount: masteryCounts[first.findID, default: 0],
                isNew: values.contains(where: \.isNew)
            )
        }
        result += quantityStacks.compactMap { stack in
            guard stack.quantity > 0, let definition = ShellContentCatalog.definition(stack.findID) else { return nil }
            return ChestInventoryStack(
                findID: stack.findID, title: definition.title, level: 0, count: stack.quantity,
                instanceIDs: [], category: definition.category, kind: definition.kind, depth: definition.depth,
                systemImage: definition.systemImage, newestAcquiredAt: stack.lastAcquiredAt,
                oldestAcquiredAt: stack.firstAcquiredAt, recentActivityAt: stack.viewedAt ?? stack.lastAcquiredAt,
                totalValuePearls: 0, speciesMasteryCount: masteryCounts[stack.findID, default: 0], isNew: stack.isNew
            )
        }
        return result.filter { matches($0, filter: filter, trackedSpeciesIDs: trackedSpeciesIDs) }.sorted(by: comparator(sort))
    }

    static func releaseValue(findID: String, level: Int) -> Int {
        if CreatureCatalog.definition(findID) != nil {
            return CreatureEconomy.releaseValuePearls(findID, level: level)
        }
        let base: Int = switch findID {
        case ShellRewardCatalog.focusWhale: 90
        case ShellRewardCatalog.focusManta: 45
        case ShellRewardCatalog.focusSeahorse: 20
        default: 10
        }
        return base + max(0, level - 1) * 2
    }

    private static func matches(_ value: ChestInventoryStack, filter: ChestFilterOption, trackedSpeciesIDs: Set<String>) -> Bool {
        switch filter {
        case .all: true
        case .creatures: value.kind == .animal
        case .objects: value.kind == .object
        case .trinkets: value.kind == .trinket
        case .closestToMastery: value.kind == .animal && value.level >= 90
        case .mastered: value.kind == .animal && value.level >= 99
        case .notMastered: value.kind == .animal && value.level < 99
        case .neededForTrackedBadges: value.kind == .animal && trackedSpeciesIDs.contains(value.findID)
        case .sunlitReef: value.depth == .sunlitReef
        case .deeperReef: value.depth == .deeperReef
        case .openBlue: value.depth == .openBlue
        case .greatBlue: value.depth == .greatBlue
        case .fishbowl: StillwaterCatalog.byID[value.findID]?.vessel == .fishbowl
        case .aquarium: StillwaterCatalog.byID[value.findID]?.vessel == .aquarium
        case .pond: StillwaterCatalog.byID[value.findID]?.vessel == .pond
        case .lake: StillwaterCatalog.byID[value.findID]?.vessel == .lake
        }
    }

    private static func comparator(_ option: ChestSortOption) -> (ChestInventoryStack, ChestInventoryStack) -> Bool {
        { left, right in
            let primary: ComparisonResult = switch option {
            case .level: compare(right.level, left.level)
            case .recent: compare(right.recentActivityAt, left.recentActivityAt)
            case .newestArrival: compare(right.newestAcquiredAt, left.newestAcquiredAt)
            case .oldestArrival: compare(left.oldestAcquiredAt, right.oldestAcquiredAt)
            case .alphabetical: left.title.localizedCaseInsensitiveCompare(right.title)
            case .value: compare(right.totalValuePearls, left.totalValuePearls)
            case .count: compare(right.count, left.count)
            case .closestToMastery: compare(max(0, 99 - left.level), max(0, 99 - right.level))
            case .speciesMasteryCount: compare(right.speciesMasteryCount, left.speciesMasteryCount)
            }
            if primary != .orderedSame { return primary == .orderedAscending }
            let name = left.title.localizedCaseInsensitiveCompare(right.title)
            if name != .orderedSame { return name == .orderedAscending }
            if left.level != right.level { return left.level > right.level }
            return left.id < right.id
        }
    }

    private static func compare<T: Comparable>(_ lhs: T, _ rhs: T) -> ComparisonResult {
        lhs < rhs ? .orderedAscending : (lhs > rhs ? .orderedDescending : .orderedSame)
    }
}

enum AchievementCategory: String, CaseIterable, Sendable {
    case all, flow, arc, creatures, mastery, collections, stillwater, movement, surge, objectives, special, historical
}
enum AchievementSort: String, CaseIterable, Sendable { case recommended, recentlyEarned, recentlyAdvanced, highestCount, closestMilestone, alphabetical }

/// Mirrors Android's `BadgeActionDestination`. Associated IDs are intentionally
/// retained until the destination consumes them so a collection action cannot
/// degrade into merely opening the right room.
enum AchievementActionDestination: Equatable, Sendable {
    case flow
    case arc
    case badgeDetails(badgeID: String)
    case collectionDetails(collectionID: String, speciesID: String? = nil)
    case chestSpecies(speciesID: String)
    case blueRegion(collectionID: String, speciesID: String? = nil)
    case stillwaterVessel(collectionID: String, speciesID: String? = nil)
    case beyondBlue(collectionID: String, speciesID: String)
}

struct CollectionSpeciesProgress: Identifiable, Equatable, Sendable {
    var id: String { speciesID }
    let speciesID: String
    let discovered: Bool
    let ownedCount: Int
    let highestLevel: Int?
    let currentLevel99Count: Int
    let lifetimeMasteryCount: Int
    let action: AchievementActionDestination?
}

enum AchievementActionPolicy {
    static func destination(
        for badgeID: String,
        category: AchievementCategory,
        ownedSpeciesIDs: Set<String>,
        discoveredSpeciesIDs: Set<String>
    ) -> AchievementActionDestination? {
        if badgeID.hasPrefix("mastery_species_") {
            let speciesID = String(badgeID.dropFirst("mastery_species_".count))
            return speciesDestination(
                speciesID: speciesID,
                ownedSpeciesIDs: ownedSpeciesIDs,
                discoveredSpeciesIDs: discoveredSpeciesIDs
            )
        }
        if let collectionID = CollectionRosterCatalog.allCollectionIDs.first(where: {
            badgeID.hasPrefix("\($0)_")
        }) {
            return .collectionDetails(collectionID: collectionID)
        }
        switch badgeID {
        case "stillwater_first_catch", "stillwater_variety", "stillwater_mastery":
            return .collectionDetails(collectionID: "collection_stillwater")
        case "across_the_depths", "keeper_of_the_blue":
            return .collectionDetails(collectionID: "collection_the_blue")
        case "one_from_every_water":
            return .collectionDetails(collectionID: "collection_all_waters")
        default:
            break
        }
        switch category {
        case .flow, .surge:
            return .flow
        case .arc:
            return .arc
        default:
            return nil
        }
    }

    static func speciesDestination(
        speciesID: String,
        ownedSpeciesIDs: Set<String>,
        discoveredSpeciesIDs: Set<String>
    ) -> AchievementActionDestination? {
        guard let creature = CreatureCatalog.definition(speciesID) else { return nil }
        if creature.secretUntilDiscovered && !discoveredSpeciesIDs.contains(speciesID) { return nil }
        if ownedSpeciesIDs.contains(speciesID) { return .chestSpecies(speciesID: speciesID) }
        switch creature.sourceType {
        case .flowEarned:
            return .blueRegion(collectionID: creature.collectionID, speciesID: speciesID)
        case .beyondBlue:
            return .beyondBlue(collectionID: creature.collectionID, speciesID: speciesID)
        case .stillwater:
            return .stillwaterVessel(collectionID: creature.primaryProgressCollectionID, speciesID: speciesID)
        }
    }
}

struct AchievementBadgeDefinition: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let description: String
    let category: AchievementCategory
    let milestones: [Int]
    let pinnable: Bool
}

enum AchievementCatalog {
    static let defaultMilestones = [1, 5, 10, 25, 50, 100, 250, 500, 1_000]
    static let definitions: [AchievementBadgeDefinition] = [
        .init(id: ShellRewardCatalog.badgeFlow10, title: "10-minute Flow", description: "Earned each time a regular Flow lasts 10 minutes or more.", category: .flow, milestones: defaultMilestones, pinnable: true),
        .init(id: ShellRewardCatalog.badgeFlow30, title: "30-minute Flow", description: "Earned each time a regular Flow lasts 30 minutes or more.", category: .flow, milestones: defaultMilestones, pinnable: true),
        .init(id: ShellRewardCatalog.badgeFlow60, title: "60-minute Flow", description: "Earned each time a regular Flow lasts 60 minutes or more.", category: .flow, milestones: defaultMilestones, pinnable: true),
        .init(id: ShellRewardCatalog.badgeFlow120, title: "2-hour Flow", description: "Earned each time a regular Flow lasts 2 hours or more.", category: .flow, milestones: defaultMilestones, pinnable: true),
        .init(id: "variety_collector", title: "Creature Discovery", description: "Records distinct creature species discovered across all waters.", category: .creatures, milestones: defaultMilestones, pinnable: true),
        .init(id: "stillwater_first_catch", title: "Stillwater First Catch", description: "Records the first creature drawn from a quiet vessel.", category: .stillwater, milestones: [], pinnable: true),
        .init(id: "stillwater_variety", title: "Stillwater Variety", description: "Records distinct Stillwater species discovered.", category: .stillwater, milestones: defaultMilestones, pinnable: true),
        .init(id: "stillwater_mastery", title: "Stillwater Mastery", description: "Records reliable Stillwater discovery and Mastery progress.", category: .stillwater, milestones: defaultMilestones, pinnable: true),
        .init(id: "mastery_first", title: "First Mastery", description: "Records the first creature to reach Level 99.", category: .mastery, milestones: [], pinnable: true),
        .init(id: "mastery_circle", title: "Circle of Mastery", description: "Records every immutable Level-99 mastery event.", category: .mastery, milestones: defaultMilestones, pinnable: true),
        .init(id: "mastery_variety", title: "Mastery Variety", description: "Records distinct species that have ever reached Level 99.", category: .mastery, milestones: defaultMilestones, pinnable: true)
    ] + CreatureCatalog.all.map { creature in
        .init(id: "mastery_species_\(creature.id)", title: "\(creature.displayName) Mastery",
              description: "Records each \(creature.displayName) that has reached Level 99.",
              category: .mastery, milestones: defaultMilestones, pinnable: true)
    } + CollectionRosterCatalog.allCollectionIDs.flatMap { collectionID in
        CollectionCompletionType.allCases.map { type in
            .init(id: "\(collectionID)_\(type.rawValue.lowercased())",
                  title: "\(CollectionRosterCatalog.title(for: collectionID)) \(type.rawValue.capitalized)",
                  description: "Preserves completion evidence for this catalog edition.",
                  category: .collections, milestones: [], pinnable: true)
        }
    }
    static let byID = Dictionary(uniqueKeysWithValues: definitions.map { ($0.id, $0) })
    static func resolve(_ id: String) -> AchievementBadgeDefinition {
        byID[id] ?? .init(id: id, title: id.replacingOccurrences(of: "_", with: " ").capitalized, description: "A historical achievement preserved from your Shell.", category: .historical, milestones: [], pinnable: true)
    }
}

struct AchievementMilestone: Equatable, Sendable {
    let exactCount: Int
    let currentThreshold: Int?
    let nextThreshold: Int?
    let progressToNext: Int
}

enum AchievementMilestoneEngine {
    static func evaluate(count: Int, thresholds: [Int] = AchievementCatalog.defaultMilestones) -> AchievementMilestone {
        let exact = max(0, count)
        let current = thresholds.last { $0 <= exact }
        let next = thresholds.first { $0 > exact }
        return AchievementMilestone(exactCount: exact, currentThreshold: current, nextThreshold: next, progressToNext: next == nil ? 0 : exact - (current ?? 0))
    }
}

struct AchievementProgress: Identifiable, Equatable, Sendable {
    var id: String { badgeID }
    let badgeID: String
    let title: String
    let description: String
    let category: AchievementCategory
    let count: Int
    let milestone: AchievementMilestone
    let firstEarnedAt: Date?
    let lastAdvancedAt: Date?
    let pinOrder: Int?
    let isNew: Bool

    var earned: Bool { count > 0 }
    var remaining: Int { max(0, (milestone.nextThreshold ?? count) - count) }
}

struct AchievementDashboard: Equatable, Sendable {
    let badges: [AchievementProgress]
    let pinned: [AchievementProgress]
    let newCount: Int
}

enum AchievementDashboardCalculator {
    static func calculate(badges: [ShellBadge], countFloors: [String: Int], pins: [String: Int]) -> AchievementDashboard {
        let byID = Dictionary(uniqueKeysWithValues: badges.map { ($0.badgeID, $0) })
        let ids = Set(AchievementCatalog.definitions.map(\.id)).union(byID.keys).union(countFloors.keys)
        let progress = ids.map { id -> AchievementProgress in
            let definition = AchievementCatalog.resolve(id)
            let stored = byID[id]
            let count = max(stored?.count ?? 0, countFloors[id, default: 0])
            return AchievementProgress(
                badgeID: id, title: definition.title, description: definition.description,
                category: definition.category, count: count,
                milestone: AchievementMilestoneEngine.evaluate(count: count, thresholds: definition.milestones),
                firstEarnedAt: stored?.firstEarnedAt, lastAdvancedAt: stored?.lastEarnedAt,
                pinOrder: pins[id], isNew: stored?.isNew == true
            )
        }.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        return AchievementDashboard(
            badges: progress,
            pinned: progress.filter { $0.pinOrder != nil }.sorted { ($0.pinOrder ?? 99) < ($1.pinOrder ?? 99) },
            newCount: progress.filter(\.isNew).count
        )
    }

    static func sorted(_ badges: [AchievementProgress], by sort: AchievementSort) -> [AchievementProgress] {
        badges.sorted { left, right in
            switch sort {
            case .recommended:
                if (left.pinOrder ?? 99) != (right.pinOrder ?? 99) { return (left.pinOrder ?? 99) < (right.pinOrder ?? 99) }
                if left.earned != right.earned { return left.earned && !right.earned }
                if left.remaining != right.remaining { return left.remaining < right.remaining }
            case .recentlyEarned:
                if left.firstEarnedAt != right.firstEarnedAt { return (left.firstEarnedAt ?? .distantPast) > (right.firstEarnedAt ?? .distantPast) }
            case .recentlyAdvanced:
                if left.lastAdvancedAt != right.lastAdvancedAt { return (left.lastAdvancedAt ?? .distantPast) > (right.lastAdvancedAt ?? .distantPast) }
            case .highestCount:
                if left.count != right.count { return left.count > right.count }
            case .closestMilestone:
                if left.remaining != right.remaining { return left.remaining < right.remaining }
            case .alphabetical:
                break
            }
            return left.title.localizedCaseInsensitiveCompare(right.title) == .orderedAscending
        }
    }
}

struct ShellNotificationItem: Identifiable, Equatable, Sendable {
    enum Kind: String, Sendable { case find, badge }
    let id: String
    let kind: Kind
    let sourceID: String
    let title: String
    let detail: String
    let occurredAt: Date
}

struct ShellCollectionBackfillResult: Equatable, Sendable {
    let version: Int
    let instanceCount: Int
    let badgeCount: Int
    let alreadyCompleted: Bool
}

enum ShellPinResult: Equatable, Sendable {
    case pinned
    case alreadyPinned
    case replacementRequired([String])
}

enum ShellCollectionError: LocalizedError, Equatable {
    case missingFind
    case invalidSlot
    case incompatibleSlot
    case notPlaceable
    case insufficientPearls(required: Int, available: Int)
    case noUpgrade
    case badgeNotEarned
    case badgeNotPinnable
    case replacementRequired

    var errorDescription: String? {
        switch self {
        case .missingFind: "Shell reward not found."
        case .invalidSlot: "Invalid nook."
        case .incompatibleSlot: "This reward cannot rest in that nook."
        case .notPlaceable: "This reward is not displayable."
        case .insufficientPearls(let required, let available): "This needs \(required) Pearls; \(available) are available."
        case .noUpgrade: "This find is resting in its current form."
        case .badgeNotEarned: "Only earned badges can be pinned."
        case .badgeNotPinnable: "This badge cannot be pinned."
        case .replacementRequired: "Choose a pinned badge to replace."
        }
    }
}
