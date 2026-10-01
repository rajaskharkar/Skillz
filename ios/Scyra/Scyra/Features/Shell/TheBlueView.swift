import SwiftUI

private struct TheBlueAnimalGroup: Identifiable, Equatable {
    let definition: CreatureDefinition
    let instances: [ShellFindInstance]
    let displayedInstanceIDs: Set<String>

    var id: String { definition.id }
    var totalCount: Int { instances.count }
    var displayedCount: Int { instances.count { displayedInstanceIDs.contains($0.id) } }
    var restingCount: Int { totalCount - displayedCount }
    var highestLevel: Int { instances.map(\.animalLevel).max() ?? 1 }
    var isNew: Bool { instances.contains(where: \.isNew) }
    var levelGroups: [(level: Int, instances: [ShellFindInstance])] {
        Dictionary(grouping: instances, by: { max(1, $0.animalLevel) })
            .map { ($0.key, $0.value.sorted { $0.acquiredAt < $1.acquiredAt }) }
            .sorted { $0.level > $1.level }
    }
}

struct TheBlueView: View {
    @ObservedObject var viewModel: ShellViewModel
    let onOpenChest: (String?) -> Void
    let focusCollectionID: String?
    let focusSpeciesID: String?
    let opensBeyondBlue: Bool
    let focusRequestID: UUID?
    let onFocusConsumed: (UUID, Bool) -> Void

    @State private var visibleZone: ShellDepthTier? = .sunlitReef
    @State private var selectedCreatureID: String?
    @State private var beyondBlueZone: ShellDepthTier?
    @State private var focusedBeyondCreatureID: String?

    init(
        viewModel: ShellViewModel,
        onOpenChest: @escaping (String?) -> Void = { _ in },
        focusCollectionID: String? = nil,
        focusSpeciesID: String? = nil,
        opensBeyondBlue: Bool = false,
        focusRequestID: UUID? = nil,
        onFocusConsumed: @escaping (UUID, Bool) -> Void = { _, _ in }
    ) {
        self.viewModel = viewModel
        self.onOpenChest = onOpenChest
        self.focusCollectionID = focusCollectionID
        self.focusSpeciesID = focusSpeciesID
        self.opensBeyondBlue = opensBeyondBlue
        self.focusRequestID = focusRequestID
        self.onFocusConsumed = onFocusConsumed
    }

    var body: some View {
        ZStack {
            if viewModel.blueRealm == nil {
                BlueRealmSelector(onSelect: viewModel.selectBlueRealm)
            } else if viewModel.blueRealm == .sea, animalGroups.isEmpty {
                TheBlueEmptyOceanView { beyondBlueZone = .sunlitReef }
            } else {
                populatedOcean
            }
        }
        .background(ScyraColors.background.ignoresSafeArea())
        .onAppear { viewModel.refresh() }
        .onChange(of: viewModel.blueRealm) { _, realm in
            if realm == nil {
                selectedCreatureID = nil
                beyondBlueZone = nil
                focusedBeyondCreatureID = nil
            } else if visibleZone?.realm != realm {
                visibleZone = visibleZones.first
            }
        }
        .onChange(of: focusRequestID, initial: true) { _, requestID in
            guard let requestID else { return }
            viewModel.refresh()
            applyFocus(requestID: requestID)
        }
        .sheet(isPresented: creatureSheetPresented) {
            if let group = selectedCreatureGroup {
                TheBlueCreatureDetailSheet(
                    group: group,
                    viewModel: viewModel,
                    onExploreBeyond: { zone in
                        selectedCreatureID = nil
                        Task { @MainActor in
                            await Task.yield()
                            focusedBeyondCreatureID = nil
                            beyondBlueZone = zone
                        }
                    },
                    onOpenChest: { instanceID in
                        selectedCreatureID = nil
                        onOpenChest(instanceID)
                    }
                )
            }
        }
        .sheet(isPresented: beyondBlueSheetPresented) {
            if let zone = beyondBlueZone {
                BeyondBlueCatalogSheet(
                    zone: zone,
                    viewModel: viewModel,
                    initialTargetID: focusedBeyondCreatureID
                ) {
                    beyondBlueZone = nil
                    focusedBeyondCreatureID = nil
                }
            }
        }
        .alert(
            "Creature encountered",
            isPresented: Binding(
                get: { viewModel.lastEncounteredCreature != nil },
                set: { if !$0 { viewModel.dismissEncounterReveal() } }
            )
        ) {
            Button("Done", action: viewModel.dismissEncounterReveal)
        } message: {
            if let creature = viewModel.lastEncounteredCreature {
                Text("\(creature.displayName) joined your Chest. Discovery evidence was recorded.")
            }
        }
        .alert(
            "The Blue",
            isPresented: Binding(
                get: { viewModel.errorMessage != nil },
                set: { if !$0 { viewModel.dismissError() } }
            )
        ) {
            Button("OK", action: viewModel.dismissError)
        } message: {
            Text(viewModel.errorMessage ?? "The current shifted. Please try again.")
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { viewModel.pendingMasteryCelebration != nil },
                set: { if !$0 { viewModel.acknowledgeMasteryCelebration() } }
            )
        ) {
            TheBlueMasteryCelebration(viewModel: viewModel)
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("the-blue-screen")
    }

    private var populatedOcean: some View {
        GeometryReader { proxy in
            ScrollView(.vertical) {
                LazyVStack(spacing: 0) {
                    ForEach(visibleZones, id: \.self) { zone in
                        Group {
                            if zone.realm == .land { landZonePage(zone) }
                            else { zonePage(zone, size: proxy.size) }
                        }
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .id(zone)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollIndicators(.hidden)
            .scrollTargetBehavior(.paging)
            .scrollPosition(id: $visibleZone)
            .overlay(alignment: .trailing) { depthRail }
        }
    }

    private var visibleZones: [ShellDepthTier] {
        BlueRealmPolicy.zones(in: viewModel.blueRealm ?? .sea)
    }

    private func landZonePage(_ zone: ShellDepthTier) -> some View {
        ZStack {
            LandEnvironmentView(zone: zone)
            VStack(alignment: .leading, spacing: 12) {
                zoneHeader(zone)
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 88))], spacing: 14) {
                        ForEach(groups(in: zone)) { group in
                            Button { selectedCreatureID = group.id } label: {
                                VStack(spacing: 5) {
                                    ScyraCanonicalIcon(systemName: group.definition.systemImage, size: 44)
                                        .foregroundStyle(ScyraColors.primary)
                                    Text(group.definition.displayName).font(ScyraTypography.label)
                                    Text("×\(group.totalCount) · Lv \(group.highestLevel)").font(ScyraTypography.caption)
                                }
                                .frame(maxWidth: .infinity, minHeight: 94)
                                .padding(6)
                                .background(ScyraColors.surface.opacity(0.74), in: RoundedRectangle(cornerRadius: 16))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("\(group.definition.displayName). Living here: \(group.totalCount). Level \(group.highestLevel). Opens details.")
                            .accessibilityIdentifier("the-blue-creature-\(group.id)")
                        }
                    }
                }
                creatureTray(zone, groups: groups(in: zone))
            }
            .padding(.leading, 22).padding(.trailing, 76).padding(.top, 20).padding(.bottom, 24)
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("the-blue-zone-\(zone.rawValue)")
    }

    private func zonePage(_ zone: ShellDepthTier, size: CGSize) -> some View {
        let groups = groups(in: zone)
        return ZStack {
            TheBlueOceanScene(zone: zone, creatureCount: groups.reduce(0) { $0 + $1.totalCount })

            VStack(alignment: .leading, spacing: 10) {
                zoneHeader(zone)
                Spacer(minLength: 160)
                creatureTray(zone, groups: groups)
            }
            .padding(.leading, 20)
            .padding(.trailing, 76)
            .padding(.vertical, 20)

            creatureField(groups, size: size)
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("the-blue-zone-\(zone.rawValue)")
    }

    private func zoneHeader(_ zone: ShellDepthTier) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            TheBlueOverlaySurface {
                VStack(alignment: .leading, spacing: 5) {
                    if zone == .sunlitReef {
                        Text("The Blue").font(ScyraTypography.screenTitle)
                        Text("Animals encountered through regular Flows swim here.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                        Text("\(totalAnimalCount) Animals · \(animalGroups.count) Species · Deepest: \(deepestZone.title)")
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.primary)
                            .padding(.bottom, 4)
                    }
                    if zone == .goldenFields {
                        Text("Land").font(ScyraTypography.screenTitle)
                        Text("Life encountered through Arcs and Beyond the Blue.")
                            .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    }
                    if let progress = viewModel.progress(for: collectionID(zone)) {
                        Text("\(progress.discovered) of \(progress.total) discovered · \(progress.mastered) mastered")
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.primary)
                        if progress.completionistEarned {
                            Text("Completionist").font(ScyraTypography.caption)
                        } else if progress.collectorEarned {
                            Text("Collector").font(ScyraTypography.caption)
                        }
                    }
                    Text(zone.title).font(ScyraTypography.screenTitle)
                    Text(zoneSubtitle(zone))
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
            }

            Button {
                focusedBeyondCreatureID = nil
                beyondBlueZone = zone
            } label: {
                TheBlueOverlaySurface {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Encounter Beyond the Blue")
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.primary)
                        Text(zone.realm == .land ? "Discover life in \(zone.title)" : "Discover life near this depth.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("the-blue-beyond-\(zone.rawValue)")
        }
    }

    private func creatureField(_ groups: [TheBlueAnimalGroup], size: CGSize) -> some View {
        ZStack {
            ForEach(Array(groups.prefix(6).enumerated()), id: \.element.id) { index, group in
                let point = creaturePosition(index: index, count: min(groups.count, 6), size: size)
                Button { selectedCreatureID = group.id } label: {
                    VStack(spacing: 3) {
                        ZStack(alignment: .topTrailing) {
                            Circle()
                                .fill(ScyraColors.surface.opacity(0.58))
                                .frame(width: 62, height: 62)
                                .overlay(Circle().stroke(ScyraColors.secondaryGold.opacity(0.3)))
                            ScyraCanonicalIcon(systemName: group.definition.systemImage, size: 36)
                                .foregroundStyle(ScyraColors.primary)
                                .frame(width: 62, height: 62)
                            if group.totalCount > 1 {
                                Text("×\(group.totalCount)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(ScyraColors.onPrimary)
                                    .padding(.horizontal, 5).padding(.vertical, 2)
                                    .background(ScyraColors.primary, in: Capsule())
                            }
                        }
                        Text(group.definition.displayName)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(ScyraColors.textPrimary)
                            .lineLimit(1)
                    }
                    .padding(5)
                    .background(ScyraColors.surface.opacity(0.48), in: RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .position(point)
                .accessibilityLabel("\(group.definition.displayName), \(group.totalCount) swimming here, highest level \(group.highestLevel). Tap for details.")
                .accessibilityIdentifier("the-blue-creature-\(group.id)")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func creatureTray(_ zone: ShellDepthTier, groups: [TheBlueAnimalGroup]) -> some View {
        TheBlueOverlaySurface {
            VStack(alignment: .leading, spacing: 7) {
                Text("\(zone.title) Life").font(ScyraTypography.label)
                if groups.isEmpty {
                    Text(zone.realm == .land ? "Quiet land is waiting here." : "Quiet water is waiting here.")
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(groups) { group in
                                Button { selectedCreatureID = group.id } label: {
                                    VStack(spacing: 3) {
                                        ScyraCanonicalIcon(systemName: group.definition.systemImage, size: 25)
                                            .foregroundStyle(ScyraColors.primary)
                                        Text(group.definition.displayName)
                                            .font(.system(size: 10, weight: .semibold))
                                            .lineLimit(1)
                                        Text("×\(group.totalCount) · Lv \(group.highestLevel)")
                                            .font(.system(size: 9))
                                            .foregroundStyle(ScyraColors.textSecondary)
                                    }
                                    .frame(width: 76, height: 66)
                                    .background(ScyraColors.elevatedSurface.opacity(0.88), in: RoundedRectangle(cornerRadius: 13))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
    }

    private var depthRail: some View {
        VStack(spacing: 12) {
            ForEach(visibleZones, id: \.self) { zone in
                let isActive = visibleZone == zone
                Button {
                    withAnimation(.easeInOut(duration: 0.35)) { visibleZone = zone }
                } label: {
                    VStack(spacing: 3) {
                        Circle()
                            .fill(isActive ? ScyraColors.secondaryGold : ScyraColors.surface.opacity(0.78))
                            .frame(width: isActive ? 15 : 11, height: isActive ? 15 : 11)
                            .overlay(Circle().stroke(ScyraColors.primary.opacity(0.35)))
                        Text(railTitle(zone))
                            .font(.system(size: 8, weight: isActive ? .bold : .medium))
                            .foregroundStyle(ScyraColors.textPrimary)
                    }
                    .frame(width: 58)
                    .frame(minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Go to \(zone.title)")
            }
        }
        .padding(.vertical, 12)
        .background(ScyraColors.surface.opacity(0.68), in: Capsule())
        .padding(.trailing, 8)
        .accessibilityIdentifier("the-blue-depth-rail")
    }

    private var animalGroups: [TheBlueAnimalGroup] {
        let displayed = Set(viewModel.placements.map(\.instanceID))
        return Dictionary(
            grouping: viewModel.instances.filter { CreatureCatalog.definition($0.findID)?.realm == viewModel.blueRealm },
            by: \.findID
        )
        .compactMap { id, instances in
            CreatureCatalog.definition(id).map {
                TheBlueAnimalGroup(definition: $0, instances: instances, displayedInstanceIDs: displayed)
            }
        }
        .sorted {
            let leftDepth = visibleZones.firstIndex(of: $0.definition.zone) ?? 0
            let rightDepth = visibleZones.firstIndex(of: $1.definition.zone) ?? 0
            if leftDepth != rightDepth { return leftDepth < rightDepth }
            return $0.definition.id < $1.definition.id
        }
    }

    private var totalAnimalCount: Int { animalGroups.reduce(0) { $0 + $1.totalCount } }
    private func groups(in zone: ShellDepthTier) -> [TheBlueAnimalGroup] { animalGroups.filter { $0.definition.zone == zone } }
    private var selectedCreatureGroup: TheBlueAnimalGroup? {
        selectedCreatureID.flatMap { id in animalGroups.first { $0.id == id } }
    }
    private var deepestZone: ShellDepthTier {
        let index = animalGroups.compactMap { ShellDepthTier.seaZones.firstIndex(of: $0.definition.zone) }.max() ?? 0
        return ShellDepthTier.seaZones[index]
    }
    private var creatureSheetPresented: Binding<Bool> {
        Binding(get: { selectedCreatureID != nil }, set: { if !$0 { selectedCreatureID = nil } })
    }
    private var beyondBlueSheetPresented: Binding<Bool> {
        Binding(get: { beyondBlueZone != nil }, set: { if !$0 { beyondBlueZone = nil } })
    }
    private func collectionID(_ zone: ShellDepthTier) -> String {
        BlueRealmPolicy.collectionID(for: zone)
    }
    private func applyFocus(requestID: UUID) {
        guard focusCollectionID != nil || focusSpeciesID != nil else { return }
        let zone = focusSpeciesID.flatMap { CreatureCatalog.definition($0)?.zone }
            ?? focusCollectionID.flatMap(zone(forCollectionID:))
        if let zone {
            viewModel.selectBlueRealm(zone.realm)
            visibleZone = zone
        }
        var didFocusSpecies = focusSpeciesID == nil
        if opensBeyondBlue, let zone, let focusSpeciesID,
           CreatureCatalog.definition(focusSpeciesID)?.sourceType == .beyondBlue {
            focusedBeyondCreatureID = focusSpeciesID
            beyondBlueZone = zone
            didFocusSpecies = true
        } else if let focusSpeciesID,
                  animalGroups.contains(where: { $0.id == focusSpeciesID }) {
            selectedCreatureID = focusSpeciesID
            didFocusSpecies = true
        }
        onFocusConsumed(requestID, zone != nil && didFocusSpecies)
    }
    private func zone(forCollectionID collectionID: String) -> ShellDepthTier? {
        BlueRealmPolicy.zone(collectionID: collectionID)
    }
    private func zoneSubtitle(_ zone: ShellDepthTier) -> String {
        BlueRealmPolicy.subtitle(for: zone)
    }
    private func railTitle(_ zone: ShellDepthTier) -> String {
        BlueRealmPolicy.railTitle(for: zone)
    }
    private func creaturePosition(index: Int, count: Int, size: CGSize) -> CGPoint {
        let columns = max(1, min(3, count))
        let column = index % columns
        let row = index / columns
        return CGPoint(x: size.width * (0.20 + CGFloat(column) * 0.22), y: size.height * (0.50 + CGFloat(row) * 0.12))
    }
}

private struct TheBlueCreatureDetailSheet: View {
    let group: TheBlueAnimalGroup
    @ObservedObject var viewModel: ShellViewModel
    let onExploreBeyond: (ShellDepthTier) -> Void
    let onOpenChest: (String?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showGrowthConfirmation = false
    @State private var showRelease = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    ScyraCard(style: .elevated) {
                        HStack(spacing: ScyraSpacing.md) {
                            ScyraCanonicalIcon(systemName: group.definition.systemImage, size: 54)
                                .foregroundStyle(ScyraColors.primary)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(group.definition.displayName) ×\(group.totalCount)").font(ScyraTypography.screenTitle)
                                Text(group.definition.zone.title)
                                    .font(ScyraTypography.label).foregroundStyle(ScyraColors.primary)
                            }
                        }
                        Text(sourceDescription)
                            .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ScyraStatPill(label: group.definition.realm == .land ? "Living here" : "Swimming", value: "\(group.totalCount)")
                            if let lifetime = viewModel.creatureLifetimeCounts[group.id] {
                                ScyraStatPill(label: "Lifetime", value: "\(lifetime.encountered)")
                                if lifetime.released > 0 { ScyraStatPill(label: "Released", value: "\(lifetime.released)") }
                                if lifetime.traded > 0 { ScyraStatPill(label: "Beyond Blue", value: "\(lifetime.traded)") }
                            }
                            ScyraStatPill(label: "Highest", value: "Lv \(group.highestLevel)")
                            ScyraStatPill(label: "In Focus", value: "\(group.displayedCount)")
                            ScyraStatPill(label: "In Chest", value: "\(group.restingCount)")
                        }
                    }

                    if let minutes = group.definition.flowTimeValueMinutes ?? group.definition.requirementMinutes {
                        ScyraCard {
                            ScyraCanonicalLabel(group.definition.realm == .land ? "Effort value" : "Created by Flow", systemImage: "map")
                                .font(ScyraTypography.cardTitle)
                            Text("\(minutes) time-minutes each")
                                .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Levels").font(ScyraTypography.cardTitle)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(group.levelGroups, id: \.level) { levelGroup in
                                    ScyraStatPill(label: "Level \(levelGroup.level)", value: "×\(levelGroup.instances.count)")
                                }
                            }
                        }
                        Text("Pearls help this creature grow larger in The Blue.")
                            .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Actions").font(ScyraTypography.cardTitle)
                        if isMastered {
                            ScyraCanonicalLabel("Mastered at Level 99", systemImage: "medal")
                                .font(ScyraTypography.cardTitle).foregroundStyle(ScyraColors.secondaryGold)
                        } else {
                            ScyraButton(
                                canGrow ? "Level Up" : "View requirements",
                                systemImage: "arrow.up.circle",
                                variant: canGrow ? .primary : .secondary,
                                isDisabled: growthInstance == nil
                            ) { showGrowthConfirmation = true }
                            Text(growthRequirementText)
                                .font(ScyraTypography.caption)
                                .foregroundStyle(canGrow ? ScyraColors.textSecondary : ScyraColors.textMuted)
                        }

                        ScyraButton("Encounter Beyond the Blue", systemImage: "water.waves", variant: .secondary) {
                            onExploreBeyond(group.definition.zone)
                        }

                        ScyraButton(
                            "Display one in Focus",
                            systemImage: "scope",
                            isDisabled: restingInstance == nil || focusSlot == nil
                        ) {
                            guard let instance = restingInstance, let slot = focusSlot else { return }
                            viewModel.place(instanceID: instance.id, slotID: slot.id)
                            dismiss()
                        }
                        if focusSlot == nil {
                            Text("No open Focus nook right now. Return something from Focus to display this creature.")
                                .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                        } else if restingInstance == nil {
                            Text("No owned creature is available in The Chest.")
                                .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                        }

                        ScyraButton("View in The Chest", systemImage: "shippingbox", variant: .secondary) {
                            onOpenChest(restingInstance?.id ?? group.instances.first?.id)
                        }
                        ScyraButton("Release", systemImage: "water.waves", variant: .destructive) {
                            showRelease = true
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
        .alert("Level up \(group.definition.displayName)?", isPresented: $showGrowthConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Level Up") {
                guard let instance = growthInstance else { return }
                viewModel.growCreature(instanceID: instance.id)
                dismiss()
            }
            .disabled(!canGrow)
        } message: {
            Text("This will level up one Level \(group.highestLevel) \(group.definition.displayName). Cost: \(growthCost) Pearls. Your balance: \(viewModel.pearlBalance) Pearls.")
        }
        .sheet(isPresented: $showRelease) {
            TheBlueReleaseSheet(group: group, viewModel: viewModel) {
                showRelease = false
                dismiss()
            }
        }
    }

    private var growthInstance: ShellFindInstance? {
        group.instances.sorted {
            if $0.animalLevel != $1.animalLevel { return $0.animalLevel > $1.animalLevel }
            return $0.acquiredAt < $1.acquiredAt
        }.first
    }
    private var growthCost: Int { CreatureEconomy.growthCostPearls(group.id, currentLevel: group.highestLevel) }
    private var isMastered: Bool { group.highestLevel >= CreatureEconomy.maxLevel }
    private var canGrow: Bool { growthInstance != nil && !isMastered && viewModel.pearlBalance >= growthCost }
    private var restingInstance: ShellFindInstance? { viewModel.firstRestingCreatureInstance(for: group.id) }
    private var focusSlot: ShellSlotDefinition? { viewModel.firstOpenFocusSlot(for: group.id) }
    private var growthRequirementText: String {
        if isMastered { return "Mastered at Level 99" }
        if growthInstance == nil { return "No active creature available to grow." }
        if canGrow { return "Level up cost: \(growthCost) Pearls" }
        return "Level up requires \(growthCost) Pearls. You need \(max(0, growthCost - viewModel.pearlBalance)) more."
    }
    private var sourceDescription: String {
        switch group.definition.sourceType {
        case .arcEarned: return "Earned through Arcs · Flows: \(group.definition.arcFlowRequirement ?? 0)"
        case .restorativeLand: return "Drawn from a quiet habitat."
        case .flowEarned:
            return "Encountered through regular Flows lasting \(group.definition.flowTimeValueMinutes ?? 0) minutes or more."
        case .beyondBlue:
            return group.definition.realm == .land
                ? "Encountered beyond The Blue and now living in \(group.definition.zone.title)."
                : "Encountered beyond The Blue and now swimming in the \(group.definition.zone.title)."
        case .stillwater:
            return "Stillwater exclusive. Drawn from a quiet vessel."
        }
    }
}

private struct TheBlueReleaseSheet: View {
    let group: TheBlueAnimalGroup
    @ObservedObject var viewModel: ShellViewModel
    let onReleased: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedCounts: [Int: Int] = [:]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    Text("Release \(group.definition.displayName)?").font(ScyraTypography.screenTitle)
                    Text("Choose which copies to release back into The Blue. Discovery, mastery, and lifetime collection evidence remain.")
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)

                    HStack {
                        ScyraStatPill(label: "Selected", value: "\(selectedInstanceIDs.count) of \(group.totalCount)")
                        ScyraStatPill(label: "You receive", value: "+\(totalReward) Pearls")
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            Button("Lowest level") { selectLowestOne() }
                            Button("All lowest") { selectAllLowest() }
                            Button("Keep highest") { keepHighest() }
                            Button("Clear") { selectedCounts = [:] }
                        }
                        .buttonStyle(.bordered)
                    }

                    ForEach(group.levelGroups, id: \.level) { levelGroup in
                        let selected = min(levelGroup.instances.count, selectedCounts[levelGroup.level, default: 0])
                        ScyraCard {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("Level \(levelGroup.level)").font(ScyraTypography.cardTitle)
                                    Text("Owned: \(levelGroup.instances.count) · +\(CreatureEconomy.releaseValuePearls(group.id, level: levelGroup.level)) Pearls each")
                                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                                }
                                Spacer()
                                Stepper(
                                    "\(selected)",
                                    value: Binding(
                                        get: { min(levelGroup.instances.count, selectedCounts[levelGroup.level, default: 0]) },
                                        set: { selectedCounts[levelGroup.level] = min(levelGroup.instances.count, max(0, $0)) }
                                    ),
                                    in: 0...levelGroup.instances.count
                                )
                                .labelsHidden()
                                Text("\(selected)").font(ScyraTypography.cardTitle).monospacedDigit()
                            }
                        }
                    }

                    HStack {
                        ScyraButton("Keep swimming", variant: .secondary) { dismiss() }
                        ScyraButton("Release", variant: .destructive, isDisabled: selectedInstanceIDs.isEmpty) {
                            if viewModel.releaseCreatures(instanceIDs: selectedInstanceIDs) { onReleased() }
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }

    private var selectedInstanceIDs: [String] {
        group.levelGroups.flatMap { levelGroup in
            Array(levelGroup.instances.prefix(selectedCounts[levelGroup.level, default: 0])).map(\.id)
        }
    }
    private var totalReward: Int {
        group.levelGroups.reduce(0) { partial, levelGroup in
            partial + min(levelGroup.instances.count, selectedCounts[levelGroup.level, default: 0])
                * CreatureEconomy.releaseValuePearls(group.id, level: levelGroup.level)
        }
    }
    private func selectLowestOne() {
        guard let level = group.levelGroups.last?.level else { return }
        selectedCounts = [level: 1]
    }
    private func selectAllLowest() {
        guard let lowest = group.levelGroups.last else { return }
        selectedCounts = [lowest.level: lowest.instances.count]
    }
    private func keepHighest() {
        guard let highest = group.levelGroups.first?.level else { return }
        selectedCounts = Dictionary(uniqueKeysWithValues: group.levelGroups.map {
            ($0.level, $0.level == highest ? 0 : $0.instances.count)
        })
    }
}

private struct BeyondBlueCatalogSheet: View {
    @State private var zone: ShellDepthTier
    @ObservedObject var viewModel: ShellViewModel
    let onDismiss: () -> Void

    @State private var targetID: String?
    @State private var selectedCounts: [String: Int] = [:]
    @State private var showConfirmation = false

    init(
        zone: ShellDepthTier,
        viewModel: ShellViewModel,
        initialTargetID: String? = nil,
        onDismiss: @escaping () -> Void
    ) {
        _zone = State(initialValue: zone)
        self.viewModel = viewModel
        self.onDismiss = onDismiss
        _targetID = State(initialValue: initialTargetID)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    Text("Beyond Blue").font(ScyraTypography.screenTitle)
                    Text("\(zone.title) encounters").font(ScyraTypography.cardTitle)
                    Text(BlueRealmPolicy.subtitle(for: zone))
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(BlueRealmPolicy.zones(in: zone.realm), id: \.self) { item in
                                Button(BlueRealmPolicy.railTitle(for: item)) {
                                    zone = item
                                    targetID = nil
                                    selectedCounts = [:]
                                }
                                .buttonStyle(.bordered)
                                .tint(item == zone ? ScyraColors.primary : ScyraColors.textSecondary)
                                .accessibilityIdentifier("beyond-blue-zone-\(item.rawValue)")
                            }
                        }
                    }
                    if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage).font(ScyraTypography.body).foregroundStyle(ScyraColors.error)
                    }
                    if let target { targetDetail(target) }
                    else {
                        ForEach(targets) { creature in
                            if creature.sourceType == .arcEarned { arcFlagshipCard(creature) }
                            else { targetCard(creature) }
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(target == nil ? "Close" : "Back") {
                        if target == nil { onDismiss() }
                        else { targetID = nil; selectedCounts = [:] }
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .alert("Encounter \(target?.displayName ?? "creature")?", isPresented: $showConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button(selectedTradeIDs.isEmpty ? "Buy" : "Trade & Buy") {
                guard let target else { return }
                if viewModel.encounterBeyondBlue(targetCreatureID: target.id, selectedInstanceIDs: Array(selectedTradeIDs)) {
                    onDismiss()
                }
            }
        } message: {
            if let quote { Text(encounterConfirmationText(quote)) }
        }
        .accessibilityIdentifier("beyond-blue-catalog")
    }

    private var targets: [CreatureDefinition] {
        BlueRealmPolicy.encounters(in: zone)
    }
    private var target: CreatureDefinition? {
        guard let targetID else { return nil }
        guard let creature = CreatureCatalog.definition(targetID),
              creature.sourceType == .beyondBlue,
              creature.zone == zone else { return nil }
        return creature
    }
    private var quote: CreaturePaymentQuote? {
        guard let target else { return nil }
        return try? viewModel.quoteBeyondBlue(targetCreatureID: target.id, selectedInstanceIDs: Array(selectedTradeIDs))
    }
    private var tradeStacks: [BeyondBlueTradeStack] { BeyondBlueTradeStack.make(from: viewModel.instances) }
    private var selectedTradeIDs: [String] {
        BeyondBlueTradeStack.selectedIDs(stacks: tradeStacks, counts: selectedCounts)
    }

    private func arcFlagshipCard(_ creature: CreatureDefinition) -> some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: 8) {
                ScyraCanonicalIcon(systemName: creature.systemImage, size: 46)
                Text(creature.displayName).font(ScyraTypography.cardTitle)
                Text("Earned through Arcs · Flows: \(creature.arcFlowRequirement ?? 0)")
                    .font(ScyraTypography.body)
                Text("Not for sale").font(ScyraTypography.label)
            }
        }
        .accessibilityIdentifier("beyond-blue-arc-\(creature.id)")
    }

    private func targetCard(_ creature: CreatureDefinition) -> some View {
        let price = creature.pearlPrice ?? 0
        return Button {
            targetID = creature.id
            selectedCounts = [:]
        } label: {
            ScyraCard(style: .elevated) {
                HStack(spacing: ScyraSpacing.md) {
                    ScyraCanonicalIcon(systemName: creature.systemImage, size: 40)
                        .foregroundStyle(ScyraColors.primary)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(creature.displayName).font(ScyraTypography.cardTitle)
                        Text("\(zone.realm == .land ? "Effort value" : "Requires"): \(creature.requirementMinutes ?? 0) minutes · or \(price) Pearls")
                            .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                        Text(viewModel.pearlBalance >= price ? "Ready to buy" : "Need \(price - viewModel.pearlBalance) more Pearls")
                            .font(ScyraTypography.label)
                            .foregroundStyle(viewModel.pearlBalance >= price ? ScyraColors.primary : ScyraColors.textMuted)
                    }
                    Spacer()
                    ScyraCanonicalIcon(systemName: "arrow.right.circle", size: 18)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("beyond-blue-target-\(creature.id)")
    }

    private func targetDetail(_ target: CreatureDefinition) -> some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
            ScyraCard(style: .elevated) {
                HStack(spacing: ScyraSpacing.md) {
                    ScyraCanonicalIcon(systemName: target.systemImage, size: 48)
                        .foregroundStyle(ScyraColors.primary)
                    VStack(alignment: .leading) {
                        Text(target.displayName).font(ScyraTypography.screenTitle)
                        Text(zone.realm == .land ? "Life waiting beyond this landscape." : "Life waiting beyond this depth.")
                            .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    }
                }
                HStack {
                    ScyraStatPill(label: zone.realm == .land ? "Effort value" : "Creature value", value: "\(target.requirementMinutes ?? 0)m")
                    ScyraStatPill(label: "Pearls only", value: "\(target.pearlPrice ?? 0)")
                }
            }

            if let quote {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Payment").font(ScyraTypography.cardTitle)
                    HStack {
                        ScyraStatPill(label: "Creatures", value: "\(quote.selectedCreatureMinutes)m")
                        ScyraStatPill(label: "Pearls", value: "\(quote.pearlCostForRemaining)")
                        if quote.pearlReturnForOverpay > 0 {
                            ScyraStatPill(label: "Returned", value: "\(quote.pearlReturnForOverpay)")
                        }
                    }
                    ProgressView(
                        value: Double(min(quote.targetRequirementMinutes, quote.selectedCreatureMinutes)),
                        total: Double(max(1, quote.targetRequirementMinutes))
                    )
                    .tint(ScyraColors.primary)
                }
            }

            Text("Trade creatures to reduce Pearl cost").font(ScyraTypography.cardTitle)
            if tradeStacks.isEmpty {
                Text("No active creatures are available to trade yet. You can still encounter this creature with Pearls.")
                    .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
            } else {
                ForEach(tradeStacks) { stack in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(stack.creature.displayName).font(ScyraTypography.label)
                        Text("Level \(stack.level) · \(stack.instances.count) owned · \(stack.valueMinutes) time-minutes each")
                            .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                        Stepper(value: Binding(
                            get: { min(selectedCounts[stack.id] ?? 0, stack.instances.count) },
                            set: { selectedCounts[stack.id] = $0 }
                        ), in: 0...stack.instances.count) {
                            Text("Selected: \(min(selectedCounts[stack.id] ?? 0, stack.instances.count))")
                        }
                        .accessibilityIdentifier("beyond-trade-\(stack.id)")
                    }
                    .padding(ScyraSpacing.md)
                    .background(ScyraColors.elevatedSurface, in: RoundedRectangle(cornerRadius: ScyraRadius.card))
                }
            }

            ScyraButton(
                selectedTradeIDs.isEmpty ? "Encounter with Pearls" : "Trade & Buy",
                systemImage: "water.waves",
                isDisabled: quote?.canEncounter != true
            ) { showConfirmation = true }
            if let quote, !quote.canEncounter {
                Text("Need \(max(0, quote.pearlCostForRemaining - viewModel.pearlBalance)) more Pearls, or more creature value.")
                    .font(ScyraTypography.caption).foregroundStyle(ScyraColors.error)
            }
        }
    }

    private func encounterConfirmationText(_ quote: CreaturePaymentQuote) -> String {
        var parts: [String] = []
        if selectedTradeIDs.isEmpty { parts.append("No creatures will leave The Blue.") }
        else { parts.append("\(selectedTradeIDs.count) selected creature(s) will leave The Blue. Your lifetime record will remain.") }
        if quote.pearlCostForRemaining > 0 { parts.append("\(quote.pearlCostForRemaining) Pearls will cover the remaining value.") }
        if quote.pearlReturnForOverpay > 0 { parts.append("Extra creature value will return as \(quote.pearlReturnForOverpay) Pearls.") }
        return parts.joined(separator: " ")
    }
}

struct TheBlueOverlaySurface<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) { self.content = content() }

    var body: some View {
        content
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(ScyraColors.surface.opacity(0.82), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(ScyraColors.primary.opacity(0.14)))
    }
}

private struct TheBlueOceanScene: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let zone: ShellDepthTier
    let creatureCount: Int

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0)) { timeline in
            let phase = reduceMotion ? 0 : timeline.date.timeIntervalSinceReferenceDate
            Canvas { context, size in
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .linearGradient(Gradient(colors: oceanColors), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height))
                )
                for index in 0..<3 {
                    let sway = CGFloat(sin(phase * 0.16 + Double(index))) * size.width * 0.04
                    var ray = Path()
                    ray.move(to: CGPoint(x: size.width * (0.08 + CGFloat(index) * 0.22) + sway, y: 0))
                    ray.addLine(to: CGPoint(x: size.width * (0.22 + CGFloat(index) * 0.2) + sway, y: size.height))
                    ray.addLine(to: CGPoint(x: size.width * (0.33 + CGFloat(index) * 0.2) + sway, y: size.height))
                    ray.addLine(to: CGPoint(x: size.width * (0.17 + CGFloat(index) * 0.22) + sway, y: 0))
                    ray.closeSubpath()
                    context.fill(ray, with: .color(Color.white.opacity(0.035)))
                }
                for index in 0..<(10 + min(creatureCount, 10)) {
                    let x = (CGFloat(index * 71) + CGFloat(phase * 8)).truncatingRemainder(dividingBy: size.width + 40) - 20
                    let y = size.height - (CGFloat(index * 47) + CGFloat(phase * 15)).truncatingRemainder(dividingBy: size.height)
                    let radius = CGFloat(2 + index % 4)
                    context.stroke(
                        Path(ellipseIn: CGRect(x: x, y: y, width: radius * 2, height: radius * 2)),
                        with: .color(Color.white.opacity(0.16))
                    )
                }
                if zone == .sunlitReef || zone == .deeperReef {
                    context.fill(
                        Path(ellipseIn: CGRect(x: -30, y: size.height * 0.86, width: size.width + 60, height: size.height * 0.25)),
                        with: .color(ScyraColors.secondaryGold.opacity(0.10))
                    )
                }
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }

    private var oceanColors: [Color] {
        switch zone {
        case .sunlitReef: [ScyraColors.primary.opacity(0.42), Color.cyan.opacity(0.18), ScyraColors.background]
        case .deeperReef: [ScyraColors.primary.opacity(0.34), Color.blue.opacity(0.20), ScyraColors.backgroundBottom]
        case .openBlue: [Color.blue.opacity(0.34), ScyraColors.primary.opacity(0.20), ScyraColors.backgroundBottom]
        case .greatBlue: [Color.indigo.opacity(0.30), Color.black.opacity(0.18), ScyraColors.backgroundBottom]
        default: [ScyraColors.background, ScyraColors.backgroundBottom]
        }
    }
}

private struct TheBlueMasteryCelebration: View {
    @ObservedObject var viewModel: ShellViewModel

    var body: some View {
        ZStack {
            LinearGradient(colors: [ScyraColors.primary, ScyraColors.backgroundBottom], startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()
            if let celebration = viewModel.pendingMasteryCelebration,
               let creature = CreatureCatalog.definition(celebration.speciesID) {
                VStack(spacing: ScyraSpacing.xl) {
                    ScyraCanonicalIcon(systemName: creature.systemImage, size: 92)
                        .foregroundStyle(.white)
                        .symbolEffect(.bounce, value: celebration.id)
                    Text("Level 99").font(ScyraTypography.screenTitle).foregroundStyle(.white)
                    Text("\(creature.displayName) Mastered")
                        .font(ScyraTypography.screenTitle).foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                    Text("Mastery evidence and collection progress have been recorded permanently.")
                        .font(ScyraTypography.body).foregroundStyle(.white.opacity(0.84))
                        .multilineTextAlignment(.center)
                    ScyraButton("Continue", systemImage: "medal", variant: .secondary) {
                        viewModel.acknowledgeMasteryCelebration()
                    }
                }
                .padding(ScyraSpacing.xl)
            }
        }
    }
}

private struct TheBlueEmptyOceanView: View {
    let onEncounter: () -> Void
    var body: some View {
        ZStack(alignment: .topLeading) {
            TheBlueOceanScene(zone: .sunlitReef, creatureCount: 0)
            VStack(alignment: .leading, spacing: 8) {
                Text("The Blue").font(ScyraTypography.screenTitle)
                Text("No animals yet.").font(ScyraTypography.cardTitle)
                Text("Complete a regular Flow lasting 10 minutes or more to encounter your first Minnow.")
                    .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                Text("Quiet water waits for the first regular Flow Animal.")
                    .font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary)
            }
            .padding(16)
            .background(ScyraColors.surface.opacity(0.82), in: RoundedRectangle(cornerRadius: 22))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(ScyraColors.primary.opacity(0.14)))
            .padding(24)
            .frame(maxWidth: 380, alignment: .leading)
        }
        .overlay(alignment: .bottom) {
            ScyraButton("Encounter Beyond the Blue", action: onEncounter)
                .padding(24)
                .accessibilityIdentifier("the-blue-beyond-sunlitReef")
        }
    }
}

#Preview {
    TheBlueView(viewModel: ShellViewModel(repository: InMemoryFlowRepository()))
}
