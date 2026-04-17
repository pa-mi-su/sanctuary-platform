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

@Injectable({ providedIn: 'root' })
export class SanctuaryApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(SANCTUARY_API_BASE_URL);

  getLiturgicalDay(date: string): Observable<LiturgicalDayResponse> {
    return this.http.get<LiturgicalDayResponse>(`${this.apiBaseUrl}/calendar/day/${date}`);
  }

  getSaintsByDate(date: string, language: 'en' | 'es' | 'pl'): Observable<SaintSummary[]> {
    const [year, month, day] = date.split('-');
    const params = new HttpParams()
      .set('month', String(Number(month)))
      .set('day', String(Number(day)))
      .set('lang', language);

    if (!year || !month || !day) {
      throw new Error(`Invalid date passed to getSaintsByDate: ${date}`);
    }

    return this.http.get<SaintSummary[]>(`${this.apiBaseUrl}/content/saints`, { params });
  }
}
