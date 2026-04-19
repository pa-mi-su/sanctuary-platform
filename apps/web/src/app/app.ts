import { Component, computed, inject, signal } from '@angular/core';
import { HomePageComponent } from './pages/home-page.component';
import { AppHeaderComponent } from './pages/app-header.component';
import { SaintsPageComponent } from './pages/saints-page.component';
import { LiturgicalPageComponent } from './pages/liturgical-page.component';
import { NovenasPageComponent } from './pages/novenas-page.component';
import { PrayersPageComponent } from './pages/prayers-page.component';
import { MePageComponent } from './pages/me-page.component';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, of, switchMap, catchError } from 'rxjs';

import {
  LiturgicalDayResponse,
  NovenaDetail,
  NovenaCalendarDateResponse,
  NovenaSummary,
  PrayerDetail,
  PrayerSummary,
  SaintDetail,
  SaintDateGroup,
  SaintSummary,
  SanctuaryApiService,
} from './core/api/sanctuary-api.service';

type AppTab = 'home' | 'novenas' | 'liturgical' | 'saints' | 'prayers' | 'me';
type CalendarView = 'day' | 'week' | 'month';
type SeasonKey = 'ADVENT' | 'CHRISTMAS' | 'LENT' | 'EASTER' | 'ORDINARY';
type CalendarCell = { date: string | null; dayNumber: number | null; label: string; seasonKey?: SeasonKey | null };
type SaintsMode = 'calendar' | 'list';
type NovenasMode = 'calendar' | 'list' | 'intentions';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HomePageComponent, AppHeaderComponent, SaintsPageComponent, LiturgicalPageComponent, NovenasPageComponent, PrayersPageComponent, MePageComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly api = inject(SanctuaryApiService);
  private readonly todayDateValue = this.formatDateForApi(new Date());

  protected readonly currentTab = signal<AppTab>('home');
  protected readonly liturgicalView = signal<CalendarView>('month');
  protected readonly saintsView = signal<CalendarView>('day');
  protected readonly novenasView = signal<CalendarView>('day');
  protected readonly saintsMode = signal<SaintsMode>('calendar');
  protected readonly novenasMode = signal<NovenasMode>('calendar');
  protected readonly showAbout = signal(false);
  protected readonly language = signal<'en' | 'es'>('en');
  protected readonly selectedDate = signal(this.formatDateForApi(new Date()));
  protected readonly saintQuery = signal('');
  protected readonly prayerQuery = signal('');
  protected readonly novenaQuery = signal('');
  protected readonly selectedSaintSlug = signal<string | null>(null);
  protected readonly selectedPrayerSlug = signal<string | null>(null);
  protected readonly selectedNovenaSlug = signal<string | null>(null);
  protected readonly selectedNovenaDayNumber = signal(1);

  protected readonly liturgicalLoadFailed = signal(false);
  protected readonly saintsLoadFailed = signal(false);
  protected readonly novenasLoadFailed = signal(false);
  protected readonly prayersLoadFailed = signal(false);

  protected readonly liturgicalRange = toSignal(
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

  protected readonly saintsRange = toSignal(
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

  protected readonly novenaCalendarRange = toSignal(
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

  protected readonly prayerResults = toSignal(
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

  protected readonly saintResults = toSignal(
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

  protected readonly saintDetail = toSignal(
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

  protected readonly prayerDetail = toSignal(
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

  protected readonly novenaDetail = toSignal(
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

  protected readonly todayLiturgicalDay = toSignal(
    this.api.getLiturgicalDay(this.todayDateValue).pipe(catchError(() => of<LiturgicalDayResponse | null>(null))),
    { initialValue: null },
  );

  protected readonly todaySaintGroup = toSignal(
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

  protected readonly todayNovenasGroup = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.getNovenasByRange(this.todayDateValue, this.todayDateValue, this.apiLanguage(language)).pipe(
          switchMap((days) => of<NovenaCalendarDateResponse | null>(days[0] ?? { date: this.todayDateValue, novenas: [] })),
          catchError(() => of<NovenaCalendarDateResponse | null>(null)),
        ),
      ),
    ),
    { initialValue: null },
  );

  protected readonly selectedDateLabel = computed(() =>
    new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(this.parseSelectedDate()),
  );

  protected readonly monthLabel = computed(() =>
    new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'long',
      year: 'numeric',
    }).format(this.parseSelectedDate()),
  );

  protected readonly liturgicalByDate = computed(() => this.toMap(this.liturgicalRange(), (entry) => entry.date));
  protected readonly saintsByDate = computed(() => this.toMap(this.saintsRange(), (entry) => entry.date));
  protected readonly novenasByDate = computed(() => this.toMap(this.novenaCalendarRange(), (entry) => entry.date));

  protected readonly selectedLiturgicalDay = computed(() => this.liturgicalByDate().get(this.selectedDate()) ?? null);
  protected readonly selectedSaintGroup = computed(() => this.saintsByDate().get(this.selectedDate()) ?? null);
  protected readonly selectedNovenas = computed(() => this.novenasByDate().get(this.selectedDate())?.novenas ?? []);

  protected readonly selectedSaintHeadline = computed(() => this.selectedSaintGroup()?.saints[0] ?? null);
  protected readonly selectedNovenaHeadline = computed(() => this.novenasByDate().get(this.selectedDate())?.startingNovena ?? null);
  protected readonly todayNovenaHeadline = computed(() => this.todayNovenasGroup()?.startingNovena ?? null);
  protected readonly selectedNovenaDay = computed(() => {
    const detail = this.novenaDetail();
    if (!detail || detail.days.length === 0) {
      return null;
    }

    return detail.days.find((day) => day.dayNumber === this.selectedNovenaDayNumber()) ?? detail.days[0];
  });

  protected readonly liturgicalCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.liturgicalView()));
  protected readonly saintsCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.saintsView()));
  protected readonly novenaCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.novenasView()));
  protected readonly seasonLegend = computed(() => [
    { key: 'ADVENT' as const, label: this.isEnglish() ? 'Advent' : 'Adviento' },
    { key: 'CHRISTMAS' as const, label: this.isEnglish() ? 'Christmas' : 'Navidad' },
    { key: 'LENT' as const, label: this.isEnglish() ? 'Lent' : 'Cuaresma' },
    { key: 'EASTER' as const, label: this.isEnglish() ? 'Easter' : 'Pascua' },
    { key: 'ORDINARY' as const, label: this.isEnglish() ? 'Ordinary Time' : 'Tiempo Ordinario' },
  ]);

  protected readonly selectedDateSeasonLabel = computed(() => {
    const liturgicalDay = this.selectedLiturgicalDay();
    if (!liturgicalDay) {
      return this.isEnglish() ? 'Loading liturgical day...' : 'Cargando día litúrgico...';
    }

    return this.toTitleCase(liturgicalDay.season.replaceAll('_', ' '));
  });

  protected readonly selectedDateDayNumber = computed(() => this.parseSelectedDate().getDate());

  protected setTab(tab: AppTab): void {
    this.currentTab.set(tab);
  }

  protected handlePrimaryNavigate(tab: AppTab): void {
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

  protected openAbout(): void {
    this.showAbout.set(true);
  }

  protected closeAbout(): void {
    this.showAbout.set(false);
  }

  protected toggleLanguage(): void {
    this.language.update((current) => (current === 'en' ? 'es' : 'en'));
    this.clearErrors();
  }

  protected isEnglish(): boolean {
    return this.language() === 'en';
  }

  protected setLiturgicalView(view: CalendarView): void {
    this.liturgicalView.set(view);
    this.liturgicalLoadFailed.set(false);
  }

  protected setSaintsView(view: CalendarView): void {
    this.saintsView.set(view);
    this.saintsLoadFailed.set(false);
  }

  protected setNovenasView(view: CalendarView): void {
    this.novenasView.set(view);
    this.novenasLoadFailed.set(false);
  }

  protected shiftSelectedDate(direction: -1 | 1): void {
    this.shiftDateByView(this.currentCalendarView(), direction);
  }

  protected shiftLiturgicalDate(direction: -1 | 1): void {
    this.shiftDateByView(this.liturgicalView(), direction);
  }

  protected shiftSaintsDate(direction: -1 | 1): void {
    this.shiftDateByView(this.saintsView(), direction);
  }

  protected shiftNovenasDate(direction: -1 | 1): void {
    this.shiftDateByView(this.novenasView(), direction);
  }

  protected shiftSelectedMonth(direction: -1 | 1): void {
    const date = this.parseSelectedDate();
    date.setMonth(date.getMonth() + direction);
    this.clearErrors();
    this.selectedDate.set(this.formatDateForApi(date));
  }

  protected resetSelectedDate(): void {
    this.clearErrors();
    this.selectedDate.set(this.formatDateForApi(new Date()));
  }

  protected isSelectedDateToday(): boolean {
    return this.selectedDate() === this.formatDateForApi(new Date());
  }

  protected todayDate(): string {
    return this.todayDateValue;
  }

  protected selectedSaintImageStyle(): string | null {
    const imageUrl = this.selectedSaintHeadline()?.imageUrl;
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})`;
  }

  protected selectedNovenaImageStyle(): string | null {
    const imageUrl = this.selectedNovenaHeadline()?.imageUrl;
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})`;
  }

  protected detailImageStyle(imageUrl: string | null | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})`;
  }

  protected cardImageStyle(imageUrl: string | null | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(6, 12, 18, 0.05), rgba(6, 12, 18, 0.28)), url(${imageUrl})`;
  }

  protected updatePrayerQuery(value: string): void {
    this.prayerQuery.set(value);
    this.prayersLoadFailed.set(false);
  }

  protected updateSaintQuery(value: string): void {
    this.saintQuery.set(value);
    this.saintsLoadFailed.set(false);
  }

  protected updateNovenaQuery(value: string): void {
    this.novenaQuery.set(value);
    this.novenasLoadFailed.set(false);
  }

  protected setNovenasMode(mode: NovenasMode): void {
    this.novenasMode.set(mode);
    this.novenasLoadFailed.set(false);
  }

  protected selectDateFromGrid(date: string): void {
    this.clearErrors();
    this.selectedDate.set(date);
  }

  protected openSaintDetail(saint: SaintSummary): void {
    this.selectedSaintSlug.set(saint.slug);
  }

  protected openPrayerDetail(prayer: PrayerSummary): void {
    this.selectedPrayerSlug.set(prayer.slug);
  }

  protected openNovenaDetail(novena: NovenaSummary): void {
    this.selectedNovenaSlug.set(novena.slug);
    this.selectedNovenaDayNumber.set(1);
  }

  protected selectNovenaDay(dayNumber: number): void {
    this.selectedNovenaDayNumber.set(dayNumber);
  }

  protected closeDetailModal(): void {
    this.selectedSaintSlug.set(null);
    this.selectedPrayerSlug.set(null);
    this.selectedNovenaSlug.set(null);
    this.selectedNovenaDayNumber.set(1);
  }

  protected showDailyTab(): void {
    this.setTab('liturgical');
  }

  protected openIntentions(): void {
    this.setTab('novenas');
    this.setNovenasMode('intentions');
    this.novenaQuery.set('');
  }

  protected browseNovenas(): void {
    this.setTab('novenas');
    this.setNovenasMode('calendar');
  }

  protected openNovenasList(): void {
    this.setTab('novenas');
    this.setNovenasMode('list');
    this.novenaQuery.set('');
  }

  protected browseSaintsCalendar(): void {
    this.setTab('saints');
    this.saintsMode.set('calendar');
  }

  protected openSaintsList(): void {
    this.setTab('saints');
    this.saintsMode.set('list');
    this.saintQuery.set('');
  }

  protected localizedSaintsCountLabel(): string {
    const count = this.selectedSaintGroup()?.saints.length ?? 0;
    return this.isEnglish() ? `Selected day · ${count} saints` : `Día seleccionado · ${count} santos`;
  }

  protected localizedNovenasCountLabel(): string {
    const count = this.selectedNovenas().length;
    return this.isEnglish()
      ? `Selected day · ${count} active novenas`
      : `Día seleccionado · ${count} novenas activas`;
  }

  protected localizedNoSaintsCopy(): string {
    return this.isEnglish()
      ? 'No saints are assigned to this feast day in the imported legacy data.'
      : 'No hay santos asignados a este día de fiesta en los datos heredados importados.';
  }

  protected localizedNoNovenasCopy(): string {
    return this.isEnglish()
      ? 'No novenas are active for this date.'
      : 'No hay novenas activas para esta fecha.';
  }

  protected localizedNovenaSearchPlaceholder(): string {
    if (this.novenasMode() === 'intentions') {
      return this.isEnglish() ? 'Search intentions' : 'Buscar intenciones';
    }

    return this.isEnglish() ? 'Search novenas' : 'Buscar novenas';
  }

  protected localizedNovenaSearchHeading(): string {
    if (this.novenasMode() === 'intentions') {
      return this.isEnglish() ? 'Intentions' : 'Intenciones';
    }

    return this.isEnglish() ? 'Novenas' : 'Novenas';
  }

  protected localizedSaintResultsLabel(): string {
    return this.isEnglish() ? `${this.saintResults().length} saints` : `${this.saintResults().length} santos`;
  }

  protected localizedPrayerResultsLabel(): string {
    return this.isEnglish() ? `${this.prayerResults().length} prayers` : `${this.prayerResults().length} oraciones`;
  }

  protected localizedIntentionsResultsLabel(): string {
    if (this.novenasMode() === 'list') {
      return this.isEnglish()
        ? `${this.novenaSearchResults().length} novenas`
        : `${this.novenaSearchResults().length} novenas`;
    }

    return this.isEnglish()
      ? `${this.novenaSearchResults().length} novenas with intentions`
      : `${this.novenaSearchResults().length} novenas con intenciones`;
  }

  protected localizedPreviewTitle(mode: 'today' | 'selected'): string {
    if (mode === 'today') {
      return this.isEnglish() ? 'Today' : 'Hoy';
    }

    return this.isEnglish() ? 'Selected Day' : 'Día seleccionado';
  }

  protected localizedNoLiturgicalCopy(): string {
    return this.isEnglish()
      ? 'No liturgical summary is available for this day.'
      : 'No hay un resumen litúrgico disponible para este día.';
  }

  protected localizedSelectedSameAsTodayCopy(): string {
    return this.isEnglish()
      ? 'Selected day matches today.'
      : 'El día seleccionado coincide con hoy.';
  }

  protected localizedIntentionsEmptyCopy(): string {
    if (this.novenasMode() === 'list') {
      return this.isEnglish()
        ? 'Browse the novena library or search for a specific novena.'
        : 'Explora la biblioteca de novenas o busca una novena específica.';
    }

    return this.isEnglish()
      ? 'Browse the available intention novenas or search for a specific intention.'
      : 'Revisa las novenas con intenciones disponibles o busca una intención específica.';
  }

  protected localizedApiErrorCopy(subject: 'saints' | 'liturgical' | 'novenas' | 'prayers'): string {
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

  protected shortSaintLabel(date: string): string {
    const saint = this.saintsByDate().get(date)?.saints[0];
    return saint ? this.truncateLabel(saint.name, 16) : '—';
  }

  protected shortLiturgicalLabel(date: string): string {
    const day = this.liturgicalByDate().get(date);
    return day ? this.truncateLabel(day.primaryRank, 18) : '—';
  }

  protected shortNovenaLabel(date: string): string {
    const novena = this.novenasByDate().get(date)?.startingNovena ?? null;
    return novena ? this.truncateLabel(novena.title, 16) : '—';
  }

  protected isDateSelected(date: string): boolean {
    return this.selectedDate() === date;
  }

  protected isDateToday(date: string): boolean {
    return date === this.formatDateForApi(new Date());
  }

  protected previewDateLabel(date: string): string {
    return new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(this.parseDate(date));
  }

  protected previewNovenas(date: string): NovenaSummary[] {
    if (date === this.todayDateValue) {
      return this.todayNovenasGroup()?.novenas ?? [];
    }

    return this.novenasByDate().get(date)?.novenas ?? [];
  }

  protected previewPrimaryNovena(date: string): NovenaSummary | null {
    if (date === this.todayDateValue) {
      return this.todayNovenasGroup()?.startingNovena ?? this.featuredNovena(this.todayNovenasGroup()?.novenas ?? []);
    }

    const day = this.novenasByDate().get(date);
    return day?.startingNovena ?? this.featuredNovena(day?.novenas ?? []);
  }

  protected previewSaints(date: string): SaintSummary[] {
    if (date === this.todayDateValue) {
      return this.todaySaintGroup()?.saints ?? [];
    }

    return this.saintsByDate().get(date)?.saints ?? [];
  }

  protected previewLiturgical(date: string): LiturgicalDayResponse | null {
    if (date === this.todayDateValue) {
      return this.todayLiturgicalDay();
    }

    return this.liturgicalByDate().get(date) ?? null;
  }

  protected novenaDayCountLabel(novena: NovenaSummary): string {
    return this.isEnglish() ? `${novena.durationDays}-day novena` : `Novena de ${novena.durationDays} días`;
  }

  protected featuredNovena(novenas: NovenaSummary[]): NovenaSummary | null {
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

  protected prayerPreviewLabel(prayer: PrayerSummary): string {
    return prayer.bodyPreview;
  }

  protected readonly weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  protected seasonKeyForDate(date: string | null): SeasonKey | null {
    if (!date) {
      return null;
    }

    if (date === this.todayDateValue && this.todayLiturgicalDay()) {
      return this.normalizeSeasonKey(this.todayLiturgicalDay()!.season);
    }

    return this.normalizeSeasonKey(this.liturgicalByDate().get(date)?.season ?? null);
  }

  protected calendarDaysWithLabels(
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

  private currentCalendarView(): CalendarView {
    switch (this.currentTab()) {
      case 'liturgical':
        return this.liturgicalView();
      case 'saints':
        return this.saintsView();
      case 'novenas':
        return this.novenasView();
      default:
        return 'day';
    }
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

  protected readonly novenaSearchResults = toSignal(
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
}
