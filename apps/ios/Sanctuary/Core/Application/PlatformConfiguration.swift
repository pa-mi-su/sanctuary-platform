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
    let environment: PlatformEnvironment
    let apiBaseURL: URL
    let authenticationEnabled: Bool

    static func current(
        bundle: Bundle = .main,
        processInfo: ProcessInfo = .processInfo
    ) -> PlatformConfiguration {
        let environment = PlatformEnvironment.current(bundle: bundle)
        let apiBaseURL = resolveAPIBaseURL(environment: environment, processInfo: processInfo)

        return PlatformConfiguration(
            environment: environment,
            apiBaseURL: apiBaseURL,
            authenticationEnabled: true
        )
    }

    private static func resolveAPIBaseURL(
        environment: PlatformEnvironment,
        processInfo: ProcessInfo
    ) -> URL {
        if let override = processInfo.environment["SANCTUARY_API_BASE_URL"],
           let url = URL(string: override),
           let scheme = url.scheme,
           ["http", "https"].contains(scheme.lowercased()) {
            return url
        }

        #if targetEnvironment(simulator)
            if environment != .prod {
                return URL(string: "http://localhost:8080")!
            }
        #endif

        // We currently operate one live API stack. Non-production iOS targets can
        // override this via SANCTUARY_API_BASE_URL when a dedicated backend exists.
        return URL(string: "https://sa-d7fe5f77e3bd409caf712e69b701f1e8.ecs.us-east-1.on.aws")!
    }
}
