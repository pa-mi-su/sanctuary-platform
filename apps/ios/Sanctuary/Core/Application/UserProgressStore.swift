import Foundation
import Combine

@MainActor
final class UserProgressStore: ObservableObject {
    @Published private(set) var commitments: [UserNovenaCommitment] = []
    @Published private(set) var favorites: [UserFavorite] = []
    @Published private(set) var userID: String?
    @Published private(set) var novenaRemindersEnabled = false

    private let repository: any UserProgressRepository
    private let reminderScheduler: NovenaReminderScheduler

    init(
        userProgressRepository: any UserProgressRepository,
        reminderScheduler: NovenaReminderScheduler = .shared
    ) {
        self.repository = userProgressRepository
        self.reminderScheduler = reminderScheduler
    }

    var isAuthenticated: Bool {
        userID != nil
    }

    func setAuthenticatedUser(id: String?) async {
        guard userID != id else {
            await refresh()
            return
        }

        userID = id

        guard id != nil else {
            commitments = []
            favorites = []
            await syncDigestReminders()
            return
        }

        await refresh()
    }

    func setNovenaRemindersEnabled(_ enabled: Bool) async {
        novenaRemindersEnabled = enabled
        await syncDigestReminders()
    }

    func refresh() async {
        guard let userID else {
            commitments = []
            favorites = []
            await syncDigestReminders()
            return
        }

        do {
            async let loadedCommitments = repository.listNovenaCommitments(userID: userID)
            async let loadedFavorites = repository.listFavorites(userID: userID)
            commitments = try await loadedCommitments
            favorites = try await loadedFavorites
        } catch {
            commitments = []
            favorites = []
        }
        await syncDigestReminders()
    }

    func activeCommitment(for novenaID: String) -> UserNovenaCommitment? {
        commitments.first { $0.novenaID == novenaID && $0.status == .active }
    }

    var activeCommitments: [UserNovenaCommitment] {
        commitments.filter { $0.status == .active }
            .sorted { $0.updatedAt > $1.updatedAt }
    }

    func favorites(for itemType: FavoriteItemType) -> [UserFavorite] {
        favorites
            .filter { $0.itemType == itemType }
            .sorted { $0.createdAt > $1.createdAt }
    }

    func isFavorite(itemType: FavoriteItemType, itemID: String) -> Bool {
        favorites.contains { $0.itemType == itemType && $0.itemID == itemID }
    }

    func setFavorite(
        _ enabled: Bool,
        itemType: FavoriteItemType,
        itemID: String
    ) async {
        guard let userID else { return }
        do {
            if enabled {
                try await repository.addFavorite(userID: userID, itemType: itemType, itemID: itemID)
            } else {
                try await repository.removeFavorite(userID: userID, itemType: itemType, itemID: itemID)
            }
            await refresh()
        } catch {}
    }

    func toggleFavorite(itemType: FavoriteItemType, itemID: String) async {
        let next = !isFavorite(itemType: itemType, itemID: itemID)
        await setFavorite(next, itemType: itemType, itemID: itemID)
    }

    func startNovena(novenaID: String) async {
        guard let userID else { return }
        let now = Date()
        let started = UserNovenaCommitment(
            userID: userID,
            novenaID: novenaID,
            startedAt: now,
            currentDay: 1,
            completedDays: [],
            reminder: ReminderConfig(enabled: false, morningHour: nil, eveningHour: nil, timeZoneID: TimeZone.current.identifier),
            status: .active,
            updatedAt: now
        )
        do {
            try await repository.upsertNovenaCommitment(started)
            await refresh()
        } catch {}
    }

    func completeCurrentDay(novenaID: String, totalDays: Int) async {
        guard let userID else { return }
        guard let current = activeCommitment(for: novenaID) else { return }
        let dayToComplete = current.currentDay
        do {
            var updated = try await repository.completeNovenaDay(
                userID: userID,
                novenaID: novenaID,
                day: dayToComplete,
                completedAt: Date()
            )

            if dayToComplete >= max(1, totalDays) {
                updated = UserNovenaCommitment(
                    userID: updated.userID,
                    novenaID: updated.novenaID,
                    startedAt: updated.startedAt,
                    currentDay: max(1, totalDays),
                    completedDays: Array(1...max(1, totalDays)),
                    reminder: updated.reminder,
                    status: .completed,
                    updatedAt: Date()
                )
                try await repository.upsertNovenaCommitment(updated)
            }
            await refresh()
        } catch {}
    }

    func stopNovena(novenaID: String) async {
        guard let userID else { return }
        do {
            try await repository.removeNovenaCommitment(userID: userID, novenaID: novenaID)
            await refresh()
        } catch {}
    }

    private func syncDigestReminders() async {
        await reminderScheduler.syncDigestReminder(
            activeCommitmentCount: activeCommitments.count,
            enabled: novenaRemindersEnabled
        )
    }
}
