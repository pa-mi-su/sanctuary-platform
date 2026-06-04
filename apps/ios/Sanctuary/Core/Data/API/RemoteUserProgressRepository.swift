import Foundation

actor RemoteUserProgressRepository: UserProgressRepository {
    private let apiClient: SanctuaryAPIClient
    private unowned let sessionStore: AccountSessionStore

    init(apiClient: SanctuaryAPIClient, sessionStore: AccountSessionStore) {
        self.apiClient = apiClient
        self.sessionStore = sessionStore
    }

    func listFavorites(userID: String) async throws -> [UserFavorite] {
        let favorites = try await authenticatedRequest { token in
            try await apiClient.favorites(token: token)
        }
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
        try await authenticatedRequest { token in
            try await apiClient.saveFavorite(itemType: itemType.rawValue, itemId: itemID, token: token)
        }
    }

    func removeFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {
        try await authenticatedRequest { token in
            try await apiClient.deleteFavorite(itemType: itemType.rawValue, itemId: itemID, token: token)
        }
    }

    func listNovenaCommitments(userID: String) async throws -> [UserNovenaCommitment] {
        let commitments = try await authenticatedRequest { token in
            try await apiClient.novenaCommitments(token: token)
        }
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
        _ = try await authenticatedRequest { token in
            try await apiClient.saveNovenaCommitment(
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
        try await authenticatedRequest { token in
            try await apiClient.deleteNovenaCommitment(novenaId: novenaID, token: token)
        }
    }

    private func profileToken() async throws -> String {
        let token = await sessionStore.authorizationToken()
        guard let token, !token.isEmpty else {
            throw SanctuaryAPIError.missingAccessToken
        }
        return token
    }

    private func authenticatedRequest<Response>(
        _ request: (String) async throws -> Response
    ) async throws -> Response {
        let token = try await profileToken()
        do {
            return try await request(token)
        } catch {
            guard isSessionRejected(error),
                  let refreshedToken = await sessionStore.refreshAuthorizationTokenAfterRejection()
            else {
                throw error
            }

            return try await request(refreshedToken)
        }
    }

    private func isSessionRejected(_ error: Error) -> Bool {
        guard case SanctuaryAPIError.serverStatus(let statusCode, _) = error else {
            return false
        }

        return statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 410
    }
}
