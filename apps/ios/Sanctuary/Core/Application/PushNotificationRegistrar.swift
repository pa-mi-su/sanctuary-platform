import Foundation
#if canImport(FirebaseCore) && canImport(FirebaseMessaging) && canImport(UserNotifications) && canImport(UIKit)
import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
#endif

@MainActor
final class PushNotificationRegistrar {
    private static var apnsTokenAvailable = false

    private let apiClient: SanctuaryAPIClient
    private let platformConfiguration: PlatformConfiguration

    init(apiClient: SanctuaryAPIClient, platformConfiguration: PlatformConfiguration) {
        self.apiClient = apiClient
        self.platformConfiguration = platformConfiguration
    }

    func registerCurrentDevice(token: String, preferredLanguage: ContentLocale?) async {
        #if canImport(FirebaseCore) && canImport(FirebaseMessaging) && canImport(UserNotifications) && canImport(UIKit)
        guard FirebaseApp.app() != nil else { return }

        let notificationsEnabled = await requestNotificationAuthorization()
        guard let fcmToken = await firebaseTokenWithRetry(), !fcmToken.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return
        }

        do {
            _ = try await apiClient.registerDevice(
                request: APIUserDeviceRegistrationRequest(
                    fcmToken: fcmToken,
                    platform: "ios",
                    appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
                    language: preferredLanguage?.rawValue ?? "en",
                    notificationsEnabled: notificationsEnabled
                ),
                token: token
            )
        } catch {
            // Device registration must not block sign-in or normal app usage.
        }
        #endif
    }

    func notificationPermissionEventName() async -> String {
        #if canImport(UserNotifications)
        let settings = await notificationSettings(center: UNUserNotificationCenter.current())
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return "notification_permission_allowed"
        case .notDetermined, .denied:
            return "notification_permission_denied"
        @unknown default:
            return "notification_permission_denied"
        }
        #else
        return "notification_permission_denied"
        #endif
    }

    func anonymousPushTokenIfAvailable() async -> (token: String?, notificationsEnabled: Bool) {
        #if canImport(FirebaseCore) && canImport(FirebaseMessaging) && canImport(UserNotifications) && canImport(UIKit)
        guard FirebaseApp.app() != nil else { return (nil, false) }

        let notificationsEnabled = await requestNotificationAuthorization()
        let token = await firebaseTokenWithRetry()?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (token?.isEmpty == false ? token : nil, notificationsEnabled)
        #else
        return (nil, false)
        #endif
    }

    static func configureFirebaseIfAvailable(platformConfiguration: PlatformConfiguration) {
        #if canImport(FirebaseCore)
        guard FirebaseApp.app() == nil,
              let options = FirebaseOptions(contentsOfFile: firebasePlistPath(for: platformConfiguration.environment))
        else {
            return
        }

        FirebaseApp.configure(options: options)
        #endif
    }

    static func markAPNSTokenAvailable() {
        apnsTokenAvailable = true
    }

    private static func firebasePlistPath(for environment: PlatformEnvironment) -> String {
        let resourceName: String
        switch environment {
        case .dev:
            resourceName = "GoogleService-Info-Dev"
        case .uat:
            resourceName = "GoogleService-Info-UAT"
        case .prod:
            resourceName = "GoogleService-Info-Prod"
        }

        return Bundle.main.path(forResource: resourceName, ofType: "plist")
            ?? Bundle.main.path(forResource: resourceName, ofType: "plist", inDirectory: "Resources/Firebase")
            ?? Bundle.main.path(forResource: resourceName, ofType: "plist", inDirectory: "Firebase")
            ?? ""
    }

    #if canImport(FirebaseCore) && canImport(FirebaseMessaging) && canImport(UserNotifications) && canImport(UIKit)
    private func requestNotificationAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await notificationSettings(center: center)

        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            UIApplication.shared.registerForRemoteNotifications()
            return true
        case .notDetermined:
            let granted = await requestAuthorization(center: center)
            if granted {
                UIApplication.shared.registerForRemoteNotifications()
            }
            return granted
        case .denied:
            return false
        @unknown default:
            return false
        }
    }

    private func firebaseTokenWithRetry() async -> String? {
        for attempt in 0..<8 {
            if attempt > 0 {
                try? await Task.sleep(nanoseconds: 500_000_000)
            }

            guard Self.apnsTokenAvailable else {
                continue
            }

            if let token = await firebaseToken(), !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return token
            }
        }

        return nil
    }

    private func firebaseToken() async -> String? {
        await withCheckedContinuation { continuation in
            Messaging.messaging().token { token, _ in
                continuation.resume(returning: token)
            }
        }
    }

    private func notificationSettings(center: UNUserNotificationCenter) async -> UNNotificationSettings {
        await withCheckedContinuation { continuation in
            center.getNotificationSettings { settings in
                continuation.resume(returning: settings)
            }
        }
    }

    private func requestAuthorization(center: UNUserNotificationCenter) async -> Bool {
        await withCheckedContinuation { continuation in
            center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                continuation.resume(returning: granted)
            }
        }
    }
    #endif
}
