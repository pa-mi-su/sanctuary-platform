import Foundation
import Combine

@MainActor
final class UserProgressStore: ObservableObject {
    @Published private(set) var commitments: [UserNovenaCommitment] = []
    @Published private(set) var favorites: [UserFavorite] = []
    @Published private(set) var userID: String?
    @Published private(set) var novenaRemindersEnabled = false
    @Published private(set) var generalDailyRemindersEnabled = false

    private let repository: any UserProgressRepository
    private let reminderScheduler: NovenaReminderScheduler
    private var revision = 0
    private var isRefreshing = false
    @Published private(set) var completingDays: Set<String> = []
    private var favoriteUpdates: Set<String> = []
    @Published private(set) var novenaMutations: Set<String> = []
    @Published var actionError: String?

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

        revision += 1
        actionError = nil
        userID = id
        commitments = []
        favorites = []

        guard id != nil else {
            await syncDigestReminders()
            return
        }

        await refresh()
    }

    func setNovenaRemindersEnabled(_ enabled: Bool) async {
        novenaRemindersEnabled = enabled
        await syncDigestReminders()
    }

    func setReminderPreferences(novenaEnabled: Bool, generalDailyEnabled: Bool) async {
        novenaRemindersEnabled = novenaEnabled
        self.generalDailyRemindersEnabled = generalDailyEnabled
        await syncDigestReminders()
    }

    func refresh() async {
        if isRefreshing {
            while isRefreshing {
                try? await Task.sleep(nanoseconds: 20_000_000)
            }
            return
        }
        isRefreshing = true
        defer { isRefreshing = false }

        guard let userID else {
            commitments = []
            favorites = []
            await syncDigestReminders()
            return
        }

        let snapshot = revision
        do {
            async let loadedCommitments = repository.listNovenaCommitments(userID: userID)
            async let loadedFavorites = repository.listFavorites(userID: userID)
            let rows = try await loadedCommitments
            let saved = try await loadedFavorites
            guard self.userID == userID, revision == snapshot else { return }
            commitments = rows
            favorites = saved
        } catch {}
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
        revision += 1
        let mutationKey = itemType.rawValue + ":" + itemID
        guard !favoriteUpdates.contains(mutationKey) else { return }
        favoriteUpdates.insert(mutationKey)
        actionError = nil
        defer { favoriteUpdates.remove(mutationKey) }
        let previousFavorites = favorites
        let favoriteKeyMatches: (UserFavorite) -> Bool = { favorite in
            favorite.itemType == itemType && favorite.itemID == itemID
        }

        if enabled {
            if !favorites.contains(where: favoriteKeyMatches) {
                favorites.append(
                    UserFavorite(
                        userID: userID,
                        itemType: itemType,
                        itemID: itemID,
                        createdAt: Date()
                    )
                )
            }
        } else {
            favorites.removeAll(where: favoriteKeyMatches)
        }

        do {
            if enabled {
                try await repository.addFavorite(userID: userID, itemType: itemType, itemID: itemID)
            } else {
                try await repository.removeFavorite(userID: userID, itemType: itemType, itemID: itemID)
            }
        } catch {
            if self.userID == userID {
                favorites = favorites.filter { !favoriteKeyMatches($0) } + previousFavorites.filter(favoriteKeyMatches)
                actionError = "progress.saveError"
            }
        }
    }

    func toggleFavorite(itemType: FavoriteItemType, itemID: String) async {
        let next = !isFavorite(itemType: itemType, itemID: itemID)
        await setFavorite(next, itemType: itemType, itemID: itemID)
    }

    func startNovena(novenaID: String) async {
        guard let userID, !novenaMutations.contains(novenaID), !completingDays.contains(novenaID) else { return }
        novenaMutations.insert(novenaID)
        defer { novenaMutations.remove(novenaID) }
        guard !commitments.contains(where: {
            $0.novenaID == novenaID && ($0.status == .active || $0.status == .completed)
        }) else { return }
        revision += 1
        actionError = nil
        let previousCommitments = commitments
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
        commitments.removeAll { $0.novenaID == novenaID }
        commitments.append(started)
        do {
            try await repository.upsertNovenaCommitment(started)
            await refresh()
        } catch {
            if self.userID == userID {
                commitments = commitments.filter { $0.novenaID != novenaID } + previousCommitments.filter { $0.novenaID == novenaID }
                actionError = "progress.saveError"
            }
        }
    }

    func completeCurrentDay(novenaID: String, totalDays: Int) async {
        guard let userID, !completingDays.contains(novenaID), !novenaMutations.contains(novenaID), let current = activeCommitment(for: novenaID) else { return }
        revision += 1
        completingDays.insert(novenaID)
        defer { completingDays.remove(novenaID) }
        actionError = nil
        let day = current.currentDay
        let end = day >= max(1, totalDays)
        let optimistic = UserNovenaCommitment(userID: userID, novenaID: novenaID,
            startedAt: current.startedAt, currentDay: end ? max(1, totalDays) : day + 1,
            completedDays: Array(Set(current.completedDays + [day])).sorted(), reminder: current.reminder,
            status: end ? .completed : .active, updatedAt: Date())
        commitments = commitments.map { $0.novenaID == novenaID ? optimistic : $0 }
        do {
            var saved = try await repository.completeNovenaDay(userID: userID, novenaID: novenaID, day: day, completedAt: Date())
            if end { saved = optimistic; try await repository.upsertNovenaCommitment(saved) }
            guard self.userID == userID else { return }
            commitments = commitments.map { $0.novenaID == novenaID ? saved : $0 }
        } catch {
            guard self.userID == userID else { return }
            commitments = commitments.map { $0.novenaID == novenaID ? current : $0 }
            actionError = "progress.saveError"
        }
    }

    func stopNovena(novenaID: String) async {
        guard let userID, !novenaMutations.contains(novenaID), !completingDays.contains(novenaID), let previous = activeCommitment(for: novenaID) else { return }
        novenaMutations.insert(novenaID)
        defer { novenaMutations.remove(novenaID) }
        revision += 1
        actionError = nil
        commitments.removeAll { $0.novenaID == novenaID }
        do { try await repository.removeNovenaCommitment(userID: userID, novenaID: novenaID) }
        catch { if self.userID == userID { commitments.append(previous); actionError = "progress.saveError" } }
    }

    private func syncDigestReminders() async {
        await reminderScheduler.syncDigestReminder(
            activeCommitmentCount: activeCommitments.count,
            novenaEnabled: novenaRemindersEnabled,
            generalDailyEnabled: generalDailyRemindersEnabled
        )
    }
}
