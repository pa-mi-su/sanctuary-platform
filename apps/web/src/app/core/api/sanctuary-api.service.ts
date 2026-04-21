import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { SANCTUARY_API_BASE_URL } from './sanctuary-api.config';

export interface LiturgicalDayResponse {
  date: string;
  season: string;
  primaryRank: string;
  observances: string[];
  readingsUrl: string;
  rankType: string;
}

export interface SaintDateGroup {
  date: string;
  saints: SaintSummary[];
}

export interface SaintSummary {
  id: string;
  slug: string;
  name: string;
  feastMonth: number;
  feastDay: number;
  feastLabel: string;
  summary: string;
  imageUrl: string | null;
}

export interface SaintSource {
  text: string;
  url: string | null;
}

export interface SaintDetail extends SaintSummary {
  biography: string;
  sources: SaintSource[];
}

export interface PrayerSummary {
  id: string;
  slug: string;
  title: string;
  bodyPreview: string;
  category: string;
  imageUrl: string | null;
}

export interface PrayerDetail {
  id: string;
  slug: string;
  title: string;
  alternateTitle: string;
  body: string;
  note: string;
  category: string;
  imageUrl: string | null;
  sourceTitle: string;
  sourceType: string;
  tags: string[];
}

export interface NovenaSummary {
  id: string;
  slug: string;
  title: string;
  description: string;
  durationDays: number;
  imageUrl: string | null;
}

export interface NovenaDayDetail {
  dayNumber: number;
  title: string;
  scripture: string;
  prayer: string;
  reflection: string;
  body: string;
}

export interface NovenaDetail extends NovenaSummary {
  tags: string[];
  intentions: string[];
  days: NovenaDayDetail[];
}

export interface NovenaCalendarDateResponse {
  date: string;
  novenas: NovenaSummary[];
  startingNovena: NovenaSummary | null;
}

export interface UserProfile {
  userId: string;
  email: string | null;
  displayName: string | null;
  avatarUrl?: string | null;
  preferredLanguage?: 'en' | 'es' | 'pl' | null;
  timeZoneId?: string | null;
  novenaRemindersEnabled?: boolean;
  feastRemindersEnabled?: boolean;
  emailUpdatesEnabled?: boolean;
  onboardingCompleted?: boolean;
  favoriteSaintCount?: number;
  favoriteNovenaCount?: number;
  favoritePrayerCount?: number;
  activeNovenaCount?: number;
  completedNovenaCount?: number;
  currentStreakDays?: number;
  longestStreakDays?: number;
  lastActiveDate?: string | null;
}

export interface UserPreferencesUpdateRequest {
  preferredLanguage: 'en' | 'es' | 'pl';
  timeZoneId: string;
  novenaRemindersEnabled: boolean;
  feastRemindersEnabled: boolean;
  emailUpdatesEnabled: boolean;
  onboardingCompleted: boolean;
}

export interface UserFavorite {
  itemType: 'saint' | 'novena' | 'prayer';
  itemId: string;
  createdAt: string;
}

export interface UserNovenaCommitment {
  novenaId: string;
  startedAt: string;
  currentDay: number;
  completedDays: number[];
  reminderEnabled: boolean;
  reminderMorningHour: number | null;
  reminderEveningHour: number | null;
  reminderTimeZoneId: string;
  status: 'active' | 'paused' | 'completed';
  updatedAt: string;
}

export interface UserNovenaCommitmentRequest {
  startedAt: string;
  currentDay: number;
  completedDays: number[];
  reminderEnabled: boolean;
  reminderMorningHour: number | null;
  reminderEveningHour: number | null;
  reminderTimeZoneId: string;
  status: 'active' | 'paused' | 'completed';
}

@Injectable({ providedIn: 'root' })
export class SanctuaryApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(SANCTUARY_API_BASE_URL);

  getLiturgicalDay(date: string): Observable<LiturgicalDayResponse> {
    return this.http.get<LiturgicalDayResponse>(`${this.apiBaseUrl}/calendar/day/${date}`);
  }

  getSaintsByDate(date: string, language: 'en' | 'es' | 'pl'): Observable<SaintSummary[]> {
    const [year, month, day] = date.split('-');
    if (!year || !month || !day) {
      throw new Error(`Invalid date passed to getSaintsByDate: ${date}`);
    }

    const params = new HttpParams()
      .set('month', String(Number(month)))
      .set('day', String(Number(day)))
      .set('lang', language);

    return this.http.get<SaintSummary[]>(`${this.apiBaseUrl}/content/saints`, { params });
  }

  getSaintsByRange(start: string, end: string, language: 'en' | 'es' | 'pl'): Observable<SaintDateGroup[]> {
    return this.http.get<SaintDateGroup[]>(`${this.apiBaseUrl}/content/saints/range`, {
      params: this.rangeParams(start, end).set('lang', language),
    });
  }

  listSaints(language: 'en' | 'es' | 'pl', query: string): Observable<SaintSummary[]> {
    return this.http.get<SaintSummary[]>(`${this.apiBaseUrl}/content/saints/search`, {
      params: new HttpParams().set('lang', language).set('query', query),
    });
  }

  getSaintDetail(slug: string, language: 'en' | 'es' | 'pl'): Observable<SaintDetail> {
    return this.http.get<SaintDetail>(`${this.apiBaseUrl}/content/saints/${slug}`, {
      params: new HttpParams().set('lang', language),
    });
  }

  getLiturgicalRange(start: string, end: string): Observable<LiturgicalDayResponse[]> {
    return this.http.get<LiturgicalDayResponse[]>(`${this.apiBaseUrl}/calendar/range`, {
      params: this.rangeParams(start, end),
    });
  }

  listPrayers(language: 'en' | 'es' | 'pl', query: string): Observable<PrayerSummary[]> {
    return this.http.get<PrayerSummary[]>(`${this.apiBaseUrl}/content/prayers`, {
      params: new HttpParams().set('lang', language).set('query', query),
    });
  }

  getPrayerDetail(slug: string, language: 'en' | 'es' | 'pl'): Observable<PrayerDetail> {
    return this.http.get<PrayerDetail>(`${this.apiBaseUrl}/content/prayers/${slug}`, {
      params: new HttpParams().set('lang', language),
    });
  }

  listNovenas(language: 'en' | 'es' | 'pl', query: string): Observable<NovenaSummary[]> {
    return this.http.get<NovenaSummary[]>(`${this.apiBaseUrl}/content/novenas`, {
      params: new HttpParams().set('lang', language).set('query', query),
    });
  }

  listNovenaIntentions(language: 'en' | 'es' | 'pl', query: string): Observable<NovenaSummary[]> {
    return this.http.get<NovenaSummary[]>(`${this.apiBaseUrl}/content/novenas/intentions`, {
      params: new HttpParams().set('lang', language).set('query', query),
    });
  }

  getNovenaDetail(slug: string, language: 'en' | 'es' | 'pl'): Observable<NovenaDetail> {
    return this.http.get<NovenaDetail>(`${this.apiBaseUrl}/content/novenas/${slug}`, {
      params: new HttpParams().set('lang', language),
    });
  }

  getNovenasByRange(start: string, end: string, language: 'en' | 'es' | 'pl'): Observable<NovenaCalendarDateResponse[]> {
    return this.http.get<NovenaCalendarDateResponse[]>(`${this.apiBaseUrl}/content/novenas/calendar`, {
      params: this.rangeParams(start, end).set('lang', language),
    });
  }

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiBaseUrl}/me`);
  }

  updateMePreferences(request: UserPreferencesUpdateRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiBaseUrl}/me/preferences`, request);
  }

  listFavorites(): Observable<UserFavorite[]> {
    return this.http.get<UserFavorite[]>(`${this.apiBaseUrl}/me/favorites`);
  }

  listNovenaCommitments(): Observable<UserNovenaCommitment[]> {
    return this.http.get<UserNovenaCommitment[]>(`${this.apiBaseUrl}/me/novena-commitments`);
  }

  saveFavorite(itemType: 'saint' | 'novena' | 'prayer', itemId: string): Observable<void> {
    return this.http.put<void>(`${this.apiBaseUrl}/me/favorites/${itemType}/${itemId}`, null);
  }

  deleteFavorite(itemType: 'saint' | 'novena' | 'prayer', itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/me/favorites/${itemType}/${itemId}`);
  }

  saveNovenaCommitment(novenaId: string, request: UserNovenaCommitmentRequest): Observable<UserNovenaCommitment> {
    return this.http.put<UserNovenaCommitment>(`${this.apiBaseUrl}/me/novena-commitments/${novenaId}`, request);
  }

  deleteNovenaCommitment(novenaId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/me/novena-commitments/${novenaId}`);
  }

  private rangeParams(start: string, end: string): HttpParams {
    return new HttpParams().set('start', start).set('end', end);
  }
}
