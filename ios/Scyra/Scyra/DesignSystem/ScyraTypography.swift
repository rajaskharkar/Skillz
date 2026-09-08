import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

enum ScyraTypography {
    enum FontName {
        /// PostScript name of Android's canonical bundled `caveatsb.ttf`.
        static let appTitle = "Caveat-SemiBold"
    }

    static let appTitle = Font.custom(FontName.appTitle, size: 30, relativeTo: .largeTitle)
    static let appTitleFallback = Font.system(size: 30, weight: .semibold, design: .rounded)

    static var appTitleResolved: Font {
        isAppTitleFontRegistered ? appTitle : appTitleFallback
    }

    static let wordmark = Font.custom(FontName.appTitle, size: 44, relativeTo: .largeTitle)
    static let handwrittenLabel = Font.custom(FontName.appTitle, size: 16, relativeTo: .body)
    static var topBarTitle: Font { appTitleResolved }
    static var handwrittenLabelResolved: Font {
        isAppTitleFontRegistered
            ? handwrittenLabel
            : Font.system(size: 16, weight: .medium, design: .rounded).italic()
    }
    static let screenTitle = Font.system(size: 28, weight: .semibold, design: .default)
    static let cardTitle = Font.system(size: 16, weight: .semibold, design: .default)
    static let body = Font.system(.subheadline, design: .default)
    static let label = Font.system(size: 14, weight: .medium, design: .default)
    static let button = Font.system(size: 14, weight: .semibold, design: .default)
    static let caption = Font.system(.caption, design: .default)
    static let rewardNumber = Font.system(.title2, design: .default).weight(.bold)

    /// SF Symbol sizing is centralized separately from text tokens so navigation icons do not
    /// scatter hardcoded system font values through feature views.
    static let navigationIcon = Font.system(size: 22, weight: .regular)

    #if canImport(UIKit)
    static var isAppTitleFontRegistered: Bool {
        UIFont(name: FontName.appTitle, size: 30) != nil
    }
    #else
    static let isAppTitleFontRegistered = true
    #endif
}
