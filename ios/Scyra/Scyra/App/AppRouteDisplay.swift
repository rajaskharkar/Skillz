import SwiftUI

struct AppRouteDisplay: Equatable, Sendable {
    let title: String
    let accessibilityLabel: String
    let systemImage: String?
    let assetImageName: String?
}

extension AppRoute {
    // SF Symbol choices mirror Android Material icons: AutoStories → books.vertical,
    // Explore → safari, EditNote → square.and.pencil, HelpOutline → questionmark.circle,
    // PsychologyAlt → brain.head.profile, AutoAwesome → sparkles.
    var display: AppRouteDisplay {
        switch self {
        case .story:
            AppRouteDisplay(
                title: "Story",
                accessibilityLabel: "Open Story",
                systemImage: "books.vertical",
                assetImageName: nil
            )
        case .horizon:
            AppRouteDisplay(
                title: "Horizon",
                accessibilityLabel: "Open Horizon",
                systemImage: "safari",
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
                systemImage: "sparkles",
                assetImageName: nil
            )
        case .pulse:
            AppRouteDisplay(
                title: "Pulse",
                accessibilityLabel: "Open Pulse",
                systemImage: "brain.head.profile",
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
