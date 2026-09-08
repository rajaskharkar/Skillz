import SwiftUI

struct ArcMetadataEditorView: View {
    let arcID: UUID
    let metadata: ArcMetadata
    let onSave: (ArcMetadata) -> Bool

    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var summary: String
    @State private var outcome: String
    @State private var highlight: String
    @State private var nextStep: String
    @State private var reflectionExpanded: Bool
    @State private var showDiscardConfirmation = false
    @State private var saveFailed = false

    init(arcID: UUID, metadata: ArcMetadata, onSave: @escaping (ArcMetadata) -> Bool) {
        self.arcID = arcID
        self.metadata = metadata
        self.onSave = onSave
        _title = State(initialValue: metadata.title ?? "")
        _summary = State(initialValue: metadata.summary ?? "")
        _outcome = State(initialValue: metadata.outcome ?? "")
        _highlight = State(initialValue: metadata.highlight ?? "")
        _nextStep = State(initialValue: metadata.nextStep ?? "")
        _reflectionExpanded = State(initialValue: metadata.hasReflection)
    }

    var body: some View {
        NavigationStack {
            Form {
                if saveFailed {
                    Section {
                        Text("Couldn't save Arc details. Try again.")
                            .foregroundStyle(ScyraColors.error)
                            .accessibilityLabel("Couldn't save Arc details. Try again.")
                    }
                }
                Section {
                    limitedField(
                        "Title",
                        placeholder: "Give this Arc a title",
                        text: $title,
                        limit: ArcMetadata.titleLimit,
                        axis: .vertical
                    )
                    limitedEditor(
                        "Summary",
                        placeholder: "What was this Arc about?",
                        text: $summary,
                        limit: ArcMetadata.summaryLimit,
                        minimumHeight: 100
                    )
                } footer: {
                    Text("Add context or capture what this Arc meant to you.")
                }

                Section {
                    Button(reflectionExpanded ? "Hide Arc reflection" : "Add Arc reflection") {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            reflectionExpanded.toggle()
                        }
                    }

                    if reflectionExpanded {
                        limitedEditor(
                            "Outcome",
                            placeholder: "What did you complete or move forward?",
                            text: $outcome,
                            limit: ArcMetadata.reflectionLimit,
                            minimumHeight: 88
                        )
                        limitedEditor(
                            "Highlight",
                            placeholder: "What stood out during this Arc?",
                            text: $highlight,
                            limit: ArcMetadata.reflectionLimit,
                            minimumHeight: 88
                        )
                        limitedEditor(
                            "Next step",
                            placeholder: "What would you like to continue later?",
                            text: $nextStep,
                            limit: ArcMetadata.reflectionLimit,
                            minimumHeight: 88
                        )
                    }
                }
            }
            .navigationTitle("Arc details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: requestClose)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save changes", action: save)
                        .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(isDirty)
            .confirmationDialog(
                "Discard Arc changes?",
                isPresented: $showDiscardConfirmation,
                titleVisibility: .visible
            ) {
                Button("Discard", role: .destructive) { dismiss() }
                Button("Keep editing", role: .cancel) {}
            } message: {
                Text("Your unsaved Arc details will be lost.")
            }
        }
        .presentationDetents([.large])
    }

    private var normalized: ArcMetadata {
        ArcMetadata.normalized(
            arcID: arcID,
            title: title,
            summary: summary,
            outcome: outcome,
            highlight: highlight,
            nextStep: nextStep
        )
    }

    private var isDirty: Bool { normalized != metadata }

    private var isWithinLimits: Bool {
        title.count <= ArcMetadata.titleLimit
            && summary.count <= ArcMetadata.summaryLimit
            && outcome.count <= ArcMetadata.reflectionLimit
            && highlight.count <= ArcMetadata.reflectionLimit
            && nextStep.count <= ArcMetadata.reflectionLimit
    }

    private var canSave: Bool { isDirty && isWithinLimits }

    private func requestClose() {
        if isDirty { showDiscardConfirmation = true }
        else { dismiss() }
    }

    private func save() {
        guard canSave else { return }
        guard onSave(normalized) else {
            saveFailed = true
            return
        }
        dismiss()
    }

    private func limitedField(
        _ label: String,
        placeholder: String,
        text: Binding<String>,
        limit: Int,
        axis: Axis
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            TextField(placeholder, text: text, axis: axis)
                .accessibilityLabel(label)
            characterCount(text.wrappedValue.count, limit: limit)
        }
    }

    private func limitedEditor(
        _ label: String,
        placeholder: String,
        text: Binding<String>,
        limit: Int,
        minimumHeight: CGFloat
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(ScyraTypography.label)
            TextField(placeholder, text: text, axis: .vertical)
                .lineLimit(3...8)
                .frame(minHeight: minimumHeight, alignment: .topLeading)
                .accessibilityLabel(label)
            characterCount(text.wrappedValue.count, limit: limit)
        }
    }

    private func characterCount(_ count: Int, limit: Int) -> some View {
        Text("\(count)/\(limit)")
            .font(ScyraTypography.caption)
            .foregroundStyle(count > limit ? ScyraColors.error : ScyraColors.textMuted)
            .frame(maxWidth: .infinity, alignment: .trailing)
            .accessibilityLabel("\(count) of \(limit) characters")
    }
}
