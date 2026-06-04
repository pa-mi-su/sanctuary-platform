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
        let apiClient = SanctuaryAPIClient(baseURL: platformConfiguration.apiBaseURL, session: apiSession())
        let contentRepository = APIContentRepository(apiClient: apiClient)
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

    private static func apiSession() -> URLSession {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 25
        configuration.waitsForConnectivity = true
        return URLSession(configuration: configuration)
    }
}
