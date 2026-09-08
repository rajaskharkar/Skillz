import SwiftUI

struct PulseView: View {
    @ObservedObject var viewModel: PulseViewModel
    let activeFlowContext: () -> (flowInstanceID: UUID, arcID: UUID?)?
    let onSaved: () -> Void
    let onCancel: () -> Void

    @State private var page: PulsePage = .pulse
    @State private var chronicleBlocksPager = false
    @State private var showDraftPrompt = false

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(title: PulseStrings.title) {
                viewModel.cancel()
                onCancel()
            }

            PulseChroniclePagerSelector(
                selection: $page,
                canLeaveChronicle: !chronicleBlocksPager
            )

            switch page {
            case .pulse:
                pulsePage
            case .chronicle:
                ChronicleTextEditorView(
                    snapshot: viewModel.chronicle,
                    onDraftChanged: viewModel.updateChronicleDraft,
                    onAdd: { _ = viewModel.addChronicleText() },
                    onUpdate: viewModel.updateChronicleMoment,
                    onDelete: viewModel.deleteChronicleMoment,
                    onMove: viewModel.moveChronicleMoment,
                    isImportingMedia: viewModel.isImportingChronicleMedia,
                    onImportMedia: viewModel.importChronicleMedia,
                    onStageMedia: viewModel.stageChronicleMedia,
                    onReplaceMedia: viewModel.replaceChronicleMedia,
                    onAddVoice: viewModel.addChronicleVoice,
                    onUpdateTranscript: viewModel.updateChronicleTranscript,
                    onEditingChanged: { chronicleBlocksPager = $0 }
                )
            }
        }
        .background(ScyraColors.background)
        .onAppear {
            viewModel.prepareForPresentation(isFlowActive: activeFlowContext() != nil)
        }
        .alert(ChronicleStrings.unfinishedMoment, isPresented: $showDraftPrompt) {
            Button(ChronicleStrings.addMoment) {
                if viewModel.addChronicleText() { save() }
            }
            Button(ChronicleStrings.discard, role: .destructive) {
                viewModel.discardChronicleDraftText()
                save()
            }
            Button(ChronicleStrings.cancel, role: .cancel) {}
        }
    }

    private var pulsePage: some View {
        ScrollView {
            VStack(spacing: 14) {
                hero
                titleCard
                journeyCard
                if viewModel.isFlowActive { attachCard }

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityLabel("Error: \(errorMessage)")
                }

                Divider()

                HStack(spacing: ScyraSpacing.sm) {
                    Button(PulseStrings.cancel) {
                        viewModel.cancel()
                        onCancel()
                    }
                    .frame(maxWidth: .infinity, minHeight: 50)

                    Button(viewModel.isSaving ? PulseStrings.saving : PulseStrings.save) {
                        if viewModel.chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            save()
                        } else {
                            showDraftPrompt = true
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .disabled(!viewModel.canSave)
                    .accessibilityIdentifier("save-pulse")
                }
            }
            .padding(.horizontal, ScyraSpacing.md)
            .padding(.vertical, ScyraSpacing.sm)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(ScyraColors.background)
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
            HStack(spacing: 10) {
                ScyraMaterialIcon(assetName: "materialPsychologyAlt", size: 24, color: ScyraColors.onPrimary)
                Text(PulseStrings.heroTitle)
                    .font(.title3.weight(.semibold))
            }
            Text(PulseStrings.heroBody)
                .font(ScyraTypography.body)
                .opacity(0.88)
        }
        .foregroundStyle(ScyraColors.onPrimary)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(ScyraSpacing.md)
        .background(ScyraColors.primary)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    private var titleCard: some View {
        VStack {
            VStack(spacing: ScyraSpacing.sm) {
                Text(PulseStrings.idea)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textMuted)
                    .tracking(0.6)
                TextField(
                    PulseStrings.titlePlaceholder,
                    text: Binding(get: { viewModel.title }, set: viewModel.updateTitle)
                )
                .font(.title2.weight(.regular))
                .multilineTextAlignment(.center)
                .padding(.horizontal, ScyraSpacing.md)
                .frame(minHeight: 56)
                .background(ScyraColors.surface.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.largeCard))
                .accessibilityLabel("Pulse idea title field")
                .accessibilityIdentifier("pulse-title-field")
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.surfaceVariant.opacity(0.50))
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private var journeyCard: some View {
        VStack {
            VStack(spacing: ScyraSpacing.sm) {
                Text(PulseStrings.journeys)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textMuted)
                    .tracking(0.6)
                    .frame(maxWidth: .infinity)

                if !viewModel.journeys.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: ScyraSpacing.sm) {
                            ForEach(viewModel.journeys.prefix(8)) { journey in
                                ScyraChip(
                                    journey.name,
                                    isSelected: journey.name.caseInsensitiveCompare(viewModel.journeyName) == .orderedSame
                                ) { viewModel.selectJourney(journey) }
                                .accessibilityLabel("Select journey \(journey.name)")
                            }
                        }
                    }
                }

                TextField(
                    PulseStrings.journeyPlaceholder,
                    text: Binding(get: { viewModel.journeyName }, set: viewModel.updateJourneyName)
                )
                .textInputAutocapitalization(.words)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, ScyraSpacing.md)
                .frame(minHeight: 52)
                .background(ScyraColors.surfaceVariant.opacity(0.48))
                .clipShape(Capsule())
                .accessibilityLabel("Journey name")
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.surfaceVariant.opacity(0.42))
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
    }

    private var attachCard: some View {
        VStack {
            Toggle(isOn: Binding(
                get: { viewModel.attachToCurrentFlow },
                set: viewModel.setAttachToCurrentFlow
            )) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(PulseStrings.attach).font(ScyraTypography.label)
                    Text(viewModel.attachToCurrentFlow
                        ? PulseStrings.attachEnabled
                        : PulseStrings.attachDisabled)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
            }
            .tint(ScyraColors.primary)
            .accessibilityLabel("Attach pulse to current flow")
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(ScyraColors.primary.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func save() {
        guard viewModel.save(activeFlowContext: activeFlowContext()) != nil else { return }
        viewModel.finishSuccessfulPresentation()
        onSaved()
    }
}

private enum PulsePage: Hashable {
    case pulse
    case chronicle
}

private struct PulseChroniclePagerSelector: View {
    @Binding var selection: PulsePage
    let canLeaveChronicle: Bool

    var body: some View {
        HStack(spacing: 3) {
            segment(.pulse, title: PulseStrings.title, iconAsset: "materialPsychologyAlt", enabled: selection != .chronicle || canLeaveChronicle)
            segment(.chronicle, title: ChronicleStrings.title, iconAsset: "materialAutoStories", enabled: true)
        }
        .padding(3)
        .frame(minWidth: 280, maxWidth: 360)
        .background(ScyraColors.primary.opacity(0.08), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
    }

    private func segment(_ page: PulsePage, title: String, iconAsset: String, enabled: Bool) -> some View {
        let selected = selection == page
        return Button {
            withAnimation(.easeInOut(duration: 0.18)) { selection = page }
        } label: {
            HStack(spacing: 7) {
                ScyraMaterialIcon(
                    assetName: iconAsset,
                    size: 20,
                    color: selected ? ScyraColors.onPrimary : ScyraColors.textPrimary
                )
                Text(title)
            }
                .font(.subheadline.weight(.medium))
                .tracking(0.6)
                .foregroundStyle(selected ? ScyraColors.onPrimary : ScyraColors.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 48)
                .padding(.horizontal, 10)
                .background(selected ? ScyraColors.primary : Color.clear, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(title)
        .accessibilityValue(selected ? "Selected" : "Not selected")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

#Preview {
    PulseView(
        viewModel: PulseViewModel(repository: InMemoryFlowRepository()),
        activeFlowContext: { nil },
        onSaved: {},
        onCancel: {}
    )
}
