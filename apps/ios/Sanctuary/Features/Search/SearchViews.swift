import SwiftUI

enum TermSearchMode {
    case intentions
    case patronage
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

                    if viewModel.isLoading {
                        SanctuaryLoadingCard(
                            title: localization.t("common.loading"),
                            detail: localization.t("common.loadingDetail")
                        )
                    } else {
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
                        .scrollDismissesKeyboard(.interactively)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
            }
            .toolbar(.hidden, for: .navigationBar)
            .leftEdgeSwipeBack(dismiss.callAsFunction)
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

    init(environment: AppEnvironment) {
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
                        title: localization.t("search.novenasTitle"),
                        dismiss: dismiss.callAsFunction
                    )

                    SearchField(
                        prompt: localization.t("search.novenasPrompt"),
                        text: $viewModel.query
                    ) {
                        Task { await viewModel.search() }
                    }

                    if viewModel.isLoading {
                        SanctuaryLoadingCard(
                            title: localization.t("common.loading"),
                            detail: localization.t("common.loadingDetail")
                        )
                    } else {
                        SearchResultsCount(count: viewModel.novenas.count)

                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                ForEach(viewModel.novenas) { novena in
                                    NavigationLink {
                                        NovenaDetailView(contentRepository: viewModel.contentRepository, novena: novena)
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
                        .scrollDismissesKeyboard(.interactively)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .toolbar(.hidden, for: .navigationBar)
            .leftEdgeSwipeBack(dismiss.callAsFunction)
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

}

struct TermSearchView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    let environment: AppEnvironment
    let mode: TermSearchMode
    @State private var query = ""
    @State private var terms: [SearchTerm] = []
    @State private var selectedTerm: SearchTerm?
    @State private var novenas: [Novena] = []
    @State private var saints: [Saint] = []
    @State private var selectedNovena: Novena?
    @State private var selectedSaint: Saint?
    @State private var openSelectedNovena = false
    @State private var openSelectedSaint = false
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackdrop()

                VStack(spacing: 14) {
                    SearchHeader(title: title, dismiss: dismiss.callAsFunction)

                    SearchField(prompt: prompt, text: $query) {
                        Task { await resetAndLoadTerms() }
                    }

                    if let selectedTerm {
                        selectedTermHeader(selectedTerm)
                    }

                    if isLoading {
                        SanctuaryLoadingCard(
                            title: localization.t("common.loading"),
                            detail: localization.t("common.loadingDetail")
                        )
                    } else if let errorMessage {
                        Text(errorMessage)
                            .font(AppTheme.rounded(16, weight: .semibold))
                            .foregroundStyle(AppTheme.glowRose)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(18)
                            .appGlassCard(cornerRadius: 22)
                    } else {
                        SearchResultsCount(count: selectedTerm == nil ? terms.count : resultCount)

                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                if selectedTerm == nil {
                                    ForEach(terms) { term in
                                        Button {
                                            Task { await select(term) }
                                        } label: {
                                            SearchResultCard(
                                                title: term.label,
                                                subtitle: resultLabel(for: term),
                                                meta: nil,
                                                accent: accent,
                                                icon: mode == .intentions ? "heart.text.square.fill" : "person.crop.circle.badge.checkmark",
                                                imageURLs: term.imageURLs
                                            )
                                        }
                                        .buttonStyle(.plain)
                                    }
                                } else if mode == .intentions {
                                    ForEach(novenas) { novena in
                                        NavigationLink {
                                            NovenaDetailView(contentRepository: environment.contentRepository, novena: novena)
                                        } label: {
                                            SearchResultCard(
                                                title: novena.titleByLocale[localization.language.contentLocale] ?? novena.titleByLocale[.en] ?? novena.slug,
                                                subtitle: novena.descriptionByLocale[localization.language.contentLocale] ?? novena.descriptionByLocale[.en] ?? "",
                                                meta: "\(novena.durationDays) days",
                                                accent: accent,
                                                icon: "book.closed.fill",
                                                imageURL: novena.imageURL
                                            )
                                        }
                                        .buttonStyle(.plain)
                                    }
                                } else {
                                    ForEach(saints) { saint in
                                        NavigationLink {
                                            SaintDetailView(contentRepository: environment.contentRepository, saint: saint)
                                        } label: {
                                            SearchResultCard(
                                                title: saint.displayName(locale: localization.language.contentLocale),
                                                subtitle: saint.summaryByLocale[localization.language.contentLocale] ?? saint.summaryByLocale[.en] ?? "",
                                                meta: saint.patronages.prefix(3).joined(separator: " • "),
                                                accent: accent,
                                                icon: "person.fill",
                                                imageURL: saint.imageURL
                                            )
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                            }
                            .padding(.bottom, 24)
                        }
                        .scrollDismissesKeyboard(.interactively)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 10)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .toolbar(.hidden, for: .navigationBar)
            .leftEdgeSwipeBack(dismiss.callAsFunction)
            .background(hiddenDirectNavigation)
            .task { await loadTerms() }
            .onChange(of: localization.language) { _ in
                Task { await resetAndLoadTerms() }
            }
            .onChange(of: query) { _ in
                Task { await resetAndLoadTerms() }
            }
        }
    }

    @ViewBuilder
    private var hiddenDirectNavigation: some View {
        NavigationLink(isActive: $openSelectedNovena) {
            if let selectedNovena {
                NovenaDetailView(contentRepository: environment.contentRepository, novena: selectedNovena)
            }
        } label: {
            EmptyView()
        }
        .hidden()

        NavigationLink(isActive: $openSelectedSaint) {
            if let selectedSaint {
                SaintDetailView(contentRepository: environment.contentRepository, saint: selectedSaint)
            }
        } label: {
            EmptyView()
        }
        .hidden()
    }

    private var title: String {
        mode == .intentions ? localization.t("search.intentionsTitle") : localization.t("search.patronageTitle")
    }

    private var prompt: String {
        mode == .intentions ? localization.t("search.intentionsPrompt") : localization.t("search.patronagePrompt")
    }

    private var accent: Color {
        mode == .intentions ? AppTheme.glowRose : AppTheme.glowGold
    }

    private var resultCount: Int {
        mode == .intentions ? novenas.count : saints.count
    }

    private func resetAndLoadTerms() async {
        selectedTerm = nil
        novenas = []
        saints = []
        await loadTerms()
    }

    private func loadTerms() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let locale = localization.language.contentLocale
            switch mode {
            case .intentions:
                terms = try await environment.contentRepository.searchIntentionTerms(locale: locale, query: query)
            case .patronage:
                terms = try await environment.contentRepository.searchPatronageTerms(locale: locale, query: query)
            }
        } catch {
            terms = []
            errorMessage = error.localizedDescription
        }
    }

    private func select(_ term: SearchTerm) async {
        selectedTerm = term
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let locale = localization.language.contentLocale
            switch mode {
            case .intentions:
                let results = try await environment.contentRepository.novenasByIntention(locale: locale, key: term.key)
                if results.count == 1 {
                    selectedTerm = nil
                    selectedNovena = results[0]
                    openSelectedNovena = true
                } else {
                    novenas = results
                }
            case .patronage:
                let results = try await environment.contentRepository.saintsByPatronage(locale: locale, key: term.key)
                if results.count == 1 {
                    selectedTerm = nil
                    selectedSaint = results[0]
                    openSelectedSaint = true
                } else {
                    saints = results
                }
            }
        } catch {
            novenas = []
            saints = []
            errorMessage = error.localizedDescription
        }
    }

    private func selectedTermHeader(_ term: SearchTerm) -> some View {
        HStack(spacing: 10) {
            Button {
                selectedTerm = nil
                novenas = []
                saints = []
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 34, height: 34)
                    .background(accent.opacity(0.18))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text(term.label)
                    .font(AppTheme.rounded(17, weight: .bold))
                    .foregroundStyle(.white)
                Text(resultLabel(for: term))
                    .font(AppTheme.rounded(13, weight: .semibold))
                    .foregroundStyle(AppTheme.cardText.opacity(0.72))
            }

            Spacer()
        }
        .padding(14)
        .appGlassCard(cornerRadius: 22)
    }

    private func resultLabel(for term: SearchTerm) -> String {
        let labels = (term.resultLabels ?? [])
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .prefix(2)
        guard !labels.isEmpty else { return resultLabel(for: term.resultCount) }

        let suffix = term.resultCount > labels.count ? " +\(term.resultCount - labels.count)" : ""
        return labels.joined(separator: " • ") + suffix
    }

    private func resultLabel(for count: Int) -> String {
        "\(count) \(localization.t("search.results"))"
    }
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
    @EnvironmentObject private var localization: LocalizationManager
    let prompt: String
    @Binding var text: String
    var onSubmit: (() -> Void)? = nil
    @FocusState private var isFocused: Bool

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
                .focused($isFocused)
                .submitLabel(.search)
                .onSubmit {
                    isFocused = false
                    onSubmit?()
                }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .appGlassCard(cornerRadius: 28)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button(localization.t("common.done")) {
                    isFocused = false
                }
            }
        }
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
    var imageURLs: [URL] = []

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            if let imageURL {
                SearchResultThumbnail(imageURL: imageURL, accent: accent, icon: icon)
            } else if !imageURLs.isEmpty {
                SearchResultThumbnailStack(imageURLs: imageURLs, accent: accent, icon: icon)
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
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .font(AppTheme.rounded(13, weight: .semibold))
                        .foregroundStyle(accent)
                        .lineLimit(3)
                }
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(AppTheme.rounded(15, weight: .medium))
                        .foregroundStyle(AppTheme.cardText.opacity(0.78))
                        .lineLimit(3)
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

private struct SearchResultThumbnailStack: View {
    let imageURLs: [URL]
    let accent: Color
    let icon: String

    private var urls: [URL] {
        Array(imageURLs.prefix(3))
    }

    var body: some View {
        if urls.count <= 1, let first = urls.first {
            SearchResultThumbnail(imageURL: first, accent: accent, icon: icon)
        } else {
            ZStack(alignment: .topLeading) {
                ForEach(Array(urls.enumerated()), id: \.offset) { index, url in
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                        default:
                            ZStack {
                                RoundedRectangle(cornerRadius: 13, style: .continuous)
                                    .fill(accent.opacity(0.16))
                                Image(systemName: icon)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(accent)
                            }
                        }
                    }
                    .frame(width: 50, height: 66)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 13, style: .continuous)
                            .stroke(Color(red: 0.08, green: 0.18, blue: 0.25), lineWidth: 2)
                    )
                    .offset(x: CGFloat(index * 12), y: CGFloat(index * 4))
                }
            }
            .frame(width: 74, height: 82, alignment: .topLeading)
        }
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
