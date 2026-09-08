import Foundation

enum RewardRevealCardType: String, Equatable, Sendable {
    case scoreBreakdown
    case stillwaterResult
    case shellBridge
    case animal
    case badge
    case arcScore
    case arcAnimals
    case arcBadges
    case arcStillwater
    case emptyShellMeaning
    case arcStoryPlaceholder
}

enum RewardRevealAnimationStyle: String, Equatable, Sendable {
    case none
    case pearlGlow
    case stillwaterRipple
    case animalSwim
    case animalBob
    case animalGlide
    case animalShadow
    case badgeStamp
}

struct RewardRevealCard: Identifiable, Equatable, Sendable {
    let id: String
    let type: RewardRevealCardType
    let title: String
    var subtitle: String?
    var body: String?
    var chip: String?
    var amountText: String?
    var systemImage: String
    var destinationHint: String?
    var animationStyle: RewardRevealAnimationStyle = .none
}

enum RewardRevealMapper {
    static func cards(for reward: FlowReward, calmMode: Bool = false) -> [RewardRevealCard] {
        if let arc = reward.arcSummary {
            let arcReveal = arcCards(for: arc, calmMode: calmMode)
            if reward.isArcOnlySummary { return arcReveal }
            let sessionReveal = reward.isSoftSession
                ? softCards(for: reward)
                : sessionCards(for: reward, calmMode: calmMode)
            return sessionReveal + arcReveal
        }
        if reward.isSoftSession { return softCards(for: reward) }
        return sessionCards(for: reward, calmMode: calmMode)
    }

    static func sessionCards(for reward: FlowReward, calmMode: Bool = false) -> [RewardRevealCard] {
        let timeBonus = reward.tenMinuteBonuses * 5
            + reward.thirtyMinuteBonuses * 15
            + reward.sixtyMinuteBonuses * 50
        var scoreLines: [String]
        if calmMode {
            scoreLines = ["\(reward.minutes) min", "Logged in your Story."]
        } else {
            scoreLines = ["Built from:", "Base Flow \(reward.baseScyraPoints)"]
            if timeBonus > 0 { scoreLines.append("Time bonuses +\(timeBonus)") }
            if reward.surgePoints > 0 { scoreLines.append("Surge +\(reward.surgePoints)") }
            if reward.arcBonusPoints > 0 { scoreLines.append("Arc bonus +\(reward.arcBonusPoints)") }
            if let multiplier = reward.arcMultiplierUsed {
                scoreLines.append("Arc multiplier: \(multiplier.formatted(.number.precision(.fractionLength(1))))×")
            }
            scoreLines.append("Swipe to see what Scyra brought back.")
        }

        var cards = [RewardRevealCard(
            id: "session-score",
            type: .scoreBreakdown,
            title: calmMode ? "Time logged" : "\(reward.finalScyraPoints) Scyra Points",
            subtitle: !calmMode && reward.shellReward.pearlsEarned > 0
                ? "Carried into The Shell as Pearls."
                : "Logged in your Story.",
            body: scoreLines.joined(separator: "\n"),
            amountText: calmMode ? "\(reward.minutes) min" : "\(reward.finalScyraPoints) Scyra Points",
            systemImage: "sparkles",
            animationStyle: reward.shellReward.pearlsEarned > 0 ? .pearlGlow : .none
        )]

        let findCounts = orderedCounts(reward.shellReward.grantedFindIDs)
        for (findID, count) in findCounts {
            // Android intentionally hides obsolete non-creature find rows.
            guard let animal = ShellRewardCatalog.animals[findID] else { continue }
            let baseTitle = "\(animal.title) encountered"
            cards.append(RewardRevealCard(
                id: "animal-\(findID)-\(count)",
                type: .animal,
                title: count > 1 ? "\(baseTitle) ×\(count)" : baseTitle,
                subtitle: "Animal · \(animal.depth)",
                body: animal.reason,
                chip: animal.depth,
                systemImage: animal.systemImage,
                destinationHint: "View later in The Blue.",
                animationStyle: animation(for: findID)
            ))
        }

        let badgeCounts = orderedCounts(reward.shellReward.badgeIDs)
        if badgeCounts.count > 1 {
            let lines = badgeCounts.map { entry in
                "\(ShellRewardCatalog.badgeTitles[entry.id] ?? "Shell reward recorded") ×\(entry.count)"
            }
            cards.append(RewardRevealCard(
                id: "badges",
                type: .badge,
                title: "Badges updated",
                subtitle: "Badge",
                body: (lines + ["Records updated from this Flow."]).joined(separator: "\n"),
                chip: "Badge",
                systemImage: "rosette",
                destinationHint: "Recorded in Badges.",
                animationStyle: .badgeStamp
            ))
        } else if let badge = badgeCounts.first {
            let name = ShellRewardCatalog.badgeTitles[badge.id] ?? "Shell reward recorded"
            cards.append(RewardRevealCard(
                id: "badge-\(badge.id)-\(badge.count)",
                type: .badge,
                title: "\(name) badge updated",
                subtitle: "Badge",
                body: badgeReason(badge.id),
                chip: "Badge",
                systemImage: "rosette",
                destinationHint: "Recorded in Badges.",
                animationStyle: .badgeStamp
            ))
        }

        if cards.count == 1 {
            let carriedPearls = reward.shellReward.pearlsEarned > 0
            cards.append(RewardRevealCard(
                id: carriedPearls ? "shell-bridge" : "session-shell-quiet",
                type: carriedPearls ? .shellBridge : .emptyShellMeaning,
                title: "The Shell was shaped",
                body: carriedPearls
                    ? "Your Scyra Points were carried into The Shell as Pearls. Use Pearls to brighten, polish, and awaken what you own."
                    : "Logged in your Story.",
                systemImage: "circle.hexagongrid.fill",
                destinationHint: carriedPearls
                    ? "Shape The Shell with Pearls."
                    : "View inside The Shell.",
                animationStyle: .pearlGlow
            ))
        }
        return cards
    }

    static func softCards(for reward: FlowReward) -> [RewardRevealCard] {
        [RewardRevealCard(
            id: "soft-stillwater",
            type: .stillwaterResult,
            title: "Soft Flow complete",
            subtitle: "Added to Stillwater.",
            body: "Spend Drops in Stillwater to draw exclusive creatures from quiet vessels.",
            amountText: "+\(reward.shellReward.stillwaterUnits) Drops",
            systemImage: "drop.fill",
            destinationHint: "View in Stillwater Room.",
            animationStyle: .stillwaterRipple
        )]
    }

    static func arcCards(for arc: ArcSummary, calmMode: Bool = false) -> [RewardRevealCard] {
        let duration = compactDuration(arc.totalDurationMs)
        var scoreBody = [
            "\(arc.totalSessions) Flows",
            "\(duration) total"
        ]
        if !calmMode {
            scoreBody.append("\(arc.totalFinalPoints) Scyra Points")
            scoreBody.append("Carried into The Shell as Pearls.")
            scoreBody.append("Peak multiplier: \(arc.peakMultiplier.formatted(.number.precision(.fractionLength(1))))×")
            if arc.totalArcBonusPoints > 0 { scoreBody.append("Arc bonus: +\(arc.totalArcBonusPoints)") }
            scoreBody.append("Swipe to see what this Arc brought back.")
        }
        var cards = [RewardRevealCard(
            id: "arc-score",
            type: .arcScore,
            title: "Arc complete",
            subtitle: calmMode ? "\(duration) total" : "\(arc.totalFinalPoints) Scyra Points",
            body: scoreBody.joined(separator: "\n"),
            amountText: calmMode ? "\(duration) total" : "\(arc.totalFinalPoints) Scyra Points",
            systemImage: "flame.fill",
            animationStyle: calmMode ? .none : .pearlGlow
        )]

        if !arc.shellSummary.animals.isEmpty {
            let lines = arc.shellSummary.animals.map {
                "\(ShellRewardCatalog.animals[$0.id]?.title ?? "Shell reward recorded") ×\($0.count)"
            }
            cards.append(RewardRevealCard(
                id: "arc-animals",
                type: .arcAnimals,
                title: "Animals encountered",
                body: (lines + ["From Flow milestones across this Arc."]).joined(separator: "\n"),
                systemImage: "fish.fill",
                destinationHint: "View later in The Blue."
            ))
        }

        if !arc.shellSummary.badges.isEmpty {
            let lines = arc.shellSummary.badges.map {
                "\(ShellRewardCatalog.badgeTitles[$0.id] ?? "Shell reward recorded") ×\($0.count)"
            }
            cards.append(RewardRevealCard(
                id: "arc-badges",
                type: .arcBadges,
                title: "Badges updated",
                body: (lines + ["Records updated across this Arc."]).joined(separator: "\n"),
                systemImage: "rosette",
                destinationHint: "Recorded in Badges.",
                animationStyle: .badgeStamp
            ))
        }

        if arc.shellSummary.stillwaterAdded > 0 {
            cards.append(RewardRevealCard(
                id: "arc-stillwater",
                type: .arcStillwater,
                title: "Soft Flow complete",
                body: "\(arc.shellSummary.stillwaterAdded / 60) quiet minutes carried into Stillwater.",
                systemImage: "drop.fill",
                destinationHint: "View in Stillwater Room.",
                animationStyle: .stillwaterRipple
            ))
        }

        cards.append(contentsOf: arc.shellSummary.unknownRewards.map { unknownCard(id: "arc-unknown-\($0.id)") })
        if cards.count == 1 {
            if arc.shellSummary.pearlsCarried > 0 {
                cards.append(RewardRevealCard(
                    id: "arc-shell-shaped",
                    type: .shellBridge,
                    title: "The Shell was shaped",
                    body: "Scyra Points from this Arc were carried into The Shell as Pearls.",
                    systemImage: "circle.hexagongrid.fill",
                    destinationHint: "Shape The Shell with Pearls.",
                    animationStyle: .pearlGlow
                ))
            } else {
                cards.append(RewardRevealCard(
                    id: "arc-story-placeholder",
                    type: .arcStoryPlaceholder,
                    title: "This Arc became part of your story",
                    body: "Voyage Hall will gather Arc journeys in a future Shell update.",
                    systemImage: "map"
                ))
            }
        }
        return cards
    }

    private static func badgeReason(_ badgeID: String) -> String {
        switch badgeID {
        case ShellRewardCatalog.badgeFlow10:
            "Earned each time a regular Flow lasts 10 minutes or more."
        case ShellRewardCatalog.badgeFlow30:
            "Earned each time a regular Flow lasts 30 minutes or more."
        case ShellRewardCatalog.badgeFlow60:
            "Earned each time a regular Flow lasts 1 hour or more."
        case ShellRewardCatalog.badgeFlow120:
            "Earned each time a regular Flow lasts 2 hours or more."
        default:
            "Records updated from this Flow."
        }
    }

    private static func animation(for findID: String) -> RewardRevealAnimationStyle {
        switch findID {
        case ShellRewardCatalog.focusMinnow: .animalSwim
        case ShellRewardCatalog.focusSeahorse: .animalBob
        case ShellRewardCatalog.focusManta: .animalGlide
        case ShellRewardCatalog.focusWhale: .animalShadow
        default: .none
        }
    }

    /// Kotlin's `groupingBy(...).eachCount()` retains first-appearance order.
    /// That order is visible in the multi-milestone reward pager, so preserve
    /// it explicitly rather than sorting IDs alphabetically.
    private static func orderedCounts(_ ids: [String]) -> [(id: String, count: Int)] {
        var indices: [String: Int] = [:]
        var result: [(id: String, count: Int)] = []
        for id in ids {
            if let index = indices[id] {
                result[index].count += 1
            } else {
                indices[id] = result.count
                result.append((id, 1))
            }
        }
        return result
    }

    private static func unknownCard(id: String) -> RewardRevealCard {
        RewardRevealCard(
            id: id,
            type: .emptyShellMeaning,
            title: "Shell reward recorded",
            body: "Recorded inside The Shell.",
            systemImage: "circle.hexagongrid",
            destinationHint: "View inside The Shell."
        )
    }

    private static func compactDuration(_ durationMs: Int64) -> String {
        let totalMinutes = max(0, durationMs) / 60_000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        if hours > 0, minutes > 0 { return "\(hours)h \(minutes)m" }
        if hours > 0 { return "\(hours)h" }
        return "\(minutes)m"
    }
}
