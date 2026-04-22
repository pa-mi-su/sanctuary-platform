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
        try await localRepository.listNovenas(locale: locale, tag: tag, query: query)
    }

    func fetchNovena(slug: String, locale: ContentLocale) async throws -> Novena? {
        try await localRepository.fetchNovena(slug: slug, locale: locale)
    }

    func listPrayers(
        locale: ContentLocale,
        category: String?,
        query: String?
    ) async throws -> [Prayer] {
        try await localRepository.listPrayers(locale: locale, category: category, query: query)
    }

    func fetchLiturgicalDay(for date: Date) async throws -> LiturgicalDay? {
        try await localRepository.fetchLiturgicalDay(for: date)
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

    private func url(from raw: String?) -> URL? {
        guard let raw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        if let direct = URL(string: raw) {
            return direct
        }
        return raw.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed).flatMap(URL.init(string:))
    }
}
