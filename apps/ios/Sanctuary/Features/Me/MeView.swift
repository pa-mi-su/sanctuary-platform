import SwiftUI
import UserNotifications

struct MeView: View {
    let environment: AppEnvironment
    @Environment(\.dismiss) private var dismiss
    var settingsMode = false
    @State private var showSettings = false
    @State private var settingsSection: String?
    @State private var showInbox = false
    @StateObject private var inbox = AnnouncementInboxStore()
    @State private var showLanguagePicker = false
    @ObservedObject private var push = PushNotifications.shared
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var selectedRoute: MeSelectionRoute?
    @State private var saintDetailsByID: [String: Saint] = [:]
    @State private var novenaDetailsByID: [String: Novena] = [:]
    @State private var prayerDetailsByID: [String: Prayer] = [:]
    @State private var isShowingDeleteAccountConfirmation = false
    @State private var isDeletingAccount = false

    var body: some View {
        if settingsMode { page }
        else {
            NavigationStack {
                page
                    .navigationDestination(isPresented: $showSettings) {
                        MeView(environment: environment, settingsMode: true).toolbar(.hidden, for: .tabBar).toolbar(.hidden, for: .navigationBar)
                    }
                    .navigationDestination(isPresented: $showInbox) {
                        AnnouncementInboxView(environment: environment, store: inbox) { url in
                            showInbox = false
                            PushNotifications.shared.destination = url
                        }.toolbar(.hidden, for: .tabBar).toolbar(.hidden, for: .navigationBar)
                    }
                    .toolbar(.hidden, for: .navigationBar)
            }
        }
    }

    private func settingsRowLabel(_ key: String, symbol: String = "chevron.right") -> some View {
        HStack(spacing: 16) {
            Text(localization.t(key)).font(AppTheme.rounded(18, weight: .semibold))
                .multilineTextAlignment(.leading).fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 12)
            Image(systemName: symbol).font(.system(size: 17, weight: .semibold))
                .foregroundStyle(AppTheme.tabActive)
        }
        .foregroundStyle(.white).padding(16).frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
        .background(AppTheme.cardBackgroundSoft, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(AppTheme.tabBorder, lineWidth: 1))
        .contentShape(RoundedRectangle(cornerRadius: 18))
    }

    private func settingsRow(_ key: String, action: @escaping () -> Void) -> some View {
        Button(action: action) { settingsRowLabel(key) }.buttonStyle(.plain)
    }

    private func headerAction(_ title: String, icon: String, unread: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon).font(.system(size: 28, weight: .semibold))
                .foregroundStyle(AppTheme.tabActive).frame(width: 56, height: 56)
                .background(AppTheme.cardBackground).clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(AppTheme.tabActive.opacity(0.7), lineWidth: 1.5))
                .overlay(alignment: .topTrailing) {
                    if unread { Circle().fill(.red).frame(width: 12, height: 12).overlay(Circle().stroke(AppTheme.cardBackground, lineWidth: 2)).offset(x: -5, y: 5) }
                }
        }.buttonStyle(.plain).accessibilityLabel(title).accessibilityValue(unread ? "Unread announcements" : "")
    }

    private var page: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if !settingsMode {
                        HStack {
                            headerAction(localization.t("inbox.title"), icon: "bell.fill", unread: inbox.unread) { showInbox = true }
                            Spacer()
                            Text(localization.t("tab.me")).font(AppTheme.rounded(32, weight: .bold)).foregroundStyle(.white)
                            Spacer()
                            headerAction(localization.t("settings.title"), icon: "gearshape.fill") { showSettings = true }
                        }
                    }
                    if settingsMode {
                        Button { if settingsSection != nil { settingsSection = nil } else { dismiss() } } label: {
                            Image(systemName: "arrow.left").font(.system(size: 20, weight: .medium))
                                .foregroundStyle(.white).frame(width: 42, height: 42)
                                .background(AppTheme.cardBackground).clipShape(Circle())
                                .overlay(Circle().stroke(AppTheme.tabBorder, lineWidth: 1))
                        }.accessibilityLabel(localization.t("common.close"))
                        Text(localization.t(settingsSection ?? "settings.title")).font(AppTheme.rounded(32, weight: .bold)).foregroundStyle(.white)
                        if let section = settingsSection {
                            switch section {
                            case "settings.account":
                                if accountStore.isAuthenticated { accountHeader; deleteAccountCard }
                                else { AccountAccessView() }
                            case "settings.general":
                                NavigationCardButton(title: localization.t("home.language") + " · " + localization.language.displayName, icon: "globe") { showLanguagePicker = true }
                            case "notifications.title": reminderPreferencesCard
                            default:
                                Link(destination: URL(string: "mailto:info@mydailysanctuary.com")!) {
                                    settingsRowLabel("about.link.emailSupport", symbol: "arrow.up.right")
                                }.buttonStyle(.plain)
                                NavigationLink { AboutDocumentView(destination: .support).toolbar(.hidden, for: .navigationBar) } label: {
                                    settingsRowLabel("about.link.support")
                                }.buttonStyle(.plain)
                                NavigationLink { AboutDocumentView(destination: .privacy).toolbar(.hidden, for: .navigationBar) } label: {
                                    settingsRowLabel("about.link.privacy")
                                }.buttonStyle(.plain)
                            }
                        } else {
                            ForEach(["settings.account", "settings.general", "notifications.title", "settings.support"], id: \.self) { section in
                                settingsRow(section) { settingsSection = section }
                            }
                            NavigationLink { AboutView().toolbar(.hidden, for: .tabBar).toolbar(.hidden, for: .navigationBar) } label: {
                                settingsRowLabel("home.about")
                            }.buttonStyle(.plain)
                            Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "")
                                .font(AppTheme.rounded(14, weight: .medium)).foregroundStyle(AppTheme.subtitleText).frame(maxWidth: .infinity).padding()
                        }
                    } else if accountStore.isAuthenticated {
                        ScheduledRemindersCard(environment: environment)
                        inProgressCard
                        completedNovenasCard
                        favoriteNovenasCard
                        favoriteSaintsCard
                        favoritePrayersCard
                    } else {
                        AccountAccessView()
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(settingsMode ? 18 : 16)
                .padding(.bottom, settingsMode ? 48 : 28)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .sheet(isPresented: $showLanguagePicker) {
            LanguagePickerSheet()
                .presentationDetents([.height(370), .large])
                .presentationDragIndicator(.visible)
        }
        .task(id: accountStore.profile?.userID) { if !settingsMode { await refreshInbox() } }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task { await accountStore.syncPushDevice(); if !settingsMode { await refreshInbox() } }
        }
        .onReceive(NotificationCenter.default.publisher(for: .announcementInboxChanged)) { _ in
            if !settingsMode { Task { await refreshInbox() } }
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
                    })
                )
                if let problem = push.registrationProblem {
                    Text(problem).font(AppTheme.rounded(14, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                }
                notificationActionCard(
                    title: localization.t("notifications.settings"),
                    subtitle: localization.t("notifications.settingsBody"),
                    icon: "gearshape.fill", trailingIcon: "arrow.up.right.square"
                ) {
                    if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
                }

            }
        }
        .task { await accountStore.syncPushDevice() }
    }

    private func notificationActionCard(title: String, subtitle: String, icon: String, trailingIcon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 23, weight: .semibold))
                    .foregroundStyle(AppTheme.tabActive)
                VStack(alignment: .leading, spacing: 6) {
                    Text(title).font(AppTheme.rounded(16, weight: .semibold)).foregroundStyle(.white)
                    Text(subtitle).font(AppTheme.rounded(13, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
                Image(systemName: trailingIcon).font(.system(size: 18, weight: .bold)).foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(AppTheme.cardBackgroundSoft, in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.4), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }

    private func refreshInbox() async {
        await inbox.load(api: environment.apiClient, token: await accountStore.authorizationToken())
    }

    private var accountHeader: some View {
        VStack(alignment: .leading, spacing: 14) {
            if !settingsMode {

            Text(localization.t("me.subtitle"))
                .font(AppTheme.rounded(18, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)

            }
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


                    }

                    Spacer(minLength: 0)
                }

                if settingsMode {
                Button(localization.t("me.logout")) {
                    accountStore.logout()
                }
                .buttonStyle(SecondaryPillButtonStyle())
                }
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

    private var completedNovenasCard: some View {
        MeCard(title: localization.t("me.completed")) {
            let completed=progressStore.commitments.filter { $0.status == .completed }
            if completed.isEmpty { Text(localization.t("me.noCompleted")).foregroundStyle(AppTheme.subtitleText) }
            ForEach(completed,id:\.novenaID) { item in
                accountLinkedRow(title: novenaTitle(for:item.novenaID), subtitle:nil) { selectedRoute = .novena(id:item.novenaID) }
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

    private func reminderToggleRow(title: String, subtitle: String, isOn: Binding<Bool>) -> some View {
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


    private var novenaLookupKey: String {
        let allIDs = progressStore.commitments.map(\.novenaID) + favoriteNovenas.map(\.itemID)
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
        let allIDs = Array(Set(progressStore.commitments.map(\.novenaID) + favoriteNovenas.map(\.itemID))).sorted()
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
    let environment: AppEnvironment
    @State private var images: [String: URL] = [:]
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject private var reminders = AccountReminders.shared
    @State private var confirmingClear = false
    @State private var visible = false
    @State private var problem: String?
    var body: some View {
        MeCard(title: localization.t("reminders.manage.title")) {
            VStack(alignment: .leading, spacing: 14) {
                if reminders.legacyNotice {
                    Text(localization.t("reminders.manage.legacy")).foregroundStyle(AppTheme.subtitleText)
                    Button(localization.t("common.close")) { reminders.dismissLegacyNotice() }
                }
                if let issue = problem ?? reminders.problem { Text(localization.t(issue)).foregroundStyle(.orange) }
                if reminders.items.isEmpty {
                    Label(localization.t("reminders.manage.empty"), systemImage: "bell.slash").foregroundStyle(AppTheme.subtitleText).padding(.vertical, 12)
                }
                ForEach(reminders.items) { record in
                    HStack(spacing: 12) {
                        Button { open(record) } label: {
                            AsyncImage(url: images[record.id]) { image in image.resizable().scaledToFill() } placeholder: {
                                Image(systemName: "book.closed.fill").foregroundStyle(AppTheme.tabActive)
                            }.frame(width: 56, height: 64).background(AppTheme.cardBackgroundSoft).clipShape(RoundedRectangle(cornerRadius: 12))
                        }.buttonStyle(.plain).accessibilityLabel(record.title)
                        VStack(alignment: .leading, spacing: 5) {
                            Button { open(record) } label: { Text(record.title) }.buttonStyle(.plain).font(AppTheme.rounded(16, weight: .semibold)).foregroundStyle(.white)
                            Text(localization.t("reminders.manage.daily") + " · " + record.formattedTime)
                                .font(AppTheme.rounded(13, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
                        }
                        Spacer(minLength: 0)
                        ContentReminderButton(kind: record.kind, slug: record.slug, title: record.title, compact: true)
                        Button(role: .destructive) { mutate { try await reminders.remove(record, account: accountStore) } } label: {
                            Image(systemName: "trash").frame(width: 44, height: 44).foregroundStyle(Color(red: 1, green: 0.65, blue: 0.65))
                        }.accessibilityLabel(localization.t("itemReminder.remove") + ": " + record.title)
                    }.padding(12).background(AppTheme.cardBackgroundSoft, in: RoundedRectangle(cornerRadius: 16))
                }
                if !reminders.items.isEmpty {
                    ReminderActionButton(title: localization.t("reminders.manage.clear"), symbol: "trash", destructive: true) { confirmingClear = true }
                }
            }.disabled(reminders.updating)
        }
        .onAppear { visible = true }
        .onDisappear { visible = false }
        .task(id: scenePhase == .active && visible) {
            guard scenePhase == .active && visible else { return }
            while !Task.isCancelled {
                await reminders.refresh(account: accountStore)
                do { try await Task.sleep(nanoseconds: 30_000_000_000) }
                catch { return }
            }
        }
        .task(id: reminders.items.map(\.id)) {
            for record in reminders.items where images[record.id] == nil {
                let locale = localization.language.contentLocale
                if record.kind == "novenas" { images[record.id] = try? await environment.contentRepository.fetchNovena(slug: record.slug, locale: locale)?.imageURL }
                else { images[record.id] = try? await environment.contentRepository.fetchPrayer(slug: record.slug, locale: locale)?.imageURL }
            }
        }
        .confirmationDialog(localization.t("reminders.manage.confirm"), isPresented: $confirmingClear, titleVisibility: .visible) {
            Button(localization.t("reminders.manage.clear"), role: .destructive) { mutate { try await reminders.clear(account: accountStore) } }
        } message: { Text(localization.t("reminders.manage.body")) }
    }
    private func open(_ record: AccountContentReminder) {
        PushNotifications.shared.destination = URL(string: "https://mydailysanctuary.com/\(record.kind)/\(record.slug)")
    }
    private func mutate(_ action: @escaping () async throws -> Void) {
        Task { @MainActor in
            do { try await action(); problem = nil } catch { problem = error.localizedDescription }
        }
    }
}
