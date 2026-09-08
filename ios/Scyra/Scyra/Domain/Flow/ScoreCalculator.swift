import Foundation

struct ScoreBreakdown: Equatable, Sendable {
    let minutes: Int
    let basePoints: Int
    let tenMinuteBonuses: Int
    let thirtyMinuteBonuses: Int
    let sixtyMinuteBonuses: Int
    let totalPoints: Int
}

struct ArcMathResult: Equatable, Sendable {
    let arcMultiplierUsed: Double
    let arcBonusPoints: Int
    let finalPoints: Int
    let nextChainBase: Double
    let didLevelUp: Bool
}

enum ScoreCalculator {
    static let millisPerMinute: Int64 = 60_000

    static func breakdown(durationMs: Int64) -> ScoreBreakdown {
        let minutes = max(0, Int(durationMs / millisPerMinute))
        let basePoints = minutes
        let sixtyMinuteBonuses = minutes / 60
        let thirtyMinuteBonuses = (minutes / 30) - (minutes / 60)
        let tenMinuteBonuses = (minutes / 10) - (minutes / 30)
        let totalPoints = basePoints
            + tenMinuteBonuses * 5
            + thirtyMinuteBonuses * 15
            + sixtyMinuteBonuses * 50

        return ScoreBreakdown(
            minutes: minutes,
            basePoints: basePoints,
            tenMinuteBonuses: tenMinuteBonuses,
            thirtyMinuteBonuses: thirtyMinuteBonuses,
            sixtyMinuteBonuses: sixtyMinuteBonuses,
            totalPoints: totalPoints
        )
    }

    static func surgePoints(plannedMs: Int64?, actualDurationMs: Int64) -> Int {
        guard let plannedMs else { return 0 }
        let plannedMinutes = max(1, Double(plannedMs) / Double(millisPerMinute))
        let actualMinutes = max(1, Double(actualDurationMs) / Double(millisPerMinute))
        let error = abs(actualMinutes - plannedMinutes) / plannedMinutes
        let multiplier = 1 + (0.35 * exp(-5 * error))
        let raw = Int((actualMinutes * multiplier).rounded())

        return actualMinutes <= plannedMinutes ? raw : min(raw, Int(plannedMinutes))
    }

    static func arcMath(
        beforeArcPoints: Int,
        chainBase: Double,
        durationMs: Int64,
        stepMs: Int64 = ArcRules.progressStepMs,
        step: Double = ArcRules.multiplierStep
    ) -> ArcMathResult {
        let used = chainBase + arcTierExtra(durationMs: durationMs)
        let boosted = Int((Double(beforeArcPoints) * used).rounded())
        let bonus = max(0, boosted - beforeArcPoints)
        let didLevelUp = durationMs >= stepMs

        return ArcMathResult(
            arcMultiplierUsed: used,
            arcBonusPoints: bonus,
            finalPoints: beforeArcPoints + bonus,
            nextChainBase: didLevelUp ? chainBase + step : chainBase,
            didLevelUp: didLevelUp
        )
    }

    private static func arcTierExtra(durationMs: Int64) -> Double {
        switch durationMs {
        case ..<(10 * millisPerMinute): 0
        case ..<(20 * millisPerMinute): 0
        case ..<(40 * millisPerMinute): 0.1
        case ..<(60 * millisPerMinute): 0.2
        case ..<(90 * millisPerMinute): 0.3
        default: 0.4
        }
    }
}
