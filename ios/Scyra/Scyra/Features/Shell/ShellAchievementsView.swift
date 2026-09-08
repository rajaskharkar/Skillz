import SwiftUI

private enum BadgeRoomTab: CaseIterable {
    case showcase
    case badgeBook
    case withinReach
    case progress

    var title: String {
        switch self {
        case .showcase: "Showcase"
        case .badgeBook: "Badge Book"
        case .withinReach: "Within Reach"
        case .progress: "Progress"
        }
    }
}

private struct CollectionDetailSelection: Identifiable, Equatable {
    var id: String { "\(collectionID):\(focusSpeciesID ?? "all")" }
    let collectionID: String
    let focusSpeciesID: String?
}

struct ShellAchievementsView: View {
    @ObservedObject var viewModel: ShellViewModel
    let focusBadgeID: String?
    let focusCollectionID: String?
    let focusSpeciesID: String?
    let onNavigate: (AchievementActionDestination) -> Void
    let onFocusConsumed: (Bool) -> Void
    @State private var selectedBadge: AchievementProgress?
    @State private var selectedCollection: CollectionDetailSelection?
    @State private var selectedTab: BadgeRoomTab = .showcase
    @State private var searchQuery = ""

    init(
        viewModel: ShellViewModel,
        focusBadgeID: String? = nil,
        focusCollectionID: String? = nil,
        focusSpeciesID: String? = nil,
        onNavigate: @escaping (AchievementActionDestination) -> Void = { _ in },
        onFocusConsumed: @escaping (Bool) -> Void = { _ in }
    ) {
        self.viewModel = viewModel
        self.focusBadgeID = focusBadgeID
        self.focusCollectionID = focusCollectionID
        self.focusSpeciesID = focusSpeciesID
        self.onNavigate = onNavigate
        self.onFocusConsumed = onFocusConsumed
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                header
                Text(summary)
                    .font(.system(size: 16, weight: .medium))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            tabRow

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 14) {
                    switch selectedTab {
                    case .showcase:
                        showcase
                        trackedBadges
                    case .badgeBook:
                        badgeBook
                    case .withinReach:
                        withinReach
                    case .progress:
                        progressPage
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
            }
        }
        .background(
            LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        )
        .sheet(item: $selectedBadge) { badge in badgeDetails(badge) }
        .sheet(item: $selectedCollection) { selection in collectionDetails(selection) }
        .alert("Replace a pinned badge?", isPresented: Binding(
            get: { viewModel.pinReplacement != nil },
            set: { if !$0 { viewModel.dismissPinReplacement() } }
        )) {
            if let replacement = viewModel.pinReplacement {
                ForEach(replacement.current, id: \.self) { badgeID in
                    Button(AchievementCatalog.resolve(badgeID).title) {
                        viewModel.pin(replacement.requested, replacing: badgeID)
                    }
                }
            }
            Button("Cancel", role: .cancel) { viewModel.dismissPinReplacement() }
        } message: {
            Text("The Showcase holds three badges. Choose one to replace.")
        }
        .onAppear {
            viewModel.refresh()
            var didConsumeFocus = false
            if let focusBadgeID {
                selectedTab = .badgeBook
                selectedBadge = viewModel.achievementDashboard.badges.first { $0.badgeID == focusBadgeID }
                if selectedBadge != nil {
                    viewModel.markBadgeViewed(focusBadgeID)
                    didConsumeFocus = true
                }
            } else if let focusCollectionID, viewModel.progress(for: focusCollectionID) != nil {
                selectedTab = .progress
                selectedCollection = .init(collectionID: focusCollectionID, focusSpeciesID: focusSpeciesID)
                didConsumeFocus = true
            }
            if focusBadgeID != nil || focusCollectionID != nil {
                onFocusConsumed(didConsumeFocus)
            }
        }
        .accessibilityIdentifier("shell-achievements-screen")
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Badges")
                .font(.system(size: 20, weight: .bold))
            Text("Badges are records of what happened.")
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

    private var summary: String {
        let earned = viewModel.achievementDashboard.badges.filter(\.earned).count
        let masteries = viewModel.creatureMasteries.count
        let completedCollections = viewModel.allCollectionProgress
            .filter(\.completionistEarned).count
        return "\(earned) earned · \(masteries) Masteries · \(completedCollections) Completionist collection\(completedCollections == 1 ? "" : "s")"
    }

    private var tabRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(BadgeRoomTab.allCases, id: \.self) { tab in
                    ScyraChip(tab.title, isSelected: selectedTab == tab) {
                        withAnimation(.easeInOut(duration: 0.18)) { selectedTab = tab }
                    }
                    .accessibilityLabel("\(tab.title) tab")
                    .accessibilityValue(selectedTab == tab ? "Selected" : "Not selected")
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.bottom, 12)
        .accessibilityIdentifier("badge-room-tabs")
    }

    @ViewBuilder private var showcase: some View {
        ScyraSectionHeader(title: "Your Showcase", subtitle: "Pin the achievements that mean the most to you.")
        HStack(alignment: .top, spacing: 8) {
            ForEach(0..<3, id: \.self) { index in
                if viewModel.achievementDashboard.pinned.indices.contains(index) {
                    let badge = viewModel.achievementDashboard.pinned[index]
                    Button { open(badge) } label: {
                        VStack(spacing: 8) {
                            badgeMedallion(badge, diameter: 64)
                            Text(badge.title)
                                .font(ScyraTypography.label)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                        }
                        .padding(8)
                        .frame(maxWidth: .infinity, minHeight: 120)
                    }
                    .buttonStyle(.plain)
                } else {
                    Text("Empty slot")
                        .font(ScyraTypography.label)
                        .foregroundStyle(ScyraColors.textSecondary)
                        .frame(maxWidth: .infinity, minHeight: 120)
                }
                
            }
        }
        .background(ScyraColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(ScyraColors.outlineVariant, lineWidth: 1))
    }

    @ViewBuilder private var trackedBadges: some View {
        ScyraSectionHeader(
            title: "Tracked Badges",
            subtitle: "Badges you have chosen to follow. Scyra records all badge progress automatically."
        )
        let tracked = viewModel.achievementDashboard.badges.filter { viewModel.trackedBadgeIDs.contains($0.badgeID) }
        if tracked.isEmpty {
            ScyraCard(style: .plain) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("No tracked badges").font(ScyraTypography.cardTitle)
                    Text("Track a badge to keep its progress and next step here.")
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                }
            }
        } else {
            ForEach(tracked) { badgeRow($0) }
        }
    }

    private var controls: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
            HStack(spacing: 10) {
                ScyraCanonicalIcon(systemName: "magnifyingglass", size: 20)
                    .foregroundStyle(ScyraColors.textSecondary)
                TextField("Search badges", text: $searchQuery)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                if !searchQuery.isEmpty {
                    Button { searchQuery = "" } label: {
                        ScyraCanonicalIcon(systemName: "xmark", size: 20)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Clear search")
                }
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 56)
            .background(ScyraColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 4).stroke(ScyraColors.outline, lineWidth: 1))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(AchievementCategory.allCases, id: \.self) { category in
                        ScyraChip(categoryTitle(category), isSelected: viewModel.achievementCategory == category) {
                            viewModel.achievementCategory = category
                        }
                    }
                }
            }
            Menu {
                ForEach(AchievementSort.allCases, id: \.self) { option in
                    Button(sortTitle(option)) { viewModel.achievementSort = option }
                }
            } label: {
                Text("Sort: \(sortTitle(viewModel.achievementSort))")
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textPrimary)
                    .padding(.horizontal, 12)
                    .frame(minHeight: 32)
                    .background(ScyraColors.surface, in: Capsule())
                    .overlay(Capsule().stroke(ScyraColors.outline, lineWidth: 1))
            }
        }
    }

    @ViewBuilder private var badgeBook: some View {
        ScyraSectionHeader(title: "Badge Book", subtitle: "Earned and locked progression goals.")
        controls
        if visibleBadgeBook.isEmpty {
            ScyraCard(style: .plain) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(searchQuery.isEmpty ? "No badges are available in this category." : "No badges match “\(searchQuery)”.")
                    Button("Reset filters") {
                        searchQuery = ""
                        viewModel.achievementCategory = .all
                    }
                    .foregroundStyle(ScyraColors.primary)
                }
            }
        } else {
            ForEach(visibleBadgeBook) { badgeRow($0) }
        }
    }

    private var visibleBadgeBook: [AchievementProgress] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return viewModel.visibleAchievements }
        return viewModel.visibleAchievements.filter {
            $0.title.localizedCaseInsensitiveContains(query) ||
                $0.description.localizedCaseInsensitiveContains(query) ||
                categoryTitle($0.category).localizedCaseInsensitiveContains(query)
        }
    }

    @ViewBuilder private var withinReach: some View {
        ScyraSectionHeader(
            title: "Within Reach",
            subtitle: "Up to three untracked goals Scyra recommends as useful next steps."
        )
        let recommendations = AchievementDashboardCalculator.sorted(
            viewModel.achievementDashboard.badges.filter {
                !$0.earned && !viewModel.trackedBadgeIDs.contains($0.badgeID) && $0.milestone.nextThreshold != nil
            },
            by: .recommended
        )
        .prefix(3)
        if recommendations.isEmpty {
            ScyraCard(style: .plain) {
                Text("Keep exploring—your next reachable goal will appear here.")
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        } else {
            ForEach(Array(recommendations)) { badgeRow($0) }
        }
    }

    @ViewBuilder private var progressPage: some View {
        let recent = viewModel.achievementDashboard.badges
            .filter { $0.earned && $0.lastAdvancedAt != nil }
            .sorted { ($0.lastAdvancedAt ?? .distantPast) > ($1.lastAdvancedAt ?? .distantPast) }
            .prefix(5)
        if !recent.isEmpty {
            ScyraSectionHeader(title: "Recently Earned and Updated", subtitle: "New and advanced achievements.")
            ForEach(Array(recent)) { badgeRow($0) }
            Divider().foregroundStyle(ScyraColors.outlineVariant)
        }
        ScyraSectionHeader(title: "Collection Progress", subtitle: "Lifetime discovery and Mastery survive release.")
        let collections = viewModel.allCollectionProgress
        if collections.isEmpty {
            ScyraCard(style: .plain) {
                Text("Complete a regular Flow or explore Stillwater to begin a collection.")
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        } else {
            ForEach(collections) { collectionCard($0) }
        }
    }

    private func collectionCard(_ progress: StillwaterCollectionProgress) -> some View {
        ScyraCard(style: .elevated, action: {
            selectedCollection = .init(collectionID: progress.id, focusSpeciesID: nil)
        }) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(progress.title).font(ScyraTypography.cardTitle)
                    Spacer()
                    ScyraCanonicalIcon(systemName: "arrow.right.circle", size: 18)
                        .foregroundStyle(ScyraColors.primary)
                }
                Text("\(progress.discovered) of \(progress.total) discovered · \(progress.mastered) mastered")
                    .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                ProgressView(value: Double(progress.mastered), total: Double(max(1, progress.total)))
                    .tint(ScyraColors.primary)
            }
        }
        .accessibilityIdentifier("collection-card-\(progress.id)")
    }

    private func badgeRow(_ badge: AchievementProgress) -> some View {
        ScyraCard(style: badge.earned ? .elevated : .plain, action: { open(badge) }) {
            HStack(spacing: 10) {
                badgeMedallion(badge, diameter: 56)
                VStack(alignment: .leading, spacing: 4) {
                    Text(badge.title).font(ScyraTypography.cardTitle)
                    Text("Completed \(badge.count) times")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    if badge.isNew { Text("NEW").font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary) }
                }
                Spacer()
                Button {
                    badge.pinOrder == nil ? viewModel.pin(badge.badgeID) : viewModel.unpin(badge.badgeID)
                } label: {
                    ScyraCanonicalIcon(systemName: badge.pinOrder == nil ? "plus.circle" : "pin", size: 24)
                        .foregroundStyle(ScyraColors.primary)
                }
                .buttonStyle(.plain)
                .disabled(!badge.earned)
                .accessibilityLabel(badge.pinOrder == nil ? "Pin" : "Unpin")
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("achievement-\(badge.badgeID)")
    }

    private func badgeMedallion(_ badge: AchievementProgress, diameter: CGFloat) -> some View {
        ZStack {
            Circle()
                .fill(badge.earned ? ScyraColors.secondaryContainer : ScyraColors.surfaceVariant)
                .overlay(Circle().stroke(ScyraColors.outlineVariant, lineWidth: 1))
            ScyraCanonicalIcon(systemName: badge.earned ? "medal.fill" : "lock", size: diameter * 0.48)
                .foregroundStyle(badge.earned ? ScyraColors.secondaryGold : ScyraColors.textMuted)
            if viewModel.trackedBadgeIDs.contains(badge.badgeID) {
                ScyraCanonicalIcon(systemName: "scope", size: 16)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
            if badge.pinOrder != nil {
                ScyraCanonicalIcon(systemName: "pin", size: 16)
                    .foregroundStyle(ScyraColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            }
        }
        .frame(width: diameter, height: diameter)
        .accessibilityHidden(true)
    }

    private func badgeDetails(_ badge: AchievementProgress) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                    ScyraCanonicalIcon(systemName: badge.earned ? "medal.fill" : "medal")
                        .font(.system(size: 58, weight: .semibold))
                        .foregroundStyle(badge.earned ? ScyraColors.secondaryGold : ScyraColors.textMuted)
                        .frame(maxWidth: .infinity).accessibilityHidden(true)
                    Text(badge.title).font(ScyraTypography.screenTitle)
                    Text(badge.description).font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    ScyraStatPill(label: "Lifetime count", value: "\(badge.count)", systemImage: "number")
                    if let next = badge.milestone.nextThreshold {
                        VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                            Text("Next milestone: \(next)").font(ScyraTypography.label)
                            ProgressView(value: Double(badge.count), total: Double(max(1, next))).tint(ScyraColors.primary)
                            Text("\(badge.remaining) remaining").font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                        }
                    } else if badge.earned {
                        Text("No further milestone is defined.").font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                    }
                    if badge.earned {
                        if badge.pinOrder == nil {
                            ScyraButton("Pin to Showcase", systemImage: "pin") { viewModel.pin(badge.badgeID); selectedBadge = nil }
                        } else {
                            ScyraButton("Remove from Showcase", systemImage: "pin.slash", variant: .ghost) { viewModel.unpin(badge.badgeID); selectedBadge = nil }
                        }
                    }
                    ScyraButton(
                        viewModel.trackedBadgeIDs.contains(badge.badgeID) ? "Stop tracking" : "Track progress",
                        systemImage: viewModel.trackedBadgeIDs.contains(badge.badgeID) ? "scope" : "scope",
                        variant: .ghost
                    ) {
                        viewModel.setBadgeTracked(
                            badge.badgeID,
                            tracked: !viewModel.trackedBadgeIDs.contains(badge.badgeID)
                        )
                        selectedBadge = nil
                    }
                    if let action = viewModel.achievementAction(for: badge) {
                        ScyraButton(actionLabel(action), systemImage: "arrow.right.circle") {
                            navigate(action)
                        }
                        .accessibilityIdentifier("achievement-action-\(badge.badgeID)")
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background.ignoresSafeArea())
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { selectedBadge = nil } } }
        }
        .presentationDetents([.medium, .large])
    }

    private func open(_ badge: AchievementProgress) {
        selectedBadge = badge
        if badge.isNew { viewModel.markBadgeViewed(badge.badgeID) }
    }

    private func collectionDetails(_ selection: CollectionDetailSelection) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                    if let progress = viewModel.progress(for: selection.collectionID) {
                        Text(progress.title).font(ScyraTypography.screenTitle)
                        Text("\(progress.discovered) discovered · \(progress.owned) owned · \(progress.mastered) mastered · \(progress.total) total")
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                        HStack {
                            collectionEvidence("Collector", earned: progress.collectorEarned)
                            collectionEvidence("Curator", earned: progress.curatorEarned)
                            collectionEvidence("Completionist", earned: progress.completionistEarned)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    ForEach(viewModel.collectionSpecies(for: selection.collectionID)) { species in
                        collectionSpeciesRow(species, focused: species.speciesID == selection.focusSpeciesID)
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { selectedCollection = nil }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .accessibilityIdentifier("collection-details-\(selection.collectionID)")
    }

    private func collectionSpeciesRow(_ species: CollectionSpeciesProgress, focused: Bool) -> some View {
        let definition = CreatureCatalog.definition(species.speciesID)
        let title = definition?.displayName ?? "Unknown creature"
        let content = HStack(spacing: ScyraSpacing.md) {
            ScyraCanonicalIcon(systemName: definition?.systemImage ?? "questionmark", size: 36)
                .foregroundStyle(ScyraColors.primary)
                .frame(width: 48)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(ScyraTypography.cardTitle)
                Text(speciesStatus(species))
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
            Spacer()
            if species.action != nil {
                ScyraCanonicalIcon(systemName: "arrow.right.circle", size: 18)
                    .foregroundStyle(ScyraColors.primary)
            }
        }
        .padding(12)
        .background(focused ? ScyraColors.secondaryContainer : ScyraColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: ScyraRadius.card).stroke(
            focused ? ScyraColors.primary : ScyraColors.outlineVariant,
            lineWidth: focused ? 2 : 1
        ))

        return Group {
            if let action = species.action {
                Button { navigate(action) } label: { content }
                    .buttonStyle(.plain)
            } else {
                content
            }
        }
        .accessibilityIdentifier("collection-species-\(species.speciesID)")
    }

    private func collectionEvidence(_ title: String, earned: Bool) -> some View {
        ScyraStatPill(label: title, value: earned ? "Earned" : "Locked")
    }

    private func speciesStatus(_ species: CollectionSpeciesProgress) -> String {
        if species.lifetimeMasteryCount > 0 {
            return "Mastered \(species.lifetimeMasteryCount) time\(species.lifetimeMasteryCount == 1 ? "" : "s") · \(species.currentLevel99Count) currently at Level 99"
        }
        if species.ownedCount > 0 {
            let level = species.highestLevel ?? 1
            return "Owned: \(species.ownedCount) · Highest Level \(level) · \(max(0, 99 - level)) levels to Mastery"
        }
        return species.discovered ? "Discovered · not currently owned" : "Undiscovered"
    }

    private func actionLabel(_ action: AchievementActionDestination) -> String {
        switch action {
        case .flow: "Open Flow"
        case .arc: "Plan an Arc"
        case .badgeDetails: "Open badge"
        case .collectionDetails: "Open collection"
        case .chestSpecies: "View in The Chest"
        case .blueRegion, .beyondBlue: "Open The Blue"
        case .stillwaterVessel: "Open Stillwater"
        }
    }

    private func navigate(_ action: AchievementActionDestination) {
        switch action {
        case .collectionDetails(let collectionID, let speciesID):
            selectedBadge = nil
            Task { @MainActor in
                await Task.yield()
                selectedCollection = .init(collectionID: collectionID, focusSpeciesID: speciesID)
            }
        case .badgeDetails(let badgeID):
            selectedCollection = nil
            selectedBadge = viewModel.achievementDashboard.badges.first { $0.badgeID == badgeID }
        default:
            selectedBadge = nil
            selectedCollection = nil
            onNavigate(action)
        }
    }

    private func categoryTitle(_ category: AchievementCategory) -> String {
        switch category {
        case .all: "All"
        case .flow: "Flow"
        case .arc: "Arc"
        case .creatures: "Creatures"
        case .mastery: "Mastery"
        case .collections: "Collections"
        case .stillwater: "Stillwater"
        case .movement: "Movement"
        case .surge: "Surge"
        case .objectives: "Objectives"
        case .special: "Special"
        case .historical: "Historical"
        }
    }
    private func sortTitle(_ sort: AchievementSort) -> String {
        switch sort {
        case .recommended: "Recommended"
        case .recentlyEarned: "Recently earned"
        case .recentlyAdvanced: "Recently advanced"
        case .highestCount: "Highest count"
        case .closestMilestone: "Closest milestone"
        case .alphabetical: "Alphabetical"
        }
    }
}

#Preview {
    ShellAchievementsView(viewModel: ShellViewModel(repository: InMemoryFlowRepository()))
}
