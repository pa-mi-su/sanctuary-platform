import Foundation

struct PreviewContentRepository: ContentRepository, SaintRangeRepository {
    let saints: [Saint]
    let novenas: [Novena]
    let prayers: [Prayer]
    let liturgicalDays: [LiturgicalDay]

    init(
        saints: [Saint] = [],
        novenas: [Novena] = [],
        prayers: [Prayer] = [],
        liturgicalDays: [LiturgicalDay] = []
    ) {
        self.saints = saints
        self.novenas = novenas
        self.prayers = prayers
        self.liturgicalDays = liturgicalDays
    }

    func listSaints(
        locale: ContentLocale,
        feastDate: FeastDateFilter?,
        query: String?
    ) async throws -> [Saint] {
        saints.filter { saint in
            let matchesDate = feastDate.map { saint.feastMonth == $0.month && saint.feastDay == $0.day } ?? true
            let matchesQuery = query.map { term in
                saint.displayName(locale: locale).localizedCaseInsensitiveContains(term)
            } ?? true
            return matchesDate && matchesQuery
        }
    }

    func fetchSaint(slug: String, locale: ContentLocale) async throws -> Saint? {
        saints.first { $0.slug == slug || $0.id == slug }
    }

    func listSaintsInRange(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [Saint] {
        let calendar = Calendar(identifier: .gregorian)
        let lower = min(startDate, endDate)
        let upper = max(startDate, endDate)
        return saints.filter { saint in
            guard let feastDate = calendar.date(from: DateComponents(year: calendar.component(.year, from: lower), month: saint.feastMonth, day: saint.feastDay)) else {
                return false
            }
            return feastDate >= lower && feastDate <= upper
        }
    }

    func listNovenas(
        locale: ContentLocale,
        tag: String?,
        query: String?
    ) async throws -> [Novena] {
        novenas.filter { novena in
            let matchesTag = tag.map { filter in
                novena.tags.contains { $0.caseInsensitiveCompare(filter) == .orderedSame }
            } ?? true
            let matchesQuery = query.map { term in
                let title = novena.titleByLocale[locale] ?? novena.titleByLocale[.en] ?? novena.slug
                return title.localizedCaseInsensitiveContains(term)
            } ?? true
            return matchesTag && matchesQuery
        }
    }

    func fetchNovena(slug: String, locale: ContentLocale) async throws -> Novena? {
        novenas.first { $0.slug == slug || $0.id == slug }
    }

    func searchNovenasByIntentions(
        locale: ContentLocale,
        query: String
    ) async throws -> [Novena] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !term.isEmpty else { return novenas }
        return novenas.filter { novena in
            let haystack = novena.days
                .compactMap { $0.bodyByLocale[locale] ?? $0.bodyByLocale[.en] }
                .joined(separator: "\n")
            return haystack.localizedCaseInsensitiveContains(term)
        }
    }

    func listNovenaCalendarDays(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [NovenaCalendarDay] {
        let calendar = Calendar(identifier: .gregorian)
        let lower = min(startDate, endDate)
        let upper = max(startDate, endDate)
        var cursor = lower
        var days: [NovenaCalendarDay] = []

        while cursor <= upper {
            days.append(NovenaCalendarDay(date: cursor, novenas: [], startingNovena: nil))
            guard let next = calendar.date(byAdding: .day, value: 1, to: cursor) else { break }
            cursor = next
        }

        return days
    }

    func fetchNovenaServingWindow(
        novenaID: String,
        year: Int
    ) async throws -> NovenaServingWindowInfo? {
        nil
    }

    func listPrayers(
        locale: ContentLocale,
        category: String?,
        query: String?
    ) async throws -> [Prayer] {
        prayers.filter { prayer in
            let matchesCategory = category.map { prayer.category.caseInsensitiveCompare($0) == .orderedSame }
                ?? (prayer.category.caseInsensitiveCompare("rosary") != .orderedSame)
            let matchesQuery = query.map { term in
                let title = prayer.titleByLocale[locale] ?? prayer.titleByLocale[.en] ?? prayer.slug
                return title.localizedCaseInsensitiveContains(term)
            } ?? true
            return matchesCategory && matchesQuery
        }
    }

    func fetchPrayer(slug: String, locale: ContentLocale) async throws -> Prayer? {
        prayers.first { $0.slug == slug || $0.id == slug }
    }

    func listLiturgicalDays(
        startDate: Date,
        endDate: Date
    ) async throws -> [LiturgicalDay] {
        let lower = min(startDate, endDate)
        let upper = max(startDate, endDate)
        return liturgicalDays.filter { $0.date >= lower && $0.date <= upper }
    }

    func fetchLiturgicalDay(for date: Date) async throws -> LiturgicalDay? {
        let calendar = Calendar(identifier: .gregorian)
        return liturgicalDays.first { calendar.isDate($0.date, inSameDayAs: date) }
    }
}
