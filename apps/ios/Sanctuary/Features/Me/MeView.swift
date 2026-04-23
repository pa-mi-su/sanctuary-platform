import SwiftUI

struct MeView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var selectedRoute: MeSelectionRoute?
    @State private var saintDetailsByID: [String: Saint] = [:]
    @State private var novenaDetailsByID: [String: Novena] = [:]

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if accountStore.isAuthenticated {
                        accountHeader
                        accountSummary
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
        .sheet(item: $selectedRoute) { route in
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
                        imageURL: nil,
                        days: []
                    ),
                    onClose: { selectedRoute = nil }
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

    private var accountSummary: some View {
        HStack(spacing: 12) {
            summaryMetric(
                title: localization.t("me.inProgress"),
                value: "\(progressStore.activeCommitments.count)"
            )
            summaryMetric(
                title: localization.t("me.favoriteNovenas"),
                value: "\(favoriteNovenas.count)"
            )
            summaryMetric(
                title: localization.t("me.favoriteSaints"),
                value: "\(favoriteSaints.count)"
            )
        }
    }

    private func summaryMetric(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(AppTheme.rounded(26, weight: .bold))
                .foregroundStyle(.white)
            Text(title)
                .font(AppTheme.rounded(14, weight: .bold))
                .foregroundStyle(AppTheme.subtitleText)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 22)
    }

    private var inProgressCard: some View {
        MeCard(title: localization.t("me.inProgress"), subtitle: "\(progressStore.activeCommitments.count) synced") {
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
