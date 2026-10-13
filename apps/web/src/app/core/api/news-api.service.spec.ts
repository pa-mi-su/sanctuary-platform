import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NewsApiService, newNewsArticle } from './news-api.service';
import { SANCTUARY_API_BASE_URL } from './sanctuary-api.config';

describe('News API HTTP contract', () => {
  let api: NewsApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SANCTUARY_API_BASE_URL, useValue: 'http://127.0.0.1:8080' },
      ],
    });
    api = TestBed.inject(NewsApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('sends the trash filter to the protected API', () => {
    api.list(true, true).subscribe();
    const request = http.expectOne('http://127.0.0.1:8080/admin/news?trash=true');
    expect(request.request.withCredentials).toBe(true);
    request.flush([]);
    api.list(true, false).subscribe();
    http.expectOne('http://127.0.0.1:8080/admin/news?trash=false').flush([]);
  });
  it('keeps the public article request separate from admin filters', () => {
    api.list().subscribe();
    http.expectOne('http://127.0.0.1:8080/content/news').flush([]);
  });
  it('sends authenticated versioned trash and restore requests with the mutation header', () => {
    const article = { ...newNewsArticle(), id: 'article-id', version: 7 };
    api.trash(article).subscribe();
    let request = http.expectOne('http://127.0.0.1:8080/admin/news/article-id?version=7');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.headers.get('X-Sanctuary-News-Admin')).toBe('1');
    expect(request.request.withCredentials).toBe(true);
    request.flush(article);
    api.restore(article).subscribe();
    request = http.expectOne('http://127.0.0.1:8080/admin/news/article-id/restore?version=7');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Sanctuary-News-Admin')).toBe('1');
    request.flush(article);
  });
});
