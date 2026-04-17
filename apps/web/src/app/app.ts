import { Component, computed, inject, signal } from '@angular/core';
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

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly api = inject(SanctuaryApiService);

  protected readonly currentTab = signal<AppTab>('home');
  protected readonly liturgicalView = signal<CalendarView>('month');
  protected readonly saintsView = signal<CalendarView>('day');
  protected readonly novenasView = signal<CalendarView>('day');
  protected readonly showAbout = signal(false);
  protected readonly language = signal<'en' | 'es'>('en');
  protected readonly selectedDate = signal(this.formatDateForApi(new Date()));
  protected readonly prayerQuery = signal('');
  protected readonly novenaQuery = signal('');
  protected readonly novenaSearchMode = signal<'title' | 'intentions'>('title');
  protected readonly selectedSaintSlug = signal<string | null>(null);
  protected readonly selectedPrayerSlug = signal<string | null>(null);
  protected readonly selectedNovenaSlug = signal<string | null>(null);
  protected readonly selectedNovenaDayNumber = signal(1);

  protected readonly liturgicalLoadFailed = signal(false);
  protected readonly saintsLoadFailed = signal(false);
  protected readonly novenasLoadFailed = signal(false);
  protected readonly prayersLoadFailed = signal(false);

  protected readonly liturgicalRange = toSignal(
    combineLatest([toObservable(this.selectedDate), toObservable(this.liturgicalView)]).pipe(
      switchMap(([date, view]) => {
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
  protected readonly selectedNovenaDay = computed(() => {
    const detail = this.novenaDetail();
    if (!detail || detail.days.length === 0) {
      return null;
    }

    return detail.days.find((day) => day.dayNumber === this.selectedNovenaDayNumber()) ?? detail.days[0];
  });

  protected readonly liturgicalCalendarDays = computed(() => this.toCalendarEntries(this.liturgicalRange()));
  protected readonly saintsCalendarDays = computed(() => this.toCalendarEntries(this.saintsRange()));
  protected readonly novenaCalendarDays = computed(() => this.toCalendarEntries(this.novenaCalendarRange()));

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
    const date = this.parseSelectedDate();
    date.setDate(date.getDate() + direction);
    this.clearErrors();
    this.selectedDate.set(this.formatDateForApi(date));
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

  protected selectedSaintImageStyle(): string | null {
    const imageUrl = this.selectedSaintHeadline()?.imageUrl;
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

  protected updatePrayerQuery(value: string): void {
    this.prayerQuery.set(value);
    this.prayersLoadFailed.set(false);
  }

  protected updateNovenaQuery(value: string): void {
    this.novenaQuery.set(value);
    this.novenasLoadFailed.set(false);
  }

  protected setNovenaSearchMode(mode: 'title' | 'intentions'): void {
    this.novenaSearchMode.set(mode);
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
    this.setNovenaSearchMode('intentions');
    this.novenaQuery.set('');
  }

  protected browseNovenas(): void {
    this.setTab('novenas');
    this.setNovenaSearchMode('title');
  }

  protected localizedSaintsCountLabel(): string {
    const count = this.selectedSaintGroup()?.saints.length ?? 0;
    return this.isEnglish() ? `${count} saints` : `${count} santos`;
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
    if (this.novenaSearchMode() === 'intentions') {
      return this.isEnglish() ? 'Search intentions' : 'Buscar intenciones';
    }

    return this.isEnglish() ? 'Search novenas' : 'Buscar novenas';
  }

  protected localizedNovenaSearchHeading(): string {
    if (this.novenaSearchMode() === 'intentions') {
      return this.isEnglish() ? 'Intentions' : 'Intenciones';
    }

    return this.isEnglish() ? 'Search Results' : 'Resultados';
  }

  protected localizedPrayerResultsLabel(): string {
    return this.isEnglish() ? `${this.prayerResults().length} prayers` : `${this.prayerResults().length} oraciones`;
  }

  protected localizedIntentionsEmptyCopy(): string {
    return this.isEnglish()
      ? 'Search by intention to see matching novenas.'
      : 'Busca por intención para ver novenas relacionadas.';
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
    const novena = this.novenasByDate().get(date)?.novenas[0];
    return novena ? this.truncateLabel(novena.title, 16) : '—';
  }

  protected isDateSelected(date: string): boolean {
    return this.selectedDate() === date;
  }

  protected isDateToday(date: string): boolean {
    return date === this.formatDateForApi(new Date());
  }

  protected novenaDayCountLabel(novena: NovenaSummary): string {
    return this.isEnglish() ? `${novena.durationDays}-day novena` : `Novena de ${novena.durationDays} días`;
  }

  protected prayerPreviewLabel(prayer: PrayerSummary): string {
    return prayer.bodyPreview;
  }

  protected readonly weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

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

  private toMap<T>(items: T[], keyFn: (item: T) => string): Map<string, T> {
    return new Map(items.map((item) => [keyFn(item), item]));
  }

  private toCalendarEntries<T extends { date: string }>(items: T[]) {
    return items.map((item) => {
      const date = this.parseDate(item.date);
      return {
        date: item.date,
        dayNumber: date.getDate(),
      };
    });
  }

  protected readonly novenaSearchResults = toSignal(
    combineLatest([toObservable(this.novenaQuery), toObservable(this.language), toObservable(this.novenaSearchMode)]).pipe(
      switchMap(([query, language, mode]) => {
        if (mode === 'intentions' && !query.trim()) {
          return of<NovenaSummary[]>([]);
        }

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
