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
        case .shell:
            AppRouteDisplay(
                title: "Shell",
                accessibilityLabel: "Open Shell",
                systemImage: nil,
                assetImageName: "scyraTurtle"
            )
        }
    }

    static let rootTopBarActions: [AppRoute] = allCases
}
