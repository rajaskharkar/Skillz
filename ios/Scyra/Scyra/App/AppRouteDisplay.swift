import SwiftUI

struct AppRouteDisplay: Equatable, Sendable {
    let title: String
    let accessibilityLabel: String
    let systemImage: String?
    let assetImageName: String?
}

extension AppRoute {
    // Root-navigation assets are the exact Material vectors used by Android.
    // Feature-only routes continue to use SF Symbols until their canonical
    // Material vectors are introduced with the corresponding feature surface.
    var display: AppRouteDisplay {
        switch self {
        case .story:
            AppRouteDisplay(title: "Story", accessibilityLabel: "Open Story", systemImage: nil, assetImageName: "materialAutoStories")
        case .horizon:
            AppRouteDisplay(title: "Horizon", accessibilityLabel: "Open Horizon", systemImage: nil, assetImageName: "materialExplore")
        case .shell:
            AppRouteDisplay(title: "Shell", accessibilityLabel: "Open Shell", systemImage: nil, assetImageName: "scyraTurtle")
        case .notepad:
            AppRouteDisplay(title: "Notepad", accessibilityLabel: "Open Notepad", systemImage: nil, assetImageName: "materialEditNote")
        case .help:
            AppRouteDisplay(title: "Help", accessibilityLabel: "Open Help", systemImage: nil, assetImageName: "materialHelpOutline")
        case .flow:
            AppRouteDisplay(title: "Flow", accessibilityLabel: "Open Flow", systemImage: "sparkles", assetImageName: nil)
        case .pulse:
            AppRouteDisplay(title: "Pulse", accessibilityLabel: "Open Pulse", systemImage: "brain.head.profile", assetImageName: nil)
        case .flowDetail(_):
            AppRouteDisplay(title: "Flow Detail", accessibilityLabel: "Open Flow Detail", systemImage: "doc.text.magnifyingglass", assetImageName: nil)
        case .flowEdit(_):
            AppRouteDisplay(title: "Edit Flow", accessibilityLabel: "Open Edit Flow", systemImage: "square.and.pencil", assetImageName: nil)
        case .pulseDetail(_):
            AppRouteDisplay(title: "Pulse Detail", accessibilityLabel: "Open Pulse Detail", systemImage: "doc.text.magnifyingglass", assetImageName: nil)
        case .pulseEdit(_):
            AppRouteDisplay(title: "Edit Pulse", accessibilityLabel: "Open Edit Pulse", systemImage: "square.and.pencil", assetImageName: nil)
        case .shellRoom(let room):
            AppRouteDisplay(title: room.title, accessibilityLabel: room.accessibilityLabel, systemImage: room.systemImage, assetImageName: nil)
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
