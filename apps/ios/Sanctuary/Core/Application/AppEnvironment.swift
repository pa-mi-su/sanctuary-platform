import Foundation

struct AppEnvironment {
    let platformConfiguration: PlatformConfiguration
    let apiClient: SanctuaryAPIClient
    let contentRepository: any ContentRepository
    let searchRepository: any SearchRepository

    func makeUserProgressRepository(sessionStore: AccountSessionStore) -> any UserProgressRepository {
        RemoteUserProgressRepository(apiClient: apiClient, sessionStore: sessionStore)
    }

    static func current() -> AppEnvironment {
        let platformConfiguration = PlatformConfiguration.current()
        let apiClient = SanctuaryAPIClient(baseURL: platformConfiguration.apiBaseURL)

        // Avoid eager large in-memory seed payload allocation at startup.
        let contentRepository = LocalContentRepository(fallbackToSeed: false)
        let searchRepository = LocalSearchRepository(contentRepository: contentRepository)

        return AppEnvironment(
            platformConfiguration: platformConfiguration,
            apiClient: apiClient,
            contentRepository: contentRepository,
            searchRepository: searchRepository
        )
    }

    static func local() -> AppEnvironment {
        current()
    }
}
