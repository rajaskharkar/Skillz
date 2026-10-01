#if DEBUG && targetEnvironment(simulator)
import Foundation

/// Explicit, simulator-only fixture. The in-memory SwiftData store never opens or
/// changes the user's database, and rewards are created through normal Flow commits.
enum ShellNavigationUITestFixture {
    static var isRequested: Bool {
        ProcessInfo.processInfo.arguments.contains("--ui-testing-shell-notifications") || landRewardsRequested || landExplorationRequested || emptyShellRequested
    }

    private static var landRewardsRequested: Bool {
        ProcessInfo.processInfo.arguments.contains("--ui-testing-land-rewards")
    }

    private static var landExplorationRequested: Bool {
        ProcessInfo.processInfo.arguments.contains("--ui-testing-land-exploration")
    }

    private static var emptyShellRequested: Bool {
        ProcessInfo.processInfo.arguments.contains("--ui-testing-empty-shell")
    }

    @MainActor
    static func seedIfRequested(repository: any ScyraRepository) throws {
        guard isRequested else { return }
        guard !emptyShellRequested else { return }
        if landRewardsRequested || landExplorationRequested {
            let arcID = UUID()
            let end = Date().addingTimeInterval(-301)
            for index in 1...3 {
                let session = FlowSession(
                    id: UUID(), flowInstanceID: UUID(), title: "Land reward \(index)", description: "",
                    journeyName: "Arc reward tests", startTime: end.addingTimeInterval(-60), endTime: end,
                    durationMs: 60_000, surgePlannedMs: nil, surgePoints: 0, scyraPoints: 1,
                    isSoftMode: false, arcID: arcID, arcIndex: index, arcMultiplierUsed: 1,
                    arcBonusPoints: 0, createdAt: end
                )
                _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
            }
            // AppRoot's real expiry path performs the grant after launch.
            if !landExplorationRequested { return }
        }
        for minutes in landExplorationRequested ? [600] : [10, 30] {
            let end = Date().addingTimeInterval(Double(-minutes * 60))
            let session = FlowSession(
                id: UUID(), flowInstanceID: UUID(), title: "Shell navigation \(minutes)",
                description: "", journeyName: "Navigation tests",
                startTime: end.addingTimeInterval(Double(-minutes * 60)), endTime: end,
                durationMs: Int64(minutes * 60_000), surgePlannedMs: nil, surgePoints: 0,
                scyraPoints: ScoreCalculator.breakdown(durationMs: Int64(minutes * 60_000)).totalPoints,
                isSoftMode: false, arcID: nil, arcIndex: nil, arcMultiplierUsed: nil,
                arcBonusPoints: 0, createdAt: end
            )
            _ = try repository.commit(session: session, activeArc: nil, recentlyEndedArc: nil)
        }
    }
}
#endif
