import SwiftUI

struct AppRouteDisplay: Equatable, Sendable {
    let title: String
    let accessibilityLabel: String
    let systemImage: String?
    let assetImageName: String?
}

extension AppRoute {
    var display: AppRouteDisplay {
        switch self {
        case .story:
            AppRouteDisplay(
                title: "Story",
                accessibilityLabel: "Open Story",
                systemImage: "book.closed",
                assetImageName: nil
            )
        case .horizon:
            AppRouteDisplay(
                title: "Horizon",
                accessibilityLabel: "Open Horizon",
                systemImage: "sun.horizon",
                assetImageName: nil
            )
        case .shell:
            AppRouteDisplay(
                title: "Shell",
                accessibilityLabel: "Open Shell",
                systemImage: nil,
                assetImageName: "scyraTurtle"
            )
        case .notepad:
            AppRouteDisplay(
                title: "Notepad",
                accessibilityLabel: "Open Notepad",
                systemImage: "square.and.pencil",
                assetImageName: nil
            )
        case .help:
            AppRouteDisplay(
                title: "Help",
                accessibilityLabel: "Open Help",
                systemImage: "questionmark.circle",
                assetImageName: nil
            )
        case .flow:
            AppRouteDisplay(
                title: "Flow",
                accessibilityLabel: "Open Flow",
                systemImage: "play.circle",
                assetImageName: nil
            )
        case .pulse:
            AppRouteDisplay(
                title: "Pulse",
                accessibilityLabel: "Open Pulse",
                systemImage: "waveform",
                assetImageName: nil
            )
        }
    }

    static let rootTopBarActions: [AppRoute] = [
        .story,
        .horizon,
        .shell,
        .notepad,
        .help
    ]
}
