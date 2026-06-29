import Foundation

/// Typed app-level and feature-level destinations for Scyra navigation.
///
/// These routes are intentionally value-based so future feature screens can
/// navigate without raw string route names or ad hoc state flags.
enum AppRoute: Hashable, Sendable {
    case story
    case horizon
    case shell
    case notepad
    case help

    case flow
    case pulse

    case flowDetail(id: String)
    case flowEdit(id: String)

    case pulseDetail(id: String)
    case pulseEdit(id: String)

    case shellRoom(ShellRoomRoute)

    var isTopLevel: Bool {
        switch self {
        case .story, .horizon, .shell, .notepad, .help:
            true
        case .flow, .pulse, .flowDetail(_), .flowEdit(_), .pulseDetail(_), .pulseEdit(_), .shellRoom(_):
            false
        }
    }

    var isStoryRoot: Bool {
        self == .story
    }

    var isStoryAction: Bool {
        self == .flow || self == .pulse
    }

    var isFlowRoute: Bool {
        switch self {
        case .flow, .flowDetail(_), .flowEdit(_):
            true
        case .story, .horizon, .shell, .notepad, .help, .pulse, .pulseDetail(_), .pulseEdit(_), .shellRoom(_):
            false
        }
    }

    var isPulseRoute: Bool {
        switch self {
        case .pulse, .pulseDetail(_), .pulseEdit(_):
            true
        case .story, .horizon, .shell, .notepad, .help, .flow, .flowDetail(_), .flowEdit(_), .shellRoom(_):
            false
        }
    }

    var isShellRoute: Bool {
        switch self {
        case .shell, .shellRoom(_):
            true
        case .story, .horizon, .notepad, .help, .flow, .pulse, .flowDetail(_), .flowEdit(_), .pulseDetail(_), .pulseEdit(_):
            false
        }
    }

    var usesScyraTopBar: Bool {
        switch self {
        case .story, .horizon, .shell, .notepad, .help, .shellRoom(_):
            true
        case .flow, .pulse, .flowDetail(_), .flowEdit(_), .pulseDetail(_), .pulseEdit(_):
            false
        }
    }

    var usesActionScreenHeader: Bool {
        isFlowRoute || isPulseRoute
    }

    var returnsToStoryOnBack: Bool {
        usesActionScreenHeader
    }

    var usesRootBackAffordance: Bool {
        returnsToStoryOnBack
    }
}

enum ShellRoomRoute: Hashable, CaseIterable, Sendable {
    case shellRoot
    case theBlue
    case stillwater
    case ideaGrove
    case lookout
    case voyageHall
    case focusRoom
    case chest
    case badges
}

extension ShellRoomRoute {
    var title: String {
        switch self {
        case .shellRoot:
            "Shell"
        case .theBlue:
            "The Blue"
        case .stillwater:
            "Stillwater"
        case .ideaGrove:
            "Idea Grove"
        case .lookout:
            "Lookout"
        case .voyageHall:
            "Voyage Hall"
        case .focusRoom:
            "Focus Room"
        case .chest:
            "Chest"
        case .badges:
            "Badges"
        }
    }

    var accessibilityLabel: String {
        "Open \(title)"
    }

    var systemImage: String? {
        switch self {
        case .shellRoot:
            nil
        case .theBlue:
            "water.waves"
        case .stillwater:
            "drop"
        case .ideaGrove:
            "brain.head.profile"
        case .lookout:
            "binoculars"
        case .voyageHall:
            "map"
        case .focusRoom:
            "scope"
        case .chest:
            "shippingbox"
        case .badges:
            "rosette"
        }
    }
}
