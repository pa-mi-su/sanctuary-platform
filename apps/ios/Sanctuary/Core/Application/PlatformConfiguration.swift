import Foundation

enum PlatformEnvironment: String, Sendable {
    case dev
    case uat
    case prod

    static func current(bundle: Bundle = .main) -> PlatformEnvironment {
        let bundleIdentifier = bundle.bundleIdentifier ?? ""

        if bundleIdentifier.hasSuffix(".dev") {
            return .dev
        }

        if bundleIdentifier.hasSuffix(".uat") {
            return .uat
        }

        return .prod
    }
}

struct PlatformConfiguration: Sendable {
    private static let apiBaseURLInfoKey = "SanctuaryAPIBaseURL"
    private static let devAPIBaseURL = "https://dev-api.mydailysanctuary.com"
    private static let productionAPIBaseURL = "https://api.mydailysanctuary.com"

    let environment: PlatformEnvironment
    let apiBaseURL: URL
    let authenticationEnabled: Bool

    static func current(
        bundle: Bundle = .main,
        processInfo: ProcessInfo = .processInfo
    ) -> PlatformConfiguration {
        let environment = PlatformEnvironment.current(bundle: bundle)
        let apiBaseURL = resolveAPIBaseURL(bundle: bundle, environment: environment, processInfo: processInfo)

        return PlatformConfiguration(
            environment: environment,
            apiBaseURL: apiBaseURL,
            authenticationEnabled: true
        )
    }

    private static func resolveAPIBaseURL(
        bundle: Bundle = .main,
        environment: PlatformEnvironment,
        processInfo: ProcessInfo
    ) -> URL {
        if let override = processInfo.environment["SANCTUARY_API_BASE_URL"],
           let url = URL(string: override),
           let scheme = url.scheme,
           ["http", "https"].contains(scheme.lowercased()) {
            return url
        }

        if let configuredBaseURL = bundle.object(forInfoDictionaryKey: apiBaseURLInfoKey) as? String,
           let url = URL(string: configuredBaseURL),
           let scheme = url.scheme,
           ["http", "https"].contains(scheme.lowercased()) {
            return url
        }

        return defaultAPIBaseURL(for: environment)
    }

    private static func defaultAPIBaseURL(for environment: PlatformEnvironment) -> URL {
        switch environment {
        case .dev:
            return URL(string: devAPIBaseURL)!
        case .uat, .prod:
            return URL(string: productionAPIBaseURL)!
        }
    }
}
