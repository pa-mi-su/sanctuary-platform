import Foundation

@main
enum VerifyPlatformConfiguration {
    static func main() throws {
        try assertConfiguration(
            bundleIdentifier: "com.pamisu.Sanctuary.dev",
            configuredAPIBaseURL: nil,
            expectedEnvironment: .dev,
            expectedAPIBaseURL: "https://dev-api.mydailysanctuary.com"
        )

        try assertConfiguration(
            bundleIdentifier: "com.pamisu.Sanctuary.dev",
            configuredAPIBaseURL: "https://dev-api.mydailysanctuary.com",
            expectedEnvironment: .dev,
            expectedAPIBaseURL: "https://dev-api.mydailysanctuary.com"
        )

        try assertConfiguration(
            bundleIdentifier: "com.pamisu.Sanctuary",
            configuredAPIBaseURL: nil,
            expectedEnvironment: .prod,
            expectedAPIBaseURL: "https://api.mydailysanctuary.com"
        )

        print("Platform configuration verification passed.")
    }

    private static func assertConfiguration(
        bundleIdentifier: String,
        configuredAPIBaseURL: String?,
        expectedEnvironment: PlatformEnvironment,
        expectedAPIBaseURL: String
    ) throws {
        let bundle = try makeBundle(identifier: bundleIdentifier, apiBaseURL: configuredAPIBaseURL)
        let configuration = PlatformConfiguration.current(bundle: bundle)

        guard configuration.environment == expectedEnvironment else {
            throw VerificationError(
                "Expected \(bundleIdentifier) to resolve environment \(expectedEnvironment.rawValue), got \(configuration.environment.rawValue)."
            )
        }

        guard configuration.apiBaseURL.absoluteString == expectedAPIBaseURL else {
            throw VerificationError(
                "Expected \(bundleIdentifier) to resolve API \(expectedAPIBaseURL), got \(configuration.apiBaseURL.absoluteString)."
            )
        }
    }

    private static func makeBundle(identifier: String, apiBaseURL: String?) throws -> Bundle {
        let bundleURL = URL(fileURLWithPath: NSTemporaryDirectory())
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("bundle")

        try FileManager.default.createDirectory(at: bundleURL, withIntermediateDirectories: true)

        var plist: [String: Any] = [
            "CFBundleExecutable": "Sanctuary",
            "CFBundleIdentifier": identifier,
            "CFBundlePackageType": "BNDL"
        ]

        if let apiBaseURL {
            plist["SanctuaryAPIBaseURL"] = apiBaseURL
        }

        let plistData = try PropertyListSerialization.data(fromPropertyList: plist, format: .xml, options: 0)
        try plistData.write(to: bundleURL.appendingPathComponent("Info.plist"))

        guard let bundle = Bundle(url: bundleURL) else {
            throw VerificationError("Could not create test bundle for \(identifier).")
        }

        return bundle
    }
}

struct VerificationError: Error, CustomStringConvertible {
    let description: String

    init(_ description: String) {
        self.description = description
    }
}
