import SwiftUI

struct ScyraTypographyPreview: View {
    private let samples: [TypographySample] = [
        TypographySample(name: "App title", text: "Scyra", font: ScyraTypography.appTitleResolved),
        TypographySample(name: "Screen title", text: "Today’s Story", font: ScyraTypography.screenTitle),
        TypographySample(name: "Card title", text: "Quiet progress", font: ScyraTypography.cardTitle),
        TypographySample(name: "Body", text: "Flow gently records the work and keeps the moment part of your Story.", font: ScyraTypography.body),
        TypographySample(name: "Label", text: "CURRENT ARC", font: ScyraTypography.label),
        TypographySample(name: "Button", text: "Begin Flow", font: ScyraTypography.button),
        TypographySample(name: "Caption", text: "Updated after each completed session.", font: ScyraTypography.caption),
        TypographySample(name: "Reward number", text: "+24", font: ScyraTypography.rewardNumber)
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                Text("Scyra Typography")
                    .font(ScyraTypography.screenTitle)
                    .foregroundStyle(ScyraColors.textPrimary)

                Text("App title font registered: \(ScyraTypography.isAppTitleFontRegistered ? "Yes" : "Fallback")")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)

                VStack(spacing: ScyraSpacing.md) {
                    ForEach(samples) { sample in
                        TypographySampleRow(sample: sample)
                    }
                }
            }
            .padding(ScyraSpacing.lg)
        }
        .background(ScyraColors.background)
    }
}

private struct TypographySample: Identifiable {
    let id = UUID()
    let name: String
    let text: String
    let font: Font
}

private struct TypographySampleRow: View {
    let sample: TypographySample

    var body: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
            Text(sample.name)
                .font(ScyraTypography.label)
                .foregroundStyle(ScyraColors.textMuted)

            Text(sample.text)
                .font(sample.font)
                .foregroundStyle(ScyraColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(ScyraSpacing.md)
        .background(ScyraColors.elevatedSurface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
    }
}

#Preview("Scyra typography") {
    ScyraTypographyPreview()
}
