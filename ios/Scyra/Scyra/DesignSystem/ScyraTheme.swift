import SwiftUI

struct ScyraTheme {
    let primary: Color
    let secondary: Color
    let background: Color
    let surface: Color
    let primaryContainer: Color
    let secondaryContainer: Color
    let textPrimary: Color
    let textSecondary: Color
    let textMuted: Color
    let border: Color
    let error: Color
    let warning: Color
    let success: Color

    static let standard = ScyraTheme(
        primary: ScyraColors.primaryManuscriptBlue,
        secondary: ScyraColors.secondaryGold,
        background: ScyraColors.background,
        surface: ScyraColors.surface,
        primaryContainer: ScyraColors.primaryContainer,
        secondaryContainer: ScyraColors.secondaryContainer,
        textPrimary: ScyraColors.textPrimary,
        textSecondary: ScyraColors.textSecondary,
        textMuted: ScyraColors.textMuted,
        border: ScyraColors.border,
        error: ScyraColors.error,
        warning: ScyraColors.warning,
        success: ScyraColors.success
    )
}

private struct ScyraThemeKey: EnvironmentKey {
    static let defaultValue = ScyraTheme.standard
}

extension EnvironmentValues {
    var scyraTheme: ScyraTheme {
        get { self[ScyraThemeKey.self] }
        set { self[ScyraThemeKey.self] = newValue }
    }
}
