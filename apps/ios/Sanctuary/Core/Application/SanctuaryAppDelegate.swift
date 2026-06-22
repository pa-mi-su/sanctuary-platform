import Foundation
#if canImport(FirebaseMessaging) && canImport(UIKit) && canImport(UserNotifications)
import FirebaseMessaging
import UIKit
import UserNotifications
#endif

#if canImport(UIKit)
final class SanctuaryAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let configuration = PlatformConfiguration.current()
        PushNotificationRegistrar.configureFirebaseIfAvailable(platformConfiguration: configuration)

        #if canImport(UserNotifications)
        UNUserNotificationCenter.current().delegate = self
        #endif

        #if canImport(FirebaseMessaging)
        Messaging.messaging().delegate = self
        #endif

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        #if canImport(FirebaseMessaging)
        Messaging.messaging().apnsToken = deviceToken
        #endif

        PushNotificationRegistrar.markAPNSTokenAvailable()
    }
}

#if canImport(FirebaseMessaging)
extension SanctuaryAppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        // The authenticated session store registers the current token after sign-in/bootstrap.
    }
}
#endif

#if canImport(UserNotifications)
extension SanctuaryAppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound, .badge]
    }
}
#endif
#endif
