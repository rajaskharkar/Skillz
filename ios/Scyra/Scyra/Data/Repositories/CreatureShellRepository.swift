import Foundation
import SwiftData

struct CreaturePaymentQuote: Equatable, Sendable {
    let targetRequirementMinutes: Int
    let selectedCreatureMinutes: Int
    let remainingMinutes: Int
    let pearlCostForRemaining: Int
    let pearlReturnForOverpay: Int
    let canEncounter: Bool
}

struct CreatureGrowthResult: Equatable, Sendable {
    let instanceID: String
    let pearlCost: Int
    let resultingLevel: Int
    let recordedMastery: Bool
}

struct MasteryCelebration: Identifiable, Equatable, Sendable {
    let id: String
    let instanceID: String
    let speciesID: String
    let createdAt: Date
}

enum CreatureShellError: LocalizedError, Equatable {
    case missingCreature
    case inactiveCreature
    case mastered
    case insufficientPearls(required: Int, available: Int)
    case invalidEncounter
    case missingSelection

    var errorDescription: String? {
        switch self {
        case .missingCreature: "Creature not found."
        case .inactiveCreature: "Only active creatures can be used."
        case .mastered: "Mastered at Level 99."
        case .insufficientPearls(let required, let available):
            "Level up requires \(required) Pearls. You need \(max(0, required - available)) more."
        case .invalidEncounter: "Only Beyond Blue creatures can be encountered here."
        case .missingSelection: "A selected creature is no longer available."
        }
    }
}

@MainActor
protocol CreatureShellRepository: AnyObject {
    func fetchStillwaterPerspective() throws -> StillwaterPerspective
    func saveStillwaterPerspective(_ perspective: StillwaterPerspective, at date: Date) throws
    func fetchUnlockedCreatureZones() throws -> Set<ShellDepthTier>
    func fetchCreatureDiscoveries() throws -> [CreatureDiscoveryEvidence]
    func fetchCreatureMasteries() throws -> [CreatureMasteryEvidence]
    func fetchCreatureCollectionProgress() throws -> [StillwaterCollectionProgress]
    func fetchStillwaterCollectionProgress() throws -> [StillwaterCollectionProgress]
    func drawFromStillwater(
        vessel: StillwaterVessel,
        rarityRoll: Int,
        selectionRoll: Int,
        at date: Date
    ) throws -> StillwaterDrawResult
    func quoteBeyondBlueEncounter(targetCreatureID: String, selectedInstanceIDs: [String]) throws -> CreaturePaymentQuote
    func encounterBeyondBlue(targetCreatureID: String, selectedInstanceIDs: [String], at date: Date) throws -> ShellFindInstance
    func growCreature(instanceID: String, transactionID: String, at date: Date) throws -> CreatureGrowthResult
    func releaseCreature(instanceID: String, at date: Date) throws -> Int
    func releaseCreatures(instanceIDs: [String], at date: Date) throws -> Int
    func creatureStatus(instanceID: String) throws -> CreatureStatus
    func fetchPendingMasteryCelebration() throws -> MasteryCelebration?
    func acknowledgeMasteryCelebration(id: String, at date: Date) throws
    func trackedAchievementIDs() throws -> Set<String>
    func setAchievementTracked(_ badgeID: String, tracked: Bool, at date: Date) throws
}

extension SwiftDataFlowRepository {
    func fetchStillwaterPerspective() throws -> StillwaterPerspective {
        guard let model = try context.fetch(FetchDescriptor<StillwaterPreferenceModel>()).first else { return .overview }
        return StillwaterPerspective(rawValue: model.perspectiveRawValue) ?? .overview
    }

    func saveStillwaterPerspective(_ perspective: StillwaterPerspective, at date: Date = Date()) throws {
        do {
            if let model = try context.fetch(FetchDescriptor<StillwaterPreferenceModel>()).first {
                model.perspectiveRawValue = perspective.rawValue
                model.updatedAt = date
            } else {
                context.insert(StillwaterPreferenceModel(perspective: perspective, updatedAt: date))
            }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func fetchUnlockedCreatureZones() throws -> Set<ShellDepthTier> {
        CreatureUnlockPolicy.unlockedZones(from: try historicalCreatureInstances())
    }

    func fetchCreatureDiscoveries() throws -> [CreatureDiscoveryEvidence] {
        try context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).map(Self.discoveryEvidence)
            .sorted { $0.firstDiscoveredAt < $1.firstDiscoveredAt }
    }

    func fetchCreatureMasteries() throws -> [CreatureMasteryEvidence] {
        try context.fetch(FetchDescriptor<CreatureMasteryModel>()).map(Self.masteryEvidence)
            .sorted { $0.achievedAt < $1.achievedAt }
    }

    func fetchStillwaterCollectionProgress() throws -> [StillwaterCollectionProgress] {
        let progress = try allCollectionProgress()
        let ordered = ["collection_stillwater"] + StillwaterVessel.allCases.map { "stillwater_\($0.rawValue)" }
        return ordered.compactMap { progress[$0] }
    }

    func fetchCreatureCollectionProgress() throws -> [StillwaterCollectionProgress] {
        let progress = try allCollectionProgress()
        return CollectionRosterCatalog.allCollectionIDs.compactMap { progress[$0] }
    }

    func drawFromStillwater(
        vessel: StillwaterVessel,
        rarityRoll: Int = Int.random(in: 0..<100),
        selectionRoll: Int = Int.random(in: 0..<Int.max),
        at date: Date = Date()
    ) throws -> StillwaterDrawResult {
        do {
            let unlocked = try fetchUnlockedCreatureZones()
            guard unlocked.contains(vessel.zone) else { throw StillwaterError.locked }
            let balance = try fetchStillwaterBalance()
            guard balance >= vessel.dropCost else {
                throw StillwaterError.insufficientDrops(required: vessel.dropCost, available: balance)
            }
            let entry = StillwaterCatalog.roll(vessel: vessel, rarityRoll: rarityRoll, selectionRoll: selectionRoll)
            guard let creature = CreatureCatalog.definition(entry.creatureID),
                  creature.sourceType == .stillwater,
                  creature.zone == vessel.zone else { throw StillwaterError.unknownCreature }

            let instance = ShellFindInstance(
                id: UUID().uuidString,
                findID: creature.id,
                acquiredAt: date,
                sourceType: "stillwater",
                sourceID: vessel.rawValue,
                currentUpgradeStageID: nil,
                isNew: true,
                isArchivedInChest: true,
                viewedAt: nil,
                animalLevel: 1,
                lastActivityAt: date
            )
            let firstDiscovery = try recordCreatureDiscoveryIfNeeded(
                speciesID: creature.id,
                sourceType: .stillwater,
                instanceID: instance.id,
                discoveredAt: date,
                timestampConfidence: "EXACT"
            )
            context.insert(ShellFindInstanceModel(instance: instance))
            context.insert(CreatureLifecycleModel(instanceID: instance.id, status: .active, updatedAt: date))
            context.insert(StillwaterLedgerModel(entry: StillwaterLedgerEntry(
                id: UUID().uuidString,
                units: -vessel.dropCost,
                sourceType: "stillwater_draw",
                sourceID: instance.id,
                createdAt: date
            )))
            try reconcileCreatureAchievements(at: date)
            try context.save()
            return StillwaterDrawResult(
                instance: instance,
                creature: creature,
                rarity: entry.rarity,
                remainingDrops: balance - vessel.dropCost,
                wasFirstDiscovery: firstDiscovery
            )
        } catch {
            context.rollback()
            throw error
        }
    }

    func quoteBeyondBlueEncounter(targetCreatureID: String, selectedInstanceIDs: [String]) throws -> CreaturePaymentQuote {
        guard let target = CreatureCatalog.definition(targetCreatureID), target.sourceType == .beyondBlue,
              let requirement = target.requirementMinutes else { throw CreatureShellError.invalidEncounter }
        let unique = Set(selectedInstanceIDs)
        let selectedModels = try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).filter { unique.contains($0.id) }
        guard selectedModels.count == unique.count else { throw CreatureShellError.missingSelection }
        for model in selectedModels where try status(for: model.id) != .active { throw CreatureShellError.inactiveCreature }
        let minutes = selectedModels.reduce(0) { result, model in
            result + CreatureEconomy.flowTimeValueMinutes(model.findID)
        }
        let remaining = max(0, requirement - minutes)
        let cost = remaining * 2
        let overpay = max(0, minutes - requirement)
        let balance = try fetchPearlBalance()
        return CreaturePaymentQuote(
            targetRequirementMinutes: requirement,
            selectedCreatureMinutes: minutes,
            remainingMinutes: remaining,
            pearlCostForRemaining: cost,
            pearlReturnForOverpay: overpay,
            canEncounter: balance >= cost
        )
    }

    func encounterBeyondBlue(targetCreatureID: String, selectedInstanceIDs: [String], at date: Date = Date()) throws -> ShellFindInstance {
        do {
            let quote = try quoteBeyondBlueEncounter(targetCreatureID: targetCreatureID, selectedInstanceIDs: selectedInstanceIDs)
            let balance = try fetchPearlBalance()
            guard quote.canEncounter else { throw CreatureShellError.insufficientPearls(required: quote.pearlCostForRemaining, available: balance) }
            guard let creature = CreatureCatalog.definition(targetCreatureID), creature.sourceType == .beyondBlue else {
                throw CreatureShellError.invalidEncounter
            }
            let selected = Set(selectedInstanceIDs)
            let instanceModels = try context.fetch(FetchDescriptor<ShellFindInstanceModel>())
            for model in instanceModels where selected.contains(model.id) {
                try setStatus(.usedBeyondBlue, for: model.id, at: date)
                model.lastActivityAt = date
                for placement in try context.fetch(FetchDescriptor<ShellPlacementModel>()) where placement.instanceID == model.id {
                    context.delete(placement)
                }
            }
            if quote.pearlCostForRemaining > 0 {
                context.insert(PearlLedgerModel(entry: .init(
                    id: UUID().uuidString, delta: -quote.pearlCostForRemaining,
                    reason: "beyond_blue_encounter", sourceType: "shell_reward",
                    sourceID: targetCreatureID, createdAt: date, note: nil
                )))
            }
            if quote.pearlReturnForOverpay > 0 {
                context.insert(PearlLedgerModel(entry: .init(
                    id: UUID().uuidString, delta: quote.pearlReturnForOverpay,
                    reason: "beyond_blue_overpay_return", sourceType: "shell_reward",
                    sourceID: targetCreatureID, createdAt: date, note: nil
                )))
            }
            let instance = ShellFindInstance(
                id: UUID().uuidString, findID: targetCreatureID, acquiredAt: date,
                sourceType: "beyond_blue", sourceID: targetCreatureID,
                currentUpgradeStageID: nil, isNew: true, isArchivedInChest: true,
                viewedAt: nil, animalLevel: 1, lastActivityAt: date
            )
            context.insert(ShellFindInstanceModel(instance: instance))
            context.insert(CreatureLifecycleModel(instanceID: instance.id, status: .active, updatedAt: date))
            _ = try recordCreatureDiscoveryIfNeeded(
                speciesID: targetCreatureID, sourceType: .beyondBlue, instanceID: instance.id,
                discoveredAt: date, timestampConfidence: "EXACT"
            )
            try reconcileCreatureAchievements(at: date)
            try context.save()
            return instance
        } catch {
            context.rollback()
            throw error
        }
    }

    func growCreature(instanceID: String, transactionID: String, at date: Date = Date()) throws -> CreatureGrowthResult {
        do {
            if let receipt = try context.fetch(FetchDescriptor<CreatureActionReceiptModel>()).first(where: { $0.transactionID == transactionID }) {
                return .init(
                    instanceID: receipt.instanceID,
                    pearlCost: receipt.pearlCost,
                    resultingLevel: receipt.resultingLevel,
                    recordedMastery: try context.fetch(FetchDescriptor<CreatureMasteryModel>()).contains { $0.transactionID == transactionID }
                )
            }
            guard let model = try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).first(where: { $0.id == instanceID }),
                  CreatureCatalog.definition(model.findID) != nil else { throw CreatureShellError.missingCreature }
            guard try status(for: instanceID) == .active else { throw CreatureShellError.inactiveCreature }
            let current = max(1, model.animalLevel)
            guard current < CreatureEconomy.maxLevel else { throw CreatureShellError.mastered }
            let cost = CreatureEconomy.growthCostPearls(model.findID, currentLevel: current)
            let balance = try fetchPearlBalance()
            guard balance >= cost else { throw CreatureShellError.insufficientPearls(required: cost, available: balance) }
            let ledgerID = "pearl:\(transactionID)"
            if try !context.fetch(FetchDescriptor<PearlLedgerModel>()).contains(where: { $0.id == ledgerID }) {
                context.insert(PearlLedgerModel(entry: .init(
                    id: ledgerID, delta: -cost, reason: "grow_creature", sourceType: "shell_reward",
                    sourceID: instanceID, createdAt: date, note: nil
                )))
            }
            let resulting = current + 1
            model.animalLevel = resulting
            model.lastActivityAt = date
            _ = try recordCreatureDiscoveryIfNeeded(
                speciesID: model.findID,
                sourceType: CreatureCatalog.definition(model.findID)?.sourceType ?? .flowEarned,
                instanceID: instanceID,
                discoveredAt: model.acquiredAt,
                timestampConfidence: "EXACT"
            )
            var recordedMastery = false
            if resulting == CreatureEconomy.maxLevel,
               try !context.fetch(FetchDescriptor<CreatureMasteryModel>()).contains(where: { $0.instanceID == instanceID }) {
                recordedMastery = true
                let evidence = CreatureMasteryEvidence(
                    id: "mastery:\(instanceID)", instanceID: instanceID, speciesID: model.findID,
                    achievedAt: date, transactionID: transactionID
                )
                context.insert(CreatureMasteryModel(evidence: evidence))
                context.insert(MasteryCelebrationModel(
                    id: "celebration:\(transactionID)", instanceID: instanceID,
                    speciesID: model.findID, createdAt: date
                ))
            }
            context.insert(CreatureActionReceiptModel(
                transactionID: transactionID, actionType: "CREATURE_LEVEL_UP",
                instanceID: instanceID, pearlCost: cost, resultingLevel: resulting, createdAt: date
            ))
            try reconcileCreatureAchievements(at: date)
            try context.save()
            return .init(instanceID: instanceID, pearlCost: cost, resultingLevel: resulting, recordedMastery: recordedMastery)
        } catch {
            context.rollback()
            throw error
        }
    }

    func releaseCreature(instanceID: String, at date: Date = Date()) throws -> Int {
        try releaseCreatures(instanceIDs: [instanceID], at: date)
    }

    func releaseCreatures(instanceIDs: [String], at date: Date = Date()) throws -> Int {
        do {
            let uniqueIDs = Array(Set(instanceIDs))
            guard !uniqueIDs.isEmpty else { throw CreatureShellError.missingSelection }
            let selectedIDs = Set(uniqueIDs)
            let models = try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).filter { selectedIDs.contains($0.id) }
            guard models.count == selectedIDs.count,
                  models.allSatisfy({ CreatureCatalog.definition($0.findID) != nil }) else {
                throw CreatureShellError.missingCreature
            }
            guard try models.allSatisfy({ try status(for: $0.id) == .active }) else {
                throw CreatureShellError.inactiveCreature
            }
            let placements = try context.fetch(FetchDescriptor<ShellPlacementModel>())
            var totalPayout = 0
            for model in models {
                let payout = CreatureEconomy.releaseValuePearls(model.findID, level: model.animalLevel)
                totalPayout += payout
                try setStatus(.released, for: model.id, at: date)
                model.lastActivityAt = date
                for placement in placements where placement.instanceID == model.id {
                    context.delete(placement)
                }
                context.insert(PearlLedgerModel(entry: .init(
                    id: UUID().uuidString, delta: payout, reason: "release_creature",
                    sourceType: "shell_reward", sourceID: model.id, createdAt: date,
                    note: "Creature release"
                )))
            }
            try reconcileCreatureAchievements(at: date)
            try context.save()
            return totalPayout
        } catch {
            context.rollback()
            throw error
        }
    }

    func creatureStatus(instanceID: String) throws -> CreatureStatus { try status(for: instanceID) }

    func fetchPendingMasteryCelebration() throws -> MasteryCelebration? {
        try context.fetch(FetchDescriptor<MasteryCelebrationModel>())
            .filter { $0.acknowledgedAt == nil }
            .sorted { $0.createdAt < $1.createdAt }
            .first.map { .init(id: $0.id, instanceID: $0.instanceID, speciesID: $0.speciesID, createdAt: $0.createdAt) }
    }

    func acknowledgeMasteryCelebration(id: String, at date: Date = Date()) throws {
        guard let model = try context.fetch(FetchDescriptor<MasteryCelebrationModel>()).first(where: { $0.id == id }) else { return }
        model.acknowledgedAt = date
        try context.save()
    }

    func trackedAchievementIDs() throws -> Set<String> {
        Set(try context.fetch(FetchDescriptor<AchievementTrackingModel>()).map(\.badgeID))
    }

    func setAchievementTracked(_ badgeID: String, tracked: Bool, at date: Date = Date()) throws {
        do {
            let existing = try context.fetch(FetchDescriptor<AchievementTrackingModel>()).first { $0.badgeID == badgeID }
            if tracked {
                if existing == nil { context.insert(AchievementTrackingModel(badgeID: badgeID, trackedAt: date)) }
            } else if let existing { context.delete(existing) }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    /// Called by Flow reward materialization in the same transaction.
    @discardableResult
    func recordCreatureDiscoveryIfNeeded(
        speciesID: String,
        sourceType: CreatureSourceType,
        instanceID: String?,
        discoveredAt: Date,
        timestampConfidence: String
    ) throws -> Bool {
        guard CreatureCatalog.definition(speciesID) != nil else { return false }
        guard try !context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).contains(where: { $0.speciesID == speciesID }) else { return false }
        context.insert(CreatureDiscoveryModel(evidence: .init(
            speciesID: speciesID, firstDiscoveredAt: discoveredAt, sourceType: sourceType,
            firstInstanceID: instanceID, timestampConfidence: timestampConfidence
        )))
        return true
    }

    func reconcileCreatureAchievements(at date: Date) throws {
        let discoveries = Set(try context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).map(\.speciesID))
        let masteries = try context.fetch(FetchDescriptor<CreatureMasteryModel>())
        let masteredSpecies = Set(masteries.map(\.speciesID))
        let stillwaterDiscoveries = discoveries.filter { CreatureCatalog.definition($0)?.sourceType == .stillwater }
        let stillwaterMasteries = masteries.filter { CreatureCatalog.definition($0.speciesID)?.sourceType == .stillwater }

        try setBadgeExact("variety_collector", count: discoveries.count, at: date)
        try setBadgeExact("stillwater_first_catch", count: stillwaterDiscoveries.isEmpty ? 0 : 1, at: date)
        try setBadgeExact("stillwater_variety", count: stillwaterDiscoveries.count, at: date)
        try setBadgeExact("mastery_first", count: masteries.isEmpty ? 0 : 1, at: date)
        try setBadgeExact("mastery_circle", count: masteries.count, at: date)
        try setBadgeExact("mastery_variety", count: masteredSpecies.count, at: date)
        try setBadgeExact("stillwater_mastery", count: stillwaterMasteries.count, at: date)
        for (species, rows) in Dictionary(grouping: masteries, by: \.speciesID) {
            try setBadgeExact("mastery_species_\(species)", count: rows.count, at: date)
        }
        try persistCurrentCollectionCompletions(at: date)
    }

    private func historicalCreatureInstances() throws -> [ShellFindInstance] {
        try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).compactMap { model in
            guard CreatureCatalog.definition(model.findID) != nil else { return nil }
            return Self.creatureInstance(model)
        }
    }

    private func allCollectionProgress() throws -> [String: StillwaterCollectionProgress] {
        let discoveries = Set(try context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).map(\.speciesID))
        let masteries = Set(try context.fetch(FetchDescriptor<CreatureMasteryModel>()).map(\.speciesID))
        let lifecycle = Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<CreatureLifecycleModel>()).map { ($0.instanceID, CreatureStatus(rawValue: $0.statusRawValue) ?? .active) })
        let owned: Set<String> = Set(try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).compactMap { model -> String? in
            guard CreatureCatalog.definition(model.findID) != nil,
                  lifecycle[model.id, default: .active] == .active else { return nil }
            return model.findID
        })
        let completions = try context.fetch(FetchDescriptor<CollectionCompletionModel>())
        var result: [String: StillwaterCollectionProgress] = [:]
        for id in CollectionRosterCatalog.allCollectionIDs {
            let roster = Set(CollectionRosterCatalog.roster(for: id))
            let title = CollectionRosterCatalog.title(for: id)
            result[id] = .init(
                id: id, title: title, total: roster.count,
                discovered: roster.intersection(discoveries).count,
                owned: roster.intersection(owned).count,
                mastered: roster.intersection(masteries).count,
                collectorEarned: completions.contains { $0.collectionID == id && $0.typeRawValue == CollectionCompletionType.collector.rawValue },
                curatorEarned: completions.contains { $0.collectionID == id && $0.typeRawValue == CollectionCompletionType.curator.rawValue },
                completionistEarned: completions.contains { $0.collectionID == id && $0.typeRawValue == CollectionCompletionType.completionist.rawValue }
            )
        }
        return result
    }

    private func persistCurrentCollectionCompletions(at date: Date) throws {
        let discoveries = Set(try context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).map(\.speciesID))
        let masteries = Set(try context.fetch(FetchDescriptor<CreatureMasteryModel>()).map(\.speciesID))
        let lifecycle = Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<CreatureLifecycleModel>()).map { ($0.instanceID, CreatureStatus(rawValue: $0.statusRawValue) ?? .active) })
        let owned = Set(try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).compactMap {
            lifecycle[$0.id, default: .active] == .active ? $0.findID : nil
        })
        let existing = Set(try context.fetch(FetchDescriptor<CollectionCompletionModel>()).map(\.id))
        for collectionID in CollectionRosterCatalog.allCollectionIDs {
            let required = CollectionRosterCatalog.roster(for: collectionID).sorted()
            guard !required.isEmpty else { continue }
            let roster = Set(required)
            let checks: [(CollectionCompletionType, Bool)] = [
                (.collector, roster.isSubset(of: discoveries)),
                (.curator, roster.isSubset(of: owned)),
                (.completionist, roster.isSubset(of: masteries))
            ]
            let hash = CollectionRosterCatalog.rosterHash(required)
            for (type, complete) in checks where complete {
                let id = "\(collectionID):\(type.rawValue):v1:\(hash)"
                guard !existing.contains(id) else { continue }
                context.insert(CollectionCompletionModel(evidence: .init(
                    id: id, collectionID: collectionID, type: type, completedAt: date,
                    rosterVersion: 1, rosterHash: hash, requiredSpeciesIDs: required
                )))
                try setBadgeExact("\(collectionID)_\(type.rawValue.lowercased())", count: 1, at: date)
            }
        }
    }

    private func setBadgeExact(_ badgeID: String, count: Int, at date: Date) throws {
        guard count > 0 else { return }
        let floor = try context.fetch(FetchDescriptor<ShellBadgeCountFloorModel>()).first { $0.badgeID == badgeID }?.minimumCount ?? 0
        let desired = max(count, floor)
        if let model = try context.fetch(FetchDescriptor<ShellBadgeModel>()).first(where: { $0.badgeID == badgeID }) {
            guard model.count < desired else { return }
            model.count = desired
            model.lastEarnedAt = date
            model.isNew = true
        } else {
            context.insert(ShellBadgeModel(badgeID: badgeID, count: desired, earnedAt: date))
        }
    }

    private func status(for instanceID: String) throws -> CreatureStatus {
        let raw = try context.fetch(FetchDescriptor<CreatureLifecycleModel>()).first { $0.instanceID == instanceID }?.statusRawValue
        return raw.flatMap(CreatureStatus.init(rawValue:)) ?? .active
    }

    private func setStatus(_ status: CreatureStatus, for instanceID: String, at date: Date) throws {
        if let model = try context.fetch(FetchDescriptor<CreatureLifecycleModel>()).first(where: { $0.instanceID == instanceID }) {
            model.statusRawValue = status.rawValue
            model.updatedAt = date
        } else {
            context.insert(CreatureLifecycleModel(instanceID: instanceID, status: status, updatedAt: date))
        }
    }

    private static func creatureInstance(_ model: ShellFindInstanceModel) -> ShellFindInstance {
        .init(id: model.id, findID: model.findID, acquiredAt: model.acquiredAt,
              sourceType: model.sourceType, sourceID: model.sourceID,
              currentUpgradeStageID: model.currentUpgradeStageID, isNew: model.isNew,
              isArchivedInChest: model.isArchivedInChest, viewedAt: model.viewedAt,
              animalLevel: model.animalLevel, lastActivityAt: model.lastActivityAt)
    }

    private static func discoveryEvidence(_ model: CreatureDiscoveryModel) -> CreatureDiscoveryEvidence {
        .init(speciesID: model.speciesID, firstDiscoveredAt: model.firstDiscoveredAt,
              sourceType: CreatureSourceType(rawValue: model.sourceTypeRawValue) ?? .flowEarned,
              firstInstanceID: model.firstInstanceID, timestampConfidence: model.timestampConfidence)
    }

    private static func masteryEvidence(_ model: CreatureMasteryModel) -> CreatureMasteryEvidence {
        .init(id: model.id, instanceID: model.instanceID, speciesID: model.speciesID,
              achievedAt: model.achievedAt, transactionID: model.transactionID,
              timestampConfidence: model.timestampConfidence)
    }
}

enum CollectionRosterCatalog {
    static let stillwaterVesselIDs = StillwaterVessel.allCases.map { "stillwater_\($0.rawValue)" }
    static let stillwaterCollectionIDs = ["collection_stillwater"] + stillwaterVesselIDs
    static let blueZoneIDs = ShellDepthTier.allCases.map { "blue_\($0.rawValue.snakeCasedForCollection)" }
    // Android's CollectionCatalog order is the user-facing Badge Book order:
    // Blue regions, Stillwater vessels, then aggregate collections.
    static let allCollectionIDs = blueZoneIDs + stillwaterVesselIDs + [
        "collection_stillwater", "collection_the_blue", "collection_all_waters"
    ]

    static func roster(for collectionID: String) -> [String] {
        switch collectionID {
        case "collection_stillwater": return CreatureCatalog.stillwaterCreatures.filter(\.participatesInCollector).map(\.id)
        case "collection_the_blue": return CreatureCatalog.all.filter { $0.sourceType != .stillwater && $0.participatesInCollector }.map(\.id)
        case "collection_all_waters": return CreatureCatalog.all.filter(\.participatesInCollector).map(\.id)
        default:
            if collectionID.hasPrefix("stillwater_"),
               let vessel = StillwaterVessel(rawValue: String(collectionID.dropFirst("stillwater_".count))) {
                return StillwaterCatalog.creatures(for: vessel).map(\.id)
            }
            if collectionID.hasPrefix("blue_"),
               let zone = ShellDepthTier.allCases.first(where: { "blue_\($0.rawValue.snakeCasedForCollection)" == collectionID }) {
                return CreatureCatalog.all.filter { $0.sourceType != .stillwater && $0.zone == zone && $0.participatesInCollector }.map(\.id)
            }
            return []
        }
    }

    static func title(for collectionID: String) -> String {
        switch collectionID {
        case "collection_stillwater": return "All Stillwater"
        case "collection_the_blue": return "The Blue"
        case "collection_all_waters": return "All Waters"
        default:
            if collectionID.hasPrefix("stillwater_") {
                return String(collectionID.dropFirst("stillwater_".count)).capitalized
            }
            if collectionID.hasPrefix("blue_") {
                return String(collectionID.dropFirst("blue_".count)).replacingOccurrences(of: "_", with: " ").capitalized
            }
            return collectionID
        }
    }

    static func rosterHash(_ values: [String]) -> String {
        values.sorted().joined(separator: "|").utf8.reduce(UInt64(14_695_981_039_346_656_037)) {
            ($0 ^ UInt64($1)) &* 1_099_511_628_211
        }.description
    }
}

private extension String {
    var snakeCasedForCollection: String {
        unicodeScalars.reduce(into: "") { result, scalar in
            if CharacterSet.uppercaseLetters.contains(scalar), !result.isEmpty { result.append("_") }
            result.append(String(scalar).lowercased())
        }
    }
}

extension InMemoryFlowRepository {
    func fetchStillwaterPerspective() -> StillwaterPerspective { stillwaterPerspective }

    func saveStillwaterPerspective(_ perspective: StillwaterPerspective, at date: Date = Date()) {
        stillwaterPerspective = perspective
    }

    func fetchUnlockedCreatureZones() -> Set<ShellDepthTier> {
        CreatureUnlockPolicy.unlockedZones(from: shellFindInstances)
    }

    func fetchCreatureDiscoveries() -> [CreatureDiscoveryEvidence] {
        creatureDiscoveries.values.sorted { $0.firstDiscoveredAt < $1.firstDiscoveredAt }
    }

    func fetchCreatureMasteries() -> [CreatureMasteryEvidence] {
        creatureMasteries.sorted { $0.achievedAt < $1.achievedAt }
    }

    func fetchStillwaterCollectionProgress() -> [StillwaterCollectionProgress] {
        let values = inMemoryCollectionProgress()
        return CollectionRosterCatalog.stillwaterCollectionIDs.compactMap { values[$0] }
    }

    func fetchCreatureCollectionProgress() -> [StillwaterCollectionProgress] {
        let values = inMemoryCollectionProgress()
        return CollectionRosterCatalog.allCollectionIDs.compactMap { values[$0] }
    }

    func drawFromStillwater(
        vessel: StillwaterVessel,
        rarityRoll: Int = Int.random(in: 0..<100),
        selectionRoll: Int = Int.random(in: 0..<Int.max),
        at date: Date = Date()
    ) throws -> StillwaterDrawResult {
        guard fetchUnlockedCreatureZones().contains(vessel.zone) else { throw StillwaterError.locked }
        let balance = fetchStillwaterBalance()
        guard balance >= vessel.dropCost else {
            throw StillwaterError.insufficientDrops(required: vessel.dropCost, available: balance)
        }
        let entry = StillwaterCatalog.roll(vessel: vessel, rarityRoll: rarityRoll, selectionRoll: selectionRoll)
        guard let creature = CreatureCatalog.definition(entry.id), creature.sourceType == .stillwater,
              creature.zone == vessel.zone else { throw StillwaterError.unknownCreature }
        let instance = ShellFindInstance(
            id: UUID().uuidString, findID: creature.id, acquiredAt: date,
            sourceType: "stillwater", sourceID: vessel.rawValue, currentUpgradeStageID: nil,
            isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date
        )
        let firstDiscovery = creatureDiscoveries[creature.id] == nil
        shellFindInstances.append(instance)
        shellCreatureStatuses[instance.id] = .active
        if firstDiscovery {
            creatureDiscoveries[creature.id] = .init(
                speciesID: creature.id, firstDiscoveredAt: date, sourceType: .stillwater,
                firstInstanceID: instance.id, timestampConfidence: "EXACT"
            )
        }
        stillwaterLedger.append(.init(
            id: UUID().uuidString, units: -vessel.dropCost,
            sourceType: "stillwater_draw", sourceID: instance.id, createdAt: date
        ))
        reconcileCreatureAchievementsInMemory(at: date)
        return .init(instance: instance, creature: creature, rarity: entry.rarity,
                     remainingDrops: balance - vessel.dropCost, wasFirstDiscovery: firstDiscovery)
    }

    func quoteBeyondBlueEncounter(targetCreatureID: String, selectedInstanceIDs: [String]) throws -> CreaturePaymentQuote {
        guard let target = CreatureCatalog.definition(targetCreatureID), target.sourceType == .beyondBlue,
              let requirement = target.requirementMinutes else { throw CreatureShellError.invalidEncounter }
        let unique = Set(selectedInstanceIDs)
        let selected = shellFindInstances.filter { unique.contains($0.id) }
        guard selected.count == unique.count else { throw CreatureShellError.missingSelection }
        guard selected.allSatisfy({ shellCreatureStatuses[$0.id, default: .active] == .active && CreatureCatalog.definition($0.findID) != nil }) else {
            throw CreatureShellError.inactiveCreature
        }
        let minutes = selected.reduce(0) { $0 + CreatureEconomy.flowTimeValueMinutes($1.findID) }
        let remaining = max(0, requirement - minutes)
        let cost = remaining * 2
        return .init(targetRequirementMinutes: requirement, selectedCreatureMinutes: minutes,
                     remainingMinutes: remaining, pearlCostForRemaining: cost,
                     pearlReturnForOverpay: max(0, minutes - requirement),
                     canEncounter: fetchPearlBalance() >= cost)
    }

    func encounterBeyondBlue(targetCreatureID: String, selectedInstanceIDs: [String], at date: Date = Date()) throws -> ShellFindInstance {
        let quote = try quoteBeyondBlueEncounter(targetCreatureID: targetCreatureID, selectedInstanceIDs: selectedInstanceIDs)
        let balance = fetchPearlBalance()
        guard quote.canEncounter else { throw CreatureShellError.insufficientPearls(required: quote.pearlCostForRemaining, available: balance) }
        let selected = Set(selectedInstanceIDs)
        for index in shellFindInstances.indices where selected.contains(shellFindInstances[index].id) {
            shellCreatureStatuses[shellFindInstances[index].id] = .usedBeyondBlue
            shellFindInstances[index].lastActivityAt = date
            shellPlacements.removeAll { $0.instanceID == shellFindInstances[index].id }
        }
        if quote.pearlCostForRemaining > 0 {
            pearlLedger.append(.init(id: UUID().uuidString, delta: -quote.pearlCostForRemaining,
                                     reason: "beyond_blue_encounter", sourceType: "shell_reward",
                                     sourceID: targetCreatureID, createdAt: date, note: nil))
        }
        if quote.pearlReturnForOverpay > 0 {
            pearlLedger.append(.init(id: UUID().uuidString, delta: quote.pearlReturnForOverpay,
                                     reason: "beyond_blue_overpay_return", sourceType: "shell_reward",
                                     sourceID: targetCreatureID, createdAt: date, note: nil))
        }
        let instance = ShellFindInstance(
            id: UUID().uuidString, findID: targetCreatureID, acquiredAt: date,
            sourceType: "beyond_blue", sourceID: targetCreatureID, currentUpgradeStageID: nil,
            isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date
        )
        shellFindInstances.append(instance)
        shellCreatureStatuses[instance.id] = .active
        if creatureDiscoveries[targetCreatureID] == nil {
            creatureDiscoveries[targetCreatureID] = .init(
                speciesID: targetCreatureID, firstDiscoveredAt: date, sourceType: .beyondBlue,
                firstInstanceID: instance.id, timestampConfidence: "EXACT"
            )
        }
        reconcileCreatureAchievementsInMemory(at: date)
        return instance
    }

    func growCreature(instanceID: String, transactionID: String, at date: Date = Date()) throws -> CreatureGrowthResult {
        guard let index = shellFindInstances.firstIndex(where: { $0.id == instanceID }),
              CreatureCatalog.definition(shellFindInstances[index].findID) != nil else { throw CreatureShellError.missingCreature }
        guard shellCreatureStatuses[instanceID, default: .active] == .active else { throw CreatureShellError.inactiveCreature }
        if let committed = creatureActionReceipts[transactionID] { return committed }
        let current = max(1, shellFindInstances[index].animalLevel)
        guard current < CreatureEconomy.maxLevel else { throw CreatureShellError.mastered }
        let cost = CreatureEconomy.growthCostPearls(shellFindInstances[index].findID, currentLevel: current)
        let balance = fetchPearlBalance()
        guard balance >= cost else { throw CreatureShellError.insufficientPearls(required: cost, available: balance) }
        let ledgerID = "pearl:\(transactionID)"
        if !pearlLedger.contains(where: { $0.id == ledgerID }) {
            pearlLedger.append(.init(id: ledgerID, delta: -cost, reason: "grow_creature",
                                     sourceType: "shell_reward", sourceID: instanceID,
                                     createdAt: date, note: nil))
        }
        shellFindInstances[index].animalLevel = current + 1
        shellFindInstances[index].lastActivityAt = date
        let species = shellFindInstances[index].findID
        if creatureDiscoveries[species] == nil, let definition = CreatureCatalog.definition(species) {
            creatureDiscoveries[species] = .init(
                speciesID: species, firstDiscoveredAt: shellFindInstances[index].acquiredAt,
                sourceType: definition.sourceType, firstInstanceID: instanceID,
                timestampConfidence: "EXACT"
            )
        }
        var recorded = false
        if current + 1 == CreatureEconomy.maxLevel,
           !creatureMasteries.contains(where: { $0.instanceID == instanceID }) {
            recorded = true
            creatureMasteries.append(.init(id: "mastery:\(instanceID)", instanceID: instanceID,
                                            speciesID: species, achievedAt: date, transactionID: transactionID))
            masteryCelebrations.append(.init(id: "celebration:\(transactionID)", instanceID: instanceID,
                                             speciesID: species, createdAt: date))
        }
        reconcileCreatureAchievementsInMemory(at: date)
        let result = CreatureGrowthResult(instanceID: instanceID, pearlCost: cost, resultingLevel: current + 1, recordedMastery: recorded)
        creatureActionReceipts[transactionID] = result
        return result
    }

    func releaseCreature(instanceID: String, at date: Date = Date()) throws -> Int {
        try releaseCreatures(instanceIDs: [instanceID], at: date)
    }

    func releaseCreatures(instanceIDs: [String], at date: Date = Date()) throws -> Int {
        let selectedIDs = Set(instanceIDs)
        guard !selectedIDs.isEmpty else { throw CreatureShellError.missingSelection }
        let selected = shellFindInstances.filter { selectedIDs.contains($0.id) }
        guard selected.count == selectedIDs.count,
              selected.allSatisfy({ CreatureCatalog.definition($0.findID) != nil }) else {
            throw CreatureShellError.missingCreature
        }
        guard selected.allSatisfy({ shellCreatureStatuses[$0.id, default: .active] == .active }) else {
            throw CreatureShellError.inactiveCreature
        }
        let payout = selected.reduce(0) {
            $0 + CreatureEconomy.releaseValuePearls($1.findID, level: $1.animalLevel)
        }
        for index in shellFindInstances.indices where selectedIDs.contains(shellFindInstances[index].id) {
            let instance = shellFindInstances[index]
            let instancePayout = CreatureEconomy.releaseValuePearls(instance.findID, level: instance.animalLevel)
            shellCreatureStatuses[instance.id] = .released
            shellFindInstances[index].lastActivityAt = date
            pearlLedger.append(.init(id: UUID().uuidString, delta: instancePayout, reason: "release_creature",
                                     sourceType: "shell_reward", sourceID: instance.id,
                                     createdAt: date, note: "Creature release"))
        }
        shellPlacements.removeAll { selectedIDs.contains($0.instanceID) }
        reconcileCreatureAchievementsInMemory(at: date)
        return payout
    }

    func creatureStatus(instanceID: String) -> CreatureStatus {
        shellCreatureStatuses[instanceID, default: .active]
    }

    func fetchPendingMasteryCelebration() -> MasteryCelebration? { masteryCelebrations.first }
    func acknowledgeMasteryCelebration(id: String, at date: Date = Date()) { masteryCelebrations.removeAll { $0.id == id } }
    func trackedAchievementIDs() -> Set<String> { trackedAchievements }
    func setAchievementTracked(_ badgeID: String, tracked: Bool, at date: Date = Date()) {
        if tracked { trackedAchievements.insert(badgeID) } else { trackedAchievements.remove(badgeID) }
    }

    func recordCreatureDiscoveryInMemoryIfNeeded(
        speciesID: String,
        instanceID: String?,
        discoveredAt: Date
    ) {
        guard creatureDiscoveries[speciesID] == nil,
              let definition = CreatureCatalog.definition(speciesID) else { return }
        creatureDiscoveries[speciesID] = .init(
            speciesID: speciesID, firstDiscoveredAt: discoveredAt,
            sourceType: definition.sourceType, firstInstanceID: instanceID,
            timestampConfidence: "EXACT"
        )
        reconcileCreatureAchievementsInMemory(at: discoveredAt)
    }

    private func inMemoryCollectionProgress() -> [String: StillwaterCollectionProgress] {
        let discoveries = Set(creatureDiscoveries.keys)
        let masteries = Set(creatureMasteries.map(\.speciesID))
        let owned = Set(shellFindInstances.compactMap {
            shellCreatureStatuses[$0.id, default: .active] == .active ? $0.findID : nil
        })
        var result: [String: StillwaterCollectionProgress] = [:]
        for id in CollectionRosterCatalog.allCollectionIDs {
            let roster = Set(CollectionRosterCatalog.roster(for: id))
            result[id] = .init(
                id: id, title: CollectionRosterCatalog.title(for: id), total: roster.count,
                discovered: roster.intersection(discoveries).count,
                owned: roster.intersection(owned).count,
                mastered: roster.intersection(masteries).count,
                collectorEarned: collectionCompletions.contains { $0.collectionID == id && $0.type == .collector },
                curatorEarned: collectionCompletions.contains { $0.collectionID == id && $0.type == .curator },
                completionistEarned: collectionCompletions.contains { $0.collectionID == id && $0.type == .completionist }
            )
        }
        return result
    }

    func reconcileCreatureAchievementsInMemory(at date: Date) {
        let discoveries = Set(creatureDiscoveries.keys)
        let mastered = Set(creatureMasteries.map(\.speciesID))
        let stillwaterDiscoveries = discoveries.filter { CreatureCatalog.definition($0)?.sourceType == .stillwater }
        let stillwaterMasteries = creatureMasteries.filter { CreatureCatalog.definition($0.speciesID)?.sourceType == .stillwater }
        setBadgeExactInMemory("variety_collector", discoveries.count, date)
        setBadgeExactInMemory("stillwater_first_catch", stillwaterDiscoveries.isEmpty ? 0 : 1, date)
        setBadgeExactInMemory("stillwater_variety", stillwaterDiscoveries.count, date)
        setBadgeExactInMemory("mastery_first", creatureMasteries.isEmpty ? 0 : 1, date)
        setBadgeExactInMemory("mastery_circle", creatureMasteries.count, date)
        setBadgeExactInMemory("mastery_variety", mastered.count, date)
        setBadgeExactInMemory("stillwater_mastery", stillwaterMasteries.count, date)
        for (species, rows) in Dictionary(grouping: creatureMasteries, by: \.speciesID) {
            setBadgeExactInMemory("mastery_species_\(species)", rows.count, date)
        }
        persistInMemoryCollectionCompletions(at: date)
    }

    private func persistInMemoryCollectionCompletions(at date: Date) {
        let discoveries = Set(creatureDiscoveries.keys)
        let mastered = Set(creatureMasteries.map(\.speciesID))
        let owned = Set(shellFindInstances.compactMap {
            shellCreatureStatuses[$0.id, default: .active] == .active ? $0.findID : nil
        })
        for id in CollectionRosterCatalog.allCollectionIDs {
            let required = CollectionRosterCatalog.roster(for: id).sorted()
            guard !required.isEmpty else { continue }
            let roster = Set(required)
            for (type, complete) in [
                (CollectionCompletionType.collector, roster.isSubset(of: discoveries)),
                (.curator, roster.isSubset(of: owned)),
                (.completionist, roster.isSubset(of: mastered))
            ] where complete {
                let hash = CollectionRosterCatalog.rosterHash(required)
                let evidenceID = "\(id):\(type.rawValue):v1:\(hash)"
                guard !collectionCompletions.contains(where: { $0.id == evidenceID }) else { continue }
                collectionCompletions.append(.init(
                    id: evidenceID, collectionID: id, type: type, completedAt: date,
                    rosterVersion: 1, rosterHash: hash, requiredSpeciesIDs: required
                ))
                setBadgeExactInMemory("\(id)_\(type.rawValue.lowercased())", 1, date)
            }
        }
    }

    private func setBadgeExactInMemory(_ id: String, _ count: Int, _ date: Date) {
        guard count > 0 else { return }
        let desired = max(count, shellBadgeCountFloors[id, default: 0])
        if let current = shellBadges[id] {
            guard current.count < desired else { return }
            shellBadges[id] = .init(badgeID: id, count: desired,
                                    firstEarnedAt: current.firstEarnedAt,
                                    lastEarnedAt: date, isNew: true)
        } else {
            shellBadges[id] = .init(badgeID: id, count: desired,
                                    firstEarnedAt: date, lastEarnedAt: date, isNew: true)
        }
    }
}
