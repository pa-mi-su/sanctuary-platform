import SwiftUI
import SafariServices
import WebKit

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
                    showsAction: novenaIDForSelectedDay() != nil,
                    onTap: {
                        guard Date() >= suppressDayTapUntil else { return }
                        guard let next = novenaIDForSelectedDay() else { return }
                        selectedNovenaSelection = CalendarSelection(id: next)
                    }
                )
            }
        }
        .task(id: "\(selectedYear)-\(selectedMonth)") {
            await loadSeasonLookups()
            await loadNovenaLookups()
        }
        .fullScreenCover(isPresented: $showSearch) {
            NovenasSearchView(environment: environment)
        }
        .fullScreenCover(item: $selectedNovenaSelection) { selection in
            NovenaDetailView(
                contentRepository: environment.contentRepository,
                novena: Novena(
                    id: selection.id,
                    slug: selection.id,
                    titleByLocale: [.en: novenaTitleByDay[selectedDay] ?? selection.id],
                    descriptionByLocale: [.en: ""],
                    durationDays: 1,
                    tags: [],
                    intentions: [],
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
            return localization.t("calendar.noNovenaStarting")
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
                let featured = entry.startingNovena
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
        .fullScreenCover(item: $selectedReadingSelection) { selection in
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
        .fullScreenCover(isPresented: $showSearch) {
            SaintsSearchView(environment: environment)
        }
        .fullScreenCover(item: $selectedSaintSelection) { selection in
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
                    intentions: [],
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
            ZStack {
                AppBackdrop()
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        HStack(spacing: 8) {
                            dateArrow("chevron.left", action: onPrev)
                            Button { onHeaderTap?() } label: {
                                VStack(spacing: 6) {
                                    Text(subtitle.components(separatedBy: "•").first?.trimmingCharacters(in: .whitespaces) ?? subtitle)
                                        .font(.subheadline.weight(.medium)).foregroundStyle(AppTheme.subtitleText)
                                    HStack(spacing: 6) {
                                        Text(headerTitle).font(.title3.weight(.bold)).lineLimit(1).minimumScaleFactor(0.7)
                                        Image(systemName: "chevron.down").font(.caption.weight(.semibold))
                                    }
                                    .foregroundStyle(AppTheme.cardText)
                                }
                                .frame(maxWidth: .infinity)
                            }.buttonStyle(.plain)
                            dateArrow("chevron.right", action: onNext)
                        }
                        HStack(spacing: 12) {
                            Button(localization.t("calendar.today"), action: onToday)
                                .font(.subheadline.weight(.semibold)).tint(AppTheme.glowBlue)
                                .frame(minHeight: 44)
                            Picker("", selection: $mode) {
                                Text(localization.t("calendar.day")).tag(CalendarMode.day)
                                Text(localization.t("calendar.week")).tag(CalendarMode.week)
                                Text(localization.t("calendar.month")).tag(CalendarMode.month)
                            }
                            .pickerStyle(.segmented)
                            .onChange(of: mode) { _ in onModeChanged() }
                        }
                        if let searchTitle {
                            CalendarSearchActionButton(title: searchTitle, systemImage: "magnifyingglass", tint: AppTheme.glowBlue, scale: 1) { onSearchTap?() }
                        }
                        if let secondarySearchTitle {
                            CalendarSearchActionButton(title: secondarySearchTitle, systemImage: "magnifyingglass", tint: AppTheme.glowGold, scale: 1) { onSecondarySearchTap?() }
                        }
                        content
                        ViewThatFits(in: .horizontal) {
                            HStack(spacing: 12) { legendItems }
                            VStack(alignment: .leading, spacing: 8) { legendItems }
                        }
                        .font(.caption).foregroundStyle(AppTheme.subtitleText)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    }
                    .padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 32)
                    .frame(maxWidth: 760)
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func dateArrow(_ icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon).font(.body.weight(.semibold))
                .foregroundStyle(AppTheme.cardText)
                .frame(width: 44, height: 44).background(AppTheme.cardBackground, in: Circle())
        }.buttonStyle(.plain)
    }

    @ViewBuilder private var legendItems: some View {
        seasonDot(color: AppTheme.advent, text: localization.t("season.advent"))
        seasonDot(color: AppTheme.christmas, text: localization.t("season.christmas"))
        seasonDot(color: AppTheme.lent, text: localization.t("season.lent"))
        seasonDot(color: AppTheme.easter, text: localization.t("season.easter"))
        seasonDot(color: AppTheme.ordinary, text: localization.t("season.ordinary"))
    }
    private func seasonDot(color: Color, text: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).overlay(Circle().stroke(AppTheme.homeInk.opacity(0.15), lineWidth: 0.5)).frame(width: 7, height: 7)
            Text(text)
        }
    }
}

private struct CalendarSearchActionButton: View {
    let title: String
    let systemImage: String
    let tint: Color
    let scale: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10 * scale) {
                Image(systemName: systemImage)
                    .font(.system(size: 16 * scale, weight: .bold))
                    .foregroundStyle(AppTheme.cardText)
                    .frame(width: 34 * scale, height: 34 * scale)
                    .background(
                        Circle()
                            .fill(Color.white.opacity(0.14))
                            .overlay(Circle().stroke(AppTheme.cardText.opacity(0.1), lineWidth: 1))
                    )
                    .clipShape(Circle())

                Text(title)
                    .font(AppTheme.rounded(15 * scale, weight: .bold))
                    .foregroundStyle(AppTheme.cardText)
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12 * scale, weight: .bold))
                    .foregroundStyle(AppTheme.subtitleText)
            }
            .padding(.horizontal, 16 * scale)
            .frame(maxWidth: .infinity)
            .frame(height: 54 * scale)
            .background(
                LinearGradient(
                    colors: [
                        AppTheme.cardBackgroundSoft,
                        AppTheme.cardBackgroundSoft,
                        AppTheme.cardBackgroundSoft
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .overlay(
                LinearGradient(
                    colors: [Color.white.opacity(0.18), Color.clear],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .clipShape(RoundedRectangle(cornerRadius: 22 * scale, style: .continuous))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22 * scale, style: .continuous)
                    .stroke(AppTheme.cardText.opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22 * scale, style: .continuous))
            .shadow(color: tint.opacity(0.06), radius: 8 * scale, x: 0, y: 4 * scale)
            .shadow(color: Color.black.opacity(0.06), radius: 8 * scale, x: 0, y: 4 * scale)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }
}

private struct DayCard: View {
    @EnvironmentObject private var localization: LocalizationManager
    let title: String
    let subtitle: String
    let imageURL: URL?
    var borderColor: Color? = AppTheme.lent
    var actionLabel: String? = nil
    var showsAction = true
    let onTap: () -> Void
    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .center, spacing: 16) {
                if let imageURL {
                    AsyncImage(url: imageURL) { image in image.resizable().scaledToFill() }
                        placeholder: { AppTheme.cardBackgroundSoft }
                        .frame(width: 84, height: 112).clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 8) {
                        Text(title).font(.system(.largeTitle, design: .serif, weight: .semibold))
                        if let borderColor { Circle().fill(borderColor).frame(width: 7, height: 7) }
                    }
                    Text(subtitle).font(.headline).fixedSize(horizontal: false, vertical: true)
                    if showsAction {
                        Text(actionLabel ?? localization.t("calendar.openDetails"))
                            .font(.subheadline).foregroundStyle(AppTheme.glowBlue)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if showsAction { Image(systemName: "chevron.right").foregroundStyle(AppTheme.subtitleText) }
            }
            .foregroundStyle(AppTheme.cardText)
            .padding(18).frame(maxWidth: .infinity, minHeight: 144, alignment: .leading)
            .background(AppTheme.cardBackground, in: RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain).disabled(!showsAction)
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

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
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

            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(cells, id: \.self) { cell in
                    switch cell {
                    case .empty:
                        Color.clear.frame(height: 60)
                    case .day(let date):
                        let day = liturgicalUICalendar.component(.day, from: date)
                        dayCell(day: day, label: labelForDay(day))
                    }
                }
            }
        }
    }

    private func dayCell(day: Int, label: String) -> some View {
        CalendarDateCell(day: day, selected: day == selectedDay,
            isToday: todayComponents.year == year && todayComponents.month == month && todayComponents.day == day,
            season: borderColorForDay(day), label: label) { onDayTap(day) }
    }

    private var weekHeaderRow: some View {
        HStack {
            ForEach(localization.weekdaySymbolsShort(), id: \.self) { day in
                Text(day)
                    .font(AppTheme.rounded(13, weight: .medium))
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(AppTheme.subtitleText)
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
                        .foregroundStyle(AppTheme.subtitleText)
                }
            }

            HStack(spacing: 4) {
                ForEach(Array(weekDays.enumerated()), id: \.offset) { _, maybeDate in
                    if let date = maybeDate {
                        let day = liturgicalUICalendar.component(.day, from: date)
                        let isToday = todayComponents.year == year && todayComponents.month == month && todayComponents.day == day
                        CalendarDateCell(day: day, selected: day == selectedDay, isToday: isToday,
                            season: borderColorForDay(day), label: labelForDay(day)) { onDayTap(day) }
                            .frame(maxWidth: .infinity)
                    } else {
                        Color.clear
                            .frame(maxWidth: .infinity)
                            .frame(height: 60)
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

private struct CalendarDateCell: View {
    let day: Int
    let selected: Bool
    let isToday: Bool
    let season: Color
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text("\(day)").font(.body.weight(selected ? .bold : .medium))
                    .foregroundStyle(selected ? AppTheme.onAccent : AppTheme.cardText)
                Circle().fill(season).overlay(Circle().stroke(AppTheme.homeInk.opacity(0.15), lineWidth: 0.5))
                    .frame(width: 5, height: 5)
            }
            .frame(maxWidth: .infinity).frame(height: 60)
            .background(selected ? AppTheme.glowBlue : AppTheme.homeSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(isToday && !selected ? AppTheme.glowBlue : AppTheme.purpleOutline, lineWidth: 1))
        }.buttonStyle(.plain)
            .accessibilityLabel("\(day), \(label)")
            .accessibilityAddTraits(selected ? .isSelected : [])
    }
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
    let url: URL
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @State private var failed = false
    @State private var reloadID = UUID()

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.homeSurface.ignoresSafeArea()
                if failed {
                    VStack(spacing: 16) {
                        Text(localization.t("common.error")).foregroundStyle(AppTheme.errorText)
                        Button(localization.t("common.retry")) { failed = false; reloadID = UUID() }
                            .buttonStyle(PrimaryPillButtonStyle())
                    }.padding()
                } else {
                    ThemedReadingsWebView(url: url, onFailure: { failed = true }).id(reloadID)
                }
            }
            .navigationTitle(localization.t("calendar.dailyReadingsTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(AppTheme.homeSurface, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { dismiss() } label: { Image(systemName: "xmark") }
                        .accessibilityLabel(localization.t("common.close"))
                }
            }
            .tint(AppTheme.homeInk)
        }.preferredColorScheme(.light)
    }
}

private struct ThemedReadingsWebView: UIViewControllerRepresentable {
    let url: URL
    let onFailure: () -> Void
    func makeCoordinator() -> Coordinator { Coordinator(onFailure: onFailure) }
    func makeUIViewController(context: Context) -> UIViewController {
        let configuration = WKWebViewConfiguration()
        if let path = Bundle.main.url(forResource: "readings-theme", withExtension: "js"),
           let script = try? String(contentsOf: path, encoding: .utf8) {
            configuration.userContentController.addUserScript(WKUserScript(source: script, injectionTime: .atDocumentEnd, forMainFrameOnly: true))
        }
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.isOpaque = false
        webView.backgroundColor = UIColor(AppTheme.homeSurface)
        webView.scrollView.backgroundColor = UIColor(AppTheme.homeSurface)
        webView.load(URLRequest(url: url))
        let controller = UIViewController()
        controller.view = webView
        return controller
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    final class Coordinator: NSObject, WKNavigationDelegate {
        let onFailure: () -> Void
        init(onFailure: @escaping () -> Void) { self.onFailure = onFailure }
        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            if (error as NSError).code != NSURLErrorCancelled { onFailure() }
        }
        func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = action.request.url else { decisionHandler(.cancel); return }
            if ["https", "http", "about"].contains(url.scheme ?? "") { decisionHandler(.allow) }
            else { UIApplication.shared.open(url); decisionHandler(.cancel) }
        }
    }
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
