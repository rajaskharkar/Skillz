import Combine

@MainActor
final class AppNavigationModel: ObservableObject {
    @Published private(set) var selectedRoute: AppRoute

    init(initialRoute: AppRoute = .story) {
        self.selectedRoute = initialRoute
    }

    func selectTopLevel(_ route: AppRoute) {
        guard route.isTopLevel else {
            assertionFailure("Non-top-level route passed to selectTopLevel")
            return
        }
        selectedRoute = route
    }

    func openStory() {
        selectedRoute = .story
    }

    func openHorizon() {
        selectedRoute = .horizon
    }

    func openShell() {
        selectedRoute = .shell
    }

    func openNotepad() {
        selectedRoute = .notepad
    }

    func openHelp() {
        selectedRoute = .help
    }

    func openFlow() {
        selectedRoute = .flow
    }

    func openPulse() {
        selectedRoute = .pulse
    }

    func openFlowDetail(id: String) {
        selectedRoute = .flowDetail(id: id)
    }

    func openFlowEdit(id: String) {
        selectedRoute = .flowEdit(id: id)
    }

    func openPulseDetail(id: String) {
        selectedRoute = .pulseDetail(id: id)
    }

    func openPulseEdit(id: String) {
        selectedRoute = .pulseEdit(id: id)
    }

    func openShellRoom(_ room: ShellRoomRoute) {
        selectedRoute = .shellRoom(room)
    }

    func backToStoryRoot() {
        selectedRoute = .story
    }
}
