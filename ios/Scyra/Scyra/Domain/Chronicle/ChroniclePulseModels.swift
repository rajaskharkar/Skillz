import Foundation

enum ChronicleOwnerType: String, Codable, Sendable {
    case activeFlow = "ACTIVE_FLOW"
    case session = "SESSION"
    case pulseDraft = "PULSE_DRAFT"
    case pulse = "PULSE"
}

struct ChronicleOwner: Hashable, Sendable {
    let type: ChronicleOwnerType
    let key: String

    var identity: String { "\(type.rawValue)|\(key)" }

    static func activeFlow(_ id: UUID) -> Self {
        Self(type: .activeFlow, key: id.uuidString)
    }

    static func session(_ id: UUID) -> Self {
        Self(type: .session, key: id.uuidString)
    }

    static func pulseDraft(_ id: UUID) -> Self {
        Self(type: .pulseDraft, key: id.uuidString)
    }

    static func pulse(_ id: UUID) -> Self {
        Self(type: .pulse, key: id.uuidString)
    }
}

enum ChronicleMomentType: String, Codable, Sendable {
    case text = "TEXT"
    case media = "MEDIA"
    case voice = "VOICE"
}

struct ChronicleMediaItem: Identifiable, Equatable, Sendable {
    let id: UUID
    let position: Int
    let localPath: String
    let mimeType: String
    let durationMs: Int64?
    let width: Int?
    let height: Int?
    let thumbnailPath: String?
    let createdAt: Date

    func repositioned(to position: Int) -> Self {
        Self(
            id: id,
            position: position,
            localPath: localPath,
            mimeType: mimeType,
            durationMs: durationMs,
            width: width,
            height: height,
            thumbnailPath: thumbnailPath,
            createdAt: createdAt
        )
    }
}

struct ChronicleMoment: Identifiable, Equatable, Sendable {
    let id: UUID
    let chronicleID: UUID
    let type: ChronicleMomentType
    let position: Int
    let text: String?
    let audioPath: String?
    let displayName: String?
    let mimeType: String?
    let durationMs: Int64?
    let originalTranscript: String?
    let transcript: String?
    let transcriptEdited: Bool
    let mediaItems: [ChronicleMediaItem]
    let createdAt: Date
    let updatedAt: Date

    static func text(
        id: UUID = UUID(),
        chronicleID: UUID,
        position: Int,
        text: String,
        now: Date
    ) -> Self {
        Self(
            id: id,
            chronicleID: chronicleID,
            type: .text,
            position: position,
            text: text,
            audioPath: nil,
            displayName: nil,
            mimeType: nil,
            durationMs: nil,
            originalTranscript: nil,
            transcript: nil,
            transcriptEdited: false,
            mediaItems: [],
            createdAt: now,
            updatedAt: now
        )
    }

    static func media(
        id: UUID = UUID(),
        chronicleID: UUID,
        position: Int,
        items: [ChronicleMediaItem],
        now: Date
    ) -> Self {
        Self(
            id: id,
            chronicleID: chronicleID,
            type: .media,
            position: position,
            text: nil,
            audioPath: nil,
            displayName: nil,
            mimeType: nil,
            durationMs: nil,
            originalTranscript: nil,
            transcript: nil,
            transcriptEdited: false,
            mediaItems: items,
            createdAt: now,
            updatedAt: now
        )
    }

    static func voice(
        id: UUID = UUID(),
        chronicleID: UUID,
        position: Int,
        localPath: String,
        mimeType: String,
        durationMs: Int64,
        now: Date
    ) -> Self {
        Self(
            id: id,
            chronicleID: chronicleID,
            type: .voice,
            position: position,
            text: nil,
            audioPath: localPath,
            displayName: "Voice note",
            mimeType: mimeType,
            durationMs: durationMs,
            originalTranscript: nil,
            transcript: nil,
            transcriptEdited: false,
            mediaItems: [],
            createdAt: now,
            updatedAt: now
        )
    }
}

struct ChronicleSnapshot: Equatable, Sendable {
    let chronicleID: UUID?
    let owner: ChronicleOwner
    let draftText: String
    let moments: [ChronicleMoment]
    let createdAt: Date?
    let updatedAt: Date?

    static func empty(owner: ChronicleOwner) -> Self {
        Self(
            chronicleID: nil,
            owner: owner,
            draftText: "",
            moments: [],
            createdAt: nil,
            updatedAt: nil
        )
    }

    var textMoments: [String] {
        moments
            .filter { $0.type == .text }
            .compactMap(\.text)
    }

    var excerpt: String? {
        textMoments
            .first { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .map { String($0.prefix(240)) }
    }

    var mediaItems: [ChronicleMediaItem] {
        moments
            .filter { $0.type == .media }
            .flatMap(\.mediaItems)
    }
}

enum PulseGroveStatus: String, Codable, CaseIterable, Sendable {
    case alive = "ALIVE"
    case insight = "INSIGHT"
    case completed = "COMPLETED"
}

struct Pulse: Identifiable, Equatable, Sendable {
    let id: UUID
    var title: String
    var description: String
    var journeyName: String?
    var parentSessionID: UUID?
    var parentFlowInstanceID: UUID?
    var arcID: UUID?
    let createdAt: Date
    var updatedAt: Date
    var groveStatus: PulseGroveStatus
    var groveStatusChangedAt: Date?
}

struct PulseDraft: Equatable, Sendable {
    static let singletonID = "active-pulse-draft"

    let creationKey: UUID
    var title: String
    var journeyName: String
    var attachToCurrentFlow: Bool
    let createdAt: Date
}

struct PulseFlowLink: Identifiable, Equatable, Sendable {
    let id: UUID
    let pulseID: UUID
    let sessionID: UUID
    let linkedAt: Date
}
