import Combine
import Foundation
import UIKit
import UserNotifications

struct FlowReminderContext: Equatable, Sendable {
    let flowInstanceID: UUID
    let title: String
    let journeyName: String
    let elapsedMs: Int64
}

enum FlowReminderPolicy {
    static let hourMs: Int64 = 3_600_000

    static func upcomingHourMarks(elapsedMs: Int64, count: Int) -> [Int] {
        guard count > 0 else { return [] }
        let completedHours = Int(max(0, elapsedMs) / hourMs)
        return Array((completedHours + 1)...(completedHours + count))
    }
}

@MainActor
protocol FlowReminderScheduling: AnyObject {
    func activate(_ context: FlowReminderContext) async
    func restore(_ context: FlowReminderContext) async
    func deactivate() async
}

@MainActor
final class NoOpFlowReminderScheduler: FlowReminderScheduling {
    func activate(_ context: FlowReminderContext) async {}
    func restore(_ context: FlowReminderContext) async {}
    func deactivate() async {}
}

@MainActor
final class UserNotificationFlowReminderScheduler: FlowReminderScheduling {
    static let identifierPrefix = "scyra.flow.hourly."
    static let deepLink = "skillz://flow"

    private let center: UNUserNotificationCenter
    private let maximumScheduledHours: Int

    init(
        center: UNUserNotificationCenter = .current(),
        maximumScheduledHours: Int = 48
    ) {
        self.center = center
        self.maximumScheduledHours = max(1, min(60, maximumScheduledHours))
    }

    func activate(_ context: FlowReminderContext) async {
        await removeOwnedRequests()

        var settings = await center.notificationSettings()
        if settings.authorizationStatus == .notDetermined {
            _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
            settings = await center.notificationSettings()
        }
        guard canSchedule(settings.authorizationStatus) else { return }

        await schedule(context)
    }

    func restore(_ context: FlowReminderContext) async {
        await removeOwnedRequests()
        let settings = await center.notificationSettings()
        guard canSchedule(settings.authorizationStatus) else { return }
        await schedule(context)
    }

    private func schedule(_ context: FlowReminderContext) async {

        let elapsedMs = max(0, context.elapsedMs)
        for hourMark in FlowReminderPolicy.upcomingHourMarks(
            elapsedMs: elapsedMs,
            count: maximumScheduledHours
        ) {
            let delayMs = Int64(hourMark) * FlowReminderPolicy.hourMs - elapsedMs
            guard delayMs > 0 else { continue }

            let content = UNMutableNotificationContent()
            content.title = "Still in Flow?"
            content.body = "No action needed if this is deliberate. Tap to return to Flow and end it if you forgot."
            content.sound = .default
            content.categoryIdentifier = "SCYRA_FLOW_REMINDER"
            content.userInfo = [
                "url": Self.deepLink,
                "flowInstanceID": context.flowInstanceID.uuidString,
                "hourMark": hourMark,
                "flowTitle": context.title,
                "journeyName": context.journeyName
            ]

            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: max(1, TimeInterval(delayMs) / 1_000),
                repeats: false
            )
            let request = UNNotificationRequest(
                identifier: "\(Self.identifierPrefix)\(context.flowInstanceID.uuidString).\(hourMark)",
                content: content,
                trigger: trigger
            )
            try? await center.add(request)
        }
    }

    func deactivate() async {
        await removeOwnedRequests()
        center.removeDeliveredNotifications(withIdentifiers: await ownedDeliveredIdentifiers())
    }

    private func removeOwnedRequests() async {
        let identifiers = await center.pendingNotificationRequests()
            .map(\.identifier)
            .filter { $0.hasPrefix(Self.identifierPrefix) }
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
    }

    private func ownedDeliveredIdentifiers() async -> [String] {
        await center.deliveredNotifications()
            .map { $0.request.identifier }
            .filter { $0.hasPrefix(Self.identifierPrefix) }
    }

    private func canSchedule(_ status: UNAuthorizationStatus) -> Bool {
        status == .authorized || status == .provisional || status == .ephemeral
    }
}

@MainActor
final class FlowDeepLinkCoordinator: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published private(set) var pendingURL: URL?

    private let center: UNUserNotificationCenter

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
        super.init()
        center.delegate = self
        center.setNotificationCategories([
            UNNotificationCategory(
                identifier: "SCYRA_FLOW_REMINDER",
                actions: [],
                intentIdentifiers: [],
                options: []
            )
        ])
    }

    func consume(_ url: URL) {
        guard Self.isFlowURL(url) else { return }
        pendingURL = url
    }

    func clearPendingURL() {
        pendingURL = nil
    }

    static func isFlowURL(_ url: URL) -> Bool {
        url.scheme?.lowercased() == "skillz" && url.host?.lowercased() == "flow"
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let value = response.notification.request.content.userInfo["url"] as? String
        Task { @MainActor [weak self] in
            if let value, let url = URL(string: value) { self?.consume(url) }
            completionHandler()
        }
    }
}

enum SurgeHapticEvent: Equatable, Sendable {
    case armed
    case started
    case midpoint
    case fiveMinutesLeft
    case twoMinutesLeft
    case oneMinuteLeft
    case thirtySecondsLeft
    case tenSecondsLeft
    case countdownTick(secondsRemaining: Int)
    case targetReached
    case completedSuccess
    case completedFailure
}

@MainActor
protocol SurgeHapticsProviding: AnyObject {
    func play(_ event: SurgeHapticEvent)
    func cancel()
}

@MainActor
final class NoOpSurgeHaptics: SurgeHapticsProviding {
    func play(_ event: SurgeHapticEvent) {}
    func cancel() {}
}

@MainActor
final class SystemSurgeHaptics: SurgeHapticsProviding {
    private var playbackTask: Task<Void, Never>?

    func play(_ event: SurgeHapticEvent) {
        playbackTask?.cancel()
        playbackTask = Task { @MainActor in
            switch event {
            case .armed, .started, .fiveMinutesLeft, .tenSecondsLeft:
                await impacts([.heavy, .heavy], spacing: .milliseconds(90))
            case .midpoint:
                impact(.heavy, intensity: 1)
            case .twoMinutesLeft, .oneMinuteLeft:
                await impacts([.medium, .heavy, .heavy], spacing: .milliseconds(80))
            case .thirtySecondsLeft:
                await impacts([.heavy, .heavy], spacing: .milliseconds(60))
            case .countdownTick(let secondsRemaining):
                let intensity = CGFloat(6 - max(1, min(5, secondsRemaining))) / 5
                impact(.rigid, intensity: max(0.55, intensity))
            case .targetReached:
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                try? await Task.sleep(for: .milliseconds(120))
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            case .completedSuccess:
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            case .completedFailure:
                UINotificationFeedbackGenerator().notificationOccurred(.warning)
            }
        }
    }

    func cancel() {
        playbackTask?.cancel()
        playbackTask = nil
    }

    private func impacts(
        _ styles: [UIImpactFeedbackGenerator.FeedbackStyle],
        spacing: Duration
    ) async {
        for (index, style) in styles.enumerated() {
            guard !Task.isCancelled else { return }
            impact(style, intensity: 1)
            if index < styles.count - 1 { try? await Task.sleep(for: spacing) }
        }
    }

    private func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle, intensity: CGFloat) {
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.prepare()
        generator.impactOccurred(intensity: intensity)
    }
}

struct SurgeRuntimeState: Equatable, Sendable {
    let plannedMs: Int64
    var midpointEmitted = false
    var fiveMinutesLeftEmitted = false
    var twoMinutesLeftEmitted = false
    var oneMinuteLeftEmitted = false
    var thirtySecondsLeftEmitted = false
    var tenSecondsLeftEmitted = false
    var countdownSecondsEmitted = Set<Int>()
    var targetReached = false
}

enum SurgeRuntimeEvaluator {
    static func evaluate(
        runtime: SurgeRuntimeState,
        elapsedMs: Int64
    ) -> (runtime: SurgeRuntimeState, events: [SurgeHapticEvent]) {
        var next = runtime
        var events: [SurgeHapticEvent] = []
        let planned = runtime.plannedMs
        let remaining = max(0, planned - elapsedMs)
        let midpoint = planned / 2

        if !next.midpointEmitted, midpoint > 0, elapsedMs >= midpoint {
            next.midpointEmitted = true
            events.append(.midpoint)
        }
        if planned > 15 * 60_000, !next.fiveMinutesLeftEmitted,
           remaining <= 5 * 60_000, elapsedMs < planned {
            next.fiveMinutesLeftEmitted = true
            events.append(.fiveMinutesLeft)
        }
        if !next.twoMinutesLeftEmitted, remaining <= 2 * 60_000, elapsedMs < planned {
            next.twoMinutesLeftEmitted = true
            events.append(.twoMinutesLeft)
        }
        if !next.oneMinuteLeftEmitted, remaining <= 60_000, elapsedMs < planned {
            next.oneMinuteLeftEmitted = true
            events.append(.oneMinuteLeft)
        }
        if !next.thirtySecondsLeftEmitted, remaining <= 30_000, elapsedMs < planned {
            next.thirtySecondsLeftEmitted = true
            events.append(.thirtySecondsLeft)
        }
        if !next.tenSecondsLeftEmitted, remaining <= 10_000, elapsedMs < planned {
            next.tenSecondsLeftEmitted = true
            events.append(.tenSecondsLeft)
        }
        if remaining >= 1, remaining <= 5_000 {
            let seconds = Int((remaining + 999) / 1_000)
            if (1...5).contains(seconds), !next.countdownSecondsEmitted.contains(seconds) {
                next.countdownSecondsEmitted.insert(seconds)
                events.append(.countdownTick(secondsRemaining: seconds))
            }
        }
        if elapsedMs >= planned, !next.targetReached {
            next.targetReached = true
            events.append(.targetReached)
        }
        return (next, events)
    }

    static func silentCatchUp(runtime: SurgeRuntimeState, elapsedMs: Int64) -> SurgeRuntimeState {
        var caught = evaluate(runtime: runtime, elapsedMs: elapsedMs).runtime
        if elapsedMs >= runtime.plannedMs {
            caught.countdownSecondsEmitted.formUnion(1...5)
        } else {
            let remaining = max(0, runtime.plannedMs - elapsedMs)
            if remaining >= 1, remaining <= 5_000 {
                let seconds = Int((remaining + 999) / 1_000)
                caught.countdownSecondsEmitted.formUnion(seconds...5)
            }
        }
        return caught
    }
}
