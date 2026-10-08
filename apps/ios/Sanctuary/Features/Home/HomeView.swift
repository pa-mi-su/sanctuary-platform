import SwiftUI

struct HomeView: View {
    let environment: AppEnvironment
    @Binding var hasPlayedIntro: Bool
    @EnvironmentObject private var localization: LocalizationManager
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.openURL) private var openURL

    @State private var revealContent = false
    @State private var showLanguageDialog = false
    @State private var showAbout = false
    @State private var showPrayersSearch = false
    @State private var showRosarySearch = false
    @State private var showIntentionsSearch = false
    @State private var showPatronageSearch = false
    @State private var showDailyReadings = false
    @State private var dailyReadingsURLOverride: URL?
    @State private var todayLiturgicalDay: LiturgicalDay?
    @State private var newsArticles: [ChurchNewsArticle] = []
    @State private var newsPage = 0
    @State private var isRefreshingNews = false
    @State private var introPhase: IntroPhase = .hidden
    @State private var fixedHomeContentHeight: CGFloat = 0

    private enum IntroPhase { case hidden, bloom, complete }

    private var dailyReadingsURL: URL {
        dailyReadingsURLOverride ?? localization.language.dailyReadingsLandingURL
    }

    private var primaryActions: [HomeAction] {
        [
            HomeAction(
                title: localization.t("home.prayers"),
                subtitle: localization.t("home.prayersSubtitle"),
                icon: "hands.sparkles.fill",
                tint: AppTheme.glowRose,
                illustrationAssetName: "HomeCardPrayers"
            ) {
                showPrayersSearch = true
            },
            HomeAction(
                title: localization.t("home.rosary"),
                subtitle: localization.t("home.rosarySubtitle"),
                icon: "circle",
                tint: AppTheme.glowGold,
                illustrationAssetName: "HomeCardRosary"
            ) {
                showRosarySearch = true
            },
            HomeAction(
                title: localization.t("home.intentions"),
                subtitle: localization.t("home.intentionsSubtitle"),
                icon: "heart.text.square.fill",
                tint: AppTheme.glowRose,
                illustrationAssetName: "HomeCardIntentions"
            ) {
                showIntentionsSearch = true
            },
            HomeAction(
                title: localization.t("home.patronage"),
                subtitle: localization.t("home.patronageSubtitle"),
                icon: "person.crop.circle.badge.checkmark",
                tint: AppTheme.glowGold,
                illustrationAssetName: "HomeCardSaints"
            ) {
                showPatronageSearch = true
            }
        ]
    }

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                let width = proxy.size.width
                let scale = ResponsiveLayout.scale(for: width)
                let contentWidth = max(0, min(width - 24, 760))
                let newsCardHeight = fixedHomeContentHeight > 0
                    ? max(250 * scale, proxy.size.height - fixedHomeContentHeight - (56 * scale))
                    : 250 * scale
                let newsPageHeight = max(176, newsCardHeight - (36 * scale) - 54)

                ZStack {
                    AppBackdrop()

                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 16 * scale) {
                            VStack(spacing: 16 * scale) {
                                HomeCompactToolbar(
                                    localization: localization,
                                    isBlooming: introPhase == .bloom,
                                    showAbout: { showAbout = true },
                                    showLanguage: { showLanguageDialog = true }
                                )
                                .padding(.top, 8 * scale)
                                .opacity(revealContent ? 1 : 0)
                                .offset(y: revealContent ? 0 : -8)

                                HomeChurchTodayCard(
                                    localization: localization,
                                    scale: scale,
                                    liturgicalDay: todayLiturgicalDay,
                                    openReadings: { showDailyReadings = true }
                                )
                                .opacity(revealContent ? 1 : 0)
                                .offset(y: revealContent ? 0 : 10)

                                HomeQuickAccessGrid(
                                    actions: primaryActions,
                                    scale: scale,
                                    title: localization.t("home.quickAccess")
                                )
                                .opacity(revealContent ? 1 : 0)
                                .offset(y: revealContent ? 0 : 12)
                            }
                            .background {
                                GeometryReader { fixedContentProxy in
                                    Color.clear.preference(
                                        key: HomeFixedContentHeightKey.self,
                                        value: fixedContentProxy.size.height
                                    )
                                }
                            }

                            Group {
                                if !newsArticles.isEmpty {
                                    ChurchNewsCarousel(
                                        articles: newsArticles,
                                        selection: $newsPage,
                                        localization: localization,
                                        pageHeight: newsPageHeight,
                                        openArticle: { openURL($0.canonicalURL) }
                                    )
                                } else {
                                    ChurchNewsLoadingCard(localization: localization)
                                }
                            }
                            .padding(.horizontal, 20 * scale)
                            .padding(.vertical, 18 * scale)
                            .frame(maxWidth: .infinity, minHeight: newsCardHeight, alignment: .top)
                            .appGlassCard(cornerRadius: 30 * scale)
                            .transition(.opacity.combined(with: .scale(scale: 0.98)))

                            Spacer(minLength: 2 * scale)
                        }
                        .frame(maxWidth: contentWidth)
                        .padding(.horizontal, 12 * scale)
                        .padding(.top, 6 * scale)
                        .padding(.bottom, 16 * scale)
                    }
                    .onPreferenceChange(HomeFixedContentHeightKey.self) { measuredHeight in
                        guard measuredHeight > 0, abs(measuredHeight - fixedHomeContentHeight) > 0.5 else { return }
                        fixedHomeContentHeight = measuredHeight
                    }
                    .refreshable {
                        await loadChurchNews()
                    }
                }
            }
            .task {
                guard !revealContent else { return }
                await playIntroIfNeeded()
            }
            .task(id: localization.language) {
                newsArticles = await environment.churchNewsRepository.cachedArticles(
                    locale: localization.language.contentLocale
                )
                while !Task.isCancelled {
                    await loadDailyReadingsURL()
                    await loadChurchNews()
                    do {
                        try await Task.sleep(for: .seconds(15 * 60))
                    } catch {
                        return
                    }
                }
            }
            .sheet(isPresented: $showLanguageDialog) {
                LanguagePickerSheet()
                    .presentationDetents([.medium])
            }
            .fullScreenCover(isPresented: $showAbout) {
                AboutView()
            }
            .fullScreenCover(isPresented: $showPrayersSearch) {
                PrayersSearchView(environment: environment)
            }
            .fullScreenCover(isPresented: $showRosarySearch) {
                PrayersSearchView(environment: environment, mode: .rosary)
            }
            .fullScreenCover(isPresented: $showIntentionsSearch) {
                TermSearchView(environment: environment, mode: .intentions)
            }
            .fullScreenCover(isPresented: $showPatronageSearch) {
                TermSearchView(environment: environment, mode: .patronage)
            }
            .fullScreenCover(isPresented: $showDailyReadings) {
                DailyReadingsView(url: dailyReadingsURL)
            }
            .toolbar(.hidden)
        }
    }

    private func playIntroIfNeeded() async {
        if hasPlayedIntro || reduceMotion {
            hasPlayedIntro = true
            introPhase = .complete
            withAnimation(.easeOut(duration: 0.2)) { revealContent = true }
            return
        }
        introPhase = .bloom
        withAnimation(.spring(response: 0.58, dampingFraction: 0.72)) { revealContent = true }
        try? await Task.sleep(for: .milliseconds(950))
        guard !Task.isCancelled else { return }
        withAnimation(.easeOut(duration: 0.35)) { introPhase = .complete }
        hasPlayedIntro = true
    }

    private func loadChurchNews() async {
        let locale = localization.language.contentLocale
        guard !isRefreshingNews else { return }
        isRefreshingNews = true
        defer { isRefreshingNews = false }
        if let refreshed = try? await environment.churchNewsRepository.refreshArticles(locale: locale),
           !refreshed.isEmpty {
            withAnimation(.easeInOut(duration: 0.25)) {
                newsArticles = refreshed
                newsPage = min(newsPage, max(0, refreshed.count - 1))
            }
        }
    }

    private func loadDailyReadingsURL() async {
        do {
            if let liturgicalDay = try await environment.contentRepository.fetchLiturgicalDay(for: Date()) {
                todayLiturgicalDay = liturgicalDay
                dailyReadingsURLOverride = localization.language.localizedDailyReadingsURL(
                    from: liturgicalDay.readingURL?.absoluteString
                ) ?? localization.language.dailyReadingsLandingURL
            } else if todayLiturgicalDay == nil {
                dailyReadingsURLOverride = localization.language.dailyReadingsLandingURL
            }
        } catch {
            if todayLiturgicalDay == nil {
                dailyReadingsURLOverride = localization.language.dailyReadingsLandingURL
            }
        }
    }
}

private struct HomeFixedContentHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

private struct HomeCompactToolbar: View {
    let localization: LocalizationManager
    let isBlooming: Bool
    let showAbout: () -> Void
    let showLanguage: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(AppTheme.glowGold.opacity(isBlooming ? 0.55 : 0.14))
                    .frame(width: isBlooming ? 52 : 40, height: isBlooming ? 52 : 40)
                    .blur(radius: isBlooming ? 10 : 6)

                Image("BrandLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 34, height: 34)
                    .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
                    .scaleEffect(isBlooming ? 0.72 : 1)
            }
            .frame(width: 40, height: 40)
            .animation(.spring(response: 0.58, dampingFraction: 0.72), value: isBlooming)

            Text("Sanctuary")
                .font(AppTheme.rounded(20, weight: .bold))
                .foregroundStyle(.white)

            Spacer(minLength: 8)

            CompactToolbarButton(
                title: localization.language.rawValue.uppercased(),
                icon: "globe",
                accessibilityLabel: "\(localization.t("home.language")): \(localization.language.displayName)",
                action: showLanguage
            )

            CompactToolbarButton(
                title: nil,
                icon: "info.circle.fill",
                accessibilityLabel: localization.t("home.about"),
                action: showAbout
            )
        }
        .padding(.horizontal, 4)
        .frame(minHeight: 42)
    }
}

private struct CompactToolbarButton: View {
    let title: String?
    let icon: String
    let accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                if let title {
                    Text(title)
                        .font(AppTheme.rounded(12, weight: .bold))
                }
            }
            .foregroundStyle(Color.white.opacity(0.9))
            .frame(minWidth: 40, minHeight: 40)
            .padding(.horizontal, title == nil ? 0 : 4)
            .background(Color.white.opacity(0.09), in: Capsule())
            .overlay(Capsule().stroke(Color.white.opacity(0.12), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}

private struct HomeChurchTodayCard: View {
    let localization: LocalizationManager
    let scale: CGFloat
    let liturgicalDay: LiturgicalDay?
    let openReadings: () -> Void

    private var accent: Color {
        switch liturgicalDay?.color.lowercased() {
        case "white": return Color.white
        case "red": return Color(hex: "#E16A63")
        case "violet": return Color(hex: "#A886D7")
        case "rose": return Color(hex: "#D994B9")
        default: return AppTheme.ordinary
        }
    }

    private var colorLabel: String {
        localization.t("liturgical.color.\(liturgicalDay?.color.lowercased() ?? "green")")
    }

    private var dateLabel: String {
        let formatter = DateFormatter()
        formatter.locale = localization.language.locale
        formatter.timeZone = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("EEE, MMM d")
        return formatter.string(from: liturgicalDay?.date ?? Date()).uppercased(with: localization.language.locale)
    }

    private var title: String {
        guard let liturgicalDay else { return localization.t("home.liturgicalFallback") }
        if liturgicalDay.rank == "Memorial of Our Lady of the Rosary" {
            return localization.t("liturgical.ourLadyRosary")
        }
        return liturgicalDay.rank
    }

    var body: some View {
        Button(action: openReadings) {
            HStack(spacing: 13 * scale) {
                RoundedRectangle(cornerRadius: 3 * scale, style: .continuous)
                    .fill(accent)
                    .frame(width: 5 * scale)
                    .shadow(color: accent.opacity(0.45), radius: 7)

                VStack(alignment: .leading, spacing: 6 * scale) {
                    HStack {
                        Label(localization.t("home.churchToday").uppercased(), systemImage: "sparkles")
                            .font(AppTheme.rounded(11.5 * scale, weight: .bold))
                            .foregroundStyle(AppTheme.glowGold)
                            .tracking(1.1)

                        Spacer()

                        Text(dateLabel)
                            .font(AppTheme.rounded(11 * scale, weight: .bold))
                            .foregroundStyle(AppTheme.subtitleText)
                    }

                    Text(title)
                        .font(AppTheme.rounded(19 * scale, weight: .bold))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                        .minimumScaleFactor(0.82)

                    HStack(spacing: 7 * scale) {
                        Circle().fill(accent).frame(width: 7 * scale, height: 7 * scale)
                        Text(colorLabel)
                        Text("•")
                        Text(localization.t("home.calendarScope"))
                    }
                    .font(AppTheme.rounded(11.5 * scale, weight: .semibold))
                    .foregroundStyle(AppTheme.subtitleText)

                    HStack {
                        Text(localization.t("home.openReadings"))
                            .font(AppTheme.rounded(12.5 * scale, weight: .bold))
                            .foregroundStyle(AppTheme.glowGold)
                        Spacer()
                        Image(systemName: "arrow.right")
                            .font(.system(size: 12 * scale, weight: .bold))
                            .foregroundStyle(AppTheme.glowGold)
                    }
                }
            }
            .padding(.horizontal, 15 * scale)
            .padding(.vertical, 13 * scale)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .appGlassCard(cornerRadius: 24 * scale)
        .accessibilityLabel("\(localization.t("home.churchToday")), \(title), \(localization.t("home.openReadings"))")
    }
}

private struct HomeQuickAccessGrid: View {
    let actions: [HomeAction]
    let scale: CGFloat
    let title: String

    private var rows: [[HomeAction]] {
        [Array(actions.prefix(2)), Array(actions.dropFirst(2))]
            .filter { !$0.isEmpty }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 9 * scale) {
            Label(title.uppercased(), systemImage: "sparkles")
                .font(AppTheme.rounded(12 * scale, weight: .bold))
                .tracking(1.2 * scale)
                .foregroundStyle(AppTheme.glowGold)

            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 9 * scale) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, action in
                        Button(action: action.action) {
                            HStack(spacing: 8 * scale) {
                                Image(action.illustrationAssetName)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 50 * scale, height: 38 * scale)
                                    .clipShape(RoundedRectangle(cornerRadius: 10 * scale, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10 * scale, style: .continuous)
                                            .stroke(Color.white.opacity(0.10), lineWidth: 1)
                                    )

                                Text(action.title)
                                    .font(AppTheme.rounded(11 * scale, weight: .bold))
                                    .foregroundStyle(.white)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.72)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(.horizontal, 8 * scale)
                            .frame(maxWidth: .infinity)
                            .frame(height: 60 * scale)
                            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 15 * scale, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 15 * scale, style: .continuous)
                                    .stroke(Color.white.opacity(0.09), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(13 * scale)
        .appGlassCard(cornerRadius: 22 * scale)
    }

}

private struct ChurchNewsCarousel: View {
    let articles: [ChurchNewsArticle]
    @Binding var selection: Int
    let localization: LocalizationManager
    let pageHeight: CGFloat
    let openArticle: (ChurchNewsArticle) -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var displayedPage = 1

    private var circularArticles: [ChurchNewsArticle] {
        guard articles.count > 1, let first = articles.first, let last = articles.last else { return articles }
        return [last] + articles + [first]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(localization.t("news.title"))
                    .font(AppTheme.rounded(15, weight: .bold))
                    .tracking(1.4)
                Spacer()
                Text("\(selection + 1) / \(articles.count)")
                    .font(AppTheme.rounded(12, weight: .bold))
            }
            .foregroundStyle(AppTheme.glowGold)

            TabView(selection: $displayedPage) {
                ForEach(Array(circularArticles.enumerated()), id: \.offset) { index, article in
                    Button { openArticle(article) } label: {
                        HStack(alignment: .top, spacing: 12) {
                            NewsThumbnail(article: article, width: 122, height: max(166, pageHeight - 10))

                            VStack(alignment: .leading, spacing: 7) {
                                Text(article.title)
                                    .font(AppTheme.rounded(17, weight: .bold))
                                    .foregroundStyle(.white)
                                    .lineLimit(4)
                                    .multilineTextAlignment(.leading)
                                if !article.summary.isEmpty {
                                    Text(article.summary)
                                        .font(AppTheme.rounded(11, weight: .medium))
                                        .foregroundStyle(AppTheme.subtitleText)
                                        .lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                }
                                Spacer(minLength: 0)
                                Text("\(sourceLabel(for: article)) • \(article.licenseName)")
                                    .lineLimit(2)
                                Text(article.publishedAt.formatted(date: .abbreviated, time: .omitted))
                            }
                            .font(AppTheme.rounded(10, weight: .semibold))
                            .foregroundStyle(AppTheme.glowGold)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                            .clipped()
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint(localization.t("news.openHint"))
                    .tag(articles.count > 1 ? index : 0)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: pageHeight)
            .onAppear { synchronizeDisplayedPage() }
            .onChange(of: articles.map(\.id)) { _ in synchronizeDisplayedPage() }
            .onChange(of: displayedPage) { newPage in handleDisplayedPageChange(newPage) }
            .task(id: displayedPage) {
                guard articles.count > 1, !reduceMotion else { return }
                do {
                    try await Task.sleep(for: .seconds(10))
                } catch {
                    return
                }
                guard !Task.isCancelled else { return }
                withAnimation(.easeInOut(duration: 0.42)) {
                    displayedPage += 1
                }
            }

            HStack(spacing: 6) {
                Text(localization.t("news.swipe"))
                    .font(AppTheme.rounded(12, weight: .bold))
                Image(systemName: "hand.draw.fill")
                Spacer()
                ForEach(articles.indices, id: \.self) { index in
                    Capsule()
                        .fill(index == selection ? AppTheme.glowGold : Color.white.opacity(0.28))
                        .frame(width: index == selection ? 18 : 6, height: 6)
                }
            }
            .foregroundStyle(AppTheme.glowGold)
            .animation(.easeInOut(duration: 0.2), value: selection)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func sourceLabel(for article: ChurchNewsArticle) -> String {
        article.language == localization.language.contentLocale.rawValue
            ? article.sourceName
            : "\(article.sourceName) • \(article.language.uppercased())"
    }

    private func synchronizeDisplayedPage() {
        guard !articles.isEmpty else { return }
        displayedPage = articles.count > 1 ? min(selection, articles.count - 1) + 1 : 0
    }

    private func handleDisplayedPageChange(_ newPage: Int) {
        guard articles.count > 1 else {
            selection = 0
            return
        }
        if newPage == 0 {
            selection = articles.count - 1
            resetDisplayedPage(to: articles.count)
        } else if newPage == articles.count + 1 {
            selection = 0
            resetDisplayedPage(to: 1)
        } else {
            selection = newPage - 1
        }
    }

    private func resetDisplayedPage(to page: Int) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { displayedPage = page }
        }
    }
}

private struct ChurchNewsLoadingCard: View {
    let localization: LocalizationManager

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(localization.t("news.title"))
                .font(AppTheme.rounded(15, weight: .bold))
                .tracking(1.4)
                .foregroundStyle(AppTheme.glowGold)
            HStack(spacing: 12) {
                ProgressView().tint(AppTheme.glowGold)
                Text(localization.t("news.loading"))
                    .font(AppTheme.rounded(13, weight: .medium))
                    .foregroundStyle(AppTheme.subtitleText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NewsThumbnail: View {
    let article: ChurchNewsArticle
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            AsyncImage(url: article.imageURL) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                case .empty:
                    ZStack {
                        Color(red: 0.05, green: 0.12, blue: 0.18)
                        ProgressView().tint(AppTheme.glowGold)
                    }
                case .failure:
                    Color.clear
                @unknown default:
                    Color.clear
                }
            }
            .frame(width: width, height: height)
            .clipped()

            Text(article.imageCredit)
                .font(AppTheme.rounded(10, weight: .medium))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.65)
                .padding(.horizontal, 8)
                .padding(.vertical, 5)
                .background(Color.black.opacity(0.68))
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(article.imageAlt)
    }
}

private struct LanguagePickerSheet: View {
    @EnvironmentObject private var localization: LocalizationManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            AppBackdrop()
            VStack(alignment: .leading, spacing: 16) {
                Text(localization.t("home.chooseLanguage"))
                    .font(AppTheme.rounded(22, weight: .semibold))
                    .foregroundStyle(AppTheme.cardText)

                ForEach(AppLanguage.allCases) { language in
                    Button(language.displayName) {
                        localization.language = language
                        dismiss()
                    }
                    .buttonStyle(PrimaryPillButtonStyle())
                }

                HStack {
                    Spacer()
                    Button(localization.t("common.close")) {
                        dismiss()
                    }
                    .font(AppTheme.rounded(16, weight: .semibold))
                    .foregroundStyle(AppTheme.purpleOutline)
                }
                .padding(.top, 8)
            }
            .padding(24)
            .appGlassCard()
            .padding(16)
        }
    }
}

private struct HomeAction {
    let title: String
    let subtitle: String
    let icon: String
    let tint: Color
    let illustrationAssetName: String
    let action: () -> Void
}

private struct HomeFeatureCard: View {
    let action: HomeAction
    let scale: CGFloat

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 28 * scale, style: .continuous)
                .fill(cardGradient)
                .overlay(
                    RoundedRectangle(cornerRadius: 28 * scale, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [Color.white.opacity(0.06), Color.clear, Color.black.opacity(0.18)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 28 * scale, style: .continuous)
                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                )

            VStack(alignment: .leading, spacing: 14 * scale) {
                Spacer(minLength: 0)

                HStack(alignment: .center, spacing: 14 * scale) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14 * scale, style: .continuous)
                            .fill(Color.white.opacity(0.12))
                            .frame(width: 42 * scale, height: 42 * scale)

                        Image(systemName: action.icon)
                            .font(.system(size: 18 * scale, weight: .semibold))
                            .foregroundStyle(action.tint)
                    }

                    VStack(alignment: .leading, spacing: 4 * scale) {
                        Text(action.title)
                            .font(AppTheme.rounded(22 * scale, weight: .bold))
                            .foregroundStyle(.white)
                        Text(action.subtitle)
                            .font(AppTheme.rounded(14 * scale, weight: .medium))
                            .foregroundStyle(Color.white.opacity(0.78))
                            .multilineTextAlignment(.leading)
                    }

                    Spacer(minLength: 0)

                    ZStack {
                        Circle()
                            .fill(Color.white.opacity(0.12))
                            .frame(width: 32 * scale, height: 32 * scale)

                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 12 * scale, weight: .bold))
                            .foregroundStyle(Color.white.opacity(0.78))
                    }
                }
            }
            .padding(.horizontal, 22 * scale)
            .padding(.vertical, 20 * scale)

            VStack {
                HStack {
                    Spacer()

                    Image(action.illustrationAssetName)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 156 * scale, height: 108 * scale)
                        .clipShape(RoundedRectangle(cornerRadius: 22 * scale, style: .continuous))
                        .shadow(color: Color.black.opacity(0.24), radius: 18 * scale, x: 0, y: 10 * scale)
                }

                Spacer()
            }
            .padding(.top, 16 * scale)
            .padding(.trailing, 16 * scale)
            .allowsHitTesting(false)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 188 * scale)
        .shadow(color: Color.black.opacity(0.24), radius: 18 * scale, x: 0, y: 10 * scale)
    }

    private var cardGradient: LinearGradient {
        switch action.illustrationAssetName {
        case "HomeCardSaints":
            return LinearGradient(
                colors: [Color(hex: "#153646").opacity(0.92), Color(hex: "#1C5461").opacity(0.76)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case "HomeCardNovenas":
            return LinearGradient(
                colors: [Color(hex: "#0D2535").opacity(0.94), Color(hex: "#1B576C").opacity(0.72)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case "HomeCardPrayers":
            return LinearGradient(
                colors: [Color(hex: "#2C3144").opacity(0.9), Color(hex: "#15424D").opacity(0.72)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case "HomeCardRosary":
            return LinearGradient(
                colors: [Color(hex: "#30384F").opacity(0.92), Color(hex: "#123E4D").opacity(0.74)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case "HomeCardDailyReadings":
            return LinearGradient(
                colors: [Color(hex: "#1C514C").opacity(0.9), Color(hex: "#143B4D").opacity(0.74)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case "HomeCardIntentions":
            return LinearGradient(
                colors: [Color(hex: "#4C3B56").opacity(0.9), Color(hex: "#15404B").opacity(0.74)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        default:
            return LinearGradient(
                colors: [AppTheme.cardBackground, AppTheme.cardBackground.opacity(0.76)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView(environment: .local(), hasPlayedIntro: .constant(false))
    }
}
