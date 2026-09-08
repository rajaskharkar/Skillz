import SwiftUI

struct PulseEditView: View {
    @StateObject private var viewModel: PulseEditViewModel
    let backAccessibilityLabel: String
    let onSaved: () -> Void
    let onCancel: () -> Void

    @State private var page: PulseEditPage = .details
    @State private var chronicleBlocksPager = false
    @State private var showDraftPrompt = false

    init(
        pulseID: UUID,
        repository: any ScyraRepository,
        backAccessibilityLabel: String = "Back to Story",
        onSaved: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        _viewModel = StateObject(wrappedValue: PulseEditViewModel(
            pulseID: pulseID,
            repository: repository
        ))
        self.backAccessibilityLabel = backAccessibilityLabel
        self.onSaved = onSaved
        self.onCancel = onCancel
    }

    var body: some View {
        VStack(spacing: 0) {
            ScyraActionScreenHeader(
                title: "Edit Pulse",
                backAccessibilityLabel: backAccessibilityLabel,
                onBack: onCancel
            )

            if viewModel.pulse == nil {
                ScyraEmptyState(
                    systemImage: "exclamationmark.triangle",
                    title: "Pulse not found",
                    message: "This Pulse may have been removed."
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                Picker("Edit Pulse page", selection: $page) {
                    ScyraCanonicalLabel("Pulse", systemImage: "brain.head.profile").tag(PulseEditPage.details)
                    ScyraCanonicalLabel(ChronicleStrings.title, systemImage: "book.pages").tag(PulseEditPage.chronicle)
                }
                .pickerStyle(.segmented)
                .disabled(chronicleBlocksPager)
                .padding(ScyraSpacing.md)

                switch page {
                case .details:
                    details
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
        }
        .background(ScyraColors.background)
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

    private var details: some View {
        ScrollView {
            VStack(spacing: 14) {
                ScyraCard(style: .elevated) {
                    VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                        Text("Refine the moment, tag, or meaning.")
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                        TextField(
                            "What was the moment?",
                            text: Binding(get: { viewModel.title }, set: viewModel.updateTitle)
                        )
                        .font(.system(.title2, design: .rounded).weight(.semibold))
                        .padding(ScyraSpacing.md)
                        .background(ScyraColors.surface.opacity(0.5))
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                    }
                }

                ScyraCard {
                    VStack(spacing: ScyraSpacing.sm) {
                        if !viewModel.journeys.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack {
                                    ForEach(viewModel.journeys.prefix(8)) { journey in
                                        ScyraChip(
                                            journey.name,
                                            isSelected: journey.name.caseInsensitiveCompare(viewModel.journeyName) == .orderedSame
                                        ) { viewModel.selectJourney(journey) }
                                    }
                                }
                            }
                        }
                        TextField(
                            "Leave blank for untagged",
                            text: Binding(get: { viewModel.journeyName }, set: viewModel.updateJourneyName)
                        )
                        .padding(ScyraSpacing.md)
                        .background(ScyraColors.surface.opacity(0.5))
                        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card))
                    }
                }

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                HStack {
                    Button("Cancel", action: onCancel)
                        .frame(maxWidth: .infinity)
                    Button(viewModel.isSaving ? "Saving…" : "Save") {
                        if viewModel.chronicle.draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            save()
                        } else {
                            showDraftPrompt = true
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                    .frame(maxWidth: .infinity)
                    .disabled(!viewModel.canSave)
                }
            }
            .padding(ScyraSpacing.md)
        }
    }

    private func save() {
        if viewModel.save() { onSaved() }
    }
}

private enum PulseEditPage: Hashable {
    case details
    case chronicle
}
