import AVFoundation
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
import UIKit

private enum ChronicleDictationTarget: Equatable {
    case draft
    case textMoment(UUID)
}

struct ChronicleTextEditorView: View {
    let snapshot: ChronicleSnapshot
    let onDraftChanged: (String) -> Void
    let onAdd: () -> Void
    let onUpdate: (UUID, String) -> Void
    let onDelete: (UUID) -> Void
    let onMove: (UUID, Int) -> Void
    var isImportingMedia = false
    var onImportMedia: ([ChronicleMediaSource]) async -> ChronicleMediaImportResult? = { _ in nil }
    var onStageMedia: ([ChronicleMediaSource]) async -> ChronicleMediaStageResult? = { _ in nil }
    var onReplaceMedia: (UUID, [ChronicleMediaItem]) -> Bool = { _, _ in false }
    var onAddVoice: (ChronicleStagedVoice) async -> Bool = { _ in false }
    var onUpdateTranscript: (UUID, String?, Bool) -> Bool = { _, _, _ in false }
    var onEditingChanged: (Bool) -> Void = { _ in }

    @Environment(\.chronicleFileStore) private var fileStore
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var capture = ChronicleAudioCaptureController()
    @StateObject private var speech = ChronicleSpeechController()
    @StateObject private var playback = ChronicleAudioPlaybackController()
    @State private var editingID: UUID?
    @State private var editingText = ""
    @State private var editingTranscriptID: UUID?
    @State private var editingTranscriptText = ""
    @State private var editingMediaID: UUID?
    @State private var editingMediaItems: [ChronicleMediaItem] = []
    @State private var editingMediaStaged: [ChronicleMediaItem] = []
    @State private var composerText = ""
    @State private var pendingDeleteID: UUID?
    @State private var selectedMedia: [PhotosPickerItem] = []
    @State private var isLoadingSelection = false
    @State private var cameraMode: ChronicleCameraMode?
    @State private var cameraRequestID = UUID()
    @State private var mediaMessage: String?
    @State private var isStartingVoice = false
    @State private var isStartingDictation = false
    @State private var isCommittingVoice = false
    @State private var dictationTarget: ChronicleDictationTarget?
    @State private var dictationSession: ChronicleDictationTextSession?
    @State private var transcriptions: [UUID: ChronicleTranscriptionViewState] = [:]

    var body: some View {
        ScrollView {
            LazyVStack(spacing: ScyraSpacing.lg) {
                if snapshot.moments.isEmpty {
                    VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                        Text(ChronicleStrings.emptyTitle)
                            .font(ScyraTypography.screenTitle)
                        Text(ChronicleStrings.emptyBody)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                ForEach(Array(snapshot.moments.enumerated()), id: \.element.id) { index, moment in
                    momentView(moment, at: index)
                    if index < snapshot.moments.endIndex - 1 { Divider() }
                }

                composer
            }
            .padding(.horizontal, ScyraSpacing.lg)
            .padding(.vertical, ScyraSpacing.lg)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(
            LinearGradient(
                colors: [ScyraColors.background, ScyraColors.backgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .alert(ChronicleStrings.removeTitle, isPresented: deleteAlertBinding) {
            Button(ChronicleStrings.cancel, role: .cancel) { pendingDeleteID = nil }
            Button(ChronicleStrings.remove, role: .destructive) {
                if let pendingDeleteID {
                    deleteMoment(pendingDeleteID)
                    if editingID == pendingDeleteID { finishEditingWithoutSaving() }
                }
                self.pendingDeleteID = nil
            }
        }
        .onAppear {
            if composerText != snapshot.draftText { composerText = snapshot.draftText }
        }
        .onChange(of: snapshot.draftText) { _, newValue in
            if composerText != newValue { composerText = newValue }
        }
        .onChange(of: selectedMedia) { _, items in
            guard !items.isEmpty else { return }
            Task { await importPickerItems(items) }
        }
        .onChange(of: isMediaBusy) { _, _ in notifyEditingState() }
        .onChange(of: capture.isRecording) { _, _ in notifyEditingState() }
        .onChange(of: speech.isDictating) { _, _ in notifyEditingState() }
        .onChange(of: isCommittingVoice) { _, _ in notifyEditingState() }
        .onChange(of: scenePhase) { _, phase in
            guard phase != .active else { return }
            cameraRequestID = UUID()
            cameraMode = nil
            playback.pause()
            cancelDictation()
            Task { await capture.discard() }
        }
        .onDisappear {
            discardStagedMedia()
            playback.stop()
            cancelDictation()
            speech.cancelAll()
            Task { await capture.discard() }
            onEditingChanged(false)
        }
        .fullScreenCover(item: $cameraMode) { mode in
            ChronicleCameraCaptureView(
                mode: mode,
                onCapture: { source in
                    cameraMode = nil
                    Task { await importSources([source], transferFailureCount: 0) }
                },
                onCancel: { cameraMode = nil }
            )
            .ignoresSafeArea()
        }
    }

    @ViewBuilder
    private func momentView(_ moment: ChronicleMoment, at index: Int) -> some View {
        if editingTranscriptID == moment.id, moment.type == .voice {
            transcriptEditor(moment)
        } else if editingMediaID == moment.id, moment.type == .media {
            mediaEditor(moment)
        } else if editingID == moment.id, moment.type == .text {
            ScyraCard(style: .elevated) {
                VStack(spacing: ScyraSpacing.sm) {
                    TextEditor(text: $editingText)
                        .font(ScyraTypography.body)
                        .frame(minHeight: 110)
                        .scrollContentBackground(.hidden)
                        .accessibilityLabel(ChronicleStrings.edit)
                        .disabled(speech.isDictating)

                    HStack {
                        Button(ChronicleStrings.remove, role: .destructive) {
                            pendingDeleteID = moment.id
                        }
                        Spacer()
                        Button(ChronicleStrings.cancel, action: finishEditingWithoutSaving)
                        Button(ChronicleStrings.done) {
                            guard !editingText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                            onUpdate(moment.id, editingText)
                            finishEditingWithoutSaving()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(ScyraColors.primary)
                        .disabled(editingText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                    .disabled(speech.isDictating)
                }
            }
        } else {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                HStack {
                    Spacer()
                    Menu {
                        if moment.type == .text {
                            Button(ChronicleStrings.edit, systemImage: "square.and.pencil") {
                                editingID = moment.id
                                editingText = moment.text ?? ""
                                notifyEditingState()
                            }
                        }
                        if moment.type == .media {
                            Button(ChronicleStrings.editMedia, systemImage: "photo.stack") {
                                beginMediaEdit(moment)
                            }
                        }
                        if moment.type == .voice, moment.transcript != nil {
                            Button(ChronicleStrings.editTranscript, systemImage: "text.cursor") {
                                beginTranscriptEdit(moment)
                            }
                        }
                        Button(ChronicleStrings.moveUp, systemImage: "arrow.up") {
                            onMove(moment.id, -1)
                        }
                        .disabled(index == 0)
                        Button(ChronicleStrings.moveDown, systemImage: "arrow.down") {
                            onMove(moment.id, 1)
                        }
                        .disabled(index == snapshot.moments.endIndex - 1)
                        Button(ChronicleStrings.remove, systemImage: "trash", role: .destructive) {
                            pendingDeleteID = moment.id
                        }
                    } label: {
                        ScyraCanonicalIcon(systemName: "ellipsis.circle")
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("Moment actions")
                }

                switch moment.type {
                case .text:
                    Text(moment.text ?? "")
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .onTapGesture {
                            editingID = moment.id
                            editingText = moment.text ?? ""
                            notifyEditingState()
                        }
                case .media:
                    ChronicleMediaMomentView(items: moment.mediaItems)
                case .voice:
                    ChronicleAudioMomentView(
                        moment: moment,
                        playback: playback,
                        transcriptionState: transcriptions[moment.id],
                        showTranscriptionControls: true,
                        transcriptionSupported: speech.transcriptionSupported,
                        onTranscribe: { startTranscription(moment) },
                        onCancelTranscription: { cancelTranscription(moment.id) },
                        onEditTranscript: { beginTranscriptEdit(moment) }
                    )
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityAction(named: ChronicleStrings.moveUp) { onMove(moment.id, -1) }
            .accessibilityAction(named: ChronicleStrings.moveDown) { onMove(moment.id, 1) }
            .accessibilityAction(named: ChronicleStrings.remove) { pendingDeleteID = moment.id }
        }
    }

    private func transcriptEditor(_ moment: ChronicleMoment) -> some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                Text(ChronicleStrings.editTranscript)
                    .font(ScyraTypography.cardTitle)
                TextEditor(text: $editingTranscriptText)
                    .font(ScyraTypography.body)
                    .frame(minHeight: 110)
                    .scrollContentBackground(.hidden)
                    .accessibilityLabel(ChronicleStrings.editTranscript)
                HStack {
                    Spacer()
                    Button(ChronicleStrings.cancel) {
                        editingTranscriptID = nil
                        editingTranscriptText = ""
                        notifyEditingState()
                    }
                    Button(ChronicleStrings.done) {
                        let trimmed = editingTranscriptText.trimmingCharacters(in: .whitespacesAndNewlines)
                        if onUpdateTranscript(moment.id, trimmed.isEmpty ? nil : trimmed, true) {
                            editingTranscriptID = nil
                            editingTranscriptText = ""
                            notifyEditingState()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                }
            }
        }
    }

    private func mediaEditor(_ moment: ChronicleMoment) -> some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                Text(ChronicleStrings.editingMedia)
                    .font(ScyraTypography.cardTitle)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: ScyraSpacing.sm) {
                        ForEach(Array(editingMediaItems.enumerated()), id: \.element.id) { index, item in
                            ZStack(alignment: .topTrailing) {
                                ChronicleMediaThumbnail(item: item)
                                    .frame(width: 176, height: 176)
                                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                                Button(role: .destructive) {
                                    removeEditingMedia(item.id)
                                } label: {
                                    ScyraCanonicalIcon(systemName: "trash")
                                        .frame(width: 40, height: 40)
                                        .background(.regularMaterial, in: Circle())
                                }
                                .padding(6)
                                .disabled(isMediaBusy)
                                .accessibilityLabel(
                                    item.mimeType.hasPrefix("video/")
                                        ? ChronicleStrings.removeVideo
                                        : ChronicleStrings.removePhoto
                                )

                                HStack(spacing: 2) {
                                    Button {
                                        moveEditingMedia(item.id, offset: -1)
                                    } label: {
                                        ScyraCanonicalIcon(systemName: "chevron.left")
                                            .frame(width: 38, height: 38)
                                    }
                                    .disabled(isMediaBusy || index == 0)
                                    .accessibilityLabel(ChronicleStrings.moveMediaEarlier)

                                    Text(ChronicleStrings.mediaPosition(index + 1, editingMediaItems.count))
                                        .font(ScyraTypography.caption)
                                        .monospacedDigit()

                                    Button {
                                        moveEditingMedia(item.id, offset: 1)
                                    } label: {
                                        ScyraCanonicalIcon(systemName: "chevron.right")
                                            .frame(width: 38, height: 38)
                                    }
                                    .disabled(isMediaBusy || index == editingMediaItems.endIndex - 1)
                                    .accessibilityLabel(ChronicleStrings.moveMediaLater)
                                }
                                .padding(.horizontal, 2)
                                .background(.regularMaterial, in: Capsule())
                                .padding(6)
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                            }
                            .frame(width: 176, height: 176)
                        }
                    }
                }

                Text(ChronicleStrings.mediaReorderHint)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                Text(ChronicleStrings.mediaEditCaptureHint)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)

                HStack {
                    Spacer()
                    Button(ChronicleStrings.cancel, action: cancelMediaEdit)
                        .disabled(isMediaBusy)
                    Button(ChronicleStrings.done) {
                        finishMediaEdit(momentID: moment.id)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                    .disabled(isMediaBusy || editingMediaItems.isEmpty)
                }
            }
        }
    }

    private var composer: some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                ZStack(alignment: .topLeading) {
                    if composerText.isEmpty {
                        Text(ChronicleStrings.writePlaceholder)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textMuted)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 8)
                            .allowsHitTesting(false)
                    }
                    TextEditor(text: $composerText)
                    .font(ScyraTypography.body)
                    .frame(minHeight: 112)
                    .scrollContentBackground(.hidden)
                    .disabled(hasActiveEditor || isMediaBusy || capture.isRecording || speech.isDictating)
                    .accessibilityLabel(ChronicleStrings.writePlaceholder)
                    .accessibilityIdentifier("chronicle-draft-field")
                    .onChange(of: composerText) { _, newValue in
                        let isProvisionalDictation = speech.isDictating && dictationTarget == .draft
                        if !isProvisionalDictation, snapshot.draftText != newValue { onDraftChanged(newValue) }
                    }
                }

                HStack(spacing: ScyraSpacing.md) {
                    PhotosPicker(
                        selection: $selectedMedia,
                        maxSelectionCount: 10,
                        matching: .any(of: [.images, .videos])
                    ) {
                        ScyraCanonicalIcon(systemName: "photo.on.rectangle")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!captureEnabled)
                    .accessibilityLabel(ChronicleStrings.gallery)

                    Button {
                        openCamera(.photo)
                    } label: {
                        ScyraCanonicalIcon(systemName: "camera")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!captureEnabled)
                    .accessibilityLabel(ChronicleStrings.camera)

                    Button {
                        openCamera(.video)
                    } label: {
                        ScyraCanonicalIcon(systemName: "video")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!captureEnabled)
                    .accessibilityLabel(ChronicleStrings.video)

                    Button {
                        startDictation()
                    } label: {
                        ScyraCanonicalIcon(systemName: "text.bubble")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!micEnabled || isStartingDictation)
                    .accessibilityLabel(ChronicleStrings.dictate)

                    Button {
                        startVoice()
                    } label: {
                        ScyraCanonicalIcon(systemName: "mic")
                            .frame(width: 44, height: 44)
                    }
                    .disabled(!micEnabled || isStartingVoice)
                    .accessibilityLabel(ChronicleStrings.recordVoice)

                    Spacer()

                    if isMediaBusy {
                        ProgressView()
                            .accessibilityLabel(ChronicleStrings.mediaImporting)
                    }
                }

                if capture.isRecording {
                    ChronicleRecordingStatusView(
                        capture: capture,
                        onDiscard: { Task { await capture.discard() } },
                        onFinish: finishVoice
                    )
                } else if speech.isDictating {
                    HStack {
                        Text(ChronicleStrings.listening)
                            .font(ScyraTypography.body)
                        Spacer()
                        Button(ChronicleStrings.cancel, action: cancelDictation)
                        Button(ChronicleStrings.finishDictation) { speech.finishDictation() }
                            .buttonStyle(.borderedProminent)
                            .tint(ScyraColors.primary)
                    }
                }

                if let mediaMessage {
                    Text(mediaMessage)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                if speech.unavailable {
                    Text(ChronicleStrings.speechUnavailable)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
                if capture.microphoneDenied || speech.microphoneDenied {
                    Text(ChronicleStrings.microphoneUnavailable)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
                if capture.failed {
                    Text(ChronicleStrings.saveError)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                }

                HStack {
                    Spacer()
                    Button(ChronicleStrings.add, action: onAdd)
                        .buttonStyle(.borderedProminent)
                        .tint(ScyraColors.primary)
                        .accessibilityIdentifier("chronicle-add-moment")
                        .disabled(
                            composerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                || hasActiveEditor
                                || isMediaBusy
                                || capture.isRecording
                                || speech.isDictating
                                || isCommittingVoice
                        )
                }
            }
        }
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { pendingDeleteID != nil },
            set: { if !$0 { pendingDeleteID = nil } }
        )
    }

    private func finishEditingWithoutSaving() {
        editingID = nil
        editingText = ""
        notifyEditingState()
    }

    private var isMediaBusy: Bool { isLoadingSelection || isImportingMedia }
    private var hasActiveEditor: Bool {
        editingID != nil || editingMediaID != nil || editingTranscriptID != nil
    }
    private var captureEnabled: Bool {
        editingID == nil
            && editingTranscriptID == nil
            && !isMediaBusy
            && !capture.isRecording
            && !speech.isDictating
            && !isCommittingVoice
            && !isStartingVoice
            && !isStartingDictation
    }
    private var micEnabled: Bool {
        !isMediaBusy
            && editingMediaID == nil
            && editingTranscriptID == nil
            && !capture.isRecording
            && !speech.isDictating
            && !isCommittingVoice
            && !isStartingVoice
            && !isStartingDictation
    }

    private func notifyEditingState() {
        onEditingChanged(
            hasActiveEditor
                || isMediaBusy
                || capture.isRecording
                || speech.isDictating
                || isCommittingVoice
                || isStartingVoice
                || isStartingDictation
        )
    }

    private func beginMediaEdit(_ moment: ChronicleMoment) {
        guard !hasActiveEditor, !isMediaBusy, moment.type == .media else { return }
        editingMediaID = moment.id
        editingMediaItems = moment.mediaItems.enumerated().map { $1.repositioned(to: $0) }
        editingMediaStaged = []
        notifyEditingState()
    }

    private func moveEditingMedia(_ id: UUID, offset: Int) {
        guard let source = editingMediaItems.firstIndex(where: { $0.id == id }) else { return }
        let destination = source + offset
        guard editingMediaItems.indices.contains(destination) else { return }
        editingMediaItems.swapAt(source, destination)
        editingMediaItems = editingMediaItems.enumerated().map { $1.repositioned(to: $0) }
    }

    private func removeEditingMedia(_ id: UUID) {
        guard editingMediaItems.count > 1 else {
            pendingDeleteID = editingMediaID
            return
        }
        editingMediaItems.removeAll { $0.id == id }
        editingMediaItems = editingMediaItems.enumerated().map { $1.repositioned(to: $0) }
    }

    private func cancelMediaEdit() {
        discardStagedMedia()
        resetMediaEdit()
        notifyEditingState()
    }

    private func finishMediaEdit(momentID: UUID) {
        let normalized = editingMediaItems.enumerated().map { $1.repositioned(to: $0) }
        guard !normalized.isEmpty, onReplaceMedia(momentID, normalized) else { return }
        let retainedIDs = Set(normalized.map(\.id))
        editingMediaStaged
            .filter { !retainedIDs.contains($0.id) }
            .forEach { item in
                fileStore.deleteIfOwned(item.localPath)
                item.thumbnailPath.map(fileStore.deleteIfOwned)
            }
        resetMediaEdit()
        notifyEditingState()
    }

    private func deleteMoment(_ id: UUID) {
        cancelTranscription(id)
        if editingTranscriptID == id {
            editingTranscriptID = nil
            editingTranscriptText = ""
        }
        if editingMediaID == id {
            discardStagedMedia()
            resetMediaEdit()
        }
        onDelete(id)
        notifyEditingState()
    }

    private func startVoice() {
        guard micEnabled, !isStartingVoice else { return }
        playback.stop()
        isStartingVoice = true
        Task {
            await capture.start(fileStore: fileStore)
            isStartingVoice = false
            notifyEditingState()
        }
    }

    private func finishVoice() {
        guard capture.isRecording, !isCommittingVoice else { return }
        Task {
            guard let staged = await capture.finish() else { return }
            isCommittingVoice = true
            let saved = await onAddVoice(staged)
            if !saved { await fileStore.discardVoiceStaging(staged) }
            isCommittingVoice = false
            notifyEditingState()
        }
    }

    private func startDictation() {
        guard micEnabled, !isStartingDictation else { return }
        playback.stop()
        let target: ChronicleDictationTarget
        let original: String
        if let editingID {
            target = .textMoment(editingID)
            original = editingText
        } else {
            target = .draft
            original = composerText
        }
        dictationTarget = target
        dictationSession = ChronicleDictationTextSession(original: original)
        isStartingDictation = true
        Task {
            await speech.startDictation(
                onPartial: applyDictation,
                onCompletion: completeDictation
            )
            isStartingDictation = false
            notifyEditingState()
        }
    }

    private func applyDictation(_ hypothesis: String) {
        guard var session = dictationSession, let target = dictationTarget else { return }
        let value = session.partial(hypothesis)
        dictationSession = session
        switch target {
        case .draft:
            composerText = value
        case .textMoment(let id) where id == editingID:
            editingText = value
        case .textMoment:
            break
        }
    }

    private func completeDictation(_ result: Result<String, Error>) {
        guard let target = dictationTarget, let session = dictationSession else { return }
        switch result {
        case .success(let text):
            applyDictation(text)
            if target == .draft { onDraftChanged(dictationSession?.current ?? session.current) }
        case .failure:
            switch target {
            case .draft:
                composerText = session.original
                onDraftChanged(session.original)
            case .textMoment(let id) where id == editingID:
                editingText = session.original
            case .textMoment:
                break
            }
        }
        dictationTarget = nil
        dictationSession = nil
        notifyEditingState()
    }

    private func cancelDictation() {
        guard let target = dictationTarget, let session = dictationSession else {
            if speech.isDictating { speech.cancelDictation() }
            return
        }
        dictationTarget = nil
        dictationSession = nil
        switch target {
        case .draft:
            composerText = session.original
            onDraftChanged(session.original)
        case .textMoment(let id) where id == editingID:
            editingText = session.original
        case .textMoment:
            break
        }
        speech.cancelDictation()
        notifyEditingState()
    }

    private func startTranscription(_ moment: ChronicleMoment) {
        guard transcriptions[moment.id] == nil,
              let path = moment.audioPath,
              let url = fileStore.resolve(path),
              FileManager.default.fileExists(atPath: url.path) else { return }
        playback.stop()
        transcriptions[moment.id] = .transcribing("")
        Task {
            await speech.startTranscription(
                id: moment.id,
                url: url,
                onPartial: { transcriptions[moment.id] = .transcribing($0) },
                onCompletion: { result in
                    switch result {
                    case .success(let transcript):
                        transcriptions[moment.id] = onUpdateTranscript(moment.id, transcript, false)
                            ? nil
                            : .failed
                    case .failure:
                        transcriptions[moment.id] = .failed
                    }
                }
            )
        }
    }

    private func cancelTranscription(_ id: UUID) {
        speech.cancelTranscription(id: id)
        transcriptions[id] = nil
    }

    private func beginTranscriptEdit(_ moment: ChronicleMoment) {
        guard !hasActiveEditor, !capture.isRecording, !speech.isDictating else { return }
        let editable = moment.transcriptEdited ? moment.transcript : (moment.originalTranscript ?? moment.transcript)
        guard let editable else { return }
        cancelTranscription(moment.id)
        editingTranscriptID = moment.id
        editingTranscriptText = editable
        notifyEditingState()
    }

    private func discardStagedMedia() {
        editingMediaStaged.forEach { item in
            fileStore.deleteIfOwned(item.localPath)
            item.thumbnailPath.map(fileStore.deleteIfOwned)
        }
        editingMediaStaged = []
    }

    private func resetMediaEdit() {
        editingMediaID = nil
        editingMediaItems = []
        editingMediaStaged = []
    }

    private func importPickerItems(_ items: [PhotosPickerItem]) async {
        isLoadingSelection = true
        mediaMessage = nil
        var sources: [ChronicleMediaSource] = []
        var transferFailures = 0
        for item in items {
            let contentType = item.supportedContentTypes.first {
                $0.conforms(to: .image) || $0.conforms(to: .movie)
            }
            do {
                guard let contentType,
                      let transfer = try await item.loadTransferable(type: ChronicleMediaTransfer.self) else {
                    transferFailures += 1
                    continue
                }
                sources.append(ChronicleMediaSource(
                    url: transfer.url,
                    contentTypeIdentifier: contentType.identifier,
                    deleteWhenFinished: true
                ))
            } catch {
                transferFailures += 1
            }
        }
        selectedMedia = []
        await importSources(sources, transferFailureCount: transferFailures)
        isLoadingSelection = false
    }

    private func importSources(
        _ sources: [ChronicleMediaSource],
        transferFailureCount: Int
    ) async {
        guard !sources.isEmpty else {
            if transferFailureCount > 0 { mediaMessage = ChronicleStrings.mediaFailed(transferFailureCount) }
            return
        }
        let failedCount: Int
        if let editingMediaID {
            let result = await onStageMedia(sources)
            if self.editingMediaID == editingMediaID, let result {
                let additions = result.items.enumerated().map {
                    $1.repositioned(to: editingMediaItems.count + $0)
                }
                editingMediaItems.append(contentsOf: additions)
                editingMediaStaged.append(contentsOf: result.items)
            } else {
                result?.items.forEach { item in
                    fileStore.deleteIfOwned(item.localPath)
                    item.thumbnailPath.map(fileStore.deleteIfOwned)
                }
            }
            failedCount = transferFailureCount + (result?.failedCount ?? sources.count)
        } else {
            let result = await onImportMedia(sources)
            failedCount = transferFailureCount + (result?.failedCount ?? sources.count)
        }
        mediaMessage = failedCount > 0 ? ChronicleStrings.mediaFailed(failedCount) : nil
    }

    private func openCamera(_ mode: ChronicleCameraMode) {
        mediaMessage = nil
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            mediaMessage = ChronicleStrings.cameraUnavailable
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            if scenePhase == .active { cameraMode = mode }
        case .notDetermined:
            let requestID = UUID()
            cameraRequestID = requestID
            Task {
                let granted = await AVCaptureDevice.requestAccess(for: .video)
                guard cameraRequestID == requestID, scenePhase == .active else { return }
                if granted {
                    cameraMode = mode
                } else {
                    mediaMessage = ChronicleStrings.cameraPermissionDenied
                }
            }
        case .denied, .restricted:
            mediaMessage = ChronicleStrings.cameraPermissionDenied
        @unknown default:
            mediaMessage = ChronicleStrings.cameraPermissionDenied
        }
    }
}
