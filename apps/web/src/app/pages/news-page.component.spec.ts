import { throwError, of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { newNewsArticle } from '../core/api/news-api.service';
import { NewsApiService } from '../core/api/news-api.service';
import { NewsPageComponent } from './news-page.component';
import { describe, it, expect } from 'vitest';

describe('News preview', () => {
  it('filters sample stories by category and resets an open article', () => {
    window.history.replaceState({}, '', '/news?preview=1');
    TestBed.configureTestingModule({ providers: [{ provide: NewsApiService, useValue: {} }] });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    window.history.replaceState({}, '', '/');
    expect(page.visibleStories()).toHaveLength(3);
    page.selectedStory.set(page.allStories[0]);
    page.selectCategory('Vatican');
    expect(page.selectedStory()).toBeNull();
    expect(page.visibleStories().map((story) => story.category)).toEqual(['Vatican']);
    page.selectCategory('Latest');
    expect(page.visibleStories()).toHaveLength(3);
  });
  it('never replaces a backend failure with sample reporting', () => {
    window.history.replaceState({}, '', '/news');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: NewsApiService,
          useValue: { list: () => throwError(() => new Error('offline')) },
        },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    expect(page.loadFailed()).toBe(true);
    expect(page.loading()).toBe(false);
    expect(page.visibleStories()).toEqual([]);
    window.history.replaceState({}, '', '/');
  });
  it('uses only server-published articles on the real page', () => {
    window.history.replaceState({}, '', '/news');
    TestBed.configureTestingModule({
      providers: [{ provide: NewsApiService, useValue: { list: () => of([]) } }],
    });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    expect(page.isMock).toBe(false);
    expect(page.visibleStories()).toEqual([]);
    expect(page.loadFailed()).toBe(false);
    window.history.replaceState({}, '', '/');
  });
  it('uses explicit homepage slots and never promotes More stories automatically', () => {
    window.history.replaceState({}, '', '/news');
    const records = ['MORE', 'SIDEBAR_2', 'LEAD', 'SIDEBAR_1'].map((placement, index) => ({
      ...newNewsArticle(),
      id: String(index),
      title: placement,
      status: 'PUBLISHED',
      placement,
    }));
    TestBed.configureTestingModule({
      providers: [{ provide: NewsApiService, useValue: { list: () => of(records) } }],
    });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    expect(page.homepage().lead?.title).toBe('LEAD');
    expect(page.homepage().side.map((s) => s.title)).toEqual(['SIDEBAR_1', 'SIDEBAR_2']);
    expect(page.homepage().more.map((s) => s.title)).toEqual(['MORE']);
    window.history.replaceState({}, '', '/');
  });
});
