import Foundation

struct AppEnvironment {
    let platformConfiguration: PlatformConfiguration
    let apiClient: SanctuaryAPIClient
    let contentRepository: any ContentRepository
    let searchRepository: any SearchRepository
    let churchNewsRepository: any ChurchNewsRepository

    func makeUserProgressRepository(sessionStore: AccountSessionStore) -> any UserProgressRepository {
        RemoteUserProgressRepository(apiClient: apiClient, sessionStore: sessionStore)
    }

    static func current() -> AppEnvironment {
        let platformConfiguration = PlatformConfiguration.current()
        let apiClient = SanctuaryAPIClient(baseURL: platformConfiguration.apiBaseURL, session: apiSession())
        let contentRepository = APIContentRepository(apiClient: apiClient)
        let searchRepository = LocalSearchRepository(contentRepository: contentRepository)
        let churchNewsRepository = APIChurchNewsRepository(apiClient: apiClient)

        return AppEnvironment(
            platformConfiguration: platformConfiguration,
            apiClient: apiClient,
            contentRepository: contentRepository,
            searchRepository: searchRepository,
            churchNewsRepository: churchNewsRepository
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
