import SwiftUI

enum AppTab: Hashable {
    case home
    case novenas
    case liturgical
    case saints
    case me
}

struct AppShellView: View {
    let environment: AppEnvironment
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab: AppTab = .home
    @State private var sharedContentLink: SharedContentLink?
    @State private var foregroundHeartbeatTask: Task<Void, Never>?
    @StateObject private var localization: LocalizationManager
    @StateObject private var accountStore: AccountSessionStore
    @StateObject private var progressStore: UserProgressStore

    init(environment: AppEnvironment) {
        self.environment = environment
        let accountStore = AccountSessionStore(
            apiClient: environment.apiClient,
            platformConfiguration: environment.platformConfiguration
        )
        _localization = StateObject(wrappedValue: LocalizationManager())
        _accountStore = StateObject(wrappedValue: accountStore)
        _progressStore = StateObject(
            wrappedValue: UserProgressStore(
                userProgressRepository: environment.makeUserProgressRepository(sessionStore: accountStore)
            )
        )
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(environment: environment)
                .tag(AppTab.home)
                .tabItem {
                    Label(localization.t("tab.home"), systemImage: "house.fill")
                }

            LazyTabContent(activeTab: $selectedTab, tab: .novenas) {
                NovenasCalendarView(environment: environment)
            }
                .tag(AppTab.novenas)
                .tabItem {
                    Label(localization.t("tab.novenas"), systemImage: "book.closed.fill")
                }

            LazyTabContent(activeTab: $selectedTab, tab: .liturgical) {
                LiturgicalCalendarView(environment: environment)
            }
                .tag(AppTab.liturgical)
                .tabItem {
                    Label(localization.t("tab.liturgical"), systemImage: "calendar.badge.clock")
                }

            LazyTabContent(activeTab: $selectedTab, tab: .saints) {
                SaintsCalendarView(environment: environment)
            }
                .tag(AppTab.saints)
                .tabItem {
                    Label(localization.t("tab.saints"), systemImage: "person.2.crop.square.stack.fill")
                }

            LazyTabContent(activeTab: $selectedTab, tab: .me) {
                MeView(environment: environment)
            }
                .tag(AppTab.me)
                .tabItem {
                    Label(localization.t("tab.me"), systemImage: "person.circle.fill")
                }
        }
        .tint(AppTheme.tabActive)
        .environmentObject(localization)
        .environmentObject(accountStore)
        .environmentObject(progressStore)
        .task {
            // Let first frame and taps land before background state refresh.
            try? await Task.sleep(nanoseconds: 700_000_000)
            await accountStore.bootstrap()
        }
        .onAppear {
            updateForegroundHeartbeat(for: scenePhase)
        }
        .onChange(of: scenePhase) { newPhase in
            updateForegroundHeartbeat(for: newPhase)
        }
        .task(id: accountStore.profile?.userID) {
            await progressStore.setAuthenticatedUser(id: accountStore.profile?.userID)
        }
        .task(id: reminderPreferenceSyncKey) {
            await progressStore.setReminderPreferences(
                novenaEnabled: accountStore.profile?.novenaRemindersEnabled ?? false,
                generalDailyEnabled: accountStore.profile?.feastRemindersEnabled ?? false
            )
        }
        .onOpenURL { url in
            openSharedContent(url)
        }
        .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
            if let url = activity.webpageURL {
                openSharedContent(url)
            }
        }
        .fullScreenCover(item: $sharedContentLink) { link in
            sharedContentDestination(for: link)
                .environmentObject(localization)
                .environmentObject(accountStore)
                .environmentObject(progressStore)
        }
    }

    private var reminderPreferenceSyncKey: String {
        let novena = accountStore.profile?.novenaRemindersEnabled == true ? "1" : "0"
        let general = accountStore.profile?.feastRemindersEnabled == true ? "1" : "0"
        return "\(novena)-\(general)-\(accountStore.profile?.userID ?? "signed-out")"
    }

    private func updateForegroundHeartbeat(for phase: ScenePhase) {
        guard phase == .active else {
            foregroundHeartbeatTask?.cancel()
            foregroundHeartbeatTask = nil
            return
        }

        guard foregroundHeartbeatTask == nil else {
            return
        }

        foregroundHeartbeatTask = Task {
            while !Task.isCancelled {
                await accountStore.recordForegroundPresence()
                try? await Task.sleep(nanoseconds: 120_000_000_000)
            }
        }
    }

    private func openSharedContent(_ url: URL) {
        guard let link = SharedContentLink.parse(url) else {
            return
        }

        switch link.kind {
        case .saint:
            selectedTab = .saints
        case .novena:
            selectedTab = .novenas
        case .prayer:
            selectedTab = .home
        }
        sharedContentLink = link
    }

    @ViewBuilder
    private func sharedContentDestination(for link: SharedContentLink) -> some View {
        SharedContentDestinationView(
            environment: environment,
            link: link,
            locale: localization.language.contentLocale,
            loadingTitle: localization.t("common.loading"),
            loadingDetail: localization.t("common.loadingDetail"),
            onClose: { sharedContentLink = nil }
        )
    }
}

private struct LazyTabContent<Content: View>: View {
    @Binding var activeTab: AppTab
    let tab: AppTab
    let content: () -> Content
    @State private var loaded = false

    var body: some View {
        Group {
            if loaded || activeTab == tab {
                content()
                    .onAppear { loaded = true }
            } else {
                Color.clear
            }
        }
    }
}

private struct SharedContentDestinationView: View {
    let environment: AppEnvironment
    let link: SharedContentLink
    let locale: ContentLocale
    let loadingTitle: String
    let loadingDetail: String
    let onClose: () -> Void

    @State private var saint: Saint?
    @State private var novena: Novena?
    @State private var prayer: Prayer?
    @State private var isLoading = true
    @State private var didFail = false

    var body: some View {
        Group {
            if let saint {
                SaintDetailView(
                    contentRepository: environment.contentRepository,
                    saint: saint,
                    onClose: onClose
                )
            } else if let novena {
                NovenaDetailView(
                    contentRepository: environment.contentRepository,
                    novena: novena,
                    onClose: onClose
                )
            } else if let prayer {
                PrayerDetailView(
                    contentRepository: environment.contentRepository,
                    prayer: prayer,
                    onClose: onClose
                )
            } else {
                sharedContentLoadingView
            }
        }
        .task(id: link.id) {
            await loadSharedContent()
        }
    }

    private var sharedContentLoadingView: some View {
        ZStack(alignment: .topLeading) {
            AppBackdrop()

            VStack {
                Spacer()

                if didFail {
                    VStack(spacing: 12) {
                        Text("Unable to open this shared link.")
                            .font(AppTheme.rounded(20, weight: .bold))
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.center)

                        Text("Please try the link again in a moment.")
                            .font(AppTheme.rounded(15, weight: .medium))
                            .foregroundStyle(AppTheme.subtitleText)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 22)
                    .appGlassCard(cornerRadius: 24)
                    .padding(.horizontal, 24)
                } else {
                    SanctuaryLoadingCard(
                        title: loadingTitle,
                        detail: isLoading ? loadingDetail : nil
                    )
                    .padding(.horizontal, 24)
                }

                Spacer()
            }

            FloatingBackButton(action: onClose)
                .padding(.leading, 22)
                .padding(.top, 18)
        }
        .leftEdgeSwipeBack(onClose)
    }

    @MainActor
    private func loadSharedContent() async {
        isLoading = true
        didFail = false
        saint = nil
        novena = nil
        prayer = nil

        do {
            switch link.kind {
            case .saint:
                saint = try await environment.contentRepository.fetchSaint(
                    slug: link.slug,
                    locale: locale
                )
                didFail = saint == nil
            case .novena:
                novena = try await environment.contentRepository.fetchNovena(
                    slug: link.slug,
                    locale: locale
                )
                didFail = novena == nil
            case .prayer:
                prayer = try await environment.contentRepository.fetchPrayer(
                    slug: link.slug,
                    locale: locale
                )
                didFail = prayer == nil
            }
        } catch {
            didFail = true
        }

        isLoading = false
    }
}

struct AppShellView_Previews: PreviewProvider {
    static var previews: some View {
        AppShellView(environment: .local())
    }
}
