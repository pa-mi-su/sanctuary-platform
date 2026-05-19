import SwiftUI
import Combine

enum PrayerSearchMode {
    case prayers
    case rosary

    var category: String? {
        switch self {
        case .prayers:
            return nil
        case .rosary:
            return "rosary"
        }
    }

    var titleKey: String {
        switch self {
        case .prayers:
            return "search.prayersTitle"
        case .rosary:
            return "search.rosaryTitle"
        }
    }

    var promptKey: String {
        switch self {
        case .prayers:
            return "search.prayersPrompt"
        case .rosary:
            return "search.rosaryPrompt"
        }
    }

    var accent: Color {
        switch self {
        case .prayers:
            return AppTheme.glowRose
        case .rosary:
            return AppTheme.glowGold
        }
    }

    var icon: String {
        switch self {
        case .prayers:
            return "hands.sparkles.fill"
        case .rosary:
            return "circle"
        }
    }
}

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
    private let mode: PrayerSearchMode
    private var locale: ContentLocale = .en
    private var allPrayers: [Prayer] = []
    private var indexedPrayers: [IndexedPrayer] = []
    private var filterTask: Task<Void, Never>?

    init(environment: AppEnvironment, mode: PrayerSearchMode) {
        self.environment = environment
        self.mode = mode
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
            allPrayers = try await environment.contentRepository.listPrayers(locale: locale, category: mode.category, query: nil)
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
        guard mode != .rosary else {
            return ""
        }

        let body = prayer.bodyByLocale[locale] ?? prayer.bodyByLocale[.en] ?? ""
        let firstLine = body.split(separator: "\n").first.map(String.init) ?? ""
        return firstLine.isEmpty ? "..." : firstLine
    }

    func meta(for prayer: Prayer, locale: ContentLocale) -> String? {
        guard mode == .rosary else {
            return nil
        }

        let note = prayer.noteByLocale[locale] ?? prayer.noteByLocale[.en] ?? ""
        let preview = prayer.bodyByLocale[locale] ?? prayer.bodyByLocale[.en] ?? ""
        return note.isEmpty ? preview : note
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
    let mode: PrayerSearchMode
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @StateObject private var viewModel: PrayersSearchViewModel

    init(environment: AppEnvironment, mode: PrayerSearchMode = .prayers) {
        self.environment = environment
        self.mode = mode
        _viewModel = StateObject(wrappedValue: PrayersSearchViewModel(environment: environment, mode: mode))
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
                        Text(localization.t(mode.titleKey))
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
                            prompt: Text(localization.t(mode.promptKey))
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
                                            meta: viewModel.meta(for: prayer, locale: locale),
                                            accent: mode.accent,
                                            icon: mode.icon,
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
    @EnvironmentObject private var progressStore: UserProgressStore
    @State private var currentPrayer: Prayer
    @State private var isFavorite = false
    @State private var isShowingExpandedHeroImage = false

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
        let body = currentPrayer.bodyByLocale[locale]
            ?? currentPrayer.bodyByLocale[.en]
            ?? ""
        return displayPrayerBody(body)
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

    private var isRosary: Bool {
        currentPrayer.category.caseInsensitiveCompare("rosary") == .orderedSame
    }

    private var canExpandHeroImage: Bool {
        currentPrayer.slug == "how_to_pray_the_rosary"
    }

    private func displayPrayerBody(_ body: String) -> String {
        guard isRosary, !alternateTitle.isEmpty else {
            return body
        }

        let normalized = body.replacingOccurrences(of: "\r\n", with: "\n")
        let lines = normalized.split(separator: "\n", omittingEmptySubsequences: false)
        guard lines.first?.trimmingCharacters(in: .whitespacesAndNewlines) == alternateTitle else {
            return body
        }

        return lines.dropFirst().joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func handleBack() {
        dismiss()
    }

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    if let imageURL {
                        PrayerHeroImage(
                            url: imageURL,
                            isExpandable: canExpandHeroImage,
                            onTap: { isShowingExpandedHeroImage = true }
                        )
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        Text(title)
                            .font(AppTheme.rounded(48, weight: .bold))
                            .minimumScaleFactor(0.58)
                            .foregroundStyle(.white)

                        if progressStore.isAuthenticated {
                            HStack(spacing: 10) {
                                Button {
                                    Task {
                                        await progressStore.toggleFavorite(itemType: .prayer, itemID: currentPrayer.id)
                                        isFavorite = progressStore.isFavorite(itemType: .prayer, itemID: currentPrayer.id)
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
                            if !noteText.isEmpty {
                                Text(noteText)
                                    .font(AppTheme.rounded(18, weight: .medium))
                                    .foregroundStyle(.white.opacity(0.95))
                                    .padding(.top, 2)
                            }
                        }
                    }
                    .padding(20)
                    .appGlassCard(cornerRadius: 28)

                    PrayerSectionCard(title: localization.t("novena.prayer"), bodyText: prayerText)

                    if !sourceTitle.isEmpty {
                        detailMetaChip(icon: "book.closed", text: sourceTitle)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .overlay(alignment: .topLeading) {
            FloatingBackButton(action: handleBack)
                .padding(.leading, 16)
                .padding(.top, 8)
        }
        .leftEdgeSwipeBack(handleBack)
        .toolbar(.hidden, for: .navigationBar)
        .fullScreenCover(isPresented: $isShowingExpandedHeroImage) {
            if let imageURL {
                ExpandedPrayerImageView(url: imageURL) {
                    isShowingExpandedHeroImage = false
                }
            }
        }
        .task(id: "\(prayer.slug)-\(locale.rawValue)") {
            do {
                if let loaded = try await contentRepository.fetchPrayer(slug: prayer.slug, locale: locale) {
                    currentPrayer = loaded
                    isFavorite = progressStore.isFavorite(itemType: .prayer, itemID: loaded.id)
                }
            } catch {
                currentPrayer = prayer
                isFavorite = progressStore.isFavorite(itemType: .prayer, itemID: prayer.id)
            }
        }
        .onAppear {
            isFavorite = progressStore.isFavorite(itemType: .prayer, itemID: currentPrayer.id)
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
    var isExpandable = false
    var onTap: () -> Void = {}

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
            .overlay(alignment: .topTrailing) {
                if isExpandable {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                        .background(AppTheme.cardBackgroundSoft)
                        .clipShape(Circle())
                        .padding(14)
                }
            }
            .contentShape(outerShape)
            .onTapGesture {
                if isExpandable {
                    onTap()
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .clipped()
        .accessibilityAddTraits(isExpandable ? .isButton : [])
        .shadow(color: Color.black.opacity(0.22), radius: 18, x: 0, y: 10)
    }
}

private struct ExpandedPrayerImageView: View {
    let url: URL
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.opacity(0.96)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    ProgressView().tint(.white)
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .padding(18)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                case .failure:
                    Image(systemName: "photo")
                        .font(.system(size: 54))
                        .foregroundStyle(.white.opacity(0.72))
                @unknown default:
                    EmptyView()
                }
            }
            .ignoresSafeArea()

            Button(action: onDismiss) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(Color.white.opacity(0.16))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .padding(.top, 18)
            .padding(.leading, 18)
        }
        .leftEdgeSwipeBack(onDismiss)
    }
}
