import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

enum ScyraTypography {
    enum FontName {
        /// Expected PostScript name for the Caveat semibold font if it is manually bundled later.
        /// The font file is intentionally not committed in this PR, so `appTitleResolved`
        /// falls back to a rounded semibold system font until registration is completed.
        static let appTitle = "Caveat-SemiBold"
    }

    static let appTitle = Font.custom(FontName.appTitle, size: 30, relativeTo: .largeTitle)
    static let appTitleFallback = Font.system(size: 30, weight: .semibold, design: .rounded)

    static var appTitleResolved: Font {
        isAppTitleFontRegistered ? appTitle : appTitleFallback
    }

    static let wordmark = Font.custom(FontName.appTitle, size: 44, relativeTo: .largeTitle)
    static var topBarTitle: Font { appTitleResolved }
    static let screenTitle = Font.system(.largeTitle, design: .rounded).weight(.bold)
    static let cardTitle = Font.system(.title3, design: .rounded).weight(.semibold)
    static let body = Font.system(.body, design: .default)
    static let label = Font.system(.subheadline, design: .rounded).weight(.medium)
    static let button = Font.system(.headline, design: .rounded).weight(.semibold)
    static let caption = Font.system(.caption, design: .default)
    static let rewardNumber = Font.system(.title2, design: .rounded).weight(.bold)

    /// SF Symbol sizing is centralized separately from text tokens so navigation icons do not
    /// scatter hardcoded system font values through feature views.
    static let navigationIcon = Font.system(size: 24, weight: .semibold)

    #if canImport(UIKit)
    static var isAppTitleFontRegistered: Bool {
        UIFont(name: FontName.appTitle, size: 30) != nil
    }
    #else
    static let isAppTitleFontRegistered = true
    #endif
}
