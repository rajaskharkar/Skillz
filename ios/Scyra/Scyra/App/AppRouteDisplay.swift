import SwiftUI

struct AppRouteDisplay: Equatable, Sendable {
    let title: String
    let accessibilityLabel: String
    let systemImage: String?
    let assetImageName: String?
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
                assetImageName: nil,
                isRootTopBarAction: false
            )
        case .flow:
            AppRouteDisplay(
                title: "Flow",
                accessibilityLabel: "Flow",
                systemImage: "timer",
                assetImageName: nil,
                isRootTopBarAction: false
            )
        case .story:
            AppRouteDisplay(
                title: "Story",
                accessibilityLabel: "Open Story",
                systemImage: "book.closed",
                assetImageName: nil,
                isRootTopBarAction: true
            )
        case .paths:
            AppRouteDisplay(
                title: "Paths",
                accessibilityLabel: "Open Paths",
                systemImage: "safari",
                assetImageName: nil,
                isRootTopBarAction: true
            )
        case .shell:
            AppRouteDisplay(
                title: "Shell",
                accessibilityLabel: "Open The Shell",
                systemImage: nil,
                assetImageName: "scyraTurtle",
                isRootTopBarAction: true
            )
        case .notepad:
            AppRouteDisplay(
                title: "Notepad",
                accessibilityLabel: "Open Notepad",
                systemImage: "square.and.pencil",
                assetImageName: nil,
                isRootTopBarAction: true
            )
        case .help:
            AppRouteDisplay(
                title: "Help",
                accessibilityLabel: "Open Help",
                systemImage: "questionmark.circle",
                assetImageName: nil,
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
