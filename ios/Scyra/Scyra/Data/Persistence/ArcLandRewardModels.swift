import Foundation
import SwiftData

@Model
final class ArcLandRewardModel {
    @Attribute(.unique) var arcID: UUID
    var flowCount: Int
    var lastFlowEndTime: Date
    var finalSessionID: UUID
    var finalizedAt: Date?

    init(journal: ArcLandRewardJournal) {
        arcID = journal.arcID
        flowCount = journal.flowCount
        lastFlowEndTime = journal.lastFlowEndTime
        finalSessionID = journal.finalSessionID
        finalizedAt = journal.finalizedAt
    }

    var journal: ArcLandRewardJournal {
        .init(arcID: arcID, flowCount: flowCount, lastFlowEndTime: lastFlowEndTime, finalSessionID: finalSessionID, finalizedAt: finalizedAt)
    }
}

/// Durable reservation spans pause, backgrounding, termination and asynchronous
/// Health reads at completion. MainActor repository transactions serialize it
/// with reward finalization; expiry never races an in-flight Arc Flow.
@Model
final class ArcLandFlowReservationModel {
    @Attribute(.unique) var id: String
    var arcID: UUID
    var flowInstanceID: UUID
    init(arcID: UUID, flowInstanceID: UUID) {
        id = "arc-flow-reservation"
        self.arcID = arcID
        self.flowInstanceID = flowInstanceID
    }
}
