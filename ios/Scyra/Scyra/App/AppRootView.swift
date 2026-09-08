import Combine
import SwiftUI

struct AppRootView: View {
    let container: AppDependencyContainer
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var navigationModel: AppNavigationModel
    @StateObject private var flowViewModel: FlowViewModel
    @StateObject private var storyViewModel: StoryViewModel
    @StateObject private var pulseViewModel: PulseViewModel
    @StateObject private var ideaGroveViewModel: IdeaGroveViewModel
    @StateObject private var horizonViewModel: HorizonViewModel
    @StateObject private var shellViewModel: ShellViewModel
    @StateObject private var lookoutViewModel: LookoutViewModel
    @StateObject private var voyageHallViewModel: VoyageHallViewModel
    @StateObject private var focusRoomViewModel: FocusRoomViewModel
    @StateObject private var preferences: AppPreferencesModel
    @StateObject private var deepLinkCoordinator: FlowDeepLinkCoordinator
    @State private var showsShellNotifications = false

    init(container: AppDependencyContainer) {
        self.container = container
        _preferences = StateObject(wrappedValue: container.preferences)
        _deepLinkCoordinator = StateObject(wrappedValue: container.deepLinkCoordinator)
        _navigationModel = StateObject(
            wrappedValue: AppNavigationModel(
                initialRoute: container.appLaunchCoordinator.initialRoute()
            )
        )
        _flowViewModel = StateObject(
            wrappedValue: FlowViewModel(
                repository: container.repository,
                movementController: container.movementController,
                reminderScheduler: container.flowReminderScheduler,
                surgeHaptics: container.surgeHaptics,
                startupError: container.persistenceStartupError
            )
        )
        _storyViewModel = StateObject(
            wrappedValue: StoryViewModel(repository: container.repository)
        )
        _pulseViewModel = StateObject(
            wrappedValue: PulseViewModel(repository: container.repository)
        )
        _ideaGroveViewModel = StateObject(
            wrappedValue: IdeaGroveViewModel(repository: container.repository)
        )
        _horizonViewModel = StateObject(
            wrappedValue: HorizonViewModel(repository: container.repository)
        )
        _shellViewModel = StateObject(
            wrappedValue: ShellViewModel(repository: container.repository)
        )
        _lookoutViewModel = StateObject(
            wrappedValue: LookoutViewModel(repository: container.repository)
        )
        _voyageHallViewModel = StateObject(
            wrappedValue: VoyageHallViewModel(repository: container.repository)
        )
        _focusRoomViewModel = StateObject(wrappedValue: FocusRoomViewModel())
    }

    var body: some View {
        RootNavigationShell(
            selectedRoute: navigationModel.selectedRoute,
            onSelectRoute: navigationModel.selectTopLevel,
            onBackToRoot: navigationModel.backToRouteRoot,
            shellPearlBalance: shellViewModel.pearlBalance,
            shellNotificationCount: shellViewModel.notifications.count,
            onShellNotifications: {
                withAnimation(.easeInOut(duration: 0.18)) {
                    showsShellNotifications.toggle()
                }
            }
        ) {
            ZStack(alignment: .topTrailing) {
                content(for: navigationModel.selectedRoute)
                if showsShellNotifications, navigationModel.selectedRoute.isShellRoute {
                    ShellNotificationsInlay(
                        notifications: shellViewModel.notifications,
                        onDismiss: { showsShellNotifications = false },
                        onMarkAllViewed: {
                            shellViewModel.markAllNotificationsViewed()
                            showsShellNotifications = false
                        },
                        onOpen: openShellNotification
                    )
                    .transition(.opacity.combined(with: .move(edge: .top)))
                    .zIndex(10)
                }
            }
        }
        .environment(\.chronicleFileStore, container.chronicleFileStore)
        .environment(\.locale, preferences.language.locale)
        .tint(ScyraColors.primary)
        .buttonBorderShape(.capsule)
        .task { await refreshMovementAndStory() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                flowViewModel.resumeFromBackground()
                Task { await refreshMovementAndStory() }
            }
        }
        .onOpenURL { _ = navigationModel.openDeepLink($0) }
        .onReceive(deepLinkCoordinator.$pendingURL.compactMap { $0 }) { url in
            if navigationModel.openDeepLink(url) {
                deepLinkCoordinator.clearPendingURL()
            }
        }
        .onChange(of: navigationModel.selectedRoute) { _, route in
            if !route.isShellRoute { showsShellNotifications = false }
        }
    }

    @ViewBuilder
    private func content(for route: AppRoute) -> some View {
        switch route {
        case .story:
            StoryView(
                viewModel: storyViewModel,
                preferences: preferences,
                onOpenPulse: navigationModel.openPulse,
                onOpenFlow: navigationModel.openFlow,
                onOpenFlowDetail: navigationModel.openFlowDetail,
                onOpenPulseDetail: navigationModel.openPulseDetail,
                onEditPulse: navigationModel.openPulseEdit
            )
        case .horizon:
            HorizonView(
                viewModel: horizonViewModel,
                onPrepareFlowPlan: { plan in
                    guard flowViewModel.prepareFromPlan(plan) else { return false }
                    navigationModel.openFlow()
                    return true
                },
                onPrepareArcPlan: { plan, restart in
                    guard flowViewModel.prepareFromArcPlan(plan, restart: restart) else { return false }
                    navigationModel.openFlow()
                    return true
                },
                onOpenCurrentFlow: navigationModel.openFlow
            )
        case .shell, .shellRoom(.shellRoot):
            ShellView(
                viewModel: shellViewModel,
                onOpenTheBlue: { navigationModel.openShellRoom(.theBlue) },
                onOpenIdeaGrove: { navigationModel.openShellRoom(.ideaGrove) },
                onOpenStillwater: { navigationModel.openShellRoom(.stillwater) },
                onOpenLookout: { navigationModel.openShellRoom(.lookout) },
                onOpenVoyageHall: { navigationModel.openShellRoom(.voyageHall) },
                onOpenFocusRoom: { navigationModel.openShellRoom(.focusRoom) },
                onOpenChest: { navigationModel.openShellRoom(.chest) },
                onOpenBadges: { navigationModel.openShellRoom(.badges) }
            )
        case .notepad:
            NotepadView()
        case .help:
            HelpView(
                movementController: container.movementController,
                preferences: preferences
            )
        case .flow:
            FlowView(
                viewModel: flowViewModel,
                preferences: preferences,
                onBackToStory: navigationModel.backToStoryRoot,
                onOpenShell: navigationModel.openShell
            )
        case .pulse:
            PulseView(
                viewModel: pulseViewModel,
                activeFlowContext: { flowViewModel.activePulseContext },
                onSaved: {
                    storyViewModel.refresh()
                    navigationModel.backToStoryRoot()
                },
                onCancel: navigationModel.backToStoryRoot
            )
        case .flowDetail(let id), .flowEdit(let id):
            FlowDetailView(
                session: storyViewModel.session(id: id),
                chronicle: UUID(uuidString: id).map(storyViewModel.chronicleForSession),
                childPulses: UUID(uuidString: id).map(storyViewModel.childPulses) ?? [],
                journeySuggestions: storyViewModel.journeys,
                onCreatePulse: { sessionID, title, description, journey in
                    storyViewModel.createHistoricalPulse(
                        parentSessionID: sessionID,
                        title: title,
                        description: description,
                        journeyName: journey
                    )
                },
                onOpenPulse: { navigationModel.openPulseDetail(id: $0.uuidString) },
                onEditPulse: { navigationModel.openPulseEdit(id: $0.uuidString) },
                onDeletePulse: storyViewModel.deletePulse,
                onDeleteFlow: storyViewModel.deleteSession,
                backAccessibilityLabel: navigationModel.storyBackAccessibilityLabel,
                onBackToStory: navigationModel.backInStory
            )
        case .pulseDetail(let id):
            PulseDetailView(
                pulse: storyViewModel.pulse(id: id),
                chronicle: UUID(uuidString: id).map(storyViewModel.chronicleForPulse),
                onEdit: navigationModel.openPulseEdit,
                backAccessibilityLabel: navigationModel.storyBackAccessibilityLabel,
                onBackToStory: navigationModel.backInStory
            )
        case .pulseEdit(let id):
            if let pulseID = UUID(uuidString: id) {
                PulseEditView(
                    pulseID: pulseID,
                    repository: container.repository,
                    backAccessibilityLabel: navigationModel.storyBackAccessibilityLabel,
                    onSaved: {
                        storyViewModel.refresh()
                        navigationModel.backInStory()
                    },
                    onCancel: navigationModel.backInStory
                )
            } else {
                ScyraEmptyState(
                    systemImage: "exclamationmark.triangle",
                    title: "Pulse not found",
                    message: "This Pulse may have been removed."
                )
            }
        case .shellRoom(.ideaGrove):
            IdeaGroveView(
                viewModel: ideaGroveViewModel,
                onStartFlow: { context in
                    if flowViewModel.prepareFromPulse(context) {
                        navigationModel.openFlow()
                    } else if flowViewModel.hasMeaningfulActiveFlow {
                        navigationModel.openFlow()
                    }
                },
                onOpenCurrentFlow: navigationModel.openFlow
            )
        case .shellRoom(.chest):
            ShellChestView(
                viewModel: shellViewModel,
                focusInstanceID: {
                    if case .chest(let id, _) = shellViewModel.pendingDestination { return id }
                    return nil
                }(),
                focusSpeciesID: {
                    if case .chest(_, let speciesID) = shellViewModel.pendingDestination { return speciesID }
                    return nil
                }(),
                onOpenBlue: { navigationModel.openShellRoom(.theBlue) },
                onFocusConsumed: shellViewModel.consumePendingDestination
            )
        case .shellRoom(.badges):
            ShellAchievementsView(
                viewModel: shellViewModel,
                focusBadgeID: {
                    if case .badges(let id, _, _) = shellViewModel.pendingDestination { return id }
                    return nil
                }(),
                focusCollectionID: {
                    if case .badges(_, let collectionID, _) = shellViewModel.pendingDestination { return collectionID }
                    return nil
                }(),
                focusSpeciesID: {
                    if case .badges(_, _, let speciesID) = shellViewModel.pendingDestination { return speciesID }
                    return nil
                }(),
                onNavigate: handleAchievementAction,
                onFocusConsumed: shellViewModel.consumePendingDestination
            )
        case .shellRoom(.stillwater):
            StillwaterView(
                viewModel: shellViewModel,
                onOpenChest: { navigationModel.openShellRoom(.chest) },
                focusCollectionID: {
                    if case .stillwater(let collectionID, _) = shellViewModel.pendingDestination { return collectionID }
                    return nil
                }(),
                focusSpeciesID: {
                    if case .stillwater(_, let speciesID) = shellViewModel.pendingDestination { return speciesID }
                    return nil
                }(),
                onFocusConsumed: shellViewModel.consumePendingDestination
            )
        case .shellRoom(.theBlue):
            TheBlueView(
                viewModel: shellViewModel,
                onOpenChest: { instanceID in
                    shellViewModel.prepareChestFocus(instanceID: instanceID)
                    navigationModel.openShellRoom(.chest)
                },
                focusCollectionID: {
                    if case .blue(let collectionID, _, _) = shellViewModel.pendingDestination { return collectionID }
                    return nil
                }(),
                focusSpeciesID: {
                    if case .blue(_, let speciesID, _) = shellViewModel.pendingDestination { return speciesID }
                    return nil
                }(),
                opensBeyondBlue: {
                    if case .blue(_, _, let opensBeyondBlue) = shellViewModel.pendingDestination { return opensBeyondBlue }
                    return false
                }(),
                onFocusConsumed: shellViewModel.consumePendingDestination
            )
        case .shellRoom(.lookout):
            LookoutView(
                viewModel: lookoutViewModel,
                onLaunchFlow: { journey in
                    if flowViewModel.prepareForJourney(journey) || flowViewModel.hasMeaningfulActiveFlow {
                        navigationModel.openFlow()
                    }
                }
            )
        case .shellRoom(.voyageHall):
            VoyageHallView(viewModel: voyageHallViewModel)
        case .shellRoom(.focusRoom):
            FocusRoomView(viewModel: focusRoomViewModel)
        }
    }

    private func refreshMovementAndStory() async {
        await container.movementController.refreshForeground()
        storyViewModel.refresh()
        shellViewModel.refresh()
    }

    private func openShellNotification(_ notification: ShellNotificationItem) {
        showsShellNotifications = false
        navigationModel.openShellRoom(shellViewModel.openNotification(notification))
    }

    private func handleAchievementAction(_ action: AchievementActionDestination) {
        if let room = shellViewModel.prepareNavigation(action) {
            navigationModel.openShellRoom(room)
            return
        }
        switch action {
        case .flow:
            navigationModel.openFlow()
        case .arc:
            horizonViewModel.requestNewArcPlan()
            navigationModel.openHorizon()
        default:
            break
        }
    }
}

#Preview {
    AppRootView(container: AppDependencyContainer())
}
