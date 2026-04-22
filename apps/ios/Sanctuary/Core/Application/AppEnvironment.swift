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
        let localContentRepository = LocalContentRepository(fallbackToSeed: false)

        // Saints now come from the API first while legacy domains continue to read locally.
        let contentRepository = HybridContentRepository(
            apiClient: apiClient,
            localRepository: localContentRepository
        )
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
