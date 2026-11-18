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

// Account definitions are synchronized; each phone schedules them at its own local wall-clock time.
import SwiftUI
import Combine


@MainActor
final class AccountReminders: ObservableObject {
    static let shared = AccountReminders()
    @Published private(set) var items: [AccountContentReminder] = []
    @Published private(set) var problem: String?
    @Published private(set) var syncing = false
    @Published private(set) var updating = false
    @Published private(set) var legacyNotice = UserDefaults.standard.bool(forKey: "reminders.legacyNotice")
    func dismissLegacyNotice() { legacyNotice = false; UserDefaults.standard.set(false, forKey: "reminders.legacyNotice") }
    private var owner: String? = UserDefaults.standard.string(forKey: "reminders.accountOwner")
    private var generation = 0
    private var refreshingGeneration: Int?
    private var queue: Task<Void, Error>?

    private init() {
        if owner != nil, let data = UserDefaults.standard.data(forKey: "reminders.accountCache"),
           let saved = try? JSONDecoder().decode([AccountContentReminder].self, from: data) { items = saved }
    }

    func signOut() {
        owner = nil; generation += 1; items = []; problem = nil
        UserDefaults.standard.removeObject(forKey: "reminders.accountOwner")
        UserDefaults.standard.removeObject(forKey: "reminders.accountCache")
        let previous = queue
        queue = Task {
            _ = await previous?.result
            let center = UNUserNotificationCenter.current()
            let ids = await center.pendingNotificationRequests().filter { $0.identifier.hasPrefix("sanctuary.item.") }.map(\.identifier)
            if ids.contains(where: { $0.hasPrefix("sanctuary.item.prayers.") || $0.hasPrefix("sanctuary.item.novenas.") }) {
                self.legacyNotice = true; UserDefaults.standard.set(true, forKey: "reminders.legacyNotice")
            }
            center.removePendingNotificationRequests(withIdentifiers: ids)
            let delivered = await center.deliveredNotifications().filter { $0.request.identifier.hasPrefix("sanctuary.item.") }.map { $0.request.identifier }
            center.removeDeliveredNotifications(withIdentifiers: delivered)
        }
    }

    func refresh(account: AccountSessionStore) async {
        guard account.hasBootstrapped, account.status != .loading else { return }
        guard account.isAuthenticated, let userID = account.profile?.userID else { signOut(); return }
        if owner != userID { signOut(); owner = userID; UserDefaults.standard.set(userID, forKey: "reminders.accountOwner") }
        let refreshVersion = generation
        guard refreshingGeneration != refreshVersion else { return }
        refreshingGeneration = refreshVersion
        defer { if refreshingGeneration == refreshVersion { refreshingGeneration = nil } }
        do { try await perform(account: account, mutation: nil) } catch { /* Keep last known account schedule when offline. */ }
    }

    func save(_ reminder: AccountContentReminder, account: AccountSessionStore) async throws {
        let allowed = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
        guard allowed else { throw SanctuaryAPIError.server(message: "Enable Sanctuary notifications in phone settings first.") }
        try await perform(account: account) { api, token in try await api.saveContentReminder(reminder, token: token) }
    }

    func remove(_ reminder: AccountContentReminder, account: AccountSessionStore) async throws {
        try await perform(account: account) { api, token in try await api.deleteContentReminder(kind: reminder.kind, slug: reminder.slug, token: token) }
    }
    func clear(account: AccountSessionStore) async throws {
        try await perform(account: account) { api, token in try await api.clearContentReminders(token: token) }
    }

    private func perform(account: AccountSessionStore, mutation: ((SanctuaryAPIClient, String) async throws -> Void)?) async throws {
        guard account.isAuthenticated, let userID = account.profile?.userID else { throw SanctuaryAPIError.missingAccessToken }
        if owner != userID { signOut(); owner = userID; UserDefaults.standard.set(userID, forKey: "reminders.accountOwner") }
        let version = generation
        let previous = queue
        let task = Task { @MainActor in
            _ = await previous?.result
            guard self.generation == version, account.isAuthenticated, account.profile?.userID == userID else { throw CancellationError() }
            self.syncing = true
            self.updating = mutation != nil
            defer { self.syncing = false; self.updating = false }
            do {
                if let mutation { try await account.reminderRequest { api, token in
                    guard self.generation == version, account.profile?.userID == userID else { throw CancellationError() }
                    try await mutation(api, token)
                } }
                let records = try await account.reminderRequest { api, token in
                    guard self.generation == version, account.profile?.userID == userID else { throw CancellationError() }
                    return try await api.contentReminders(token: token)
                }
                guard self.generation == version, account.isAuthenticated, account.profile?.userID == userID else { throw CancellationError() }
                self.items = records
                UserDefaults.standard.set(try JSONEncoder().encode(records), forKey: "reminders.accountCache")
                try await self.reconcile(records, owner: userID, generation: version)
                self.problem = nil
            } catch {
                if self.generation == version {
                    let settings = await UNUserNotificationCenter.current().notificationSettings()
                    if self.generation == version {
                        self.problem = settings.authorizationStatus == .denied ? "itemReminder.permission" : "reminders.manage.unavailable"
                    }
                }
                throw error
            }
        }
        queue = task
        try await task.value
    }

    private func reconcile(_ records: [AccountContentReminder], owner: String, generation version: Int) async throws {
        let center = UNUserNotificationCenter.current()
        let desired = Set(records.map { $0.notificationID(owner: owner) })
        let pending = await center.pendingNotificationRequests()
        let removed = pending.filter { $0.identifier.hasPrefix("sanctuary.item.") && !desired.contains($0.identifier) }.map(\.identifier)
        center.removePendingNotificationRequests(withIdentifiers: removed)
        let delivered = await center.deliveredNotifications().filter { $0.request.identifier.hasPrefix("sanctuary.item.") && !desired.contains($0.request.identifier) }.map { $0.request.identifier }
        center.removeDeliveredNotifications(withIdentifiers: delivered)
        let settings = await center.notificationSettings()
        guard records.isEmpty || settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
            throw SanctuaryAPIError.server(message: "Enable Sanctuary notifications in phone settings.")
        }
        for record in records {
            guard self.generation == version else { throw CancellationError() }
            // Preserve unchanged native triggers during quiet refreshes, including near their fire time.
            if let existing = pending.first(where: { $0.identifier == record.notificationID(owner: owner) }),
               let trigger = existing.trigger as? UNCalendarNotificationTrigger,
               trigger.repeats, trigger.dateComponents.hour == record.hour, trigger.dateComponents.minute == record.minute,
               existing.content.title == record.title,
               existing.content.userInfo["destination"] as? String == "https://mydailysanctuary.com/\(record.kind)/\(record.slug)" { continue }
            let content = UNMutableNotificationContent()
            content.title = record.title
            content.body = "Take a moment to pray in Sanctuary."
            content.sound = .default
            content.userInfo = ["destination": "https://mydailysanctuary.com/\(record.kind)/\(record.slug)"]
            var components = DateComponents(); components.hour = record.hour; components.minute = record.minute
            try await center.add(UNNotificationRequest(identifier: record.notificationID(owner: owner), content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)))
        }
    }
}

struct ContentReminderButton: View {
    let kind: String
    let slug: String
    let title: String
    var compact = false
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @ObservedObject private var reminders = AccountReminders.shared
    @State private var accountRequired = false
    @State private var showing = false
    @State private var time = Date(timeIntervalSince1970: 8 * 3600)
    @State private var saving = false
    @State private var loadingTime = false
    @State private var problem: String?
    private var record: AccountContentReminder? { reminders.items.first { $0.kind == kind && $0.slug == slug } }

    var body: some View {
        Button {
            if accountStore.isAuthenticated { showing = true } else { accountRequired = true }
        } label: {
            Label(localization.t(record != nil ? "itemReminder.edit" : "itemReminder.add"), systemImage: compact ? "pencil" : (record != nil ? "bell.badge.fill" : "bell"))
                .labelStyle(ReminderButtonLabelStyle(compact: compact))
                .font(AppTheme.rounded(16, weight: .semibold)).foregroundStyle(.white)
                .padding(.horizontal, 18).padding(.vertical, 12)
                .background(AppTheme.cardBackgroundSoft, in: Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.25), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(localization.t(record != nil ? "itemReminder.edit" : "itemReminder.add"))
        .accountRequiredPrompt(isPresented: $accountRequired, onAuthenticated: { showing = true })
        .onChange(of: accountStore.isAuthenticated) { authenticated in if !authenticated { showing = false } }
        .task { await reminders.refresh(account: accountStore) }
        .onChange(of: showing) { visible in if visible { loadingTime = true; Task {
            await reminders.refresh(account: accountStore)
            time = record?.time ?? Date(timeIntervalSince1970: 8 * 3600)
            problem = reminders.problem.map { localization.t($0) }
            loadingTime = false
        } } }
        .sheet(isPresented: $showing) {
            NavigationStack {
                Form {
                    Section {
                        Text(title).font(.headline)
                        Text(localization.t("itemReminder.body")).font(.footnote)
                        DatePicker(localization.t("itemReminder.time"), selection: $time, displayedComponents: .hourAndMinute)
                            .environment(\.timeZone, TimeZone(secondsFromGMT: 0)!)
                        if let problem { Text(problem).foregroundStyle(.red) }
                    }
                    Section {
                        ReminderActionButton(title: localization.t("itemReminder.save"), symbol: "checkmark.circle.fill") { mutate {
                            var clock = Calendar(identifier: .gregorian); clock.timeZone = TimeZone(secondsFromGMT: 0)!
                            let parts = clock.dateComponents([.hour, .minute], from: time)
                            try await reminders.save(AccountContentReminder(kind: kind, slug: slug, title: title, hour: parts.hour ?? 8, minute: parts.minute ?? 0), account: accountStore)
                        } }
                        if let record {
                            ReminderActionButton(title: localization.t("itemReminder.remove"), symbol: "trash", destructive: true) {
                                mutate { try await reminders.remove(record, account: accountStore) }
                            }
                        }
                    }
                    .disabled(saving || reminders.syncing)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
                }
                .disabled(saving || loadingTime || reminders.syncing)
                .scrollContentBackground(.hidden).background(AppTheme.gradientTop)
                .navigationTitle(localization.t("itemReminder.title"))
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(localization.t("common.cancel")) { showing = false } } }
            }
            .preferredColorScheme(.dark).tint(.white).presentationBackground(AppTheme.gradientTop)
            .presentationDetents([.medium, .large])
        }
    }
    private func mutate(_ action: @escaping () async throws -> Void) {
        guard !saving else { return }; saving = true
        Task { @MainActor in
            defer { saving = false }
            do { try await action(); showing = false; problem = nil }
            catch { problem = error.localizedDescription }
        }
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

private struct ReminderButtonLabelStyle: LabelStyle {
    let compact: Bool
    func makeBody(configuration: Configuration) -> some View {
        if compact { configuration.icon }
        else { HStack { configuration.icon; configuration.title } }
    }
}
