import SwiftUI

/// Centralized SwiftUI color tokens for the Scyra design system.
///
/// Active iOS primary is Scyra manuscript blue (`#2F4F6F`). Teal (`#3F8F8B`)
/// remains available only as an Android-parity/supporting color and semantic reward tint.
enum ScyraColors {
    // MARK: - Brand

    static let primaryManuscriptBlue = Color(red: 47.0 / 255.0, green: 79.0 / 255.0, blue: 111.0 / 255.0) // Active iOS primary #2F4F6F.
    static let androidPrimaryTeal = Color(red: 63.0 / 255.0, green: 143.0 / 255.0, blue: 139.0 / 255.0) // Android parity/supporting token #3F8F8B.
    static let secondaryGold = Color(red: 184.0 / 255.0, green: 165.0 / 255.0, blue: 106.0 / 255.0) // Android AntiqueGold #B8A56A.

    // MARK: - Surfaces

    static let background = Color(red: 242.0 / 255.0, green: 235.0 / 255.0, blue: 221.0 / 255.0) // Android GryffindorOffWhite #F2EBDD.
    static let backgroundBottom = Color(red: 228.0 / 255.0, green: 216.0 / 255.0, blue: 187.0 / 255.0) // Android light surface #E4D8BB.
    static let surface = Color(red: 228.0 / 255.0, green: 216.0 / 255.0, blue: 187.0 / 255.0) // Android light surface/surfaceVariant #E4D8BB.
    static let elevatedSurface = Color.white.opacity(0.82) // Subtle iOS glass card treatment retained from the existing shell/top-bar UI.

    // MARK: - Containers

    static let primaryContainer = primaryManuscriptBlue.opacity(0.16)
    static let secondaryContainer = secondaryGold.opacity(0.18)

    // MARK: - Text

    static let textPrimary = Color.black
    static let textSecondary = Color.black.opacity(0.75)
    static let textMuted = Color.black.opacity(0.55)
    static let textDisabled = Color.black.opacity(0.35)

    // MARK: - Lines

    static let border = primaryManuscriptBlue.opacity(0.20)
    static let hairline = primaryManuscriptBlue.opacity(0.14)

    // MARK: - Rewards / Semantic

    static let rewardPearl = Color(red: 217.0 / 255.0, green: 192.0 / 255.0, blue: 138.0 / 255.0)
    static let rewardMovement = androidPrimaryTeal
    static let rewardArc = Color(red: 140.0 / 255.0, green: 106.0 / 255.0, blue: 168.0 / 255.0)
    static let rewardSurge = Color(red: 209.0 / 255.0, green: 180.0 / 255.0, blue: 90.0 / 255.0)
    static let success = Color(red: 47.0 / 255.0, green: 143.0 / 255.0, blue: 134.0 / 255.0)
    static let warning = Color(red: 204.0 / 255.0, green: 138.0 / 255.0, blue: 62.0 / 255.0)
    static let error = Color(red: 127.0 / 255.0, green: 9.0 / 255.0, blue: 9.0 / 255.0)
}
