import Foundation
import Combine
import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications

@MainActor
final class PushNotifications: ObservableObject {
    static let shared = PushNotifications()
    @Published var destination: URL?
    @Published var enabled = UserDefaults.standard.bool(forKey: "sanctuary.announcements")
    static let tokenChanged = Notification.Name("sanctuary.pushTokenChanged")
    private let defaults = UserDefaults.standard
    var installationID: String {
        if let id = defaults.string(forKey: "sanctuary.installation") { return id }
        let id = UUID().uuidString
        defaults.set(id, forKey: "sanctuary.installation")
        return id
    }
    func resetConsent() {
        enabled = false
        defaults.set(false, forKey: "sanctuary.announcements")
        if FirebaseApp.app() != nil { Messaging.messaging().deleteToken { _ in } }
    }
    func setEnabled(_ value: Bool) async {
        var allowed = value
        if value {
            allowed = (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
            if allowed { UIApplication.shared.registerForRemoteNotifications() }
        }
        enabled = allowed
        defaults.set(allowed, forKey: "sanctuary.announcements")
    }
    func sync(api: SanctuaryAPIClient, token: String) async {
        guard FirebaseApp.app() != nil else { return }
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        let authorized = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        if enabled && authorized && Messaging.messaging().apnsToken == nil { UIApplication.shared.registerForRemoteNotifications() }
        // APNs registration is asynchronous; its delegate triggers another sync once ready.
        guard Messaging.messaging().apnsToken != nil,
              let fcm = try? await Messaging.messaging().token() else { return }
        let consent = enabled && authorized
        try? await api.registerPushDevice(request: .init(installationId: installationID, token: fcm, platform: "ios", appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown", enabled: consent), token: token)
    }
    func signOut(api: SanctuaryAPIClient, token: String?) async {
        await setEnabled(false)
        if let token { try? await api.unregisterPushDevice(installationId: installationID, token: token) }
        if FirebaseApp.app() != nil { try? await Messaging.messaging().deleteToken() }
    }
    static func configure() {
        guard FirebaseApp.app() == nil else { return }
        let suffix: String
        switch PlatformConfiguration.current().environment {
        case .dev: suffix = "Dev"
        case .uat: suffix = "UAT"
        case .prod: suffix = "Prod"
        }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info-" + suffix, ofType: "plist"), let options = FirebaseOptions(contentsOfFile: path) else { return }
        FirebaseApp.configure(options: options)
    }
}

final class SanctuaryAppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        MainActor.assumeIsolated { PushNotifications.configure() }
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        return true
    }
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        NotificationCenter.default.post(name: PushNotifications.tokenChanged, object: nil)
    }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        NotificationCenter.default.post(name: PushNotifications.tokenChanged, object: nil)
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let isAnnouncement = notification.request.content.userInfo["messageId"] != nil
        Task { @MainActor in completionHandler(!isAnnouncement || PushNotifications.shared.enabled ? [.banner, .sound] : []) }
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if let destination = response.notification.request.content.userInfo["destination"] as? String,
           let url = URL(string: destination), url.scheme == "https", url.host == "mydailysanctuary.com" {
            Task { @MainActor in PushNotifications.shared.destination = url }
        }
        completionHandler()
    }
}
