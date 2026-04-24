import SwiftUI

enum NovenaSearchMode {
    case standard
    case intentions
}

struct SaintsSearchView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @StateObject private var viewModel: SaintsListViewModel

    init(environment: AppEnvironment) {
        _viewModel = StateObject(
            wrappedValue: SaintsListViewModel(
                contentRepository: environment.contentRepository
            )
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                VStack(spacing: 14) {
                    SearchHeader(
                        title: localization.t("search.saintsTitle"),
                        dismiss: dismiss.callAsFunction
                    )

                    SearchField(
                        prompt: localization.t("search.saintsPrompt"),
                        text: $viewModel.query
                    ) {
                        Task { await viewModel.search() }
                    }

                    SearchResultsCount(count: viewModel.saints.count)

                    ScrollView(showsIndicators: false) {
                        LazyVStack(spacing: 10) {
                            ForEach(viewModel.saints) { saint in
                                NavigationLink {
                                    SaintDetailView(contentRepository: viewModel.contentRepository, saint: saint)
                                } label: {
                                    SearchResultCard(
                                        title: viewModel.displayName(for: saint),
                                        subtitle: viewModel.summary(for: saint),
                                        meta: feastLabel(for: saint),
                                        accent: AppTheme.glowGold,
                                        icon: "person.fill",
                                        imageURL: saint.imageURL
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.bottom, 24)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
            }
            .toolbar(.hidden, for: .navigationBar)
            .task {
                viewModel.setLocale(localization.language.contentLocale)
                await viewModel.load()
            }
            .onChange(of: localization.language) { newValue in
                Task {
                    viewModel.setLocale(newValue.contentLocale)
                    await viewModel.load()
                }
            }
            .onChange(of: viewModel.query) { _ in
                Task { await viewModel.search() }
            }
        }
    }

    private func feastLabel(for saint: Saint) -> String {
        saint.feastLabelByLocale[localization.language.contentLocale]
            ?? saint.feastLabelByLocale[.en]
            ?? localization.formatMonthDay(month: saint.feastMonth, day: saint.feastDay)
    }
}

struct NovenasSearchView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @StateObject private var viewModel: NovenasListViewModel
    let environment: AppEnvironment
    let mode: NovenaSearchMode
    @State private var intentionsQuery = ""
    @State private var intentionItems: [IntentionSearchItem] = []

    init(environment: AppEnvironment, mode: NovenaSearchMode = .standard) {
        self.environment = environment
        self.mode = mode
        _viewModel = StateObject(
            wrappedValue: NovenasListViewModel(
                useCase: ListNovenasUseCase(contentRepository: environment.contentRepository)
            )
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                VStack(spacing: 14) {
                    SearchHeader(
                        title: mode == .intentions ? localization.t("calendar.searchIntentions") : localization.t("search.novenasTitle"),
                        dismiss: dismiss.callAsFunction
                    )

                    if mode == .intentions {
                        SearchField(
                            prompt: localization.t("search.intentionsPrompt"),
                            text: $intentionsQuery
                        )

                        SearchResultsCount(count: filteredIntentionItems.count)

                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                ForEach(filteredIntentionItems) { item in
                                    NavigationLink {
                                        NovenaDetailView(contentRepository: environment.contentRepository, novena: item.novena)
                                    } label: {
                                        SearchResultCard(
                                            title: item.title,
                                            subtitle: item.subtitle,
                                            meta: item.meta,
                                            accent: AppTheme.glowRose,
                                            icon: "heart.text.square.fill",
                                            imageURL: item.novena.imageURL
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.bottom, 24)
                        }
                    } else {
                        SearchField(
                            prompt: localization.t("search.novenasPrompt"),
                            text: $viewModel.query
                        ) {
                            Task { await viewModel.search() }
                        }

                        SearchResultsCount(count: viewModel.novenas.count)

                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                ForEach(viewModel.novenas) { novena in
                                    NavigationLink {
                                        NovenaDetailView(contentRepository: environment.contentRepository, novena: novena)
                                    } label: {
                                        SearchResultCard(
                                            title: viewModel.title(for: novena),
                                            subtitle: viewModel.summary(for: novena),
                                            meta: viewModel.dayText(for: novena),
                                            accent: AppTheme.glowBlue,
                                            icon: "book.closed.fill",
                                            imageURL: novena.imageURL
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
            .task {
                viewModel.setLocale(localization.language.contentLocale)
                await viewModel.load()
                if mode == .intentions {
                    await rebuildIntentionItems()
                }
            }
            .onChange(of: localization.language) { newValue in
                Task {
                    viewModel.setLocale(newValue.contentLocale)
                    await viewModel.load()
                    if mode == .intentions {
                        await rebuildIntentionItems()
                    }
                }
            }
            .onChange(of: viewModel.query) { _ in
                guard mode == .standard else { return }
                Task { await viewModel.search() }
            }
            .onChange(of: intentionsQuery) { _ in
                guard mode == .intentions else { return }
                Task { await rebuildIntentionItems() }
            }
        }
    }

    private var filteredIntentionItems: [IntentionSearchItem] {
        let q = normalized(intentionsQuery)
        guard !q.isEmpty else { return intentionItems }
        let rankedIDs = SearchMatcher.rankedIDs(for: q, in: intentionItems) { $0.document }
        let itemByID = Dictionary(uniqueKeysWithValues: intentionItems.map { ($0.id, $0) })
        return rankedIDs.compactMap { itemByID[$0] }
    }

    private func rebuildIntentionItems() async {
        let locale = localization.language.contentLocale
        let trimmedQuery = intentionsQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let results: [Novena]

        if trimmedQuery.isEmpty, !viewModel.novenas.isEmpty {
            results = viewModel.novenas
        } else {
            results = (try? await environment.contentRepository.searchNovenasByIntentions(locale: locale, query: intentionsQuery)) ?? []
        }

        intentionItems = results.map { novena in
            let title = viewModel.title(for: novena)
            let summary = viewModel.summary(for: novena)
            let intentionsSummary = formattedIntentions(for: novena)
            let document = SearchMatcher.Document(
                itemID: novena.id,
                primaryText: title,
                secondaryText: "\(novena.slug) \((novena.tags).joined(separator: " ")) \((novena.intentions).joined(separator: " "))",
                auxiliaryText: "\(summary) \(intentionsSummary)"
            )
            return IntentionSearchItem(
                id: novena.id,
                novena: novena,
                title: title,
                subtitle: intentionsSummary.isEmpty ? summary : intentionsSummary,
                meta: viewModel.dayText(for: novena),
                document: document
            )
        }
    }

    private func formattedIntentions(for novena: Novena) -> String {
        let cleaned = novena.intentions
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        guard !cleaned.isEmpty else { return "" }
        return Array(cleaned.prefix(3)).joined(separator: " • ")
    }

    private func normalized(_ value: String) -> String {
        SearchMatcher.normalize(value)
    }
}

private struct IntentionSearchItem: Identifiable {
    let id: String
    let novena: Novena
    let title: String
    let subtitle: String
    let meta: String
    let document: SearchMatcher.Document
}

struct GlobalSearchView: View {
    @EnvironmentObject private var localization: LocalizationManager
    let environment: AppEnvironment

    var body: some View {
        TabView {
            SaintsSearchView(environment: environment)
                .tabItem { Label(localization.t("tab.saints"), systemImage: "person.2.fill") }
            NovenasSearchView(environment: environment)
                .tabItem { Label(localization.t("tab.novenas"), systemImage: "book.closed.fill") }
        }
        .tint(AppTheme.tabActive)
    }
}

private struct SearchHeader: View {
    let title: String
    let dismiss: () -> Void

    var body: some View {
        HStack {
            Button(action: dismiss) {
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

            Spacer()

            Text(title)
                .font(.system(size: 24, weight: .heavy))
                .foregroundStyle(.white)

            Spacer()

            Color.clear.frame(width: 52, height: 52)
        }
        .padding(.top, 8)
    }
}

private struct SearchField: View {
    let prompt: String
    @Binding var text: String
    var onSubmit: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(AppTheme.cardText.opacity(0.75))
            TextField(
                "",
                text: $text,
                prompt: Text(prompt)
                    .foregroundColor(AppTheme.cardText.opacity(0.58))
            )
                .foregroundColor(AppTheme.cardText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .onSubmit { onSubmit?() }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .appGlassCard(cornerRadius: 28)
    }
}

private struct SearchResultsCount: View {
    @EnvironmentObject private var localization: LocalizationManager
    let count: Int

    var body: some View {
        HStack {
            Text("\(count) \(localization.t("search.results"))")
                .font(AppTheme.rounded(17, weight: .medium))
                .foregroundStyle(.white.opacity(0.92))
            Spacer()
        }
    }
}

struct SearchResultCard: View {
    let title: String
    let subtitle: String
    let meta: String?
    let accent: Color
    let icon: String
    var imageURL: URL? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            if let imageURL {
                SearchResultThumbnail(imageURL: imageURL, accent: accent, icon: icon)
            } else {
                ZStack {
                    Circle()
                        .fill(accent.opacity(0.18))
                        .frame(width: 42, height: 42)
                    Image(systemName: icon)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(accent)
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(AppTheme.rounded(20, weight: .bold))
                    .foregroundStyle(AppTheme.cardText)
                    .lineLimit(2)
                Text(subtitle)
                    .font(AppTheme.rounded(15, weight: .medium))
                    .foregroundStyle(AppTheme.cardText.opacity(0.78))
                    .lineLimit(3)
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .font(AppTheme.rounded(12, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.68))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(AppTheme.cardBackgroundSoft)
                        .clipShape(Capsule())
                }
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white.opacity(0.52))
                .padding(.top, 3)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 15)
        .appGlassCard(cornerRadius: 24)
        .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct SearchResultThumbnail: View {
    let imageURL: URL
    let accent: Color
    let icon: String

    var body: some View {
        AsyncImage(url: imageURL) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            default:
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(accent.opacity(0.16))
                    Image(systemName: icon)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(accent)
                }
            }
        }
        .frame(width: 64, height: 88)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }
}
