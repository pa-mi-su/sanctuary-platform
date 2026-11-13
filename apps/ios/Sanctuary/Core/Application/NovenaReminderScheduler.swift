import Foundation
#if canImport(UserNotifications)
import UserNotifications
#endif

actor NovenaReminderScheduler {
    static let shared = NovenaReminderScheduler()

    private let morningIdentifier = "sanctuary.novena.digest.morning"
    private let eveningIdentifier = "sanctuary.novena.digest.evening"
    private let morningHour = 8
    private let eveningHour = 20

    func syncDigestReminder(
        activeCommitmentCount: Int,
        novenaEnabled: Bool,
        generalDailyEnabled: Bool
    ) async {
        #if canImport(UserNotifications)
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [morningIdentifier, eveningIdentifier])
        guard novenaEnabled || generalDailyEnabled else { return }
        guard await ensureAuthorizedForNotifications(center: center) else { return }

        if activeCommitmentCount > 0 && novenaEnabled {
            let title = "Continue your novena"
            let body = activeCommitmentCount == 1
                ? "You have a novena in progress. Take a calm moment to continue your prayer in Sanctuary."
                : "You have \(activeCommitmentCount) novenas in progress. Take a calm moment to continue your prayer in Sanctuary."

            await scheduleDailyDigest(
                center: center,
                identifier: morningIdentifier,
                title: title,
                body: body,
                hour: morningHour,
                activeCommitmentCount: activeCommitmentCount
            )
            await scheduleDailyDigest(
                center: center,
                identifier: eveningIdentifier,
                title: title,
                body: body,
                hour: eveningHour,
                activeCommitmentCount: activeCommitmentCount
            )
            return
        }

        guard generalDailyEnabled else { return }

        await scheduleDailyDigest(
            center: center,
            identifier: morningIdentifier,
            title: "Your sanctuary is calling",
            body: "Spend a peaceful moment exploring saints, prayer, and perhaps beginning a novena today.",
            hour: morningHour,
            activeCommitmentCount: 0
        )
        #endif
    }

    #if canImport(UserNotifications)
    private func ensureAuthorizedForNotifications(center: UNUserNotificationCenter) async -> Bool {
        let settings = await notificationSettings(center: center)
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .denied:
            return false
        case .notDetermined:
            return await requestAuthorization(center: center)
        @unknown default:
            return false
        }
    }

    private func scheduleDailyDigest(
        center: UNUserNotificationCenter,
        identifier: String,
        title: String,
        body: String,
        hour: Int,
        activeCommitmentCount: Int
    ) async {
        var components = DateComponents()
        components.hour = hour
        components.minute = 0
        components.second = 0
        components.timeZone = .current

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = [
            "kind": "novena-reminder-digest",
            "inProgressCount": activeCommitmentCount,
        ]

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        _ = try? await add(center: center, request: request)
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

    private func add(center: UNUserNotificationCenter, request: UNNotificationRequest) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            center.add(request) { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }
    #endif
}

// Per-item reminders are local to this device and independent of account-level digests.
import SwiftUI

struct ContentReminderButton: View {
    let kind: String
    let slug: String
    let title: String
    @EnvironmentObject private var localization: LocalizationManager
    @State private var showing = false
    @State private var time = Date()
    @State private var scheduled = false
    @State private var problem: String?
    private var identifier: String { "sanctuary.item.\(kind).\(slug)" }

    var body: some View {
        Button { showing = true } label: {
            Label(localization.t(scheduled ? "itemReminder.edit" : "itemReminder.add"), systemImage: scheduled ? "bell.badge.fill" : "bell")
                .font(AppTheme.rounded(16, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 18).padding(.vertical, 12)
                .background(AppTheme.cardBackgroundSoft, in: Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.25), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .task { await load() }
        .sheet(isPresented: $showing) {
            NavigationStack {
                Form {
                    Section {
                        Text(title).font(.headline)
                        Text(localization.t("itemReminder.body")).font(.footnote)
                        DatePicker(localization.t("itemReminder.time"), selection: $time, displayedComponents: .hourAndMinute)
                        if let problem { Text(problem).foregroundStyle(.red) }
                    }
                    Button(localization.t("itemReminder.save")) { Task { await save() } }
                    if scheduled {
                        Button(localization.t("itemReminder.remove"), role: .destructive) {
                            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
                            scheduled = false; showing = false
                        }
                    }
                }
                .scrollContentBackground(.hidden)
                .background(AppTheme.gradientTop)
                .navigationTitle(localization.t("itemReminder.title"))
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(localization.t("common.cancel")) { showing = false } } }
            }
            .preferredColorScheme(.dark)
            .tint(.white)
            .presentationBackground(AppTheme.gradientTop)
            .presentationDetents([.medium, .large])
        }
    }
    @MainActor private func load() async {
        let requests = await UNUserNotificationCenter.current().pendingNotificationRequests()
        if let trigger = requests.first(where: { $0.identifier == identifier })?.trigger as? UNCalendarNotificationTrigger,
           let date = Calendar.current.date(from: trigger.dateComponents) {
            scheduled = true; time = date
        }
    }
    @MainActor private func save() async {
        let center = UNUserNotificationCenter.current()
        do {
            guard try await center.requestAuthorization(options: [.alert, .sound]) else {
                problem = localization.t("itemReminder.permission"); return
            }
            let pending = await center.pendingNotificationRequests()
            guard scheduled || pending.count < 60 else { problem = localization.t("itemReminder.limit"); return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = localization.t("itemReminder.notification")
            content.sound = .default
            content.userInfo = ["destination": "https://mydailysanctuary.com/\(kind)/\(slug)"]
            let components = Calendar.current.dateComponents([.hour, .minute], from: time)
            try await center.add(UNNotificationRequest(identifier: identifier, content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)))
            scheduled = true; problem = nil; showing = false
        } catch { problem = localization.t("itemReminder.error") }
    }
}
