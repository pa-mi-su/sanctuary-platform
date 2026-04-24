import SwiftUI
import Combine

@MainActor
final class PrayersSearchViewModel: ObservableObject {
    private struct IndexedPrayer: Sendable {
        let prayer: Prayer
        let document: SearchMatcher.Document
    }

    @Published var query: String = ""
    @Published private(set) var prayers: [Prayer] = []
    @Published private(set) var isLoading = false

    private let environment: AppEnvironment
    private var locale: ContentLocale = .en
    private var allPrayers: [Prayer] = []
    private var indexedPrayers: [IndexedPrayer] = []
    private var filterTask: Task<Void, Never>?

    init(environment: AppEnvironment) {
        self.environment = environment
    }

    func setLocale(_ locale: ContentLocale) {
        self.locale = locale
        rebuildIndex()
        scheduleFilter(immediate: true)
    }

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            allPrayers = try await environment.contentRepository.listPrayers(locale: locale, category: nil, query: nil)
            rebuildIndex()
            scheduleFilter(immediate: true)
        } catch {
            allPrayers = []
            indexedPrayers = []
            prayers = []
        }
    }

    func search() async {
        scheduleFilter()
    }

    func title(for prayer: Prayer, locale: ContentLocale) -> String {
        prayer.titleByLocale[locale] ?? prayer.titleByLocale[.en] ?? prayer.slug
    }

    func subtitle(for prayer: Prayer, locale: ContentLocale) -> String {
        let body = prayer.bodyByLocale[locale] ?? prayer.bodyByLocale[.en] ?? ""
        let firstLine = body.split(separator: "\n").first.map(String.init) ?? ""
        return firstLine.isEmpty ? "..." : firstLine
    }

    private func scheduleFilter(immediate: Bool = false) {
        filterTask?.cancel()
        let q = normalized(query)

        guard !q.isEmpty else {
            prayers = allPrayers
            return
        }

        let snapshot = indexedPrayers
        filterTask = Task {
            if !immediate {
                try? await Task.sleep(nanoseconds: 70_000_000)
            }
            guard !Task.isCancelled else { return }

            let filtered = await Task.detached(priority: .userInitiated) {
                let rankedIDs = SearchMatcher.rankedIDs(for: q, in: snapshot) { $0.document }
                let prayerByID = Dictionary(uniqueKeysWithValues: snapshot.map { ($0.prayer.id, $0.prayer) })
                return rankedIDs.compactMap { prayerByID[$0] }
            }.value

            guard !Task.isCancelled else { return }
            guard normalized(self.query) == q else { return }
            self.prayers = filtered
        }
    }

    private func rebuildIndex() {
        indexedPrayers = allPrayers.map { prayer in
            let title = prayer.titleByLocale[locale] ?? prayer.titleByLocale[.en] ?? prayer.slug
            let body = prayer.bodyByLocale[locale] ?? prayer.bodyByLocale[.en] ?? ""
            let document = SearchMatcher.Document(
                itemID: prayer.id,
                primaryText: title,
                secondaryText: "\(prayer.category) \(prayer.slug) \(prayer.tags.joined(separator: " "))",
                auxiliaryText: body
            )
            return IndexedPrayer(prayer: prayer, document: document)
        }
    }

    private func normalized(_ value: String) -> String {
        SearchMatcher.normalize(value)
    }
}

struct PrayersSearchView: View {
    let environment: AppEnvironment
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @StateObject private var viewModel: PrayersSearchViewModel

    init(environment: AppEnvironment) {
        self.environment = environment
        _viewModel = StateObject(wrappedValue: PrayersSearchViewModel(environment: environment))
    }

    private var locale: ContentLocale { localization.language.contentLocale }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                VStack(spacing: 14) {
                    HStack {
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(width: 52, height: 52)
                                .background(AppTheme.cardBackgroundSoft)
                                .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .contentShape(Circle())
                        .highPriorityGesture(TapGesture().onEnded { dismiss() })
                        Spacer()
                        Text(localization.t("search.prayersTitle"))
                            .font(.system(size: 24, weight: .heavy))
                            .foregroundStyle(.white)
                        Spacer()
                        Color.clear.frame(width: 52, height: 52)
                    }
                    .padding(.top, 8)

                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(AppTheme.cardText.opacity(0.75))
                        TextField(
                            "",
                            text: $viewModel.query,
                            prompt: Text(localization.t("search.prayersPrompt"))
                                .foregroundColor(AppTheme.cardText.opacity(0.58))
                        )
                            .foregroundColor(AppTheme.cardText)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .submitLabel(.search)
                            .onSubmit { Task { await viewModel.search() } }
                    }
                    .padding(.horizontal, 18)
                    .padding(.vertical, 14)
                    .appGlassCard(cornerRadius: 28)

                    if viewModel.isLoading {
                        SanctuaryLoadingCard(
                            title: localization.t("common.loading"),
                            detail: localization.t("common.loadingDetail")
                        )
                    } else {
                        HStack {
                            Text("\(viewModel.prayers.count) \(localization.t("search.results"))")
                                .font(AppTheme.rounded(17, weight: .medium))
                                .foregroundStyle(.white.opacity(0.92))
                            Spacer()
                        }

                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                ForEach(viewModel.prayers) { prayer in
                                    NavigationLink {
                                        PrayerDetailView(contentRepository: environment.contentRepository, prayer: prayer)
                                    } label: {
                                        SearchResultCard(
                                            title: viewModel.title(for: prayer, locale: locale),
                                            subtitle: viewModel.subtitle(for: prayer, locale: locale),
                                            meta: nil,
                                            accent: AppTheme.glowRose,
                                            icon: "hands.sparkles.fill",
                                            imageURL: prayer.imageURL
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.bottom, 24)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .task {
            viewModel.setLocale(locale)
            await viewModel.load()
        }
        .onChange(of: localization.language) { newValue in
            Task {
                viewModel.setLocale(newValue.contentLocale)
                await viewModel.search()
            }
        }
        .onChange(of: viewModel.query) { _ in
            Task { await viewModel.search() }
        }
    }
}

struct PrayerDetailView: View {
    let contentRepository: any ContentRepository
    let prayer: Prayer
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @State private var currentPrayer: Prayer

    init(contentRepository: any ContentRepository, prayer: Prayer) {
        self.contentRepository = contentRepository
        self.prayer = prayer
        _currentPrayer = State(initialValue: prayer)
    }

    private var locale: ContentLocale { localization.language.contentLocale }
    private var title: String {
        currentPrayer.titleByLocale[locale]
            ?? currentPrayer.titleByLocale[.en]
            ?? currentPrayer.slug
    }

    private var alternateTitle: String {
        currentPrayer.alternateTitleByLocale[locale]
            ?? currentPrayer.alternateTitleByLocale[.en]
            ?? ""
    }

    private var prayerText: String {
        currentPrayer.bodyByLocale[locale]
            ?? currentPrayer.bodyByLocale[.en]
            ?? ""
    }

    private var noteText: String {
        currentPrayer.noteByLocale[locale]
            ?? currentPrayer.noteByLocale[.en]
            ?? ""
    }

    private var sourceTitle: String {
        currentPrayer.sourceTitle ?? ""
    }

    private var imageURL: URL? {
        currentPrayer.imageURL
    }

    private func handleBack() {
        dismiss()
    }

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 16) {
                        Button { handleBack() } label: {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(width: 52, height: 52)
                                .background(AppTheme.cardBackgroundSoft)
                                .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .contentShape(Circle())
                        .highPriorityGesture(TapGesture().onEnded { handleBack() })

                        Text(title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                    }
                    .padding(.top, 8)
                    .zIndex(10)

                    if let imageURL {
                        PrayerHeroImage(url: imageURL)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text(title)
                            .font(AppTheme.rounded(52, weight: .bold))
                            .minimumScaleFactor(0.62)
                            .foregroundStyle(.white)

                        if !alternateTitle.isEmpty {
                            detailMetaChip(icon: "quote.bubble", text: alternateTitle)
                        }
                    }
                    .padding(20)
                    .appGlassCard(cornerRadius: 28)

                    PrayerSectionCard(title: localization.t("novena.prayer"), bodyText: prayerText)

                    if !noteText.isEmpty {
                        PrayerSectionCard(title: localization.t("detail.note"), bodyText: noteText)
                    }

                    if !sourceTitle.isEmpty {
                        detailMetaChip(icon: "book.closed", text: sourceTitle)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .task(id: "\(prayer.slug)-\(locale.rawValue)") {
            do {
                if let loaded = try await contentRepository.fetchPrayer(slug: prayer.slug, locale: locale) {
                    currentPrayer = loaded
                }
            } catch {
                currentPrayer = prayer
            }
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
}

private struct PrayerSectionCard: View {
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.rounded(22, weight: .bold))
                .foregroundStyle(AppTheme.cardText)
            Divider().background(AppTheme.cardText.opacity(0.2))
            Text(bodyText)
                .font(AppTheme.rounded(18, weight: .medium))
                .foregroundStyle(AppTheme.cardText.opacity(0.92))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appGlassCard(cornerRadius: 24)
    }
}

private struct PrayerHeroImage: View {
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
                        Image(systemName: "photo")
                            .font(.system(size: 46))
                            .foregroundStyle(.white.opacity(0.7))
                    @unknown default:
                        EmptyView()
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
