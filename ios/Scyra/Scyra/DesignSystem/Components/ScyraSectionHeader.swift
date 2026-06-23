import SwiftUI

struct ScyraSectionHeader<Trailing: View>: View {
    let title: String
    let subtitle: String?
    private let trailing: Trailing

    init(title: String, subtitle: String? = nil, @ViewBuilder trailing: () -> Trailing) {
        self.title = title; self.subtitle = subtitle; self.trailing = trailing()
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: ScyraSpacing.md) {
            VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                Text(title).font(ScyraTypography.cardTitle).foregroundStyle(ScyraColors.textPrimary)
                if let subtitle { Text(subtitle).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary) }
            }
            Spacer(minLength: ScyraSpacing.sm)
            trailing
        }
    }
}

extension ScyraSectionHeader where Trailing == EmptyView {
    init(title: String, subtitle: String? = nil) { self.init(title: title, subtitle: subtitle) { EmptyView() } }
}

#Preview("ScyraSectionHeader") { VStack { ScyraSectionHeader(title: "Today", subtitle: "Your current Flow story"); ScyraSectionHeader(title: "Rewards") { ScyraChip("Preview", isSelected: true) } }.padding().background(ScyraColors.background) }
