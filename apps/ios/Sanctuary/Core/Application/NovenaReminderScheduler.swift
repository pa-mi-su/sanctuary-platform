import Foundation
#if canImport(UserNotifications)
import UserNotifications
#endif

actor NovenaReminderScheduler {
    static let shared = NovenaReminderScheduler()

    private let morningIdentifier = "sanctuary.novena.digest.morning"
    private let eveningIdentifier = "sanctuary.novena.digest.evening"

    func syncDigestReminder(
        activeCommitmentCount: Int,
        novenaEnabled: Bool,
        generalDailyEnabled: Bool
    ) async {
        #if canImport(UserNotifications)
        // Upgrade cleanup: cancel only old digests, preserving per-item reminders.
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [morningIdentifier, eveningIdentifier])
        #endif
    }


}

// Per-item reminders are local to this device and independent of announcement preferences.
import SwiftUI

struct ContentReminderButton: View {
    let kind: String
    let slug: String
    let title: String
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @State private var accountRequired = false
    @State private var showing = false
    @State private var time = Date()
    @State private var scheduled = false
    @State private var problem: String?
    private var identifier: String { "sanctuary.item.\(kind).\(slug)" }

    var body: some View {
        Button {
            if accountStore.isAuthenticated { showing = true } else { accountRequired = true }
        } label: {
            Label(localization.t(scheduled ? "itemReminder.edit" : "itemReminder.add"), systemImage: scheduled ? "bell.badge.fill" : "bell")
                .font(AppTheme.rounded(16, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 18).padding(.vertical, 12)
                .background(AppTheme.cardBackgroundSoft, in: Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.25), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accountRequiredPrompt(isPresented: $accountRequired, onAuthenticated: { showing = true })
        .onChange(of: accountStore.isAuthenticated) { authenticated in if !authenticated { showing = false } }
        .task { await load() }
        .onChange(of: showing) { visible in if visible { Task { await load() } } }
        .sheet(isPresented: $showing) {
            NavigationStack {
                Form {
                    Section {
                        Text(title).font(.headline)
                        Text(localization.t("itemReminder.body")).font(.footnote)
                        DatePicker(localization.t("itemReminder.time"), selection: $time, displayedComponents: .hourAndMinute)
                        if let problem { Text(problem).foregroundStyle(.red) }
                    }
                    Section {
                        ReminderActionButton(title: localization.t("itemReminder.save"), symbol: "checkmark.circle.fill") { Task { await save() } }
                        if scheduled {
                            ReminderActionButton(title: localization.t("itemReminder.remove"), symbol: "trash", destructive: true) {
                                let center = UNUserNotificationCenter.current()
                                center.removePendingNotificationRequests(withIdentifiers: [identifier])
                                center.removeDeliveredNotifications(withIdentifiers: [identifier])
                                scheduled = false; showing = false
                            }
                        }
                    }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
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
        scheduled = false
        if let trigger = requests.first(where: { $0.identifier == identifier })?.trigger as? UNCalendarNotificationTrigger,
           let date = Calendar.current.date(from: trigger.dateComponents) {
            scheduled = true; time = date
        }
    }
    @MainActor private func save() async {
        guard accountStore.isAuthenticated else { showing = false; accountRequired = true; return }
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


struct ReminderActionButton: View {
    let title: String
    let symbol: String
    var destructive = false
    let action: () -> Void
    var body: some View {
        Button(role: destructive ? .destructive : nil, action: action) {
            Label(title, systemImage: symbol)
                .font(AppTheme.rounded(16, weight: .bold))
                .frame(maxWidth: .infinity, minHeight: 50)
                .foregroundStyle(destructive ? Color(red: 1, green: 0.65, blue: 0.65) : .white)
                .background(destructive ? Color.red.opacity(0.12) : Color(red: 0.13, green: 0.48, blue: 0.40), in: RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(destructive ? Color.red.opacity(0.65) : Color.white.opacity(0.35), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
