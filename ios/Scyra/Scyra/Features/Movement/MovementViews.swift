import SwiftUI

struct MovementSettingsCard: View {
    @ObservedObject var controller: MovementController

    var body: some View {
        ScyraCard(style: .elevated) {
            VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                HStack(alignment: .top, spacing: ScyraSpacing.sm) {
                    ScyraCanonicalIcon(systemName: "figure.walk")
                        .font(.title3)
                        .foregroundStyle(ScyraColors.rewardMovement)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(MovementStrings.title).font(ScyraTypography.cardTitle)
                        Text(headline).font(ScyraTypography.body.weight(.medium))
                    }
                    Spacer()
                    if controller.isAvailable, controller.accessWasRequested {
                        Toggle(
                            MovementStrings.title,
                            isOn: Binding(
                                get: { controller.isEnabled },
                                set: { enabled in
                                    if enabled { Task { await controller.enable() } }
                                    else { controller.requestDisable() }
                                }
                            )
                        )
                        .labelsHidden()
                        .disabled(controller.isBusy)
                        .accessibilityLabel("Movement Bonus toggle")
                    }
                }

                Text(bodyText)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                Text(MovementStrings.privacy)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textMuted)

                if controller.isAvailable, !controller.isEnabled, !controller.accessWasRequested {
                    Button {
                        Task { await controller.enable() }
                    } label: {
                        if controller.isBusy {
                            ProgressView().frame(maxWidth: .infinity)
                        } else {
                            ScyraCanonicalLabel(MovementStrings.connect, systemImage: "heart.text.square")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(ScyraColors.rewardMovement)
                    .disabled(controller.isBusy)
                }

                if let error = controller.errorMessage {
                    Text(error)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .accessibilityLabel("Error: \(error)")
                }
            }
        }
        .alert(MovementStrings.disableTitle, isPresented: $controller.showDisableWarning) {
            Button(MovementStrings.keepOn, role: .cancel, action: controller.keepEnabled)
            Button(MovementStrings.disableAnyway, role: .destructive, action: controller.disableAnyway)
        } message: {
            Text(MovementStrings.disableBody)
        }
    }

    private var headline: String {
        if !controller.isAvailable { return MovementStrings.unavailableHeadline }
        if controller.isEnabled { return MovementStrings.activeHeadline }
        if !controller.accessWasRequested { return MovementStrings.enableHeadline }
        return MovementStrings.offHeadline
    }

    private var bodyText: String {
        if !controller.isAvailable { return MovementStrings.healthUnavailable }
        if controller.isEnabled { return MovementStrings.activeBody }
        if !controller.accessWasRequested { return MovementStrings.enableBody }
        return MovementStrings.offBody
    }
}

struct MovementBonusActivePill: View {
    var body: some View {
        HStack(spacing: ScyraSpacing.sm) {
            ScyraCanonicalIcon(systemName: "figure.walk")
                .foregroundStyle(ScyraColors.rewardMovement)
            Text(MovementStrings.activePill).font(ScyraTypography.label)
            Spacer(minLength: 0)
        }
        .foregroundStyle(ScyraColors.textPrimary)
        .padding(.horizontal, 12)
        .frame(minHeight: 44)
        .background(ScyraColors.rewardMovement.opacity(0.14))
        .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.capsule))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MovementStrings.activePillAccessibility)
    }
}

struct MovementBonusRewardBlock: View {
    let steps: Int64
    let points: Int64

    var body: some View {
        if steps > 0, points > 0 {
            VStack(alignment: .leading, spacing: 6) {
                ScyraCanonicalLabel(MovementStrings.title, systemImage: "figure.walk")
                    .font(ScyraTypography.cardTitle)
                    .foregroundStyle(ScyraColors.rewardMovement)
                Text("Steps during this Flow: \(MovementStrings.steps(steps))")
                    .font(ScyraTypography.body)
                Text("Earned: \(MovementStrings.points(points))")
                    .font(ScyraTypography.cardTitle)
                    .foregroundStyle(ScyraColors.rewardMovement)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(ScyraSpacing.md)
            .background(ScyraColors.rewardMovement.opacity(0.14))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Movement Bonus. \(MovementStrings.steps(steps)). \(MovementStrings.points(points)).")
        }
    }
}

struct FlowMovementLine: View {
    let snapshot: FlowHealthSnapshot
    var showsSyncStatus = true

    var body: some View {
        if let steps = snapshot.steps, steps > 0, snapshot.rawMovementPoints > 0 {
            VStack(alignment: .leading, spacing: 3) {
                ScyraCanonicalLabel(
                    "\(MovementStrings.steps(steps)) · +\(snapshot.rawMovementPoints) Movement",
                    systemImage: "figure.walk"
                )
                .font(ScyraTypography.label)
                .foregroundStyle(ScyraColors.rewardMovement)
                if showsSyncStatus, snapshot.updatedAfterSync {
                    Text(MovementStrings.updatedAfterSync)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.textMuted)
                }
            }
            .accessibilityElement(children: .combine)
        }
    }
}
