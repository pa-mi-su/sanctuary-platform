import Foundation

actor HybridContentRepository: ContentRepository, SaintRangeRepository {
    private let apiClient: SanctuaryAPIClient
    private let localRepository: LocalContentRepository

    init(apiClient: SanctuaryAPIClient, localRepository: LocalContentRepository) {
        self.apiClient = apiClient
        self.localRepository = localRepository
    }

    func listSaints(
        locale: ContentLocale,
        feastDate: FeastDateFilter?,
        query: String?
    ) async throws -> [Saint] {
        do {
            let remoteSaints = try await apiClient.listSaints(locale: locale, feastDate: feastDate, query: query)
            return remoteSaints.map { mapSaintSummary($0, locale: locale) }
        } catch {
            return try await localRepository.listSaints(locale: locale, feastDate: feastDate, query: query)
        }
    }

    func fetchSaint(slug: String, locale: ContentLocale) async throws -> Saint? {
        do {
            if let remoteSaint = try await apiClient.fetchSaint(slug: slug, locale: locale) {
                return mapSaintDetail(remoteSaint, locale: locale)
            }
            return nil
        } catch {
            return try await localRepository.fetchSaint(slug: slug, locale: locale)
        }
    }

    func listSaintsInRange(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [Saint] {
        do {
            let remoteSaints = try await apiClient.listSaintsInRange(
                locale: locale,
                startDate: startDate,
                endDate: endDate
            )
            return remoteSaints.map { mapSaintSummary($0, locale: locale) }
        } catch {
            return try await localRepository.listSaintsInRange(
                locale: locale,
                startDate: startDate,
                endDate: endDate
            )
        }
    }

    func listNovenas(
        locale: ContentLocale,
        tag: String?,
        query: String?
    ) async throws -> [Novena] {
        do {
            let remoteNovenas = try await apiClient.listNovenas(locale: locale, query: query)
            let mapped = remoteNovenas.map { mapNovenaSummary($0, locale: locale) }
            guard let tag, !tag.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                return mapped
            }
            let normalizedTag = tag.lowercased()
            return mapped.filter { $0.tags.contains(where: { $0.lowercased() == normalizedTag }) }
        } catch {
            return try await localRepository.listNovenas(locale: locale, tag: tag, query: query)
        }
    }

    func fetchNovena(slug: String, locale: ContentLocale) async throws -> Novena? {
        do {
            if let remoteNovena = try await apiClient.fetchNovena(slug: slug, locale: locale) {
                return mapNovenaDetail(remoteNovena, locale: locale)
            }
            return nil
        } catch {
            return try await localRepository.fetchNovena(slug: slug, locale: locale)
        }
    }

    func searchNovenasByIntentions(
        locale: ContentLocale,
        query: String
    ) async throws -> [Novena] {
        do {
            let summaries = try await apiClient.searchNovenasByIntentions(locale: locale, query: query)
            var results: [Novena] = []
            for summary in summaries {
                if let detail = try? await apiClient.fetchNovena(slug: summary.slug, locale: locale) {
                    results.append(mapNovenaDetail(detail, locale: locale))
                } else {
                    results.append(mapNovenaSummary(summary, locale: locale))
                }
            }
            return results
        } catch {
            return try await localRepository.searchNovenasByIntentions(locale: locale, query: query)
        }
    }

    func listNovenaCalendarDays(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [NovenaCalendarDay] {
        do {
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
        } catch {
            return try await localRepository.listNovenaCalendarDays(
                locale: locale,
                startDate: startDate,
                endDate: endDate
            )
        }
    }

    func fetchNovenaServingWindow(
        novenaID: String,
        year: Int
    ) async throws -> NovenaServingWindowInfo? {
        do {
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
        } catch {
            return try await localRepository.fetchNovenaServingWindow(novenaID: novenaID, year: year)
        }
    }

    func listPrayers(
        locale: ContentLocale,
        category: String?,
        query: String?
    ) async throws -> [Prayer] {
        try await localRepository.listPrayers(locale: locale, category: category, query: query)
    }

    func listLiturgicalDays(
        startDate: Date,
        endDate: Date
    ) async throws -> [LiturgicalDay] {
        do {
            let remoteDays = try await apiClient.listLiturgicalDays(startDate: startDate, endDate: endDate)
            return remoteDays.compactMap(mapLiturgicalDay)
        } catch {
            return try await localRepository.listLiturgicalDays(startDate: startDate, endDate: endDate)
        }
    }

    func fetchLiturgicalDay(for date: Date) async throws -> LiturgicalDay? {
        do {
            return mapLiturgicalDay(try await apiClient.fetchLiturgicalDay(date: date))
        } catch {
            return try await localRepository.fetchLiturgicalDay(for: date)
        }
    }

    private func mapSaintSummary(_ response: APIContentSaintSummaryResponse, locale: ContentLocale) -> Saint {
        Saint(
            id: response.id,
            slug: response.slug,
            name: response.name,
            nameByLocale: localizedValueMap(value: response.name, locale: locale),
            feastMonth: response.feastMonth,
            feastDay: response.feastDay,
            imageURL: url(from: response.imageUrl),
            tags: [],
            patronages: [],
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
            patronages: [],
            feastLabelByLocale: localizedValueMap(value: response.feastLabel, locale: locale),
            summaryByLocale: localizedValueMap(value: response.summary ?? "", locale: locale),
            biographyByLocale: localizedValueMap(value: response.biography ?? "", locale: locale),
            prayersByLocale: [:],
            sources: response.sources
        )
    }

    private func localizedValueMap(value: String, locale: ContentLocale) -> [ContentLocale: String] {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [:] }
        return [.en: trimmed, locale: trimmed]
    }

    private func mapNovenaSummary(_ response: APIContentNovenaSummaryResponse, locale: ContentLocale) -> Novena {
        Novena(
            id: response.id,
            slug: response.slug,
            titleByLocale: localizedValueMap(value: response.title, locale: locale),
            descriptionByLocale: localizedValueMap(value: response.description, locale: locale),
            durationDays: response.durationDays,
            tags: [],
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
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: trimmed)
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
            readingURL: url(from: response.readingsUrl)
        )
    }
}
