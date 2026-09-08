import Foundation

enum CreatureStatus: String, Codable, CaseIterable, Sendable {
    case active = "ACTIVE"
    case released = "RELEASED"
    case usedBeyondBlue = "USED_BEYOND_BLUE"
}

enum CreatureSourceType: String, Codable, Sendable {
    case flowEarned = "FLOW_EARNED"
    case beyondBlue = "BEYOND_BLUE"
    case stillwater = "STILLWATER"
}

enum CreatureMasteryTier: String, CaseIterable, Sendable {
    case seasoned, proven, veteran, ascendant, mastered
}

struct CreatureDefinition: Identifiable, Equatable, Sendable {
    var id: String { creatureID }
    let creatureID: String
    let displayName: String
    let zone: ShellDepthTier
    let sourceType: CreatureSourceType
    let flowTimeValueMinutes: Int?
    let requirementMinutes: Int?
    let systemImage: String
    let participatesInCollector: Bool
    let participatesInCompletionist: Bool
    let secretUntilDiscovered: Bool
    let rosterVersion: Int

    var pearlPrice: Int? { requirementMinutes.map { $0 * 2 } }
    var collectionID: String {
        sourceType == .stillwater ? "collection_stillwater" : "blue_\(zone.rawValue.snakeCased)"
    }
    var primaryProgressCollectionID: String {
        guard sourceType == .stillwater,
              let vessel = StillwaterCatalog.byID[creatureID]?.vessel else { return collectionID }
        return "stillwater_\(vessel.rawValue)"
    }
    var collectionIDs: Set<String> {
        if sourceType == .stillwater {
            return [primaryProgressCollectionID, "collection_stillwater", "collection_all_waters"]
        }
        return [collectionID, "collection_the_blue", "collection_all_waters"]
    }
}

enum CreatureCatalog {
    private static func flow(_ id: String, _ name: String, _ zone: ShellDepthTier, _ minutes: Int) -> CreatureDefinition {
        .init(creatureID: id, displayName: name, zone: zone, sourceType: .flowEarned,
              flowTimeValueMinutes: minutes, requirementMinutes: nil, systemImage: icon(for: zone),
              participatesInCollector: true, participatesInCompletionist: true,
              secretUntilDiscovered: false, rosterVersion: 1)
    }

    private static func beyond(_ id: String, _ name: String, _ zone: ShellDepthTier, _ minutes: Int) -> CreatureDefinition {
        .init(creatureID: id, displayName: name, zone: zone, sourceType: .beyondBlue,
              flowTimeValueMinutes: nil, requirementMinutes: minutes, systemImage: icon(for: zone),
              participatesInCollector: true, participatesInCompletionist: true,
              secretUntilDiscovered: false, rosterVersion: 1)
    }

    private static func icon(for zone: ShellDepthTier) -> String {
        switch zone {
        case .sunlitReef, .deeperReef: "fish.fill"
        case .openBlue: "water.waves"
        case .greatBlue: "drop.triangle.fill"
        }
    }

    private static let blue: [CreatureDefinition] = [
        flow(ShellRewardCatalog.focusMinnow, "Minnow", .sunlitReef, 10),
        beyond("creature_clownfish", "Clownfish", .sunlitReef, 30),
        beyond("creature_blue_tang", "Blue Tang", .sunlitReef, 45),
        beyond("creature_butterflyfish", "Butterflyfish", .sunlitReef, 60),
        beyond("creature_angelfish", "Angelfish", .sunlitReef, 90),
        beyond("creature_parrotfish", "Parrotfish", .sunlitReef, 120),
        beyond("creature_jellyfish", "Jellyfish", .sunlitReef, 180),
        beyond("creature_sea_turtle", "Sea Turtle", .sunlitReef, 300),
        beyond("creature_sea_otter", "Sea Otter", .sunlitReef, 420),
        beyond("creature_seal", "Seal", .sunlitReef, 480),

        flow(ShellRewardCatalog.focusSeahorse, "Seahorse", .deeperReef, 30),
        beyond("creature_starfish", "Starfish", .deeperReef, 90),
        beyond("creature_sea_urchin", "Sea Urchin", .deeperReef, 120),
        beyond("creature_pufferfish", "Pufferfish", .deeperReef, 180),
        beyond("creature_lionfish", "Scorpionfish", .deeperReef, 240),
        beyond("creature_moray_eel", "Moray Eel", .deeperReef, 300),
        beyond("creature_stingray", "Stingray", .deeperReef, 360),
        beyond("focus_octopus", "Octopus", .deeperReef, 480),
        beyond("creature_sea_snake", "Sea Snake", .deeperReef, 540),
        beyond("creature_squid", "Squid", .deeperReef, 600),

        flow(ShellRewardCatalog.focusManta, "Manta", .openBlue, 60),
        beyond("creature_flying_fish", "Flying Fish", .openBlue, 120),
        beyond("creature_barracuda", "Needlefish", .openBlue, 180),
        beyond("creature_swordfish", "Swordfish", .openBlue, 240),
        beyond("creature_dolphin", "Dolphin", .openBlue, 360),
        beyond("creature_ocean_sunfish", "Ocean Sunfish", .openBlue, 480),
        beyond("creature_penguin", "Penguin", .openBlue, 540),
        beyond("creature_sea_lion", "Sea Lion", .openBlue, 600),
        beyond("creature_orca", "Orca", .openBlue, 720),
        beyond("creature_great_white_shark", "Great White Shark", .openBlue, 960),

        flow(ShellRewardCatalog.focusWhale, "Whale", .greatBlue, 120),
        beyond("creature_anglerfish", "Anglerfish", .greatBlue, 300),
        beyond("creature_leatherback_turtle", "Leatherback Turtle", .greatBlue, 360),
        beyond("creature_giant_squid", "Giant Squid", .greatBlue, 600),
        beyond("creature_humpback_whale", "Humpback Whale", .greatBlue, 720),
        beyond("creature_blue_whale", "Blue Whale", .greatBlue, 900),
        beyond("creature_megalodon", "Megalodon", .greatBlue, 1_200),
        beyond("creature_kraken", "Kraken", .greatBlue, 1_500),
        beyond("creature_leviathan", "Leviathan", .greatBlue, 1_800)
    ]

    private static let stillwater: [CreatureDefinition] = StillwaterCatalog.creatures.map { entry in
        .init(creatureID: entry.creatureID, displayName: entry.displayName, zone: entry.vessel.zone,
              sourceType: .stillwater, flowTimeValueMinutes: nil,
              requirementMinutes: Int(entry.vessel.dropCost / 60), systemImage: icon(for: entry.vessel.zone),
              participatesInCollector: true, participatesInCompletionist: true,
              secretUntilDiscovered: false, rosterVersion: 1)
    }

    static let all = blue + stillwater
    static let byID = Dictionary(uniqueKeysWithValues: all.map { ($0.id, $0) })
    static let flowEarned = all.filter { $0.sourceType == .flowEarned }
    static let beyondBlue = all.filter { $0.sourceType == .beyondBlue }
    static let stillwaterCreatures = all.filter { $0.sourceType == .stillwater }
    static func definition(_ id: String) -> CreatureDefinition? { byID[id] }
}

enum StillwaterVessel: String, CaseIterable, Codable, Identifiable, Sendable {
    case fishbowl, aquarium, pond, lake
    var id: String { rawValue }

    var dropCost: Int64 {
        switch self {
        case .fishbowl: 15_000
        case .aquarium: 25_000
        case .pond: 45_000
        case .lake: 75_000
        }
    }
    var level: Int { Self.allCases.firstIndex(of: self).map { $0 + 1 } ?? 1 }
    var zone: ShellDepthTier { Self.allCases[level - 1].depthForIndex }
    private var depthForIndex: ShellDepthTier {
        switch self {
        case .fishbowl: .sunlitReef
        case .aquarium: .deeperReef
        case .pond: .openBlue
        case .lake: .greatBlue
        }
    }
    var title: String { rawValue.capitalized }
    var rewardDescription: String {
        switch self {
        case .fishbowl: "Tiny shallow-water creature"
        case .aquarium: "Colorful reeflike creature"
        case .pond: "Large roaming-water creature"
        case .lake: "Rare dark-water creature"
        }
    }
    var categoryDescription: String {
        switch self {
        case .fishbowl: "Level 1 · Shallow Stillwater"
        case .aquarium: "Level 2 · Reeflike Stillwater"
        case .pond: "Level 3 · Wide Stillwater"
        case .lake: "Level 4 · Dark Stillwater"
        }
    }
    var requiresConfirmation: Bool { self == .pond || self == .lake }
}

enum StillwaterRarity: String, Codable, CaseIterable, Sendable { case common, uncommon, rare, mythic }

struct StillwaterCreatureEntry: Identifiable, Equatable, Sendable {
    var id: String { creatureID }
    let creatureID: String
    let displayName: String
    let vessel: StillwaterVessel
    let rarity: StillwaterRarity
}

enum StillwaterCatalog {
    static let creatures: [StillwaterCreatureEntry] = [
        entry("stillwater_shrimp", "Shrimp", .fishbowl, .common),
        entry("stillwater_crab", "Crab", .fishbowl, .common),
        entry("stillwater_clam", "Clam", .fishbowl, .common),
        entry("stillwater_snail", "Snail", .fishbowl, .uncommon),
        entry("stillwater_limpet", "Limpet", .fishbowl, .uncommon),
        entry("stillwater_barnacle", "Barnacle", .fishbowl, .uncommon),
        entry("stillwater_cowrie", "Cowrie", .fishbowl, .rare),
        entry("stillwater_horseshoe", "Horseshoe", .fishbowl, .mythic),
        entry("stillwater_goby", "Goby", .aquarium, .common),
        entry("stillwater_wrasse", "Wrasse", .aquarium, .common),
        entry("stillwater_blenny", "Blenny", .aquarium, .common),
        entry("stillwater_lionfish", "Lionfish", .aquarium, .uncommon),
        entry("stillwater_anemone", "Anemone", .aquarium, .uncommon),
        entry("stillwater_cuttlefish", "Cuttlefish", .aquarium, .uncommon),
        entry("stillwater_moray", "Moray", .aquarium, .rare),
        entry("stillwater_nautilus", "Nautilus", .aquarium, .mythic),
        entry("stillwater_mahi", "Mahi", .pond, .common),
        entry("stillwater_wahoo", "Wahoo", .pond, .common),
        entry("stillwater_bonito", "Bonito", .pond, .common),
        entry("stillwater_barracuda", "Barracuda", .pond, .uncommon),
        entry("stillwater_amberjack", "Amberjack", .pond, .uncommon),
        entry("stillwater_grouper", "Grouper", .pond, .uncommon),
        entry("stillwater_marlin", "Marlin", .pond, .rare),
        entry("stillwater_sailfish", "Sailfish", .pond, .mythic),
        entry("stillwater_fangtooth", "Fangtooth", .lake, .common),
        entry("stillwater_viperfish", "Viperfish", .lake, .common),
        entry("stillwater_hatchetfish", "Hatchetfish", .lake, .common),
        entry("stillwater_gulper", "Gulper", .lake, .uncommon),
        entry("stillwater_grenadier", "Grenadier", .lake, .uncommon),
        entry("stillwater_oarfish", "Oarfish", .lake, .uncommon),
        entry("stillwater_blackdragon", "Blackdragon", .lake, .rare),
        entry("stillwater_coelacanth", "Coelacanth", .lake, .mythic)
    ]
    static let byID = Dictionary(uniqueKeysWithValues: creatures.map { ($0.id, $0) })

    static func creatures(for vessel: StillwaterVessel) -> [StillwaterCreatureEntry] {
        creatures.filter { $0.vessel == vessel }
    }

    static func rarity(for roll: Int) -> StillwaterRarity {
        switch positiveModulo(roll, 100) {
        case 0...59: .common
        case 60...89: .uncommon
        case 90...97: .rare
        default: .mythic
        }
    }

    static func roll(vessel: StillwaterVessel, rarityRoll: Int, selectionRoll: Int) -> StillwaterCreatureEntry {
        let all = creatures(for: vessel)
        let desired = rarity(for: rarityRoll)
        let pool = all.filter { $0.rarity == desired }
        let candidates = pool.isEmpty ? all : pool
        return candidates[positiveModulo(selectionRoll, candidates.count)]
    }

    private static func entry(_ id: String, _ name: String, _ vessel: StillwaterVessel, _ rarity: StillwaterRarity) -> StillwaterCreatureEntry {
        .init(creatureID: id, displayName: name, vessel: vessel, rarity: rarity)
    }

    private static func positiveModulo(_ value: Int, _ divisor: Int) -> Int {
        guard divisor > 0 else { return 0 }
        let result = value % divisor
        return result >= 0 ? result : result + divisor
    }
}

enum StillwaterPerspective: String, CaseIterable, Codable, Sendable {
    case overview, fishbowl, aquarium, pond, lake
    var vessel: StillwaterVessel? { StillwaterVessel(rawValue: rawValue) }
    init(vessel: StillwaterVessel) { self = Self(rawValue: vessel.rawValue) ?? .overview }
}

struct CreatureDiscoveryEvidence: Identifiable, Equatable, Sendable {
    var id: String { speciesID }
    let speciesID: String
    let firstDiscoveredAt: Date
    let sourceType: CreatureSourceType
    let firstInstanceID: String?
    let timestampConfidence: String
}

struct CreatureMasteryEvidence: Identifiable, Equatable, Sendable {
    let id: String
    let instanceID: String
    let speciesID: String
    let achievedAt: Date
    let transactionID: String
    let timestampConfidence: String

    init(
        id: String,
        instanceID: String,
        speciesID: String,
        achievedAt: Date,
        transactionID: String,
        timestampConfidence: String = "EXACT"
    ) {
        self.id = id
        self.instanceID = instanceID
        self.speciesID = speciesID
        self.achievedAt = achievedAt
        self.transactionID = transactionID
        self.timestampConfidence = timestampConfidence
    }
}

enum CollectionCompletionType: String, CaseIterable, Codable, Sendable {
    case collector = "COLLECTOR"
    case curator = "CURATOR"
    case completionist = "COMPLETIONIST"
}

struct CollectionCompletionEvidence: Identifiable, Equatable, Sendable {
    let id: String
    let collectionID: String
    let type: CollectionCompletionType
    let completedAt: Date
    let rosterVersion: Int
    let rosterHash: String
    let requiredSpeciesIDs: [String]
}

struct StillwaterCollectionProgress: Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let total: Int
    let discovered: Int
    let owned: Int
    let mastered: Int
    let collectorEarned: Bool
    let curatorEarned: Bool
    let completionistEarned: Bool
}

struct StillwaterDrawResult: Equatable, Sendable {
    let instance: ShellFindInstance
    let creature: CreatureDefinition
    let rarity: StillwaterRarity
    let remainingDrops: Int64
    let wasFirstDiscovery: Bool
}

enum StillwaterError: LocalizedError, Equatable {
    case locked
    case insufficientDrops(required: Int64, available: Int64)
    case unknownCreature

    var errorDescription: String? {
        switch self {
        case .locked: "Progress farther in The Blue to unlock this vessel."
        case .insufficientDrops: "Not enough Drops yet."
        case .unknownCreature: "That creature could not surface. Please try again."
        }
    }
}

enum CreatureEconomy {
    static let maxLevel = 99

    static func flowTimeValueMinutes(_ creatureID: String) -> Int {
        guard let definition = CreatureCatalog.definition(creatureID) else { return 0 }
        return definition.flowTimeValueMinutes ?? definition.requirementMinutes ?? 0
    }

    static func canonicalPearlValue(_ creatureID: String) -> Int { flowTimeValueMinutes(creatureID) * 2 }

    static func baseGrowthCost(_ creatureID: String) -> Int {
        switch creatureID {
        case ShellRewardCatalog.focusMinnow: 25
        case ShellRewardCatalog.focusSeahorse: 75
        case ShellRewardCatalog.focusManta: 200
        case ShellRewardCatalog.focusWhale: 600
        default: max(25, flowTimeValueMinutes(creatureID))
        }
    }

    static func growthCostPearls(_ creatureID: String, currentLevel: Int) -> Int {
        let level = min(max(currentLevel, 1), maxLevel - 1)
        let base = Double(baseGrowthCost(creatureID))
        let multiplier = 1 + Double(level) * 0.12 + pow(Double(level), 1.45) * 0.018 + log(Double(level)) * 0.35
        return max(1, Int((base * multiplier).rounded(.towardZero)))
    }

    static func cumulativeGrowthCostPearls(_ creatureID: String, level: Int) -> Int64 {
        let safe = min(max(level, 1), maxLevel)
        guard safe > 1 else { return 0 }
        return (1..<safe).reduce(0) { $0 + Int64(growthCostPearls(creatureID, currentLevel: $1)) }
    }

    static func releaseSalvageRate(level: Int) -> Double {
        switch min(max(level, 1), maxLevel) {
        case 99...: 0.35
        case 75...: 0.30
        case 50...: 0.25
        case 25...: 0.20
        case 10...: 0.15
        default: 0.10
        }
    }

    static func releaseValuePearls(_ creatureID: String, level: Int = 1) -> Int {
        guard let definition = CreatureCatalog.definition(creatureID) else { return 0 }
        let safe = min(max(level, 1), maxLevel)
        let normal = Double(canonicalPearlValue(creatureID))
            + Double(cumulativeGrowthCostPearls(creatureID, level: safe)) * releaseSalvageRate(level: safe)
        let adjusted = definition.sourceType == .stillwater ? normal * 0.25 : normal
        return max(1, Int(adjusted.rounded()))
    }

    static func masteryTier(level: Int) -> CreatureMasteryTier? {
        switch level {
        case 99...: .mastered
        case 75...: .ascendant
        case 50...: .veteran
        case 25...: .proven
        case 10...: .seasoned
        default: nil
        }
    }
}

enum CreatureUnlockPolicy {
    static func unlockedZones(from historicalFinds: [ShellFindInstance]) -> Set<ShellDepthTier> {
        let order: [ShellDepthTier] = [.sunlitReef, .deeperReef, .openBlue, .greatBlue]
        let deepest = historicalFinds.compactMap { instance -> Int? in
            guard let creature = CreatureCatalog.definition(instance.findID),
                  creature.sourceType != .stillwater else { return nil }
            return order.firstIndex(of: creature.zone)
        }.max() ?? 0
        return Set(order.prefix(deepest + 1))
    }
}

private extension String {
    var snakeCased: String {
        unicodeScalars.reduce(into: "") { result, scalar in
            if CharacterSet.uppercaseLetters.contains(scalar), !result.isEmpty { result.append("_") }
            result.append(String(scalar).lowercased())
        }
    }
}
