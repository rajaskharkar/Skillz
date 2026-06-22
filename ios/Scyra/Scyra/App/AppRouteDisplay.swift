import SwiftUI

struct AppRouteDisplay: Equatable, Sendable {
    let title: String
    let accessibilityLabel: String
    let systemImage: String?
    let isRootTopBarAction: Bool
}

extension AppRoute {
    var display: AppRouteDisplay {
        switch self {
        case .home:
            AppRouteDisplay(
                title: "Home",
                accessibilityLabel: "Home",
                systemImage: "house",
                isRootTopBarAction: false
            )
        case .flow:
            AppRouteDisplay(
                title: "Flow",
                accessibilityLabel: "Flow",
                systemImage: "timer",
                isRootTopBarAction: false
            )
        case .story:
            AppRouteDisplay(
                title: "Story",
                accessibilityLabel: "Open Story",
                systemImage: "book.closed",
                isRootTopBarAction: true
            )
        case .paths:
            AppRouteDisplay(
                title: "Paths",
                accessibilityLabel: "Open Paths",
                systemImage: "safari",
                isRootTopBarAction: true
            )
        case .shell:
            AppRouteDisplay(
                title: "Shell",
                accessibilityLabel: "Open The Shell",
                systemImage: "tortoise",
                isRootTopBarAction: true
            )
        case .notepad:
            AppRouteDisplay(
                title: "Notepad",
                accessibilityLabel: "Open Notepad",
                systemImage: "square.and.pencil",
                isRootTopBarAction: true
            )
        case .help:
            AppRouteDisplay(
                title: "Help",
                accessibilityLabel: "Open Help",
                systemImage: "questionmark.circle",
                isRootTopBarAction: true
            )
        }
    }

    static let rootTopBarActions: [AppRoute] = [
        .story,
        .paths,
        .shell,
        .notepad,
        .help
    ]
}
