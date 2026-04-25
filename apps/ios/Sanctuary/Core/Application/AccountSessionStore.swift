import Foundation
import SwiftUI
import Combine

enum AccountSessionStatus: Sendable {
    case signedOut
    case loading
    case awaitingConfirmation
    case authenticated
    case failed
}

struct AccountSession: Codable, Sendable {
    let accessToken: String
    let idToken: String
    let refreshToken: String?
    let tokenType: String
    let expiresAt: Date
    let email: String
    let displayName: String
}

struct UserAccountProfile: Sendable {
    let userID: String
    let email: String?
    let firstName: String?
    let lastName: String?
    let displayName: String
    let preferredLanguage: ContentLocale?
    let avatarURL: URL?
    let timeZoneID: String?
    let novenaRemindersEnabled: Bool
    let feastRemindersEnabled: Bool
    let emailUpdatesEnabled: Bool
    let onboardingCompleted: Bool
    let favoriteSaintCount: Int
    let favoriteNovenaCount: Int
    let favoritePrayerCount: Int
    let activeNovenaCount: Int
    let completedNovenaCount: Int
}

@MainActor
final class AccountSessionStore: ObservableObject {
    @Published private(set) var status: AccountSessionStatus = .signedOut
    @Published private(set) var session: AccountSession?
    @Published private(set) var profile: UserAccountProfile?
    @Published private(set) var pendingConfirmationEmail: String?
    @Published private(set) var pendingPasswordResetEmail: String?
    @Published private(set) var message: String?
    @Published private(set) var isErrorMessage = false

    private let apiClient: SanctuaryAPIClient
    private let secureStore: KeychainStore
    private let platformConfiguration: PlatformConfiguration
    private let sessionAccountKey = "primary-session"
    private var refreshTask: Task<Void, Never>?
    private let refreshBuffer: TimeInterval = 60

    init(
        apiClient: SanctuaryAPIClient,
        platformConfiguration: PlatformConfiguration,
        secureStore: KeychainStore = KeychainStore(service: "com.pamisu.Sanctuary.session")
    ) {
        self.apiClient = apiClient
        self.platformConfiguration = platformConfiguration
        self.secureStore = secureStore
    }

    var isConfigured: Bool {
        platformConfiguration.authenticationEnabled
    }

    var isAuthenticated: Bool {
        status == .authenticated && session != nil
    }

    var accessToken: String? {
        session?.accessToken
    }

    var idToken: String? {
        session?.idToken
    }

    func bootstrap() async {
        guard isConfigured else {
            setMessage("Authentication is not configured for this environment yet.", isError: true)
            return
        }

        guard let restored = loadStoredSession() else {
            status = .signedOut
            return
        }

        if restored.expiresAt <= Date() {
            guard let refreshToken = restored.refreshToken, !refreshToken.isEmpty else {
                clearStoredSession()
                return
            }

            session = restored
            status = .loading
            await renewSession(using: refreshToken, fallbackSession: restored, refreshProfileAfter: true)
            return
        }

        session = restored
        status = .loading
        scheduleRefresh(for: restored)
        await refreshProfile()
    }

    func register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ) async {
        guard isConfigured else {
            setMessage("Authentication is not configured for this environment yet.", isError: true)
            return
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.register(
                APIAuthRegisterRequest(
                    firstName: firstName,
                    lastName: lastName,
                    email: email,
                    password: password
                )
            )
            pendingConfirmationEmail = response.email
            status = .awaitingConfirmation
            setMessage("Your account is almost ready. Enter the confirmation code we emailed you.", isError: false)
        } catch {
            status = .failed
            setMessage(error.localizedDescription, isError: true)
        }
    }

    func confirmRegistration(code: String) async -> Bool {
        guard let email = pendingConfirmationEmail, !email.isEmpty else {
            setMessage("We need the email address you used to register.", isError: true)
            return false
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.confirm(
                APIAuthConfirmRequest(email: email, code: code)
            )
            pendingConfirmationEmail = nil
            status = .signedOut
            setMessage(response.message, isError: false)
            return true
        } catch {
            status = .failed
            setMessage(error.localizedDescription, isError: true)
            return false
        }
    }

    func resendConfirmation() async {
        guard let email = pendingConfirmationEmail, !email.isEmpty else {
            setMessage("We need the email address you used to register.", isError: true)
            return
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.resendConfirmation(email: email)
            status = .awaitingConfirmation
            setMessage(response.message, isError: false)
        } catch {
            status = .failed
            setMessage(error.localizedDescription, isError: true)
        }
    }

    func forgotPassword(email: String) async {
        guard isConfigured else {
            setMessage("Authentication is not configured for this environment yet.", isError: true)
            return
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.forgotPassword(email: email)
            pendingPasswordResetEmail = email
            status = .signedOut
            setMessage(response.message, isError: false)
        } catch {
            status = .failed
            setMessage(error.localizedDescription, isError: true)
        }
    }

    func resetPassword(email: String, code: String, newPassword: String) async -> Bool {
        guard isConfigured else {
            setMessage("Authentication is not configured for this environment yet.", isError: true)
            return false
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.resetPassword(email: email, code: code, newPassword: newPassword)
            pendingPasswordResetEmail = nil
            status = .signedOut
            setMessage(response.message, isError: false)
            return true
        } catch {
            status = .failed
            setMessage(error.localizedDescription, isError: true)
            return false
        }
    }

    func login(email: String, password: String) async {
        guard isConfigured else {
            setMessage("Authentication is not configured for this environment yet.", isError: true)
            return
        }

        status = .loading
        clearMessage()

        do {
            let response = try await apiClient.login(APIAuthLoginRequest(email: email, password: password))
            let session = AccountSession(
                accessToken: response.accessToken,
                idToken: response.idToken,
                refreshToken: response.refreshToken,
                tokenType: response.tokenType,
                expiresAt: Date().addingTimeInterval(TimeInterval(response.expiresIn)),
                email: response.email,
                displayName: response.displayName
            )
            self.session = session
            pendingConfirmationEmail = nil
            pendingPasswordResetEmail = nil
            try persist(session)
            scheduleRefresh(for: session)
            await refreshProfile(fallbackSession: session)
        } catch {
            clearStoredSession()
            status = .failed
            setMessage(error.localizedDescription, isError: true)
        }
    }

    func refreshProfile(fallbackSession: AccountSession? = nil) async {
        guard let token = session?.idToken ?? fallbackSession?.idToken ?? session?.accessToken ?? fallbackSession?.accessToken else {
            clearStoredSession()
            return
        }

        do {
            let response = try await apiClient.me(token: token)
            profile = mapProfile(response, fallbackSession: fallbackSession ?? session)
            status = .authenticated
            clearMessage()
        } catch {
            if let fallbackSession {
                profile = placeholderProfile(from: fallbackSession)
                status = .authenticated
                setMessage("Signed in, but Sanctuary could not refresh your full profile yet.", isError: true)
            } else {
                clearStoredSession()
                status = .failed
                setMessage(error.localizedDescription, isError: true)
            }
        }
    }

    func logout() {
        clearStoredSession()
    }

    func clearTransientMessage() {
        clearMessage()
    }

    func setConfirmedPrompt() {
        status = .signedOut
        setMessage("Your account is confirmed. Please sign in to continue.", isError: false)
    }

    private func mapProfile(
        _ response: APIUserProfileResponse,
        fallbackSession: AccountSession?
    ) -> UserAccountProfile {
        let firstAndLastName = [response.firstName?.trimmed, response.lastName?.trimmed]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
            .trimmed

        let resolvedDisplayName = [
            firstAndLastName,
            sanitizedDisplayName(response.displayName),
            sanitizedDisplayName(fallbackSession?.displayName),
            fallbackSession?.displayName.trimmed,
            response.email?.trimmed,
            fallbackSession?.email.trimmed,
            response.userId
        ]
            .compactMap { $0 }
            .first { !$0.isEmpty } ?? response.userId

        return UserAccountProfile(
            userID: response.userId,
            email: response.email ?? fallbackSession?.email,
            firstName: response.firstName,
            lastName: response.lastName,
            displayName: resolvedDisplayName,
            preferredLanguage: response.preferredLanguage.flatMap(ContentLocale.init(rawValue:)),
            avatarURL: response.avatarUrl.flatMap(URL.init(string:)),
            timeZoneID: response.timeZoneId,
            novenaRemindersEnabled: response.novenaRemindersEnabled,
            feastRemindersEnabled: response.feastRemindersEnabled,
            emailUpdatesEnabled: response.emailUpdatesEnabled,
            onboardingCompleted: response.onboardingCompleted,
            favoriteSaintCount: response.favoriteSaintCount,
            favoriteNovenaCount: response.favoriteNovenaCount,
            favoritePrayerCount: response.favoritePrayerCount,
            activeNovenaCount: response.activeNovenaCount,
            completedNovenaCount: response.completedNovenaCount
        )
    }

    private func sanitizedDisplayName(_ value: String?) -> String? {
        guard let trimmed = value?.trimmed, !trimmed.isEmpty else {
            return nil
        }

        let uuidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
        if trimmed.contains(uuidPattern) {
            return nil
        }

        return trimmed
    }

    private func placeholderProfile(from session: AccountSession) -> UserAccountProfile {
        UserAccountProfile(
            userID: session.email,
            email: session.email,
            firstName: nil,
            lastName: nil,
            displayName: session.displayName,
            preferredLanguage: nil,
            avatarURL: nil,
            timeZoneID: nil,
            novenaRemindersEnabled: false,
            feastRemindersEnabled: false,
            emailUpdatesEnabled: false,
            onboardingCompleted: false,
            favoriteSaintCount: 0,
            favoriteNovenaCount: 0,
            favoritePrayerCount: 0,
            activeNovenaCount: 0,
            completedNovenaCount: 0
        )
    }

    private func persist(_ session: AccountSession) throws {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(session)
        try secureStore.save(data, for: sessionAccountKey)
    }

    private func renewSession(
        using refreshToken: String,
        fallbackSession: AccountSession,
        refreshProfileAfter: Bool
    ) async {
        do {
            let response = try await apiClient.refreshSession(refreshToken: refreshToken)
            let refreshedSession = AccountSession(
                accessToken: response.accessToken,
                idToken: response.idToken,
                refreshToken: response.refreshToken ?? refreshToken,
                tokenType: response.tokenType,
                expiresAt: Date().addingTimeInterval(TimeInterval(response.expiresIn)),
                email: response.email,
                displayName: response.displayName
            )

            session = refreshedSession
            try persist(refreshedSession)
            scheduleRefresh(for: refreshedSession)

            if refreshProfileAfter {
                await refreshProfile(fallbackSession: refreshedSession)
            } else {
                status = .authenticated
                clearMessage()
            }
        } catch {
            clearStoredSession()
        }
    }

    private func loadStoredSession() -> AccountSession? {
        do {
            guard let data = try secureStore.load(account: sessionAccountKey) else {
                return nil
            }
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode(AccountSession.self, from: data)
        } catch {
            secureStore.delete(account: sessionAccountKey)
            return nil
        }
    }

    private func clearStoredSession() {
        refreshTask?.cancel()
        refreshTask = nil
        secureStore.delete(account: sessionAccountKey)
        session = nil
        profile = nil
        pendingConfirmationEmail = nil
        pendingPasswordResetEmail = nil
        status = .signedOut
        clearMessage()
    }

    private func scheduleRefresh(for session: AccountSession) {
        refreshTask?.cancel()
        refreshTask = nil

        guard let refreshToken = session.refreshToken, !refreshToken.isEmpty else {
            return
        }

        let delay = max(session.expiresAt.timeIntervalSinceNow - refreshBuffer, 1)
        refreshTask = Task { [weak self] in
            let duration = UInt64(delay * 1_000_000_000)
            try? await Task.sleep(nanoseconds: duration)
            guard !Task.isCancelled, let self else { return }
            await self.renewSession(using: refreshToken, fallbackSession: session, refreshProfileAfter: false)
        }
    }

    private func setMessage(_ value: String, isError: Bool) {
        message = value
        isErrorMessage = isError
    }

    private func clearMessage() {
        message = nil
        isErrorMessage = false
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
