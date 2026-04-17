import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, of, switchMap, catchError } from 'rxjs';

import { LiturgicalDayResponse, SaintSummary, SanctuaryApiService } from './core/api/sanctuary-api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly api = inject(SanctuaryApiService);

  protected readonly currentTab = signal<'home' | 'novenas' | 'liturgical' | 'saints' | 'me'>('home');
  protected readonly liturgicalView = signal<'day' | 'week' | 'month'>('day');
  protected readonly showAbout = signal(false);
  protected readonly language = signal<'en' | 'es'>('en');
  protected readonly selectedDate = signal(this.formatDateForApi(new Date()));
  protected readonly liturgicalDayLoadFailed = signal(false);
  protected readonly saintsLoadFailed = signal(false);

  protected readonly liturgicalDay = toSignal(
    toObservable(this.selectedDate).pipe(
      switchMap((date) =>
        this.api.getLiturgicalDay(date).pipe(
          catchError(() => {
            this.liturgicalDayLoadFailed.set(true);
            return of<LiturgicalDayResponse | null>(null);
          }),
        ),
      ),
    ),
    { initialValue: null },
  );

  protected readonly saintsForSelectedDate = toSignal(
    combineLatest([toObservable(this.selectedDate), toObservable(this.language)]).pipe(
      switchMap(([date, language]) =>
        this.api.getSaintsByDate(date, language === 'es' ? 'es' : 'en').pipe(
          catchError(() => {
            this.saintsLoadFailed.set(true);
            return of<SaintSummary[]>([]);
          }),
        ),
      ),
    ),
    { initialValue: [] },
  );

  protected readonly selectedDateLabel = computed(() =>
    new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(this.parseSelectedDate()),
  );

  protected readonly liturgicalMonthLabel = computed(() =>
    new Intl.DateTimeFormat(this.isEnglish() ? 'en-US' : 'es-ES', {
      month: 'long',
      year: 'numeric',
    }).format(this.parseSelectedDate()),
  );

  protected readonly selectedSaintHeadline = computed(() => this.saintsForSelectedDate()[0] ?? null);
  protected readonly selectedDateDayNumber = computed(() => this.parseSelectedDate().getDate());
  protected readonly selectedDateSeasonLabel = computed(() => {
    const liturgicalDay = this.liturgicalDay();
    if (!liturgicalDay) {
      return this.isEnglish() ? 'Loading liturgical day...' : 'Cargando día litúrgico...';
    }

    return this.toTitleCase(liturgicalDay.season.replaceAll('_', ' '));
  });

  protected setTab(tab: 'home' | 'novenas' | 'liturgical' | 'saints' | 'me'): void {
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
  }

  protected isEnglish(): boolean {
    return this.language() === 'en';
  }

  protected setLiturgicalView(view: 'day' | 'week' | 'month'): void {
    this.liturgicalView.set(view);
  }

  protected shiftLiturgicalMonth(direction: -1 | 1): void {
    this.shiftSelectedDate(direction);
  }

  protected shiftSelectedDate(direction: -1 | 1): void {
    const date = this.parseSelectedDate();
    date.setDate(date.getDate() + direction);
    this.clearApiErrors();
    this.selectedDate.set(this.formatDateForApi(date));
  }

  protected resetSelectedDate(): void {
    this.clearApiErrors();
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

    return `linear-gradient(180deg, rgba(0, 0, 0, 0.15), rgba(0, 0, 0, 0.25)), url(${imageUrl})`;
  }

  protected trackSaintBySlug(_: number, saint: SaintSummary): string {
    return saint.slug;
  }

  protected localizedSaintsCountLabel(): string {
    const count = this.saintsForSelectedDate().length;
    return this.isEnglish() ? `${count} saints` : `${count} santos`;
  }

  protected localizedNoSaintsCopy(): string {
    return this.isEnglish()
      ? 'No saints are assigned to this feast day in the imported legacy data.'
      : 'No hay santos asignados a este día de fiesta en los datos heredados importados.';
  }

  protected localizedApiErrorCopy(subject: 'saints' | 'liturgical'): string {
    if (this.isEnglish()) {
      return subject === 'saints'
        ? 'We could not load saints from the API right now.'
        : 'We could not load the liturgical day from the API right now.';
    }

    return subject === 'saints'
      ? 'No pudimos cargar los santos desde la API en este momento.'
      : 'No pudimos cargar el día litúrgico desde la API en este momento.';
  }

  private clearApiErrors(): void {
    this.liturgicalDayLoadFailed.set(false);
    this.saintsLoadFailed.set(false);
  }

  private parseSelectedDate(): Date {
    return new Date(`${this.selectedDate()}T00:00:00`);
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
}
