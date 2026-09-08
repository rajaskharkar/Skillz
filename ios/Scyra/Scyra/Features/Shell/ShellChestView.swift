import SwiftUI

struct ShellChestView: View {
    @ObservedObject var viewModel: ShellViewModel
    let focusInstanceID: String?
    let focusSpeciesID: String?
    let onOpenBlue: () -> Void
    let onFocusConsumed: (Bool) -> Void
    @State private var selectedStack: ChestInventoryStack?
    @State private var pendingReleaseInstanceID: String?

    private let columns = [GridItem(.adaptive(minimum: 104, maximum: 104), spacing: 10)]

    init(
        viewModel: ShellViewModel,
        focusInstanceID: String? = nil,
        focusSpeciesID: String? = nil,
        onOpenBlue: @escaping () -> Void,
        onFocusConsumed: @escaping (Bool) -> Void = { _ in }
    ) {
        self.viewModel = viewModel
        self.focusInstanceID = focusInstanceID
        self.focusSpeciesID = focusSpeciesID
        self.onOpenBlue = onOpenBlue
        self.onFocusConsumed = onFocusConsumed
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                header
                if viewModel.allChestStacks.isEmpty {
                    emptyChest
                } else {
                    controls
                    inventory
                }
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .background(shellBackground)
        .sheet(item: $selectedStack) { stack in stackDetail(stack) }
        .confirmationDialog(
            "Release this creature?",
            isPresented: Binding(
                get: { pendingReleaseInstanceID != nil },
                set: { if !$0 { pendingReleaseInstanceID = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Release for \(pendingReleaseValue) Pearls", role: .destructive) {
                guard let id = pendingReleaseInstanceID else { return }
                pendingReleaseInstanceID = nil
                selectedStack = nil
                viewModel.releaseCreature(instanceID: id)
            }
            Button("Keep creature", role: .cancel) { pendingReleaseInstanceID = nil }
        } message: {
            Text("The copy leaves your active collection. Discovery, Level-99 mastery, and earned collection evidence remain permanent.")
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { viewModel.pendingMasteryCelebration != nil },
                set: { if !$0 { viewModel.acknowledgeMasteryCelebration() } }
            )
        ) {
            masteryCelebration
        }
        .onAppear {
            viewModel.refresh()
            var focusedStack: ChestInventoryStack?
            if let focusInstanceID {
                focusedStack = viewModel.allChestStacks.first { $0.instanceIDs.contains(focusInstanceID) }
            } else if let focusSpeciesID {
                focusedStack = viewModel.allChestStacks.first { $0.findID == focusSpeciesID }
            }
            selectedStack = focusedStack
            if focusInstanceID != nil || focusSpeciesID != nil {
                onFocusConsumed(focusedStack != nil)
            }
        }
        .accessibilityIdentifier("shell-chest-screen")
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("The Chest")
                .font(.system(size: 20, weight: .bold))
            Text("Your creatures from The Blue.")
                .font(ScyraTypography.body)
                .foregroundStyle(ScyraColors.textPrimary.opacity(0.78))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(ScyraColors.surface.opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(ScyraColors.secondaryGold.opacity(0.24), lineWidth: 1)
        }
        .accessibilityElement(children: .combine)
    }

    private var emptyChest: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 100)
            Text("The Chest is empty.")
                .font(.system(size: 22, weight: .bold))
                .multilineTextAlignment(.center)
            Text("Dive into The Blue to find your first creature.")
                .font(ScyraTypography.body)
                .foregroundStyle(ScyraColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 6)
                .padding(.bottom, 16)
            ScyraButton("Go to The Blue", action: onOpenBlue)
            Spacer(minLength: 100)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("shell-chest-empty")
    }

    private var inventoryStats: some View {
        let creatureCount = viewModel.chestStacks.reduce(0) { $0 + $1.count }
        return Text("\(creatureCount) creatures owned · \(viewModel.chestStacks.count) stacks")
            .font(ScyraTypography.label)
            .foregroundStyle(ScyraColors.textSecondary)
            .lineLimit(1)
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
            inventoryStats
            HStack(spacing: ScyraSpacing.sm) {
                Menu {
                    ForEach(ChestSortOption.allCases, id: \.self) { option in
                        Button(option.title) { viewModel.chestSort = option }
                    }
                } label: {
                    Text("Sort: \(viewModel.chestSort.title)")
                        .font(ScyraTypography.label)
                        .foregroundStyle(ScyraColors.onSecondary)
                        .padding(.horizontal, 12)
                        .frame(minHeight: 32)
                        .background(ScyraColors.secondaryGold, in: Capsule())
                }
                Menu {
                    ForEach(canonicalFilters, id: \.self) { option in
                        Button(option.title) { viewModel.chestFilter = option }
                    }
                } label: {
                    Text(viewModel.chestFilter.title)
                        .font(ScyraTypography.label)
                        .foregroundStyle(viewModel.chestFilter == .all ? ScyraColors.textPrimary : ScyraColors.onSecondary)
                        .padding(.horizontal, 12)
                        .frame(minHeight: 32)
                        .background(viewModel.chestFilter == .all ? ScyraColors.surface : ScyraColors.secondaryGold, in: Capsule())
                        .overlay(Capsule().stroke(ScyraColors.outline, lineWidth: viewModel.chestFilter == .all ? 1 : 0))
                }
                Spacer(minLength: 0)
            }
        }
    }

    private var canonicalFilters: [ChestFilterOption] {
        [.all, .closestToMastery, .mastered, .notMastered, .neededForTrackedBadges,
         .sunlitReef, .deeperReef, .openBlue, .greatBlue,
         .fishbowl, .aquarium, .pond, .lake]
    }

    @ViewBuilder private var inventory: some View {
        if viewModel.chestStacks.isEmpty {
            VStack(spacing: 8) {
                Text(viewModel.chestFilter == .neededForTrackedBadges
                     ? "None of your tracked badges currently require creatures from The Chest."
                     : "No creatures match this filter.")
                    .font(.system(size: 16, weight: .medium))
                    .multilineTextAlignment(.center)
                Button("Clear filter") { viewModel.chestFilter = .all }
                    .font(ScyraTypography.button)
                    .foregroundStyle(ScyraColors.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 80)
        } else {
            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(viewModel.chestStacks) { stack in
                    inventoryTile(stack)
                }
            }
        }
    }

    private func inventoryTile(_ stack: ChestInventoryStack) -> some View {
        Button { selectedStack = stack } label: {
            ZStack {
                ScyraColors.surface
                ScyraCanonicalIcon(systemName: stack.systemImage, size: 54)
                    .foregroundStyle(stack.kind == .animal ? ScyraColors.primary : ScyraColors.secondaryGold)
                    .accessibilityHidden(true)
                if stack.count > 1 {
                    chestBadge("x\(stack.count)")
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                        .padding(8)
                }
                chestBadge(levelBadge(stack))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    .padding(8)
            }
            .frame(width: 104, height: 104)
            .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
            .shadow(color: .black.opacity(0.08), radius: 3, y: 1)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(stack.title). Level \(stack.level). Owned: \(stack.count).")
        .accessibilityIdentifier("chest-stack-\(stack.findID)-\(stack.level)")
    }

    private func levelBadge(_ stack: ChestInventoryStack) -> String {
        switch stack.level {
        case 99...: "Mastered"
        case 98: "1 to mastery"
        case 95...: "Near mastery"
        case 90...: "\(99 - stack.level) levels to mastery"
        default: "Lv \(stack.level)"
        }
    }

    private func chestBadge(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(ScyraColors.onPrimary)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(ScyraColors.primary, in: Capsule())
    }

    private func stackDetail(_ stack: ChestInventoryStack) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    ScyraCanonicalIcon(systemName: stack.systemImage)
                        .font(.system(size: 48, weight: .semibold)).foregroundStyle(ScyraColors.primary)
                        .frame(maxWidth: .infinity).accessibilityHidden(true)
                    Text(stack.title).font(ScyraTypography.screenTitle)
                    Text(ShellContentCatalog.definition(stack.findID)?.description ?? "")
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    HStack { ScyraStatPill(label: "Owned", value: "\(stack.count)"); if stack.level > 0 { ScyraStatPill(label: "Level", value: "\(stack.level)") } }
                    if let instanceID = stack.instanceIDs.first,
                       let instance = viewModel.instance(id: instanceID),
                       let definition = ShellContentCatalog.definition(stack.findID) {
                        if definition.kind == .animal {
                            if stack.level < CreatureEconomy.maxLevel {
                                let cost = CreatureEconomy.growthCostPearls(stack.findID, currentLevel: stack.level)
                                VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                                    Text("Grow toward Level 99").font(ScyraTypography.label)
                                    Text("The next level costs \(cost) Pearls.")
                                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                                    ScyraButton(
                                        "Grow to Level \(stack.level + 1)",
                                        systemImage: "arrow.up.circle.fill",
                                        isDisabled: viewModel.pearlBalance < cost
                                    ) {
                                        viewModel.growCreature(instanceID: viewModel.preferredInstanceID(for: stack) ?? instanceID)
                                        selectedStack = nil
                                    }
                                }
                            } else {
                                ScyraCanonicalLabel("Mastered at Level 99", systemImage: "sparkles")
                                    .font(ScyraTypography.cardTitle).foregroundStyle(ScyraColors.secondaryGold)
                            }
                            let releaseID = viewModel.preferredInstanceID(for: stack) ?? instanceID
                            let releaseValue = CreatureEconomy.releaseValuePearls(stack.findID, level: stack.level)
                            ScyraButton(
                                "Release for \(releaseValue) Pearls",
                                systemImage: "water.waves",
                                variant: .destructive
                            ) {
                                selectedStack = nil
                                pendingReleaseInstanceID = releaseID
                            }
                        } else if let next = ShellContentCatalog.nextUpgrade(findID: stack.findID, currentStageID: instance.currentUpgradeStageID) {
                            ScyraButton("\(next.verb) for \(next.pearlCost) Pearls", systemImage: "sparkles", isDisabled: viewModel.pearlBalance < next.pearlCost) {
                                viewModel.upgrade(instanceID: instanceID)
                                selectedStack = nil
                            }
                        }
                        let slots = ShellContentCatalog.focusSlots.filter { ShellContentCatalog.isCompatible(slot: $0, find: definition) }
                        if !slots.isEmpty {
                            Menu {
                                ForEach(slots) { slot in Button(slot.title) { viewModel.place(instanceID: instanceID, slotID: slot.id); selectedStack = nil } }
                            } label: {
                                ScyraCanonicalLabel("Place in Focus Room", systemImage: "scope")
                                    .font(ScyraTypography.button).foregroundStyle(ScyraColors.primary)
                            }
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(shellBackground)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { selectedStack = nil } } }
        }
        .presentationDetents([.medium, .large])
    }

    private var pendingReleaseValue: Int {
        guard let id = pendingReleaseInstanceID,
              let instance = viewModel.instance(id: id) else { return 0 }
        return CreatureEconomy.releaseValuePearls(instance.findID, level: instance.animalLevel)
    }

    private var masteryCelebration: some View {
        ZStack {
            LinearGradient(
                colors: [ScyraColors.primary, ScyraColors.backgroundBottom],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ).ignoresSafeArea()
            if let celebration = viewModel.pendingMasteryCelebration,
               let creature = CreatureCatalog.definition(celebration.speciesID) {
                VStack(spacing: ScyraSpacing.xl) {
                    ScyraCanonicalIcon(systemName: creature.systemImage)
                        .font(.system(size: 92, weight: .bold))
                        .foregroundStyle(.white)
                        .symbolEffect(.bounce, value: celebration.id)
                        .accessibilityHidden(true)
                    Text("Level 99")
                        .font(ScyraTypography.screenTitle).foregroundStyle(.white)
                    Text("\(creature.displayName) Mastered")
                        .font(ScyraTypography.screenTitle).foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                    Text("Mastery evidence and collection progress have been recorded permanently.")
                        .font(ScyraTypography.body).foregroundStyle(.white.opacity(0.84))
                        .multilineTextAlignment(.center)
                    ScyraButton("Continue", systemImage: "sparkles", variant: .secondary) {
                        viewModel.acknowledgeMasteryCelebration()
                    }
                }
                .padding(ScyraSpacing.xl)
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("mastery-celebration")
            }
        }
    }

    private var shellBackground: some View {
        LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom).ignoresSafeArea()
    }
}

#Preview {
    ShellChestView(viewModel: ShellViewModel(repository: InMemoryFlowRepository()), onOpenBlue: {})
}
