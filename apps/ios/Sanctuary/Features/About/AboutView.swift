import SwiftUI

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager

    private let desktopURL = "https://mydailysanctuary.com"
    private let supportEmail = "mailto:info@mydailysanctuary.com"
    private let usccbURL = "https://bible.usccb.org/daily-bible-reading"
    private let wikipediaURL = "https://www.wikipedia.org/"
    private let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—"
    private let buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "—"
    private let environment = AboutView.nonProductionEnvironmentLabel

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 14) {
                        AboutCard(title: localization.t("about.title")) {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Sanctuary")
                                    .font(AppTheme.rounded(13, weight: .bold))
                                    .tracking(1.6)
                                    .foregroundStyle(AppTheme.heroEyebrow)

                                Text(localization.t("about.heroTitle"))
                                    .font(AppTheme.rounded(30, weight: .bold))
                                    .foregroundStyle(.white)

                                Text(localization.t("about.subtitle"))
                                    .foregroundStyle(AppTheme.cardText.opacity(0.9))
                            }
                        }

                        AboutCard(title: localization.t("about.desktopVersion")) {
                            Text(localization.t("about.desktopBody"))
                            LinkButton(title: localization.t("about.link.desktop"), url: desktopURL)
                        }

                        AboutCard(title: localization.t("about.versionTitle")) {
                            Text("\(localization.t("about.versionLabel")): \(appVersion)")
                            Text("\(localization.t("about.buildLabel")): \(buildNumber)")
                            if let environment {
                                Text("\(localization.t("about.environmentLabel")): \(environment)")
                            }
                        }

                        AboutCard(title: localization.t("about.whatsInApp")) {
                            Text(localization.t("about.item.liturgical"))
                            Text(localization.t("about.item.saints"))
                            Text(localization.t("about.item.novenas"))
                        }

                        AboutCard(title: localization.t("about.references")) {
                            Text(localization.t("about.refsIntro"))
                            Text(localization.t("about.source.usccb"))
                            Text(localization.t("about.source.wikipedia"))
                                .padding(.bottom, 8)

                            LinkButton(title: localization.t("about.link.usccb"), url: usccbURL)
                            LinkButton(title: localization.t("about.link.wikipedia"), url: wikipediaURL)
                        }

                        AboutCard(title: localization.t("about.contact")) {
                            Text(localization.t("about.contactBodyDetailed"))

                            VStack(spacing: 10) {
                                LinkButton(title: localization.t("about.link.emailSupport"), url: supportEmail)

                                NavigationLink(value: AboutDestination.support) {
                                    ActionPillLabel(title: localization.t("about.link.support"))
                                }
                                .buttonStyle(.plain)

                                NavigationLink(value: AboutDestination.privacy) {
                                    ActionPillLabel(title: localization.t("about.link.privacy"))
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        Text(localization.t("about.copyright"))
                            .font(AppTheme.rounded(14, weight: .medium))
                            .foregroundStyle(AppTheme.cardText.opacity(0.88))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.top, 6)
                    }
                    .padding(16)
                    .padding(.bottom, 24)
                }
            }
            .navigationDestination(for: AboutDestination.self) { destination in
                AboutDocumentView(destination: destination)
            }
            .toolbar {
#if os(iOS)
                ToolbarItem(placement: .topBarLeading) {
                    Button(localization.t("common.close")) { dismiss() }
                        .foregroundStyle(.white)
                }
#else
                ToolbarItem(placement: .navigation) {
                    Button(localization.t("common.close")) { dismiss() }
                        .foregroundStyle(.white)
                }
#endif
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private static var nonProductionEnvironmentLabel: String? {
        let bundleID = Bundle.main.bundleIdentifier ?? ""
        if bundleID.hasSuffix(".dev") { return "DEV" }
        if bundleID.hasSuffix(".uat") { return "UAT" }
        return nil
    }
}

private enum AboutDestination: Hashable {
    case support
    case privacy
}

private struct AboutCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.rounded(24, weight: .bold))
                .foregroundStyle(AppTheme.cardText)

            Divider().background(AppTheme.cardText.opacity(0.25))

            VStack(alignment: .leading, spacing: 6) {
                content
            }
            .font(AppTheme.rounded(17, weight: .medium))
            .foregroundStyle(AppTheme.cardText.opacity(0.9))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }
}

private struct LinkButton: View {
    let title: String
    let url: String

    var body: some View {
        Link(destination: URL(string: url)!) {
            Text(title)
                .font(AppTheme.rounded(16, weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .buttonStyle(PrimaryPillButtonStyle())
    }
}

private struct DisabledButton: View {
    let title: String

    var body: some View {
        Text(title)
            .font(AppTheme.rounded(16, weight: .semibold))
            .foregroundStyle(Color.white.opacity(0.82))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(AppTheme.cardBackgroundSoft)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct ActionPillLabel: View {
    let title: String

    var body: some View {
        Text(title)
            .font(AppTheme.rounded(16, weight: .semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(AppTheme.cardBackgroundSoft)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(AppTheme.purpleOutline.opacity(0.6), lineWidth: 1)
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

private struct AboutDocumentView: View {
    let destination: AboutDestination
    @EnvironmentObject private var localization: LocalizationManager

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    Text("Sanctuary")
                        .font(AppTheme.rounded(13, weight: .bold))
                        .tracking(1.6)
                        .foregroundStyle(AppTheme.heroEyebrow)

                    Text(title)
                        .font(AppTheme.rounded(30, weight: .bold))
                        .foregroundStyle(.white)

                    if destination == .privacy {
                        Text(localization.t("about.privacyEffectiveDate"))
                            .font(AppTheme.rounded(15, weight: .semibold))
                            .foregroundStyle(AppTheme.subtitleText)
                    }

                    VStack(alignment: .leading, spacing: 18) {
                        ForEach(sections, id: \.title) { section in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(section.title)
                                    .font(AppTheme.rounded(20, weight: .bold))
                                    .foregroundStyle(.white)

                                Text(section.body)
                                    .font(AppTheme.rounded(16, weight: .medium))
                                    .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }

                    LinkButton(title: localization.t("about.link.emailSupport"), url: "mailto:info@mydailysanctuary.com")

                    Text(localization.t("about.copyright"))
                        .font(AppTheme.rounded(14, weight: .medium))
                        .foregroundStyle(AppTheme.cardText.opacity(0.88))
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 6)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .appGlassCard(cornerRadius: 28)
                .padding(16)
                .padding(.bottom, 24)
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private var title: String {
        switch destination {
        case .support:
            return localization.t("about.link.support")
        case .privacy:
            return localization.t("about.link.privacy")
        }
    }

    private var sections: [(title: String, body: String)] {
        switch destination {
        case .support:
            return [
                (localization.t("about.support.helpTitle"), localization.t("about.support.helpBody")),
                (localization.t("about.support.featuresTitle"), localization.t("about.support.featuresBody")),
                (localization.t("about.support.responseTitle"), localization.t("about.support.responseBody"))
            ]
        case .privacy:
            return [
                (localization.t("about.privacy.collectTitle"), localization.t("about.privacy.collectBody")),
                (localization.t("about.privacy.locationTitle"), localization.t("about.privacy.locationBody")),
                (localization.t("about.privacy.notificationsTitle"), localization.t("about.privacy.notificationsBody")),
                (localization.t("about.privacy.sharingTitle"), localization.t("about.privacy.sharingBody")),
                (localization.t("about.privacy.choicesTitle"), localization.t("about.privacy.choicesBody")),
                (localization.t("about.privacy.contactTitle"), localization.t("about.privacy.contactBody"))
            ]
        }
    }
}

struct AboutView_Previews: PreviewProvider {
    static var previews: some View {
        AboutView()
    }
}
