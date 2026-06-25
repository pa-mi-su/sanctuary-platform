import SwiftUI

struct HomeView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager

    @State private var revealContent = false
    @State private var showLanguageDialog = false
    @State private var showAbout = false
    @State private var showPrayersSearch = false
    @State private var showRosarySearch = false
    @State private var showSaintsList = false
    @State private var showNovenasSearch = false
    @State private var showIntentionsSearch = false
    @State private var showDailyReadings = false
    @State private var dailyReadingsURLOverride: URL?

    private var dailyReadingsURL: URL {
        dailyReadingsURLOverride ?? localization.language.dailyReadingsLandingURL
    }

    private var primaryActions: [HomeAction] {
        [
            HomeAction(
                title: localization.t("home.saints"),
                subtitle: localization.t("home.saintsSubtitle"),
                icon: "person.3.fill",
                tint: AppTheme.glowGold,
                illustrationAssetName: "HomeCardSaints"
            ) {
                showSaintsList = true
            },
            HomeAction(
                title: localization.t("tab.novenas"),
                subtitle: localization.t("home.novenasSubtitle"),
                icon: "book.closed.fill",
                tint: AppTheme.glowBlue,
                illustrationAssetName: "HomeCardNovenas"
            ) {
                showNovenasSearch = true
            },
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
                title: localization.t("home.daily"),
                subtitle: localization.t("home.dailySubtitle"),
                icon: "sun.max.fill",
                tint: AppTheme.todayHighlight,
                illustrationAssetName: "HomeCardDailyReadings"
            ) {
                showDailyReadings = true
            },
            HomeAction(
                title: localization.t("home.intentions"),
                subtitle: localization.t("home.intentionsSubtitle"),
                icon: "heart.text.square.fill",
                tint: AppTheme.glowRose,
                illustrationAssetName: "HomeCardIntentions"
            ) {
                showIntentionsSearch = true
            }
        ]
    }

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                let width = proxy.size.width
                let scale = ResponsiveLayout.scale(for: width)
                let logoSize = ResponsiveLayout.value(146, width: width)
                let contentWidth = max(0, min(width - 24, 760))

                ZStack {
                    AppBackdrop()

                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 16 * scale) {
                            HStack(spacing: 10 * scale) {
                                TopActionButton(title: localization.t("home.about"), icon: "info.circle") {
                                    showAbout = true
                                }
                                TopActionButton(title: "\(localization.t("home.language")): \(localization.language.displayName)", icon: "translate") {
                                    showLanguageDialog = true
                                }
                            }
                            .padding(.top, 8 * scale)
                            .opacity(revealContent ? 1 : 0)
                            .offset(y: revealContent ? 0 : -8)

                            VStack(spacing: 16 * scale) {
                                Text(localization.t("home.eyebrow"))
                                    .font(AppTheme.rounded(13 * scale, weight: .bold))
                                    .tracking(1.8 * scale)
                                    .foregroundStyle(AppTheme.heroEyebrow)
                                    .multilineTextAlignment(.center)

                                ZStack {
                                    Circle()
                                        .fill(AppTheme.glowGold.opacity(0.22))
                                        .frame(width: logoSize + (44 * scale), height: logoSize + (44 * scale))
                                        .blur(radius: 16)

                                    Image("BrandLogo")
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: logoSize, height: logoSize)
                                        .clipShape(RoundedRectangle(cornerRadius: 30 * scale, style: .continuous))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 30 * scale, style: .continuous)
                                                .stroke(Color.white.opacity(0.14), lineWidth: 1)
                                        )
                                        .shadow(color: Color.black.opacity(0.26), radius: 22, x: 0, y: 12)
                                }
                                .padding(.top, 6 * scale)

                                VStack(spacing: 10 * scale) {
                                    Text(localization.t("home.welcome"))
                                        .font(AppTheme.rounded(31 * scale, weight: .bold))
                                        .minimumScaleFactor(0.7)
                                        .multilineTextAlignment(.center)
                                        .foregroundStyle(.white)

                                    Text(localization.t("home.connect"))
                                        .font(AppTheme.rounded(17 * scale, weight: .semibold))
                                        .multilineTextAlignment(.center)
                                        .foregroundStyle(AppTheme.subtitleText)
                                }

                                Text(localization.t("home.supporting"))
                                    .font(AppTheme.rounded(14 * scale, weight: .medium))
                                    .foregroundStyle(Color.white.opacity(0.65))
                                    .multilineTextAlignment(.center)
                            }
                            .padding(.horizontal, 20 * scale)
                            .padding(.vertical, 18 * scale)
                            .frame(maxWidth: .infinity)
                            .appGlassCard(cornerRadius: 30 * scale)
                            .opacity(revealContent ? 1 : 0)
                            .offset(y: revealContent ? 0 : 14)

                            VStack(spacing: 16 * scale) {
                                ForEach(Array(primaryActions.enumerated()), id: \.offset) { index, action in
                                    Button(action: action.action) {
                                        HomeFeatureCard(action: action, scale: scale)
                                    }
                                    .buttonStyle(.plain)
                                        .opacity(revealContent ? 1 : 0)
                                        .offset(y: revealContent ? 0 : CGFloat(18 + (index * 5)))
                                        .animation(
                                            .spring(response: 0.54, dampingFraction: 0.86)
                                                .delay(0.08 + (Double(index) * 0.04)),
                                            value: revealContent
                                        )
                                }
                            }
                            .padding(.bottom, 18 * scale)
                        }
                        .frame(maxWidth: contentWidth)
                        .padding(.horizontal, 12 * scale)
                        .padding(.top, 6 * scale)
                        .padding(.bottom, 16 * scale)
                    }
                }
            }
            .task {
                guard !revealContent else { return }
                withAnimation(.spring(response: 0.62, dampingFraction: 0.88)) {
                    revealContent = true
                }
            }
            .task(id: localization.language) {
                await loadDailyReadingsURL()
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
            .fullScreenCover(isPresented: $showSaintsList) {
                SaintsSearchView(environment: environment)
            }
            .fullScreenCover(isPresented: $showNovenasSearch) {
                NovenasSearchView(environment: environment)
            }
            .fullScreenCover(isPresented: $showIntentionsSearch) {
                NovenasSearchView(environment: environment, mode: .intentions)
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
        HomeView(environment: .local())
    }
}
