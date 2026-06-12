import Combine
import SwiftUI

@MainActor
final class AskSanctuaryViewModel: ObservableObject {
    @Published var message = ""
    @Published private(set) var response: APIAskSanctuaryResponse?
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var disclaimerVersion = "v1"
    @Published private(set) var isAvailable = true
    @Published private(set) var unavailableMessage: String?

    private let apiClient: SanctuaryAPIClient
    private let accountStore: AccountSessionStore

    init(apiClient: SanctuaryAPIClient, accountStore: AccountSessionStore) {
        self.apiClient = apiClient
        self.accountStore = accountStore
    }

    var canSubmit: Bool {
        !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isLoading
    }

    func fetchStatus() async throws -> APIAskSanctuaryStatusResponse {
        guard let token = await accountStore.authorizationToken() else {
            throw SanctuaryViewModelError.requiresSignIn
        }
        let status = try await authenticatedFetchStatus(token: token)
        disclaimerVersion = status.disclaimerVersion
        isAvailable = status.available
        unavailableMessage = status.unavailableMessage
        return status
    }

    func acceptDisclaimer() async throws -> APIAskSanctuaryStatusResponse {
        guard let token = await accountStore.authorizationToken() else {
            throw SanctuaryViewModelError.requiresSignIn
        }
        let status = try await authenticatedAcceptDisclaimer(token: token)
        disclaimerVersion = status.disclaimerVersion
        isAvailable = status.available
        unavailableMessage = status.unavailableMessage
        return status
    }

    func ask() async {
        let trimmedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedMessage.isEmpty else {
            return
        }

        guard let token = await accountStore.authorizationToken() else {
            errorMessage = "Sign in to ask Sanctuary."
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            response = try await authenticatedAsk(message: trimmedMessage, token: token)
        } catch {
            errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    func clear() {
        response = nil
        errorMessage = nil
        message = ""
    }

    private func authenticatedAsk(message: String, token: String) async throws -> APIAskSanctuaryResponse {
        do {
            return try await apiClient.askSanctuary(message: message, token: token)
        } catch {
            guard isSessionRejected(error),
                  let refreshedToken = await accountStore.refreshAuthorizationTokenAfterRejection()
            else {
                throw error
            }

            return try await apiClient.askSanctuary(message: message, token: refreshedToken)
        }
    }

    private func authenticatedFetchStatus(token: String) async throws -> APIAskSanctuaryStatusResponse {
        do {
            return try await apiClient.fetchAskSanctuaryStatus(token: token)
        } catch {
            guard isSessionRejected(error),
                  let refreshedToken = await accountStore.refreshAuthorizationTokenAfterRejection()
            else {
                throw error
            }

            return try await apiClient.fetchAskSanctuaryStatus(token: refreshedToken)
        }
    }

    private func authenticatedAcceptDisclaimer(token: String) async throws -> APIAskSanctuaryStatusResponse {
        do {
            return try await apiClient.acceptAskSanctuaryDisclaimer(token: token)
        } catch {
            guard isSessionRejected(error),
                  let refreshedToken = await accountStore.refreshAuthorizationTokenAfterRejection()
            else {
                throw error
            }

            return try await apiClient.acceptAskSanctuaryDisclaimer(token: refreshedToken)
        }
    }

    private func isSessionRejected(_ error: Error) -> Bool {
        guard case SanctuaryAPIError.serverStatus(let statusCode, _) = error else {
            return false
        }
        return statusCode == 401 || statusCode == 403
    }
}

private enum SanctuaryViewModelError: LocalizedError {
    case requiresSignIn

    var errorDescription: String? {
        "Sign in to ask Sanctuary."
    }
}

struct AskSanctuaryView: View {
    private static let disclaimerAcceptedVersionKey = "askSanctuaryDisclaimerAcceptedVersion"

    let environment: AppEnvironment

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var localization: LocalizationManager
    @AppStorage(Self.disclaimerAcceptedVersionKey) private var acceptedDisclaimerVersion = ""
    @StateObject private var viewModel: AskSanctuaryViewModel
    @State private var showDisclaimer = false
    @State private var isCheckingDisclaimer = false
    @State private var disclaimerErrorMessage: String?
    @FocusState private var messageFocused: Bool

    init(environment: AppEnvironment, accountStore: AccountSessionStore) {
        self.environment = environment
        _viewModel = StateObject(
            wrappedValue: AskSanctuaryViewModel(
                apiClient: environment.apiClient,
                accountStore: accountStore
            )
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        header

                        if accountStore.isAuthenticated {
                            if viewModel.isAvailable {
                                askPanel

                                if viewModel.isLoading {
                                    loadingCard
                                } else if let response = viewModel.response {
                                    responseCard(response)
                                }

                                if let errorMessage = viewModel.errorMessage {
                                    messageCard(errorMessage, isError: true)
                                }
                            } else {
                                unavailablePanel
                            }
                        } else {
                            signedOutPanel
                        }
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 20)
                    .padding(.bottom, 28)
                    .frame(maxWidth: 760)
                    .frame(maxWidth: .infinity)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle("Ask Sanctuary")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 34, height: 34)
                            .background(
                                Circle()
                                    .fill(Color.white.opacity(0.12))
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close Ask Sanctuary")
                }

            }
            .sheet(isPresented: $showDisclaimer) {
                AskSanctuaryDisclaimerView(
                    errorMessage: disclaimerErrorMessage,
                    onCancel: {
                        dismiss()
                    },
                    onAccept: {
                        await acceptDisclaimer()
                    }
                )
                .interactiveDismissDisabled(true)
            }
            .task(id: accountStore.isAuthenticated) {
                await refreshDisclaimerStatus()
            }
        }
    }

    private func refreshDisclaimerStatus() async {
        guard accountStore.isAuthenticated else {
            showDisclaimer = false
            return
        }

        guard !isCheckingDisclaimer else {
            return
        }

        isCheckingDisclaimer = true
        defer { isCheckingDisclaimer = false }

        do {
            let status = try await viewModel.fetchStatus()
            if !status.available {
                showDisclaimer = false
            } else if status.disclaimerAccepted {
                acceptedDisclaimerVersion = status.disclaimerVersion
                showDisclaimer = false
            } else {
                showDisclaimer = true
            }
            disclaimerErrorMessage = nil
        } catch {
            disclaimerErrorMessage = error.localizedDescription
            showDisclaimer = true
        }
    }

    private func acceptDisclaimer() async {
        do {
            let status = try await viewModel.acceptDisclaimer()
            acceptedDisclaimerVersion = status.disclaimerVersion
            disclaimerErrorMessage = nil
            showDisclaimer = false
        } catch {
            disclaimerErrorMessage = error.localizedDescription
            showDisclaimer = true
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label("Ask Sanctuary", systemImage: "sparkles")
                .font(AppTheme.rounded(15, weight: .bold))
                .foregroundStyle(AppTheme.tabActive)

            Text("Bring what you are carrying.")
                .font(AppTheme.rounded(32, weight: .bold))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)

            Text("Ask for a Catholic companion response with Scripture references, a saint, a prayer, a short reflection, and one next step.")
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }

    private var askPanel: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("What would you like to bring to prayer?")
                .font(AppTheme.rounded(18, weight: .bold))
                .foregroundStyle(.white)

            TextEditor(text: $viewModel.message)
                .focused($messageFocused)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(.white)
                .scrollContentBackground(.hidden)
                .frame(minHeight: 132)
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color.white.opacity(0.08))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color.white.opacity(messageFocused ? 0.34 : 0.14), lineWidth: 1)
                        )
                )
                .overlay(alignment: .topLeading) {
                    if viewModel.message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text("Example: I feel overwhelmed and need a short prayer before work.")
                            .font(AppTheme.rounded(15, weight: .medium))
                            .foregroundStyle(Color.white.opacity(0.46))
                            .padding(.horizontal, 18)
                            .padding(.vertical, 20)
                            .allowsHitTesting(false)
                    }
                }
                .overlay(alignment: .bottomTrailing) {
                    if messageFocused {
                        Button {
                            messageFocused = false
                        } label: {
                            Image(systemName: "keyboard.chevron.compact.down")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(width: 42, height: 38)
                                .background(
                                    RoundedRectangle(cornerRadius: 15, style: .continuous)
                                        .fill(Color.white.opacity(0.12))
                                )
                        }
                        .buttonStyle(.plain)
                        .padding(10)
                        .accessibilityLabel("Dismiss keyboard")
                    }
                }

            HStack(spacing: 12) {
                Button {
                    messageFocused = false
                    Task { await viewModel.ask() }
                } label: {
                    Label(viewModel.isLoading ? "Asking..." : "Ask", systemImage: "paperplane.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(!viewModel.canSubmit)
                .opacity(viewModel.canSubmit ? 1 : 0.55)

                if viewModel.response != nil || !viewModel.message.isEmpty {
                    Button {
                        messageFocused = false
                        viewModel.clear()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .bold))
                            .frame(width: 48, height: 48)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.white)
                    .background(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(AppTheme.cardBackgroundSoft)
                    )
                }
            }
        }
        .padding(18)
        .appGlassCard(cornerRadius: 24)
    }

    private var signedOutPanel: some View {
        VStack(alignment: .leading, spacing: 16) {
            messageCard("Ask Sanctuary requires a free account so responses can be protected, abuse-limited, and eventually tied to your own history.", isError: false)

            AccountAccessView()
        }
    }

    private var unavailablePanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Ask Sanctuary is unavailable", systemImage: "pause.circle")
                .font(AppTheme.rounded(18, weight: .bold))
                .foregroundStyle(.white)

            Text(viewModel.unavailableMessage ?? "Ask Sanctuary is temporarily unavailable. Please try again later.")
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }

    private var loadingCard: some View {
        HStack(spacing: 12) {
            ProgressView()
                .tint(AppTheme.tabActive)

            Text("Preparing a response...")
                .font(AppTheme.rounded(16, weight: .semibold))
                .foregroundStyle(AppTheme.subtitleText)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 22)
    }

    private func responseCard(_ response: APIAskSanctuaryResponse) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            if response.requiresAccount {
                messageCard(response.message ?? "Please sign in to use Ask Sanctuary.", isError: false)
            } else if response.requiresUpgrade {
                messageCard(response.message ?? "You have reached today’s Ask Sanctuary limit.", isError: false)
            } else if let message = response.message, response.status != "OK" {
                messageCard(message, isError: response.status == "GUARDED")
            }

            if let theme = response.theme {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Theme")
                        .font(AppTheme.rounded(13, weight: .bold))
                        .foregroundStyle(AppTheme.tabActive)
                    Text(theme)
                        .font(AppTheme.rounded(24, weight: .bold))
                        .foregroundStyle(.white)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            if response.oldTestament != nil || response.newTestament != nil {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Scripture")
                        .font(AppTheme.rounded(13, weight: .bold))
                        .foregroundStyle(AppTheme.tabActive)

                    if let oldTestament = response.oldTestament {
                        responseRow(icon: "book.closed", title: "Old Testament", body: oldTestament.displayText)
                    }

                    if let newTestament = response.newTestament {
                        responseRow(icon: "book", title: "New Testament", body: newTestament.displayText)
                    }
                }
            }

            if let saint = response.saint {
                responseSection(title: "Saint", body: saint)
            }

            if let prayer = response.prayer {
                responseSection(title: "Prayer", body: prayer)
            }

            if let reflection = response.reflection {
                responseSection(title: "Reflection", body: reflection)
            }

            if let action = response.action {
                responseSection(title: "Action", body: action)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }

    private func messageCard(_ text: String, isError: Bool) -> some View {
        Text(text)
            .font(AppTheme.rounded(15, weight: .semibold))
            .foregroundStyle(isError ? Color(red: 1, green: 0.88, blue: 0.88) : AppTheme.subtitleText)
            .fixedSize(horizontal: false, vertical: true)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(isError ? Color.red.opacity(0.16) : Color.white.opacity(0.08))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                    )
            )
    }

    private func responseSection(title: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(AppTheme.rounded(13, weight: .bold))
                .foregroundStyle(AppTheme.tabActive)
            Text(body)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func responseRow(icon: String, title: String, body: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppTheme.glowGold)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(AppTheme.rounded(13, weight: .bold))
                    .foregroundStyle(.white.opacity(0.82))
                Text(body)
                    .font(AppTheme.rounded(16, weight: .semibold))
                    .foregroundStyle(AppTheme.subtitleText)
            }
        }
    }
}

private struct AskSanctuaryDisclaimerView: View {
    let errorMessage: String?
    let onCancel: () -> Void
    let onAccept: () async -> Void

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 10) {
                        Label("Before You Ask", systemImage: "lock.shield")
                            .font(AppTheme.rounded(15, weight: .bold))
                            .foregroundStyle(AppTheme.tabActive)

                        Text("A private place for prayerful support.")
                            .font(AppTheme.rounded(30, weight: .bold))
                            .foregroundStyle(.white)
                            .fixedSize(horizontal: false, vertical: true)

                        Text("Ask Sanctuary can help you reflect, pray, and find a next faithful step. Please read this once before continuing.")
                            .font(AppTheme.rounded(16, weight: .medium))
                            .foregroundStyle(AppTheme.subtitleText)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(18)
                    .appGlassCard(cornerRadius: 24)

                    VStack(alignment: .leading, spacing: 14) {
                        disclaimerRow(
                            icon: "eye.slash",
                            title: "Your question text is not stored",
                            body: "Sanctuary does not save the text you type. We store limited metadata, such as usage counts, safety category, response status, and a one-way hash used for abuse prevention and response reuse."
                        )

                        disclaimerRow(
                            icon: "sparkles",
                            title: "Processed with faith in mind",
                            body: "Your question is processed to prepare a Catholic companion response. Sanctuary operators should not see your question text in the app database."
                        )

                        disclaimerRow(
                            icon: "cross.case",
                            title: "Not emergency or professional care",
                            body: "Ask Sanctuary is not a priest, doctor, therapist, lawyer, or emergency service. If there is immediate danger, call emergency services. For confession, sacramental questions, medical care, legal advice, or serious mental health needs, speak with the appropriate professional or a priest."
                        )

                        disclaimerRow(
                            icon: "heart",
                            title: "Catholic spiritual support",
                            body: "Responses are meant for prayer, Scripture reflection, encouragement, and simple next steps. They may be imperfect and should be received with discernment."
                        )
                    }
                    .padding(18)
                    .appGlassCard(cornerRadius: 24)

                    if let errorMessage {
                        Text(errorMessage)
                            .font(AppTheme.rounded(15, weight: .semibold))
                            .foregroundStyle(Color(red: 1, green: 0.88, blue: 0.88))
                            .fixedSize(horizontal: false, vertical: true)
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                RoundedRectangle(cornerRadius: 18, style: .continuous)
                                    .fill(Color.red.opacity(0.16))
                            )
                    }

                    Button {
                        Task { await onAccept() }
                    } label: {
                        Label("I Understand", systemImage: "checkmark.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PrimaryPillButtonStyle())

                    Button {
                        onCancel()
                    } label: {
                        Text("Not Now")
                            .font(AppTheme.rounded(16, weight: .bold))
                            .foregroundStyle(AppTheme.subtitleText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 18)
                .padding(.top, 24)
                .padding(.bottom, 32)
                .frame(maxWidth: 680)
                .frame(maxWidth: .infinity)
            }

            VStack {
                HStack {
                    Button {
                        onCancel()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 34, height: 34)
                            .background(
                                Circle()
                                    .fill(Color.white.opacity(0.12))
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close Ask Sanctuary")

                    Spacer()
                }
                .padding(.horizontal, 18)
                .padding(.top, 14)

                Spacer()
            }
        }
    }

    private func disclaimerRow(icon: String, title: String, body: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(AppTheme.glowGold)
                .frame(width: 26)

            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(AppTheme.rounded(16, weight: .bold))
                    .foregroundStyle(.white)
                Text(body)
                    .font(AppTheme.rounded(15, weight: .medium))
                    .foregroundStyle(AppTheme.subtitleText)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
