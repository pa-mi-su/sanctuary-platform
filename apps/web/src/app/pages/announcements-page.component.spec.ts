import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AnnouncementsPageComponent } from './announcements-page.component';
import { AnnouncementInboxService } from '../core/state/announcement-inbox.service';
const entry = (id: string, destination: string, body: string) => ({
  id,
  title: 'Sanctuary update',
  body,
  destination,
  published_at: 1794834000000,
});
describe('Announcement presentation', () => {
  it('shows full plain messages without expanders or an irrelevant content link', async () => {
    const rows = [
      entry('plain', 'https://mydailysanctuary.com/', 'A full announcement. '.repeat(20)),
    ];
    const state = signal({ readIds: ['plain'], clearedIds: [], clearedBefore: 0 });
    const inbox = {
      items: signal(rows),
      visible: signal(rows),
      state,
      busy: signal(false),
      error: signal(''),
      undo: signal(null),
      more: signal(false),
      change: vi.fn(),
      load: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: AnnouncementInboxService, useValue: inbox }],
    });
    const fixture = TestBed.createComponent(AnnouncementsPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const article = fixture.nativeElement.querySelector('article');
    expect(article.textContent).toContain(rows[0].body);
    expect(article.querySelector('[aria-expanded]')).toBeNull();
    expect(article.querySelector('a')).toBeNull();
    fixture.destroy();
  });
  it('keeps content links only for actual associated Sanctuary content', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AnnouncementInboxService, useValue: {} }],
    });
    const fixture = TestBed.createComponent(AnnouncementsPageComponent);
    const view = fixture.componentInstance;
    expect(view.hasContent('https://mydailysanctuary.com/saints/st-cecilia')).toBe(true);
    expect(view.hasContent('https://mydailysanctuary.com/novenas/st-joseph')).toBe(true);
    expect(view.hasContent('https://mydailysanctuary.com/prayers/our-father')).toBe(true);
    expect(view.hasContent('https://mydailysanctuary.com/')).toBe(false);
    expect(view.hasContent('https://untrusted.example/saints/test')).toBe(false);
    fixture.destroy();
  });
});
