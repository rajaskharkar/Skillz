import Foundation

/// Presentation contracts shared by the realm browser, child routes and tests.
enum BlueRealmPolicy {
    static func zones(in realm: CreatureRealm) -> [ShellDepthTier] {
        realm == .sea ? ShellDepthTier.seaZones : ShellDepthTier.landZones
    }

    static func collectionID(for zone: ShellDepthTier) -> String {
        "blue_" + zone.rawValue.unicodeScalars.reduce(into: "") { value, scalar in
            if CharacterSet.uppercaseLetters.contains(scalar) { value.append("_") }
            value.append(String(scalar).lowercased())
        }
    }

    static func zone(collectionID: String) -> ShellDepthTier? {
        ShellDepthTier.allCases.first { self.collectionID(for: $0) == collectionID }
    }

    /// Android lists the Arc flagship alongside purchasable Land species; it
    /// is informational, never a purchase target. Browsing has no unlock gate.
    static func encounters(in zone: ShellDepthTier) -> [CreatureDefinition] {
        CreatureCatalog.all.filter {
            $0.zone == zone && ($0.sourceType == .beyondBlue || $0.sourceType == .arcEarned)
        }
    }

    static func subtitle(for zone: ShellDepthTier) -> String {
        switch zone {
        case .sunlitReef: "Short regular Flows begin life here."
        case .deeperReef: "Longer regular Flows bring rarer life into view."
        case .openBlue: "Hour-long Flows leave wide currents behind."
        case .greatBlue: "The longest Flows echo in the great deep."
        case .goldenFields: "Warm fields, open paths, familiar life."
        case .ancientWoods: "Life beneath an ancient canopy."
        case .openSands: "Quiet life across the wide sands."
        case .highPeaks: "Wildlife among snow and stone."
        case .greatWild: "The quiet strength of the wilderness."
        }
    }

    static func railTitle(for zone: ShellDepthTier) -> String {
        switch zone {
        case .sunlitReef: "Sunlit"
        case .deeperReef: "Deeper"
        case .openBlue: "Open"
        case .greatBlue: "Great"
        case .goldenFields: "Fields"
        case .ancientWoods: "Woods"
        case .openSands: "Sands"
        case .highPeaks: "Peaks"
        case .greatWild: "Wild"
        }
    }
}

struct BeyondBlueTradeStack: Identifiable {
    let creature: CreatureDefinition
    let level: Int
    let instances: [ShellFindInstance]
    var id: String { "\(creature.id):\(level)" }
    var valueMinutes: Int { CreatureEconomy.flowTimeValueMinutes(creature.id) }

    /// Input is active inventory, including placed copies. Trading crosses
    /// realm boundaries, and older copies at the selected level leave first.
    static func make(from activeInstances: [ShellFindInstance]) -> [Self] {
        Dictionary(grouping: activeInstances.filter { CreatureCatalog.definition($0.findID) != nil }) {
            "\($0.findID):\(max(1, $0.animalLevel))"
        }.compactMap { _, instances in
            guard let first = instances.first, let creature = CreatureCatalog.definition(first.findID) else { return nil }
            return Self(creature: creature, level: max(1, first.animalLevel), instances: instances.sorted {
                $0.acquiredAt == $1.acquiredAt ? $0.id < $1.id : $0.acquiredAt < $1.acquiredAt
            })
        }.sorted { $0.id < $1.id }
    }

    static func selectedIDs(stacks: [Self], counts: [String: Int]) -> [String] {
        stacks.flatMap { stack in
            stack.instances.prefix(max(0, min(counts[stack.id] ?? 0, stack.instances.count))).map(\.id)
        }
    }
}
