import SwiftUI

enum ChronicleTranscriptionViewState: Equatable {
    case transcribing(String)
    case failed
}

struct ChronicleAudioMomentView: View {
    let moment: ChronicleMoment
    @ObservedObject var playback: ChronicleAudioPlaybackController
    var transcriptionState: ChronicleTranscriptionViewState?
    var showTranscriptionControls = false
    var transcriptionSupported = false
    var onTranscribe: () -> Void = {}
    var onCancelTranscription: () -> Void = {}
    var onEditTranscript: () -> Void = {}

    @Environment(\.chronicleFileStore) private var fileStore

    private var durationMs: Int64 { max(0, moment.durationMs ?? 0) }
    private var resolvedURL: URL? { moment.audioPath.flatMap(fileStore.resolve) }
    private var isAvailable: Bool {
        guard let resolvedURL else { return false }
        return FileManager.default.fileExists(atPath: resolvedURL.path)
    }
    private var isActive: Bool { playback.activeSourceID == moment.id }
    private var resolvedPosition: Int64 { isActive ? min(playback.positionMs, resolvedDuration) : 0 }
    private var resolvedDuration: Int64 {
        isActive && playback.durationMs > 0 ? playback.durationMs : durationMs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(ChronicleStrings.voiceNote)
                .font(ScyraTypography.cardTitle)
                .foregroundStyle(ScyraColors.textPrimary)

            if isAvailable, let resolvedURL {
                HStack {
                    Button {
                        playback.toggle(
                            sourceID: moment.id,
                            url: resolvedURL,
                            fallbackDurationMs: durationMs
                        )
                    } label: {
                        ScyraCanonicalIcon(systemName: isActive && playback.isPlaying ? "pause.fill" : "play.fill")
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel(
                        isActive && playback.isPlaying ? ChronicleStrings.pause : ChronicleStrings.play
                    )

                    Slider(
                        value: Binding(
                            get: { Double(resolvedPosition) },
                            set: {
                                playback.seek(
                                    sourceID: moment.id,
                                    url: resolvedURL,
                                    fallbackDurationMs: durationMs,
                                    to: Int64($0)
                                )
                            }
                        ),
                        in: 0...Double(max(1, resolvedDuration))
                    )
                }
                Text("\(formatDuration(resolvedPosition)) / \(formatDuration(resolvedDuration))")
                    .font(ScyraTypography.caption)
                    .monospacedDigit()
                    .foregroundStyle(ScyraColors.textSecondary)
                if isActive && playback.hasError {
                    Text(ChronicleStrings.playbackFailed)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textSecondary)
                }
            } else {
                Text(ChronicleStrings.voiceUnavailable)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textSecondary)
            }

            Text(ChronicleStrings.originalTranscript)
                .font(ScyraTypography.label)
                .foregroundStyle(ScyraColors.textPrimary)
            if let originalDisplay {
                Text(originalDisplay)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textPrimary)
            }

            switch transcriptionState {
            case .transcribing:
                Text(ChronicleStrings.transcribing)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textPrimary)
                Button(ChronicleStrings.cancel, action: onCancelTranscription)
            case .failed:
                Text(ChronicleStrings.transcriptionFailed)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textPrimary)
                Button(ChronicleStrings.retry, action: onTranscribe)
            case nil:
                if showTranscriptionControls, moment.originalTranscript == nil {
                    if transcriptionSupported {
                        Button(ChronicleStrings.transcribe, action: onTranscribe)
                    } else {
                        Text(ChronicleStrings.transcriptionUnavailable)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                }
            }

            if moment.transcriptEdited {
                Text(ChronicleStrings.editedTranscript)
                    .font(ScyraTypography.label)
                    .foregroundStyle(ScyraColors.textPrimary)
                if let transcript = moment.transcript {
                    Text(transcript)
                        .font(ScyraTypography.body)
                        .foregroundStyle(ScyraColors.textPrimary)
                }
                if showTranscriptionControls {
                    Button(ChronicleStrings.editTranscript, action: onEditTranscript)
                }
            } else if moment.originalTranscript != nil, showTranscriptionControls {
                Button(ChronicleStrings.editTranscript, action: onEditTranscript)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var originalDisplay: String? {
        if case .transcribing(let partial) = transcriptionState, !partial.isEmpty { return partial }
        return moment.originalTranscript
    }
}

struct ChronicleRecordingStatusView: View {
    @ObservedObject var capture: ChronicleAudioCaptureController
    let onDiscard: () -> Void
    let onFinish: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
            Text("\(ChronicleStrings.recording) · \(formatDuration(capture.elapsedMs))")
                .font(ScyraTypography.body)
                .monospacedDigit()

            HStack(alignment: .bottom, spacing: 2) {
                ForEach(Array(capture.amplitudes.enumerated()), id: \.offset) { _, amplitude in
                    Capsule()
                        .fill(ScyraColors.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 4 + amplitude * 28)
                }
            }
            .frame(height: 32, alignment: .bottom)

            HStack {
                Spacer()
                Button(ChronicleStrings.discardRecording, action: onDiscard)
                    .disabled(capture.isFinishing)
                Button(ChronicleStrings.finishRecording, action: onFinish)
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.primary)
                    .disabled(capture.isFinishing)
            }
        }
    }
}

func formatDuration(_ milliseconds: Int64) -> String {
    let seconds = max(0, milliseconds) / 1_000
    return String(format: "%d:%02d", seconds / 60, seconds % 60)
}
