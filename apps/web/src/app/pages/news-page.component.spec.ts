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
  it('keeps the Vatican lead and U.S. sidebar in their assigned positions on every tab', () => {
    window.history.replaceState({}, '', '/news');
    const records = [
      {
        ...newNewsArticle(),
        id: 'side',
        title: 'U.S. sidebar',
        category: 'U.S. Church',
        placement: 'SIDEBAR_1',
        status: 'PUBLISHED',
      },
      {
        ...newNewsArticle(),
        id: 'lead',
        title: 'Vatican lead',
        category: 'Vatican',
        placement: 'LEAD',
        status: 'PUBLISHED',
      },
      {
        ...newNewsArticle(),
        id: 'more',
        title: 'Vatican more',
        category: 'Vatican',
        placement: 'MORE',
        status: 'PUBLISHED',
      },
    ];
    TestBed.configureTestingModule({
      providers: [{ provide: NewsApiService, useValue: { list: () => of(records) } }],
    });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    expect(page.homepage().lead?.id).toBe('lead');
    expect(page.homepage().side.map((s) => s.id)).toEqual(['side']);
    page.selectCategory('Vatican');
    expect(page.homepage().lead?.id).toBe('lead');
    expect(page.homepage().side).toEqual([]);
    expect(page.homepage().more.map((s) => s.id)).toEqual(['more']);
    page.selectCategory('U.S. Church');
    expect(page.homepage().lead).toBeUndefined();
    expect(page.homepage().side.map((s) => s.id)).toEqual(['side']);
    expect(page.homepage().more).toEqual([]);
  });
  it('renders a lone sidebar without a lead card and does not promote More stories', async () => {
    window.history.replaceState({}, '', '/news');
    const records = [
      {
        ...newNewsArticle(),
        id: 'side',
        title: 'Sidebar',
        category: 'U.S. Church',
        placement: 'SIDEBAR_1',
        status: 'PUBLISHED',
      },
      {
        ...newNewsArticle(),
        id: 'more',
        title: 'More',
        category: 'Vatican',
        placement: 'MORE',
        status: 'PUBLISHED',
      },
    ];
    TestBed.configureTestingModule({
      imports: [NewsPageComponent],
      providers: [{ provide: NewsApiService, useValue: { list: () => of(records) } }],
    });
    const fixture = TestBed.createComponent(NewsPageComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.lead')).toBeNull();
    expect(
      fixture.nativeElement.querySelector('.front-page.no-lead .middle-column h3').textContent,
    ).toBe('Sidebar');
    expect(fixture.nativeElement.querySelector('.more-section h3').textContent).toBe('More');
    fixture.componentInstance.selectCategory('Vatican');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.lead')).toBeNull();
    expect(fixture.nativeElement.querySelector('.middle-column')).toBeNull();
    expect(fixture.nativeElement.querySelector('.more-section h3').textContent).toBe('More');
  });
  it('clears an article deep link when returning to a section', () => {
    window.history.replaceState({}, '', '/news?article=side&api=dev');
    const record = {
      ...newNewsArticle(),
      id: 'side',
      title: 'Sidebar',
      placement: 'SIDEBAR_1',
      status: 'PUBLISHED',
    };
    TestBed.configureTestingModule({
      providers: [{ provide: NewsApiService, useValue: { list: () => of([record]) } }],
    });
    const page = TestBed.runInInjectionContext(() => new NewsPageComponent());
    expect(page.selectedStory()?.id).toBe('side');
    page.selectCategory('U.S. Church');
    expect(page.selectedStory()).toBeNull();
    expect(new URLSearchParams(window.location.search).has('article')).toBe(false);
    expect(new URLSearchParams(window.location.search).get('api')).toBe('dev');
    window.history.replaceState({}, '', '/');
  });
});
