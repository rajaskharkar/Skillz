import Foundation
import Testing
@testable import Scyra

@MainActor
struct PreferencesParityTests {
    @Test func defaultsAndPersistenceMatchAndroidUserPrefs() {
        let suite = "ScyraPreferencesTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }

        let store = UserDefaultsAppPreferencesStore(defaults: defaults)
        let first = AppPreferencesModel(store: store)
        #expect(first.showScoreUI)
        #expect(!first.calmMode)
        #expect(first.language == .system)

        first.setShowScoreUI(false)
        first.setCalmMode(true)
        first.setLanguage(.marathi)

        let restored = AppPreferencesModel(store: store)
        #expect(!restored.showScoreUI)
        #expect(restored.calmMode)
        #expect(restored.language == .marathi)

        restored.setLanguage(.system)
        #expect(defaults.string(forKey: "app_language_tag") == nil)
    }

    @Test func languageCatalogMatchesAndroidTagsAndUnknownFallsBackToSystem() {
        #expect(AppLanguage.allCases.map(\.tag) == [nil, "en", "es", "hi", "mr"])
        #expect(AppLanguage(tag: "es") == .spanish)
        #expect(AppLanguage(tag: "fr") == .system)
    }

    @Test func calmRewardMappingUsesTimeFirstCopyAndSuppressesArcScore() {
        let regular = platformReward()
        let regularCard = RewardRevealMapper.cards(for: regular, calmMode: true)[0]
        #expect(regularCard.title == "Time logged")
        #expect(regularCard.amountText == "10 min")
        #expect(regularCard.body == "10 min\nLogged in your Story.")
        #expect(regularCard.subtitle == "Logged in your Story.")

        let arc = ArcSummary(
            totalSessions: 2,
            totalDurationMs: 1_200_000,
            totalFinalPoints: 60,
            totalArcBonusPoints: 5,
            peakMultiplier: 1.2,
            shellSummary: .empty
        )
        let arcReward = FlowReward.arcOnly(arcID: UUID(), summary: arc)
        let arcCard = RewardRevealMapper.cards(for: arcReward, calmMode: true)[0]
        #expect(arcCard.subtitle == "20m total")
        #expect(arcCard.body == "2 Flows\n20m total")
        #expect(arcCard.animationStyle == .none)
    }
}

@MainActor
struct FlowPlatformParityTests {
    @Test func remindersStartAtTheNextUncrossedActiveHourWithoutCatchUp() {
        #expect(FlowReminderPolicy.upcomingHourMarks(elapsedMs: 0, count: 3) == [1, 2, 3])
        #expect(FlowReminderPolicy.upcomingHourMarks(elapsedMs: 3_599_999, count: 2) == [1, 2])
        #expect(FlowReminderPolicy.upcomingHourMarks(elapsedMs: 3_600_000, count: 3) == [2, 3, 4])
        #expect(FlowReminderPolicy.upcomingHourMarks(elapsedMs: 9_000_000, count: 2) == [3, 4])
    }

    @Test func flowDeepLinkContractMatchesAndroid() {
        #expect(FlowDeepLinkCoordinator.isFlowURL(URL(string: "skillz://flow")!))
        #expect(!FlowDeepLinkCoordinator.isFlowURL(URL(string: "skillz://shell")!))
        #expect(!FlowDeepLinkCoordinator.isFlowURL(URL(string: "https://flow")!))

        let navigation = AppNavigationModel()
        #expect(navigation.openDeepLink(URL(string: "skillz://flow")!))
        #expect(navigation.selectedRoute == .flow)
    }

    @Test func surgeEvaluatorEmitsEachCanonicalMilestoneOnlyOnce() {
        var runtime = SurgeRuntimeState(plannedMs: 20 * 60_000)
        var result = SurgeRuntimeEvaluator.evaluate(runtime: runtime, elapsedMs: 10 * 60_000)
        #expect(result.events == [.midpoint])
        runtime = result.runtime

        result = SurgeRuntimeEvaluator.evaluate(runtime: runtime, elapsedMs: 15 * 60_000)
        #expect(result.events == [.fiveMinutesLeft])
        runtime = result.runtime

        result = SurgeRuntimeEvaluator.evaluate(runtime: runtime, elapsedMs: 18 * 60_000)
        #expect(result.events == [.twoMinutesLeft])
        runtime = result.runtime

        result = SurgeRuntimeEvaluator.evaluate(runtime: runtime, elapsedMs: 19 * 60_000)
        #expect(result.events == [.oneMinuteLeft])
        runtime = result.runtime

        result = SurgeRuntimeEvaluator.evaluate(runtime: runtime, elapsedMs: 20 * 60_000)
        #expect(result.events == [.targetReached])
        #expect(SurgeRuntimeEvaluator.evaluate(runtime: result.runtime, elapsedMs: 20 * 60_000).events.isEmpty)
    }

    @Test func launchRestorationDoesNotRequestNotificationAuthorization() async throws {
        let repository = InMemoryFlowRepository()
        let now = Date(timeIntervalSince1970: 12_000)
        repository.saveActiveFlow(ActiveFlowSnapshot(
            flowInstanceID: UUID(),
            title: "Restored",
            journeyName: "Work",
            mode: .flow,
            isInFlowMode: true,
            isRunning: true,
            accumulatedDurationMs: 10_000,
            segmentStartedAt: now.addingTimeInterval(-5),
            firstStartedAt: now.addingTimeInterval(-15),
            surgePlannedMs: nil,
            createdAt: now.addingTimeInterval(-15)
        ))
        let reminders = RecordingReminderScheduler()
        _ = FlowViewModel(repository: repository, reminderScheduler: reminders, now: { now })
        await Task.yield()
        await Task.yield()
        #expect(reminders.activations.isEmpty)
        #expect(reminders.restorations.count == 1)
    }

    @Test func flowEntryAndExitSynchronizeReminderAndSurgeFeedback() async {
        let repository = InMemoryFlowRepository()
        let clock = PlatformTestClock(Date(timeIntervalSince1970: 20_000))
        let reminders = RecordingReminderScheduler()
        let haptics = RecordingSurgeHaptics()
        let viewModel = FlowViewModel(
            repository: repository,
            reminderScheduler: reminders,
            surgeHaptics: haptics,
            now: { clock.value }
        )

        viewModel.setSurge(minutes: 20)
        viewModel.enterFlowMode()
        await Task.yield()
        #expect(haptics.events.prefix(2) == [.armed, .started])
        #expect(reminders.activations.count == 1)

        clock.value = clock.value.addingTimeInterval(10 * 60)
        viewModel.refreshElapsed(at: clock.value)
        #expect(haptics.events.contains(.midpoint))

        viewModel.exitFlowMode()
        await Task.yield()
        #expect(reminders.deactivationCount == 1)
        #expect(haptics.cancelCount >= 1)
    }

    @Test func foregroundRefreshRenewsRunningFlowRemindersWithoutRequestingAuthorization() async {
        let repository = InMemoryFlowRepository()
        let clock = PlatformTestClock(Date(timeIntervalSince1970: 30_000))
        let reminders = RecordingReminderScheduler()
        let viewModel = FlowViewModel(
            repository: repository,
            reminderScheduler: reminders,
            now: { clock.value }
        )
        viewModel.updateTitle("Long Flow")
        viewModel.updateJourneyName("Work")
        viewModel.enterFlowMode()
        await Task.yield()
        #expect(reminders.activations.count == 1)

        clock.value = clock.value.addingTimeInterval(3_700)
        viewModel.resumeFromBackground()
        await Task.yield()

        #expect(reminders.activations.count == 1)
        #expect(reminders.restorations.count == 1)
        #expect(reminders.restorations.first?.elapsedMs == 3_700_000)
    }
}

@MainActor
private final class RecordingReminderScheduler: FlowReminderScheduling {
    var activations: [FlowReminderContext] = []
    var restorations: [FlowReminderContext] = []
    var deactivationCount = 0

    func activate(_ context: FlowReminderContext) async { activations.append(context) }
    func restore(_ context: FlowReminderContext) async { restorations.append(context) }
    func deactivate() async { deactivationCount += 1 }
}

@MainActor
private final class RecordingSurgeHaptics: SurgeHapticsProviding {
    var events: [SurgeHapticEvent] = []
    var cancelCount = 0
    func play(_ event: SurgeHapticEvent) { events.append(event) }
    func cancel() { cancelCount += 1 }
}

@MainActor
private final class PlatformTestClock {
    var value: Date
    init(_ value: Date) { self.value = value }
}

private func platformReward(arcSummary: ArcSummary? = nil) -> FlowReward {
    FlowReward(
        id: UUID(),
        minutes: 10,
        baseScyraPoints: 20,
        tenMinuteBonuses: 1,
        thirtyMinuteBonuses: 0,
        sixtyMinuteBonuses: 0,
        finalScyraPoints: 25,
        surgePoints: 0,
        movementSteps: nil,
        movementPoints: 0,
        arcIndex: nil,
        arcMultiplierUsed: nil,
        arcBonusPoints: 0,
        arcNextMultiplier: nil,
        arcDidLevelUp: false,
        isSoftSession: false,
        arcSummary: arcSummary,
        shellReward: ShellSessionReward(pearlsEarned: 25)
    )
}
