import CoreGraphics

enum ScyraSpacing {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 16
    static let lg: CGFloat = 24
    static let xl: CGFloat = 32
    /// Canonical Compose screens use 16dp horizontal content padding.
    static let screenPadding: CGFloat = 16
    /// Material icon buttons use a 48dp interaction target.
    static let topBarTapTarget: CGFloat = 48
}
