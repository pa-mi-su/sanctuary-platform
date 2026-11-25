import SwiftUI

struct HomeView: View {
    let environment: AppEnvironment
    var isObscured = false
    @State private var heroIsVisible = true
    @EnvironmentObject private var localization: LocalizationManager

    @State private var showPrayersSearch = false
    @State private var showRosarySearch = false
    @State private var showIntentionsSearch = false
    @State private var showPatronageSearch = false
    @State private var showDailyReadings = false
    @State private var dailyReadingsURLOverride: URL?

    private var dailyReadingsURL: URL {
        dailyReadingsURLOverride ?? localization.language.dailyReadingsLandingURL
    }

    private var primaryActions: [HomeAction] {
        [
            HomeAction(
                id: .daily,
                title: localization.t("home.daily"),
                subtitle: localization.t("home.dailySubtitle"),
                icon: "sun.max.fill"
            ) {
                showDailyReadings = true
            },
            HomeAction(
                id: .prayers,
                title: localization.t("home.prayers"),
                subtitle: localization.t("home.prayersSubtitle"),
                icon: "hands.sparkles.fill"
            ) {
                showPrayersSearch = true
            },
            HomeAction(
                id: .patronage,
                title: localization.t("home.patronage"),
                subtitle: localization.t("home.patronageSubtitle"),
                icon: "person.crop.circle.badge.checkmark"
            ) {
                showPatronageSearch = true
            },
            HomeAction(
                id: .intentions,
                title: localization.t("home.intentions"),
                subtitle: localization.t("home.intentionsSubtitle"),
                icon: "heart.text.square.fill"
            ) {
                showIntentionsSearch = true
            },
            HomeAction(
                id: .rosary,
                title: localization.t("home.rosary"),
                subtitle: localization.t("home.rosarySubtitle"),
                icon: "circle"
            ) {
                showRosarySearch = true
            }
        ]
    }

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                let width = proxy.size.width
                let contentWidth = min(width, 760)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        HomeFeaturedCarousel(
                            actions: primaryActions,
                            isObscured: isObscured || showPrayersSearch || showRosarySearch
                                || showIntentionsSearch || showPatronageSearch || showDailyReadings
                        )
                        .background {
                            GeometryReader { geometry in
                                Color.clear.preference(key: HomeHeroVisibilityKey.self,
                                    value: geometry.frame(in: .named("homeScroll")).maxY)
                            }
                        }
                        .overlay(alignment: .top) {
                            HStack {
                                Text(localization.t("tab.home"))
                                    .font(.system(.largeTitle, weight: .bold))
                                    .foregroundStyle(AppTheme.onAccent)
                                Spacer()
                                Image("BrandLogo")
                                    .resizable().scaledToFit()
                                    .frame(width: 42, height: 42)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .accessibilityLabel("Sanctuary")
                            }
                            .padding(.horizontal, 24)
                            .padding(.top, 12)
                        }

                        LinearGradient(colors: [AppTheme.heroBase, AppTheme.homeSurface],
                            startPoint: .top, endPoint: .bottom)
                            .frame(height: 36)
                            .accessibilityHidden(true)

                        VStack(spacing: 32) {
                            HStack(alignment: .top, spacing: 10) {
                                ForEach(primaryActions.filter { $0.id != .rosary }) { action in
                                    Button(action: action.action) {
                                        VStack(spacing: 9) {
                                            Image(systemName: action.icon)
                                                .font(.system(size: 24, weight: .regular))
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 62)
                                                .background(AppTheme.homeSurfaceRaised, in: RoundedRectangle(cornerRadius: 16))
                                            Text(action.title)
                                                .font(.system(size: 11, weight: .semibold))
                                                .multilineTextAlignment(.center)
                                        }
                                        .foregroundStyle(AppTheme.homeInk)
                                        .frame(maxWidth: .infinity, alignment: .top)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.horizontal, 24)

                            VStack(spacing: 12) {
                                Text("MATTHEW 11:28")
                                    .font(.caption)
                                    .tracking(1.2)
                                    .foregroundStyle(AppTheme.homeMuted)
                                Text(localization.t("home.verse"))
                                    .font(.system(.title2, design: .serif))
                                    .lineSpacing(4)
                                    .multilineTextAlignment(.center)
                                    .foregroundStyle(AppTheme.homeInk)
                            }
                            .padding(.horizontal, 32)
                            .padding(.vertical, 8)

                            HomeDailyPrayerList(
                                title: localization.t("home.explore"),
                                actions: primaryActions.filter { [.daily, .prayers, .rosary].contains($0.id) }
                            )

                            HomeFeatureShelf(
                                title: localization.t("home.guidance"),
                                actions: primaryActions.filter { [.patronage, .intentions].contains($0.id) }
                            )
                        }
                        .padding(.top, 24)
                        .padding(.bottom, 112)
                        .background(AppTheme.homeSurface)
                    }
                    .frame(maxWidth: contentWidth)
                    .frame(maxWidth: .infinity)
                }
                .coordinateSpace(name: "homeScroll")
                .onPreferenceChange(HomeHeroVisibilityKey.self) { heroIsVisible = $0 > 0 }
                .background(AppTheme.homeSurface, ignoresSafeAreaEdges: [])
            }
            .background((heroIsVisible ? AppTheme.heroBase : AppTheme.homeSurface).ignoresSafeArea(edges: .top))
            .preferredColorScheme(heroIsVisible ? .dark : .light)
            .task(id: localization.language) {
                await loadDailyReadingsURL()
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

    private func loadDailyReadingsURL() async {
        do {
            if let liturgicalDay = try await environment.contentRepository.fetchLiturgicalDay(for: Date()),
               let localized = localization.language.localizedDailyReadingsURL(
                   from: liturgicalDay.readingURL?.absoluteString
               ) {
                dailyReadingsURLOverride = localized
            } else {
                dailyReadingsURLOverride = localization.language.dailyReadingsLandingURL
            }
        } catch {
            dailyReadingsURLOverride = localization.language.dailyReadingsLandingURL
        }
    }
}

struct LanguagePickerSheet: View {
    @EnvironmentObject private var localization: LocalizationManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            AppBackdrop()
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(localization.t("home.chooseLanguage"))
                        .font(AppTheme.rounded(24, weight: .bold)).foregroundStyle(AppTheme.cardText)
                        .padding(.bottom, 4)
                    ForEach(AppLanguage.allCases) { language in
                        Button {
                            localization.language = language
                            dismiss()
                        } label: {
                            HStack {
                                Text(language.displayName).font(AppTheme.rounded(17, weight: .semibold))
                                Spacer()
                                if language == localization.language { Image(systemName: "checkmark").foregroundStyle(AppTheme.tabActive) }
                            }
                            .foregroundStyle(AppTheme.cardText).padding(.horizontal, 18).padding(.vertical, 16)
                            .frame(maxWidth: .infinity, minHeight: 56)
                            .background(language == localization.language ? AppTheme.cardBackgroundSoft : AppTheme.cardBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                            .overlay(RoundedRectangle(cornerRadius: 18).stroke(AppTheme.tabBorder, lineWidth: 1))
                        }.buttonStyle(.plain)
                    }
                }.padding(.horizontal, 16).padding(.top, 24).padding(.bottom, 24)
            }
        }
    }
}

private struct HomeHeroVisibilityKey: PreferenceKey {
    static var defaultValue: CGFloat = 1
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private enum HomeDestination {
    case daily, prayers, patronage, intentions, rosary
}

private struct HomeAction: Identifiable {
    let id: HomeDestination
    let title: String
    let subtitle: String
    let icon: String
    let action: () -> Void
}

/// Sentinel pages at both ends make manual swipes and automatic advances loop forward.
private struct HomeFeaturedCarousel: View {
    let actions: [HomeAction]
    let isObscured: Bool
    @EnvironmentObject private var localization: LocalizationManager
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOverEnabled
    @ScaledMetric(relativeTo: .body) private var heroHeight: CGFloat = 470
    @GestureState private var isDragging = false
    @State private var position = 1
    @State private var isPaused = false
    @State private var isVisible = false

    private var pageCount: Int { actions.count + 1 }
    private var selectedPage: Int { logicalPage(position) }
    private func logicalPage(_ position: Int) -> Int { (position - 1 + pageCount) % pageCount }
    private func title(for page: Int) -> String {
        page == 0 ? localization.t("home.welcome") : actions[page - 1].title
    }
    private var playbackKey: String {
        "\(position)-\(isPaused)-\(isVisible)-\(scenePhase)-\(isObscured)-\(isDragging)-\(reduceMotion)-\(voiceOverEnabled)"
    }

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $position) {
                ForEach(0..<(pageCount + 2), id: \.self) { index in
                    Group {
                        if logicalPage(index) == 0 {
                            HomeWelcomeSlide()
                        } else {
                            HomeHeroSlide(action: actions[logicalPage(index) - 1], openLabel: localization.t("home.open"))
                        }
                    }
                    .tag(index)
                    .accessibilityHidden(logicalPage(index) != selectedPage)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: heroHeight)
            .simultaneousGesture(DragGesture(minimumDistance: 10).updating($isDragging) { _, state, _ in state = true })

            HStack(spacing: 0) {
                ForEach(0..<pageCount, id: \.self) { index in
                    Button {
                        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.35)) { position = index + 1 }
                    } label: {
                        Capsule()
                            .fill(Color.white.opacity(selectedPage == index ? 0.95 : 0.25))
                            .frame(width: selectedPage == index ? 20 : 5, height: 5)
                            .frame(width: 40, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(title(for: index))
                    .accessibilityValue("\(index + 1) / \(pageCount)")
                    .accessibilityAddTraits(selectedPage == index ? .isSelected : [])
                }
                Button {
                    isPaused.toggle()
                } label: {
                    Image(systemName: isPaused ? "play.fill" : "pause.fill")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(Color.white.opacity(0.6))
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(localization.t(isPaused ? "home.resumeCarousel" : "home.pauseCarousel"))
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 12)
        }
        .background(AppTheme.heroBase)
        .onAppear { isVisible = true }
        .onDisappear { isVisible = false }
        .task(id: position) {
            guard position == 0 || position == pageCount + 1 else { return }
            let boundary = position
            do { try await Task.sleep(nanoseconds: reduceMotion ? 0 : 400_000_000) } catch { return }
            guard position == boundary else { return }
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { position = boundary == 0 ? pageCount : 1 }
        }
        .task(id: playbackKey) {
            guard isVisible, scenePhase == .active, !isPaused, !isObscured,
                  !isDragging, !reduceMotion, !voiceOverEnabled else { return }
            do { try await Task.sleep(nanoseconds: 7_000_000_000) } catch { return }
            withAnimation(.easeInOut(duration: 0.4)) { position += 1 }
        }
    }
}

private extension HomeAction {
    var editorialAsset: String {
        switch id {
        case .rosary: return "HomeEditorialRosary"
        case .patronage: return "HomeEditorialWelcome"
        default: return "HomeEditorialScripture"
        }
    }
}

private struct HomeEditorialBackground: View {
    let asset: String

    var body: some View {
        GeometryReader { proxy in
            Image(asset)
                .resizable().scaledToFill()
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipped()
                .overlay {
                    LinearGradient(
                        stops: [.init(color: .black.opacity(0.25), location: 0),
                                .init(color: .clear, location: 0.38),
                                .init(color: AppTheme.heroBase.opacity(0.8), location: 0.73),
                                .init(color: AppTheme.heroBase, location: 1)],
                        startPoint: .top, endPoint: .bottom
                    )
                }
        }
        .accessibilityHidden(true)
    }
}

private struct HomeWelcomeSlide: View {
    @EnvironmentObject private var localization: LocalizationManager

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Spacer()
            Text(localization.t("home.heroEyebrow"))
                .font(.caption.weight(.medium))
                .padding(.horizontal, 9).padding(.vertical, 5)
                .background(.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 5))
            Text(localization.t("home.welcome"))
                .font(.system(.title, weight: .bold))
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(localization.t("home.welcomeBody"))
                .font(.body)
                .foregroundStyle(AppTheme.onAccent.opacity(0.85))
        }
        .foregroundStyle(AppTheme.onAccent)
        .padding(.horizontal, 24)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        .background { HomeEditorialBackground(asset: "HomeEditorialWelcome") }
    }
}

private struct HomeHeroSlide: View {
    let action: HomeAction
    let openLabel: String

    var body: some View {
        Button(action: action.action) {
            VStack(alignment: .leading, spacing: 12) {
                Spacer()
                Image(systemName: "arrow.right")
                    .font(.system(size: 23, weight: .semibold))
                    .foregroundStyle(AppTheme.heroBase)
                    .frame(width: 60, height: 60)
                    .background(.white, in: Circle())
                    .frame(maxWidth: .infinity)
                Spacer()
                Text(action.subtitle)
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 9).padding(.vertical, 5)
                    .background(.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 5))
                Text(action.title)
                    .font(.system(.title, weight: .bold))
                    .fixedSize(horizontal: false, vertical: true)
                Text(openLabel)
                    .font(.body)
                    .foregroundStyle(AppTheme.onAccent.opacity(0.85))
            }
            .foregroundStyle(AppTheme.onAccent)
            .padding(.horizontal, 24)
            .padding(.top, 90)
            .padding(.bottom, 16)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
            .background { HomeEditorialBackground(asset: action.editorialAsset) }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(action.title). \(action.subtitle)")
        .accessibilityHint(openLabel)
    }
}

private struct HomeDailyPrayerList: View {
    let title: String
    let actions: [HomeAction]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.system(.title2, weight: .bold))
                .accessibilityAddTraits(.isHeader)
            ForEach(actions) { action in
                Button(action: action.action) {
                    HStack(spacing: 14) {
                        Image(action.editorialAsset)
                            .resizable().scaledToFill()
                            .frame(width: 68, height: 68)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 5) {
                            Text(action.title).font(.headline).fixedSize(horizontal: false, vertical: true)
                            Text(action.subtitle).font(.subheadline).fixedSize(horizontal: false, vertical: true)
                                .foregroundStyle(AppTheme.homeMuted)
                        }
                        Spacer(minLength: 0)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(AppTheme.homeMuted)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .foregroundStyle(AppTheme.homeInk)
        .padding(.horizontal, 24)
    }
}

private struct HomeFeatureShelf: View {
    let title: String
    let actions: [HomeAction]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.system(.title2, weight: .bold))
                .accessibilityAddTraits(.isHeader)
                .padding(.horizontal, 24)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], alignment: .leading, spacing: 12) {
                ForEach(actions) { action in
                    Button(action: action.action) {
                        VStack(alignment: .leading, spacing: 8) {
                            GeometryReader { geometry in
                                Image(action.editorialAsset)
                                    .resizable().scaledToFill()
                                    .frame(width: geometry.size.width, height: 128)
                                    .clipped()
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .accessibilityHidden(true)
                            }
                            .frame(height: 128)
                            Text(action.title).font(.headline)
                                .fixedSize(horizontal: false, vertical: true)
                            Text(action.subtitle).font(.subheadline)
                                .foregroundStyle(AppTheme.homeMuted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .multilineTextAlignment(.leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .foregroundStyle(AppTheme.homeInk)
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView(environment: .local())
    }
}
