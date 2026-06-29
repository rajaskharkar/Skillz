import Foundation

/// Typed app-level and Story action destinations for the Scyra shell.
///
/// Story is the launch/root destination, matching Android's Story-root
/// experience. Top-level route selections and Story floating actions replace
/// selected state instead of pushing onto a navigation stack.
enum AppRoute: Hashable, CaseIterable, Sendable {
    case story
    case horizon
    case shell
    case notepad
    case help
    case flow
    case pulse

    var isStoryRoot: Bool {
        self == .story
    }

    var usesRootBackAffordance: Bool {
        self == .flow || self == .pulse
    }
}
