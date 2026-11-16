import SwiftUI

struct AboutView: View {
  @Environment(\.dismiss) private var dismiss
  @EnvironmentObject private var localization: LocalizationManager

  private let desktopURL = "https://mydailysanctuary.com"
  private let supportEmail = "mailto:info@mydailysanctuary.com"
  private let usccbURL = "https://bible.usccb.org/daily-bible-reading"
  private let wikipediaURL = "https://www.wikipedia.org/"
  private let appVersion =
    Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—"
  private let buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "—"
  private let environment = AboutView.nonProductionEnvironmentLabel

  var body: some View {
    NavigationStack {
      ZStack(alignment: .topLeading) {
        AppBackdrop()

        ScrollView(showsIndicators: false) {
          VStack(alignment: .leading, spacing: 14) {
            Text(localization.t("about.heroTitle"))
              .font(AppTheme.rounded(34, weight: .bold)).foregroundStyle(.white)
            Text(localization.t("about.subtitle"))
              .font(AppTheme.rounded(16, weight: .medium)).foregroundStyle(AppTheme.heroEyebrow)

            AboutCard(title: "Sanctuary") {
              VStack(alignment: .leading, spacing: 10) {
                Text("Sanctuary")
                  .font(AppTheme.rounded(13, weight: .bold))
                  .tracking(1.6)
                  .foregroundStyle(AppTheme.heroEyebrow)

                Text(localization.t("about.heroTitle"))
                  .font(AppTheme.rounded(28, weight: .bold))
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

              LinkButton(title: localization.t("about.link.usccb"), url: usccbURL)
              LinkButton(title: localization.t("about.link.wikipedia"), url: wikipediaURL)
            }

            AboutCard(title: localization.t("about.contact")) {
              Text(localization.t("about.contactBodyDetailed"))

              VStack(spacing: 10) {
                LinkButton(title: localization.t("about.link.emailSupport"), url: supportEmail)

                NavigationLink(value: AboutDestination.support) {
                  Text(localization.t("about.link.support"))
                }
                .buttonStyle(AboutActionStyle(secondary: true))

                NavigationLink(value: AboutDestination.privacy) {
                  Text(localization.t("about.link.privacy"))
                }
                .buttonStyle(AboutActionStyle(secondary: true))
              }
            }

            Text(localization.t("about.copyright"))
              .font(AppTheme.rounded(14, weight: .medium))
              .foregroundStyle(AppTheme.cardText.opacity(0.88))
              .frame(maxWidth: .infinity, alignment: .center)
              .padding(.top, 6)
          }
          .padding(.horizontal, 24)
          .padding(.top, 76)
          .padding(.bottom, 48)
        }
        Button {
          dismiss()
        } label: {
          Image(systemName: "arrow.left").font(.system(size: 20, weight: .medium))
            .foregroundStyle(.white).frame(width: 42, height: 42)
            .background(AppTheme.cardBackground).clipShape(Circle())
            .overlay(Circle().stroke(AppTheme.tabBorder, lineWidth: 1))
        }
        .accessibilityLabel(localization.t("common.close"))
        .padding(.leading, 18).padding(.top, 12)
      }
      .navigationDestination(for: AboutDestination.self) { destination in
        AboutDocumentView(destination: destination)
      }
      .toolbar(.hidden, for: .navigationBar)
    }
    .navigationBarTitleDisplayMode(.inline)
    .toolbarBackground(.hidden, for: .navigationBar)
    .toolbarColorScheme(.dark, for: .navigationBar)
    .leftEdgeSwipeBack(dismiss.callAsFunction)
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

      VStack(alignment: .leading, spacing: 10) {
        content
      }
      .font(AppTheme.rounded(16, weight: .medium))
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
    }
    .buttonStyle(AboutActionStyle())
  }
}

private struct AboutActionStyle: ButtonStyle {
  var secondary = false
  func makeBody(configuration: Configuration) -> some View {
    configuration.label
      .font(AppTheme.rounded(14, weight: .semibold))
      .foregroundStyle(.white)
      .padding(.horizontal, 16).padding(.vertical, 10)
      .frame(maxWidth: .infinity, minHeight: 44)
      .background {
        if secondary { AppTheme.cardBackgroundSoft } else { AppTheme.primaryButtonGradient }
      }
      .clipShape(RoundedRectangle(cornerRadius: 20))
      .overlay(
        RoundedRectangle(cornerRadius: 20).stroke(
          secondary ? AppTheme.purpleOutline.opacity(0.6) : Color.white.opacity(0.16), lineWidth: 1)
      )
      .opacity(configuration.isPressed ? 0.82 : 1)
  }
}

private struct AboutDocumentView: View {
  let destination: AboutDestination
  @EnvironmentObject private var localization: LocalizationManager
  @Environment(\.dismiss) private var dismiss

  var body: some View {
    ZStack(alignment: .topLeading) {
      AppBackdrop()
      ScrollView(showsIndicators: false) {
        VStack(alignment: .leading, spacing: 16) {
          Text(title).font(AppTheme.rounded(34, weight: .bold)).foregroundStyle(.white)
          if destination == .privacy {
            Text(localization.t("about.privacyEffectiveDate"))
              .font(AppTheme.rounded(16, weight: .medium)).foregroundStyle(AppTheme.heroEyebrow)
          }
          ForEach(sections, id: \.title) { section in
            AboutCard(title: section.title) {
              Text(section.body).fixedSize(horizontal: false, vertical: true)
            }
          }
          LinkButton(
            title: localization.t("about.link.emailSupport"),
            url: "mailto:info@mydailysanctuary.com")
          Text(localization.t("about.copyright"))
            .font(AppTheme.rounded(14, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
            .frame(maxWidth: .infinity).padding(.top, 6)
        }
        .padding(.horizontal, 24).padding(.top, 76).padding(.bottom, 48)
      }
      Button {
        dismiss()
      } label: {
        Image(systemName: "arrow.left").font(.system(size: 20, weight: .medium))
          .foregroundStyle(.white).frame(width: 42, height: 42)
          .background(AppTheme.cardBackground).clipShape(Circle())
          .overlay(Circle().stroke(AppTheme.tabBorder, lineWidth: 1))
      }.accessibilityLabel(localization.t("common.close"))
        .padding(.leading, 18).padding(.top, 12)
    }
    .toolbar(.hidden, for: .navigationBar)
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
        (
          localization.t("about.support.featuresTitle"),
          localization.t("about.support.featuresBody")
        ),
        (
          localization.t("about.support.responseTitle"),
          localization.t("about.support.responseBody")
        ),
      ]
    case .privacy:
      return [
        (localization.t("about.privacy.collectTitle"), localization.t("about.privacy.collectBody")),
        (
          localization.t("about.privacy.locationTitle"),
          localization.t("about.privacy.locationBody")
        ),
        (
          localization.t("about.privacy.notificationsTitle"),
          localization.t("about.privacy.notificationsBody")
        ),
        (localization.t("about.privacy.sharingTitle"), localization.t("about.privacy.sharingBody")),
        (localization.t("about.privacy.choicesTitle"), localization.t("about.privacy.choicesBody")),
        (localization.t("about.privacy.contactTitle"), localization.t("about.privacy.contactBody")),
      ]
    }
  }
}

struct AboutView_Previews: PreviewProvider {
  static var previews: some View {
    AboutView()
  }
}
