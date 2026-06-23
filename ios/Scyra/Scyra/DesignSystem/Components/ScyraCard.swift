import SwiftUI

enum ScyraCardStyle {
    case plain
    case elevated
}

struct ScyraCard<Content: View>: View {
    private let style: ScyraCardStyle
    private let padding: CGFloat
    private let action: (() -> Void)?
    private let content: Content

    init(style: ScyraCardStyle = .plain, padding: CGFloat = ScyraSpacing.md, action: (() -> Void)? = nil, @ViewBuilder content: () -> Content) {
        self.style = style
        self.padding = padding
        self.action = action
        self.content = content()
    }

    var body: some View {
        Group {
            if let action {
                Button(action: action) { cardBody }
                    .buttonStyle(.plain)
            } else {
                cardBody
            }
        }
    }

    private var cardBody: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(ScyraColors.elevatedSurface)
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous)
                    .stroke(ScyraColors.border, lineWidth: 1)
            )
            .shadow(
                color: style == .elevated ? ScyraColors.primaryManuscriptBlue.opacity(0.12) : .clear,
                radius: style == .elevated ? 18 : 0,
                x: 0,
                y: style == .elevated ? 8 : 0
            )
    }
}

#Preview("ScyraCard") {
    VStack(spacing: ScyraSpacing.md) {
        ScyraCard { Text("A quiet card for Scyra content.").font(ScyraTypography.body) }
        ScyraCard(style: .elevated) { Text("Elevated manuscript note.").font(ScyraTypography.cardTitle) }
    }
    .padding()
    .background(ScyraColors.background)
}
