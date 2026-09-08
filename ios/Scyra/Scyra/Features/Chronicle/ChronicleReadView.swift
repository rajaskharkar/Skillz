import SwiftUI

struct ChronicleReadView: View {
    let snapshot: ChronicleSnapshot
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var playback = ChronicleAudioPlaybackController()

    var body: some View {
        Group {
            if snapshot.moments.isEmpty {
                ScyraCard {
                    VStack(alignment: .leading, spacing: ScyraSpacing.xs) {
                        Text(ChronicleStrings.emptyTitle).font(ScyraTypography.cardTitle)
                        Text(ChronicleStrings.emptyBody)
                            .font(ScyraTypography.body)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                ScyraCard(style: .elevated) {
                    VStack(alignment: .leading, spacing: ScyraSpacing.md) {
                        ScyraCanonicalLabel(ChronicleStrings.title, systemImage: "book.pages")
                            .font(ScyraTypography.cardTitle)
                            .foregroundStyle(ScyraColors.primary)
                        ForEach(Array(snapshot.moments.enumerated()), id: \.element.id) { index, moment in
                            if index > 0 { Divider() }
                            switch moment.type {
                            case .text:
                                Text(moment.text ?? "")
                                    .font(ScyraTypography.body)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            case .media:
                                ChronicleMediaMomentView(items: moment.mediaItems)
                            case .voice:
                                ChronicleAudioMomentView(moment: moment, playback: playback)
                            }
                        }
                    }
                }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase != .active { playback.pause() }
        }
        .onDisappear { playback.stop() }
    }
}
