import SwiftUI
import SafariServices

enum CalendarMode: String, CaseIterable {
    case day = "Day"
    case week = "Week"
    case month = "Month"
}

private let liturgicalUICalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = .autoupdatingCurrent
    return calendar
}()

private func currentLiturgicalDateComponents() -> DateComponents {
    liturgicalUICalendar.dateComponents([.year, .month, .day], from: Date())
}

struct NovenasCalendarView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager
    @State private var mode: CalendarMode = .day
    @State private var selectedDay = currentLiturgicalDateComponents().day ?? 1
    @State private var selectedMonth = currentLiturgicalDateComponents().month ?? 1
    @State private var selectedYear = currentLiturgicalDateComponents().year ?? 2000
    @State private var showSearch = false
    @State private var showIntentionsSearch = false
    @State private var selectedNovenaSelection: CalendarSelection?
    @State private var tapFeedbackMessage: String?
    @State private var suppressDayTapUntil: Date = .distantPast
    @State private var showDatePicker = false
    @State private var novenaIDByDay: [Int: String] = [:]
    @State private var novenaTitleByDay: [Int: String] = [:]
    @State private var novenaImageURLByDay: [Int: URL] = [:]
    @State private var seasonByDay: [Int: LiturgicalSeason] = [:]

    private var displayedSeason: LiturgicalSeason {
        seasonByDay[selectedDay] ?? .ordinary
    }

    private var displayedSeasonBorderColor: Color {
        AppTheme.liturgicalBorderColor(for: displayedSeason)
    }

    private func borderColor(for day: Int) -> Color {
        guard let season = seasonByDay[day] else {
            return displayedSeasonBorderColor
        }
        return AppTheme.liturgicalBorderColor(for: season)
    }

    var body: some View {
        let maxDay = daysInMonth(year: selectedYear, month: selectedMonth)
        CalendarScaffold(
            headerTitle: mode == .day
                ? localization.formatMonthDayYear(month: selectedMonth, day: selectedDay, year: selectedYear)
                : localization.formatMonthYear(month: selectedMonth, year: selectedYear),
            subtitle: localization.t("calendar.subtitle.novenas"),
            mode: $mode,
            searchTitle: localization.t("calendar.searchNovenas"),
            secondarySearchTitle: localization.t("calendar.searchIntentions"),
            onSearchTap: { showSearch = true },
            onSecondarySearchTap: { showIntentionsSearch = true },
            onPrev: { goPrevious() },
            onNext: { goNext() },
            onToday: {
                let today = currentLiturgicalDateComponents()
                selectedDay = today.day ?? selectedDay
                selectedMonth = today.month ?? selectedMonth
                selectedYear = today.year ?? selectedYear
                mode = .day
            },
            onModeChanged: { suppressDayTapUntil = Date().addingTimeInterval(0.35) },
            onHeaderTap: { showDatePicker = true }
        ) {
            if mode == .month {
                MonthGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: novenaLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            } else if mode == .week {
                WeekGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: novenaLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            } else {
                DayCard(
                    title: "\(selectedDay)",
                    subtitle: selectedNovenaTitleForDay(),
                    imageURL: selectedNovenaImageURLForDay(),
                    borderColor: displayedSeasonBorderColor,
                    onTap: {
                        guard Date() >= suppressDayTapUntil else { return }
                        let next = novenaIDForSelectedDay()
                        if let next {
                            selectedNovenaSelection = CalendarSelection(id: next)
                        } else {
                            tapFeedbackMessage = "\(localization.t("calendar.noNovenaMapped")) \(localization.monthName(selectedMonth)) \(selectedDay)."
                        }
                    }
                )
            }
        }
        .task(id: "\(selectedYear)-\(selectedMonth)") {
            await loadSeasonLookups()
            await loadNovenaLookups()
        }
        .sheet(isPresented: $showSearch) {
            NovenasSearchView(environment: environment)
        }
        .sheet(isPresented: $showIntentionsSearch) {
            NovenasSearchView(environment: environment, mode: .intentions)
        }
        .sheet(item: $selectedNovenaSelection) { selection in
            NovenaDetailView(
                contentRepository: environment.contentRepository,
                novena: Novena(
                    id: selection.id,
                    slug: selection.id,
                    titleByLocale: [.en: novenaTitleByDay[selectedDay] ?? selection.id],
                    descriptionByLocale: [.en: ""],
                    durationDays: 1,
                    tags: [],
                    imageURL: novenaImageURLByDay[selectedDay],
                    days: []
                ),
                displayYear: selectedYear,
                onClose: { selectedNovenaSelection = nil }
            )
        }
        .sheet(isPresented: $showDatePicker) {
            CalendarDatePickerSheet(
                initialDate: selectedDate(),
                onApply: { date in apply(date: date) }
            )
        }
        .alert(localization.t("calendar.noEntry"), isPresented: Binding(
            get: { tapFeedbackMessage != nil },
            set: { if !$0 { tapFeedbackMessage = nil } }
        )) {
            Button(localization.t("calendar.ok"), role: .cancel) { tapFeedbackMessage = nil }
        } message: {
            Text(tapFeedbackMessage ?? "")
        }
    }

    private func novenaIDForSelectedDay() -> String? {
        novenaIDByDay[selectedDay]
    }

    private func selectedNovenaTitleForDay() -> String {
        guard let title = novenaTitleByDay[selectedDay] else {
            return localization.t("calendar.noNovenaAvailable")
        }
        return title
    }

    private func novenaLabel(for day: Int) -> String {
        guard let title = novenaTitleByDay[day] else {
            return "·"
        }
        return shortLabel(title)
    }

    private func selectedNovenaImageURLForDay() -> URL? {
        novenaImageURLByDay[selectedDay]
    }

    private func loadNovenaLookups() async {
        let calendar = Calendar(identifier: .gregorian)
        guard let monthStart = makeCalendarDate(year: selectedYear, month: selectedMonth, day: 1) else { return }
        guard let monthEnd = calendar.date(byAdding: DateComponents(month: 1, day: -1), to: monthStart) else { return }

        do {
            let entries = try await environment.contentRepository.listNovenaCalendarDays(
                locale: localization.language.contentLocale,
                startDate: monthStart,
                endDate: monthEnd
            )

            var ids: [Int: String] = [:]
            var titles: [Int: String] = [:]
            var images: [Int: URL] = [:]

            for entry in entries {
                let day = calendar.component(.day, from: entry.date)
                let featured = entry.startingNovena ?? entry.novenas.first
                guard let featured else { continue }
                ids[day] = featured.id
                titles[day] = featured.titleByLocale[localization.language.contentLocale]
                    ?? featured.titleByLocale[.en]
                    ?? featured.slug
                if let imageURL = featured.imageURL {
                    images[day] = imageURL
                }
            }

            novenaIDByDay = ids
            novenaTitleByDay = titles
            novenaImageURLByDay = images
        } catch {
            novenaIDByDay = [:]
            novenaTitleByDay = [:]
            novenaImageURLByDay = [:]
        }
    }

    private func loadSeasonLookups() async {
        guard let startDate = makeCalendarDate(year: selectedYear, month: selectedMonth, day: 1),
              let endDate = makeCalendarDate(
                year: selectedYear,
                month: selectedMonth,
                day: daysInMonth(year: selectedYear, month: selectedMonth)
              ) else {
            seasonByDay = [:]
            return
        }

        do {
            let days = try await environment.contentRepository.listLiturgicalDays(
                startDate: startDate,
                endDate: endDate
            )

            seasonByDay = days.reduce(into: [:]) { partialResult, day in
                let dayNumber = liturgicalUICalendar.component(.day, from: day.date)
                partialResult[dayNumber] = day.season
            }
        } catch {
            seasonByDay = [:]
        }
    }

    private func selectedDate() -> Date {
        let clampedDay = min(selectedDay, daysInMonth(year: selectedYear, month: selectedMonth))
        return makeCalendarDate(year: selectedYear, month: selectedMonth, day: clampedDay) ?? Date()
    }

    private func apply(date: Date) {
        let cal = liturgicalUICalendar
        selectedYear = cal.component(.year, from: date)
        selectedMonth = cal.component(.month, from: date)
        selectedDay = cal.component(.day, from: date)
    }

    private func shift(days: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .day, value: days, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func shift(months: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .month, value: months, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func goPrevious() {
        switch mode {
        case .day: shift(days: -1)
        case .week: shift(days: -7)
        case .month: shift(months: -1)
        }
    }

    private func goNext() {
        switch mode {
        case .day: shift(days: 1)
        case .week: shift(days: 7)
        case .month: shift(months: 1)
        }
    }
}

struct LiturgicalCalendarView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager
    @State private var mode: CalendarMode = .month
    @State private var selectedDay = currentLiturgicalDateComponents().day ?? 1
    @State private var selectedMonth = currentLiturgicalDateComponents().month ?? 1
    @State private var selectedYear = currentLiturgicalDateComponents().year ?? 2000
    @State private var suppressDayTapUntil: Date = .distantPast
    @State private var selectedReadingSelection: ReadingSelection?
    @State private var showDatePicker = false
    @State private var liturgicalDayByDay: [Int: LiturgicalDay] = [:]

    private var displayedSeason: LiturgicalSeason {
        liturgicalDayByDay[selectedDay]?.season ?? .ordinary
    }

    private var displayedSeasonBorderColor: Color {
        AppTheme.liturgicalBorderColor(for: displayedSeason)
    }

    private func borderColor(for day: Int) -> Color {
        guard let season = liturgicalDayByDay[day]?.season else {
            return displayedSeasonBorderColor
        }
        return AppTheme.liturgicalBorderColor(for: season)
    }

    var body: some View {
        let maxDay = daysInMonth(year: selectedYear, month: selectedMonth)
        CalendarScaffold(
            headerTitle: mode == .day
                ? localization.formatMonthDayYear(month: selectedMonth, day: selectedDay, year: selectedYear)
                : localization.formatMonthYear(month: selectedMonth, year: selectedYear),
            subtitle: localization.t("calendar.subtitle.liturgical"),
            mode: $mode,
            searchTitle: nil,
            secondarySearchTitle: nil,
            onSearchTap: nil,
            onSecondarySearchTap: nil,
            onPrev: { goPrevious() },
            onNext: { goNext() },
            onToday: {
                let today = currentLiturgicalDateComponents()
                selectedDay = today.day ?? selectedDay
                selectedMonth = today.month ?? selectedMonth
                selectedYear = today.year ?? selectedYear
                mode = .day
            },
            onModeChanged: { suppressDayTapUntil = Date().addingTimeInterval(0.35) },
            onHeaderTap: { showDatePicker = true }
        ) {
            if mode == .month {
                MonthGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: liturgicalLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            } else if mode == .week {
                WeekGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: liturgicalLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            } else {
                DayCard(
                    title: "\(selectedDay)",
                    subtitle: liturgicalTitleForDay(),
                    imageURL: nil,
                    borderColor: displayedSeasonBorderColor,
                    actionLabel: localization.t("calendar.openDailyReadings"),
                    onTap: {
                        guard Date() >= suppressDayTapUntil else { return }
                        let url = liturgicalReadingURLForDay()
                        selectedReadingSelection = ReadingSelection(url: url)
                    }
                )
            }
        }
        .task(id: "\(selectedYear)-\(selectedMonth)") {
            await loadLiturgicalLookups()
        }
        .sheet(item: $selectedReadingSelection) { selection in
            DailyReadingsView(url: selection.url)
        }
        .sheet(isPresented: $showDatePicker) {
            CalendarDatePickerSheet(
                initialDate: selectedDate(),
                onApply: { date in apply(date: date) }
            )
        }
    }

    private func liturgicalTitleForDay() -> String {
        if let rank = liturgicalDayByDay[selectedDay]?.rank {
            return localizedLiturgicalRank(rank, localization: localization)
        }
        return localization.t("calendar.dailyReadings")
    }

    private func liturgicalLabel(for day: Int) -> String {
        if let rank = liturgicalDayByDay[day]?.rank {
            return shortLabel(localizedLiturgicalRank(rank, localization: localization))
        }
        return "📖"
    }

    private func liturgicalReadingURLForDay() -> URL {
        if let raw = liturgicalDayByDay[selectedDay]?.readingURL?.absoluteString,
           let url = localization.language.localizedDailyReadingsURL(from: raw) {
            return url
        }
        return localization.language.dailyReadingsLandingURL
    }

    private func loadLiturgicalLookups() async {
        guard let startDate = makeCalendarDate(year: selectedYear, month: selectedMonth, day: 1),
              let endDate = makeCalendarDate(
                year: selectedYear,
                month: selectedMonth,
                day: daysInMonth(year: selectedYear, month: selectedMonth)
              ) else {
            liturgicalDayByDay = [:]
            return
        }

        do {
            let days = try await environment.contentRepository.listLiturgicalDays(
                startDate: startDate,
                endDate: endDate
            )

            liturgicalDayByDay = days.reduce(into: [:]) { partialResult, day in
                let dayNumber = liturgicalUICalendar.component(.day, from: day.date)
                partialResult[dayNumber] = day
            }
        } catch {
            liturgicalDayByDay = [:]
        }
    }

    private func selectedDate() -> Date {
        let clampedDay = min(selectedDay, daysInMonth(year: selectedYear, month: selectedMonth))
        return makeCalendarDate(year: selectedYear, month: selectedMonth, day: clampedDay) ?? Date()
    }

    private func apply(date: Date) {
        let cal = liturgicalUICalendar
        selectedYear = cal.component(.year, from: date)
        selectedMonth = cal.component(.month, from: date)
        selectedDay = cal.component(.day, from: date)
    }

    private func shift(days: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .day, value: days, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func shift(months: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .month, value: months, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func goPrevious() {
        switch mode {
        case .day: shift(days: -1)
        case .week: shift(days: -7)
        case .month: shift(months: -1)
        }
    }

    private func goNext() {
        switch mode {
        case .day: shift(days: 1)
        case .week: shift(days: 7)
        case .month: shift(months: 1)
        }
    }
}

struct SaintsCalendarView: View {
    let environment: AppEnvironment
    @EnvironmentObject private var localization: LocalizationManager
    @State private var mode: CalendarMode = .day
    @State private var selectedDay = currentLiturgicalDateComponents().day ?? 1
    @State private var selectedMonth = currentLiturgicalDateComponents().month ?? 1
    @State private var selectedYear = currentLiturgicalDateComponents().year ?? 2000
    @State private var showSearch = false
    @State private var selectedSaintSelection: CalendarSelection?
    @State private var tapFeedbackMessage: String?
    @State private var suppressDayTapUntil: Date = .distantPast
    @State private var saintByDay: [Int: Saint] = [:]
    @State private var showDatePicker = false
    @State private var seasonByDay: [Int: LiturgicalSeason] = [:]

    private var displayedSeason: LiturgicalSeason {
        seasonByDay[selectedDay] ?? .ordinary
    }

    private var displayedSeasonBorderColor: Color {
        AppTheme.liturgicalBorderColor(for: displayedSeason)
    }

    private func borderColor(for day: Int) -> Color {
        guard let season = seasonByDay[day] else {
            return displayedSeasonBorderColor
        }
        return AppTheme.liturgicalBorderColor(for: season)
    }

    var body: some View {
        let monthName = localization.monthName(selectedMonth)
        let maxDay = daysInMonth(year: selectedYear, month: selectedMonth)
        CalendarScaffold(
            headerTitle: mode == .day
                ? localization.formatMonthDayYear(month: selectedMonth, day: selectedDay, year: selectedYear)
                : localization.formatMonthYear(month: selectedMonth, year: selectedYear),
            subtitle: localization.t("calendar.subtitle.saints"),
            mode: $mode,
            searchTitle: localization.t("calendar.searchSaints"),
            secondarySearchTitle: nil,
            onSearchTap: { showSearch = true },
            onSecondarySearchTap: nil,
            onPrev: { goPrevious() },
            onNext: { goNext() },
            onToday: {
                let today = currentLiturgicalDateComponents()
                selectedDay = today.day ?? selectedDay
                selectedMonth = today.month ?? selectedMonth
                selectedYear = today.year ?? selectedYear
                mode = .day
            },
            onModeChanged: { suppressDayTapUntil = Date().addingTimeInterval(0.35) },
            onHeaderTap: { showDatePicker = true }
        ) {
            if mode == .day {
                DayCard(
                    title: "\(selectedDay)",
                    subtitle: selectedSaintNameForDay(),
                    imageURL: selectedSaintImageURLForDay(),
                    borderColor: displayedSeasonBorderColor,
                    onTap: {
                        guard Date() >= suppressDayTapUntil else { return }
                        let next = saintIDForSelectedDay()
                        if let next {
                            selectedSaintSelection = CalendarSelection(id: next)
                        } else {
                            tapFeedbackMessage = "\(localization.t("calendar.noSaintMapped")) \(monthName) \(selectedDay)."
                        }
                    }
                )
            } else if mode == .week {
                WeekGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: saintLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            } else {
                MonthGrid(
                    year: selectedYear,
                    month: selectedMonth,
                    daysInMonth: maxDay,
                    selectedDay: selectedDay,
                    borderColorForDay: borderColor(for:),
                    labelForDay: saintLabel(for:)
                ) { day in
                    selectedDay = day
                    mode = .day
                }
            }
        }
        .task(id: "\(selectedYear)-\(selectedMonth)") {
            await loadSeasonLookups()
            await loadSaintLookups()
        }
        .sheet(isPresented: $showSearch) {
            SaintsSearchView(environment: environment)
        }
        .sheet(item: $selectedSaintSelection) { selection in
            SaintDetailView(
                contentRepository: environment.contentRepository,
                saint: saintByDay[selectedDay] ?? Saint(
                    id: selection.id,
                    slug: selection.id,
                    name: localization.t("tab.saints"),
                    nameByLocale: [.en: localization.t("tab.saints")],
                    feastMonth: selectedMonth,
                    feastDay: selectedDay,
                    imageURL: nil,
                    tags: [],
                    patronages: [],
                    feastLabelByLocale: [:],
                    summaryByLocale: [:],
                    biographyByLocale: [:],
                    prayersByLocale: [:],
                    sources: []
                ),
                displayYear: selectedYear,
                onClose: { selectedSaintSelection = nil }
            )
        }
        .alert(localization.t("calendar.noEntry"), isPresented: Binding(
            get: { tapFeedbackMessage != nil },
            set: { if !$0 { tapFeedbackMessage = nil } }
        )) {
            Button(localization.t("calendar.ok"), role: .cancel) { tapFeedbackMessage = nil }
        } message: {
            Text(tapFeedbackMessage ?? "")
        }
        .sheet(isPresented: $showDatePicker) {
            CalendarDatePickerSheet(
                initialDate: selectedDate(),
                onApply: { date in apply(date: date) }
            )
        }
    }

    private func saintIDForSelectedDay() -> String? {
        saintByDay[selectedDay]?.id
    }

    private func selectedSaintNameForDay() -> String {
        saintByDay[selectedDay]?.displayName(locale: localization.language.contentLocale) ?? localization.t("tab.saints")
    }

    private func saintLabel(for day: Int) -> String {
        guard let name = saintByDay[day]?.displayName(locale: localization.language.contentLocale) else {
            return "·"
        }
        return shortLabel(name)
    }

    private func selectedSaintImageURLForDay() -> URL? {
        saintByDay[selectedDay]?.imageURL
    }

    private func loadSaintLookups() async {
        let locale = localization.language.contentLocale
        guard let startDate = makeCalendarDate(year: selectedYear, month: selectedMonth, day: 1),
              let endDate = makeCalendarDate(
            year: selectedYear,
            month: selectedMonth,
            day: daysInMonth(year: selectedYear, month: selectedMonth)
              ) else {
            saintByDay = [:]
            return
        }

        guard let saintRangeRepository = environment.contentRepository as? any SaintRangeRepository else {
            saintByDay = [:]
            return
        }

        do {
            let saints = try await saintRangeRepository.listSaintsInRange(
                locale: locale,
                startDate: startDate,
                endDate: endDate
            )

            saintByDay = saints.reduce(into: [:]) { partialResult, saint in
                partialResult[saint.feastDay] = partialResult[saint.feastDay] ?? saint
            }
        } catch {
            saintByDay = [:]
        }
    }

    private func loadSeasonLookups() async {
        guard let startDate = makeCalendarDate(year: selectedYear, month: selectedMonth, day: 1),
              let endDate = makeCalendarDate(
                year: selectedYear,
                month: selectedMonth,
                day: daysInMonth(year: selectedYear, month: selectedMonth)
              ) else {
            seasonByDay = [:]
            return
        }

        do {
            let days = try await environment.contentRepository.listLiturgicalDays(
                startDate: startDate,
                endDate: endDate
            )

            seasonByDay = days.reduce(into: [:]) { partialResult, day in
                let dayNumber = liturgicalUICalendar.component(.day, from: day.date)
                partialResult[dayNumber] = day.season
            }
        } catch {
            seasonByDay = [:]
        }
    }

    private func selectedDate() -> Date {
        let clampedDay = min(selectedDay, daysInMonth(year: selectedYear, month: selectedMonth))
        return makeCalendarDate(year: selectedYear, month: selectedMonth, day: clampedDay) ?? Date()
    }

    private func apply(date: Date) {
        let cal = liturgicalUICalendar
        selectedYear = cal.component(.year, from: date)
        selectedMonth = cal.component(.month, from: date)
        selectedDay = cal.component(.day, from: date)
    }

    private func shift(days: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .day, value: days, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func shift(months: Int) {
        guard let next = liturgicalUICalendar.date(byAdding: .month, value: months, to: selectedDate()) else { return }
        apply(date: next)
    }

    private func goPrevious() {
        switch mode {
        case .day: shift(days: -1)
        case .week: shift(days: -7)
        case .month: shift(months: -1)
        }
    }

    private func goNext() {
        switch mode {
        case .day: shift(days: 1)
        case .week: shift(days: 7)
        case .month: shift(months: 1)
        }
    }
}

private struct CalendarScaffold<Content: View>: View {
    @EnvironmentObject private var localization: LocalizationManager
    let headerTitle: String
    let subtitle: String
    @Binding var mode: CalendarMode
    let searchTitle: String?
    let secondarySearchTitle: String?
    let onSearchTap: (() -> Void)?
    let onSecondarySearchTap: (() -> Void)?
    let onPrev: () -> Void
    let onNext: () -> Void
    let onToday: () -> Void
    let onModeChanged: () -> Void
    let onHeaderTap: (() -> Void)?
    @ViewBuilder let content: Content

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            let scale = ResponsiveLayout.scale(for: width)
            let contentWidth = max(0, min(width - 24, 760))

            ZStack {
                AppBackdrop()

                VStack(spacing: 10 * scale) {
                    VStack(spacing: 12 * scale) {
                        HStack {
                            Button(action: onPrev) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 17 * scale, weight: .bold))
                                    .foregroundStyle(.white)
                                    .frame(width: 42 * scale, height: 42 * scale)
                                    .background(Color.white.opacity(0.08))
                                    .clipShape(Circle())
                            }
                            Spacer()
                            Button {
                                onHeaderTap?()
                            } label: {
                                HStack(spacing: 6) {
                                    Text(headerTitle)
                                        .font(AppTheme.rounded(27 * scale, weight: .bold))
                                        .foregroundStyle(.white)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.75)
                                    if onHeaderTap != nil {
                                        Image(systemName: "chevron.down")
                                            .font(.system(size: 16 * scale, weight: .semibold))
                                            .foregroundStyle(.white.opacity(0.9))
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            Spacer()
                            Button(action: onNext) {
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 17 * scale, weight: .bold))
                                    .foregroundStyle(.white)
                                    .frame(width: 42 * scale, height: 42 * scale)
                                    .background(Color.white.opacity(0.08))
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.horizontal, 14 * scale)
                        .padding(.top, 10 * scale)

                        Text(subtitle)
                            .font(AppTheme.rounded(14 * scale, weight: .medium))
                            .foregroundStyle(AppTheme.subtitleText)
                    }
                    .padding(.bottom, 12 * scale)
                    .appGlassCard(cornerRadius: 28 * scale)

                    HStack(spacing: 8 * scale) {
                        pillModeButton(localization.t("calendar.today"), isActive: false, action: onToday)
                        Spacer(minLength: 6 * scale)
                        pillModeButton(localization.t("calendar.day"), isActive: mode == .day) {
                            onModeChanged()
                            mode = .day
                        }
                        pillModeButton(localization.t("calendar.week"), isActive: mode == .week) {
                            onModeChanged()
                            mode = .week
                        }
                        pillModeButton(localization.t("calendar.month"), isActive: mode == .month) {
                            onModeChanged()
                            mode = .month
                        }
                    }
                    .padding(.horizontal, 12 * scale)
                    .padding(.vertical, 8 * scale)
                    .appGlassCard(cornerRadius: 26 * scale)

                    content
                        .padding(.horizontal, 12 * scale)

                    Spacer(minLength: 6 * scale)

                    if let searchTitle {
                        Button(searchTitle) { onSearchTap?() }
                            .buttonStyle(PrimaryPillButtonStyle())
                            .padding(.horizontal, 12 * scale)
                    }

                    if let secondarySearchTitle {
                        Button(secondarySearchTitle) { onSecondarySearchTap?() }
                            .buttonStyle(SecondaryPillButtonStyle())
                            .padding(.horizontal, 12 * scale)
                    }

                    HStack(spacing: 12 * scale) {
                        seasonDot(color: AppTheme.advent, text: localization.t("season.advent"))
                        seasonDot(color: AppTheme.christmas, text: localization.t("season.christmas"))
                        seasonDot(color: AppTheme.lent, text: localization.t("season.lent"))
                        seasonDot(color: AppTheme.easter, text: localization.t("season.easter"))
                        seasonDot(color: AppTheme.ordinary, text: localization.t("season.ordinary"))
                    }
                    .font(AppTheme.rounded(11 * scale, weight: .medium))
                    .foregroundStyle(.white.opacity(0.86))
                    .padding(.horizontal, 14 * scale)
                    .padding(.vertical, 12 * scale)
                    .appGlassCard(cornerRadius: 24 * scale)
                }
                .frame(maxWidth: contentWidth)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .padding(.top, 4 * scale)
                .contentShape(Rectangle())
                .simultaneousGesture(
                    DragGesture(minimumDistance: 24)
                        .onEnded { value in
                            guard abs(value.translation.width) > abs(value.translation.height) else { return }
                            if value.translation.width < -40 {
                                onNext()
                            } else if value.translation.width > 40 {
                                onPrev()
                            }
                        }
                )
            }
        }
    }

    private func pillModeButton(_ title: String, isActive: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(AppTheme.rounded(15, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 11)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(isActive ? AnyShapeStyle(AppTheme.primaryButtonGradient) : AnyShapeStyle(AppTheme.cardBackgroundSoft))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(isActive ? Color.white.opacity(0.12) : AppTheme.purpleOutline.opacity(0.45), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.32, dampingFraction: 0.84), value: isActive)
    }

    private func seasonDot(color: Color, text: String) -> some View {
        HStack(spacing: 5) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(text)
        }
    }
}

private struct DayCard: View {
    @EnvironmentObject private var localization: LocalizationManager
    let title: String
    let subtitle: String
    let imageURL: URL?
    var borderColor: Color? = AppTheme.lent
    var actionLabel: String? = nil
    let onTap: () -> Void
    private let cardHeight: CGFloat = 142

    var body: some View {
        let outerShape = RoundedRectangle(cornerRadius: 28, style: .continuous)
        let innerShape = RoundedRectangle(cornerRadius: 22, style: .continuous)
        let borderWidth: CGFloat = borderColor == nil ? 0 : 8
        Button(action: onTap) {
            ZStack {
                outerShape
                    .fill(AppTheme.cardBackground)
                    .overlay {
                        if let imageURL {
                            GeometryReader { geo in
                                AsyncImage(url: imageURL) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .scaledToFill()
                                            .frame(width: geo.size.width, height: geo.size.height)
                                            .blur(radius: 30)
                                            .saturation(0.78)
                                            .opacity(0.5)
                                    case .empty:
                                        Color.clear
                                    case .failure:
                                        Color.clear
                                    @unknown default:
                                        Color.clear
                                    }
                                }
                            }
                            .clipShape(outerShape)
                        }
                    }

                innerShape
                    .fill(AppTheme.cardBackground)
                    .overlay {
                        ZStack {
                            if let imageURL {
                                GeometryReader { geo in
                                    AsyncImage(url: imageURL) { phase in
                                        switch phase {
                                        case .success(let image):
                                            ZStack {
                                                image
                                                    .resizable()
                                                    .scaledToFit()
                                                    .frame(
                                                        width: max(0, geo.size.width - 18),
                                                        height: max(0, geo.size.height - 18)
                                                    )
                                                    .mask(
                                                        LinearGradient(
                                                            stops: [
                                                                .init(color: .clear, location: 0.0),
                                                                .init(color: .white.opacity(0.9), location: 0.18),
                                                                .init(color: .white, location: 0.5),
                                                                .init(color: .white.opacity(0.9), location: 0.82),
                                                                .init(color: .clear, location: 1.0)
                                                            ],
                                                            startPoint: .leading,
                                                            endPoint: .trailing
                                                        )
                                                    )
                                                    .shadow(color: .black.opacity(0.22), radius: 6, x: 0, y: 2)
                                            }
                                        case .empty:
                                            Color.white.opacity(0.08)
                                        case .failure:
                                            Color.white.opacity(0.08)
                                        @unknown default:
                                            Color.white.opacity(0.08)
                                        }
                                    }
                                }
                            }

                            LinearGradient(
                                colors: [Color.black.opacity(0.04), Color.black.opacity(0.24)],
                                startPoint: .top,
                                endPoint: .bottom
                            )

                            VStack(alignment: .leading, spacing: 10) {
                                HStack(alignment: .top) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(title)
                                            .font(AppTheme.rounded(34, weight: .bold))
                                            .foregroundStyle(.white)
                                        Text(subtitle)
                                            .font(AppTheme.rounded(17, weight: .semibold))
                                            .foregroundStyle(.white.opacity(0.92))
                                            .multilineTextAlignment(.leading)
                                            .lineLimit(2)
                                    }

                                    Spacer()

                                    Image(systemName: "arrow.up.right")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundStyle(.white.opacity(0.74))
                                        .padding(10)
                                        .background(Color.white.opacity(0.10))
                                        .clipShape(Circle())
                                }

                                Spacer()

                                Text(actionLabel ?? localization.t("calendar.openDetails"))
                                    .font(AppTheme.rounded(13, weight: .semibold))
                                    .foregroundStyle(.white.opacity(0.92))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(Color.white.opacity(0.12))
                                    .clipShape(Capsule())
                            }
                            .padding(18)
                        }
                        .clipShape(innerShape)
                    }
                    .overlay {
                        innerShape
                            .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
                    }
                    .padding(borderWidth)

                if let borderColor {
                    outerShape
                        .strokeBorder(borderColor.opacity(0.98), lineWidth: 6)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: cardHeight)
            .clipShape(outerShape)
        }
        .contentShape(outerShape)
        .frame(maxWidth: .infinity)
        .frame(height: cardHeight)
        .buttonStyle(.plain)
        .shadow(color: Color.black.opacity(0.2), radius: 18, x: 0, y: 10)
    }
}

private struct MonthGrid: View {
    @EnvironmentObject private var localization: LocalizationManager
    let year: Int
    let month: Int
    let daysInMonth: Int
    let selectedDay: Int
    var borderColorForDay: (Int) -> Color = { _ in AppTheme.lent }
    let labelForDay: (Int) -> String
    let onDayTap: (Int) -> Void

    private enum MonthGridCell: Hashable {
        case empty(Int)
        case day(Date)
    }

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 7)
    private var cells: [MonthGridCell] {
        monthGridDates(year: year, month: month, daysInMonth: daysInMonth)
            .enumerated()
            .map { index, date in
                if let date {
                    return .day(date)
                }
                return .empty(index)
            }
    }
    private var todayComponents: DateComponents {
        currentLiturgicalDateComponents()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            weekHeaderRow

            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(cells, id: \.self) { cell in
                    switch cell {
                    case .empty:
                        Color.clear.frame(height: 72)
                    case .day(let date):
                        let day = liturgicalUICalendar.component(.day, from: date)
                        dayCell(day: day, label: labelForDay(day))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func dayCell(day: Int, label: String) -> some View {
        let isToday = todayComponents.year == year && todayComponents.month == month && todayComponents.day == day
        Button {
            onDayTap(day)
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(AppTheme.cardBackgroundSoft)
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(isToday ? AppTheme.todayHighlight : borderColorForDay(day), lineWidth: isToday ? 3 : 1.6)

                VStack(spacing: 6) {
                    Text("\(day)")
                        .font(AppTheme.rounded(15, weight: .semibold))
                        .foregroundStyle(.white)
                    Text(label)
                        .font(AppTheme.rounded(10, weight: .medium))
                        .foregroundStyle(.white.opacity(0.86))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                .padding(6)
            }
        }
        .frame(height: 72)
        .buttonStyle(.plain)
        .scaleEffect(day == selectedDay ? 1 : 0.985)
        .animation(.spring(response: 0.28, dampingFraction: 0.84), value: selectedDay)
    }

    private var weekHeaderRow: some View {
        HStack {
            ForEach(localization.weekdaySymbolsShort(), id: \.self) { day in
                Text(day)
                    .font(AppTheme.rounded(13, weight: .medium))
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(.white.opacity(0.82))
            }
        }
    }
}

private struct WeekGrid: View {
    @EnvironmentObject private var localization: LocalizationManager
    let year: Int
    let month: Int
    let daysInMonth: Int
    let selectedDay: Int
    var borderColorForDay: (Int) -> Color = { _ in AppTheme.lent }
    let labelForDay: (Int) -> String
    let onDayTap: (Int) -> Void

    private var todayComponents: DateComponents {
        currentLiturgicalDateComponents()
    }
    private var weekDays: [Date?] {
        weekGridDates(year: year, month: month, selectedDay: selectedDay)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                ForEach(localization.weekdaySymbolsShort(), id: \.self) { day in
                    Text(day)
                        .font(AppTheme.rounded(13, weight: .medium))
                        .frame(maxWidth: .infinity)
                        .foregroundStyle(.white.opacity(0.82))
                }
            }

            HStack(spacing: 10) {
                ForEach(Array(weekDays.enumerated()), id: \.offset) { _, maybeDate in
                    if let date = maybeDate {
                        let day = liturgicalUICalendar.component(.day, from: date)
                        let isToday = todayComponents.year == year && todayComponents.month == month && todayComponents.day == day
                        Button {
                            onDayTap(day)
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 20, style: .continuous)
                                    .fill(AppTheme.cardBackgroundSoft)
                                RoundedRectangle(cornerRadius: 20, style: .continuous)
                                    .stroke(isToday ? AppTheme.todayHighlight : borderColorForDay(day), lineWidth: isToday ? 3 : 1.6)

                                VStack(spacing: 6) {
                                    Text("\(day)")
                                        .font(AppTheme.rounded(15, weight: .semibold))
                                        .foregroundStyle(.white)
                                    Text(labelForDay(day))
                                        .font(AppTheme.rounded(10, weight: .medium))
                                        .foregroundStyle(.white.opacity(0.86))
                                        .multilineTextAlignment(.center)
                                        .lineLimit(2)
                                }
                                .padding(6)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 80)
                        .buttonStyle(.plain)
                        .scaleEffect(day == selectedDay ? 1 : 0.985)
                        .animation(.spring(response: 0.28, dampingFraction: 0.84), value: selectedDay)
                    } else {
                        Color.clear
                            .frame(maxWidth: .infinity)
                            .frame(height: 80)
                    }
                }
            }
        }
    }
}

private func monthGridDates(year: Int, month: Int, daysInMonth: Int) -> [Date?] {
    guard let firstDay = makeCalendarDate(year: year, month: month, day: 1) else {
        return []
    }
    let weekday = liturgicalUICalendar.component(.weekday, from: firstDay)
    let leadingEmptyCells = (weekday - liturgicalUICalendar.firstWeekday + 7) % 7

    var cells = Array<Date?>(repeating: nil, count: leadingEmptyCells)
    cells.reserveCapacity(leadingEmptyCells + daysInMonth)

    for day in 1...daysInMonth {
        cells.append(makeCalendarDate(year: year, month: month, day: day))
    }

    return cells
}

private func weekGridDates(year: Int, month: Int, selectedDay: Int) -> [Date?] {
    guard let selectedDate = makeCalendarDate(year: year, month: month, day: selectedDay) else {
        return Array(repeating: nil, count: 7)
    }
    let weekday = liturgicalUICalendar.component(.weekday, from: selectedDate)
    let daysFromWeekStart = (weekday - liturgicalUICalendar.firstWeekday + 7) % 7
    guard let weekStart = liturgicalUICalendar.date(byAdding: .day, value: -daysFromWeekStart, to: selectedDate) else {
        return Array(repeating: nil, count: 7)
    }

    return (0..<7).map { offset in
        guard let date = liturgicalUICalendar.date(byAdding: .day, value: offset, to: weekStart) else {
            return nil
        }

        let components = liturgicalUICalendar.dateComponents([.year, .month], from: date)
        guard components.year == year, components.month == month else {
            return nil
        }
        return date
    }
}

private func makeCalendarDate(year: Int, month: Int, day: Int) -> Date? {
    var components = DateComponents()
    components.year = year
    components.month = month
    components.day = day
    components.hour = 12
    return liturgicalUICalendar.date(from: components)
}

private struct CalendarSelection: Identifiable {
    let id: String
}

private func shortLabel(_ raw: String, max: Int = 14) -> String {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard trimmed.count > max else { return trimmed }
    return String(trimmed.prefix(max - 1)) + "…"
}

private func daysInMonth(year: Int, month: Int) -> Int {
    var components = DateComponents()
    components.year = year
    components.month = month
    guard let date = liturgicalUICalendar.date(from: components),
          let range = liturgicalUICalendar.range(of: .day, in: .month, for: date)
    else {
        return 31
    }
    return range.count
}

private struct ReadingSelection: Identifiable {
    let id = UUID()
    let url: URL
}

@MainActor
private func localizedLiturgicalRank(_ raw: String, localization: LocalizationManager) -> String {
    switch localization.language {
    case .en:
        return raw
    case .es:
        return raw
            .replacingOccurrences(of: "Sunday", with: "Domingo")
            .replacingOccurrences(of: "Monday", with: "Lunes")
            .replacingOccurrences(of: "Tuesday", with: "Martes")
            .replacingOccurrences(of: "Wednesday", with: "Miércoles")
            .replacingOccurrences(of: "Thursday", with: "Jueves")
            .replacingOccurrences(of: "Friday", with: "Viernes")
            .replacingOccurrences(of: "Saturday", with: "Sábado")
            .replacingOccurrences(of: " of Lent", with: " de Cuaresma")
            .replacingOccurrences(of: " of Advent", with: " de Adviento")
            .replacingOccurrences(of: " of Easter", with: " de Pascua")
            .replacingOccurrences(of: " of Christmas", with: " de Navidad")
            .replacingOccurrences(of: " of Ordinary Time", with: " del Tiempo Ordinario")
    case .pl:
        return raw
            .replacingOccurrences(of: "Sunday", with: "Niedziela")
            .replacingOccurrences(of: "Monday", with: "Poniedziałek")
            .replacingOccurrences(of: "Tuesday", with: "Wtorek")
            .replacingOccurrences(of: "Wednesday", with: "Środa")
            .replacingOccurrences(of: "Thursday", with: "Czwartek")
            .replacingOccurrences(of: "Friday", with: "Piątek")
            .replacingOccurrences(of: "Saturday", with: "Sobota")
            .replacingOccurrences(of: " of Lent", with: " Wielkiego Postu")
            .replacingOccurrences(of: " of Advent", with: " Adwentu")
            .replacingOccurrences(of: " of Easter", with: " Okresu Wielkanocnego")
            .replacingOccurrences(of: " of Christmas", with: " Okresu Bożego Narodzenia")
            .replacingOccurrences(of: " of Ordinary Time", with: " Okresu Zwykłego")
    }
}

private struct CalendarDatePickerSheet: View {
    @EnvironmentObject private var localization: LocalizationManager
    let initialDate: Date
    let onApply: (Date) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var selectedDate: Date

    init(initialDate: Date, onApply: @escaping (Date) -> Void) {
        self.initialDate = initialDate
        self.onApply = onApply
        _selectedDate = State(initialValue: initialDate)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                DatePicker(
                    localization.t("common.pickDateLabel"),
                    selection: $selectedDate,
                    displayedComponents: [.date]
                )
                .datePickerStyle(.wheel)
                .padding(.horizontal, 12)
                Spacer()
            }
            .padding(.top, 8)
            .navigationTitle(localization.t("calendar.pickDate"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(localization.t("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(localization.t("common.apply")) {
                        onApply(selectedDate)
                        dismiss()
                    }
                }
            }
        }
        .environment(\.locale, localization.language.locale)
        .presentationDetents([.medium, .large])
    }
}

struct DailyReadingsView: View {
    @EnvironmentObject private var localization: LocalizationManager
    let url: URL
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            InAppSafariView(url: url)
                .ignoresSafeArea()
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(localization.t("common.close")) { dismiss() }
                    }
                }
        }
    }
}

struct InAppSafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.dismissButtonStyle = .close
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

struct CalendarViews_Previews: PreviewProvider {
    static var previews: some View {
        TabView {
            NovenasCalendarView(environment: .local()).tabItem { Text("Novenas") }
            LiturgicalCalendarView(environment: .local()).tabItem { Text("Liturgical") }
            SaintsCalendarView(environment: .local()).tabItem { Text("Saints") }
        }
    }
}
