import Testing
@testable import Scyra

@MainActor
struct AppFoundationTests {
    @Test func initialRouteIsStory() {
        let coordinator = AppLaunchCoordinator()

        #expect(coordinator.initialRoute() == .story)
    }

    @Test func appRouteSupportsEqualityAndHashing() {
        #expect(AppRoute.flow == AppRoute.flow)
        #expect(AppRoute.flow != AppRoute.story)
        #expect(Set([AppRoute.story, AppRoute.story, AppRoute.help]).count == 2)
    }

    @Test func dependencyContainerExposesInjectedLaunchCoordinator() {
        let coordinator = AppLaunchCoordinator()
        let container = AppDependencyContainer(appLaunchCoordinator: coordinator)

        #expect(container.appLaunchCoordinator.initialRoute() == .story)
    }

    @Test func rootTopBarActionOrderMatchesScyraNavigation() {
        #expect(AppRoute.rootTopBarActions == [.story, .horizon, .shell, .notepad, .help])
    }

    @Test func rootTopBarActionsAreMarkedAsRootActions() {
        #expect(AppRoute.rootTopBarActions.allSatisfy { $0.isTopLevel })
    }

    @Test func nonTopBarRoutesAreNotMarkedAsRootActions() {
        #expect(AppRoute.flow.isTopLevel == false)
    }

    @Test func shellRoomsReturnToShellRoot() {
        let navigation = AppNavigationModel(initialRoute: .shellRoom(.ideaGrove))
        navigation.backToRouteRoot()
        #expect(navigation.selectedRoute == .shell)
        #expect(AppRoute.shellRoom(.ideaGrove).usesRootBackAffordance)
    }

    @Test func typedShellRootIsARealShellRoute() {
        let route = AppRoute.shellRoom(.shellRoot)
        #expect(route.isShellRoute)
        #expect(route.matchesTopBarAction(.shell))
        #expect(ShellRoomRoute.shellRoot.title == "Shell")
    }

    @Test func storyChildNavigationReturnsThroughItsPresentingContext() {
        let navigation = AppNavigationModel()

        navigation.openFlowDetail(id: "flow")
        navigation.openPulseDetail(id: "pulse")
        navigation.openPulseEdit(id: "pulse")

        #expect(navigation.storyBackAccessibilityLabel == "Back to Pulse details")
        navigation.backInStory()
        #expect(navigation.selectedRoute == .pulseDetail(id: "pulse"))
        #expect(navigation.storyBackAccessibilityLabel == "Back to Flow details")
        navigation.backInStory()
        #expect(navigation.selectedRoute == .flowDetail(id: "flow"))
        #expect(navigation.storyBackAccessibilityLabel == "Back to Story")
        navigation.backInStory()
        #expect(navigation.selectedRoute == .story)
    }

    @Test func leavingStoryClearsNestedNavigationHistory() {
        let navigation = AppNavigationModel()

        navigation.openFlowDetail(id: "flow")
        navigation.openPulseDetail(id: "pulse")
        navigation.openShell()
        navigation.openPulseEdit(id: "other-pulse")
        navigation.backInStory()

        #expect(navigation.selectedRoute == .story)
    }

    @Test func routeDisplayTitlesAreStable() {
        #expect(AppRoute.story.display.title == "Story")
        #expect(AppRoute.horizon.display.title == "Horizon")
        #expect(AppRoute.shell.display.title == "Shell")
        #expect(AppRoute.notepad.display.title == "Notepad")
        #expect(AppRoute.help.display.title == "Help")
    }
}
