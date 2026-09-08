import Foundation
import SwiftData

@Model
final class JourneyModel {
    @Attribute(.unique) var id: UUID
    @Attribute(.unique) var name: String
    var createdAt: Date

    @Relationship(deleteRule: .cascade, inverse: \FlowSessionModel.journey)
    var flows: [FlowSessionModel]

    @Relationship(deleteRule: .nullify, inverse: \PulseModel.journey)
    var pulses: [PulseModel]

    init(id: UUID = UUID(), name: String, createdAt: Date) {
        self.id = id
        self.name = name
        self.createdAt = createdAt
        self.flows = []
        self.pulses = []
    }
}

@Model
final class FlowSessionModel {
    @Attribute(.unique) var id: UUID
    @Attribute(.unique) var flowInstanceID: UUID
    var title: String
    var flowDescription: String
    var journeyNameSnapshot: String
    var startTime: Date
    var endTime: Date
    var durationMs: Int64
    var surgePlannedMs: Int64?
    var surgePoints: Int
    var scyraPoints: Int
    var isSoftMode: Bool
    var arcID: UUID?
    var arcIndex: Int?
    var arcMultiplierUsed: Double?
    var arcBonusPoints: Int
    var createdAt: Date
    var journey: JourneyModel?

    init(session: FlowSession, journey: JourneyModel) {
        self.id = session.id
        self.flowInstanceID = session.flowInstanceID
        self.title = session.title
        self.flowDescription = session.description
        self.journeyNameSnapshot = session.journeyName
        self.startTime = session.startTime
        self.endTime = session.endTime
        self.durationMs = session.durationMs
        self.surgePlannedMs = session.surgePlannedMs
        self.surgePoints = session.surgePoints
        self.scyraPoints = session.scyraPoints
        self.isSoftMode = session.isSoftMode
        self.arcID = session.arcID
        self.arcIndex = session.arcIndex
        self.arcMultiplierUsed = session.arcMultiplierUsed
        self.arcBonusPoints = session.arcBonusPoints
        self.createdAt = session.createdAt
        self.journey = journey
    }
}

@Model
final class ActiveFlowModel {
    @Attribute(.unique) var id: String
    @Attribute(.unique) var flowInstanceID: UUID
    var title: String
    var journeyName: String
    var modeRawValue: String
    var isInFlowMode: Bool
    var isRunning: Bool
    var accumulatedDurationMs: Int64
    var segmentStartedAt: Date?
    var firstStartedAt: Date?
    var surgePlannedMs: Int64?
    var healthEnabledAtStart: Bool = false
    var healthAccessRequestedAtStart: Bool = false
    var movementBonusEligibleAtStart: Bool = false
    var originPulseID: UUID?
    var originPulseTitleSnapshot: String?
    var originPulseJourneyNameSnapshot: String?
    var activeIntervalsData: Data?
    var createdAt: Date

    init(snapshot: ActiveFlowSnapshot) {
        self.id = ActiveFlowSnapshot.singletonID
        self.flowInstanceID = snapshot.flowInstanceID
        self.title = snapshot.title
        self.journeyName = snapshot.journeyName
        self.modeRawValue = snapshot.mode.rawValue
        self.isInFlowMode = snapshot.isInFlowMode
        self.isRunning = snapshot.isRunning
        self.accumulatedDurationMs = snapshot.accumulatedDurationMs
        self.segmentStartedAt = snapshot.segmentStartedAt
        self.firstStartedAt = snapshot.firstStartedAt
        self.surgePlannedMs = snapshot.surgePlannedMs
        self.healthEnabledAtStart = snapshot.healthEnabledAtStart
        self.healthAccessRequestedAtStart = snapshot.healthAccessRequestedAtStart
        self.movementBonusEligibleAtStart = snapshot.movementBonusEligibleAtStart
        self.originPulseID = snapshot.originPulseID
        self.originPulseTitleSnapshot = snapshot.originPulseTitle
        self.originPulseJourneyNameSnapshot = snapshot.originPulseJourneyName
        self.activeIntervalsData = try? JSONEncoder().encode(snapshot.activeIntervals)
        self.createdAt = snapshot.createdAt
    }

    func update(from snapshot: ActiveFlowSnapshot) {
        flowInstanceID = snapshot.flowInstanceID
        title = snapshot.title
        journeyName = snapshot.journeyName
        modeRawValue = snapshot.mode.rawValue
        isInFlowMode = snapshot.isInFlowMode
        isRunning = snapshot.isRunning
        accumulatedDurationMs = snapshot.accumulatedDurationMs
        segmentStartedAt = snapshot.segmentStartedAt
        firstStartedAt = snapshot.firstStartedAt
        surgePlannedMs = snapshot.surgePlannedMs
        healthEnabledAtStart = snapshot.healthEnabledAtStart
        healthAccessRequestedAtStart = snapshot.healthAccessRequestedAtStart
        movementBonusEligibleAtStart = snapshot.movementBonusEligibleAtStart
        originPulseID = snapshot.originPulseID
        originPulseTitleSnapshot = snapshot.originPulseTitle
        originPulseJourneyNameSnapshot = snapshot.originPulseJourneyName
        activeIntervalsData = try? JSONEncoder().encode(snapshot.activeIntervals)
        createdAt = snapshot.createdAt
    }
}

@Model
final class FlowHealthSnapshotModel {
    @Attribute(.unique) var sessionID: UUID
    var healthEnabledAtStart: Bool
    var accessRequestedAtStart: Bool
    var statusRawValue: String
    var steps: Int64?
    var rawMovementPoints: Int64
    var finalMovementScyraContribution: Int64
    var finalMovementPearlContribution: Int64
    var firstCheckedAt: Date?
    var lastCheckedAt: Date?
    var capturedAt: Date?
    var expiresAt: Date?
    var checkCount: Int
    var flowStartTime: Date
    var flowEndTime: Date
    var activeIntervalsData: Data?
    var sourceLabel: String
    var updatedAfterSync: Bool

    init(snapshot: FlowHealthSnapshot) {
        sessionID = snapshot.sessionID
        healthEnabledAtStart = snapshot.healthEnabledAtStart
        accessRequestedAtStart = snapshot.accessRequestedAtStart
        statusRawValue = snapshot.status.rawValue
        steps = snapshot.steps
        rawMovementPoints = snapshot.rawMovementPoints
        finalMovementScyraContribution = snapshot.finalMovementScyraContribution
        finalMovementPearlContribution = snapshot.finalMovementPearlContribution
        firstCheckedAt = snapshot.firstCheckedAt
        lastCheckedAt = snapshot.lastCheckedAt
        capturedAt = snapshot.capturedAt
        expiresAt = snapshot.expiresAt
        checkCount = snapshot.checkCount
        flowStartTime = snapshot.flowStartTime
        flowEndTime = snapshot.flowEndTime
        activeIntervalsData = try? JSONEncoder().encode(snapshot.activeIntervals)
        sourceLabel = snapshot.sourceLabel
        updatedAfterSync = snapshot.updatedAfterSync
    }

    func update(from snapshot: FlowHealthSnapshot) {
        healthEnabledAtStart = snapshot.healthEnabledAtStart
        accessRequestedAtStart = snapshot.accessRequestedAtStart
        statusRawValue = snapshot.status.rawValue
        steps = snapshot.steps
        rawMovementPoints = snapshot.rawMovementPoints
        finalMovementScyraContribution = snapshot.finalMovementScyraContribution
        finalMovementPearlContribution = snapshot.finalMovementPearlContribution
        firstCheckedAt = snapshot.firstCheckedAt
        lastCheckedAt = snapshot.lastCheckedAt
        capturedAt = snapshot.capturedAt
        expiresAt = snapshot.expiresAt
        checkCount = snapshot.checkCount
        flowStartTime = snapshot.flowStartTime
        flowEndTime = snapshot.flowEndTime
        activeIntervalsData = try? JSONEncoder().encode(snapshot.activeIntervals)
        sourceLabel = snapshot.sourceLabel
        updatedAfterSync = snapshot.updatedAfterSync
    }
}

@Model
final class FlowRewardBreakdownModel {
    @Attribute(.unique) var sessionID: UUID
    var nonMovementPreMultiplierPoints: Int64
    var pulseBonusPoints: Int64
    var surgeBonusPoints: Int64
    var otherPreMultiplierBonusPoints: Int64
    var movementPoints: Int64
    var preMultiplierTotal: Int64
    var arcMultiplier: Double
    var streakMultiplier: Double
    var otherMultiplier: Double
    var arcBonusPoints: Int64
    var finalScyraPoints: Int64
    var pearlsEarned: Int64
    var pearlEligible: Bool

    init(breakdown: FlowRewardBreakdown) {
        sessionID = breakdown.sessionID
        nonMovementPreMultiplierPoints = breakdown.nonMovementPreMultiplierPoints
        pulseBonusPoints = breakdown.pulseBonusPoints
        surgeBonusPoints = breakdown.surgeBonusPoints
        otherPreMultiplierBonusPoints = breakdown.otherPreMultiplierBonusPoints
        movementPoints = breakdown.movementPoints
        preMultiplierTotal = breakdown.preMultiplierTotal
        arcMultiplier = breakdown.arcMultiplier
        streakMultiplier = breakdown.streakMultiplier
        otherMultiplier = breakdown.otherMultiplier
        arcBonusPoints = breakdown.arcBonusPoints
        finalScyraPoints = breakdown.finalScyraPoints
        pearlsEarned = breakdown.pearlsEarned
        pearlEligible = breakdown.pearlEligible
    }

    func update(from breakdown: FlowRewardBreakdown) {
        nonMovementPreMultiplierPoints = breakdown.nonMovementPreMultiplierPoints
        pulseBonusPoints = breakdown.pulseBonusPoints
        surgeBonusPoints = breakdown.surgeBonusPoints
        otherPreMultiplierBonusPoints = breakdown.otherPreMultiplierBonusPoints
        movementPoints = breakdown.movementPoints
        preMultiplierTotal = breakdown.preMultiplierTotal
        arcMultiplier = breakdown.arcMultiplier
        streakMultiplier = breakdown.streakMultiplier
        otherMultiplier = breakdown.otherMultiplier
        arcBonusPoints = breakdown.arcBonusPoints
        finalScyraPoints = breakdown.finalScyraPoints
        pearlsEarned = breakdown.pearlsEarned
        pearlEligible = breakdown.pearlEligible
    }
}

@Model
final class ArcStateModel {
    @Attribute(.unique) var slot: String
    var arcID: UUID
    var isPending: Bool
    var multiplier: Double
    var progressMs: Int64
    var lastSessionEndTime: Date
    var sessionCount: Int
    var pauseUsedMs: Int64
    var pauseStartedAt: Date?

    init(slot: String, state: ArcRuntimeState) {
        self.slot = slot
        self.arcID = state.id
        self.isPending = state.isPending
        self.multiplier = state.multiplier
        self.progressMs = state.progressMs
        self.lastSessionEndTime = state.lastSessionEndTime
        self.sessionCount = state.sessionCount
        self.pauseUsedMs = state.pauseUsedMs
        self.pauseStartedAt = state.pauseStartedAt
    }

    func update(from state: ArcRuntimeState) {
        arcID = state.id
        isPending = state.isPending
        multiplier = state.multiplier
        progressMs = state.progressMs
        lastSessionEndTime = state.lastSessionEndTime
        sessionCount = state.sessionCount
        pauseUsedMs = state.pauseUsedMs
        pauseStartedAt = state.pauseStartedAt
    }
}

@Model
final class ArcMetadataModel {
    @Attribute(.unique) var arcID: UUID
    var title: String?
    var summary: String?
    var outcome: String?
    var highlight: String?
    var nextStep: String?
    var createdAt: Date
    var updatedAt: Date

    init(metadata: ArcMetadata, createdAt: Date, updatedAt: Date) {
        arcID = metadata.arcID
        title = metadata.title
        summary = metadata.summary
        outcome = metadata.outcome
        highlight = metadata.highlight
        nextStep = metadata.nextStep
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    func update(from metadata: ArcMetadata, at date: Date) {
        title = metadata.title
        summary = metadata.summary
        outcome = metadata.outcome
        highlight = metadata.highlight
        nextStep = metadata.nextStep
        updatedAt = date
    }
}

@Model
final class ChronicleModel {
    @Attribute(.unique) var id: UUID
    @Attribute(.unique) var ownerIdentity: String
    var ownerTypeRawValue: String
    var ownerKey: String
    var draftText: String
    var createdAt: Date
    var updatedAt: Date

    @Relationship(deleteRule: .cascade, inverse: \ChronicleMomentModel.chronicle)
    var moments: [ChronicleMomentModel]

    init(id: UUID = UUID(), owner: ChronicleOwner, draftText: String = "", now: Date) {
        self.id = id
        self.ownerIdentity = owner.identity
        self.ownerTypeRawValue = owner.type.rawValue
        self.ownerKey = owner.key
        self.draftText = draftText
        self.createdAt = now
        self.updatedAt = now
        self.moments = []
    }

    func promote(to owner: ChronicleOwner, now: Date) {
        ownerIdentity = owner.identity
        ownerTypeRawValue = owner.type.rawValue
        ownerKey = owner.key
        updatedAt = now
    }
}

@Model
final class ChronicleMomentModel {
    @Attribute(.unique) var id: UUID
    var typeRawValue: String
    var position: Int
    var text: String?
    var audioPath: String?
    var displayName: String?
    var mimeType: String?
    var durationMs: Int64?
    var originalTranscript: String?
    var transcript: String?
    var transcriptEdited: Bool
    var createdAt: Date
    var updatedAt: Date
    var chronicle: ChronicleModel?

    @Relationship(deleteRule: .cascade, inverse: \ChronicleMediaItemModel.moment)
    var mediaItems: [ChronicleMediaItemModel]

    init(moment: ChronicleMoment, chronicle: ChronicleModel) {
        self.id = moment.id
        self.typeRawValue = moment.type.rawValue
        self.position = moment.position
        self.text = moment.text
        self.audioPath = moment.audioPath
        self.displayName = moment.displayName
        self.mimeType = moment.mimeType
        self.durationMs = moment.durationMs
        self.originalTranscript = moment.originalTranscript
        self.transcript = moment.transcript
        self.transcriptEdited = moment.transcriptEdited
        self.createdAt = moment.createdAt
        self.updatedAt = moment.updatedAt
        self.chronicle = chronicle
        self.mediaItems = []
    }
}

@Model
final class ChronicleMediaItemModel {
    @Attribute(.unique) var id: UUID
    var position: Int
    var localPath: String
    var mimeType: String
    var durationMs: Int64?
    var width: Int?
    var height: Int?
    var thumbnailPath: String?
    var createdAt: Date
    var moment: ChronicleMomentModel?

    init(item: ChronicleMediaItem, moment: ChronicleMomentModel) {
        self.id = item.id
        self.position = item.position
        self.localPath = item.localPath
        self.mimeType = item.mimeType
        self.durationMs = item.durationMs
        self.width = item.width
        self.height = item.height
        self.thumbnailPath = item.thumbnailPath
        self.createdAt = item.createdAt
        self.moment = moment
    }
}

@Model
final class PulseModel {
    @Attribute(.unique) var id: UUID
    var title: String
    var pulseDescription: String
    var journeyNameSnapshot: String?
    var parentSessionID: UUID?
    var parentFlowInstanceID: UUID?
    var arcID: UUID?
    var createdAt: Date
    var updatedAt: Date
    var groveStatusRawValue: String
    var groveStatusChangedAt: Date?
    var journey: JourneyModel?

    init(pulse: Pulse, journey: JourneyModel?) {
        self.id = pulse.id
        self.title = pulse.title
        self.pulseDescription = pulse.description
        self.journeyNameSnapshot = pulse.journeyName
        self.parentSessionID = pulse.parentSessionID
        self.parentFlowInstanceID = pulse.parentFlowInstanceID
        self.arcID = pulse.arcID
        self.createdAt = pulse.createdAt
        self.updatedAt = pulse.updatedAt
        self.groveStatusRawValue = pulse.groveStatus.rawValue
        self.groveStatusChangedAt = pulse.groveStatusChangedAt
        self.journey = journey
    }
}

@Model
final class PulseCreationModel {
    @Attribute(.unique) var creationKey: UUID
    @Attribute(.unique) var pulseID: UUID
    var createdAt: Date

    init(creationKey: UUID, pulseID: UUID, createdAt: Date) {
        self.creationKey = creationKey
        self.pulseID = pulseID
        self.createdAt = createdAt
    }
}

@Model
final class PulseDraftModel {
    @Attribute(.unique) var id: String
    @Attribute(.unique) var creationKey: UUID
    var title: String
    var journeyName: String
    var attachToCurrentFlow: Bool
    var createdAt: Date

    init(draft: PulseDraft) {
        self.id = PulseDraft.singletonID
        self.creationKey = draft.creationKey
        self.title = draft.title
        self.journeyName = draft.journeyName
        self.attachToCurrentFlow = draft.attachToCurrentFlow
        self.createdAt = draft.createdAt
    }

    func update(from draft: PulseDraft) {
        creationKey = draft.creationKey
        title = draft.title
        journeyName = draft.journeyName
        attachToCurrentFlow = draft.attachToCurrentFlow
        createdAt = draft.createdAt
    }
}

@Model
final class PulseFlowLinkModel {
    @Attribute(.unique) var id: UUID
    var pulseID: UUID
    @Attribute(.unique) var sessionID: UUID
    var linkedAt: Date

    init(link: PulseFlowLink) {
        self.id = link.id
        self.pulseID = link.pulseID
        self.sessionID = link.sessionID
        self.linkedAt = link.linkedAt
    }
}

@Model
final class PearlLedgerModel {
    @Attribute(.unique) var id: String
    var delta: Int
    var reason: String
    var sourceType: String
    var sourceID: String?
    var createdAt: Date
    var note: String?

    init(entry: PearlLedgerEntry) {
        id = entry.id
        delta = entry.delta
        reason = entry.reason
        sourceType = entry.sourceType
        sourceID = entry.sourceID
        createdAt = entry.createdAt
        note = entry.note
    }
}

@Model
final class StillwaterLedgerModel {
    @Attribute(.unique) var id: String
    var units: Int64
    var sourceType: String
    var sourceID: String?
    var createdAt: Date

    init(entry: StillwaterLedgerEntry) {
        id = entry.id
        units = entry.units
        sourceType = entry.sourceType
        sourceID = entry.sourceID
        createdAt = entry.createdAt
    }
}

@Model
final class ShellRewardEventModel {
    @Attribute(.unique) var id: String
    var sourceSessionID: UUID
    var arcID: UUID?
    var typeRawValue: String
    var rewardID: String?
    var quantity: Int64
    var occurredAt: Date

    init(event: ShellRewardEvent) {
        id = event.id
        sourceSessionID = event.sourceSessionID
        arcID = event.arcID
        typeRawValue = event.type.rawValue
        rewardID = event.rewardID
        quantity = event.quantity
        occurredAt = event.occurredAt
    }
}

@Model
final class ShellFindGrantModel {
    @Attribute(.unique) var id: String
    var findID: String
    var sourceSessionID: UUID
    var acquiredAt: Date
    var isNew: Bool

    init(id: String, findID: String, sourceSessionID: UUID, acquiredAt: Date, isNew: Bool = true) {
        self.id = id
        self.findID = findID
        self.sourceSessionID = sourceSessionID
        self.acquiredAt = acquiredAt
        self.isNew = isNew
    }
}

@Model
final class ShellBadgeModel {
    @Attribute(.unique) var badgeID: String
    var count: Int
    var firstEarnedAt: Date
    var lastEarnedAt: Date
    var isNew: Bool

    init(badgeID: String, count: Int, earnedAt: Date, isNew: Bool = true) {
        self.badgeID = badgeID
        self.count = count
        self.firstEarnedAt = earnedAt
        self.lastEarnedAt = earnedAt
        self.isNew = isNew
    }
}

@Model
final class ShellFindInstanceModel {
    @Attribute(.unique) var id: String
    var findID: String
    var acquiredAt: Date
    var sourceType: String
    var sourceID: String?
    var currentUpgradeStageID: String?
    var isNew: Bool
    var isArchivedInChest: Bool
    var viewedAt: Date?
    var animalLevel: Int
    var lastActivityAt: Date

    init(instance: ShellFindInstance) {
        id = instance.id
        findID = instance.findID
        acquiredAt = instance.acquiredAt
        sourceType = instance.sourceType
        sourceID = instance.sourceID
        currentUpgradeStageID = instance.currentUpgradeStageID
        isNew = instance.isNew
        isArchivedInChest = instance.isArchivedInChest
        viewedAt = instance.viewedAt
        animalLevel = instance.animalLevel
        lastActivityAt = instance.lastActivityAt
    }
}

@Model
final class ShellFindStackModel {
    @Attribute(.unique) var findID: String
    var quantity: Int
    var firstAcquiredAt: Date
    var lastAcquiredAt: Date
    var isNew: Bool
    var viewedAt: Date?

    init(stack: ShellFindStack) {
        findID = stack.findID
        quantity = stack.quantity
        firstAcquiredAt = stack.firstAcquiredAt
        lastAcquiredAt = stack.lastAcquiredAt
        isNew = stack.isNew
        viewedAt = stack.viewedAt
    }
}

@Model
final class ShellPlacementModel {
    @Attribute(.unique) var id: String
    var roomID: String
    var slotID: String
    @Attribute(.unique) var instanceID: String
    var placedAt: Date

    init(placement: ShellPlacement) {
        id = placement.id
        roomID = placement.roomID
        slotID = placement.slotID
        instanceID = placement.instanceID
        placedAt = placement.placedAt
    }
}

@Model
final class ShellFindUpgradeModel {
    @Attribute(.unique) var id: String
    var instanceID: String
    var fromStageID: String?
    var toStageID: String
    var pearlCost: Int
    var upgradedAt: Date

    init(upgrade: ShellFindUpgrade) {
        id = upgrade.id
        instanceID = upgrade.instanceID
        fromStageID = upgrade.fromStageID
        toStageID = upgrade.toStageID
        pearlCost = upgrade.pearlCost
        upgradedAt = upgrade.upgradedAt
    }
}

@Model
final class ShellBadgePinModel {
    @Attribute(.unique) var badgeID: String
    var pinOrder: Int
    var pinnedAt: Date

    init(badgeID: String, pinOrder: Int, pinnedAt: Date) {
        self.badgeID = badgeID
        self.pinOrder = pinOrder
        self.pinnedAt = pinnedAt
    }
}

@Model
final class ShellBadgeCountFloorModel {
    @Attribute(.unique) var badgeID: String
    var minimumCount: Int
    var verifiedCountAtReconciliation: Int
    var source: String
    var reconciledAt: Date

    init(badgeID: String, minimumCount: Int, verifiedCountAtReconciliation: Int, source: String, reconciledAt: Date) {
        self.badgeID = badgeID
        self.minimumCount = minimumCount
        self.verifiedCountAtReconciliation = verifiedCountAtReconciliation
        self.source = source
        self.reconciledAt = reconciledAt
    }
}

@Model
final class ShellCollectionBackfillModel {
    @Attribute(.unique) var version: Int
    var completedAt: Date
    var instanceCount: Int
    var badgeCount: Int

    init(version: Int, completedAt: Date, instanceCount: Int, badgeCount: Int) {
        self.version = version
        self.completedAt = completedAt
        self.instanceCount = instanceCount
        self.badgeCount = badgeCount
    }
}

@Model
final class StillwaterPreferenceModel {
    @Attribute(.unique) var id: Int
    var perspectiveRawValue: String
    var updatedAt: Date

    init(perspective: StillwaterPerspective, updatedAt: Date) {
        id = 1
        perspectiveRawValue = perspective.rawValue
        self.updatedAt = updatedAt
    }
}

@Model
final class CreatureLifecycleModel {
    @Attribute(.unique) var instanceID: String
    var statusRawValue: String
    var updatedAt: Date

    init(instanceID: String, status: CreatureStatus, updatedAt: Date) {
        self.instanceID = instanceID
        statusRawValue = status.rawValue
        self.updatedAt = updatedAt
    }
}

@Model
final class CreatureDiscoveryModel {
    @Attribute(.unique) var speciesID: String
    var firstDiscoveredAt: Date
    var sourceTypeRawValue: String
    var firstInstanceID: String?
    var timestampConfidence: String

    init(evidence: CreatureDiscoveryEvidence) {
        speciesID = evidence.speciesID
        firstDiscoveredAt = evidence.firstDiscoveredAt
        sourceTypeRawValue = evidence.sourceType.rawValue
        firstInstanceID = evidence.firstInstanceID
        timestampConfidence = evidence.timestampConfidence
    }
}

@Model
final class CreatureMasteryModel {
    @Attribute(.unique) var id: String
    @Attribute(.unique) var instanceID: String
    var speciesID: String
    var achievedAt: Date
    @Attribute(.unique) var transactionID: String
    var timestampConfidence: String

    init(evidence: CreatureMasteryEvidence) {
        id = evidence.id
        instanceID = evidence.instanceID
        speciesID = evidence.speciesID
        achievedAt = evidence.achievedAt
        transactionID = evidence.transactionID
        timestampConfidence = evidence.timestampConfidence
    }
}

@Model
final class CollectionCompletionModel {
    @Attribute(.unique) var id: String
    var collectionID: String
    var typeRawValue: String
    var completedAt: Date
    var rosterVersion: Int
    var rosterHash: String
    var requiredSpeciesIDsRawValue: String

    init(evidence: CollectionCompletionEvidence) {
        id = evidence.id
        collectionID = evidence.collectionID
        typeRawValue = evidence.type.rawValue
        completedAt = evidence.completedAt
        rosterVersion = evidence.rosterVersion
        rosterHash = evidence.rosterHash
        requiredSpeciesIDsRawValue = evidence.requiredSpeciesIDs.sorted().joined(separator: "\n")
    }
}

@Model
final class AchievementTrackingModel {
    @Attribute(.unique) var badgeID: String
    var trackedAt: Date

    init(badgeID: String, trackedAt: Date) {
        self.badgeID = badgeID
        self.trackedAt = trackedAt
    }
}

@Model
final class MasteryCelebrationModel {
    @Attribute(.unique) var id: String
    var instanceID: String
    var speciesID: String
    var createdAt: Date
    var acknowledgedAt: Date?

    init(id: String, instanceID: String, speciesID: String, createdAt: Date, acknowledgedAt: Date? = nil) {
        self.id = id
        self.instanceID = instanceID
        self.speciesID = speciesID
        self.createdAt = createdAt
        self.acknowledgedAt = acknowledgedAt
    }
}

@Model
final class CreatureActionReceiptModel {
    @Attribute(.unique) var transactionID: String
    var actionType: String
    var instanceID: String
    var pearlCost: Int
    var resultingLevel: Int
    var createdAt: Date

    init(transactionID: String, actionType: String, instanceID: String, pearlCost: Int, resultingLevel: Int, createdAt: Date) {
        self.transactionID = transactionID
        self.actionType = actionType
        self.instanceID = instanceID
        self.pearlCost = pearlCost
        self.resultingLevel = resultingLevel
        self.createdAt = createdAt
    }
}

@Model
final class FlowPlanModel {
    @Attribute(.unique) var id: UUID
    var title: String
    var journeyName: String?
    var isSoftMode: Bool
    var targetMinutes: Int?
    var launchWithSurge: Bool
    var pinned: Bool
    var archived: Bool
    var launchCount: Int
    var lastLaunchedAt: Date?
    var createdAt: Date
    var updatedAt: Date

    init(plan: FlowPlan) {
        id = plan.id
        title = plan.title
        journeyName = plan.journeyName
        isSoftMode = plan.isSoftMode
        targetMinutes = plan.targetMinutes
        launchWithSurge = plan.launchWithSurge
        pinned = plan.pinned
        archived = plan.archived
        launchCount = plan.launchCount
        lastLaunchedAt = plan.lastLaunchedAt
        createdAt = plan.createdAt
        updatedAt = plan.updatedAt
    }

    func update(input: FlowPlanInput, at date: Date) {
        title = input.title
        journeyName = input.journeyName
        isSoftMode = input.isSoftMode
        targetMinutes = input.targetMinutes
        launchWithSurge = input.launchWithSurge
        updatedAt = date
    }
}

@Model
final class ArcPlanModel {
    @Attribute(.unique) var id: UUID
    var title: String
    var isInStudio: Bool
    var archived: Bool
    var launchCount: Int
    var lastLaunchedAt: Date?
    var recurrenceRawValue: String
    var recurrenceDaysCSV: String
    var createdAt: Date
    var updatedAt: Date

    @Relationship(deleteRule: .cascade, inverse: \ArcPlanStepModel.plan)
    var steps: [ArcPlanStepModel]

    init(id: UUID = UUID(), input: ArcPlanInput, now: Date) {
        self.id = id
        title = input.title
        isInStudio = input.isInStudio
        archived = false
        launchCount = 0
        lastLaunchedAt = nil
        recurrenceRawValue = input.recurrence.rawValue
        recurrenceDaysCSV = Self.daysCSV(input.recurrenceDays)
        createdAt = now
        updatedAt = now
        steps = []
    }

    func update(input: ArcPlanInput, at date: Date) {
        title = input.title
        isInStudio = input.isInStudio
        recurrenceRawValue = input.recurrence.rawValue
        recurrenceDaysCSV = Self.daysCSV(input.recurrenceDays)
        updatedAt = date
    }

    private static func daysCSV(_ days: Set<Int>) -> String {
        days.sorted().map(String.init).joined(separator: ",")
    }
}

@Model
final class ArcPlanStepModel {
    @Attribute(.unique) var id: UUID
    var orderIndex: Int
    var sourceFlowPlanID: UUID?
    var titleSnapshot: String
    var journeyNameSnapshot: String?
    var isSoftModeSnapshot: Bool
    var targetMinutesSnapshot: Int?
    var launchWithSurgeSnapshot: Bool
    var linkStateRawValue: String
    var createdAt: Date
    var updatedAt: Date
    var plan: ArcPlanModel?

    init(
        id: UUID = UUID(),
        orderIndex: Int,
        input: ArcPlanStepInput,
        plan: ArcPlanModel,
        now: Date
    ) {
        self.id = id
        self.orderIndex = orderIndex
        sourceFlowPlanID = input.sourceFlowPlanID
        titleSnapshot = input.titleSnapshot
        journeyNameSnapshot = input.journeyNameSnapshot
        isSoftModeSnapshot = input.isSoftModeSnapshot
        targetMinutesSnapshot = input.targetMinutesSnapshot
        launchWithSurgeSnapshot = input.launchWithSurgeSnapshot
        linkStateRawValue = input.linkState.rawValue
        createdAt = now
        updatedAt = now
        self.plan = plan
    }
}

@Model
final class ActivePlannedArcRunModel {
    @Attribute(.unique) var id: String
    var arcPlanID: UUID
    var arcTitle: String
    var currentStepIndex: Int
    var totalSteps: Int
    var currentStepTitle: String
    var currentJourneyName: String?
    var currentIsSoftMode: Bool
    var startedAt: Date
    var updatedAt: Date

    init(run: ActivePlannedArcRun) {
        id = ActivePlannedArcRun.singletonID
        arcPlanID = run.arcPlanID
        arcTitle = run.arcTitle
        currentStepIndex = run.currentStepIndex
        totalSteps = run.totalSteps
        currentStepTitle = run.currentStepTitle
        currentJourneyName = run.currentJourneyName
        currentIsSoftMode = run.currentIsSoftMode
        startedAt = run.startedAt
        updatedAt = run.updatedAt
    }

    func update(from run: ActivePlannedArcRun) {
        arcPlanID = run.arcPlanID
        arcTitle = run.arcTitle
        currentStepIndex = run.currentStepIndex
        totalSteps = run.totalSteps
        currentStepTitle = run.currentStepTitle
        currentJourneyName = run.currentJourneyName
        currentIsSoftMode = run.currentIsSoftMode
        startedAt = run.startedAt
        updatedAt = run.updatedAt
    }
}

@Model
final class LookoutObjectiveModel {
    @Attribute(.unique) var id: UUID
    var journeyID: UUID
    var journeyNameSnapshot: String
    var periodRawValue: String
    var kindRawValue: String
    var targetDurationMs: Int64
    var startAt: Date
    var weeklyBoundaryDay: Int?
    var currentStreak: Int
    var maxStreak: Int
    var totalCompletions: Int
    var isArchived: Bool
    var createdAt: Date
    var updatedAt: Date

    init(objective: LookoutObjective) {
        id = objective.id
        journeyID = objective.journeyID
        journeyNameSnapshot = objective.journeyNameSnapshot
        periodRawValue = objective.period.rawValue
        kindRawValue = objective.kind.rawValue
        targetDurationMs = objective.targetDurationMs
        startAt = objective.startAt
        weeklyBoundaryDay = objective.weeklyBoundaryDay
        currentStreak = objective.currentStreak
        maxStreak = objective.maxStreak
        totalCompletions = objective.totalCompletions
        isArchived = objective.isArchived
        createdAt = objective.createdAt
        updatedAt = objective.updatedAt
    }
}

@Model
final class LookoutCompletionModel {
    @Attribute(.unique) var id: UUID
    @Attribute(.unique) var cycleKey: String
    var objectiveID: UUID
    var journeyID: UUID
    var journeyNameSnapshot: String
    var periodRawValue: String
    var kindRawValue: String
    var periodStart: Date
    var periodEnd: Date
    var completedAt: Date
    var achievedDurationMs: Int64
    var targetDurationMs: Int64
    var baseRewardPearls: Int
    var streakBeforeCompletion: Int
    var streakMultiplier: Double
    var finalRewardPearls: Int
    var badgeKey: String
    var badgeLabelSnapshot: String
    var pearlsClaimed: Bool
    var pearlsClaimedAt: Date?

    init(completion: LookoutCompletion) {
        id = completion.id
        cycleKey = Self.makeCycleKey(
            objectiveID: completion.objectiveID,
            start: completion.periodStart,
            end: completion.periodEnd
        )
        objectiveID = completion.objectiveID
        journeyID = completion.journeyID
        journeyNameSnapshot = completion.journeyNameSnapshot
        periodRawValue = completion.period.rawValue
        kindRawValue = completion.kind.rawValue
        periodStart = completion.periodStart
        periodEnd = completion.periodEnd
        completedAt = completion.completedAt
        achievedDurationMs = completion.achievedDurationMs
        targetDurationMs = completion.targetDurationMs
        baseRewardPearls = completion.baseRewardPearls
        streakBeforeCompletion = completion.streakBeforeCompletion
        streakMultiplier = completion.streakMultiplier
        finalRewardPearls = completion.finalRewardPearls
        badgeKey = completion.badgeKey
        badgeLabelSnapshot = completion.badgeLabelSnapshot
        pearlsClaimed = completion.pearlsClaimed
        pearlsClaimedAt = completion.pearlsClaimedAt
    }

    static func makeCycleKey(objectiveID: UUID, start: Date, end: Date) -> String {
        "\(objectiveID.uuidString):\(start.timeIntervalSince1970):\(end.timeIntervalSince1970)"
    }
}

@Model
final class LookoutSkippedCycleModel {
    @Attribute(.unique) var id: String
    var objectiveID: UUID
    var periodStart: Date
    var periodEnd: Date
    var skippedAt: Date

    init(cycle: ObjectiveSkippedCycle) {
        id = cycle.id
        objectiveID = cycle.objectiveID
        periodStart = cycle.periodStart
        periodEnd = cycle.periodEnd
        skippedAt = cycle.skippedAt
    }
}

@Model
final class LookoutProcessedSessionModel {
    @Attribute(.unique) var sessionID: UUID
    var processedAt: Date

    init(sessionID: UUID, processedAt: Date) {
        self.sessionID = sessionID
        self.processedAt = processedAt
    }
}

enum ArcPersistenceSlot {
    static let active = "active"
    static let recentlyEnded = "recently-ended"
}

enum ScyraSchemaV1: VersionedSchema {
    static let versionIdentifier = Schema.Version(1, 0, 0)
    static let models: [any PersistentModel.Type] = [
        JourneyModel.self,
        FlowSessionModel.self,
        ActiveFlowModel.self,
        ArcStateModel.self,
        ArcMetadataModel.self,
        FlowHealthSnapshotModel.self,
        FlowRewardBreakdownModel.self,
        ChronicleModel.self,
        ChronicleMomentModel.self,
        ChronicleMediaItemModel.self,
        PulseModel.self,
        PulseCreationModel.self,
        PulseDraftModel.self,
        PulseFlowLinkModel.self,
        PearlLedgerModel.self,
        StillwaterLedgerModel.self,
        ShellRewardEventModel.self,
        ShellFindGrantModel.self,
        ShellBadgeModel.self
    ]
}

enum ScyraSchemaV2: VersionedSchema {
    static let versionIdentifier = Schema.Version(2, 0, 0)
    static let models: [any PersistentModel.Type] = ScyraSchemaV1.models + [FlowPlanModel.self]
}

enum ScyraSchemaV3: VersionedSchema {
    static let versionIdentifier = Schema.Version(3, 0, 0)
    static let models: [any PersistentModel.Type] = ScyraSchemaV2.models + [
        ArcPlanModel.self,
        ArcPlanStepModel.self,
        ActivePlannedArcRunModel.self
    ]
}

enum ScyraSchemaV4: VersionedSchema {
    static let versionIdentifier = Schema.Version(4, 0, 0)
    static let models: [any PersistentModel.Type] = ScyraSchemaV3.models + [
        ShellFindInstanceModel.self,
        ShellFindStackModel.self,
        ShellPlacementModel.self,
        ShellFindUpgradeModel.self,
        ShellBadgePinModel.self,
        ShellBadgeCountFloorModel.self,
        ShellCollectionBackfillModel.self
    ]
}

enum ScyraSchemaV5: VersionedSchema {
    static let versionIdentifier = Schema.Version(5, 0, 0)
    static let models: [any PersistentModel.Type] = ScyraSchemaV4.models + [
        StillwaterPreferenceModel.self,
        CreatureLifecycleModel.self,
        CreatureDiscoveryModel.self,
        CreatureMasteryModel.self,
        CollectionCompletionModel.self,
        AchievementTrackingModel.self,
        MasteryCelebrationModel.self,
        CreatureActionReceiptModel.self
    ]
}

enum ScyraSchemaV6: VersionedSchema {
    static let versionIdentifier = Schema.Version(6, 0, 0)
    static let models: [any PersistentModel.Type] = ScyraSchemaV5.models + [
        LookoutObjectiveModel.self,
        LookoutCompletionModel.self,
        LookoutSkippedCycleModel.self,
        LookoutProcessedSessionModel.self
    ]
}

enum ScyraMigrationPlan: SchemaMigrationPlan {
    static let schemas: [any VersionedSchema.Type] = [
        ScyraSchemaV1.self,
        ScyraSchemaV2.self,
        ScyraSchemaV3.self,
        ScyraSchemaV4.self,
        ScyraSchemaV5.self,
        ScyraSchemaV6.self
    ]
    static let stages: [MigrationStage] = [
        .lightweight(fromVersion: ScyraSchemaV1.self, toVersion: ScyraSchemaV2.self),
        .lightweight(fromVersion: ScyraSchemaV2.self, toVersion: ScyraSchemaV3.self),
        .lightweight(fromVersion: ScyraSchemaV3.self, toVersion: ScyraSchemaV4.self),
        .lightweight(fromVersion: ScyraSchemaV4.self, toVersion: ScyraSchemaV5.self),
        .lightweight(fromVersion: ScyraSchemaV5.self, toVersion: ScyraSchemaV6.self)
    ]
}

enum ScyraPersistenceFactory {
    static func makeContainer(inMemory: Bool = false) throws -> ModelContainer {
        let schema = Schema(versionedSchema: ScyraSchemaV6.self)
        let configuration = ModelConfiguration(
            "Scyra",
            schema: schema,
            isStoredInMemoryOnly: inMemory
        )
        return try ModelContainer(
            for: schema,
            migrationPlan: ScyraMigrationPlan.self,
            configurations: [configuration]
        )
    }
}
