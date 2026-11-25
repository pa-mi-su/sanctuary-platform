import SwiftUI

enum AppTheme {
    static let cardBackgroundElevated = Color(hex: "#D3E1D4")
    static let heroBase = Color(hex: "#073C3B")
    static let errorSurface = Color(hex: "#F0DDD7")
    static let warningText = Color(hex: "#755019")
    static let warningSurface = Color(hex: "#EADFC5")
    static let successText = Color(hex: "#24634F")
    static let qrCanvas = Color(hex: "#FFFFFF")
    static let homeSurface = Color(hex: "#D8E3D9")
    static let homeSurfaceRaised = Color(hex: "#C4D5C8")
    static let homeInk = Color(hex: "#173D36")
    static let errorText = Color(hex: "#9C2630")
    static let onAccent = Color(hex: "#F4F2E7")
    static let border = Color(hex: "#A9C2B2")
    static let homeMuted = Color(hex: "#465B4D")

    static let gradientTop = homeSurface
    static let gradientMid = Color(hex: "#D2E0D4")
    static let gradientBottom = homeSurface
    static let glowBlue = Color(hex: "#24634F")
    static let glowGold = Color(hex: "#75521D")
    static let glowRose = Color(hex: "#88596E")

    static let primaryButtonTop = glowBlue
    static let primaryButtonBottom = Color(hex: "#174B3D")
    static let primaryButtonText = onAccent
    static let purpleButton = glowBlue
    static let purpleOutline = Color(hex: "#6F9380")
    static let cardBackground = Color(hex: "#E3EBE2")
    static let cardBackgroundSoft = homeSurfaceRaised
    static let cardText = homeInk
    static let subtitleText = homeMuted
    static let heroEyebrow = glowBlue

    static let tabBackground = homeSurface
    static let tabBorder = border
    static let tabInactive = homeMuted
    static let tabActive = glowBlue
    static let advent = Color(hex: "#7858B9")
    static let christmas = Color(hex: "#DCB969")
    static let lent = Color(hex: "#9B5087")
    static let easter = Color(hex: "#F5F5FA")
    static let ordinary = Color(hex: "#3C9B5F")
    static let todayHighlight = Color(hex: "#F2CF63")

    static var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [gradientTop, gradientMid, gradientBottom],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static var primaryButtonGradient: LinearGradient {
        LinearGradient(
            colors: [primaryButtonTop, primaryButtonBottom],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static func rounded(_ size: CGFloat, weight: Font.Weight) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }

    // Calendar markers and legend share the color contract season roles.
    static func liturgicalBorderColor(for season: LiturgicalSeason) -> Color {
        switch season {
        case .advent:
            return advent
        case .christmas:
            return christmas
        case .lent:
            return lent
        case .easter:
            return easter
        case .ordinary:
            return ordinary
        }
    }
}

/// A single quiet sage surface is shared by every non-editorial screen.
struct AppBackdrop: View {
    var body: some View {
        AppTheme.homeSurface
            .ignoresSafeArea()
            .preferredColorScheme(.light)
    }
}

struct AppGlassCardStyle: ViewModifier {
    let cornerRadius: CGFloat

    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(AppTheme.cardBackground)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .fill(.white.opacity(0.02))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .stroke(AppTheme.border, lineWidth: 1)
                    )
                    .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
            )
    }
}

extension View {
    func appGlassCard(cornerRadius: CGFloat = 24) -> some View {
        modifier(AppGlassCardStyle(cornerRadius: cornerRadius))
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

struct PrimaryPillButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(AppTheme.rounded(16, weight: .semibold))
            .foregroundStyle(isEnabled ? AppTheme.primaryButtonText : AppTheme.subtitleText)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background {
                Rectangle().fill(isEnabled ? AnyShapeStyle(AppTheme.primaryButtonGradient) : AnyShapeStyle(AppTheme.cardBackgroundSoft))
                    .opacity(configuration.isPressed ? 0.82 : 1.0)
            }
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color.white.opacity(0.16), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.985 : 1.0)
            .shadow(color: AppTheme.glowBlue.opacity(configuration.isPressed ? 0.12 : 0.28), radius: 14, x: 0, y: 10)
    }
}

struct SecondaryPillButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(AppTheme.rounded(16, weight: .semibold))
            .foregroundStyle(AppTheme.cardText)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(AppTheme.cardBackgroundSoft.opacity(configuration.isPressed ? 0.9 : 1.0))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(AppTheme.purpleOutline.opacity(0.6), lineWidth: 1)
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.985 : 1.0)
    }
}

struct TopActionButton: View {
    let title: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(AppTheme.rounded(13, weight: .semibold))
                    .frame(width: 26, height: 26)
                    .background(AppTheme.homeSurfaceRaised)
                    .clipShape(Circle())
                Text(title)
                    .font(AppTheme.rounded(12, weight: .semibold))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .foregroundStyle(AppTheme.cardText)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(AppTheme.cardBackgroundSoft)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(AppTheme.border, lineWidth: 1)
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

/// Shared, clearly bounded navigation action used by Menu and Me.
struct NavigationCardButton: View {
    let title: String
    let icon: String
    var subtitle: String? = nil
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon).font(.system(size: 25)).foregroundStyle(AppTheme.tabActive)
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(AppTheme.rounded(18, weight: .semibold)).foregroundStyle(AppTheme.cardText)
                    if let subtitle { Text(subtitle).font(AppTheme.rounded(15, weight: .medium)).foregroundStyle(AppTheme.subtitleText) }
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right").foregroundStyle(AppTheme.tabActive)
            }
            .padding(18).frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
            .background(AppTheme.cardBackground).clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppTheme.purpleOutline.opacity(0.6), lineWidth: 1))
        }.buttonStyle(.plain)
    }
}
