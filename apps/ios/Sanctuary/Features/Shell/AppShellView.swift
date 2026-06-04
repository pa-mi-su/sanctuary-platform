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
    @State private var selectedTab: AppTab = .home
    @State private var sharedContentLink: SharedContentLink?
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
        }
    }

    private var reminderPreferenceSyncKey: String {
        let novena = accountStore.profile?.novenaRemindersEnabled == true ? "1" : "0"
        let general = accountStore.profile?.feastRemindersEnabled == true ? "1" : "0"
        return "\(novena)-\(general)-\(accountStore.profile?.userID ?? "signed-out")"
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
        switch link.kind {
        case .saint:
            SaintDetailView(
                contentRepository: environment.contentRepository,
                saint: Saint(
                    id: link.slug,
                    slug: link.slug,
                    name: "",
                    nameByLocale: [.en: ""],
                    feastMonth: 1,
                    feastDay: 1,
                    imageURL: nil,
                    tags: [],
                    patronages: [],
                    feastLabelByLocale: [:],
                    summaryByLocale: [:],
                    biographyByLocale: [:],
                    prayersByLocale: [:],
                    sources: []
                ),
                onClose: { sharedContentLink = nil }
            )
        case .novena:
            NovenaDetailView(
                contentRepository: environment.contentRepository,
                novena: Novena(
                    id: link.slug,
                    slug: link.slug,
                    titleByLocale: [.en: ""],
                    descriptionByLocale: [:],
                    durationDays: 9,
                    tags: [],
                    intentions: [],
                    imageURL: nil,
                    days: []
                ),
                onClose: { sharedContentLink = nil }
            )
        case .prayer:
            PrayerDetailView(
                contentRepository: environment.contentRepository,
                prayer: Prayer(
                    id: link.slug,
                    slug: link.slug,
                    category: "prayer",
                    titleByLocale: [.en: ""],
                    bodyByLocale: [:],
                    alternateTitleByLocale: [:],
                    noteByLocale: [:],
                    imageURL: nil,
                    sourceTitle: nil,
                    sourceType: nil,
                    tags: []
                ),
                onClose: { sharedContentLink = nil }
            )
        }
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

struct AppShellView_Previews: PreviewProvider {
    static var previews: some View {
        AppShellView(environment: .local())
    }
}
