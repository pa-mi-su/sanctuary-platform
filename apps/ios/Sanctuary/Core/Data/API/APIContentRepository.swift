import Foundation

actor APIChurchNewsRepository: ChurchNewsRepository {
    private let apiClient: SanctuaryAPIClient
    private let cacheRoot: URL

    init(apiClient: SanctuaryAPIClient) {
        self.apiClient = apiClient
        self.cacheRoot = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        for locale in ContentLocale.allCases {
            let legacyCache = cacheRoot.appendingPathComponent("church-news-v2-\(locale.rawValue).json")
            try? FileManager.default.removeItem(at: legacyCache)
        }
    }

    func cachedArticles(locale: ContentLocale) async -> [ChurchNewsArticle] {
        let cacheURL = cacheURL(for: locale)
        guard let data = try? Data(contentsOf: cacheURL),
              let envelope = try? JSONDecoder.sanctuaryNews.decode(NewsCacheEnvelope.self, from: data),
              Date().timeIntervalSince(envelope.savedAt) < 7 * 24 * 60 * 60
        else { return [] }
        return envelope.articles
    }

    func refreshArticles(locale: ContentLocale) async throws -> [ChurchNewsArticle] {
        let responses = try await apiClient.listChurchNews(locale: locale)
        let articles: [ChurchNewsArticle] = responses.compactMap { response -> ChurchNewsArticle? in
            guard let canonicalURL = URL(string: response.canonicalUrl),
                  canonicalURL.scheme == "https",
                  let imageURL = URL(string: response.imageUrl), imageURL.scheme == "https",
                  let licenseURL = URL(string: response.licenseUrl), licenseURL.scheme == "https"
            else { return nil }
            return ChurchNewsArticle(
                id: response.id,
                title: response.title,
                summary: response.summary,
                sourceName: response.sourceName,
                canonicalURL: canonicalURL,
                imageURL: imageURL,
                imageAlt: response.imageAlt,
                imageCredit: response.imageCredit,
                licenseName: response.licenseName,
                licenseURL: licenseURL,
                publishedAt: response.publishedAt,
                language: response.language
            )
        }
        let envelope = NewsCacheEnvelope(savedAt: Date(), articles: articles)
        if !articles.isEmpty, let data = try? JSONEncoder.sanctuaryNews.encode(envelope) {
            let cacheURL = cacheURL(for: locale)
            try? FileManager.default.createDirectory(
                at: cacheURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            try? data.write(to: cacheURL, options: Data.WritingOptions.atomic)
        }
        return articles
    }

    private func cacheURL(for locale: ContentLocale) -> URL {
        cacheRoot.appendingPathComponent("church-news-v3-\(locale.rawValue).json")
    }

    private struct NewsCacheEnvelope: Codable {
        let savedAt: Date
        let articles: [ChurchNewsArticle]
    }
}

private extension JSONDecoder {
    nonisolated static var sanctuaryNews: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}

private extension JSONEncoder {
    nonisolated static var sanctuaryNews: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}

actor APIContentRepository: ContentRepository, SaintRangeRepository {
    private let apiClient: SanctuaryAPIClient
    private let cacheRoot: URL
    private let apiDayCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        return calendar
    }()

    init(apiClient: SanctuaryAPIClient) {
        self.apiClient = apiClient
        self.cacheRoot = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
    }

    func listSaints(
        locale: ContentLocale,
        feastDate: FeastDateFilter?,
        query: String?
    ) async throws -> [Saint] {
        let remoteSaints = try await apiClient.listSaints(locale: locale, feastDate: feastDate, query: query)
        return remoteSaints.map { mapSaintSummary($0, locale: locale) }
    }

    func fetchSaint(slug: String, locale: ContentLocale) async throws -> Saint? {
        guard let remoteSaint = try await apiClient.fetchSaint(slug: slug, locale: locale) else {
            return nil
        }
        return mapSaintDetail(remoteSaint, locale: locale)
    }

    func listSaintsInRange(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [Saint] {
        let remoteGroups = try await apiClient.listSaintsInRange(
            locale: locale,
            startDate: startDate,
            endDate: endDate
        )
        return remoteGroups.flatMap { group in
            let feastDate = date(from: group.date)
            return group.saints.map { mapSaintSummary($0, locale: locale, feastDateOverride: feastDate) }
        }
    }

    func listNovenas(
        locale: ContentLocale,
        tag: String?,
        query: String?
    ) async throws -> [Novena] {
        let remoteNovenas = try await apiClient.listNovenas(locale: locale, query: query)
        let mapped = remoteNovenas.map { mapNovenaSummary($0, locale: locale) }
        guard let tag, !tag.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return mapped
        }
        let normalizedTag = tag.lowercased()
        return mapped.filter { $0.tags.contains(where: { $0.lowercased() == normalizedTag }) }
    }

    func fetchNovena(slug: String, locale: ContentLocale) async throws -> Novena? {
        guard let remoteNovena = try await apiClient.fetchNovena(slug: slug, locale: locale) else {
            return nil
        }
        return mapNovenaDetail(remoteNovena, locale: locale)
    }

    func searchIntentionTerms(
        locale: ContentLocale,
        query: String
    ) async throws -> [SearchTerm] {
        try await apiClient.searchIntentionTerms(locale: locale, query: query.trimmingCharacters(in: .whitespacesAndNewlines))
            .map(mapSearchTerm)
    }

    func novenasByIntention(
        locale: ContentLocale,
        key: String
    ) async throws -> [Novena] {
        try await apiClient.novenasByIntention(locale: locale, key: key)
            .map { mapNovenaSummary($0, locale: locale) }
    }

    func searchPatronageTerms(
        locale: ContentLocale,
        query: String
    ) async throws -> [SearchTerm] {
        try await apiClient.searchPatronageTerms(locale: locale, query: query.trimmingCharacters(in: .whitespacesAndNewlines))
            .map(mapSearchTerm)
    }

    func saintsByPatronage(
        locale: ContentLocale,
        key: String
    ) async throws -> [Saint] {
        try await apiClient.saintsByPatronage(locale: locale, key: key)
            .map { mapSaintSummary($0, locale: locale) }
    }

    func listNovenaCalendarDays(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [NovenaCalendarDay] {
        let remoteDays = try await apiClient.listNovenaCalendarDays(
            locale: locale,
            startDate: startDate,
            endDate: endDate
        )
        return remoteDays.compactMap { day in
            guard let parsedDate = date(from: day.date) else { return nil }
            return NovenaCalendarDay(
                date: parsedDate,
                novenas: day.novenas.map { mapNovenaSummary($0, locale: locale) },
                startingNovena: day.startingNovena.map { mapNovenaSummary($0, locale: locale) }
            )
        }
    }

    func fetchNovenaServingWindow(
        novenaID: String,
        year: Int
    ) async throws -> NovenaServingWindowInfo? {
        guard let response = try await apiClient.fetchNovenaServingWindow(novenaID: novenaID, year: year),
              let startDate = date(from: response.startDate),
              let endDate = date(from: response.endDate),
              let feastDate = date(from: response.feastDate)
        else {
            return nil
        }

        return NovenaServingWindowInfo(
            novenaID: response.novenaId,
            startDate: startDate,
            endDate: endDate,
            feastDate: feastDate
        )
    }

    func listPrayers(
        locale: ContentLocale,
        category: String?,
        query: String?
    ) async throws -> [Prayer] {
        let normalizedCategory = category?.trimmingCharacters(in: .whitespacesAndNewlines)
        let remotePrayers = try await apiClient.listPrayers(
            locale: locale,
            category: normalizedCategory?.isEmpty == false ? normalizedCategory : nil,
            excludeCategory: normalizedCategory?.isEmpty == false ? nil : "rosary",
            query: query
        )
        let mapped = remotePrayers.map { mapPrayerSummary($0, locale: locale) }
        guard let normalizedCategory, !normalizedCategory.isEmpty else {
            return mapped.filter { $0.category.caseInsensitiveCompare("rosary") != .orderedSame }
        }
        return mapped.filter { $0.category.lowercased() == normalizedCategory.lowercased() }
    }

    func fetchPrayer(
        slug: String,
        locale: ContentLocale
    ) async throws -> Prayer? {
        guard let remotePrayer = try await apiClient.fetchPrayer(slug: slug, locale: locale) else {
            return nil
        }
        return mapPrayerDetail(remotePrayer, locale: locale)
    }

    func listLiturgicalDays(
        startDate: Date,
        endDate: Date
    ) async throws -> [LiturgicalDay] {
        let remoteDays = try await apiClient.listLiturgicalDays(startDate: startDate, endDate: endDate)
        return remoteDays.compactMap(mapLiturgicalDay)
    }

    func fetchLiturgicalDay(for date: Date) async throws -> LiturgicalDay? {
        do {
            let day = mapLiturgicalDay(try await apiClient.fetchLiturgicalDay(date: date))
            if let day {
                persistLiturgicalDay(day, for: date)
            }
            return day ?? cachedLiturgicalDay(for: date)
        } catch {
            if let cached = cachedLiturgicalDay(for: date) {
                return cached
            }
            throw error
        }
    }

    private func persistLiturgicalDay(_ day: LiturgicalDay, for date: Date) {
        guard apiDayCalendar.isDate(day.date, inSameDayAs: date),
              let data = try? JSONEncoder.sanctuaryNews.encode(day)
        else { return }

        let cacheURL = liturgicalDayCacheURL(for: date)
        try? FileManager.default.createDirectory(
            at: cacheURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        try? data.write(to: cacheURL, options: .atomic)
    }

    private func cachedLiturgicalDay(for date: Date) -> LiturgicalDay? {
        let cacheURL = liturgicalDayCacheURL(for: date)
        guard let data = try? Data(contentsOf: cacheURL),
              let day = try? JSONDecoder.sanctuaryNews.decode(LiturgicalDay.self, from: data),
              apiDayCalendar.isDate(day.date, inSameDayAs: date)
        else { return nil }
        return day
    }

    private func liturgicalDayCacheURL(for date: Date) -> URL {
        let components = apiDayCalendar.dateComponents([.year, .month, .day], from: date)
        let key = String(
            format: "%04d-%02d-%02d",
            components.year ?? 0,
            components.month ?? 0,
            components.day ?? 0
        )
        return cacheRoot
            .appendingPathComponent("liturgical-day-v1", isDirectory: true)
            .appendingPathComponent("\(key).json")
    }

    private func mapSaintSummary(
        _ response: APIContentSaintSummaryResponse,
        locale: ContentLocale,
        feastDateOverride: Date? = nil
    ) -> Saint {
        let feastMonth = feastDateOverride.map { apiDayCalendar.component(.month, from: $0) } ?? response.feastMonth
        let feastDay = feastDateOverride.map { apiDayCalendar.component(.day, from: $0) } ?? response.feastDay

        return Saint(
            id: response.id,
            slug: response.slug,
            name: response.name,
            nameByLocale: localizedValueMap(value: response.name, locale: locale),
            feastMonth: feastMonth,
            feastDay: feastDay,
            imageURL: url(from: response.imageUrl),
            tags: [],
            patronages: response.patronages ?? [],
            intentions: response.intentions ?? [],
            feastLabelByLocale: localizedValueMap(value: response.feastLabel, locale: locale),
            summaryByLocale: localizedValueMap(value: response.summary ?? "", locale: locale),
            biographyByLocale: [:],
            prayersByLocale: [:],
            sources: []
        )
    }

    private func mapSaintDetail(_ response: APIContentSaintDetailResponse, locale: ContentLocale) -> Saint {
        Saint(
            id: response.id,
            slug: response.slug,
            name: response.name,
            nameByLocale: localizedValueMap(value: response.name, locale: locale),
            feastMonth: response.feastMonth,
            feastDay: response.feastDay,
            imageURL: url(from: response.imageUrl),
            tags: [],
            patronages: response.patronages ?? [],
            intentions: response.intentions ?? [],
            feastLabelByLocale: localizedValueMap(value: response.feastLabel, locale: locale),
            summaryByLocale: localizedValueMap(value: response.summary ?? "", locale: locale),
            biographyByLocale: localizedValueMap(value: response.biography ?? "", locale: locale),
            prayersByLocale: [:],
            sources: response.sources.map { source in
                if let url = source.url?.trimmingCharacters(in: .whitespacesAndNewlines), !url.isEmpty {
                    return "\(source.text) \(url)"
                }
                return source.text
            }
        )
    }

    private func localizedValueMap(value: String, locale: ContentLocale) -> [ContentLocale: String] {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [:] }
        var values: [ContentLocale: String] = [.en: trimmed]
        values[locale] = trimmed
        return values
    }

    private func mapSearchTerm(_ response: APIContentSearchTermResponse) -> SearchTerm {
        SearchTerm(
            key: response.key,
            label: response.label,
            resultCount: response.resultCount,
            resultLabels: response.resultLabels,
            imageURLs: response.imageUrls?.compactMap { url(from: $0) } ?? []
        )
    }

    private func mapNovenaSummary(_ response: APIContentNovenaSummaryResponse, locale: ContentLocale) -> Novena {
        Novena(
            id: response.id,
            slug: response.slug,
            titleByLocale: localizedValueMap(value: response.title, locale: locale),
            descriptionByLocale: localizedValueMap(value: response.description, locale: locale),
            durationDays: response.durationDays,
            tags: [],
            intentions: response.intentions ?? [],
            imageURL: url(from: response.imageUrl),
            days: []
        )
    }

    private func mapNovenaDetail(_ response: APIContentNovenaDetailResponse, locale: ContentLocale) -> Novena {
        let days = response.days.map { day in
            let title = localizedValueMap(value: day.title ?? "", locale: locale)
            let scripture = localizedValueMap(value: day.scripture ?? "", locale: locale)
            let prayer = localizedValueMap(value: day.prayer ?? "", locale: locale)
            let reflection = localizedValueMap(value: day.reflection ?? "", locale: locale)
            let body = localizedValueMap(value: day.body ?? "", locale: locale)

            return NovenaDay(
                dayNumber: day.dayNumber,
                titleByLocale: title,
                scriptureByLocale: scripture,
                prayerByLocale: prayer,
                reflectionByLocale: reflection,
                bodyByLocale: body.isEmpty ? [
                    .en: [title[.en], scripture[.en], prayer[.en], reflection[.en]]
                        .compactMap { $0 }
                        .joined(separator: "\n\n")
                ] : body
            )
        }

        return Novena(
            id: response.id,
            slug: response.slug,
            titleByLocale: localizedValueMap(value: response.title, locale: locale),
            descriptionByLocale: localizedValueMap(value: response.description, locale: locale),
            durationDays: response.durationDays,
            tags: response.tags,
            intentions: response.intentions,
            imageURL: url(from: response.imageUrl),
            days: days
        )
    }

    private func url(from raw: String?) -> URL? {
        guard let raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        if let direct = URL(string: raw) {
            return direct
        }
        return raw.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed).flatMap(URL.init(string:))
    }

    private func date(from raw: String) -> Date? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let parts = trimmed.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]) else {
            return nil
        }

        var components = DateComponents()
        components.calendar = apiDayCalendar
        components.timeZone = apiDayCalendar.timeZone
        components.year = year
        components.month = month
        components.day = day
        components.hour = 12
        return apiDayCalendar.date(from: components)
    }

    private func mapLiturgicalDay(_ response: APILiturgicalDayResponse) -> LiturgicalDay? {
        guard let date = date(from: response.date),
              let season = LiturgicalSeason(rawValue: response.season.lowercased()) else {
            return nil
        }

        return LiturgicalDay(
            date: date,
            season: season,
            rank: response.primaryRank,
            observances: response.observances,
            readingURL: url(from: response.readingsUrl),
            rankType: response.rankType,
            color: response.color ?? defaultLiturgicalColor(
                for: season,
                rankType: response.rankType,
                title: response.primaryRank
            )
        )
    }

    private func defaultLiturgicalColor(for season: LiturgicalSeason, rankType: String, title: String) -> String {
        if title.contains("Palm Sunday") || title.contains("Good Friday") || title.contains("Pentecost") {
            return "RED"
        }
        if ["SOLEMNITY", "FEAST", "MEMORIAL"].contains(rankType.uppercased()) {
            return "WHITE"
        }
        switch season {
        case .advent, .lent: return "VIOLET"
        case .christmas, .easter: return "WHITE"
        case .ordinary: return "GREEN"
        }
    }

    private func mapPrayerSummary(_ response: APIPrayerSummaryResponse, locale: ContentLocale) -> Prayer {
        Prayer(
            id: response.id,
            slug: response.slug,
            category: response.category,
            titleByLocale: localizedValueMap(value: response.title, locale: locale),
            bodyByLocale: localizedValueMap(value: response.bodyPreview, locale: locale),
            imageURL: url(from: response.imageUrl),
            tags: []
        )
    }

    private func mapPrayerDetail(_ response: APIPrayerDetailResponse, locale: ContentLocale) -> Prayer {
        Prayer(
            id: response.id,
            slug: response.slug,
            category: response.category,
            titleByLocale: localizedValueMap(value: response.title, locale: locale),
            bodyByLocale: localizedValueMap(value: response.body, locale: locale),
            alternateTitleByLocale: localizedValueMap(value: response.alternateTitle ?? "", locale: locale),
            noteByLocale: localizedValueMap(value: response.note ?? "", locale: locale),
            imageURL: url(from: response.imageUrl),
            sourceTitle: response.sourceTitle,
            sourceType: response.sourceType,
            tags: response.tags
        )
    }
}
