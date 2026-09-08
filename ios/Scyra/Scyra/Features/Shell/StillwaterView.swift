import SwiftUI

struct StillwaterView: View {
    @ObservedObject var viewModel: ShellViewModel
    let onOpenChest: () -> Void
    let focusCollectionID: String?
    let focusSpeciesID: String?
    let onFocusConsumed: (Bool) -> Void
    @State private var focusedVessel: StillwaterVessel?
    @State private var focusedSpeciesID: String?

    init(
        viewModel: ShellViewModel,
        onOpenChest: @escaping () -> Void,
        focusCollectionID: String? = nil,
        focusSpeciesID: String? = nil,
        onFocusConsumed: @escaping (Bool) -> Void = { _ in }
    ) {
        self.viewModel = viewModel
        self.onOpenChest = onOpenChest
        self.focusCollectionID = focusCollectionID
        self.focusSpeciesID = focusSpeciesID
        self.onFocusConsumed = onFocusConsumed
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    roomHeader
                    dropsCard
                    if let overall = viewModel.progress(for: "collection_stillwater") {
                        collectionCard(overall)
                            .id("stillwater-collection-all")
                    }
                    Text("Choose a vessel. Something quiet may surface.")
                        .font(ScyraTypography.cardTitle)
                        .foregroundStyle(ScyraColors.textPrimary)
                    ForEach(orderedVessels) { vessel in
                        vesselCard(vessel)
                            .id("stillwater-vessel-\(vessel.rawValue)")
                        if let progress = viewModel.progress(for: "stillwater_\(vessel.rawValue)") {
                            collectionCard(progress)
                        }
                    }
                    explainer
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(background)
            .onAppear {
                viewModel.refresh()
                applyInitialFocus(using: proxy)
            }
        }
        .confirmationDialog(
            confirmationTitle,
            isPresented: Binding(
                get: { viewModel.pendingStillwaterConfirmation != nil },
                set: { if !$0 { viewModel.dismissStillwaterConfirmation() } }
            ),
            titleVisibility: .visible
        ) {
            Button("Draw", action: viewModel.confirmStillwaterDraw)
            Button("Cancel", role: .cancel, action: viewModel.dismissStillwaterConfirmation)
        } message: {
            if let vessel = viewModel.pendingStillwaterConfirmation {
                Text("This will spend \(vessel.dropCost.formatted()) Drops.")
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.stillwaterReveal != nil },
                set: { if !$0 { viewModel.dismissStillwaterReveal() } }
            )
        ) {
            if let reveal = viewModel.stillwaterReveal { revealSheet(reveal) }
        }
        .alert(
            "Stillwater",
            isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.dismissError() } }
            )
        ) {
            Button("OK", action: viewModel.dismissError)
        } message: {
            Text(viewModel.errorMessage ?? "Something went quiet. Please try again.")
        }
        .accessibilityIdentifier("stillwater-screen")
    }

    private var orderedVessels: [StillwaterVessel] {
        StillwaterVessel.allCases
    }

    private var roomHeader: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
            Text("Stillwater")
                .font(ScyraTypography.screenTitle)
            Text("Soft flows gather Drops. Spend them in quiet vessels to draw Stillwater-exclusive creatures.")
                .font(ScyraTypography.body)
                .foregroundStyle(ScyraColors.textSecondary)
        }
        .accessibilityElement(children: .combine)
    }

    private var dropsCard: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColors.primary, ScyraColors.primary.opacity(0.76)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            ShellInteriorDrawing()
            GeometryReader { proxy in
                Canvas { context, size in
                    for index in 0..<5 {
                        let radius = CGFloat(56 + index * 38)
                        let rect = CGRect(
                            x: size.width * 0.72 - radius,
                            y: size.height * 0.35 - radius,
                            width: radius * 2,
                            height: radius * 2
                        )
                        context.stroke(
                            Path(ellipseIn: rect),
                            with: .color(.white.opacity(0.07 - Double(index) * 0.008)),
                            lineWidth: 1
                        )
                    }
                    for index in 0..<4 {
                        let radius = CGFloat(36 + index * 28)
                        let rect = CGRect(
                            x: size.width * 0.22 - radius,
                            y: size.height * 0.78 - radius,
                            width: radius * 2,
                            height: radius * 2
                        )
                        context.stroke(
                            Path(ellipseIn: rect),
                            with: .color(.white.opacity(0.05 - Double(index) * 0.007)),
                            lineWidth: 1
                        )
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                HStack {
                    Text("Stillwater").font(ScyraTypography.label)
                    Spacer()
                    if hasAvailableDraw {
                        Text("Ready to draw")
                            .font(ScyraTypography.caption)
                            .padding(.horizontal, ScyraSpacing.sm)
                            .padding(.vertical, ScyraSpacing.xs)
                            .background(.white.opacity(0.14))
                            .clipShape(Capsule())
                    }
                }
                Spacer()
                Text("\(viewModel.stillwaterDrops.formatted()) Drops available")
                    .font(.system(size: 28, weight: .bold))
                    .monospacedDigit()
                    .accessibilityIdentifier("stillwater-available-drops")
                Text("\(viewModel.stillwaterLifetimeDrops.formatted()) Drops gathered all time")
                    .font(ScyraTypography.body)
                    .opacity(0.8)
                    .monospacedDigit()
                Text("Soft flows gather Drops.")
                    .font(ScyraTypography.caption)
                    .opacity(0.76)
            }
            .foregroundStyle(.white)
            .padding(22)
        }
        .frame(minHeight: 240)
        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(viewModel.stillwaterDrops) Drops available. \(viewModel.stillwaterLifetimeDrops) Drops gathered all time.\(hasAvailableDraw ? " Ready to draw." : "")")
    }

    private func vesselCard(_ vessel: StillwaterVessel) -> some View {
        let unlocked = viewModel.isVesselUnlocked(vessel)
        let affordable = viewModel.stillwaterDrops >= vessel.dropCost
        let focused = focusedVessel == vessel
        return VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(vessel.title).font(.system(size: 22, weight: .bold))
                        Text("\(vessel.dropCost.formatted()) Drops")
                            .font(.system(size: 14, weight: .semibold)).foregroundStyle(ScyraColors.secondaryGold)
                    }
                    Spacer()
                    Text(!unlocked ? "Locked" : affordable ? "Ready" : "Filling")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(affordable && unlocked ? ScyraColors.onSecondary : ScyraColors.textPrimary)
                        .padding(.horizontal, 10).padding(.vertical, 6)
                        .background(statusBackground(unlocked: unlocked, affordable: affordable))
                        .clipShape(Capsule())
                }
                Text(vessel.rewardDescription).font(.system(size: 16))
                Text(vessel.categoryDescription).font(.system(size: 14)).foregroundStyle(ScyraColors.textSecondary)
                if focused, let focusedSpeciesID,
                   let creature = CreatureCatalog.definition(focusedSpeciesID) {
                    HStack(spacing: ScyraSpacing.sm) {
                        ScyraCanonicalIcon(systemName: creature.systemImage, size: 30)
                            .foregroundStyle(ScyraColors.primary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(creature.displayName).font(ScyraTypography.cardTitle)
                            Text(viewModel.isCreatureDiscovered(creature.id) ? "Discovered · draw another here" : "Undiscovered · this creature surfaces here")
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(ScyraColors.secondaryContainer, in: RoundedRectangle(cornerRadius: ScyraRadius.card))
                    .accessibilityIdentifier("stillwater-focused-species-\(creature.id)")
                }
                if !unlocked {
                    Text("Reach this depth in The Blue first.")
                        .font(.system(size: 14)).foregroundStyle(ScyraColors.textMuted)
                } else if !affordable {
                    ProgressView(value: viewModel.stillwaterProgressValue(for: vessel))
                        .tint(ScyraColors.primary)
                    Text("\(viewModel.stillwaterDrops.formatted()) / \(vessel.dropCost.formatted()) Drops")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    Text("\(viewModel.stillwaterDropsNeeded(for: vessel).formatted()) more needed")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                }
                HStack {
                    Spacer()
                    ScyraButton(
                        affordable && unlocked ? "Draw" : "Keep filling",
                        isDisabled: !unlocked || !affordable
                    ) { viewModel.requestStillwaterDraw(vessel) }
                    .accessibilityIdentifier("stillwater-draw-\(vessel.rawValue)")
                }
        }
        .padding(16)
        .background(ScyraColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(
            focused ? ScyraColors.primary : ScyraColors.outlineVariant,
            lineWidth: focused ? 3 : 1
        ))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("stillwater-vessel-\(vessel.rawValue)")
    }

    private func collectionCard(_ progress: StillwaterCollectionProgress) -> some View {
        ScyraCard {
            VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                Text("\(progress.title) collection").font(ScyraTypography.label)
                Text("Discovered \(progress.discovered) of \(progress.total)")
                Text("Owned \(progress.owned) of \(progress.total)")
                Text("Mastered \(progress.mastered) of \(progress.total)")
                Text("Collector: \(progress.collectorEarned ? "Earned" : "Locked")")
                Text("Curator: \(progress.curatorEarned ? "Earned" : "Locked")")
                Text("Completionist: \(progress.completionistEarned ? "Earned" : "Locked")")
            }
            .font(ScyraTypography.caption)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(progress.title) collection. \(progress.discovered) of \(progress.total) discovered, \(progress.owned) owned, \(progress.mastered) mastered. Collector \(progress.collectorEarned ? "earned" : "locked"), Curator \(progress.curatorEarned ? "earned" : "locked"), Completionist \(progress.completionistEarned ? "earned" : "locked").")
    }

    private var explainer: some View {
        Text("Each draw spends Drops and adds one exclusive creature to your Chest. Common creatures surface most often; Rare and Mythic creatures are intentionally uncommon. Discovery and collection evidence is preserved even if a creature is later released or traded.")
            .font(.system(size: 14))
            .foregroundStyle(ScyraColors.textPrimary)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(ScyraColors.secondaryContainer.opacity(0.52))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private func revealSheet(_ reveal: StillwaterDrawResult) -> some View {
        NavigationStack {
            VStack(spacing: ScyraSpacing.lg) {
                ScyraCanonicalIcon(systemName: reveal.creature.systemImage)
                    .font(.system(size: 72, weight: .semibold))
                    .foregroundStyle(ScyraColors.primary)
                    .symbolEffect(.bounce, value: reveal.instance.id)
                    .accessibilityHidden(true)
                Text("Something surfaced")
                    .font(ScyraTypography.screenTitle)
                Text(reveal.creature.displayName)
                    .font(ScyraTypography.screenTitle)
                Text(reveal.rarity.rawValue.capitalized)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.secondaryGold)
                Text(reveal.creature.zone.title)
                    .font(ScyraTypography.body)
                if reveal.wasFirstDiscovery {
                    ScyraCanonicalLabel("New discovery recorded", systemImage: "sparkles")
                        .font(ScyraTypography.label).foregroundStyle(ScyraColors.primary)
                }
                Text("Stillwater exclusive · Added to Chest")
                    .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                Text("\(reveal.remainingDrops.formatted()) Drops remain")
                    .font(ScyraTypography.caption).monospacedDigit()
                HStack {
                    ScyraButton("Done", variant: .secondary) { viewModel.dismissStillwaterReveal() }
                    ScyraButton("View in Chest", systemImage: "shippingbox.fill") {
                        viewModel.dismissStillwaterReveal()
                        onOpenChest()
                    }
                }
            }
            .padding(ScyraSpacing.xl)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(background)
        }
        .presentationDetents([.medium, .large])
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("stillwater-reveal")
    }

    private var confirmationTitle: String {
        guard let vessel = viewModel.pendingStillwaterConfirmation else { return "Draw from Stillwater?" }
        return "Draw from \(vessel.title)?"
    }

    private func applyInitialFocus(using proxy: ScrollViewProxy) {
        guard focusCollectionID != nil || focusSpeciesID != nil else { return }
        let vessel = focusSpeciesID.flatMap { StillwaterCatalog.byID[$0]?.vessel }
            ?? focusCollectionID.flatMap { id in
                guard id.hasPrefix("stillwater_") else { return nil }
                return StillwaterVessel(rawValue: String(id.dropFirst("stillwater_".count)))
            }
        focusedVessel = vessel
        focusedSpeciesID = focusSpeciesID
        if let vessel {
            viewModel.selectStillwaterPerspective(.init(vessel: vessel))
        }
        Task { @MainActor in
            await Task.yield()
            withAnimation(.easeInOut(duration: 0.25)) {
                proxy.scrollTo(
                    vessel.map { "stillwater-vessel-\($0.rawValue)" } ?? "stillwater-collection-all",
                    anchor: .center
                )
            }
        }
        onFocusConsumed(vessel != nil || focusCollectionID == "collection_stillwater")
    }

    private var hasAvailableDraw: Bool {
        StillwaterVessel.allCases.contains {
            viewModel.isVesselUnlocked($0) && viewModel.stillwaterDrops >= $0.dropCost
        }
    }

    private func statusBackground(unlocked: Bool, affordable: Bool) -> Color {
        if !unlocked { return ScyraColors.surfaceVariant }
        return affordable ? ScyraColors.secondaryGold : ScyraColors.secondaryContainer
    }

    private var background: some View {
        LinearGradient(
            colors: [ScyraColors.background, ScyraColors.backgroundBottom],
            startPoint: .top,
            endPoint: .bottom
        ).ignoresSafeArea()
    }
}

#Preview {
    StillwaterView(viewModel: ShellViewModel(repository: InMemoryFlowRepository()), onOpenChest: {})
}
