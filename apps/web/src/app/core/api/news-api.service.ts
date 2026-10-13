import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SANCTUARY_API_BASE_URL } from './sanctuary-api.config';

export interface NewsArticle {
  id: string | null;
  title: string;
  category: string;
  summary: string;
  body: string;
  author: string;
  imageId: string | null;
  imageAlt: string;
  imageCaption: string;
  imageCredit: string;
  sources: string;
  status: string;
  publishAt: string | null;
  isLead: boolean;
  placement: string;
  deletedAt: string | null;
  version: number;
  updatedAt: string | null;
  corrections: { note: string; createdAt: string }[];
}
export function newNewsArticle(): NewsArticle {
  return {
    id: null,
    title: '',
    category: 'U.S. Church',
    summary: '',
    body: '',
    author: '',
    imageId: null,
    imageAlt: '',
    imageCaption: '',
    imageCredit: '',
    sources: '',
    status: 'DRAFT',
    publishAt: null,
    isLead: false,
    placement: 'MORE',
    deletedAt: null,
    version: 0,
    updatedAt: null,
    corrections: [],
  };
}
@Injectable({ providedIn: 'root' })
export class NewsApiService {
  private readonly http = inject(HttpClient);
  readonly base = inject(SANCTUARY_API_BASE_URL);
  private readonly options = {
    headers: new HttpHeaders({ 'X-Sanctuary-News-Admin': '1' }),
    withCredentials: true,
  };
  login(email: string, password: string) {
    return this.http.post<{
      state: string;
      challengeId: string | null;
      secret: string | null;
      message: string;
    }>(`${this.base}/auth/news/login`, { email, password }, this.options);
  }
  verify(challengeId: string, code: string) {
    return this.http.post<{
      state: string;
      challengeId: string | null;
      secret: string | null;
      message: string;
    }>(`${this.base}/auth/news/verify`, { challengeId, code }, this.options);
  }
  logout() {
    return this.http.post(`${this.base}/admin/news/logout`, null, this.options);
  }
  list(admin = false, trash = false) {
    return this.http.get<NewsArticle[]>(`${this.base}/${admin ? 'admin' : 'content'}/news`, {
      withCredentials: true,
      params: admin ? { trash: String(trash) } : {},
    });
  }
  save(article: NewsArticle) {
    return article.id
      ? this.http.put<NewsArticle>(`${this.base}/admin/news/${article.id}`, article, this.options)
      : this.http.post<NewsArticle>(`${this.base}/admin/news`, article, this.options);
  }
  trash(article: NewsArticle) {
    return this.http.delete<NewsArticle>(
      `${this.base}/admin/news/${article.id}?version=${article.version}`,
      this.options,
    );
  }
  restore(article: NewsArticle) {
    return this.http.post<NewsArticle>(
      `${this.base}/admin/news/${article.id}/restore?version=${article.version}`,
      null,
      this.options,
    );
  }
  upload(file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ id: string }>(`${this.base}/admin/news/images`, form, this.options);
  }
  correction(id: string, note: string) {
    return this.http.post<NewsArticle>(
      `${this.base}/admin/news/${id}/corrections`,
      { note },
      this.options,
    );
  }
  image(id: string, admin = false) {
    return `${this.base}/${admin ? 'admin' : 'content'}/news/images/${id}`;
  }
}
