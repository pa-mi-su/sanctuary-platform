import SwiftUI
import UserNotifications

struct MeView: View {
    let environment: AppEnvironment
    @ObservedObject private var push = PushNotifications.shared
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var selectedRoute: MeSelectionRoute?
    @State private var saintDetailsByID: [String: Saint] = [:]
    @State private var novenaDetailsByID: [String: Novena] = [:]
    @State private var prayerDetailsByID: [String: Prayer] = [:]
    @State private var novenaReminderToggle = false
    @State private var dailyReminderToggle = false
    @State private var isSavingReminderPreferences = false
    @State private var isShowingDeleteAccountConfirmation = false
    @State private var isDeletingAccount = false

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if accountStore.isAuthenticated {
                        accountHeader
                        reminderPreferencesCard
                        ScheduledRemindersCard()
                        inProgressCard
                        favoriteNovenasCard
                        favoriteSaintsCard
                        favoritePrayersCard
                        deleteAccountCard
                    } else {
                        reminderPreferencesCard
                        ScheduledRemindersCard()
                        AccountAccessView()
                    }
                }
                .padding(16)
                .padding(.bottom, 28)
            }
            .scrollDismissesKeyboard(.interactively)
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
        .task(id: favoritePrayerLookupKey) {
            await loadFavoritePrayerDetails()
        }
        .task(id: reminderPreferenceKey) {
            novenaReminderToggle = accountStore.profile?.novenaRemindersEnabled ?? false
            dailyReminderToggle = accountStore.profile?.feastRemindersEnabled ?? false
        }
        .fullScreenCover(item: $selectedRoute) { route in
            switch route {
            case .saint(let saint):
                SaintDetailView(
                    contentRepository: environment.contentRepository,
                    saint: saint,
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
            case .prayer(let prayer):
                PrayerDetailView(
                    contentRepository: environment.contentRepository,
                    prayer: prayer
                )
            }
        }
        .alert(localization.t("me.deleteAccount.confirmTitle"), isPresented: $isShowingDeleteAccountConfirmation) {
            Button(localization.t("common.cancel"), role: .cancel) {}
            Button(localization.t("me.deleteAccount.confirmAction"), role: .destructive) {
                deleteAccount()
            }
        } message: {
            Text(localization.t("me.deleteAccount.confirmBody"))
        }
    }

    private var reminderPreferencesCard: some View {
        MeCard(title: localization.t("notifications.title")) {
            VStack(alignment: .leading, spacing: 12) {
                reminderToggleRow(
                    title: localization.t("announcements.title"),
                    subtitle: localization.t("announcements.body"),
                    isOn: Binding(get: { push.enabled }, set: { value in
                        Task { await push.setEnabled(value); await accountStore.syncPushDevice() }
                    }), available: true
                )
                if accountStore.isAuthenticated {
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
                DisclosureGroup(localization.t("notifications.details")) {
                    VStack(alignment: .leading, spacing: 8) {
                        if !push.registrationStatus.isEmpty {
                            Text(push.registrationStatus).font(.footnote)
                        }
                        Button(localization.t("notifications.refresh")) { Task { await accountStore.syncPushDevice() } }
                            .font(.footnote.weight(.semibold))
                    }.padding(.top, 8)
                }
                .font(AppTheme.rounded(13, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)
                Button {
                    if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
                } label: {
                    Label(localization.t("notifications.settings"), systemImage: "arrow.up.right.square")
                        .font(AppTheme.rounded(13, weight: .semibold))
                }
                .foregroundStyle(AppTheme.subtitleText)
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
                            Task {
                                await openFavoriteSaint(favorite.itemID)
                            }
                        }
                    }
                }
            }
        }
    }

    private var favoritePrayersCard: some View {
        MeCard(title: localization.t("me.favoritePrayers")) {
            if favoritePrayers.isEmpty {
                Text(localization.t("me.noneFavoritePrayers"))
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.85))
            } else {
                VStack(spacing: 10) {
                    ForEach(favoritePrayers, id: \.itemID) { favorite in
                        accountLinkedRow(title: prayerTitle(for: favorite.itemID), subtitle: nil) {
                            Task {
                                await openFavoritePrayer(favorite.itemID)
                            }
                        }
                    }
                }
            }
        }
    }

    private var deleteAccountCard: some View {
        MeCard(title: localization.t("me.deleteAccount.title")) {
            VStack(alignment: .leading, spacing: 12) {
                Text(localization.t("me.deleteAccount.body"))
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.86))
                    .fixedSize(horizontal: false, vertical: true)

                Button {
                    isShowingDeleteAccountConfirmation = true
                } label: {
                    HStack {
                        if isDeletingAccount {
                            ProgressView()
                                .tint(.white)
                        }
                        Text(localization.t(isDeletingAccount ? "me.deleteAccount.deleting" : "me.deleteAccount.action"))
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(DestructivePillButtonStyle())
                .disabled(isDeletingAccount || accountStore.status == .loading)
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

    private func reminderToggleRow(title: String, subtitle: String, isOn: Binding<Bool>, available: Bool = false) -> some View {
        let active = isOn.wrappedValue

        return HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .center, spacing: 8) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)


                }

                Text(subtitle)
                    .font(AppTheme.rounded(13, weight: .medium))
                    .foregroundStyle(active ? .white.opacity(0.88) : .white.opacity(0.78))
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)

            Toggle(title, isOn: isOn)
                .labelsHidden()
                .toggleStyle(NotificationSwitchStyle())
                .accessibilityLabel(title)
                .tint(Color(red: 0.16, green: 0.65, blue: 0.56))
                .disabled(!available && isSavingReminderPreferences)
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
                .stroke(active ? Color.white.opacity(0.4) : Color.white.opacity(0.24), lineWidth: 1)
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

    private var favoritePrayers: [UserFavorite] {
        progressStore.favorites(for: .prayer)
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
        saintDetailsByID[id]?.displayName(locale: localization.language.contentLocale) ?? formattedFavoriteSaintName(from: id)
    }

    private func novenaTitle(for id: String) -> String {
        let locale = localization.language.contentLocale
        return novenaDetailsByID[id]?.titleByLocale[locale]
            ?? novenaDetailsByID[id]?.titleByLocale[.en]
            ?? id
    }

    private func prayerTitle(for id: String) -> String {
        let locale = localization.language.contentLocale
        return prayerDetailsByID[id]?.titleByLocale[locale]
            ?? prayerDetailsByID[id]?.titleByLocale[.en]
            ?? formattedFavoritePrayerName(from: id)
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

    private var favoritePrayerLookupKey: String {
        ([localization.language.contentLocale.rawValue] + favoritePrayers.map(\.itemID).sorted()).joined(separator: "|")
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
        var resolvedSaints: [String: Saint] = [:]

        if let summaries = try? await environment.contentRepository.listSaints(
            locale: locale,
            feastDate: nil,
            query: nil
        ) {
            let favoritesByID = Set(favoriteSaints.map(\.itemID))
            for saint in summaries where favoritesByID.contains(saint.id) || favoritesByID.contains(saint.slug) {
                resolvedSaints[saint.id] = saint
                resolvedSaints[saint.slug] = saint
            }
        }

        saintDetailsByID = resolvedSaints

        for favorite in favoriteSaints {
            let slug = resolvedSaints[favorite.itemID]?.slug ?? favorite.itemID
            if let saint = try? await environment.contentRepository.fetchSaint(slug: slug, locale: locale) {
                resolvedSaints[favorite.itemID] = saint
                resolvedSaints[saint.slug] = saint
            }
        }

        saintDetailsByID = resolvedSaints
    }

    private func loadFavoritePrayerDetails() async {
        guard !favoritePrayers.isEmpty else {
            prayerDetailsByID = [:]
            return
        }

        let locale = localization.language.contentLocale
        var resolvedPrayers: [String: Prayer] = [:]

        if let summaries = try? await environment.contentRepository.listPrayers(
            locale: locale,
            category: nil,
            query: nil
        ) {
            let favoritesByID = Set(favoritePrayers.map(\.itemID))
            for prayer in summaries where favoritesByID.contains(prayer.id) || favoritesByID.contains(prayer.slug) {
                resolvedPrayers[prayer.id] = prayer
                resolvedPrayers[prayer.slug] = prayer
            }
        }

        prayerDetailsByID = resolvedPrayers

        for favorite in favoritePrayers {
            let slug = resolvedPrayers[favorite.itemID]?.slug ?? favorite.itemID
            if let prayer = try? await environment.contentRepository.fetchPrayer(slug: slug, locale: locale) {
                resolvedPrayers[favorite.itemID] = prayer
                resolvedPrayers[prayer.id] = prayer
                resolvedPrayers[prayer.slug] = prayer
            }
        }

        prayerDetailsByID = resolvedPrayers
    }

    private func openFavoriteSaint(_ id: String) async {
        if let saint = saintDetailsByID[id] {
            selectedRoute = .saint(saint: saint)
            return
        }

        await loadFavoriteSaintDetails()

        if let saint = saintDetailsByID[id] {
            selectedRoute = .saint(saint: saint)
        }
    }

    private func openFavoritePrayer(_ id: String) async {
        if let prayer = prayerDetailsByID[id] {
            selectedRoute = .prayer(prayer: prayer)
            return
        }

        await loadFavoritePrayerDetails()

        if let prayer = prayerDetailsByID[id] {
            selectedRoute = .prayer(prayer: prayer)
        }
    }

    private func formattedFavoriteSaintName(from id: String) -> String {
        let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return id }

        let rawTokens = trimmed.split(separator: "_").map(String.init)
        let nameTokens: [String]

        if rawTokens.count >= 4,
           rawTokens[0].count == 2,
           rawTokens[1].count == 2,
           rawTokens[2].lowercased() == "saint" {
            nameTokens = ["Saint"] + rawTokens.dropFirst(3)
        } else {
            nameTokens = rawTokens
        }

        return nameTokens
            .map { token in
                let lower = token.lowercased()
                guard let first = lower.first else { return token }
                return first.uppercased() + lower.dropFirst()
            }
            .joined(separator: " ")
    }

    private func formattedFavoritePrayerName(from id: String) -> String {
        let trimmed = id.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return id }

        return trimmed
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .split(whereSeparator: \.isWhitespace)
            .map { token in
                let lower = token.lowercased()
                guard let first = lower.first else { return String(token) }
                return first.uppercased() + lower.dropFirst()
            }
            .joined(separator: " ")
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

    private func deleteAccount() {
        guard !isDeletingAccount else { return }
        isDeletingAccount = true

        Task {
            let deleted = await accountStore.deleteAccount()
            if deleted {
                await progressStore.setAuthenticatedUser(id: nil)
            }
            isDeletingAccount = false
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

private struct DestructivePillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(AppTheme.rounded(16, weight: .bold))
            .foregroundStyle(.white)
            .padding(.vertical, 13)
            .padding(.horizontal, 16)
            .background(
                Capsule()
                    .fill(Color(red: 0.62, green: 0.14, blue: 0.18).opacity(configuration.isPressed ? 0.78 : 0.92))
            )
            .overlay(
                Capsule()
                    .stroke(Color.white.opacity(0.18), lineWidth: 1)
            )
            .opacity(configuration.isPressed ? 0.86 : 1)
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
    case saint(saint: Saint)
    case novena(id: String)
    case prayer(prayer: Prayer)

    var id: String {
        switch self {
        case .saint(let saint):
            return "saint:\(saint.id)"
        case .novena(let id):
            return "novena:\(id)"
        case .prayer(let prayer):
            return "prayer:\(prayer.id)"
        }
    }
}


private struct NotificationSwitchStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button { configuration.isOn.toggle() } label: {
            ZStack(alignment: configuration.isOn ? .trailing : .leading) {
                Capsule().fill(configuration.isOn ? Color(red: 0.16, green: 0.65, blue: 0.56) : Color(red: 0.31, green: 0.40, blue: 0.46))
                Capsule().stroke(Color.white.opacity(0.5), lineWidth: 1)
                Circle().fill(.white).padding(4).shadow(color: .black.opacity(0.25), radius: 2, y: 1)
                    .frame(width: 32, height: 32)
            }
            .frame(width: 54, height: 32)
            .animation(.easeInOut(duration: 0.18), value: configuration.isOn)
        }
        .buttonStyle(.plain)
        .accessibilityValue(configuration.isOn ? Text("On") : Text("Off"))
    }
}


private struct ScheduledRemindersCard: View {
    @EnvironmentObject private var localization: LocalizationManager
    @State private var reminders: [UNNotificationRequest] = []
    @State private var confirmingClear = false
    var body: some View {
        MeCard(title: localization.t("reminders.manage.title")) {
            VStack(alignment: .leading, spacing: 14) {
                Text(localization.t("reminders.manage.body"))
                    .font(AppTheme.rounded(13, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                if reminders.isEmpty {
                    Label(localization.t("reminders.manage.empty"), systemImage: "bell.slash")
                        .font(AppTheme.rounded(14, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                        .padding(.vertical, 12)
                } else {
                    ForEach(reminders, id: \.identifier) { request in
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 5) {
                                Text(request.content.title).font(AppTheme.rounded(16, weight: .semibold)).foregroundStyle(.white)
                                if let date = (request.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate() {
                                    Text(localization.t("reminders.manage.daily") + " · " + date.formatted(date: .omitted, time: .shortened))
                                        .font(AppTheme.rounded(13, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                                }
                            }
                            Spacer(minLength: 0)
                            Button(role: .destructive) { remove([request.identifier]) } label: {
                                Image(systemName: "trash").font(.system(size: 18, weight: .semibold))
                                    .frame(width: 44, height: 44).foregroundStyle(Color(red: 1, green: 0.65, blue: 0.65))
                                    .background(Color.red.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(localization.t("itemReminder.remove") + ": " + request.content.title)
                        }
                        .padding(12).background(AppTheme.cardBackgroundSoft, in: RoundedRectangle(cornerRadius: 16))
                    }
                    ReminderActionButton(title: localization.t("reminders.manage.clear"), symbol: "trash", destructive: true) { confirmingClear = true }
                }
            }
        }
        .task { await reload() }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in Task { await reload() } }
        .confirmationDialog(localization.t("reminders.manage.confirm"), isPresented: $confirmingClear, titleVisibility: .visible) {
            Button(localization.t("reminders.manage.clear"), role: .destructive) { remove(reminders.map(\.identifier)) }
        } message: { Text(localization.t("reminders.manage.body")) }
    }
    @MainActor private func reload() async {
        reminders = await UNUserNotificationCenter.current().pendingNotificationRequests()
            .filter { $0.identifier.hasPrefix("sanctuary.item.") }
            .sorted { $0.content.title.localizedCaseInsensitiveCompare($1.content.title) == .orderedAscending }
    }
    private func remove(_ ids: [String]) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ids)
        center.removeDeliveredNotifications(withIdentifiers: ids)
        reminders.removeAll { ids.contains($0.identifier) }
    }
}
