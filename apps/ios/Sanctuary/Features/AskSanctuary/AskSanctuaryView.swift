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
        normalizedFeelingWords(message) != nil && !isLoading
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

    func ask(language: AppLanguage, invalidInputMessage: String, signInMessage: String) async {
        guard let normalizedMessage = normalizedFeelingWords(message) else {
            errorMessage = invalidInputMessage
            return
        }

        guard let token = await accountStore.authorizationToken() else {
            errorMessage = signInMessage
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            response = try await authenticatedAsk(message: normalizedMessage, language: language, token: token)
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

    private func authenticatedAsk(message: String, language: AppLanguage, token: String) async throws -> APIAskSanctuaryResponse {
        do {
            return try await apiClient.askSanctuary(message: message, locale: language, token: token)
        } catch {
            guard isSessionRejected(error),
                  let refreshedToken = await accountStore.refreshAuthorizationTokenAfterRejection()
            else {
                throw error
            }

            return try await apiClient.askSanctuary(message: message, locale: language, token: refreshedToken)
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

    private func normalizedFeelingWords(_ text: String) -> String? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        let allowed = CharacterSet.letters
            .union(.whitespacesAndNewlines)
            .union(CharacterSet(charactersIn: ",-'"))
        guard trimmed.unicodeScalars.allSatisfy({ allowed.contains($0) }) else {
            return nil
        }

        let blockedWords: Set<String> = [
            "ass", "bullshit", "crap", "fart", "fuck", "fucked", "fucking",
            "nigger", "pee", "poo", "poop", "pooped", "pooping", "shit", "shitting"
        ]
        let words = trimmed
            .replacingOccurrences(of: ",", with: " ")
            .split(whereSeparator: { $0.isWhitespace })
            .map { String($0).lowercased() }

        guard words.count == 1 else { return nil }
        for word in words {
            guard word.count >= 2, word.count <= 24, !blockedWords.contains(word) else {
                return nil
            }
        }
        return words[0]
    }
}

private enum SanctuaryViewModelError: LocalizedError {
    case requiresSignIn

    var errorDescription: String? {
        nil
    }
}

struct AskSanctuaryView: View {
    let environment: AppEnvironment

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var localization: LocalizationManager
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
            .navigationTitle(localization.t("ask.title"))
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
                    .accessibilityLabel(localization.t("ask.closeAccessibility"))
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
                .environmentObject(localization)
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
            _ = try await viewModel.acceptDisclaimer()
            disclaimerErrorMessage = nil
            showDisclaimer = false
        } catch {
            disclaimerErrorMessage = error.localizedDescription
            showDisclaimer = true
        }
    }

    private func ask() async {
        await viewModel.ask(
            language: localization.language,
            invalidInputMessage: localization.t("ask.promptHelp"),
            signInMessage: localization.t("ask.signInRequired")
        )
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label(localization.t("ask.eyebrow"), systemImage: "sparkles")
                .font(AppTheme.rounded(15, weight: .bold))
                .foregroundStyle(AppTheme.tabActive)

            Text(localization.t("ask.headline"))
                .font(AppTheme.rounded(32, weight: .bold))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)

            Text(localization.t("ask.body"))
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
            Text(localization.t("ask.promptTitle"))
                .font(AppTheme.rounded(18, weight: .bold))
                .foregroundStyle(.white)

            TextField(localization.t("ask.placeholder"), text: $viewModel.message)
                .focused($messageFocused)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(.white)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
                .submitLabel(.send)
                .onSubmit {
                    messageFocused = false
                    Task { await ask() }
                }
                .frame(minHeight: 56)
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color.white.opacity(0.08))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color.white.opacity(messageFocused ? 0.34 : 0.14), lineWidth: 1)
                        )
                )

            Text(localization.t("ask.promptHelp"))
                .font(AppTheme.rounded(13, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.62))

            HStack(spacing: 12) {
                Button {
                    messageFocused = false
                    Task { await ask() }
                } label: {
                    Label(viewModel.isLoading ? localization.t("ask.loadingShort") : localization.t("ask.submit"), systemImage: "paperplane.fill")
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
            messageCard(localization.t("ask.accountRequiredBody"), isError: false)

            AccountAccessView()
        }
    }

    private var unavailablePanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(localization.t("ask.unavailableTitle"), systemImage: "pause.circle")
                .font(AppTheme.rounded(18, weight: .bold))
                .foregroundStyle(.white)

            Text(localization.language == .en ? (viewModel.unavailableMessage ?? localization.t("ask.unavailableBody")) : localization.t("ask.unavailableBody"))
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

            Text(localization.t("ask.loading"))
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
                messageCard(response.message ?? localization.t("ask.signInRequired"), isError: false)
            } else if response.requiresUpgrade {
                messageCard(response.message ?? localization.t("ask.limitReached"), isError: false)
            } else if let message = response.message, response.status != "OK" {
                messageCard(message, isError: response.status == "GUARDED")
            }

            if let theme = response.theme {
                VStack(alignment: .leading, spacing: 6) {
                    Text(localization.t("ask.theme"))
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
                    Text(localization.t("ask.scripture"))
                        .font(AppTheme.rounded(13, weight: .bold))
                        .foregroundStyle(AppTheme.tabActive)

                    if let oldTestament = response.oldTestament {
                        responseRow(icon: "book.closed", title: localization.t("ask.oldTestament"), body: oldTestament.displayText)
                    }

                    if let newTestament = response.newTestament {
                        responseRow(icon: "book", title: localization.t("ask.newTestament"), body: newTestament.displayText)
                    }
                }
            }

            if let saint = response.saint {
                responseSection(title: localization.t("ask.saint"), body: saint)
            }

            if let prayer = response.prayer {
                responseSection(title: localization.t("ask.prayer"), body: prayer)
            }

            if let reflection = response.reflection {
                responseSection(title: localization.t("ask.reflection"), body: reflection)
            }

            if let action = response.action {
                responseSection(title: localization.t("ask.action"), body: action)
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
    @EnvironmentObject private var localization: LocalizationManager

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 10) {
                        Label(localization.t("ask.disclaimerTitle"), systemImage: "lock.shield")
                            .font(AppTheme.rounded(15, weight: .bold))
                            .foregroundStyle(AppTheme.tabActive)

                        Text(localization.t("ask.disclaimerHeadline"))
                            .font(AppTheme.rounded(30, weight: .bold))
                            .foregroundStyle(.white)
                            .fixedSize(horizontal: false, vertical: true)

                        Text(localization.t("ask.disclaimerBody"))
                            .font(AppTheme.rounded(16, weight: .medium))
                            .foregroundStyle(AppTheme.subtitleText)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(18)
                    .appGlassCard(cornerRadius: 24)

                    VStack(alignment: .leading, spacing: 14) {
                        disclaimerRow(
                            icon: "eye.slash",
                            title: localization.t("ask.disclaimerPrivacyTitle"),
                            body: localization.t("ask.disclaimerPrivacyBody")
                        )

                        disclaimerRow(
                            icon: "sparkles",
                            title: localization.t("ask.disclaimerFaithTitle"),
                            body: localization.t("ask.disclaimerFaithBody")
                        )

                        disclaimerRow(
                            icon: "cross.case",
                            title: localization.t("ask.disclaimerCareTitle"),
                            body: localization.t("ask.disclaimerCareBody")
                        )

                        disclaimerRow(
                            icon: "heart",
                            title: localization.t("ask.disclaimerSupportTitle"),
                            body: localization.t("ask.disclaimerSupportBody")
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
                        Label(localization.t("ask.disclaimerAccept"), systemImage: "checkmark.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PrimaryPillButtonStyle())

                    Button {
                        onCancel()
                    } label: {
                        Text(localization.t("ask.disclaimerCancel"))
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
                    .accessibilityLabel(localization.t("ask.closeAccessibility"))

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
