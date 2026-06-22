import Testing
@testable import Scyra

struct AppFoundationTests {
    @Test func initialRouteIsHome() {
        let coordinator = AppLaunchCoordinator()

        #expect(coordinator.initialRoute() == .home)
    }

    @Test func appRouteSupportsEqualityAndHashing() {
        #expect(AppRoute.flow == AppRoute.flow)
        #expect(AppRoute.flow != AppRoute.story)
        #expect(Set([AppRoute.home, AppRoute.home, AppRoute.help]).count == 2)
    }

    @Test func dependencyContainerExposesInjectedLaunchCoordinator() {
        let coordinator = AppLaunchCoordinator()
        let container = AppDependencyContainer(appLaunchCoordinator: coordinator)

        #expect(container.appLaunchCoordinator.initialRoute() == .home)
    }
}
