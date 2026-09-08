import SwiftUI

struct RewardRevealDeckView: View {
    let cards: [RewardRevealCard]
    let onDone: () -> Void
    let onEnterShell: (() -> Void)?
    let primaryTitle: String
    let primaryIdentifier: String

    @State private var selectedIndex = 0

    init(
        cards: [RewardRevealCard],
        onDone: @escaping () -> Void,
        onEnterShell: (() -> Void)? = nil,
        primaryTitle: String = FlowStrings.done,
        primaryIdentifier: String = "reward-done"
    ) {
        self.cards = cards
        self.onDone = onDone
        self.onEnterShell = onEnterShell
        self.primaryTitle = primaryTitle
        self.primaryIdentifier = primaryIdentifier
    }

    var body: some View {
        VStack(spacing: 10) {
            TabView(selection: $selectedIndex) {
                ForEach(Array(cards.enumerated()), id: \.element.id) { index, card in
                    RewardRevealPageView(
                        card: card,
                        isSelected: selectedIndex == index
                    )
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 360)
            .frame(maxWidth: .infinity)

            Text("\(selectedIndex + 1) of \(cards.count)")
                .font(ScyraTypography.caption)
                .foregroundStyle(ScyraColors.textSecondary)
                .accessibilityLabel("Reward \(selectedIndex + 1) of \(cards.count)")

            HStack(spacing: 6) {
                ForEach(cards.indices, id: \.self) { index in
                    Capsule()
                        .fill(index == selectedIndex ? ScyraColors.secondaryGold : ScyraColors.textPrimary.opacity(0.22))
                        .frame(width: index == selectedIndex ? 18 : 6, height: 6)
                        .animation(.easeInOut(duration: 0.18), value: selectedIndex)
                        .accessibilityHidden(true)
                }
            }

            HStack(spacing: ScyraSpacing.xs) {
                Spacer(minLength: 0)
                if let onEnterShell {
                    rewardDialogButton(FlowStrings.enterShell, action: onEnterShell)
                    .accessibilityIdentifier("reward-enter-shell")
                }
                rewardDialogButton(primaryTitle, action: onDone)
                .accessibilityIdentifier(primaryIdentifier)
            }
            .padding(.horizontal, ScyraSpacing.lg)
        }
        .padding(.top, 10)
        .padding(.bottom, ScyraSpacing.md)
        .background(ScyraColors.background.ignoresSafeArea())
    }

    private func rewardDialogButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(ScyraTypography.button)
                .foregroundStyle(ScyraColors.primary)
                .padding(.horizontal, 12)
                .frame(minHeight: 48)
        }
        .buttonStyle(.plain)
    }
}

private struct RewardRevealPageView: View {
    let card: RewardRevealCard
    let isSelected: Bool

    var body: some View {
        RewardRevealCardView(card: card)
            .padding(.horizontal, 20)
            .opacity(isSelected ? 1 : 0.72)
            .scaleEffect(isSelected ? 1 : 0.96)
            .offset(y: isSelected ? 0 : 10)
            .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}

private struct RewardRevealCardView: View {
    let card: RewardRevealCard

    private var tint: Color {
        switch card.type {
        case .scoreBreakdown, .arcScore:
            ScyraColors.primary
        default:
            ScyraColors.secondaryGold
        }
    }

    private var iconAsset: String {
        switch card.type {
        case .animal, .arcAnimals:
            "materialOutlinedPets"
        case .stillwaterResult, .arcStillwater:
            "materialOutlinedWaterDrop"
        case .badge, .arcBadges:
            "materialOutlinedEmojiEvents"
        default:
            "materialOutlinedAutoAwesome"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                ScyraMaterialIcon(assetName: iconAsset, size: 34, color: tint)
                    .scaleEffect(card.animationStyle == .none ? 1 : 1.08)
                    .frame(width: 64, height: 64)
                    .background(tint.opacity(card.type == .scoreBreakdown || card.type == .arcScore ? 0.14 : 0.16))
                    .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))

                VStack(alignment: .leading, spacing: 6) {
                    if let chip = card.chip {
                        Text(chip)
                            .font(ScyraTypography.caption)
                            .fontWeight(.medium)
                            .foregroundStyle(ScyraColors.textPrimary)
                            .padding(.horizontal, ScyraSpacing.sm)
                            .padding(.vertical, 5)
                            .background(ScyraColors.background)
                            .clipShape(Capsule())
                    }

                    Text(card.title)
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(ScyraColors.textPrimary)
                        .multilineTextAlignment(.leading)

                    if let subtitle = card.subtitle {
                        Text(subtitle)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(ScyraColors.secondaryGold)
                            .multilineTextAlignment(.leading)
                    }
                }

                if let body = card.body {
                    Text(body)
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .multilineTextAlignment(.leading)
                }

                if let destinationHint = card.destinationHint {
                    Text(destinationHint)
                        .font(ScyraTypography.label)
                        .fontWeight(.semibold)
                        .foregroundStyle(ScyraColors.primary)
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(ScyraColors.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityIdentifier("reward-card-\(card.type.rawValue)")
    }

    private var accessibilityLabel: String {
        [card.title, card.amountText, card.subtitle, card.body, card.destinationHint]
            .compactMap { $0 }
            .joined(separator: ". ")
    }
}

#Preview {
    RewardRevealDeckView(
        cards: [
            RewardRevealCard(
                id: "preview",
                type: .shellBridge,
                title: "The Shell was shaped",
                body: "Your Scyra Points were carried into The Shell as Pearls.",
                systemImage: "circle.hexagongrid.fill",
                destinationHint: "Shape The Shell with Pearls.",
                animationStyle: .pearlGlow
            )
        ],
        onDone: {}
    )
}
