import SwiftUI

/// Renders a template vector copied from the Material icon used by Android.
/// Keeping these behind one view prevents SF Symbol metrics from leaking back
/// into product-defining Scyra surfaces.
struct ScyraMaterialIcon: View {
    let assetName: String
    var size: CGFloat = 24
    var color: Color = ScyraColors.textPrimary

    var body: some View {
        Image(assetName)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundStyle(color)
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

struct ScyraMaterialLabel: View {
    let title: String
    let assetName: String
    var iconSize: CGFloat = 20

    init(_ title: String, assetName: String, iconSize: CGFloat = 20) {
        self.title = title
        self.assetName = assetName
        self.iconSize = iconSize
    }

    var body: some View {
        HStack(spacing: 8) {
            ScyraMaterialIcon(assetName: assetName, size: iconSize, color: ScyraColors.textPrimary)
            Text(title)
        }
    }
}

/// Label equivalent that keeps Android's Material vector metrics instead of silently
/// reintroducing SF Symbols through SwiftUI's `Label(systemImage:)` convenience API.
struct ScyraCanonicalLabel: View {
    let title: String
    let systemName: String
    var iconSize: CGFloat = 20

    init(_ title: String, systemImage systemName: String, iconSize: CGFloat = 20) {
        self.title = title
        self.systemName = systemName
        self.iconSize = iconSize
    }

    var body: some View {
        HStack(spacing: 8) {
            ScyraCanonicalIcon(systemName: systemName, size: iconSize)
            Text(title)
        }
    }
}

/// Compatibility bridge for existing iOS call sites. Product icons resolve to
/// the same Material vectors imported by Android; iOS-only transport/media
/// affordances retain a native fallback until Android supplies a counterpart.
struct ScyraCanonicalIcon: View {
    let systemName: String
    var size: CGFloat = 24

    @ViewBuilder
    var body: some View {
        if Self.isPearl(systemName) {
            ShellPearlMiniIcon()
                .frame(width: size, height: size)
                .accessibilityHidden(true)
        } else if let assetName = Self.assetName(for: systemName) {
            Image(assetName)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
                .accessibilityHidden(true)
        } else {
            Image(systemName: systemName)
                .font(.system(size: size))
                .accessibilityHidden(true)
        }
    }

    private static func isPearl(_ name: String) -> Bool {
        name == "circle.hexagongrid" || name == "circle.hexagongrid.fill"
    }

    private static func assetName(for name: String) -> String? {
        switch name {
        case "magnifyingglass": "materialFilledSearch"
        case "arrow.uturn.backward": "materialFilledUndo"
        case "arrow.uturn.forward": "materialFilledRedo"
        case "arrow.up.to.line": "materialFilledVerticalAlignTop"
        case "arrow.down.to.line": "materialFilledVerticalAlignBottom"
        case "bold": "materialFilledFormatBold"
        case "italic": "materialFilledFormatItalic"
        case "underline": "materialFilledFormatUnderlined"
        case "strikethrough": "materialFilledStrikethroughS"
        case "textformat.subscript": "materialFilledSubscript"
        case "textformat.superscript": "materialFilledSuperscript"
        case "chevron.up", "arrow.up": "materialFilledKeyboardArrowUp"
        case "chevron.down", "arrow.down": "materialFilledKeyboardArrowDown"
        case "chevron.left": "materialFilledArrowBackIosNew"
        case "chevron.right": "materialFilledArrowForwardIos"
        case "arrow.right.circle": "materialOutlinedArrowForward"
        case "arrow.left": "materialOutlinedArrowBack"
        case "arrow.counterclockwise": "materialOutlinedRotateLeft"
        case "xmark": "materialFilledClose"
        case "ellipsis", "ellipsis.circle": "materialOutlinedMoreVert"
        case "trash": "materialOutlinedDelete"
        case "trash.fill": "materialFilledDelete"
        case "plus": "materialOutlinedAdd"
        case "plus.circle": "materialOutlinedAddCircleOutline"
        case "minus": "materialOutlinedRemove"
        case "play.fill": "materialFilledPlayArrow"
        case "play.circle", "play.circle.fill": "materialOutlinedPlayArrow"
        case "pause.fill": "materialFilledPause"
        case "stop.fill": "materialFilledStop"
        case "sparkles", "wand.and.stars": "materialAutoAwesome"
        case "brain.head.profile": "materialPsychologyAlt"
        case "books.vertical", "book.pages": "materialAutoStories"
        case "book.closed", "book.closed.fill": "materialMenuBook"
        case "chart.xyaxis.line", "chart.line.uptrend.xyaxis": "materialTimeline"
        case "safari": "materialExplore"
        case "cloud": "materialOutlinedCloudQueue"
        case "pin", "pin.slash": "materialOutlinedPushPin"
        case "leaf", "leaf.fill": "materialSpa"
        case "point.topleft.down.to.point.bottomright.curvepath": "materialOutlinedAutoGraph"
        case "scope": "materialOutlinedCenterFocusStrong"
        case "eye", "eye.fill", "binoculars": "materialOutlinedVisibility"
        case "drop", "drop.fill": "materialOutlinedWaterDrop"
        case "water.waves": "materialOutlinedWaves"
        case "camera.filters": "materialOutlinedFilterVintage"
        case "shippingbox", "shippingbox.fill": "materialOutlinedInventory2"
        case "rosette", "medal", "medal.fill", "checkmark.seal.fill": "materialOutlinedMilitaryTech"
        case "figure.walk": "materialOutlinedDirectionsWalk"
        case "camera": "materialOutlinedPhotoCamera"
        case "video": "materialOutlinedVideocam"
        case "mic": "materialOutlinedMicNone"
        case "photo.on.rectangle": "materialOutlinedCollections"
        case "checkmark.circle.fill": "materialOutlinedCheckCircle"
        case "checkmark": "materialFilledCheck"
        case "bell": "materialOutlinedNotifications"
        case "bolt.fill", "flame.fill": "materialOutlinedSpeed"
        case "map", "sailboat", "sailboat.fill": "materialOutlinedRoute"
        case "lock", "lock.fill": "materialOutlinedLock"
        case "square.and.pencil": "materialOutlinedEdit"
        case "photo.stack": "materialOutlinedCollections"
        case "timer", "clock": "materialFilledTimer"
        case "diamond.fill": "materialOutlinedDiamond"
        case "tree.fill": "materialOutlinedGrass"
        default: nil
        }
    }
}
