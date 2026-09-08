import Combine
import Foundation

@MainActor
final class AppNavigationModel: ObservableObject {
    @Published private(set) var selectedRoute: AppRoute
    private var storyRouteStack: [AppRoute] = []

    init(initialRoute: AppRoute = .story) {
        self.selectedRoute = initialRoute
    }

    func selectTopLevel(_ route: AppRoute) {
        guard route.isTopLevel else {
            assertionFailure("Non-top-level route passed to selectTopLevel")
            return
        }
        storyRouteStack.removeAll()
        selectedRoute = route
    }

    func openStory() {
        storyRouteStack.removeAll()
        selectedRoute = .story
    }

    func openHorizon() {
        storyRouteStack.removeAll()
        selectedRoute = .horizon
    }

    func openShell() {
        storyRouteStack.removeAll()
        selectedRoute = .shell
    }

    func openNotepad() {
        storyRouteStack.removeAll()
        selectedRoute = .notepad
    }

    func openHelp() {
        storyRouteStack.removeAll()
        selectedRoute = .help
    }

    func openFlow() {
        storyRouteStack.removeAll()
        selectedRoute = .flow
    }

    @discardableResult
    func openDeepLink(_ url: URL) -> Bool {
        guard FlowDeepLinkCoordinator.isFlowURL(url) else { return false }
        openFlow()
        return true
    }

    func openPulse() {
        storyRouteStack.removeAll()
        selectedRoute = .pulse
    }

    func openFlowDetail(id: String) {
        pushStoryRoute(.flowDetail(id: id))
    }

    func openFlowEdit(id: String) {
        pushStoryRoute(.flowEdit(id: id))
    }

    func openPulseDetail(id: String) {
        pushStoryRoute(.pulseDetail(id: id))
    }

    func openPulseEdit(id: String) {
        pushStoryRoute(.pulseEdit(id: id))
    }

    func openShellRoom(_ room: ShellRoomRoute) {
        storyRouteStack.removeAll()
        selectedRoute = .shellRoom(room)
    }

    func backToStoryRoot() {
        storyRouteStack.removeAll()
        selectedRoute = .story
    }

    func backInStory() {
        selectedRoute = storyRouteStack.popLast() ?? .story
    }

    var storyBackAccessibilityLabel: String {
        switch storyRouteStack.last {
        case .flowDetail, .flowEdit:
            "Back to Flow details"
        case .pulseDetail, .pulseEdit:
            "Back to Pulse details"
        default:
            "Back to Story"
        }
    }

    func backToRouteRoot() {
        storyRouteStack.removeAll()
        if case .shellRoom = selectedRoute {
            selectedRoute = .shell
        } else {
            selectedRoute = .story
        }
    }

    private func pushStoryRoute(_ route: AppRoute) {
        if selectedRoute != route {
            storyRouteStack.append(selectedRoute.isStoryRoute ? selectedRoute : .story)
        }
        selectedRoute = route
    }
}

private extension AppRoute {
    var isStoryRoute: Bool {
        switch self {
        case .story, .flowDetail, .flowEdit, .pulseDetail, .pulseEdit:
            true
        default:
            false
        }
    }
}
