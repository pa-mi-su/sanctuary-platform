import Foundation
import Combine
import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
import OSLog

@MainActor
final class PushNotifications: ObservableObject {
    static let shared = PushNotifications()
    @Published var destination: URL?
    @Published var registrationStatus = ""
    private let credentialStore = KeychainStore(service: "sanctuary.push." + (Bundle.main.bundleIdentifier ?? "app"))
    @Published private(set) var enabled = false
    @Published private(set) var needsInvitation = false
    @Published var registrationProblem: String?
    private let consent: AnnouncementConsent
    private static let logger = Logger(subsystem: "app.sanctuary.push", category: "registration")
    private init() {
        let store = KeychainStore(service: "sanctuary.push." + (Bundle.main.bundleIdentifier ?? "app"))
        consent = AnnouncementConsent(defaults: .standard, backup: try? store.load(account: "announcement-consent")) {
            try store.save($0, for: "announcement-consent")
        }
        enabled = consent.choice == true
        needsInvitation = consent.needsInvitation
    }
    func declineInvitation() {
        do { try consent.choose(false); enabled = false; needsInvitation = false }
        catch { registrationProblem = "Your notification choice could not be saved. Please try again." }
    }
    static let tokenChanged = Notification.Name("sanctuary.pushTokenChanged")
    private let defaults = UserDefaults.standard
    var installationID: String {
        if let id = defaults.string(forKey: "sanctuary.installation") {
            try? credentialStore.save(Data(id.utf8), for: "installation-id")
            return id
        }
        if let data = try? credentialStore.load(account: "installation-id"), let id = String(data: data, encoding: .utf8) {
            defaults.set(id, forKey: "sanctuary.installation")
            return id
        }
        let id = UUID().uuidString
        defaults.set(id, forKey: "sanctuary.installation")
        try? credentialStore.save(Data(id.utf8), for: "installation-id")
        return id
    }
    private func installationSecret() throws -> String {
        if let data = try credentialStore.load(account: "installation-secret"), let secret = String(data: data, encoding: .utf8) { return secret }
        let secret = UUID().uuidString.replacingOccurrences(of: "-", with: "") + UUID().uuidString.replacingOccurrences(of: "-", with: "")
        try credentialStore.save(Data(secret.utf8), for: "installation-secret")
        return secret
    }
    func setEnabled(_ value: Bool) async {
        var allowed = value
        if value {
            allowed = (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
            if allowed { UIApplication.shared.registerForRemoteNotifications() }
        }
        if value { defaults.set(true, forKey: "sanctuary.badgeAuthorizationRequested") }
        do {
            try consent.choose(allowed)
            enabled = allowed
            needsInvitation = false
            registrationProblem = value && !allowed ? "Allow notifications in phone settings to receive Sanctuary alerts." : nil
        } catch {
            enabled = consent.choice == true
            registrationProblem = "Your notification choice could not be saved. Please try again."
        }
    }
    func sync(api: SanctuaryAPIClient, token: String?) async {
        let savedRevision = (try? credentialStore.load(account: "registration-revision")).flatMap { String(data: $0, encoding: .utf8) }.flatMap(Int.init) ?? 0
        let revision = max(defaults.integer(forKey: "sanctuary.pushRevision"), savedRevision) + 1
        try? credentialStore.save(Data(String(revision).utf8), for: "registration-revision")
        defaults.set(revision, forKey: "sanctuary.pushRevision")
        if !enabled && !defaults.bool(forKey: "sanctuary.pushRegistered") { registrationStatus = "Announcements are off."; return }
        guard FirebaseApp.app() != nil else { registrationStatus = "Notification configuration is unavailable."; return }
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        let authorized = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        // Upgrade existing opt-ins once. Subsequent syncs respect the person's OS badge settings.
        if enabled && authorized && !defaults.bool(forKey: "sanctuary.badgeAuthorizationRequested") {
            _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
            defaults.set(true, forKey: "sanctuary.badgeAuthorizationRequested")
        }
        if enabled && authorized && Messaging.messaging().apnsToken == nil { UIApplication.shared.registerForRemoteNotifications() }
        // APNs registration is asynchronous; its delegate triggers another sync once ready.
        guard Messaging.messaging().apnsToken != nil else {
            registrationStatus = enabled ? "Waiting for Apple notification registration…" : "Announcements are off."
            return
        }
        let consent = enabled && authorized
        do {
            let fcm = try await Messaging.messaging().token()
            try await api.registerPushDevice(request: .init(installationId: installationID, token: fcm, platform: "ios", appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown", enabled: consent, installationSecret: try installationSecret(), revision: revision), token: token)
            defaults.set(true, forKey: "sanctuary.pushRegistered")
            registrationStatus = consent ? "Announcements enabled" + (token == nil ? " · no account required" : "") : "Announcements are off."
            registrationProblem = enabled && !authorized ? "Allow notifications in phone settings to receive Sanctuary alerts." : nil
        } catch {
            registrationStatus = "Could not sync notifications. Try again with an internet connection."
            registrationProblem = "Alerts could not connect. We’ll try again automatically when you return to the app."
            let status: Int
            if case SanctuaryAPIError.serverStatus(let code, _) = error { status = code } else { status = 0 }
            Self.logger.error("Push registration failed: \(String(describing: type(of: error)), privacy: .public) status=\(status)")
        }
    }
    // The icon badge represents unacknowledged delivered phone alerts, not the inbox total.
    // Only remote announcements are removed; scheduled and delivered local reminders stay intact.
    func acknowledgeAnnouncementAlerts() async {
        let center = UNUserNotificationCenter.current()
        let delivered = await center.deliveredNotifications()
        let identifiers = delivered.filter { $0.request.trigger is UNPushNotificationTrigger }.map { $0.request.identifier }
        center.removeDeliveredNotifications(withIdentifiers: identifiers)
        try? await center.setBadgeCount(0)
    }
    func clearStaleIconBadge() async {
        let center = UNUserNotificationCenter.current()
        let delivered = await center.deliveredNotifications()
        if !delivered.contains(where: { $0.request.trigger is UNPushNotificationTrigger }) {
            try? await center.setBadgeCount(0)
        }
    }
    func signOut(api: SanctuaryAPIClient) async {
        // Detach identity while preserving this installation's separate announcement consent.
        await sync(api: api, token: nil)
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
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        Task { @MainActor in PushNotifications.shared.registrationStatus = "Apple notification registration failed. Check notification settings and try refreshing." }
    }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        NotificationCenter.default.post(name: PushNotifications.tokenChanged, object: nil)
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let isAnnouncement = notification.request.content.userInfo["messageId"] != nil
        Task { @MainActor in
            // Local reminders remain in Notification Center even when received in the foreground,
            // matching Android's persistent notification tray. Announcement consent is separate.
            let presentation: UNNotificationPresentationOptions = isAnnouncement ? [.banner, .sound, .badge] : [.banner, .list, .sound]
            completionHandler(!isAnnouncement || PushNotifications.shared.enabled ? presentation : [])
        }
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if let destination = response.notification.request.content.userInfo["destination"] as? String,
           let url = URL(string: destination), url.scheme == "https", (url.host == "mydailysanctuary.com" || (PlatformEnvironment.current() == .dev && url.host == "dev.mydailysanctuary.com")) {
            Task { @MainActor in PushNotifications.shared.destination = url }
        }
        completionHandler()
    }
}
