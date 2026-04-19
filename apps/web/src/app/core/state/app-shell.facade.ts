import { computed, inject, Injectable, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, of, switchMap } from 'rxjs';

import {
  LiturgicalDayResponse,
  NovenaCalendarDateResponse,
  NovenaDetail,
  NovenaSummary,
  PrayerDetail,
  PrayerSummary,
  SaintDateGroup,
  SaintDetail,
  SaintSummary,
  SanctuaryApiService,
} from '../api/sanctuary-api.service';

export type AppTab = 'home' | 'novenas' | 'liturgical' | 'saints' | 'prayers' | 'me';
export type CalendarView = 'day' | 'week' | 'month';
export type SeasonKey = 'ADVENT' | 'CHRISTMAS' | 'LENT' | 'EASTER' | 'ORDINARY';
export type CalendarCell = { date: string | null; dayNumber: number | null; label: string; seasonKey?: SeasonKey | null };
export type SaintsMode = 'calendar' | 'list';
export type NovenasMode = 'calendar' | 'list' | 'intentions';

@Injectable({ providedIn: 'root' })
export class AppShellFacade {
  private readonly api = inject(SanctuaryApiService);
  private readonly todayDateValue = this.formatDateForApi(new Date());

  readonly currentTab = signal<AppTab>('home');
  readonly liturgicalView = signal<CalendarView>('month');
  readonly saintsView = signal<CalendarView>('day');
  readonly novenasView = signal<CalendarView>('day');
  readonly saintsMode = signal<SaintsMode>('calendar');
  readonly novenasMode = signal<NovenasMode>('calendar');
  readonly showAbout = signal(false);
  readonly language = signal<'en' | 'es'>('en');
  readonly selectedDate = signal(this.formatDateForApi(new Date()));
  readonly saintQuery = signal('');
  readonly prayerQuery = signal('');
  readonly novenaQuery = signal('');
  readonly selectedSaintSlug = signal<string | null>(null);
  readonly selectedPrayerSlug = signal<string | null>(null);
  readonly selectedNovenaSlug = signal<string | null>(null);
  readonly selectedNovenaDayNumber = signal(1);

  readonly liturgicalLoadFailed = signal(false);
  readonly saintsLoadFailed = signal(false);
  readonly novenasLoadFailed = signal(false);
  readonly prayersLoadFailed = signal(false);

  readonly liturgicalRange = toSignal(
    combineLatest([
      toObservable(this.selectedDate),
      toObservable(this.currentTab),
      toObservable(this.liturgicalView),
      toObservable(this.saintsView),
      toObservable(this.novenasView),
    ]).pipe(
      switchMap(([date, currentTab, liturgicalView, saintsView, novenasView]) => {
        const view =
          currentTab === 'liturgical'
            ? liturgicalView
            : currentTab === 'saints'
              ? saintsView
              : currentTab === 'novenas'
                ? novenasView
                : 'day';
        const range = this.getDateRange(date, view);
        return this.api.getLiturgicalRange(range.start, range.end).pipe(
          catchError(() => {
            this.liturgicalLoadFailed.set(true);
            return of<LiturgicalDayResponse[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly saintsRange = toSignal(
    combineLatest([toObservable(this.selectedDate), toObservable(this.saintsView), toObservable(this.language)]).pipe(
      switchMap(([date, view, language]) => {
        const range = this.getDateRange(date, view);
        return this.api.getSaintsByRange(range.start, range.end, this.apiLanguage(language)).pipe(
          catchError(() => {
            this.saintsLoadFailed.set(true);
            return of<SaintDateGroup[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly novenaCalendarRange = toSignal(
    combineLatest([toObservable(this.selectedDate), toObservable(this.novenasView), toObservable(this.language)]).pipe(
      switchMap(([date, view, language]) => {
        const range = this.getDateRange(date, view);
        return this.api.getNovenasByRange(range.start, range.end, this.apiLanguage(language)).pipe(
          catchError(() => {
            this.novenasLoadFailed.set(true);
            return of<NovenaCalendarDateResponse[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly prayerResults = toSignal(
    combineLatest([toObservable(this.prayerQuery), toObservable(this.language)]).pipe(
      switchMap(([query, language]) =>
        this.api.listPrayers(this.apiLanguage(language), query).pipe(
          catchError(() => {
            this.prayersLoadFailed.set(true);
            return of<PrayerSummary[]>([]);
          }),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly saintResults = toSignal(
    combineLatest([toObservable(this.saintQuery), toObservable(this.language)]).pipe(
      switchMap(([query, language]) =>
        this.api.listSaints(this.apiLanguage(language), query).pipe(
          catchError(() => {
            this.saintsLoadFailed.set(true);
            return of<SaintSummary[]>([]);
          }),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly saintDetail = toSignal(
    combineLatest([toObservable(this.selectedSaintSlug), toObservable(this.language)]).pipe(
      switchMap(([slug, language]) => {
        if (!slug) {
          return of<SaintDetail | null>(null);
        }

        return this.api.getSaintDetail(slug, this.apiLanguage(language)).pipe(
          catchError(() => of<SaintDetail | null>(null)),
        );
      }),
    ),
    { initialValue: null },
  );

  readonly prayerDetail = toSignal(
    combineLatest([toObservable(this.selectedPrayerSlug), toObservable(this.language)]).pipe(
      switchMap(([slug, language]) => {
        if (!slug) {
          return of<PrayerDetail | null>(null);
        }

        return this.api.getPrayerDetail(slug, this.apiLanguage(language)).pipe(
          catchError(() => of<PrayerDetail | null>(null)),
        );
      }),
    ),
    { initialValue: null },
  );

  readonly novenaDetail = toSignal(
    combineLatest([toObservable(this.selectedNovenaSlug), toObservable(this.language)]).pipe(
      switchMap(([slug, language]) => {
        if (!slug) {
          return of<NovenaDetail | null>(null);
        }

        return this.api.getNovenaDetail(slug, this.apiLanguage(language)).pipe(
          catchError(() => of<NovenaDetail | null>(null)),
        );
      }),
    ),
    { initialValue: null },
  );

  readonly todayLiturgicalDay = toSignal(
    this.api.getLiturgicalDay(this.todayDateValue).pipe(catchError(() => of<LiturgicalDayResponse | null>(null))),
    { initialValue: null },
  );

  readonly todaySaintGroup = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.getSaintsByDate(this.todayDateValue, this.apiLanguage(language)).pipe(
          switchMap((saints) => of<SaintDateGroup | null>({ date: this.todayDateValue, saints })),
          catchError(() => of<SaintDateGroup | null>(null)),
        ),
      ),
    ),
    { initialValue: null },
  );

  readonly todayNovenasGroup = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.getNovenasByRange(this.todayDateValue, this.todayDateValue, this.apiLanguage(language)).pipe(
          switchMap((days) => of<NovenaCalendarDateResponse | null>(days[0] ?? { date: this.todayDateValue, novenas: [], startingNovena: null })),
          catchError(() => of<NovenaCalendarDateResponse | null>(null)),
        ),
      ),
    ),
    { initialValue: null },
  );

  readonly selectedDateLabel = computed(() =>
    new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(this.parseSelectedDate()),
  );

  readonly liturgicalByDate = computed(() => this.toMap(this.liturgicalRange(), (entry) => entry.date));
  readonly saintsByDate = computed(() => this.toMap(this.saintsRange(), (entry) => entry.date));
  readonly novenasByDate = computed(() => this.toMap(this.novenaCalendarRange(), (entry) => entry.date));

  readonly selectedLiturgicalDay = computed(() => this.liturgicalByDate().get(this.selectedDate()) ?? null);
  readonly selectedSaintGroup = computed(() => this.saintsByDate().get(this.selectedDate()) ?? null);
  readonly selectedNovenas = computed(() => this.novenasByDate().get(this.selectedDate())?.novenas ?? []);

  readonly selectedSaintHeadline = computed(() => this.selectedSaintGroup()?.saints[0] ?? null);
  readonly selectedNovenaHeadline = computed(() => this.novenasByDate().get(this.selectedDate())?.startingNovena ?? null);
  readonly selectedNovenaDay = computed(() => {
    const detail = this.novenaDetail();
    if (!detail || detail.days.length === 0) {
      return null;
    }

    return detail.days.find((day) => day.dayNumber === this.selectedNovenaDayNumber()) ?? detail.days[0];
  });

  readonly liturgicalCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.liturgicalView()));
  readonly saintsCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.saintsView()));
  readonly novenaCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.novenasView()));

  readonly seasonLegend = computed(() => [
    { key: 'ADVENT' as const, label: this.isEnglish() ? 'Advent' : 'Adviento' },
    { key: 'CHRISTMAS' as const, label: this.isEnglish() ? 'Christmas' : 'Navidad' },
    { key: 'LENT' as const, label: this.isEnglish() ? 'Lent' : 'Cuaresma' },
    { key: 'EASTER' as const, label: this.isEnglish() ? 'Easter' : 'Pascua' },
    { key: 'ORDINARY' as const, label: this.isEnglish() ? 'Ordinary Time' : 'Tiempo Ordinario' },
  ]);

  readonly selectedDateSeasonLabel = computed(() => {
    const liturgicalDay = this.selectedLiturgicalDay();
    if (!liturgicalDay) {
      return this.isEnglish() ? 'Loading liturgical day...' : 'Cargando día litúrgico...';
    }

    return this.toTitleCase(liturgicalDay.season.replaceAll('_', ' '));
  });

  readonly selectedDateDayNumber = computed(() => this.parseSelectedDate().getDate());

  readonly novenaSearchResults = toSignal(
    combineLatest([toObservable(this.novenaQuery), toObservable(this.language), toObservable(this.novenasMode)]).pipe(
      switchMap(([query, language, mode]) => {
        const request = mode === 'intentions'
          ? this.api.listNovenaIntentions(this.apiLanguage(language), query)
          : this.api.listNovenas(this.apiLanguage(language), query);

        return request.pipe(
          catchError(() => {
            this.novenasLoadFailed.set(true);
            return of<NovenaSummary[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  setTab(tab: AppTab): void {
    this.currentTab.set(tab);
  }

  handlePrimaryNavigate(tab: AppTab): void {
    if (tab === 'novenas') {
      this.browseNovenas();
      return;
    }

    if (tab === 'saints') {
      this.browseSaintsCalendar();
      return;
    }

    this.setTab(tab);
  }

  openAbout(): void {
    this.showAbout.set(true);
  }

  closeAbout(): void {
    this.showAbout.set(false);
  }

  toggleLanguage(): void {
    this.language.update((current) => (current === 'en' ? 'es' : 'en'));
    this.clearErrors();
  }

  isEnglish(): boolean {
    return this.language() === 'en';
  }

  setLiturgicalView(view: CalendarView): void {
    this.liturgicalView.set(view);
    this.liturgicalLoadFailed.set(false);
  }

  setSaintsView(view: CalendarView): void {
    this.saintsView.set(view);
    this.saintsLoadFailed.set(false);
  }

  setNovenasView(view: CalendarView): void {
    this.novenasView.set(view);
    this.novenasLoadFailed.set(false);
  }

  shiftLiturgicalDate(direction: -1 | 1): void {
    this.shiftDateByView(this.liturgicalView(), direction);
  }

  shiftSaintsDate(direction: -1 | 1): void {
    this.shiftDateByView(this.saintsView(), direction);
  }

  shiftNovenasDate(direction: -1 | 1): void {
    this.shiftDateByView(this.novenasView(), direction);
  }

  resetSelectedDate(): void {
    this.clearErrors();
    this.selectedDate.set(this.formatDateForApi(new Date()));
  }

  todayDate(): string {
    return this.todayDateValue;
  }

  selectedSaintImageStyle(): string | null {
    const imageUrl = this.selectedSaintHeadline()?.imageUrl;
    return imageUrl ? `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})` : null;
  }

  selectedNovenaImageStyle(): string | null {
    const imageUrl = this.selectedNovenaHeadline()?.imageUrl;
    return imageUrl ? `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})` : null;
  }

  cardImageStyle(imageUrl: string | null | undefined): string | null {
    return imageUrl ? `linear-gradient(180deg, rgba(6, 12, 18, 0.05), rgba(6, 12, 18, 0.28)), url(${imageUrl})` : null;
  }

  updatePrayerQuery(value: string): void {
    this.prayerQuery.set(value);
    this.prayersLoadFailed.set(false);
  }

  updateSaintQuery(value: string): void {
    this.saintQuery.set(value);
    this.saintsLoadFailed.set(false);
  }

  updateNovenaQuery(value: string): void {
    this.novenaQuery.set(value);
    this.novenasLoadFailed.set(false);
  }

  selectDateFromGrid(date: string): void {
    this.clearErrors();
    this.selectedDate.set(date);
  }

  openSaintDetail(saint: SaintSummary): void {
    this.selectedSaintSlug.set(saint.slug);
  }

  openPrayerDetail(prayer: PrayerSummary): void {
    this.selectedPrayerSlug.set(prayer.slug);
  }

  openNovenaDetail(novena: NovenaSummary): void {
    this.selectedNovenaSlug.set(novena.slug);
    this.selectedNovenaDayNumber.set(1);
  }

  selectNovenaDay(dayNumber: number): void {
    this.selectedNovenaDayNumber.set(dayNumber);
  }

  closeDetailModal(): void {
    this.selectedSaintSlug.set(null);
    this.selectedPrayerSlug.set(null);
    this.selectedNovenaSlug.set(null);
    this.selectedNovenaDayNumber.set(1);
  }

  showDailyTab(): void {
    this.setTab('liturgical');
  }

  openIntentions(): void {
    this.setTab('novenas');
    this.novenasMode.set('intentions');
    this.novenaQuery.set('');
    this.novenasLoadFailed.set(false);
  }

  browseNovenas(): void {
    this.setTab('novenas');
    this.novenasMode.set('calendar');
    this.novenasLoadFailed.set(false);
  }

  openNovenasList(): void {
    this.setTab('novenas');
    this.novenasMode.set('list');
    this.novenaQuery.set('');
  }

  browseSaintsCalendar(): void {
    this.setTab('saints');
    this.saintsMode.set('calendar');
  }

  openSaintsList(): void {
    this.setTab('saints');
    this.saintsMode.set('list');
    this.saintQuery.set('');
  }

  localizedSaintsCountLabel(): string {
    const count = this.selectedSaintGroup()?.saints.length ?? 0;
    return this.isEnglish() ? `Selected day · ${count} saints` : `Día seleccionado · ${count} santos`;
  }

  localizedNovenasCountLabel(): string {
    const count = this.selectedNovenas().length;
    return this.isEnglish()
      ? `Selected day · ${count} active novenas`
      : `Día seleccionado · ${count} novenas activas`;
  }

  localizedNoSaintsCopy(): string {
    return this.isEnglish()
      ? 'No saints are assigned to this feast day in the imported legacy data.'
      : 'No hay santos asignados a este día de fiesta en los datos heredados importados.';
  }

  localizedNoNovenasCopy(): string {
    return this.isEnglish()
      ? 'No novenas are active for this date.'
      : 'No hay novenas activas para esta fecha.';
  }

  localizedNovenaSearchPlaceholder(): string {
    return this.novenasMode() === 'intentions'
      ? this.isEnglish() ? 'Search intentions' : 'Buscar intenciones'
      : this.isEnglish() ? 'Search novenas' : 'Buscar novenas';
  }

  localizedSaintResultsLabel(): string {
    return this.isEnglish() ? `${this.saintResults().length} saints` : `${this.saintResults().length} santos`;
  }

  localizedPrayerResultsLabel(): string {
    return this.isEnglish() ? `${this.prayerResults().length} prayers` : `${this.prayerResults().length} oraciones`;
  }

  localizedIntentionsResultsLabel(): string {
    if (this.novenasMode() === 'list') {
      return `${this.novenaSearchResults().length} novenas`;
    }

    return this.isEnglish()
      ? `${this.novenaSearchResults().length} novenas with intentions`
      : `${this.novenaSearchResults().length} novenas con intenciones`;
  }

  localizedPreviewTitle(mode: 'today' | 'selected'): string {
    return mode === 'today'
      ? this.isEnglish() ? 'Today' : 'Hoy'
      : this.isEnglish() ? 'Selected Day' : 'Día seleccionado';
  }

  localizedNoLiturgicalCopy(): string {
    return this.isEnglish()
      ? 'No liturgical summary is available for this day.'
      : 'No hay un resumen litúrgico disponible para este día.';
  }

  localizedSelectedSameAsTodayCopy(): string {
    return this.isEnglish()
      ? 'Selected day matches today.'
      : 'El día seleccionado coincide con hoy.';
  }

  localizedIntentionsEmptyCopy(): string {
    if (this.novenasMode() === 'list') {
      return this.isEnglish()
        ? 'Browse the novena library or search for a specific novena.'
        : 'Explora la biblioteca de novenas o busca una novena específica.';
    }

    return this.isEnglish()
      ? 'Browse the available intention novenas or search for a specific intention.'
      : 'Revisa las novenas con intenciones disponibles o busca una intención específica.';
  }

  localizedApiErrorCopy(subject: 'saints' | 'liturgical' | 'novenas' | 'prayers'): string {
    if (this.isEnglish()) {
      switch (subject) {
        case 'saints':
          return 'We could not load saints from the API right now.';
        case 'liturgical':
          return 'We could not load the liturgical day from the API right now.';
        case 'novenas':
          return 'We could not load novenas from the API right now.';
        case 'prayers':
          return 'We could not load prayers from the API right now.';
      }
    }

    switch (subject) {
      case 'saints':
        return 'No pudimos cargar los santos desde la API en este momento.';
      case 'liturgical':
        return 'No pudimos cargar el día litúrgico desde la API en este momento.';
      case 'novenas':
        return 'No pudimos cargar las novenas desde la API en este momento.';
      case 'prayers':
        return 'No pudimos cargar las oraciones desde la API en este momento.';
    }
  }

  shortSaintLabel(date: string): string {
    const saint = this.saintsByDate().get(date)?.saints[0];
    return saint ? this.truncateLabel(saint.name, 16) : '—';
  }

  shortLiturgicalLabel(date: string): string {
    const day = this.liturgicalByDate().get(date);
    return day ? this.truncateLabel(day.primaryRank, 18) : '—';
  }

  shortNovenaLabel(date: string): string {
    const novena = this.novenasByDate().get(date)?.startingNovena ?? null;
    return novena ? this.truncateLabel(novena.title, 16) : '—';
  }

  previewDateLabel(date: string): string {
    return new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(this.parseDate(date));
  }

  previewNovenas(date: string): NovenaSummary[] {
    return date === this.todayDateValue
      ? this.todayNovenasGroup()?.novenas ?? []
      : this.novenasByDate().get(date)?.novenas ?? [];
  }

  previewPrimaryNovena(date: string): NovenaSummary | null {
    if (date === this.todayDateValue) {
      return this.todayNovenasGroup()?.startingNovena ?? this.featuredNovena(this.todayNovenasGroup()?.novenas ?? []);
    }

    const day = this.novenasByDate().get(date);
    return day?.startingNovena ?? this.featuredNovena(day?.novenas ?? []);
  }

  previewSaints(date: string): SaintSummary[] {
    return date === this.todayDateValue
      ? this.todaySaintGroup()?.saints ?? []
      : this.saintsByDate().get(date)?.saints ?? [];
  }

  previewLiturgical(date: string): LiturgicalDayResponse | null {
    return date === this.todayDateValue
      ? this.todayLiturgicalDay()
      : this.liturgicalByDate().get(date) ?? null;
  }

  featuredNovena(novenas: NovenaSummary[]): NovenaSummary | null {
    if (!novenas.length) {
      return null;
    }

    return [...novenas].sort((left, right) => {
      if (left.durationDays !== right.durationDays) {
        return left.durationDays - right.durationDays;
      }

      return left.title.localeCompare(right.title);
    })[0];
  }

  seasonKeyForDate(date: string | null): SeasonKey | null {
    if (!date) {
      return null;
    }

    if (date === this.todayDateValue && this.todayLiturgicalDay()) {
      return this.normalizeSeasonKey(this.todayLiturgicalDay()!.season);
    }

    return this.normalizeSeasonKey(this.liturgicalByDate().get(date)?.season ?? null);
  }

  calendarDaysWithLabels(
    cells: CalendarCell[],
    labelForDate: (date: string) => string,
  ): CalendarCell[] {
    return cells.map((day) => ({
      ...day,
      label: day.date ? labelForDate(day.date) : '',
      seasonKey: this.seasonKeyForDate(day.date),
    }));
  }

  private clearErrors(): void {
    this.liturgicalLoadFailed.set(false);
    this.saintsLoadFailed.set(false);
    this.novenasLoadFailed.set(false);
    this.prayersLoadFailed.set(false);
  }

  private getDateRange(date: string, view: CalendarView): { start: string; end: string } {
    const base = this.parseDate(date);
    if (view === 'day') {
      return { start: date, end: date };
    }

    if (view === 'week') {
      const start = new Date(base);
      start.setDate(base.getDate() - base.getDay());
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      return { start: this.formatDateForApi(start), end: this.formatDateForApi(end) };
    }

    const start = new Date(base.getFullYear(), base.getMonth(), 1);
    const end = new Date(base.getFullYear(), base.getMonth() + 1, 0);
    return { start: this.formatDateForApi(start), end: this.formatDateForApi(end) };
  }

  private parseSelectedDate(): Date {
    return this.parseDate(this.selectedDate());
  }

  private parseDate(value: string): Date {
    return new Date(`${value}T00:00:00`);
  }

  private formatDateForApi(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private toTitleCase(value: string): string {
    return value
      .toLowerCase()
      .split(' ')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private truncateLabel(value: string, maxLength: number): string {
    return value.length <= maxLength ? value : `${value.slice(0, maxLength - 1)}…`;
  }

  private apiLanguage(language: 'en' | 'es'): 'en' | 'es' | 'pl' {
    return language;
  }

  private normalizeSeasonKey(value: string | null | undefined): SeasonKey | null {
    switch (value) {
      case 'ADVENT':
      case 'CHRISTMAS':
      case 'LENT':
      case 'EASTER':
      case 'ORDINARY':
        return value;
      default:
        return null;
    }
  }

  private toMap<T>(items: T[], keyFn: (item: T) => string): Map<string, T> {
    return new Map(items.map((item) => [keyFn(item), item]));
  }

  private toCalendarEntries(selectedDate: string, view: CalendarView): CalendarCell[] {
    const base = this.parseDate(selectedDate);

    if (view === 'day') {
      return [this.toCalendarCell(base)];
    }

    if (view === 'week') {
      return this.buildWeekCalendarEntries(base);
    }

    return this.buildMonthCalendarEntries(base);
  }

  private buildMonthCalendarEntries(base: Date): CalendarCell[] {
    const year = base.getFullYear();
    const month = base.getMonth();
    const firstDay = new Date(year, month, 1);
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const leadingEmptyCells = firstDay.getDay();
    const cells: CalendarCell[] = Array.from({ length: leadingEmptyCells }, () => this.emptyCalendarCell());

    for (let day = 1; day <= daysInMonth; day += 1) {
      cells.push(this.toCalendarCell(new Date(year, month, day)));
    }

    return cells;
  }

  private buildWeekCalendarEntries(base: Date): CalendarCell[] {
    const year = base.getFullYear();
    const month = base.getMonth();
    const weekStart = new Date(base);
    weekStart.setDate(base.getDate() - base.getDay());

    return Array.from({ length: 7 }, (_, offset) => {
      const date = new Date(weekStart);
      date.setDate(weekStart.getDate() + offset);

      if (date.getFullYear() !== year || date.getMonth() !== month) {
        return this.emptyCalendarCell();
      }

      return this.toCalendarCell(date);
    });
  }

  private toCalendarCell(date: Date): CalendarCell {
    return {
      date: this.formatDateForApi(date),
      dayNumber: date.getDate(),
      label: '',
    };
  }

  private emptyCalendarCell(): CalendarCell {
    return { date: null, dayNumber: null, label: '' };
  }

  private shiftDateByView(view: CalendarView, direction: -1 | 1): void {
    const date = this.parseSelectedDate();

    if (view === 'day') {
      date.setDate(date.getDate() + direction);
    } else if (view === 'week') {
      date.setDate(date.getDate() + (direction * 7));
    } else {
      const originalDay = date.getDate();
      date.setDate(1);
      date.setMonth(date.getMonth() + direction);
      const daysInTargetMonth = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
      date.setDate(Math.min(originalDay, daysInTargetMonth));
    }

    this.clearErrors();
    this.selectedDate.set(this.formatDateForApi(date));
  }
}
