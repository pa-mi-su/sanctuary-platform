import Foundation

actor RemoteUserProgressRepository: UserProgressRepository {
    private let apiClient: SanctuaryAPIClient
    private unowned let sessionStore: AccountSessionStore

    init(apiClient: SanctuaryAPIClient, sessionStore: AccountSessionStore) {
        self.apiClient = apiClient
        self.sessionStore = sessionStore
    }

    func listFavorites(userID: String) async throws -> [UserFavorite] {
        let token = try await accessToken()
        let favorites = try await apiClient.favorites(token: token)
        return favorites.compactMap { favorite in
            guard let itemType = FavoriteItemType(rawValue: favorite.itemType.lowercased()) else {
                return nil
            }

            return UserFavorite(
                userID: userID,
                itemType: itemType,
                itemID: favorite.itemId,
                createdAt: favorite.createdAt
            )
        }
    }

    func addFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {
        let token = try await accessToken()
        try await apiClient.saveFavorite(itemType: itemType.rawValue, itemId: itemID, token: token)
    }

    func removeFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {
        let token = try await accessToken()
        try await apiClient.deleteFavorite(itemType: itemType.rawValue, itemId: itemID, token: token)
    }

    func listNovenaCommitments(userID: String) async throws -> [UserNovenaCommitment] {
        let token = try await accessToken()
        let commitments = try await apiClient.novenaCommitments(token: token)
        return commitments.compactMap { dto in
            guard let status = CommitmentStatus(rawValue: dto.status.lowercased()) else {
                return nil
            }

            return UserNovenaCommitment(
                userID: userID,
                novenaID: dto.novenaId,
                startedAt: dto.startedAt,
                currentDay: dto.currentDay,
                completedDays: dto.completedDays,
                reminder: ReminderConfig(
                    enabled: dto.reminderEnabled,
                    morningHour: dto.reminderMorningHour,
                    eveningHour: dto.reminderEveningHour,
                    timeZoneID: dto.reminderTimeZoneId
                ),
                status: status,
                updatedAt: dto.updatedAt
            )
        }
    }

    func upsertNovenaCommitment(_ commitment: UserNovenaCommitment) async throws {
        let token = try await accessToken()
        _ = try await apiClient.saveNovenaCommitment(
            novenaId: commitment.novenaID,
            request: APIUserNovenaCommitmentRequest(
                startedAt: commitment.startedAt,
                currentDay: commitment.currentDay,
                completedDays: commitment.completedDays,
                reminderEnabled: commitment.reminder.enabled,
                reminderMorningHour: commitment.reminder.morningHour,
                reminderEveningHour: commitment.reminder.eveningHour,
                reminderTimeZoneId: commitment.reminder.timeZoneID,
                status: commitment.status.rawValue
            ),
            token: token
        )
    }

    func completeNovenaDay(
        userID: String,
        novenaID: String,
        day: Int,
        completedAt: Date
    ) async throws -> UserNovenaCommitment {
        guard var existing = try await listNovenaCommitments(userID: userID)
            .first(where: { $0.novenaID == novenaID && $0.status == .active }) else {
            throw SanctuaryAPIError.server(message: "No active novena was found to update.")
        }

        let completedDays = Set(existing.completedDays + [day]).sorted()
        existing = UserNovenaCommitment(
            userID: existing.userID,
            novenaID: existing.novenaID,
            startedAt: existing.startedAt,
            currentDay: max(existing.currentDay, day + 1),
            completedDays: completedDays,
            reminder: existing.reminder,
            status: existing.status,
            updatedAt: completedAt
        )

        try await upsertNovenaCommitment(existing)
        return existing
    }

    func removeNovenaCommitment(userID: String, novenaID: String) async throws {
        let token = try await accessToken()
        try await apiClient.deleteNovenaCommitment(novenaId: novenaID, token: token)
    }

    private func accessToken() async throws -> String {
        let token = await MainActor.run { sessionStore.accessToken }
        guard let token, !token.isEmpty else {
            throw SanctuaryAPIError.missingAccessToken
        }
        return token
    }
}
