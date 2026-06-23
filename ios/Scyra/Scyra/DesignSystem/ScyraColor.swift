import SwiftUI

/// Backwards-compatible shim for older call sites. New code should use `ScyraColors` or
/// `ScyraTheme.standard` directly.
enum ScyraColor {
    static let primaryTeal = ScyraColors.primaryManuscriptBlue
    static let backgroundTop = ScyraColors.background
    static let backgroundBottom = ScyraColors.backgroundBottom
    static let textPrimary = ScyraColors.textPrimary
    static let textSecondary = ScyraColors.textSecondary
    static let textMuted = ScyraColors.textMuted
    static let cardBackground = ScyraColors.elevatedSurface
    static let topBarBackground = ScyraColors.elevatedSurface
    static let topBarSelectedBackground = ScyraColors.primaryContainer
    static let topBarHairline = ScyraColors.hairline
    static let pearlGold = ScyraColors.rewardPearl
}
