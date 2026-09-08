import SwiftUI

struct ShellView: View {
    @ObservedObject var viewModel: ShellViewModel
    let onOpenTheBlue: () -> Void
    let onOpenIdeaGrove: () -> Void
    let onOpenStillwater: () -> Void
    let onOpenLookout: () -> Void
    let onOpenVoyageHall: () -> Void
    let onOpenFocusRoom: () -> Void
    let onOpenChest: () -> Void
    let onOpenBadges: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(ScyraTypography.caption)
                        .foregroundStyle(ScyraColors.error)
                        .accessibilityIdentifier("shell-balance-error")
                }

                ShellHeartScene(
                    pearlBalance: viewModel.pearlBalance,
                    newCreatureCount: newCreatureCount,
                    restingCreatureCount: restingCreatureCount,
                    newChestCount: viewModel.instances.filter(\.isNew).count,
                    newBadgeCount: viewModel.achievementDashboard.newCount,
                    onOpenLookout: onOpenLookout,
                    onOpenVoyageHall: onOpenVoyageHall,
                    onOpenIdeaGrove: onOpenIdeaGrove,
                    onOpenFocusRoom: onOpenFocusRoom,
                    onOpenStillwater: onOpenStillwater,
                    onOpenTheBlue: onOpenTheBlue,
                    onOpenChest: onOpenChest,
                    onOpenBadges: onOpenBadges
                )
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .background(
            LinearGradient(
                colors: [ScyraColors.primary, ScyraColors.background],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .onAppear(perform: viewModel.refresh)
    }

    private var restingCreatureCount: Int {
        let placed = Set(viewModel.placements.map(\.instanceID))
        return viewModel.instances.count {
            CreatureCatalog.definition($0.findID) != nil && !placed.contains($0.id) && $0.isArchivedInChest
        }
    }

    private var newCreatureCount: Int {
        viewModel.instances.count { CreatureCatalog.definition($0.findID) != nil && $0.isNew }
    }

}

#Preview {
    ShellView(
        viewModel: ShellViewModel(repository: InMemoryFlowRepository()),
        onOpenTheBlue: {},
        onOpenIdeaGrove: {},
        onOpenStillwater: {},
        onOpenLookout: {},
        onOpenVoyageHall: {},
        onOpenFocusRoom: {},
        onOpenChest: {},
        onOpenBadges: {}
    )
}
