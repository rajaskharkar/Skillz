import SwiftUI

/// Backwards-compatible shim for older call sites. New code should use `ScyraColors`
/// or `ScyraTheme.standard` directly.
enum ScyraColor {
    static let primaryTeal = ScyraColors.primary
    static let legacyRavenclawBlue = ScyraColors.legacyRavenclawBlue
    static let backgroundTop = ScyraColors.background
    static let backgroundBottom = ScyraColors.backgroundBottom
    static let textPrimary = ScyraColors.textPrimary
    static let textSecondary = ScyraColors.textSecondary
    static let textMuted = ScyraColors.textMuted
    static let cardBackground = ScyraColors.elevatedSurface
    static let topBarBackground = ScyraColors.primary
    static let topBarSelectedBackground = ScyraColors.onPrimary.opacity(0.18)
    static let topBarHairline = Color.clear
    static let pearlGold = ScyraColors.rewardPearl
}
