import Foundation

/// Typed app-level destinations for the Scyra top-level shell.
///
/// Story is the launch/root destination, matching Android's Story-root
/// experience. Top-level route selections replace this state instead of pushing
/// onto a navigation stack.
enum AppRoute: Hashable, CaseIterable, Sendable {
    case story
    case flow
    case pulse
    case shell

    var isRoot: Bool {
        self == .story
    }
}
