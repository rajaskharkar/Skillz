import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

/// Centralized SwiftUI color tokens for the Scyra design system.
///
/// Scyra primary color: SlytherinButNiceTeal `#3F8F8B`.
/// This teal is the canonical active primary for Android, iOS, navigation,
/// primary controls, containers, borders, hairlines, and brand UI accents.
/// Legacy RavenclawBlue/manuscript blue `#2F4F6F` is not the active Scyra primary.
enum ScyraColors {
    // MARK: - Brand

    static let slytherinButNiceTeal = color(0x3F8F8B) // Canonical Scyra primary #3F8F8B.
    static let primary = slytherinButNiceTeal
    static let primaryTeal = slytherinButNiceTeal
    static let legacyRavenclawBlue = color(0x2F4F6F) // Legacy/deprecated; not active Scyra primary.
    /// Android uses AntiqueGold in light mode and Bronze in dark mode.
    static let secondaryGold = adaptive(light: 0xB8A56A, dark: 0xB7893A)
    static let onPrimary = color(0xF2EBDD)
    static let onSecondary = Color.black

    // MARK: - Surfaces

    static let background = adaptive(light: 0xF2EBDD, dark: 0x1A1412)
    static let backgroundBottom = background
    static let surface = adaptive(light: 0xE4D8BB, dark: 0x221C19)
    static let surfaceVariant = surface
    /// Android cards are opaque Material surfaces, not translucent iOS glass.
    static let elevatedSurface = surface

    // MARK: - Containers

    static let primaryContainer = primary.opacity(0.16)
    static let secondaryContainer = secondaryGold.opacity(0.18)

    // MARK: - Text

    static let textPrimary = adaptive(light: 0x000000, dark: 0xF5F5F5)
    static let textSecondary = adaptive(light: 0x000000, dark: 0xF5F5F5).opacity(0.75)
    static let textMuted = adaptive(light: 0x000000, dark: 0xF5F5F5).opacity(0.55)
    static let textDisabled = adaptive(light: 0x000000, dark: 0xF5F5F5).opacity(0.35)

    // MARK: - Lines

    static let border = primary.opacity(0.20)
    static let hairline = primary.opacity(0.14)
    /// Material 3 defaults used by Android because Scyra does not override these roles.
    static let outline = adaptive(light: 0x79747E, dark: 0x938F99)
    static let outlineVariant = adaptive(light: 0xCAC4D0, dark: 0x49454F)

    // MARK: - Rewards / Semantic

    static let rewardPearl = color(0xD9C08A)
    static let rewardMovement = primary
    static let rewardArc = color(0x8C6AA8)
    static let rewardSurge = color(0xD1B45A)
    static let success = color(0x2F8F86)
    static let warning = color(0xCC8A3E)
    static let error = color(0x7F0909)

    private static func color(_ rgb: UInt32) -> Color {
        Color(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }

    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        #if canImport(UIKit)
        Color(uiColor: UIColor { traits in
            let rgb = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(
                red: CGFloat((rgb >> 16) & 0xFF) / 255,
                green: CGFloat((rgb >> 8) & 0xFF) / 255,
                blue: CGFloat(rgb & 0xFF) / 255,
                alpha: 1
            )
        })
        #else
        color(light)
        #endif
    }
}
