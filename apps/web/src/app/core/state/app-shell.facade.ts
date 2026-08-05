import { computed, inject, Injectable, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';

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
  SearchTerm,
  SanctuaryApiService,
  UserFavorite,
  UserNovenaCommitment,
  UserNovenaCommitmentRequest,
  UserPreferencesUpdateRequest,
  UserProfile,
} from '../api/sanctuary-api.service';
import { SanctuaryAuthService } from '../auth/sanctuary-auth.service';

export type AppTab = 'home' | 'novenas' | 'intentions' | 'patronage' | 'liturgical' | 'saints' | 'prayers' | 'rosaries' | 'about' | 'auth' | 'me';
export type CalendarView = 'day' | 'week' | 'month';
export type SeasonKey = 'ADVENT' | 'CHRISTMAS' | 'LENT' | 'EASTER' | 'ORDINARY';
export type CalendarCell = { date: string | null; dayNumber: number | null; label: string; seasonKey?: SeasonKey | null };
export type SaintsMode = 'calendar' | 'list';
export type NovenasMode = 'calendar' | 'list';
export type AppLanguage = 'en' | 'es' | 'pl';
export type LegalDocumentType = 'support' | 'privacy';
export interface MeLinkedItem {
  id: string;
  slug: string;
  title: string;
  subtitle: string;
  imageUrl: string | null;
}
export interface LocalNovenaProgress {
  novenaId: string;
  startedAt: string;
  currentDay: number;
  completedDays: number[];
  status: 'active' | 'paused' | 'completed';
}

const LOCAL_NOVENA_PROGRESS_KEY = 'sanctuary.localNovenaProgress';
@Injectable({ providedIn: 'root' })
export class AppShellFacade {
  private readonly api = inject(SanctuaryApiService);
  private readonly auth = inject(SanctuaryAuthService);
  private readonly todayDateValue = this.formatDateForApi(new Date());

  readonly currentTab = signal<AppTab>('home');
  readonly liturgicalView = signal<CalendarView>('month');
  readonly saintsView = signal<CalendarView>('day');
  readonly novenasView = signal<CalendarView>('day');
  readonly saintsMode = signal<SaintsMode>('calendar');
  readonly novenasMode = signal<NovenasMode>('calendar');
  readonly termQuery = signal('');
  readonly selectedTerm = signal<SearchTerm | null>(null);
  readonly selectedIntentionNovenas = signal<NovenaSummary[]>([]);
  readonly selectedPatronageSaints = signal<SaintSummary[]>([]);
  readonly termLoadFailed = signal(false);
  readonly activeLegalDocument = signal<LegalDocumentType | null>(null);
  readonly accountRequiredPrompt = signal(false);
  readonly authInitialStep = signal<'landing' | 'login' | 'register'>('landing');
  readonly language = signal<AppLanguage>('en');
  readonly authState = this.auth.state;
  readonly isAuthenticated = computed(() => this.authState().status === 'authenticated');
  readonly authConfigured = computed(() => this.authState().configured);
  readonly authMessage = computed(() => this.authState().message);
  readonly currentUserName = computed(() => {
    const profile = this.userProfile();
    const authState = this.authState();
    const candidate =
      profile?.displayName ??
      this.joinNames(profile?.firstName, profile?.lastName) ??
      authState.displayName ??
      this.emailName(profile?.email ?? authState.email);

    return this.looksLikeIdentifier(candidate) ? this.emailName(profile?.email ?? authState.email) : candidate;
  });
  readonly selectedDate = signal(this.formatDateForApi(new Date()));
  readonly saintQuery = signal('');
  readonly prayerQuery = signal('');
  readonly rosaryQuery = signal('');
  readonly novenaQuery = signal('');
  readonly selectedSaintSlug = signal<string | null>(null);
  readonly selectedPrayerSlug = signal<string | null>(null);
  readonly selectedNovenaSlug = signal<string | null>(null);
  readonly selectedNovenaId = signal<string | null>(null);
  readonly selectedNovenaDayNumber = signal(1);
  readonly selectedNovenaDayManuallyChanged = signal(false);
  readonly localNovenaProgress = signal<Record<string, LocalNovenaProgress>>(this.loadLocalNovenaProgress());
  readonly favoriteOverrides = signal<Record<string, boolean>>({});
  readonly completedNovenaTitle = signal<string | null>(null);
  readonly savePreferencesPending = signal(false);
  readonly savePreferencesMessage = signal<string | null>(null);
  readonly savePreferencesError = signal(false);
  readonly deleteAccountPending = signal(false);
  readonly deleteAccountMessage = signal<string | null>(null);
  readonly deleteAccountError = signal(false);

  readonly liturgicalLoadFailed = signal(false);
  readonly saintsLoadFailed = signal(false);
  readonly novenasLoadFailed = signal(false);
  readonly prayersLoadFailed = signal(false);
  private readonly userProfileReloadToken = signal(0);
  private readonly userCollectionsReloadToken = signal(0);
  private readonly userProfileOverride = signal<UserProfile | null>(null);
  private suppressNextHistoryUpdate = false;

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
        this.api.listPrayers(this.apiLanguage(language), query, { excludeCategory: 'rosary' }).pipe(
          map((prayers) => prayers.filter((prayer) => prayer.category?.toLowerCase() !== 'rosary')),
          catchError(() => {
            this.prayersLoadFailed.set(true);
            return of<PrayerSummary[]>([]);
          }),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly rosaryResults = toSignal(
    combineLatest([toObservable(this.rosaryQuery), toObservable(this.language)]).pipe(
      switchMap(([query, language]) =>
        this.api.listPrayers(this.apiLanguage(language), query, { category: 'rosary' }).pipe(
          map((prayers) => prayers.filter((prayer) => prayer.category?.toLowerCase() === 'rosary')),
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
          startWith(null),
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

  readonly userProfileResponse = toSignal(
    combineLatest([toObservable(this.authState), toObservable(this.userProfileReloadToken)]).pipe(
      switchMap(([authState]) =>
        authState.status === 'authenticated'
          ? this.api.getMe().pipe(catchError((error) => this.handleSessionReadError<UserProfile | null>(error, null)))
          : of<UserProfile | null>(null)
      ),
    ),
    { initialValue: null },
  );
  readonly userProfile = computed(() => this.userProfileOverride() ?? this.userProfileResponse());

  readonly userFavorites = toSignal(
    combineLatest([toObservable(this.authState), toObservable(this.userCollectionsReloadToken)]).pipe(
      switchMap(([authState]) =>
        authState.status === 'authenticated'
          ? this.api.listFavorites().pipe(catchError((error) => this.handleSessionReadError<UserFavorite[]>(error, [])))
          : of<UserFavorite[]>([])
      ),
    ),
    { initialValue: [] },
  );

  readonly userNovenaCommitments = toSignal(
    combineLatest([toObservable(this.authState), toObservable(this.userCollectionsReloadToken)]).pipe(
      switchMap(([authState]) =>
        authState.status === 'authenticated'
          ? this.api.listNovenaCommitments().pipe(catchError((error) => this.handleSessionReadError<UserNovenaCommitment[]>(error, [])))
          : of<UserNovenaCommitment[]>([])
      ),
    ),
    { initialValue: [] },
  );

  readonly meSaintCatalog = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.listSaints(this.apiLanguage(language), '').pipe(
          catchError(() => of<SaintSummary[]>([])),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly meNovenaCatalog = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.listNovenas(this.apiLanguage(language), '').pipe(
          catchError(() => of<NovenaSummary[]>([])),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly mePrayerCatalog = toSignal(
    toObservable(this.language).pipe(
      switchMap((language) =>
        this.api.listPrayers(this.apiLanguage(language), '').pipe(
          catchError(() => of<PrayerSummary[]>([])),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly favoriteNovenaCount = computed(() => this.isAuthenticated() ? this.favoriteCount('novena') : 0);
  readonly favoriteSaintCount = computed(() => this.isAuthenticated() ? this.favoriteCount('saint') : 0);
  readonly favoritePrayerCount = computed(() => this.isAuthenticated() ? this.favoriteCount('prayer') : 0);
  readonly activeNovenaCommitmentCount = computed(() =>
    this.isAuthenticated()
      ? this.userNovenaCommitments().filter((commitment) => commitment.status === 'active').length
      : 0
  );
  readonly meActiveNovenaItems = computed(() => {
    if (!this.isAuthenticated()) {
      return [];
    }

    const novenasById = new Map(this.meNovenaCatalog().map((novena) => [novena.id, novena] as const));
    const commitmentsById = new Map(
      this.userNovenaCommitments()
        .filter((commitment) => commitment.status === 'active')
        .map((commitment) => [commitment.novenaId, commitment] as const),
    );

    return this.userNovenaCommitments()
      .filter((commitment) => commitment.status === 'active')
      .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
      .map((commitment) => {
        const novena = novenasById.get(commitment.novenaId);
        return novena ? { novena, commitment } : null;
      })
      .filter((entry): entry is { novena: NovenaSummary; commitment: UserNovenaCommitment } => Boolean(entry))
      .map(({ novena, commitment }) => ({
        id: novena.id,
        slug: novena.slug,
        title: novena.title,
        subtitle: this.translate(
          `Day ${Math.min(commitment.currentDay, novena.durationDays)} of ${novena.durationDays}`,
          `Día ${Math.min(commitment.currentDay, novena.durationDays)} de ${novena.durationDays}`,
          `Dzień ${Math.min(commitment.currentDay, novena.durationDays)} z ${novena.durationDays}`,
        ),
        imageUrl: novena.imageUrl,
      }));
  });
  readonly meFavoriteNovenaItems = computed(() => {
    if (!this.isAuthenticated()) {
      return [];
    }

    const novenasById = new Map(this.meNovenaCatalog().map((novena) => [novena.id, novena] as const));
    return this.userFavorites()
      .filter((favorite) => favorite.itemType === 'novena')
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
      .map((favorite) => novenasById.get(favorite.itemId))
      .filter((novena): novena is NovenaSummary => Boolean(novena))
      .map((novena) => ({
        id: novena.id,
        slug: novena.slug,
        title: novena.title,
        subtitle: this.translate(
          `${novena.durationDays}-day novena`,
          `Novena de ${novena.durationDays} días`,
          `${novena.durationDays}-dniowa nowenna`,
        ),
        imageUrl: novena.imageUrl,
      }));
  });
  readonly meFavoriteSaintItems = computed(() => {
    if (!this.isAuthenticated()) {
      return [];
    }

    const saintsById = new Map(this.meSaintCatalog().map((saint) => [saint.id, saint] as const));
    return this.userFavorites()
      .filter((favorite) => favorite.itemType === 'saint')
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
      .map((favorite) => saintsById.get(favorite.itemId))
      .filter((saint): saint is SaintSummary => Boolean(saint))
      .map((saint) => ({
        id: saint.id,
        slug: saint.slug,
        title: saint.name,
        subtitle: saint.feastLabel,
        imageUrl: saint.imageUrl,
      }));
  });
  readonly meFavoritePrayerItems = computed(() => {
    if (!this.isAuthenticated()) {
      return [];
    }

    const prayersById = new Map(
      this.mePrayerCatalog().flatMap((prayer) => [[prayer.id, prayer] as const, [prayer.slug, prayer] as const]),
    );
    return this.userFavorites()
      .filter((favorite) => favorite.itemType === 'prayer')
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
      .map((favorite) => prayersById.get(favorite.itemId))
      .filter((prayer): prayer is PrayerSummary => Boolean(prayer))
      .map((prayer) => ({
        id: prayer.id,
        slug: prayer.slug,
        title: prayer.title,
        subtitle: this.translate('Prayer', 'Oración', 'Modlitwa'),
        imageUrl: prayer.imageUrl,
      }));
  });
  readonly selectedNovenaProgress = computed(() => {
    if (!this.isAuthenticated()) {
      return null;
    }

    const detail = this.novenaDetail();
    return this.novenaProgressForNovena(
      detail?.id ?? this.selectedNovenaId(),
      detail?.slug ?? this.selectedNovenaSlug(),
    );
  });
  readonly selectedSaintIsFavorite = computed(() => {
    const saint = this.saintDetail();
    return saint ? this.isFavorite('saint', saint.id) : false;
  });
  readonly selectedNovenaIsFavorite = computed(() => {
    const novena = this.novenaDetail();
    return novena ? this.isFavorite('novena', novena.id) : false;
  });
  readonly selectedPrayerIsFavorite = computed(() => {
    const prayer = this.prayerDetail();
    return prayer ? this.isFavorite('prayer', prayer.id) : false;
  });

  constructor() {
    void this.auth.completeRedirectIfPresent();
    if (typeof window !== 'undefined') {
      this.openSharedContentFromPath(window.location.pathname, false);
      window.addEventListener('popstate', () => {
        this.openSharedContentFromPath(window.location.pathname, false);
      });
    }
  }

  readonly selectedDateLabel = computed(() =>
    new Intl.DateTimeFormat(this.dateLocale(), {
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
  readonly selectedNovenaHeadline = computed(() => {
    const selectedDay = this.novenasByDate().get(this.selectedDate());
    return selectedDay?.startingNovena ?? null;
  });
  readonly selectedNovenaDay = computed(() => {
    const detail = this.novenaDetail();
    if (!detail || detail.days.length === 0) {
      return null;
    }

    const progress = this.selectedNovenaProgress();
    const selectedDayNumber = !this.selectedNovenaDayManuallyChanged() && progress?.status === 'active'
      ? progress.currentDay
      : this.selectedNovenaDayNumber();

    return detail.days.find((day) => day.dayNumber === selectedDayNumber) ?? detail.days[0];
  });

  readonly liturgicalCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.liturgicalView()));
  readonly saintsCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.saintsView()));
  readonly novenaCalendarDays = computed(() => this.toCalendarEntries(this.selectedDate(), this.novenasView()));

  readonly seasonLegend = computed(() => [
    { key: 'ADVENT' as const, label: this.translate('Advent', 'Adviento', 'Adwent') },
    { key: 'CHRISTMAS' as const, label: this.translate('Christmas', 'Navidad', 'Boze Narodzenie') },
    { key: 'LENT' as const, label: this.translate('Lent', 'Cuaresma', 'Wielki Post') },
    { key: 'EASTER' as const, label: this.translate('Easter', 'Pascua', 'Wielkanoc') },
    { key: 'ORDINARY' as const, label: this.translate('Ordinary Time', 'Tiempo Ordinario', 'Okres Zwykly') },
  ]);

  readonly selectedDateSeasonLabel = computed(() => {
    const liturgicalDay = this.selectedLiturgicalDay();
    if (!liturgicalDay) {
      return this.translate('Loading liturgical day...', 'Cargando día litúrgico...', 'Ladowanie dnia liturgicznego...');
    }

    return this.toTitleCase(liturgicalDay.season.replaceAll('_', ' '));
  });

  readonly selectedDateDayNumber = computed(() => this.parseSelectedDate().getDate());

  readonly novenaSearchResults = toSignal(
    combineLatest([toObservable(this.novenaQuery), toObservable(this.language), toObservable(this.novenasMode)]).pipe(
      switchMap(([query, language]) => {
        return this.api.listNovenas(this.apiLanguage(language), query).pipe(
          catchError(() => {
            this.novenasLoadFailed.set(true);
            return of<NovenaSummary[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly intentionTerms = toSignal(
    combineLatest([toObservable(this.termQuery), toObservable(this.language), toObservable(this.currentTab)]).pipe(
      switchMap(([query, language, tab]) => {
        if (tab !== 'intentions') {
          return of<SearchTerm[]>([]);
        }

        return this.api.searchIntentionTerms(this.apiLanguage(language), query).pipe(
          catchError(() => {
            this.termLoadFailed.set(true);
            return of<SearchTerm[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly patronageTerms = toSignal(
    combineLatest([toObservable(this.termQuery), toObservable(this.language), toObservable(this.currentTab)]).pipe(
      switchMap(([query, language, tab]) => {
        if (tab !== 'patronage') {
          return of<SearchTerm[]>([]);
        }

        return this.api.searchPatronageTerms(this.apiLanguage(language), query).pipe(
          catchError(() => {
            this.termLoadFailed.set(true);
            return of<SearchTerm[]>([]);
          }),
        );
      }),
    ),
    { initialValue: [] },
  );

  readonly weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  setTab(tab: AppTab): void {
    if (tab !== this.currentTab() || tab === 'intentions' || tab === 'patronage') {
      this.clearSelectedTerm();
      this.termLoadFailed.set(false);
    }
    this.currentTab.set(tab);
    if (tab === 'me' && this.isAuthenticated()) {
      this.refreshUserCollections();
    }
  }

  private resetViewportToTop(): void {
    const scroll = () => {
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      }
    };

    scroll();
    if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(scroll);
    }
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

    if (tab === 'me' && !this.isAuthenticated()) {
      this.openAuth();
      return;
    }

    this.setTab(tab);
  }

  openAuth(): void {
    this.savePreferencesMessage.set(null);
    this.authInitialStep.set('landing');
    this.setTab('auth');
  }

  requireAccount(): void {
    this.accountRequiredPrompt.set(true);
  }

  dismissAccountRequiredPrompt(): void {
    this.accountRequiredPrompt.set(false);
  }

  openAuthenticationFromPrompt(step: 'login' | 'register'): void {
    this.accountRequiredPrompt.set(false);
    this.closeDetailModal();
    this.authInitialStep.set(step);
    this.setTab('auth');
  }

  loginDemoUser(): void {
    void this.auth.startLogin();
  }

  registerDemoUser(): void {
    void this.auth.startRegister();
  }

  logout(): void {
    this.userProfileOverride.set(null);
    this.favoriteOverrides.set({});
    this.savePreferencesMessage.set(null);
    this.savePreferencesError.set(false);
    this.deleteAccountMessage.set(null);
    this.deleteAccountError.set(false);
    this.auth.logout();
    if (this.currentTab() === 'me') {
      this.setTab('auth');
    }
  }

  deleteAccount(): void {
    if (!this.isAuthenticated() || this.deleteAccountPending()) {
      return;
    }

    this.deleteAccountPending.set(true);
    this.deleteAccountMessage.set(null);
    this.deleteAccountError.set(false);

    this.api.deleteMe().subscribe({
      next: () => {
        this.deleteAccountPending.set(false);
        this.userProfileOverride.set(null);
        this.favoriteOverrides.set({});
        this.localNovenaProgress.set({});
        this.persistLocalNovenaProgress({});
        this.auth.logout();
        this.setTab('auth');
      },
      error: (error) => {
        if (this.isSessionRejected(error)) {
          this.deleteAccountPending.set(false);
          this.userProfileOverride.set(null);
          this.favoriteOverrides.set({});
          this.localNovenaProgress.set({});
          this.persistLocalNovenaProgress({});
          this.auth.logout();
          this.setTab('auth');
          return;
        }

        this.deleteAccountPending.set(false);
        this.deleteAccountError.set(true);
        this.deleteAccountMessage.set(
          this.translate(
            'We could not delete your account right now. Please try again.',
            'No pudimos eliminar tu cuenta en este momento. Inténtalo de nuevo.',
            'Nie można teraz usunąć konta. Spróbuj ponownie.'
          )
        );
      },
    });
  }

  private handleSessionReadError<T>(error: unknown, fallback: T) {
    if (this.isSessionRejected(error)) {
      this.userProfileOverride.set(null);
      this.favoriteOverrides.set({});
      this.auth.logout();
      this.setTab('auth');
    }

    return of(fallback);
  }

  private isSessionRejected(error: unknown): boolean {
    return error instanceof HttpErrorResponse && [401, 403, 404, 410].includes(error.status);
  }

  openLegalDocument(document: LegalDocumentType): void {
    this.activeLegalDocument.set(document);
    this.setTab('about');
    this.pushPath(`/${document}`);
  }

  closeLegalDocument(): void {
    this.activeLegalDocument.set(null);
    this.pushPath('/');
  }

  setLanguage(language: AppLanguage): void {
    this.language.set(language);
    this.clearErrors();
  }

  saveUserPreferences(request: UserPreferencesUpdateRequest): void {
    if (!this.isAuthenticated()) {
      return;
    }

    this.savePreferencesPending.set(true);
    this.savePreferencesMessage.set(null);
    this.savePreferencesError.set(false);

    this.api.updateMePreferences(request).subscribe({
      next: (profile) => {
        this.userProfileOverride.set(profile);
        this.language.set(profile.preferredLanguage ?? request.preferredLanguage);
        this.savePreferencesPending.set(false);
        this.savePreferencesError.set(false);
        this.savePreferencesMessage.set(
          this.translate(
            'Settings saved.',
            'Configuración guardada.',
            'Ustawienia zapisane.'
          )
        );
      },
      error: () => {
        this.savePreferencesPending.set(false);
        this.savePreferencesError.set(true);
        this.savePreferencesMessage.set(
          this.translate(
            'We could not save your settings right now.',
            'No pudimos guardar tu configuración en este momento.',
            'Nie można teraz zapisać ustawień.'
          )
        );
      },
    });
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

  updateRosaryQuery(value: string): void {
    this.rosaryQuery.set(value);
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
    this.pushSharedContentPath('saints', saint.slug);
  }

  openPrayerDetail(prayer: PrayerSummary): void {
    this.selectedPrayerSlug.set(prayer.slug);
    this.pushSharedContentPath('prayers', prayer.slug);
  }

  openNovenaDetail(novena: NovenaSummary): void {
    this.selectedNovenaId.set(novena.id);
    this.selectedNovenaSlug.set(novena.slug);
    this.selectedNovenaDayManuallyChanged.set(false);
    this.selectedNovenaDayNumber.set(this.novenaProgressForNovena(novena.id, novena.slug)?.currentDay ?? 1);
    this.pushSharedContentPath('novenas', novena.slug);
  }

  selectNovenaDay(dayNumber: number): void {
    this.selectedNovenaDayManuallyChanged.set(true);
    this.selectedNovenaDayNumber.set(dayNumber);
  }

  startSelectedNovena(): void {
    const detail = this.novenaDetail();
    if (!detail || !this.isAuthenticated()) {
      return;
    }

    const progress: LocalNovenaProgress = {
      novenaId: detail.id,
      startedAt: new Date().toISOString(),
      currentDay: 1,
      completedDays: [],
      status: 'active',
    };
    this.saveLocalNovenaProgress(progress);
    this.selectedNovenaDayManuallyChanged.set(false);
    this.selectedNovenaDayNumber.set(1);
    this.syncNovenaProgress(progress);
  }

  stopSelectedNovena(): void {
    const detail = this.novenaDetail();
    if (!detail || !this.isAuthenticated()) {
      return;
    }

    this.localNovenaProgress.update((current) => {
      const next = { ...current };
      delete next[detail.id];
      this.persistLocalNovenaProgress(next);
      return next;
    });
    this.api.deleteNovenaCommitment(detail.id).subscribe({
      next: () => {
        this.refreshUserCollections();
      },
      error: () => undefined,
    });
  }

  completeSelectedNovenaDay(): void {
    const detail = this.novenaDetail();
    const selectedDay = this.selectedNovenaDay();
    const existing = this.selectedNovenaProgress();
    if (!detail || !selectedDay || !existing || !this.isAuthenticated()) {
      return;
    }

    const completedDays = Array.from(new Set([...existing.completedDays, selectedDay.dayNumber])).sort((left, right) => left - right);
    const isComplete = completedDays.length >= detail.days.length;
    const nextDay = isComplete ? selectedDay.dayNumber : Math.min(selectedDay.dayNumber + 1, detail.days.length);
    const progress: LocalNovenaProgress = {
      ...existing,
      currentDay: nextDay,
      completedDays,
      status: isComplete ? 'completed' : 'active',
    };

    this.saveLocalNovenaProgress(progress);
    this.syncNovenaProgress(progress);

    if (isComplete) {
      this.completedNovenaTitle.set(detail.title);
      return;
    }

    this.selectedNovenaDayManuallyChanged.set(false);
    this.selectedNovenaDayNumber.set(nextDay);
  }

  closeCompletionModal(): void {
    this.completedNovenaTitle.set(null);
  }

  toggleSelectedSaintFavorite(): void {
    const saint = this.saintDetail();
    if (saint) {
      this.toggleFavorite('saint', saint.id);
    }
  }

  toggleSelectedNovenaFavorite(): void {
    const novena = this.novenaDetail();
    if (novena) {
      this.toggleFavorite('novena', novena.id);
    }
  }

  toggleSelectedPrayerFavorite(): void {
    const prayer = this.prayerDetail();
    if (prayer) {
      this.toggleFavorite('prayer', prayer.id);
    }
  }

  closeDetailModal(): void {
    this.selectedSaintSlug.set(null);
    this.selectedPrayerSlug.set(null);
    this.selectedNovenaSlug.set(null);
    this.selectedNovenaId.set(null);
    this.selectedNovenaDayManuallyChanged.set(false);
    this.selectedNovenaDayNumber.set(1);
    if (typeof window !== 'undefined' && window.location.pathname !== '/') {
      window.history.pushState({}, '', '/');
    }
  }

  private openSharedContentFromPath(pathname: string, updateHistory: boolean): void {
    const [kind, slug] = pathname.split('/').filter(Boolean);
    if (kind === 'privacy' || kind === 'support') {
      this.selectedSaintSlug.set(null);
      this.selectedPrayerSlug.set(null);
      this.selectedNovenaSlug.set(null);
      this.selectedNovenaId.set(null);
      this.activeLegalDocument.set(kind);
      this.setTab('about');
      return;
    }

    if (!kind || !slug) {
      if (!updateHistory) {
        this.selectedSaintSlug.set(null);
        this.selectedPrayerSlug.set(null);
        this.selectedNovenaSlug.set(null);
        this.selectedNovenaId.set(null);
        this.activeLegalDocument.set(null);
      }
      return;
    }

    this.activeLegalDocument.set(null);
    this.suppressNextHistoryUpdate = !updateHistory;
    switch (kind) {
      case 'saints':
        this.setTab('saints');
        this.selectedSaintSlug.set(decodeURIComponent(slug));
        break;
      case 'novenas':
        this.setTab('novenas');
        this.selectedNovenaSlug.set(decodeURIComponent(slug));
        this.selectedNovenaId.set(null);
        this.selectedNovenaDayManuallyChanged.set(false);
        this.selectedNovenaDayNumber.set(1);
        break;
      case 'prayers':
        this.setTab('prayers');
        this.selectedPrayerSlug.set(decodeURIComponent(slug));
        break;
      default:
        this.suppressNextHistoryUpdate = false;
    }
  }

  private pushSharedContentPath(kind: 'saints' | 'novenas' | 'prayers', slug: string): void {
    if (this.suppressNextHistoryUpdate) {
      this.suppressNextHistoryUpdate = false;
      return;
    }

    if (typeof window === 'undefined') {
      return;
    }

    const path = `/${kind}/${encodeURIComponent(slug)}`;
    if (window.location.pathname !== path) {
      window.history.pushState({}, '', path);
    }
  }

  private pushPath(path: string): void {
    if (typeof window !== 'undefined' && window.location.pathname !== path) {
      window.history.pushState({}, '', path);
    }
  }

  showDailyTab(): void {
    const todayReadingsUrl = this.todayLiturgicalDay()?.readingsUrl ?? null;
    if (todayReadingsUrl && typeof window !== 'undefined') {
      window.open(todayReadingsUrl, '_blank', 'noopener,noreferrer');
      return;
    }

    this.setTab('liturgical');
    this.resetViewportToTop();
  }

  openIntentions(): void {
    this.setTab('intentions');
    this.termQuery.set('');
    this.selectedTerm.set(null);
    this.selectedIntentionNovenas.set([]);
    this.selectedPatronageSaints.set([]);
    this.termLoadFailed.set(false);
    this.resetViewportToTop();
  }

  openPatronage(): void {
    this.setTab('patronage');
    this.termQuery.set('');
    this.selectedTerm.set(null);
    this.selectedIntentionNovenas.set([]);
    this.selectedPatronageSaints.set([]);
    this.termLoadFailed.set(false);
    this.resetViewportToTop();
  }

  updateTermQuery(query: string): void {
    this.termQuery.set(query);
    this.selectedTerm.set(null);
    this.selectedIntentionNovenas.set([]);
    this.selectedPatronageSaints.set([]);
    this.termLoadFailed.set(false);
  }

  clearSelectedTerm(): void {
    this.selectedTerm.set(null);
    this.selectedIntentionNovenas.set([]);
    this.selectedPatronageSaints.set([]);
  }

  selectTerm(term: SearchTerm): void {
    this.selectedTerm.set(term);
    this.termLoadFailed.set(false);
    if (this.currentTab() === 'patronage') {
      this.api.getSaintsByPatronage(this.apiLanguage(this.language()), term.key).pipe(
        catchError(() => {
          this.termLoadFailed.set(true);
          return of<SaintSummary[]>([]);
        }),
      ).subscribe((saints) => {
        if (saints.length === 1) {
          this.openSaintDetail(saints[0]);
          return;
        }
        this.selectedPatronageSaints.set(saints);
      });
      return;
    }

    this.api.getNovenasByIntention(this.apiLanguage(this.language()), term.key).pipe(
      catchError(() => {
        this.termLoadFailed.set(true);
        return of<NovenaSummary[]>([]);
      }),
    ).subscribe((novenas) => {
      if (novenas.length === 1) {
        this.openNovenaDetail(novenas[0]);
        return;
      }
      this.selectedIntentionNovenas.set(novenas);
    });
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
    this.novenasLoadFailed.set(false);
    this.resetViewportToTop();
  }

  browseSaintsCalendar(): void {
    this.setTab('saints');
    this.saintsMode.set('calendar');
  }

  openSaintsList(): void {
    this.setTab('saints');
    this.saintsMode.set('list');
    this.saintQuery.set('');
    this.saintsLoadFailed.set(false);
    this.resetViewportToTop();
  }

  openPrayersList(): void {
    this.setTab('prayers');
    this.prayerQuery.set('');
    this.prayersLoadFailed.set(false);
    this.resetViewportToTop();
  }

  openRosariesList(): void {
    this.setTab('rosaries');
    this.rosaryQuery.set('');
    this.prayersLoadFailed.set(false);
    this.resetViewportToTop();
  }

  localizedSaintsCountLabel(): string {
    return this.translate(
      'Selected day · Featured saint',
      'Día seleccionado · Santo destacado',
      'Wybrany dzień · Wyróżniony święty'
    );
  }

  localizedNovenasCountLabel(): string {
    return this.translate(
      'Selected day · Featured novena',
      'Día seleccionado · Novena destacada',
      'Wybrany dzień · Wyróżniona nowenna'
    );
  }

  localizedNoSaintsCopy(): string {
    return this.translate(
      'No saints are assigned to this feast day in the imported legacy data.',
      'No hay santos asignados a este día de fiesta en los datos heredados importados.',
      'W zaimportowanych danych nie przypisano świętych do tego dnia święta.'
    );
  }

  localizedNoNovenasCopy(): string {
    return this.translate(
      'Sanctuary does not have a novena starting on this day.',
      'Sanctuary no tiene una novena que comience este día.',
      'Sanctuary nie ma nowenny rozpoczynającej się tego dnia.'
    );
  }

  localizedNovenaSearchPlaceholder(): string {
    return this.translate('Search novenas', 'Buscar novenas', 'Szukaj nowenn');
  }

  localizedSaintResultsLabel(): string {
    return this.translate(
      `${this.saintResults().length} saints`,
      `${this.saintResults().length} santos`,
      `${this.saintResults().length} świętych`
    );
  }

  localizedPrayerResultsLabel(): string {
    return this.translate(
      `${this.prayerResults().length} prayers`,
      `${this.prayerResults().length} oraciones`,
      `${this.prayerResults().length} modlitw`
    );
  }

  localizedPrayerTitle(): string {
    return this.translate('Prayers', 'Oraciones', 'Modlitwy');
  }

  localizedPrayerSearchPlaceholder(): string {
    return this.translate('Search prayers', 'Buscar oraciones', 'Szukaj modlitw');
  }

  localizedRosaryResultsLabel(): string {
    return this.translate(
      `${this.rosaryResults().length} rosaries`,
      `${this.rosaryResults().length} rosarios`,
      `${this.rosaryResults().length} różańców`
    );
  }

  localizedRosaryTitle(): string {
    return this.translate('Rosary', 'Rosario', 'Różaniec');
  }

  localizedRosarySearchPlaceholder(): string {
    return this.translate('Search rosaries', 'Buscar rosarios', 'Szukaj różańców');
  }

  localizedIntentionsResultsLabel(): string {
    return this.translate(
      `${this.novenaSearchResults().length} novenas`,
      `${this.novenaSearchResults().length} novenas`,
      `${this.novenaSearchResults().length} nowenn`
    );
  }

  localizedTermResultsLabel(): string {
    const tab = this.currentTab();
    if (this.selectedTerm()) {
      const count = tab === 'patronage'
        ? this.selectedPatronageSaints().length
        : this.selectedIntentionNovenas().length;
      return this.translate(
        `${count} ${count === 1 ? 'result' : 'results'}`,
        `${count} ${count === 1 ? 'resultado' : 'resultados'}`,
        `${count} wyników`
      );
    }

    const count = tab === 'patronage' ? this.patronageTerms().length : this.intentionTerms().length;
    return this.translate(
      `${count} ${count === 1 ? 'term' : 'terms'}`,
      `${count} ${count === 1 ? 'término' : 'términos'}`,
      `${count} haseł`
    );
  }

  localizedTermEmptyCopy(): string {
    return this.currentTab() === 'patronage'
      ? this.translate(
        'Search for a patronage, then choose it to find the matching saint or saints.',
        'Busca un patronazgo y elígelo para encontrar el santo o los santos relacionados.',
        'Wyszukaj patronat, a potem wybierz go, aby znaleźć świętego lub świętych.'
      )
      : this.translate(
        'Search for an intention, then choose it to find the matching novena or novenas.',
        'Busca una intención y elígela para encontrar la novena o las novenas relacionadas.',
        'Wyszukaj intencję, a potem wybierz ją, aby znaleźć nowennę lub nowenny.'
      );
  }

  localizedPreviewTitle(mode: 'today' | 'selected'): string {
    return mode === 'today'
      ? this.translate('Today', 'Hoy', 'Dzisiaj')
      : this.translate('Selected Day', 'Día seleccionado', 'Wybrany dzień');
  }

  localizedNoLiturgicalCopy(): string {
    return this.translate(
      'No liturgical summary is available for this day.',
      'No hay un resumen litúrgico disponible para este día.',
      'Brak podsumowania liturgicznego dla tego dnia.'
    );
  }

  localizedSelectedSameAsTodayCopy(): string {
    return this.translate(
      'Selected day matches today.',
      'El día seleccionado coincide con hoy.',
      'Wybrany dzień jest taki sam jak dzisiaj.'
    );
  }

  localizedIntentionsEmptyCopy(): string {
    return this.translate(
      'Browse the novena library or search for a specific novena.',
      'Explora la biblioteca de novenas o busca una novena específica.',
      'Przeglądaj bibliotekę nowenn lub wyszukaj konkretną nowennę.'
    );
  }

  localizedApiErrorCopy(subject: 'saints' | 'liturgical' | 'novenas' | 'prayers'): string {
    switch (subject) {
      case 'saints':
        return this.translate(
          'We could not load saints from the API right now.',
          'No pudimos cargar los santos desde la API en este momento.',
          'Nie można teraz załadować świętych z API.'
        );
      case 'liturgical':
        return this.translate(
          'We could not load the liturgical day from the API right now.',
          'No pudimos cargar el día litúrgico desde la API en este momento.',
          'Nie można teraz załadować dnia liturgicznego z API.'
        );
      case 'novenas':
        return this.translate(
          'We could not load novenas from the API right now.',
          'No pudimos cargar las novenas desde la API en este momento.',
          'Nie można teraz załadować nowenn z API.'
        );
      case 'prayers':
        return this.translate(
          'We could not load prayers from the API right now.',
          'No pudimos cargar las oraciones desde la API en este momento.',
          'Nie można teraz załadować modlitw z API.'
        );
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
    return new Intl.DateTimeFormat(this.dateLocale(), {
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
      return this.todayNovenasGroup()?.startingNovena ?? null;
    }

    const day = this.novenasByDate().get(date);
    return day?.startingNovena ?? null;
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

  private isFavorite(itemType: 'saint' | 'novena' | 'prayer', itemId: string): boolean {
    if (!this.isAuthenticated()) {
      return false;
    }

    const override = this.favoriteOverrides()[this.favoriteKey(itemType, itemId)];
    if (override !== undefined) {
      return override;
    }

    return this.userFavorites().some((favorite) => favorite.itemType === itemType && favorite.itemId === itemId);
  }

  private toggleFavorite(itemType: 'saint' | 'novena' | 'prayer', itemId: string): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const currentlyFavorite = this.isFavorite(itemType, itemId);
    this.favoriteOverrides.update((current) => ({ ...current, [this.favoriteKey(itemType, itemId)]: !currentlyFavorite }));

    const request = currentlyFavorite
      ? this.api.deleteFavorite(itemType, itemId)
      : this.api.saveFavorite(itemType, itemId);
    request.subscribe({
      next: () => {
        this.refreshUserCollections();
      },
      error: () => undefined,
    });
  }

  private favoriteCount(itemType: 'saint' | 'novena' | 'prayer'): number {
    const ids = new Set(
      this.userFavorites().filter((favorite) => favorite.itemType === itemType).map((favorite) => favorite.itemId),
    );
    for (const [key, value] of Object.entries(this.favoriteOverrides())) {
      const [type, id] = key.split(':');
      if (type !== itemType || !id) {
        continue;
      }
      if (value) {
        ids.add(id);
      } else {
        ids.delete(id);
      }
    }
    return ids.size;
  }

  private favoriteKey(itemType: 'saint' | 'novena' | 'prayer', itemId: string): string {
    return `${itemType}:${itemId}`;
  }

  private novenaProgressForNovena(novenaId: string | null | undefined, novenaSlug?: string | null): LocalNovenaProgress | null {
    const candidateIds = [novenaId, novenaSlug].filter((value): value is string => Boolean(value));
    if (!candidateIds.length) {
      return null;
    }

    const serverCommitment = this.userNovenaCommitments().find((commitment) => candidateIds.includes(commitment.novenaId));
    if (serverCommitment) {
      return this.toLocalNovenaProgress(serverCommitment);
    }

    for (const candidateId of candidateIds) {
      const progress = this.localNovenaProgress()[candidateId];
      if (progress) {
        return progress;
      }
    }

    return null;
  }

  private toLocalNovenaProgress(commitment: UserNovenaCommitment): LocalNovenaProgress {
    return {
      novenaId: commitment.novenaId,
      startedAt: commitment.startedAt,
      currentDay: commitment.currentDay,
      completedDays: commitment.completedDays,
      status: commitment.status,
    };
  }

  private saveLocalNovenaProgress(progress: LocalNovenaProgress): void {
    this.localNovenaProgress.update((current) => {
      const next = { ...current, [progress.novenaId]: progress };
      this.persistLocalNovenaProgress(next);
      return next;
    });
  }

  private syncNovenaProgress(progress: LocalNovenaProgress): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const request: UserNovenaCommitmentRequest = {
      startedAt: progress.startedAt,
      currentDay: progress.currentDay,
      completedDays: progress.completedDays,
      reminderEnabled: false,
      reminderMorningHour: null,
      reminderEveningHour: null,
      reminderTimeZoneId: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
      status: progress.status,
    };
    this.api.saveNovenaCommitment(progress.novenaId, request).subscribe({
      next: () => {
        this.refreshUserCollections();
      },
      error: () => undefined,
    });
  }

  openMeActiveNovena(item: MeLinkedItem): void {
    const novena = this.meNovenaCatalog().find((entry) => entry.id === item.id || entry.slug === item.slug);
    if (novena) {
      this.setTab('novenas');
      this.openNovenaDetail(novena);
    }
  }

  openMeFavoriteNovena(item: MeLinkedItem): void {
    const novena = this.meNovenaCatalog().find((entry) => entry.id === item.id || entry.slug === item.slug);
    if (novena) {
      this.setTab('novenas');
      this.openNovenaDetail(novena);
    }
  }

  openMeFavoriteSaint(item: MeLinkedItem): void {
    const saint = this.meSaintCatalog().find((entry) => entry.id === item.id || entry.slug === item.slug);
    if (saint) {
      this.openSaintDetail(saint);
      this.setTab('saints');
    }
  }

  openMeFavoritePrayer(item: MeLinkedItem): void {
    const prayer = this.mePrayerCatalog().find((entry) => entry.id === item.id || entry.slug === item.slug);
    if (prayer) {
      this.setTab(prayer.category?.toLowerCase() === 'rosary' ? 'rosaries' : 'prayers');
      this.openPrayerDetail(prayer);
    }
  }

  private refreshUserCollections(): void {
    this.userProfileReloadToken.update((value) => value + 1);
    this.userCollectionsReloadToken.update((value) => value + 1);
  }

  private loadLocalNovenaProgress(): Record<string, LocalNovenaProgress> {
    try {
      return JSON.parse(localStorage.getItem(LOCAL_NOVENA_PROGRESS_KEY) ?? '{}') as Record<string, LocalNovenaProgress>;
    } catch {
      return {};
    }
  }

  private persistLocalNovenaProgress(progress: Record<string, LocalNovenaProgress>): void {
    localStorage.setItem(LOCAL_NOVENA_PROGRESS_KEY, JSON.stringify(progress));
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

  private apiLanguage(language: AppLanguage): 'en' | 'es' | 'pl' {
    return language;
  }

  private translate(english: string, spanish: string, polish: string): string {
    switch (this.language()) {
      case 'es':
        return spanish;
      case 'pl':
        return polish;
      default:
        return english;
    }
  }

  private dateLocale(): string {
    return {
      en: 'en-US',
      es: 'es-ES',
      pl: 'pl-PL',
    }[this.language()];
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

  private joinNames(firstName?: string | null, lastName?: string | null): string | null {
    if (firstName && lastName) {
      return `${firstName} ${lastName}`;
    }

    return firstName ?? lastName ?? null;
  }

  private emailName(email?: string | null): string | null {
    if (!email) {
      return null;
    }

    const localPart = email.split('@')[0];
    return localPart ? this.toTitleCase(localPart.replace(/[._-]+/g, ' ')) : null;
  }

  private looksLikeIdentifier(value?: string | null): boolean {
    if (!value) {
      return false;
    }

    return /^[0-9a-f]{8}-[0-9a-f-]{20,}$/i.test(value);
  }
}
