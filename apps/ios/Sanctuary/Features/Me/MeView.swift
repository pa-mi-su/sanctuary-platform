import SwiftUI

struct MeView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var selectedRoute: MeSelectionRoute?
    @State private var saintDetailsByID: [String: Saint] = [:]
    @State private var novenaDetailsByID: [String: Novena] = [:]
    @State private var novenaReminderToggle = false
    @State private var dailyReminderToggle = false
    @State private var isSavingReminderPreferences = false

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if accountStore.isAuthenticated {
                        accountHeader
                        reminderPreferencesCard
                        inProgressCard
                        favoriteNovenasCard
                        favoriteSaintsCard
                    } else {
                        AccountAccessView()
                    }
                }
                .padding(16)
                .padding(.bottom, 28)
            }
        }
        .task {
            if accountStore.isAuthenticated {
                await accountStore.refreshProfile()
                await progressStore.refresh()
            }
        }
        .task(id: favoriteSaintLookupKey) {
            await loadFavoriteSaintDetails()
        }
        .task(id: novenaLookupKey) {
            await loadNovenaDetails()
        }
        .task(id: reminderPreferenceKey) {
            novenaReminderToggle = accountStore.profile?.novenaRemindersEnabled ?? false
            dailyReminderToggle = accountStore.profile?.feastRemindersEnabled ?? false
        }
        .fullScreenCover(item: $selectedRoute) { route in
            switch route {
            case .saint(let id):
                SaintDetailView(
                    contentRepository: environment.contentRepository,
                    saint: saintDetailsByID[id] ?? Saint(
                        id: id,
                        slug: id,
                        name: saintName(for: id),
                        nameByLocale: [
                            .en: saintName(for: id),
                            .es: saintName(for: id),
                            .pl: saintName(for: id)
                        ],
                        feastMonth: 1,
                        feastDay: 1,
                        imageURL: nil,
                        tags: [],
                        patronages: [],
                        feastLabelByLocale: [.en: ""],
                        summaryByLocale: [.en: ""],
                        biographyByLocale: [.en: ""],
                        prayersByLocale: [.en: []],
                        sources: []
                    ),
                    onClose: { selectedRoute = nil }
                )
            case .novena(let id):
                NovenaDetailView(
                    contentRepository: environment.contentRepository,
                    novena: novenaDetailsByID[id] ?? Novena(
                        id: id,
                        slug: id,
                        titleByLocale: [.en: novenaTitle(for: id)],
                        descriptionByLocale: [.en: ""],
                        durationDays: novenaDuration(for: id),
                        tags: [],
                        intentions: [],
                        imageURL: nil,
                        days: []
                    ),
                    onClose: { selectedRoute = nil }
                )
            }
        }
    }

    private var reminderPreferencesCard: some View {
        MeCard(title: localization.t("me.reminders")) {
            VStack(alignment: .leading, spacing: 10) {
                reminderToggleRow(
                    title: localization.t("me.reminders.inProgressTitle"),
                    subtitle: localization.t("me.reminders.inProgressBody"),
                    isOn: Binding(
                        get: { novenaReminderToggle },
                        set: { newValue in
                            novenaReminderToggle = newValue
                            persistReminderPreferences()
                        }
                    )
                )

                reminderToggleRow(
                    title: localization.t("me.reminders.generalTitle"),
                    subtitle: localization.t("me.reminders.generalBody"),
                    isOn: Binding(
                        get: { dailyReminderToggle },
                        set: { newValue in
                            dailyReminderToggle = newValue
                            persistReminderPreferences()
                        }
                    )
                )
            }
        }
    }

    private var accountHeader: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(localization.t("tab.me"))
                .font(AppTheme.rounded(42, weight: .bold))
                .foregroundStyle(.white)

            Text(localization.t("me.subtitle"))
                .font(AppTheme.rounded(18, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)

            VStack(alignment: .leading, spacing: 18) {
                Text(localization.t("me.signedIn"))
                    .font(AppTheme.rounded(13, weight: .bold))
                    .tracking(1.6)
                    .foregroundStyle(AppTheme.heroEyebrow)

                HStack(alignment: .top, spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [Color.white.opacity(0.12), Color.white.opacity(0.02)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 64, height: 64)

                        Text(initials)
                            .font(AppTheme.rounded(22, weight: .bold))
                            .foregroundStyle(.white)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(resolvedDisplayName)
                            .font(AppTheme.rounded(30, weight: .bold))
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.leading)

                        if let email = resolvedEmail {
                            Text(email)
                                .font(AppTheme.rounded(16, weight: .medium))
                                .foregroundStyle(AppTheme.subtitleText)
                                .textSelection(.enabled)
                        }

                        Text(localization.t("me.identitySupport"))
                            .font(AppTheme.rounded(15, weight: .medium))
                            .foregroundStyle(AppTheme.subtitleText)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Spacer(minLength: 0)
                }

                Button(localization.t("me.logout")) {
                    accountStore.logout()
                }
                .buttonStyle(SecondaryPillButtonStyle())
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .appGlassCard(cornerRadius: 24)
        }
    }

    private var inProgressCard: some View {
        MeCard(title: localization.t("me.inProgress")) {
            if progressStore.activeCommitments.isEmpty {
                Text(localization.t("me.noneInProgress"))
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.85))
            } else {
                VStack(spacing: 10) {
                    ForEach(progressStore.activeCommitments, id: \.novenaID) { commitment in
                        let title = novenaTitle(for: commitment.novenaID)
                        let total = novenaDuration(for: commitment.novenaID)
                        let dayLabel = "Day \(min(commitment.currentDay, total)) of \(total)"
                        accountLinkedRow(title: title, subtitle: dayLabel) {
                            selectedRoute = .novena(id: commitment.novenaID)
                        }
                    }
                }
            }
        }
    }

    private var favoriteNovenasCard: some View {
        MeCard(title: localization.t("me.favoriteNovenas")) {
            if favoriteNovenas.isEmpty {
                Text(localization.t("me.noneFavoriteNovenas"))
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.85))
            } else {
                VStack(spacing: 10) {
                    ForEach(favoriteNovenas, id: \.itemID) { favorite in
                        accountLinkedRow(title: novenaTitle(for: favorite.itemID), subtitle: nil) {
                            selectedRoute = .novena(id: favorite.itemID)
                        }
                    }
                }
            }
        }
    }

    private var favoriteSaintsCard: some View {
        MeCard(title: localization.t("me.favoriteSaints")) {
            if favoriteSaints.isEmpty {
                Text(localization.t("me.noneFavoriteSaints"))
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.85))
            } else {
                VStack(spacing: 10) {
                    ForEach(favoriteSaints, id: \.itemID) { favorite in
                        accountLinkedRow(title: saintName(for: favorite.itemID), subtitle: nil) {
                            selectedRoute = .saint(id: favorite.itemID)
                        }
                    }
                }
            }
        }
    }

    private func accountLinkedRow(title: String, subtitle: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(AppTheme.rounded(13, weight: .medium))
                            .foregroundStyle(.white.opacity(0.82))
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9))
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 14)
            .background(AppTheme.cardBackgroundSoft)
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color.white.opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func reminderToggleRow(title: String, subtitle: String, isOn: Binding<Bool>) -> some View {
        let active = isOn.wrappedValue

        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .center, spacing: 8) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)

                    Text(localization.t(active ? "common.on" : "common.off"))
                        .font(AppTheme.rounded(11, weight: .bold))
                        .foregroundStyle(active ? AppTheme.gradientTop : .white.opacity(0.82))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            Capsule()
                                .fill(active ? AppTheme.tabActive : Color.white.opacity(0.12))
                        )
                }

                Text(subtitle)
                    .font(AppTheme.rounded(13, weight: .medium))
                    .foregroundStyle(active ? .white.opacity(0.88) : .white.opacity(0.78))
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)

            Toggle("", isOn: isOn)
                .labelsHidden()
                .tint(AppTheme.tabActive)
                .disabled(isSavingReminderPreferences)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(active ? AppTheme.tabActive.opacity(0.16) : AppTheme.cardBackgroundSoft)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: active ? [Color.white.opacity(0.08), AppTheme.tabActive.opacity(0.02)] : [Color.clear, Color.clear],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(active ? AppTheme.tabActive.opacity(0.42) : Color.white.opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .animation(.spring(response: 0.24, dampingFraction: 0.82), value: active)
    }

    private var favoriteNovenas: [UserFavorite] {
        progressStore.favorites(for: .novena)
    }

    private var favoriteSaints: [UserFavorite] {
        progressStore.favorites(for: .saint)
    }

    private var resolvedDisplayName: String {
        let firstAndLast = [
            accountStore.profile?.firstName?.trimmingCharacters(in: .whitespacesAndNewlines),
            accountStore.profile?.lastName?.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        return [
            firstAndLast,
            accountStore.profile?.displayName,
            accountStore.session?.displayName,
            accountStore.profile?.email,
            accountStore.session?.email,
            localization.t("me.fallbackName")
        ]
        .compactMap { value in
            let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines)
            return (trimmed?.isEmpty == false) ? trimmed : nil
        }
        .first ?? localization.t("me.fallbackName")
    }

    private var resolvedEmail: String? {
        let email = accountStore.profile?.email ?? accountStore.session?.email
        let trimmed = email?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (trimmed?.isEmpty == false) ? trimmed : nil
    }

    private var initials: String {
        let parts = resolvedDisplayName
            .split(whereSeparator: \.isWhitespace)
            .map(String.init)
            .filter { !$0.isEmpty }

        if parts.isEmpty {
            return "S"
        }

        let collected = parts.prefix(2).compactMap { $0.first?.uppercased() }.joined()
        return collected.isEmpty ? "S" : collected
    }

    private func saintName(for id: String) -> String {
        saintDetailsByID[id]?.displayName(locale: localization.language.contentLocale) ?? id
    }

    private func novenaTitle(for id: String) -> String {
        let locale = localization.language.contentLocale
        return novenaDetailsByID[id]?.titleByLocale[locale]
            ?? novenaDetailsByID[id]?.titleByLocale[.en]
            ?? id
    }

    private var favoriteSaintLookupKey: String {
        ([localization.language.contentLocale.rawValue] + favoriteSaints.map(\.itemID).sorted()).joined(separator: "|")
    }

    private var reminderPreferenceKey: String {
        let novena = accountStore.profile?.novenaRemindersEnabled == true ? "1" : "0"
        let general = accountStore.profile?.feastRemindersEnabled == true ? "1" : "0"
        return "\(novena)-\(general)-\(accountStore.profile?.userID ?? "signed-out")"
    }

    private var novenaLookupKey: String {
        let allIDs = progressStore.activeCommitments.map(\.novenaID) + favoriteNovenas.map(\.itemID)
        return ([localization.language.contentLocale.rawValue] + allIDs.sorted()).joined(separator: "|")
    }

    private func novenaDuration(for id: String) -> Int {
        max(1, novenaDetailsByID[id]?.durationDays ?? 9)
    }

    private func loadFavoriteSaintDetails() async {
        guard !favoriteSaints.isEmpty else {
            saintDetailsByID = [:]
            return
        }

        let locale = localization.language.contentLocale
        var loadedDetails: [String: Saint] = [:]

        for favorite in favoriteSaints {
            if let saint = try? await environment.contentRepository.fetchSaint(slug: favorite.itemID, locale: locale) {
                loadedDetails[favorite.itemID] = saint
            }
        }

        saintDetailsByID = loadedDetails
    }

    private func loadNovenaDetails() async {
        let allIDs = Array(Set(progressStore.activeCommitments.map(\.novenaID) + favoriteNovenas.map(\.itemID))).sorted()
        guard !allIDs.isEmpty else {
            novenaDetailsByID = [:]
            return
        }

        let locale = localization.language.contentLocale
        var loadedDetails: [String: Novena] = [:]

        for id in allIDs {
            if let novena = try? await environment.contentRepository.fetchNovena(slug: id, locale: locale) {
                loadedDetails[id] = novena
            }
        }

        novenaDetailsByID = loadedDetails
    }

    private func persistReminderPreferences() {
        guard !isSavingReminderPreferences else { return }
        let pendingNovena = novenaReminderToggle
        let pendingDaily = dailyReminderToggle
        isSavingReminderPreferences = true

        Task {
            let success = await accountStore.updateReminderPreferences(
                novenaEnabled: pendingNovena,
                dailyEnabled: pendingDaily
            )

            if success {
                await progressStore.setReminderPreferences(
                    novenaEnabled: pendingNovena,
                    generalDailyEnabled: pendingDaily
                )
            } else {
                novenaReminderToggle = accountStore.profile?.novenaRemindersEnabled ?? false
                dailyReminderToggle = accountStore.profile?.feastRemindersEnabled ?? false
            }

            isSavingReminderPreferences = false
        }
    }
}

private struct MeCard<Content: View>: View {
    let title: String
    var subtitle: String?
    @ViewBuilder let content: Content

    init(title: String, subtitle: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.rounded(24, weight: .bold))
                .foregroundStyle(AppTheme.cardText)
            if let subtitle {
                Text(subtitle)
                    .font(AppTheme.rounded(18, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.84))
            }
            Divider().background(AppTheme.cardText.opacity(0.25))
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }
}

struct MeView_Previews: PreviewProvider {
    static var previews: some View {
        MeView(environment: .local())
            .environmentObject(
                UserProgressStore(
                    userProgressRepository: LocalUserProgressRepository()
                )
            )
            .environmentObject(
                AccountSessionStore(
                    apiClient: AppEnvironment.local().apiClient,
                    platformConfiguration: AppEnvironment.local().platformConfiguration
                )
            )
            .environmentObject(LocalizationManager())
    }
}

private enum MeSelectionRoute: Identifiable {
    case saint(id: String)
    case novena(id: String)

    var id: String {
        switch self {
        case .saint(let id):
            return "saint:\(id)"
        case .novena(let id):
            return "novena:\(id)"
        }
    }
}
