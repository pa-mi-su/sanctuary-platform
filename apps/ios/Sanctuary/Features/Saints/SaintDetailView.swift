import SwiftUI

struct SaintDetailView: View {
    let contentRepository: any ContentRepository
    let saint: Saint
    var displayYear: Int? = nil
    var allowsRelatedNavigation: Bool = true
    var onClose: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var isFavorite = false
    @State private var detailedSaint: Saint?

    init(
        contentRepository: any ContentRepository,
        saint: Saint,
        displayYear: Int? = nil,
        allowsRelatedNavigation: Bool = true,
        onClose: (() -> Void)? = nil
    ) {
        self.contentRepository = contentRepository
        self.saint = saint
        self.displayYear = displayYear
        self.allowsRelatedNavigation = allowsRelatedNavigation
        self.onClose = onClose
    }

    private var locale: ContentLocale { localization.language.contentLocale }
    private var currentSaint: Saint { detailedSaint ?? saint }

    private var displayName: String {
        let baseName = currentSaint.displayName(locale: locale)
        let raw = baseName.replacingOccurrences(of: #",\s*\d{3,4}[–-]\d{2,4}$"#, with: "", options: .regularExpression)
        return raw.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var biography: String {
        currentSaint.biographyByLocale[locale]
            ?? currentSaint.biographyByLocale[.en]
            ?? ""
    }

    private var summary: String {
        currentSaint.summaryByLocale[locale]
            ?? currentSaint.summaryByLocale[.en]
            ?? ""
    }

    private var prayers: [String] {
        currentSaint.prayersByLocale[locale] ?? currentSaint.prayersByLocale[.en] ?? []
    }

    private var feastLabel: String {
        currentSaint.feastLabelByLocale[locale]
            ?? currentSaint.feastLabelByLocale[.en]
            ?? ""
    }

    private var feastDateString: String {
        let year = displayYear ?? Calendar.current.component(.year, from: Date())
        let month = currentSaint.feastMonth
        let day = currentSaint.feastDay
        return localization.formatMonthDay(month: month, day: day, year: year)
    }

    private func handleBack() {
        dismiss()
        onClose?()
    }

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if let imageURL = imageURL {
                        RemoteHeroImage(url: imageURL)
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        Text(displayName)
                            .font(AppTheme.rounded(46, weight: .bold))
                            .minimumScaleFactor(0.6)
                            .foregroundStyle(.white)

                        if progressStore.isAuthenticated {
                            HStack(spacing: 10) {
                                Button {
                                    Task {
                                        await progressStore.toggleFavorite(itemType: .saint, itemID: saint.id)
                                        isFavorite = progressStore.isFavorite(itemType: .saint, itemID: saint.id)
                                    }
                                } label: {
                                    HStack(spacing: 10) {
                                        Image(systemName: isFavorite ? "heart.fill" : "heart")
                                        Text(isFavorite ? localization.t("detail.savedFavorites") : localization.t("detail.addFavorites"))
                                    }
                                    .font(AppTheme.rounded(16, weight: .semibold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 18)
                                    .padding(.vertical, 12)
                                    .background(isFavorite ? AnyShapeStyle(AppTheme.primaryButtonGradient) : AnyShapeStyle(AppTheme.cardBackgroundSoft))
                                    .overlay(
                                        Capsule()
                                            .stroke(Color.white.opacity(0.12), lineWidth: 1)
                                    )
                                    .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                                .animation(.spring(response: 0.32, dampingFraction: 0.82), value: isFavorite)

                                Spacer()
                            }
                        }

                        VStack(alignment: .leading, spacing: 10) {
                            detailMetaChip(icon: "calendar", text: "\(localization.t("detail.feastDate")): \(feastDateString)")
                            if !feastLabel.isEmpty {
                                detailMetaChip(icon: "sparkles", text: feastLabel)
                            }
                        }
                    }
                    .padding(20)
                    .appGlassCard(cornerRadius: 28)

                    if !summary.isEmpty {
                        DetailCard(title: localization.t("detail.summary")) {
                            Text(summary)
                                .font(AppTheme.rounded(18, weight: .medium))
                                .foregroundStyle(AppTheme.cardText.opacity(0.9))
                        }
                    }

                    if !biography.isEmpty {
                        DetailCard(title: localization.t("detail.biography")) {
                            Text(biography)
                                .font(AppTheme.rounded(18, weight: .medium))
                                .foregroundStyle(AppTheme.cardText.opacity(0.9))
                        }
                    }

                            if !currentSaint.patronages.isEmpty {
                        DetailCard(title: localization.t("detail.patronages")) {
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(currentSaint.patronages, id: \.self) { patronage in
                                    Text("• \(patronage)")
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                            }
                        }
                    }

                    if !prayers.isEmpty {
                        DetailCard(title: localization.t("detail.prayers")) {
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(prayers, id: \.self) { prayer in
                                    Text("• \(prayer)")
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                            }
                        }
                    }

                    if !sources.isEmpty {
                        DetailCard(title: localization.t("detail.sources")) {
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(sources, id: \.self) { source in
                                    if let url = URL(string: source), source.lowercased().hasPrefix("http") {
                                        Link(destination: url) {
                                            Text("• \(source)")
                                                .font(AppTheme.rounded(15, weight: .medium))
                                                .underline()
                                                .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                        }
                                    } else {
                                        Text("• \(source)")
                                            .font(AppTheme.rounded(15, weight: .medium))
                                            .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 26)
            }
        }
        .safeAreaInset(edge: .top) {
            StickyBackHeader(title: displayName, action: handleBack)
        }
        .leftEdgeSwipeBack(handleBack)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            async let loadedSaint: Saint? = Task.detached(priority: .userInitiated) {
                try? await contentRepository.fetchSaint(slug: saint.slug, locale: locale)
            }.value
            detailedSaint = await loadedSaint
            isFavorite = progressStore.isFavorite(itemType: .saint, itemID: saint.id)
        }
    }

    private func detailMetaChip(icon: String, text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(AppTheme.glowGold)
            Text(text)
                .font(AppTheme.rounded(15, weight: .medium))
                .foregroundStyle(.white.opacity(0.9))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(AppTheme.cardBackgroundSoft)
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .clipShape(Capsule())
    }

    private var imageURL: URL? {
        currentSaint.imageURL
    }

    private var sources: [String] {
        currentSaint.sources
    }
}

private struct DetailCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.rounded(22, weight: .bold))
                .foregroundStyle(AppTheme.cardText)

            Divider().background(AppTheme.cardText.opacity(0.2))

            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }
}

private struct RemoteHeroImage: View {
    let url: URL

    var body: some View {
        GeometryReader { proxy in
            let containerSize = proxy.size
            let outerShape = RoundedRectangle(cornerRadius: 28, style: .continuous)
            let innerShape = RoundedRectangle(cornerRadius: 18, style: .continuous)

            ZStack {
                outerShape
                    .fill(AppTheme.cardBackground)

                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView().tint(.white)
                    case .success(let image):
                        ZStack {
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: containerSize.width, height: containerSize.height)
                                .clipped()
                                .blur(radius: 26)
                                .saturation(0.7)
                                .opacity(0.78)

                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(
                                    width: max(containerSize.width - 24, 0),
                                    height: max(containerSize.height - 24, 0)
                                )
                                .frame(width: max(containerSize.width - 24, 0), height: max(containerSize.height - 24, 0))
                                .clipped()
                                .clipShape(innerShape)
                                .overlay(
                                    innerShape
                                        .stroke(Color.white.opacity(0.34), lineWidth: 1)
                                )
                        }
                        .frame(width: containerSize.width, height: containerSize.height)
                        .clipped()
                    case .failure:
                        Color.gray.opacity(0.25)
                    @unknown default:
                        Color.gray.opacity(0.25)
                    }
                }
                .frame(width: containerSize.width, height: containerSize.height)
            }
            .frame(width: containerSize.width, height: containerSize.height)
            .clipShape(outerShape)
            .overlay(
                outerShape
                    .stroke(Color.white.opacity(0.16), lineWidth: 1.5)
            )
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .clipped()
        .allowsHitTesting(false)
        .shadow(color: Color.black.opacity(0.22), radius: 18, x: 0, y: 10)
    }
}

struct SaintDetailView_Previews: PreviewProvider {
    private static let previewSaint = Saint(
        id: "saint-joseph",
        slug: "saint-joseph",
        name: "Saint Joseph",
        nameByLocale: [.en: "Saint Joseph"],
        feastMonth: 3,
        feastDay: 19,
        imageURL: nil,
        tags: ["guardian", "family"],
        patronages: ["Fathers", "Workers"],
        feastLabelByLocale: [.en: "March 19"],
        summaryByLocale: [.en: "Guardian of the Holy Family and a steady patron for daily life."],
        biographyByLocale: [.en: "Saint Joseph is honored as the faithful guardian of Jesus and Mary, a model of quiet obedience and trust."],
        prayersByLocale: [.en: ["Saint Joseph, pray for us."]],
        sources: []
    )

    static var previews: some View {
        SaintDetailView(
            contentRepository: PreviewContentRepository(saints: [previewSaint]),
            saint: previewSaint
        )
            .environmentObject(LocalizationManager())
            .environmentObject(
                UserProgressStore(userProgressRepository: LocalUserProgressRepository())
            )
    }
}
