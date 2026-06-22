import Foundation

/// Typed app-level destinations for future Scyra parity work.
///
/// These cases are placeholders only. Feature implementations and real navigation
/// destinations will be added in later phases when each product area is scoped.
enum AppRoute: Hashable, Sendable {
    case home
    case flow
    case story
    case paths
    case shell
    case notepad
    case help
}
