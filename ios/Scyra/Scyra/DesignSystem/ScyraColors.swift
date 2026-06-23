import SwiftUI

/// Centralized SwiftUI color tokens for the Scyra design system.
///
/// Android wires Material `primary` to `SlytherinButNiceTeal` (`#3F8F8B`), and iOS
/// uses that teal as the active app primary for Android parity. Manuscript blue
/// (`#2F4F6F`, Android `RavenclawBlue`) remains available as a supporting brand token.
enum ScyraColors {
    // MARK: - Brand

    static let primaryTeal = Color(red: 63.0 / 255.0, green: 143.0 / 255.0, blue: 139.0 / 255.0) // Active iOS primary matching Android SlytherinButNiceTeal #3F8F8B.
    static let primaryManuscriptBlue = Color(red: 47.0 / 255.0, green: 79.0 / 255.0, blue: 111.0 / 255.0) // Supporting manuscript blue; Android RavenclawBlue #2F4F6F.
    static let secondaryGold = Color(red: 184.0 / 255.0, green: 165.0 / 255.0, blue: 106.0 / 255.0) // Android AntiqueGold #B8A56A.

    // MARK: - Surfaces

    static let background = Color(red: 242.0 / 255.0, green: 235.0 / 255.0, blue: 221.0 / 255.0) // Android GryffindorOffWhite #F2EBDD.
    static let backgroundBottom = Color(red: 228.0 / 255.0, green: 216.0 / 255.0, blue: 187.0 / 255.0) // Android light surface #E4D8BB.
    static let surface = Color(red: 228.0 / 255.0, green: 216.0 / 255.0, blue: 187.0 / 255.0) // Android light surface/surfaceVariant #E4D8BB.
    static let elevatedSurface = Color.white.opacity(0.82) // Provisional iOS glass card treatment retained from the existing shell/top-bar UI.

    // MARK: - Containers

    static let primaryContainer = primaryTeal.opacity(0.16) // Provisional iOS equivalent; Android does not define a primaryContainer token.
    static let secondaryContainer = secondaryGold.opacity(0.18) // Provisional iOS equivalent; Android uses AntiqueGold as secondary.

    // MARK: - Text

    static let textPrimary = Color.black // Android GryffindorBlack #000000.
    static let textSecondary = Color.black.opacity(0.75) // Android frequently derives secondary text from onSurface alpha.
    static let textMuted = Color.black.opacity(0.55) // Android frequently derives muted text from onSurface alpha.
    static let textDisabled = Color.black.opacity(0.35) // Android frequently derives disabled text from onSurface alpha.

    // MARK: - Lines

    static let border = primaryTeal.opacity(0.20) // Provisional iOS hairline based on the brand primary.
    static let hairline = primaryTeal.opacity(0.14) // Retains existing top-bar hairline opacity.

    // MARK: - Rewards / Semantic

    static let rewardPearl = Color(red: 217.0 / 255.0, green: 192.0 / 255.0, blue: 138.0 / 255.0) // Android shell pearl drawing #D9C08A.
    static let rewardMovement = primaryTeal // Provisional Scyra reward token from Android active primary #3F8F8B.
    static let rewardArc = Color(red: 140.0 / 255.0, green: 106.0 / 255.0, blue: 168.0 / 255.0) // Android story palette #8C6AA8.
    static let rewardSurge = Color(red: 209.0 / 255.0, green: 180.0 / 255.0, blue: 90.0 / 255.0) // Android story palette #D1B45A.
    static let success = Color(red: 47.0 / 255.0, green: 143.0 / 255.0, blue: 134.0 / 255.0) // Android story palette #2F8F86.
    static let warning = Color(red: 204.0 / 255.0, green: 138.0 / 255.0, blue: 62.0 / 255.0) // Android story palette #CC8A3E.
    static let error = Color(red: 127.0 / 255.0, green: 9.0 / 255.0, blue: 9.0 / 255.0) // Android GryffindorRed #7F0909.
}
