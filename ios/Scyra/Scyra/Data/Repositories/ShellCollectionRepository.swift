import Foundation
import SwiftData

@MainActor
protocol ShellCollectionRepository: AnyObject {
    func fetchShellFindInstances() throws -> [ShellFindInstance]
    func fetchShellFindStacks() throws -> [ShellFindStack]
    func fetchShellPlacements(roomID: String) throws -> [ShellPlacement]
    func fetchShellFindUpgrades(instanceID: String) throws -> [ShellFindUpgrade]
    func fetchAchievementDashboard() throws -> AchievementDashboard
    func fetchShellNotifications() throws -> [ShellNotificationItem]
    func reconcileShellCollections(version: Int, at date: Date) throws -> ShellCollectionBackfillResult
    func invitePearlObjectToChest(findID: String, at date: Date) throws -> ShellFindInstance
    func placeShellFind(instanceID: String, roomID: String, slotID: String, at date: Date) throws
    func returnShellFindToChest(instanceID: String, at date: Date) throws
    func upgradeShellFind(instanceID: String, at date: Date) throws -> ShellFindUpgrade
    func pinAchievement(badgeID: String, replacing: String?, at date: Date) throws -> ShellPinResult
    func unpinAchievement(badgeID: String) throws
    func markShellNotificationViewed(id: String, at date: Date) throws
    func markAllShellNotificationsViewed(at date: Date) throws
    func setAchievementCountFloor(badgeID: String, minimumCount: Int, source: String, at date: Date) throws
}

extension SwiftDataFlowRepository {
    func fetchShellFindInstances() throws -> [ShellFindInstance] {
        let statuses = Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<CreatureLifecycleModel>()).map { ($0.instanceID, CreatureStatus(rawValue: $0.statusRawValue) ?? .active) })
        return try context.fetch(FetchDescriptor<ShellFindInstanceModel>())
            .filter { statuses[$0.id, default: .active] == .active }
            .map(Self.shellFindInstance)
            .sorted { $0.acquiredAt > $1.acquiredAt }
    }

    func fetchShellFindStacks() throws -> [ShellFindStack] {
        try context.fetch(FetchDescriptor<ShellFindStackModel>())
            .map(Self.shellFindStack)
            .sorted { $0.lastAcquiredAt > $1.lastAcquiredAt }
    }

    func fetchShellPlacements(roomID: String) throws -> [ShellPlacement] {
        try context.fetch(FetchDescriptor<ShellPlacementModel>())
            .filter { $0.roomID == roomID }
            .map(Self.shellPlacement)
            .sorted { $0.placedAt > $1.placedAt }
    }

    func fetchShellFindUpgrades(instanceID: String) throws -> [ShellFindUpgrade] {
        try context.fetch(FetchDescriptor<ShellFindUpgradeModel>())
            .filter { $0.instanceID == instanceID }
            .map(Self.shellUpgrade)
            .sorted { $0.upgradedAt > $1.upgradedAt }
    }

    func fetchAchievementDashboard() throws -> AchievementDashboard {
        let floors = Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<ShellBadgeCountFloorModel>()).map { ($0.badgeID, $0.minimumCount) })
        let pins = Dictionary(uniqueKeysWithValues: try context.fetch(FetchDescriptor<ShellBadgePinModel>()).map { ($0.badgeID, $0.pinOrder) })
        return AchievementDashboardCalculator.calculate(badges: try fetchShellBadges(), countFloors: floors, pins: pins)
    }

    func fetchShellNotifications() throws -> [ShellNotificationItem] {
        let finds = try fetchShellFindInstances().filter(\.isNew).compactMap { instance -> ShellNotificationItem? in
            guard let definition = ShellContentCatalog.definition(instance.findID) else { return nil }
            return ShellNotificationItem(
                id: "FIND:\(instance.id)", kind: .find, sourceID: instance.id,
                title: definition.title, detail: definition.depth?.title ?? "New Shell find", occurredAt: instance.acquiredAt
            )
        }
        let badges = try fetchShellBadges().filter(\.isNew).map { badge in
            let definition = AchievementCatalog.resolve(badge.badgeID)
            return ShellNotificationItem(
                id: "BADGE:\(badge.badgeID)", kind: .badge, sourceID: badge.badgeID,
                title: definition.title, detail: badge.count == 1 ? "Achievement earned" : "Count advanced to \(badge.count)",
                occurredAt: badge.lastEarnedAt
            )
        }
        return (finds + badges).sorted { $0.occurredAt > $1.occurredAt }
    }

    func reconcileShellCollections(version: Int = 1, at date: Date = Date()) throws -> ShellCollectionBackfillResult {
        if let receipt = try context.fetch(FetchDescriptor<ShellCollectionBackfillModel>()).first(where: { $0.version == version }) {
            return ShellCollectionBackfillResult(version: version, instanceCount: receipt.instanceCount, badgeCount: receipt.badgeCount, alreadyCompleted: true)
        }
        do {
            let existingIDs = Set(try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).map(\.id))
            var insertedInstances = 0
            for grant in try context.fetch(FetchDescriptor<ShellFindGrantModel>()) where !existingIDs.contains(grant.id) {
                let definition = ShellContentCatalog.definition(grant.findID)
                if definition?.stackable == true {
                    try addShellStack(findID: grant.findID, quantity: 1, acquiredAt: grant.acquiredAt, markNew: false, viewedAt: date)
                } else {
                    context.insert(ShellFindInstanceModel(instance: ShellFindInstance(
                        id: grant.id, findID: grant.findID, acquiredAt: grant.acquiredAt,
                        sourceType: "session", sourceID: grant.sourceSessionID.uuidString,
                        currentUpgradeStageID: ShellContentCatalog.upgrades(for: grant.findID).first?.id,
                        isNew: false, isArchivedInChest: true, viewedAt: date,
                        animalLevel: 1, lastActivityAt: grant.acquiredAt
                    )))
                    insertedInstances += 1
                }
            }

            let badgeEvents = try context.fetch(FetchDescriptor<ShellRewardEventModel>())
                .filter { $0.typeRawValue == ShellRewardEventType.badgeUpdated.rawValue && $0.rewardID != nil }
            let eventGroups = Dictionary(grouping: badgeEvents, by: { $0.rewardID ?? "" })
            var materializedBadges = 0
            let storedBadges = try context.fetch(FetchDescriptor<ShellBadgeModel>())
            for (badgeID, events) in eventGroups where !badgeID.isEmpty {
                let count = events.reduce(0) { $0 + Int($1.quantity) }
                let first = events.map(\.occurredAt).min() ?? date
                let last = events.map(\.occurredAt).max() ?? date
                if let badge = storedBadges.first(where: { $0.badgeID == badgeID }) {
                    badge.count = max(badge.count, count)
                    badge.firstEarnedAt = min(badge.firstEarnedAt, first)
                    badge.lastEarnedAt = max(badge.lastEarnedAt, last)
                } else if count > 0 {
                    context.insert(ShellBadgeModel(badgeID: badgeID, count: count, earnedAt: first, isNew: false))
                    materializedBadges += 1
                }
                try upsertCountFloor(badgeID: badgeID, minimumCount: count, verifiedCount: count, source: "reward_event_backfill", at: date)
            }

            if version >= 2 {
                let badgeIDsBefore = Set(try context.fetch(FetchDescriptor<ShellBadgeModel>()).map(\.badgeID))
                let discoveriesBefore = Set(try context.fetch(FetchDescriptor<CreatureDiscoveryModel>()).map(\.speciesID))
                let masteriesBefore = Set(try context.fetch(FetchDescriptor<CreatureMasteryModel>()).map(\.instanceID))
                for find in try context.fetch(FetchDescriptor<ShellFindInstanceModel>()) {
                    guard let creature = CreatureCatalog.definition(find.findID) else { continue }
                    if !discoveriesBefore.contains(find.findID) {
                        _ = try recordCreatureDiscoveryIfNeeded(
                            speciesID: find.findID, sourceType: creature.sourceType,
                            instanceID: find.id, discoveredAt: find.acquiredAt,
                            timestampConfidence: "ESTIMATED_FROM_ACQUISITION"
                        )
                    }
                    if find.animalLevel >= CreatureEconomy.maxLevel,
                       !masteriesBefore.contains(find.id),
                       try !context.fetch(FetchDescriptor<CreatureMasteryModel>()).contains(where: { $0.instanceID == find.id }) {
                        context.insert(CreatureMasteryModel(evidence: .init(
                            id: "mastery:\(find.id)", instanceID: find.id, speciesID: find.findID,
                            achievedAt: find.lastActivityAt, transactionID: "backfill:mastery:\(find.id)",
                            timestampConfidence: "ESTIMATED_FROM_ACQUISITION"
                        )))
                    }
                }
                try reconcileCreatureAchievements(at: date)
                for badge in try context.fetch(FetchDescriptor<ShellBadgeModel>())
                where !badgeIDsBefore.contains(badge.badgeID) && Self.isCreatureEvidenceBadge(badge.badgeID) {
                    badge.isNew = false
                }
            }
            context.insert(ShellCollectionBackfillModel(
                version: version, completedAt: date, instanceCount: insertedInstances, badgeCount: materializedBadges
            ))
            try context.save()
            return ShellCollectionBackfillResult(version: version, instanceCount: insertedInstances, badgeCount: materializedBadges, alreadyCompleted: false)
        } catch {
            context.rollback()
            throw error
        }
    }

    func invitePearlObjectToChest(findID: String, at date: Date = Date()) throws -> ShellFindInstance {
        do {
            guard let definition = ShellContentCatalog.definition(findID), definition.isPearlObject,
                  let cost = definition.pearlCost else { throw ShellCollectionError.missingFind }
            let balance = try fetchPearlBalance()
            guard balance >= cost else { throw ShellCollectionError.insufficientPearls(required: cost, available: balance) }
            let instance = ShellFindInstance(
                id: UUID().uuidString, findID: findID, acquiredAt: date,
                sourceType: "pearl_basin", sourceID: nil,
                currentUpgradeStageID: ShellContentCatalog.upgrades(for: findID).first?.id,
                isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date
            )
            context.insert(PearlLedgerModel(entry: PearlLedgerEntry(
                id: "shell_reward:\(instance.id):invite_object", delta: -cost, reason: "invite_object",
                sourceType: "shell_reward", sourceID: instance.id, createdAt: date, note: nil
            )))
            context.insert(ShellFindInstanceModel(instance: instance))
            try context.save()
            return instance
        } catch {
            context.rollback()
            throw error
        }
    }

    func placeShellFind(instanceID: String, roomID: String = "FOCUS", slotID: String, at date: Date = Date()) throws {
        do {
            let instances = try context.fetch(FetchDescriptor<ShellFindInstanceModel>())
            guard let instance = instances.first(where: { $0.id == instanceID }),
                  let definition = ShellContentCatalog.definition(instance.findID) else { throw ShellCollectionError.missingFind }
            guard definition.placeable else { throw ShellCollectionError.notPlaceable }
            guard roomID == "FOCUS", let slot = ShellContentCatalog.focusSlots.first(where: { $0.id == slotID }) else { throw ShellCollectionError.invalidSlot }
            guard ShellContentCatalog.isCompatible(slot: slot, find: definition) else { throw ShellCollectionError.incompatibleSlot }
            let placements = try context.fetch(FetchDescriptor<ShellPlacementModel>())
            if let unchanged = placements.first(where: { $0.roomID == roomID && $0.slotID == slotID && $0.instanceID == instanceID }) {
                _ = unchanged
                return
            }
            for placement in placements where placement.instanceID == instanceID || (placement.roomID == roomID && placement.slotID == slotID) {
                if let displaced = instances.first(where: { $0.id == placement.instanceID }) {
                    displaced.isArchivedInChest = true
                    displaced.lastActivityAt = date
                }
                context.delete(placement)
            }
            instance.isArchivedInChest = false
            instance.lastActivityAt = date
            context.insert(ShellPlacementModel(placement: ShellPlacement(
                id: UUID().uuidString, roomID: roomID, slotID: slotID, instanceID: instanceID, placedAt: date
            )))
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func returnShellFindToChest(instanceID: String, at date: Date = Date()) throws {
        do {
            let instances = try context.fetch(FetchDescriptor<ShellFindInstanceModel>())
            guard let instance = instances.first(where: { $0.id == instanceID }) else { throw ShellCollectionError.missingFind }
            for placement in try context.fetch(FetchDescriptor<ShellPlacementModel>()) where placement.instanceID == instanceID {
                context.delete(placement)
            }
            instance.isArchivedInChest = true
            instance.lastActivityAt = date
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func upgradeShellFind(instanceID: String, at date: Date = Date()) throws -> ShellFindUpgrade {
        do {
            guard let instance = try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).first(where: { $0.id == instanceID }),
                  let definition = ShellContentCatalog.definition(instance.findID), definition.upgradeable else { throw ShellCollectionError.noUpgrade }
            guard definition.kind != .animal,
                  let next = ShellContentCatalog.nextUpgrade(findID: instance.findID, currentStageID: instance.currentUpgradeStageID) else { throw ShellCollectionError.noUpgrade }
            let balance = try fetchPearlBalance()
            guard balance >= next.pearlCost else { throw ShellCollectionError.insufficientPearls(required: next.pearlCost, available: balance) }
            let upgrade = ShellFindUpgrade(
                id: UUID().uuidString, instanceID: instanceID, fromStageID: instance.currentUpgradeStageID,
                toStageID: next.id, pearlCost: next.pearlCost, upgradedAt: date
            )
            context.insert(PearlLedgerModel(entry: PearlLedgerEntry(
                id: "shell_reward:\(upgrade.id):shape_find", delta: -next.pearlCost, reason: "shape_find",
                sourceType: "shell_reward", sourceID: instanceID, createdAt: date, note: nil
            )))
            context.insert(ShellFindUpgradeModel(upgrade: upgrade))
            instance.currentUpgradeStageID = next.id
            instance.lastActivityAt = date
            try context.save()
            return upgrade
        } catch {
            context.rollback()
            throw error
        }
    }

    func pinAchievement(badgeID: String, replacing: String? = nil, at date: Date = Date()) throws -> ShellPinResult {
        do {
            let definition = AchievementCatalog.resolve(badgeID)
            guard definition.pinnable else { throw ShellCollectionError.badgeNotPinnable }
            guard try fetchAchievementDashboard().badges.first(where: { $0.badgeID == badgeID })?.earned == true else { throw ShellCollectionError.badgeNotEarned }
            var pins = try context.fetch(FetchDescriptor<ShellBadgePinModel>()).sorted { $0.pinOrder < $1.pinOrder }
            if pins.contains(where: { $0.badgeID == badgeID }) { return .alreadyPinned }
            if pins.count >= 3, replacing == nil { return .replacementRequired(pins.map(\.badgeID)) }
            var desiredOrder = pins.count
            if let replacing, let old = pins.first(where: { $0.badgeID == replacing }) {
                desiredOrder = old.pinOrder
                context.delete(old)
                pins.removeAll { $0.badgeID == replacing }
            } else if pins.count >= 3 {
                throw ShellCollectionError.replacementRequired
            }
            context.insert(ShellBadgePinModel(badgeID: badgeID, pinOrder: desiredOrder, pinnedAt: date))
            try normalizePins()
            return .pinned
        } catch {
            context.rollback()
            throw error
        }
    }

    func unpinAchievement(badgeID: String) throws {
        do {
            for pin in try context.fetch(FetchDescriptor<ShellBadgePinModel>()) where pin.badgeID == badgeID { context.delete(pin) }
            try normalizePins()
        } catch {
            context.rollback()
            throw error
        }
    }

    func markShellNotificationViewed(id: String, at date: Date = Date()) throws {
        do {
            let parts = id.split(separator: ":", maxSplits: 1).map(String.init)
            guard parts.count == 2 else { return }
            if parts[0] == "FIND", let find = try context.fetch(FetchDescriptor<ShellFindInstanceModel>()).first(where: { $0.id == parts[1] }) {
                find.isNew = false
                find.viewedAt = find.viewedAt ?? date
            } else if parts[0] == "BADGE", let badge = try context.fetch(FetchDescriptor<ShellBadgeModel>()).first(where: { $0.badgeID == parts[1] }) {
                badge.isNew = false
            }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func markAllShellNotificationsViewed(at date: Date = Date()) throws {
        do {
            for find in try context.fetch(FetchDescriptor<ShellFindInstanceModel>()) {
                find.isNew = false
                find.viewedAt = find.viewedAt ?? date
            }
            for stack in try context.fetch(FetchDescriptor<ShellFindStackModel>()) {
                stack.isNew = false
                stack.viewedAt = stack.viewedAt ?? date
            }
            for badge in try context.fetch(FetchDescriptor<ShellBadgeModel>()) { badge.isNew = false }
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func setAchievementCountFloor(badgeID: String, minimumCount: Int, source: String, at date: Date = Date()) throws {
        do {
            let verified = try fetchShellBadges().first(where: { $0.badgeID == badgeID })?.count ?? 0
            try upsertCountFloor(badgeID: badgeID, minimumCount: minimumCount, verifiedCount: verified, source: source, at: date)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
    }

    func materializeShellFindReward(id: String, findID: String, sourceSessionID: UUID, acquiredAt: Date, markNew: Bool = true) throws {
        guard let definition = ShellContentCatalog.definition(findID) else { return }
        if definition.stackable {
            try addShellStack(findID: findID, quantity: 1, acquiredAt: acquiredAt, markNew: markNew, viewedAt: markNew ? nil : acquiredAt)
        } else if try !context.fetch(FetchDescriptor<ShellFindInstanceModel>()).contains(where: { $0.id == id }) {
            context.insert(ShellFindInstanceModel(instance: ShellFindInstance(
                id: id, findID: findID, acquiredAt: acquiredAt, sourceType: "session", sourceID: sourceSessionID.uuidString,
                currentUpgradeStageID: ShellContentCatalog.upgrades(for: findID).first?.id,
                isNew: markNew, isArchivedInChest: true, viewedAt: markNew ? nil : acquiredAt,
                animalLevel: 1, lastActivityAt: acquiredAt
            )))
            let source = CreatureCatalog.definition(findID)?.sourceType ?? .flowEarned
            _ = try recordCreatureDiscoveryIfNeeded(
                speciesID: findID, sourceType: source, instanceID: id,
                discoveredAt: acquiredAt, timestampConfidence: "EXACT"
            )
            try reconcileCreatureAchievements(at: acquiredAt)
        }
    }

    private func addShellStack(findID: String, quantity: Int, acquiredAt: Date, markNew: Bool, viewedAt: Date?) throws {
        let stacks = try context.fetch(FetchDescriptor<ShellFindStackModel>())
        if let stack = stacks.first(where: { $0.findID == findID }) {
            stack.quantity += quantity
            stack.lastAcquiredAt = max(stack.lastAcquiredAt, acquiredAt)
            stack.isNew = stack.isNew || markNew
            if stack.viewedAt == nil { stack.viewedAt = viewedAt }
        } else {
            context.insert(ShellFindStackModel(stack: ShellFindStack(
                findID: findID, quantity: quantity, firstAcquiredAt: acquiredAt, lastAcquiredAt: acquiredAt,
                isNew: markNew, viewedAt: viewedAt
            )))
        }
    }

    private func upsertCountFloor(badgeID: String, minimumCount: Int, verifiedCount: Int, source: String, at date: Date) throws {
        let floors = try context.fetch(FetchDescriptor<ShellBadgeCountFloorModel>())
        if let floor = floors.first(where: { $0.badgeID == badgeID }) {
            floor.minimumCount = max(floor.minimumCount, minimumCount)
            floor.verifiedCountAtReconciliation = max(floor.verifiedCountAtReconciliation, verifiedCount)
            floor.source = source
            floor.reconciledAt = date
        } else {
            context.insert(ShellBadgeCountFloorModel(
                badgeID: badgeID, minimumCount: max(0, minimumCount),
                verifiedCountAtReconciliation: max(0, verifiedCount), source: source, reconciledAt: date
            ))
        }
    }

    private func normalizePins() throws {
        let pins = try context.fetch(FetchDescriptor<ShellBadgePinModel>()).sorted { $0.pinOrder < $1.pinOrder }
        for (index, pin) in pins.enumerated() { pin.pinOrder = index }
        try context.save()
    }

    private static func shellFindInstance(_ model: ShellFindInstanceModel) -> ShellFindInstance {
        ShellFindInstance(
            id: model.id, findID: model.findID, acquiredAt: model.acquiredAt,
            sourceType: model.sourceType, sourceID: model.sourceID,
            currentUpgradeStageID: model.currentUpgradeStageID, isNew: model.isNew,
            isArchivedInChest: model.isArchivedInChest, viewedAt: model.viewedAt,
            animalLevel: model.animalLevel, lastActivityAt: model.lastActivityAt
        )
    }
    private static func shellFindStack(_ model: ShellFindStackModel) -> ShellFindStack {
        ShellFindStack(findID: model.findID, quantity: model.quantity, firstAcquiredAt: model.firstAcquiredAt, lastAcquiredAt: model.lastAcquiredAt, isNew: model.isNew, viewedAt: model.viewedAt)
    }
    private static func shellPlacement(_ model: ShellPlacementModel) -> ShellPlacement {
        ShellPlacement(id: model.id, roomID: model.roomID, slotID: model.slotID, instanceID: model.instanceID, placedAt: model.placedAt)
    }
    private static func shellUpgrade(_ model: ShellFindUpgradeModel) -> ShellFindUpgrade {
        ShellFindUpgrade(id: model.id, instanceID: model.instanceID, fromStageID: model.fromStageID, toStageID: model.toStageID, pearlCost: model.pearlCost, upgradedAt: model.upgradedAt)
    }
    private static func isCreatureEvidenceBadge(_ id: String) -> Bool {
        id == "variety_collector" || id.hasPrefix("mastery_") || id.hasPrefix("stillwater_")
            || id.hasPrefix("collection_") || id.hasPrefix("blue_")
    }
}

extension InMemoryFlowRepository {
    func fetchShellFindInstances() -> [ShellFindInstance] {
        shellFindInstances.filter { shellCreatureStatuses[$0.id, default: .active] == .active }.sorted { $0.acquiredAt > $1.acquiredAt }
    }
    func fetchShellFindStacks() -> [ShellFindStack] { shellFindStacks.values.sorted { $0.lastAcquiredAt > $1.lastAcquiredAt } }
    func fetchShellPlacements(roomID: String) -> [ShellPlacement] { shellPlacements.filter { $0.roomID == roomID }.sorted { $0.placedAt > $1.placedAt } }
    func fetchShellFindUpgrades(instanceID: String) -> [ShellFindUpgrade] { shellFindUpgrades.filter { $0.instanceID == instanceID }.sorted { $0.upgradedAt > $1.upgradedAt } }
    func fetchAchievementDashboard() -> AchievementDashboard {
        AchievementDashboardCalculator.calculate(badges: fetchShellBadges(), countFloors: shellBadgeCountFloors, pins: shellBadgePins)
    }
    func fetchShellNotifications() -> [ShellNotificationItem] {
        let finds = shellFindInstances.filter(\.isNew).compactMap { instance -> ShellNotificationItem? in
            ShellContentCatalog.definition(instance.findID).map {
                ShellNotificationItem(id: "FIND:\(instance.id)", kind: .find, sourceID: instance.id, title: $0.title, detail: $0.depth?.title ?? "New Shell find", occurredAt: instance.acquiredAt)
            }
        }
        let badges = fetchShellBadges().filter(\.isNew).map {
            ShellNotificationItem(id: "BADGE:\($0.badgeID)", kind: .badge, sourceID: $0.badgeID, title: AchievementCatalog.resolve($0.badgeID).title, detail: "Count advanced to \($0.count)", occurredAt: $0.lastEarnedAt)
        }
        return (finds + badges).sorted { $0.occurredAt > $1.occurredAt }
    }

    func reconcileShellCollections(version: Int = 1, at date: Date = Date()) -> ShellCollectionBackfillResult {
        if let prior = shellCollectionBackfills[version] { return ShellCollectionBackfillResult(version: version, instanceCount: prior.0, badgeCount: prior.1, alreadyCompleted: true) }
        var instanceCount = 0
        for grant in shellFindGrants where !shellFindInstances.contains(where: { $0.id == grant.id }) {
            materializeShellFindRewardInMemory(id: grant.id, findID: grant.findID, sourceSessionID: grant.sourceSessionID, acquiredAt: grant.acquiredAt, markNew: false)
            instanceCount += 1
        }
        let grouped = Dictionary(grouping: shellRewardEvents.filter { $0.type == .badgeUpdated && $0.rewardID != nil }, by: { $0.rewardID ?? "" })
        for (id, events) in grouped where !id.isEmpty {
            let count = events.reduce(0) { $0 + Int($1.quantity) }
            shellBadgeCountFloors[id] = max(shellBadgeCountFloors[id, default: 0], count)
            if shellBadges[id] == nil, count > 0 {
                let first = events.map(\.occurredAt).min() ?? date
                shellBadges[id] = ShellBadge(badgeID: id, count: count, firstEarnedAt: first, lastEarnedAt: events.map(\.occurredAt).max() ?? first, isNew: false)
            }
        }
        if version >= 2 {
            let badgeIDsBefore = Set(shellBadges.keys)
            for find in shellFindInstances {
                recordCreatureDiscoveryInMemoryIfNeeded(speciesID: find.findID, instanceID: find.id, discoveredAt: find.acquiredAt)
                if find.animalLevel >= CreatureEconomy.maxLevel,
                   !creatureMasteries.contains(where: { $0.instanceID == find.id }),
                   CreatureCatalog.definition(find.findID) != nil {
                    creatureMasteries.append(.init(
                        id: "mastery:\(find.id)", instanceID: find.id, speciesID: find.findID,
                        achievedAt: find.lastActivityAt, transactionID: "backfill:mastery:\(find.id)",
                        timestampConfidence: "ESTIMATED_FROM_ACQUISITION"
                    ))
                }
            }
            reconcileCreatureAchievementsInMemory(at: date)
            for id in shellBadges.keys where !badgeIDsBefore.contains(id) && Self.isCreatureEvidenceBadgeInMemory(id) {
                guard let badge = shellBadges[id] else { continue }
                shellBadges[id] = .init(
                    badgeID: badge.badgeID, count: badge.count, firstEarnedAt: badge.firstEarnedAt,
                    lastEarnedAt: badge.lastEarnedAt, isNew: false
                )
            }
        }
        shellCollectionBackfills[version] = (instanceCount, grouped.count)
        return ShellCollectionBackfillResult(version: version, instanceCount: instanceCount, badgeCount: grouped.count, alreadyCompleted: false)
    }

    func invitePearlObjectToChest(findID: String, at date: Date = Date()) throws -> ShellFindInstance {
        guard let definition = ShellContentCatalog.definition(findID), definition.isPearlObject, let cost = definition.pearlCost else { throw ShellCollectionError.missingFind }
        let balance = fetchPearlBalance()
        guard balance >= cost else { throw ShellCollectionError.insufficientPearls(required: cost, available: balance) }
        let value = ShellFindInstance(id: UUID().uuidString, findID: findID, acquiredAt: date, sourceType: "pearl_basin", sourceID: nil, currentUpgradeStageID: ShellContentCatalog.upgrades(for: findID).first?.id, isNew: true, isArchivedInChest: true, viewedAt: nil, animalLevel: 1, lastActivityAt: date)
        pearlLedger.append(PearlLedgerEntry(id: "shell_reward:\(value.id):invite_object", delta: -cost, reason: "invite_object", sourceType: "shell_reward", sourceID: value.id, createdAt: date, note: nil))
        shellFindInstances.append(value)
        return value
    }

    func placeShellFind(instanceID: String, roomID: String = "FOCUS", slotID: String, at date: Date = Date()) throws {
        guard let index = shellFindInstances.firstIndex(where: { $0.id == instanceID }), let definition = ShellContentCatalog.definition(shellFindInstances[index].findID) else { throw ShellCollectionError.missingFind }
        guard definition.placeable else { throw ShellCollectionError.notPlaceable }
        guard roomID == "FOCUS", let slot = ShellContentCatalog.focusSlots.first(where: { $0.id == slotID }) else { throw ShellCollectionError.invalidSlot }
        guard ShellContentCatalog.isCompatible(slot: slot, find: definition) else { throw ShellCollectionError.incompatibleSlot }
        let displaced = shellPlacements.filter { $0.instanceID == instanceID || ($0.roomID == roomID && $0.slotID == slotID) }.map(\.instanceID)
        for displacedID in displaced where displacedID != instanceID {
            if let displacedIndex = shellFindInstances.firstIndex(where: { $0.id == displacedID }) { shellFindInstances[displacedIndex].isArchivedInChest = true; shellFindInstances[displacedIndex].lastActivityAt = date }
        }
        shellPlacements.removeAll { $0.instanceID == instanceID || ($0.roomID == roomID && $0.slotID == slotID) }
        shellFindInstances[index].isArchivedInChest = false
        shellFindInstances[index].lastActivityAt = date
        shellPlacements.append(ShellPlacement(id: UUID().uuidString, roomID: roomID, slotID: slotID, instanceID: instanceID, placedAt: date))
    }

    func returnShellFindToChest(instanceID: String, at date: Date = Date()) throws {
        guard let index = shellFindInstances.firstIndex(where: { $0.id == instanceID }) else { throw ShellCollectionError.missingFind }
        shellPlacements.removeAll { $0.instanceID == instanceID }
        shellFindInstances[index].isArchivedInChest = true
        shellFindInstances[index].lastActivityAt = date
    }

    func upgradeShellFind(instanceID: String, at date: Date = Date()) throws -> ShellFindUpgrade {
        guard let index = shellFindInstances.firstIndex(where: { $0.id == instanceID }), let definition = ShellContentCatalog.definition(shellFindInstances[index].findID), definition.upgradeable, definition.kind != .animal, let next = ShellContentCatalog.nextUpgrade(findID: definition.id, currentStageID: shellFindInstances[index].currentUpgradeStageID) else { throw ShellCollectionError.noUpgrade }
        let balance = fetchPearlBalance()
        guard balance >= next.pearlCost else { throw ShellCollectionError.insufficientPearls(required: next.pearlCost, available: balance) }
        let value = ShellFindUpgrade(id: UUID().uuidString, instanceID: instanceID, fromStageID: shellFindInstances[index].currentUpgradeStageID, toStageID: next.id, pearlCost: next.pearlCost, upgradedAt: date)
        pearlLedger.append(PearlLedgerEntry(id: "shell_reward:\(value.id):shape_find", delta: -next.pearlCost, reason: "shape_find", sourceType: "shell_reward", sourceID: instanceID, createdAt: date, note: nil))
        shellFindUpgrades.append(value)
        shellFindInstances[index].currentUpgradeStageID = next.id
        shellFindInstances[index].lastActivityAt = date
        return value
    }

    func pinAchievement(badgeID: String, replacing: String? = nil, at date: Date = Date()) throws -> ShellPinResult {
        guard fetchAchievementDashboard().badges.first(where: { $0.badgeID == badgeID })?.earned == true else { throw ShellCollectionError.badgeNotEarned }
        if shellBadgePins[badgeID] != nil { return .alreadyPinned }
        let current = shellBadgePins.sorted { $0.value < $1.value }
        if current.count >= 3, replacing == nil { return .replacementRequired(current.map(\.key)) }
        let order = replacing.flatMap { shellBadgePins.removeValue(forKey: $0) } ?? current.count
        if current.count >= 3, replacing != nil, order == current.count { throw ShellCollectionError.replacementRequired }
        shellBadgePins[badgeID] = order
        normalizeInMemoryPins()
        return .pinned
    }
    func unpinAchievement(badgeID: String) { shellBadgePins.removeValue(forKey: badgeID); normalizeInMemoryPins() }
    func markShellNotificationViewed(id: String, at date: Date = Date()) {
        let parts = id.split(separator: ":", maxSplits: 1).map(String.init)
        guard parts.count == 2 else { return }
        if parts[0] == "FIND", let index = shellFindInstances.firstIndex(where: { $0.id == parts[1] }) { shellFindInstances[index].isNew = false; shellFindInstances[index].viewedAt = shellFindInstances[index].viewedAt ?? date }
        if parts[0] == "BADGE", let badge = shellBadges[parts[1]] { shellBadges[parts[1]] = ShellBadge(badgeID: badge.badgeID, count: badge.count, firstEarnedAt: badge.firstEarnedAt, lastEarnedAt: badge.lastEarnedAt, isNew: false) }
    }
    func markAllShellNotificationsViewed(at date: Date = Date()) {
        for index in shellFindInstances.indices { shellFindInstances[index].isNew = false; shellFindInstances[index].viewedAt = shellFindInstances[index].viewedAt ?? date }
        for (id, badge) in shellBadges { shellBadges[id] = ShellBadge(badgeID: id, count: badge.count, firstEarnedAt: badge.firstEarnedAt, lastEarnedAt: badge.lastEarnedAt, isNew: false) }
    }
    func setAchievementCountFloor(badgeID: String, minimumCount: Int, source: String, at date: Date = Date()) { shellBadgeCountFloors[badgeID] = max(shellBadgeCountFloors[badgeID, default: 0], minimumCount) }

    func materializeShellFindRewardInMemory(id: String, findID: String, sourceSessionID: UUID, acquiredAt: Date, markNew: Bool = true) {
        guard let definition = ShellContentCatalog.definition(findID) else { return }
        if definition.stackable {
            if var stack = shellFindStacks[findID] { stack.quantity += 1; stack.lastAcquiredAt = max(stack.lastAcquiredAt, acquiredAt); stack.isNew = stack.isNew || markNew; shellFindStacks[findID] = stack }
            else { shellFindStacks[findID] = ShellFindStack(findID: findID, quantity: 1, firstAcquiredAt: acquiredAt, lastAcquiredAt: acquiredAt, isNew: markNew, viewedAt: markNew ? nil : acquiredAt) }
        } else if !shellFindInstances.contains(where: { $0.id == id }) {
            shellFindInstances.append(ShellFindInstance(id: id, findID: findID, acquiredAt: acquiredAt, sourceType: "session", sourceID: sourceSessionID.uuidString, currentUpgradeStageID: ShellContentCatalog.upgrades(for: findID).first?.id, isNew: markNew, isArchivedInChest: true, viewedAt: markNew ? nil : acquiredAt, animalLevel: 1, lastActivityAt: acquiredAt))
            shellCreatureStatuses[id] = .active
            recordCreatureDiscoveryInMemoryIfNeeded(speciesID: findID, instanceID: id, discoveredAt: acquiredAt)
        }
    }
    private func normalizeInMemoryPins() { for (index, pair) in shellBadgePins.sorted(by: { $0.value < $1.value }).enumerated() { shellBadgePins[pair.key] = index } }
    private static func isCreatureEvidenceBadgeInMemory(_ id: String) -> Bool {
        id == "variety_collector" || id.hasPrefix("mastery_") || id.hasPrefix("stillwater_")
            || id.hasPrefix("collection_") || id.hasPrefix("blue_")
    }
}
