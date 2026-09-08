import SwiftUI

struct HorizonView: View {
    @ObservedObject var viewModel: HorizonViewModel
    let onPrepareFlowPlan: (FlowPlan) -> Bool
    let onPrepareArcPlan: (ArcPlan, Bool) -> Bool
    let onOpenCurrentFlow: () -> Void

    @State private var editor: FlowPlanEditorPresentation?
    @State private var dreamsExpanded = false
    @State private var pendingDelete: FlowPlan?
    @State private var arcEditor: ArcPlanEditorPresentation?
    @State private var deferredArcEditor: ArcPlanEditorPresentation?
    @State private var selectedArc: ArcPlan?
    @State private var selectedSuggestion: SuggestedRoute?
    @State private var pendingDeleteArc: ArcPlan?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ScyraSpacing.md) {
                header
                primaryTabs

                if viewModel.isLoading {
                    ProgressView("Loading paths")
                        .frame(maxWidth: .infinity, minHeight: 160)
                        .accessibilityIdentifier("horizon-loading")
                } else if let error = viewModel.errorMessage,
                          viewModel.activePlans.isEmpty,
                          viewModel.dreamPlans.isEmpty,
                          viewModel.arcPlans.isEmpty {
                    ScyraEmptyState(
                        systemImage: "exclamationmark.triangle",
                        title: "Horizon unavailable",
                        message: error,
                        actionTitle: "Try again",
                        action: viewModel.refresh
                    )
                } else if viewModel.selectedPrimaryTab == .flows {
                    flowLibrary
                } else {
                    arcLibrary
                }
            }
            .padding(.horizontal, ScyraSpacing.screenPadding)
            .padding(.top, ScyraSpacing.md)
            .padding(.bottom, ScyraSpacing.xl)
        }
        .background(ScyraColors.background)
        .onAppear {
            viewModel.refresh()
            presentRequestedArcEditor()
        }
        .onChange(of: viewModel.pendingNewArcRequestID) { _, requestID in
            if requestID != nil { presentRequestedArcEditor() }
        }
        .sheet(item: $editor) { presentation in
            FlowPlanEditorSheet(
                initialDraft: presentation.draft,
                suggestions: viewModel.journeySuggestions,
                isSaving: viewModel.isSaving,
                errorMessage: viewModel.errorMessage,
                onCancel: { editor = nil },
                onSave: { draft in
                    if viewModel.save(draft) { editor = nil }
                }
            )
            .presentationDetents([.large])
        }
        .sheet(item: $arcEditor) { presentation in
            ArcPlanEditorSheet(
                initialDraft: presentation.draft,
                flowPlans: viewModel.activePlans,
                isSaving: viewModel.isSaving,
                errorMessage: viewModel.errorMessage,
                onCancel: { arcEditor = nil },
                onSave: { draft in
                    if viewModel.saveArc(draft, editing: presentation.editingID) != nil {
                        arcEditor = nil
                    }
                }
            )
        }
        .sheet(item: $selectedArc, onDismiss: presentDeferredArcEditor) { plan in
            ArcPlanDetailSheet(
                plan: viewModel.arcPlan(id: plan.id) ?? plan,
                activeRun: viewModel.activePlannedArcRun,
                onClose: { selectedArc = nil },
                onEdit: {
                    deferredArcEditor = ArcPlanEditorPresentation(plan: viewModel.arcPlan(id: plan.id) ?? plan)
                    selectedArc = nil
                },
                onStudioToggle: {
                    viewModel.setInStudio(plan, isInStudio: !plan.isInStudio)
                    selectedArc = viewModel.arcPlan(id: plan.id)
                },
                onBegin: { restart in launch(plan, restart: restart) }
            )
        }
        .sheet(item: $selectedSuggestion) { route in
            SuggestedRouteDetailSheet(
                route: route,
                isSaving: viewModel.isSaving,
                errorMessage: viewModel.errorMessage,
                onClose: { selectedSuggestion = nil },
                onSave: { addToStudio in
                    if viewModel.saveSuggestedRoute(route, addToStudio: addToStudio) != nil {
                        selectedSuggestion = nil
                    }
                },
                onBegin: {
                    guard let plan = viewModel.saveSuggestedRoute(route, addToStudio: false) else { return }
                    launch(plan, restart: true)
                }
            )
        }
        .alert(
            pendingDelete?.archived == true
                ? HorizonStrings.deleteDreamTitle
                : HorizonStrings.deletePlanTitle,
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            )
        ) {
            Button(HorizonStrings.cancel, role: .cancel) { pendingDelete = nil }
            Button(HorizonStrings.delete, role: .destructive) {
                if let pendingDelete { viewModel.delete(pendingDelete) }
                pendingDelete = nil
            }
        } message: {
            Text(
                pendingDelete?.archived == true
                    ? HorizonStrings.deleteDreamBody
                    : HorizonStrings.deletePlanBody
            )
        }
        .alert("Delete arc?", isPresented: Binding(
            get: { pendingDeleteArc != nil },
            set: { if !$0 { pendingDeleteArc = nil } }
        )) {
            Button(HorizonStrings.cancel, role: .cancel) { pendingDeleteArc = nil }
            Button(HorizonStrings.delete, role: .destructive) {
                if let pendingDeleteArc { viewModel.delete(pendingDeleteArc) }
                pendingDeleteArc = nil
            }
        } message: {
            Text("This will permanently remove this arc.")
        }
        .alert(HorizonStrings.activeFlow, isPresented: $viewModel.showsActiveFlowConflict) {
            Button(HorizonStrings.cancel, role: .cancel) {}
            Button(HorizonStrings.viewFlow, action: onOpenCurrentFlow)
        } message: {
            Text(HorizonStrings.activeFlowBody)
        }
    }

    @ViewBuilder private var flowLibrary: some View {
        if viewModel.activePlans.isEmpty, viewModel.dreamPlans.isEmpty {
                    ScyraEmptyState(
                        assetImageName: "materialAutoAwesome",
                        title: HorizonStrings.emptyTitle,
                        message: HorizonStrings.emptyBody,
                        actionTitle: HorizonStrings.planFlow,
                        action: presentNewPlan
                    )
        } else {
            if !viewModel.activePlans.isEmpty {
                sectionHeader
                ForEach(viewModel.activePlans) { plan in
                    FlowPlanCard(
                        plan: plan,
                        onLaunch: { launch(plan) },
                        onPin: { viewModel.setPinned(plan, pinned: !plan.pinned) },
                        onDream: { viewModel.moveToDreams(plan) },
                        onDelete: { pendingDelete = plan }
                    )
                }
            }
            if !viewModel.dreamPlans.isEmpty {
                dreamsHeader
                if dreamsExpanded {
                    ForEach(viewModel.dreamPlans) { plan in
                        DreamFlowPlanCard(
                            plan: plan,
                            onRestore: { viewModel.restore(plan) },
                            onDelete: { pendingDelete = plan }
                        )
                    }
                }
            }
        }
    }

    private func presentRequestedArcEditor() {
        guard viewModel.consumeNewArcPlanRequest() else { return }
        viewModel.clearError()
        arcEditor = ArcPlanEditorPresentation()
    }

    @ViewBuilder private var arcLibrary: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                ScyraCanonicalLabel("Your Studio", systemImage: "point.topleft.down.to.point.bottomright.curvepath")
                    .font(ScyraTypography.cardTitle)
                Text("Design the sequences you want to move through.")
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
            Spacer()
            Button("Create Arc") {
                viewModel.clearError()
                arcEditor = ArcPlanEditorPresentation()
            }
            .buttonStyle(.borderedProminent)
            .tint(ScyraColors.primary)
            .accessibilityIdentifier("horizon-create-arc")
        }

        if viewModel.arcPlans.isEmpty {
            ScyraEmptyState(
                systemImage: "point.topleft.down.to.point.bottomright.curvepath",
                title: "Build your first Arc",
                message: "Use Your Studio when you know the next few Flows you want to move through.",
                actionTitle: "Create Arc",
                action: { arcEditor = ArcPlanEditorPresentation() }
            )
        } else {
            ForEach(viewModel.arcPlans) { plan in
                ArcPlanCard(
                    plan: plan,
                    activeRun: viewModel.activePlannedArcRun,
                    onOpen: { selectedArc = plan },
                    onEdit: { arcEditor = ArcPlanEditorPresentation(plan: plan) },
                    onStudioToggle: { viewModel.setInStudio(plan, isInStudio: !plan.isInStudio) },
                    onDelete: { pendingDeleteArc = plan }
                )
            }
        }

        VStack(alignment: .leading, spacing: 4) {
            Text("Suggested Scenes").font(ScyraTypography.cardTitle)
            Text("Start from a shape, then make it yours.")
                .font(ScyraTypography.caption)
                .foregroundStyle(ScyraColors.textSecondary)
        }
        .padding(.top, ScyraSpacing.sm)
        ForEach(SuggestedRoutesCatalog.routes) { route in
            SuggestedRouteCard(route: route) { selectedSuggestion = route }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                ScyraMaterialIcon(assetName: "materialExplore", size: 24, color: ScyraColors.primary)
                Text(HorizonStrings.title)
            }
                .font(.system(size: 28, weight: .semibold, design: .monospaced))
                .foregroundStyle(ScyraColors.secondaryGold)
            Text(HorizonStrings.subtitle)
                .font(ScyraTypography.body)
                .foregroundStyle(ScyraColors.textSecondary)
        }
        .accessibilityElement(children: .combine)
    }

    private var primaryTabs: some View {
        HStack(spacing: 6) {
            ForEach(HorizonPrimaryTab.allCases) { tab in
                let selected = viewModel.selectedPrimaryTab == tab
                Button {
                    withAnimation(.easeInOut(duration: 0.18)) {
                        viewModel.selectedPrimaryTab = tab
                    }
                } label: {
                    Text(tab.rawValue)
                        .font(.subheadline.weight(selected ? .semibold : .medium))
                        .foregroundStyle(selected ? ScyraColors.secondaryGold : ScyraColors.textMuted)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(selected ? ScyraColors.surface : ScyraColors.surfaceVariant, in: Capsule())
                        .shadow(color: selected ? Color.black.opacity(0.10) : .clear, radius: 1, y: 1)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tab.rawValue)
                .accessibilityValue(selected ? "Selected" : "Not selected")
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
        .padding(3)
        .background(ScyraColors.surfaceVariant, in: Capsule())
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("horizon-primary-tab")
    }

    private var sectionHeader: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    ScyraMaterialIcon(assetName: "materialAutoAwesome", size: 20, color: ScyraColors.primary)
                    Text(HorizonStrings.plannedFlows)
                }
                    .font(ScyraTypography.cardTitle)
                    .foregroundStyle(ScyraColors.textPrimary)
                Text(HorizonStrings.plannedFlowsSubtitle)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
            Spacer()
            Button(HorizonStrings.planFlow, action: presentNewPlan)
                .buttonStyle(.borderedProminent)
                .tint(ScyraColors.primary)
                .accessibilityIdentifier("horizon-plan-flow")
        }
    }

    private var dreamsHeader: some View {
        Button { dreamsExpanded.toggle() } label: {
            HStack(spacing: 10) {
                ScyraCanonicalIcon(systemName: "cloud")
                VStack(alignment: .leading, spacing: 2) {
                    Text(HorizonStrings.dreams).font(ScyraTypography.cardTitle)
                    Text(dreamCountText)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
                Spacer()
                Text(dreamsExpanded ? HorizonStrings.hide : HorizonStrings.show)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.primary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(ScyraColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Dreams. (dreamCountText).")
        .accessibilityValue(dreamsExpanded ? "Expanded" : "Collapsed")
        .accessibilityIdentifier("horizon-dreams")
    }

    private var dreamCountText: String {
        let count = viewModel.dreamPlans.count
        return count == 1 ? "1 flow for someday" : "\(count) flows for someday"
    }

    private func presentNewPlan() {
        viewModel.clearError()
        editor = FlowPlanEditorPresentation(draft: FlowPlanDraft())
    }

    private func presentDeferredArcEditor() {
        guard let deferredArcEditor else { return }
        self.deferredArcEditor = nil
        arcEditor = deferredArcEditor
    }

    private func launch(_ plan: FlowPlan) {
        if onPrepareFlowPlan(plan) {
            viewModel.recordSuccessfulLaunch(plan)
        } else {
            viewModel.reportActiveFlowConflict()
        }
    }

    private func launch(_ plan: ArcPlan, restart: Bool) {
        if onPrepareArcPlan(viewModel.arcPlan(id: plan.id) ?? plan, restart) {
            selectedArc = nil
            selectedSuggestion = nil
            viewModel.refresh()
        } else {
            viewModel.reportActiveFlowConflict()
        }
    }
}

private struct ArcPlanEditorPresentation: Identifiable {
    let id = UUID()
    let editingID: UUID?
    let draft: ArcPlanDraft

    init() {
        editingID = nil
        draft = ArcPlanDraft()
    }

    init(plan: ArcPlan) {
        editingID = plan.id
        draft = ArcPlanDraft(plan: plan)
    }
}

private struct FlowPlanEditorPresentation: Identifiable {
    let id = UUID()
    let draft: FlowPlanDraft
}

private struct FlowPlanEditorSheet: View {
    @State private var draft: FlowPlanDraft
    let suggestions: [String]
    let isSaving: Bool
    let errorMessage: String?
    let onCancel: () -> Void
    let onSave: (FlowPlanDraft) -> Void

    init(
        initialDraft: FlowPlanDraft,
        suggestions: [String],
        isSaving: Bool,
        errorMessage: String?,
        onCancel: @escaping () -> Void,
        onSave: @escaping (FlowPlanDraft) -> Void
    ) {
        _draft = State(initialValue: initialDraft)
        self.suggestions = suggestions
        self.isSaving = isSaving
        self.errorMessage = errorMessage
        self.onCancel = onCancel
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(HorizonStrings.flowIntentPrompt)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textMuted)
                        TextField(HorizonStrings.flowIntentPlaceholder, text: $draft.title)
                            .font(ScyraTypography.cardTitle)
                            .textInputAutocapitalization(.sentences)
                            .accessibilityIdentifier("flow-plan-title")
                    }
                    .padding(18)
                    .background(ScyraColors.surface.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

                    VStack(alignment: .leading, spacing: 10) {
                        Text(HorizonStrings.journey).font(ScyraTypography.label)
                        TextField(HorizonStrings.journeyPlaceholder, text: $draft.journeyName)
                            .textFieldStyle(.roundedBorder)
                            .accessibilityIdentifier("flow-plan-journey")
                        if !suggestions.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(suggestions.prefix(8), id: \.self) { journey in
                                        Button(journey) {
                                            draft.journeyName = draft.journeyName == journey ? "" : journey
                                        }
                                        .buttonStyle(.bordered)
                                        .tint(ScyraColors.primary)
                                    }
                                }
                            }
                        }
                    }

                    VStack(spacing: 14) {
                        Toggle(isOn: softBinding) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(HorizonStrings.softFlow).font(ScyraTypography.label)
                                Text(HorizonStrings.softFlowBody)
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                        }
                        .tint(ScyraColors.primary)
                        .accessibilityIdentifier("flow-plan-soft")

                        Divider()

                        Toggle(isOn: surgeBinding) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(HorizonStrings.surge).font(ScyraTypography.label)
                                Text(surgeBody)
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                        }
                        .disabled(draft.isSoftMode)
                        .tint(ScyraColors.primary)
                        .accessibilityIdentifier("flow-plan-surge")

                        if draft.launchWithSurge, !draft.isSoftMode {
                            HStack {
                                TextField("25", text: $draft.targetMinutesText)
                                    .keyboardType(.numberPad)
                                    .textFieldStyle(.roundedBorder)
                                    .frame(maxWidth: 100)
                                    .accessibilityLabel("Target minutes")
                                    .accessibilityIdentifier("flow-plan-minutes")
                                    .onChange(of: draft.targetMinutesText) { _, value in
                                        draft.targetMinutesText = String(value.filter(\.isNumber).prefix(3))
                                    }
                                Text(HorizonStrings.minutes)
                                    .font(ScyraTypography.label)
                                    .foregroundStyle(ScyraColors.textSecondary)
                                Spacer()
                            }
                        }
                    }
                    .padding(16)
                    .background(ScyraColors.surface.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

                    if let errorMessage, !errorMessage.isEmpty {
                        Text(errorMessage)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.error)
                            .accessibilityIdentifier("flow-plan-error")
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .navigationTitle(HorizonStrings.planFlow)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(HorizonStrings.cancel, action: onCancel).disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? HorizonStrings.saving : HorizonStrings.save) {
                        onSave(draft)
                    }
                    .disabled(isSaving || draft.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("flow-plan-save")
                }
            }
        }
    }

    private var softBinding: Binding<Bool> {
        Binding(
            get: { draft.isSoftMode },
            set: { enabled in
                draft.isSoftMode = enabled
                if enabled {
                    draft.launchWithSurge = false
                    draft.targetMinutesText = ""
                }
            }
        )
    }

    private var surgeBinding: Binding<Bool> {
        Binding(
            get: { draft.launchWithSurge },
            set: { enabled in
                guard !draft.isSoftMode else { return }
                draft.launchWithSurge = enabled
                if !enabled { draft.targetMinutesText = "" }
            }
        )
    }

    private var surgeBody: String {
        if draft.isSoftMode { return HorizonStrings.surgeSoftBody }
        return draft.launchWithSurge ? HorizonStrings.surgeOnBody : HorizonStrings.surgeOffBody
    }
}

private struct FlowPlanCard: View {
    let plan: FlowPlan
    let onLaunch: () -> Void
    let onPin: () -> Void
    let onDream: () -> Void
    let onDelete: () -> Void

    var body: some View {
        ScyraCard(style: .elevated, padding: 16) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 10) {
                    Button(action: onLaunch) {
                        HStack(alignment: .top, spacing: 10) {
                            ScyraCanonicalIcon(systemName: plan.isSoftMode ? "leaf" : "sparkles")
                                .foregroundStyle(plan.isSoftMode ? ScyraColors.secondaryGold : ScyraColors.primary)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(plan.title)
                                    .font(ScyraTypography.cardTitle)
                                    .foregroundStyle(ScyraColors.textPrimary)
                                if !metadata.isEmpty {
                                    Text(metadata)
                                        .font(ScyraTypography.caption)
                                        .foregroundStyle(ScyraColors.textSecondary)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)

                    if plan.pinned {
                        ScyraCanonicalIcon(systemName: "pin")
                            .foregroundStyle(ScyraColors.textSecondary)
                            .accessibilityLabel("Pinned")
                    }

                    Menu {
                        Button(plan.pinned ? HorizonStrings.unpin : HorizonStrings.pin, action: onPin)
                        Button(HorizonStrings.moveToDreams, action: onDream)
                        Button(HorizonStrings.delete, role: .destructive, action: onDelete)
                    } label: {
                        ScyraCanonicalIcon(systemName: "ellipsis")
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("More actions")
                }

                if !badges.isEmpty {
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 8) { badgeContent }
                        VStack(alignment: .leading, spacing: 8) { badgeContent }
                    }
                }
                Divider()
                Text(launchText)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("flow-plan-card-\(plan.id.uuidString)")
    }

    @ViewBuilder private var badgeContent: some View {
        ForEach(badges, id: \.self) { badge in
            Text(badge)
                .font(ScyraTypography.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(ScyraColors.surface)
                .clipShape(Capsule())
        }
    }

    private var badges: [String] {
        var result: [String] = []
        if let minutes = plan.targetMinutes { result.append("\(minutes) min") }
        if plan.launchWithSurge { result.append(HorizonStrings.surge) }
        if plan.isSoftMode { result.append("Soft") }
        return result
    }

    private var metadata: String {
        var values: [String] = []
        if let journey = plan.journeyName, !journey.isEmpty { values.append(journey) }
        if let minutes = plan.targetMinutes { values.append("\(minutes)m") }
        if plan.launchWithSurge { values.append(HorizonStrings.surge) }
        return values.joined(separator: " • ")
    }

    private var launchText: String {
        switch plan.launchCount {
        case 0: HorizonStrings.notLaunched
        case 1: "Launched 1 time"
        default: "Launched \(plan.launchCount) times"
        }
    }
}

private struct DreamFlowPlanCard: View {
    let plan: FlowPlan
    let onRestore: () -> Void
    let onDelete: () -> Void

    var body: some View {
        ScyraCard(style: .plain, padding: 16) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    ScyraCanonicalIcon(systemName: plan.isSoftMode ? "leaf" : "sparkles")
                    Text(plan.title).font(ScyraTypography.cardTitle)
                    Spacer()
                    Menu {
                        Button(HorizonStrings.bringBack, action: onRestore)
                        Button(HorizonStrings.delete, role: .destructive, action: onDelete)
                    } label: {
                        ScyraCanonicalIcon(systemName: "ellipsis").frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("More actions")
                }
                if let journey = plan.journeyName, !journey.isEmpty {
                    Text(journey).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                }
                Divider()
                Text(HorizonStrings.savedInDreams)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("dream-flow-plan-card-\(plan.id.uuidString)")
    }
}

private struct ArcPlanCard: View {
    let plan: ArcPlan
    let activeRun: ActivePlannedArcRun?
    let onOpen: () -> Void
    let onEdit: () -> Void
    let onStudioToggle: () -> Void
    let onDelete: () -> Void

    var body: some View {
        ScyraCard(style: .elevated, padding: 16) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 10) {
                    Button(action: onOpen) {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                ScyraCanonicalIcon(systemName: "point.topleft.down.to.point.bottomright.curvepath")
                                    .foregroundStyle(ScyraColors.primary)
                                Text(plan.title)
                                    .font(ScyraTypography.cardTitle)
                                    .foregroundStyle(ScyraColors.textPrimary)
                            }
                            Text(metadata)
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("arc-plan-open-\(plan.id.uuidString)")
                    Menu {
                        Button("Edit", action: onEdit)
                        Button(plan.isInStudio ? "Remove from Studio" : "Add to Studio", action: onStudioToggle)
                        Button(HorizonStrings.delete, role: .destructive, action: onDelete)
                    } label: {
                        ScyraCanonicalIcon(systemName: "ellipsis").frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("More actions")
                }
                if activeRun?.arcPlanID == plan.id {
                    ScyraCanonicalLabel(
                        "Arc in progress • Step \((activeRun?.currentStepIndex ?? 0) + 1) of \(activeRun?.totalSteps ?? plan.steps.count)",
                        systemImage: "play.circle.fill"
                    )
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.primary)
                }
                Text(plan.steps.map(\.titleSnapshot).joined(separator: "  →  "))
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .lineLimit(2)
                Divider()
                Text(launchText)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("arc-plan-card-\(plan.id.uuidString)")
    }

    private var metadata: String {
        var values = [plan.steps.count == 1 ? "1 Flow" : "\(plan.steps.count) Flows"]
        if let total = plan.totalTargetMinutes { values.append("\(total) min") }
        if plan.hasSurge { values.append("Surge") }
        if plan.isInStudio { values.append("Studio") }
        return values.joined(separator: " • ")
    }

    private var launchText: String {
        switch plan.launchCount {
        case 0: "Not launched yet"
        case 1: "Launched 1 time"
        default: "Launched \(plan.launchCount) times"
        }
    }
}

private struct SuggestedRouteCard: View {
    let route: SuggestedRoute
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            ScyraCard(style: .plain, padding: 16) {
                HStack(spacing: 12) {
                    ScyraCanonicalIcon(systemName: "wand.and.stars")
                        .foregroundStyle(ScyraColors.secondaryGold)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(route.title).font(ScyraTypography.cardTitle)
                        Text(route.subtitle)
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                        Text("\(route.category) • ~\(route.approximateMinutes ?? 0) min • \(route.steps.count) Flows")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textMuted)
                    }
                    Spacer()
                    ScyraCanonicalIcon(systemName: "chevron.right").foregroundStyle(ScyraColors.textMuted)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Suggested sequence. \(route.title). \(route.subtitle). Use this shape.")
    }
}

private struct ArcPlanDetailSheet: View {
    let plan: ArcPlan
    let activeRun: ActivePlannedArcRun?
    let onClose: () -> Void
    let onEdit: () -> Void
    let onStudioToggle: () -> Void
    let onBegin: (Bool) -> Void

    private var isActive: Bool { activeRun?.arcPlanID == plan.id }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    ScyraCard(style: .elevated, padding: 18) {
                        VStack(alignment: .leading, spacing: 10) {
                            ScyraCanonicalLabel(plan.isInStudio ? "Studio Arc" : "Arc", systemImage: "point.topleft.down.to.point.bottomright.curvepath")
                                .font(ScyraTypography.label)
                                .foregroundStyle(ScyraColors.primary)
                            Text(plan.title).font(ScyraTypography.screenTitle)
                            Text(summary)
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                    }
                    if isActive {
                        ScyraCard(style: .plain, padding: 16) {
                            ScyraCanonicalLabel(
                                "Arc in progress. Resume from step \((activeRun?.currentStepIndex ?? 0) + 1) of \(activeRun?.totalSteps ?? plan.steps.count).",
                                systemImage: "play.circle"
                            )
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.primary)
                        }
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Route").font(ScyraTypography.cardTitle)
                        Text("This is the sequence you’ll move through when you begin.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    ForEach(plan.steps) { step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(step.orderIndex + 1)")
                                .font(ScyraTypography.label)
                                .frame(width: 32, height: 32)
                                .background(ScyraColors.primaryContainer)
                                .clipShape(Circle())
                            VStack(alignment: .leading, spacing: 3) {
                                HStack {
                                    Text(step.titleSnapshot).font(ScyraTypography.cardTitle)
                                    if isActive, activeRun?.currentStepIndex == step.orderIndex {
                                        Text("Current").font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary)
                                    }
                                }
                                Text(stepMetadata(step))
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(ScyraColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                    }
                    ScyraCard(style: .plain, padding: 16) {
                        HStack {
                            Text("Repeat").font(ScyraTypography.label)
                            Spacer()
                            Text(recurrenceText)
                                .font(ScyraTypography.body)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .navigationTitle(plan.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Close", action: onClose) }
                ToolbarItemGroup(placement: .primaryAction) {
                    Button("Edit", action: onEdit)
                    Menu {
                        Button(plan.isInStudio ? "Remove from Studio" : "Add to Studio", action: onStudioToggle)
                        if isActive { Button("Restart", role: .destructive) { onBegin(true) } }
                    } label: { ScyraCanonicalIcon(systemName: "ellipsis.circle") }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(isActive ? "Resume Arc" : "Begin Arc") { onBegin(false) }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.ultraThinMaterial)
                    .accessibilityIdentifier("arc-plan-begin")
            }
        }
        .accessibilityIdentifier("arc-plan-detail")
    }

    private var summary: String {
        var values = [plan.steps.count == 1 ? "1 Flow" : "\(plan.steps.count) Flows"]
        if let total = plan.totalTargetMinutes { values.append("\(total) min") }
        if plan.hasSurge { values.append("\(plan.steps.filter(\.launchWithSurgeSnapshot).count) Surge") }
        return values.joined(separator: " • ")
    }

    private var recurrenceText: String {
        guard plan.recurrence == .custom else { return plan.recurrence.title }
        let names = [1: "Mon", 2: "Tue", 3: "Wed", 4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun"]
        return plan.recurrenceDays.sorted().compactMap { names[$0] }.joined(separator: ", ")
    }

    private func stepMetadata(_ step: ArcPlanStep) -> String {
        var values: [String] = []
        if let journey = step.journeyNameSnapshot, !journey.isEmpty { values.append(journey) }
        if let minutes = step.targetMinutesSnapshot { values.append("\(minutes) min") }
        else { values.append("Untimed") }
        if step.launchWithSurgeSnapshot { values.append("Surge") }
        return values.joined(separator: " • ")
    }
}

private struct SuggestedRouteDetailSheet: View {
    let route: SuggestedRoute
    let isSaving: Bool
    let errorMessage: String?
    let onClose: () -> Void
    let onSave: (Bool) -> Void
    let onBegin: () -> Void
    @State private var addToStudio = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(route.subtitle)
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textSecondary)
                    ScyraCanonicalLabel("\(route.category) • ~\(route.approximateMinutes ?? 0) min", systemImage: "wand.and.stars")
                        .font(ScyraTypography.label)
                        .foregroundStyle(ScyraColors.primary)
                    ForEach(Array(route.steps.enumerated()), id: \.offset) { index, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(index + 1)").font(ScyraTypography.label)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(step.title).font(ScyraTypography.cardTitle)
                                Text("\(step.journeyName) • \(step.targetMinutes ?? 0) min\(step.launchWithSurge ? " • Surge" : "")")
                                    .font(ScyraTypography.caption)
                                    .foregroundStyle(ScyraColors.textSecondary)
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(ScyraColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                    }
                    Toggle("Add to Studio", isOn: $addToStudio).tint(ScyraColors.primary)
                    if let errorMessage, !errorMessage.isEmpty {
                        Text(errorMessage).foregroundStyle(ScyraColors.error)
                    }
                    Button("Save as Arc") { onSave(addToStudio) }
                        .buttonStyle(.bordered)
                        .disabled(isSaving)
                    Button("Begin Arc", action: onBegin)
                        .buttonStyle(.borderedProminent)
                        .tint(ScyraColors.primary)
                        .disabled(isSaving)
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .navigationTitle(route.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close", action: onClose) } }
        }
    }
}

private struct ArcPlanEditorSheet: View {
    @State private var draft: ArcPlanDraft
    let flowPlans: [FlowPlan]
    let isSaving: Bool
    let errorMessage: String?
    let onCancel: () -> Void
    let onSave: (ArcPlanDraft) -> Void

    init(
        initialDraft: ArcPlanDraft,
        flowPlans: [FlowPlan],
        isSaving: Bool,
        errorMessage: String?,
        onCancel: @escaping () -> Void,
        onSave: @escaping (ArcPlanDraft) -> Void
    ) {
        _draft = State(initialValue: initialDraft)
        self.flowPlans = flowPlans
        self.isSaving = isSaving
        self.errorMessage = errorMessage
        self.onCancel = onCancel
        self.onSave = onSave
    }

    private var availableFlows: [FlowPlan] { flowPlans.filter { !$0.isSoftMode } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Name your arc").font(ScyraTypography.cardTitle)
                        Text("Give this arc a clear name before you shape its steps.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                        TextField("Morning Routine", text: $draft.title)
                            .textFieldStyle(.roundedBorder)
                            .accessibilityIdentifier("arc-plan-title")
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Choose your Flows").font(ScyraTypography.cardTitle)
                        Text("Pick at least two planned Flows for this Arc.")
                            .font(ScyraTypography.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                        if availableFlows.isEmpty {
                            Text("No planned flows match this filter yet.")
                                .font(ScyraTypography.body)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                        ForEach(availableFlows) { flow in
                            let selected = draft.steps.contains { $0.sourceFlowPlanID == flow.id }
                            Button { toggle(flow) } label: {
                                HStack {
                                    ScyraCanonicalIcon(systemName: selected ? "checkmark.circle.fill" : "circle")
                                    VStack(alignment: .leading) {
                                        Text(flow.title).font(ScyraTypography.label)
                                        if let journey = flow.journeyName { Text(journey).font(ScyraTypography.caption) }
                                    }
                                    Spacer()
                                    if let minutes = flow.targetMinutes { Text("\(minutes)m").font(ScyraTypography.caption) }
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityValue(selected ? "Selected" : "Not selected")
                        }
                    }
                    .padding(16)
                    .background(ScyraColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))

                    if !draft.steps.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Shape the sequence").font(ScyraTypography.cardTitle)
                            ForEach(Array(draft.steps.enumerated()), id: \.element.id) { index, step in
                                selectedStep(step, index: index)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Repeat").font(ScyraTypography.cardTitle)
                        Picker("Repeat", selection: recurrenceBinding) {
                            ForEach(ArcPlanRecurrence.allCases) { recurrence in
                                Text(recurrence.title).tag(recurrence)
                            }
                        }
                        .pickerStyle(.menu)
                        if draft.recurrence == .custom {
                            HStack(spacing: 6) {
                                ForEach(Array(zip(1...7, ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"])), id: \.0) { day, name in
                                    Button(name) { toggleDay(day) }
                                        .buttonStyle(.bordered)
                                        .tint(draft.recurrenceDays.contains(day) ? ScyraColors.primary : ScyraColors.textMuted)
                                }
                            }
                        }
                        Toggle("Add to Studio", isOn: $draft.isInStudio).tint(ScyraColors.primary)
                    }

                    if let errorMessage, !errorMessage.isEmpty {
                        Text(errorMessage)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.error)
                            .accessibilityIdentifier("arc-plan-error")
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(ScyraColors.background)
            .navigationTitle("Your Studio")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel", action: onCancel).disabled(isSaving) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Saving…" : "Save Arc") { onSave(draft) }
                        .disabled(isSaving || draft.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .accessibilityIdentifier("arc-plan-save")
                }
            }
        }
        .accessibilityIdentifier("arc-plan-editor")
    }

    private func selectedStep(_ step: ArcPlanStepDraft, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("\(index + 1). \(step.title)").font(ScyraTypography.label)
                Spacer()
                Button { move(index, by: -1) } label: { ScyraCanonicalIcon(systemName: "arrow.up") }
                    .disabled(index == 0)
                    .accessibilityLabel("Move step up")
                Button { move(index, by: 1) } label: { ScyraCanonicalIcon(systemName: "arrow.down") }
                    .disabled(index == draft.steps.count - 1)
                    .accessibilityLabel("Move step down")
                Button(role: .destructive) { draft.steps.remove(at: index) } label: { ScyraCanonicalIcon(systemName: "xmark") }
                    .accessibilityLabel("Remove step")
            }
            HStack {
                TextField("25", text: stepMinutesBinding(index))
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
                    .frame(width: 90)
                Text("minutes").font(ScyraTypography.caption)
                Spacer()
                Toggle("Surge", isOn: stepSurgeBinding(index)).labelsHidden()
                    .disabled(Int(draft.steps[index].targetMinutesText) ?? 0 <= 0)
                Text("Surge").font(ScyraTypography.caption)
            }
        }
        .padding(14)
        .background(ScyraColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
    }

    private var recurrenceBinding: Binding<ArcPlanRecurrence> {
        Binding(
            get: { draft.recurrence },
            set: { value in
                draft.recurrence = value
                draft.recurrenceDays = value.defaultDays
            }
        )
    }

    private func stepMinutesBinding(_ index: Int) -> Binding<String> {
        Binding(
            get: { draft.steps[index].targetMinutesText },
            set: { value in
                let cleaned = String(value.filter(\.isNumber).prefix(3))
                draft.steps[index].targetMinutesText = cleaned
                if Int(cleaned) ?? 0 <= 0 { draft.steps[index].launchWithSurge = false }
            }
        )
    }

    private func stepSurgeBinding(_ index: Int) -> Binding<Bool> {
        Binding(
            get: { draft.steps[index].launchWithSurge },
            set: { draft.steps[index].launchWithSurge = $0 && (Int(draft.steps[index].targetMinutesText) ?? 0) > 0 }
        )
    }

    private func toggle(_ flow: FlowPlan) {
        if let index = draft.steps.firstIndex(where: { $0.sourceFlowPlanID == flow.id }) {
            draft.steps.remove(at: index)
        } else {
            draft.steps.append(ArcPlanStepDraft(plan: flow))
        }
    }

    private func move(_ index: Int, by offset: Int) {
        let destination = index + offset
        guard draft.steps.indices.contains(destination) else { return }
        draft.steps.swapAt(index, destination)
    }

    private func toggleDay(_ day: Int) {
        if draft.recurrenceDays.contains(day) { draft.recurrenceDays.remove(day) }
        else { draft.recurrenceDays.insert(day) }
    }
}
