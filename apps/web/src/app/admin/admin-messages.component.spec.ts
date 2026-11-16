import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';
import { AdminMessagesComponent } from './admin-messages.component';
describe('Admin messaging', () => {
  let component: AdminMessagesComponent;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SANCTUARY_API_BASE_URL, useValue: 'https://api.example.test' },
      ],
    });
    component = TestBed.runInInjectionContext(() => new AdminMessagesComponent());
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('dismisses notices without discarding the draft or review', async () => {
    const fixture = TestBed.createComponent(AdminMessagesComponent);
    const view = fixture.componentInstance;
    view.title = 'Draft invitation';
    view.body = 'Keep this message';
    view.preview = { recipientVersion: 'reviewed' };
    view.confirmed = true;
    view.notice.set('No old history to clear.');
    fixture.detectChanges();
    http.expectOne('https://api.example.test/admin-api/messages?page=0').flush({
      enabled: true,
      testDevices: [],
      items: [],
      hasMore: false,
    });
    await fixture.whenStable();
    view.historyError.set('History could not refresh.');
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('[aria-label="Dismiss message"]')!.click();
    root.querySelector<HTMLButtonElement>('[aria-label="Dismiss history error"]')!.click();
    fixture.detectChanges();
    expect(root.querySelector('[aria-label="Dismiss message"]')).toBeNull();
    expect(root.querySelector('[aria-label="Dismiss history error"]')).toBeNull();
    expect(view.title).toBe('Draft invitation');
    expect(view.body).toBe('Keep this message');
    expect(view.preview.recipientVersion).toBe('reviewed');
    expect(view.confirmed).toBe(true);
    fixture.destroy();
  });

  it('defaults to owner test only and invalidates review after editing', async () => {
    expect(component.audience).toBe('test');
    component.title = 'Prayer';
    component.body = 'Take a moment';
    component.testDevice = 'device';
    const review = component.review();
    const request = http.expectOne('https://api.example.test/admin-api/messages/preview');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-Sanctuary-Admin')).toBe('1');
    request.flush({
      count: 1,
      recipientVersion: 'snapshot',
      destination: 'https://mydailysanctuary.com/',
    });
    await review;
    component.confirmed = true;
    component.invalidate();
    expect(component.preview).toBeNull();
    expect(component.confirmed).toBe(false);
    await component.send();
    http.expectNone('https://api.example.test/admin-api/messages');
  });
  it('keeps the send identifier across a network failure to avoid duplicate broadcasts', async () => {
    component.preview = { recipientVersion: 'snapshot' };
    component.confirmed = true;
    component.requestId = 'request-id';
    const first = component.send();
    http.expectOne('https://api.example.test/admin-api/messages').error(new ProgressEvent('error'));
    await first;
    expect(component.requestId).toBe('request-id');
    const retry = component.send();
    const request = http.expectOne('https://api.example.test/admin-api/messages');
    expect(request.request.body.id).toBe('request-id');
    request.flush({ queued: true });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    await retry;
    expect(component.preview).toBeNull();
  });
  it('does not restore an outdated review after the draft changes in flight', async () => {
    component.title = 'Original';
    component.body = 'Message';
    const review = component.review();
    const request = http.expectOne('https://api.example.test/admin-api/messages/preview');
    component.body = 'Changed';
    component.invalidate();
    request.flush({ count: 1, recipientVersion: 'old' });
    await review;
    expect(component.preview).toBeNull();
  });
  it('keeps a newly edited draft unsent when an earlier send completes', async () => {
    component.title = 'Original';
    component.body = 'Message';
    component.preview = { recipientVersion: 'snapshot' };
    component.confirmed = true;
    component.requestId = 'id';
    const send = component.send();
    const request = http.expectOne('https://api.example.test/admin-api/messages');
    component.title = 'New draft';
    component.invalidate();
    request.flush({ queued: true });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    await send;
    expect(component.hasDraft()).toBe(true);
  });
  it('supports signed-out audiences without requiring an owner test device', async () => {
    component.audience = 'guests';
    component.title = 'Welcome';
    component.body = 'A public prayer invitation';
    const review = component.review();
    const request = http.expectOne('https://api.example.test/admin-api/messages/preview');
    expect(request.request.body.audience).toBe('guests');
    expect(request.request.body.testDevice).toBeNull();
    request.flush({ count: 2, recipientVersion: 'guest-snapshot' });
    await review;
    expect(component.preview.count).toBe(2);
    expect(component.audienceLabel('guests')).toContain('Signed-out');
  });
  it('clears the entire composer and prevents resending after a successful submission', async () => {
    Object.assign(component, {
      title: 'Prayer',
      body: 'A moment of peace',
      kind: 'prayers',
      contentId: 'prayer-1',
      audience: 'all',
      testDevice: 'old-device',
      query: 'peace',
      choices: [{ id: 'prayer-1', title: 'Peace' }],
      contentPage: 2,
      contentMore: true,
      preview: { count: 11, recipientVersion: 'snapshot' },
      confirmed: true,
      requestId: 'original-send-id',
    });
    const send = component.send();
    http.expectOne('https://api.example.test/admin-api/messages').flush({ queued: true });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(component.title).toBe('');
    expect(component.body).toBe('');
    expect(component.kind).toBe('home');
    expect(component.audience).toBe('test');
    expect(component.testDevice).toBe('');
    expect(component.contentId).toBe('');
    expect(component.query).toBe('');
    expect(component.choices).toEqual([]);
    expect(component.contentPage).toBe(0);
    expect(component.contentMore).toBe(false);
    expect(component.preview).toBeNull();
    expect(component.confirmed).toBe(false);
    expect(component.requestId).toBe('');
    expect(component.hasDraft()).toBe(false);
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    await send;
    await component.send();
    http.expectNone('https://api.example.test/admin-api/messages');
  });
  it('does not restore a sent draft or suggest resending when history refresh fails', async () => {
    component.title = 'Prayer';
    component.body = 'Message';
    component.preview = { count: 11, recipientVersion: 'snapshot' };
    component.confirmed = true;
    component.requestId = 'send-id';
    const send = component.send();
    http.expectOne('https://api.example.test/admin-api/messages').flush({ queued: true });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .error(new ProgressEvent('error'));
    await send;
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(component.title).toBe('');
    expect(component.preview).toBeNull();
    expect(component.requestId).toBe('');
    expect(component.completed()).toBe(true);
    expect(component.historyError()).toContain('History could not refresh');
    await component.send();
    http.expectNone('https://api.example.test/admin-api/messages');
  });
  it('uses friendly content names and honest submission status without totals', () => {
    component.kind = 'saints';
    component.contentId = 'saint-1';
    component.choices = [{ id: 'saint-1', title: 'St. Cecilia' }];
    expect(component.destinationLabel()).toBe('Saint / feast day: St. Cecilia');
    component.kind = 'home';
    expect(component.destinationLabel()).toBe('Announcement only (no content link)');
    expect(component.submissionStatus({ pending: 11 })).toBe('Queued for sending');
    expect(component.submissionStatus({ accepted: 11 })).toBe('Submitted to Firebase');
    expect(component.submissionStatus({ failed: 11 })).toBe('Sending failed');
    expect(component.submissionStatus({ unknown: 11 })).toBe(
      'Sending status could not be confirmed',
    );
  });

  it('renders review and history without registration totals, raw URLs or internal IDs', async () => {
    const fixture = TestBed.createComponent(AdminMessagesComponent);
    const view = fixture.componentInstance;
    view.title = 'Invitation';
    view.body = 'A moment to pray';
    view.audience = 'all';
    view.preview = {
      count: 11,
      recipientVersion: 'snapshot',
      destination: 'https://dev.mydailysanctuary.com/',
    };
    fixture.detectChanges();
    http.expectOne('https://api.example.test/admin-api/messages?page=0').flush({
      enabled: true,
      testDevices: [],
      hasMore: false,
      items: [
        {
          id: 'history-1',
          title: 'Previous invitation',
          body: 'Message',
          audience: 'all',
          created_at: '2026-11-15T18:00:00Z',
          actor_sub: 'internal-actor-id',
          recipients: 11,
          accepted: 11,
        },
      ],
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
    await fixture.whenStable();
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Review');
    expect(text).toContain('Opens: Announcement only (no content link)');
    expect(text).toContain('Announcements & alerts');
    expect(text).toContain('Submitted for sending');
    expect(text).not.toContain('app installations');
    expect(text).not.toContain('11 recipients');
    expect(text).not.toContain('dev.mydailysanctuary.com');
    expect(text).not.toContain('internal-actor-id');
    fixture.destroy();
  });
  it('unlocks the cleared composer without waiting for history to respond', async () => {
    component.title = 'Prayer';
    component.body = 'Message';
    component.preview = { count: 1, recipientVersion: 'snapshot' };
    component.confirmed = true;
    component.requestId = 'send-id';
    const send = component.send();
    http.expectOne('https://api.example.test/admin-api/messages').flush({ queued: true });
    await send;
    const history = http.expectOne('https://api.example.test/admin-api/messages?page=0');
    expect(component.busy()).toBe(false);
    expect(component.refreshingHistory()).toBe(true);
    expect(component.completed()).toBe(true);
    expect(component.title).toBe('');
    component.createAnother();
    expect(component.completed()).toBe(false);
    component.title = 'Another message';
    component.body = 'New invitation';
    expect(component.hasDraft()).toBe(true);
    history.flush({ items: [], hasMore: false, enabled: true, testDevices: [] });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(component.title).toBe('Another message');
  });
  it('times out stalled history without blocking a new message or undoing submission', async () => {
    vi.useFakeTimers();
    try {
      component.title = 'Prayer';
      component.body = 'Message';
      component.preview = { count: 1, recipientVersion: 'snapshot' };
      component.confirmed = true;
      component.requestId = 'send-id';
      const send = component.send();
      http.expectOne('https://api.example.test/admin-api/messages').flush({ queued: true });
      await send;
      http.expectOne('https://api.example.test/admin-api/messages?page=0');
      await vi.advanceTimersByTimeAsync(15001);
      expect(component.busy()).toBe(false);
      expect(component.refreshingHistory()).toBe(false);
      expect(component.completed()).toBe(true);
      expect(component.historyError()).toContain('History could not refresh');
      expect(component.preview).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });
  it('requires confirmation before clearing history and preserves the current draft', async () => {
    component.history = [{ id: 'latest-reviewed-id' }];
    component.title = 'Unsent draft';
    component.body = 'Keep this';
    await component.clearHistory();
    http.expectNone('https://api.example.test/admin-api/messages/history');
    component.requestClearHistory();
    component.history = [{ id: 'newer-unreviewed-id' }];
    const clear = component.clearHistory();
    const request = http.expectOne('https://api.example.test/admin-api/messages/history');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body.throughMessageId).toBe('latest-reviewed-id');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ cleared: 1 });
    await clear;
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    expect(component.confirmClearHistory).toBe(false);
    expect(component.title).toBe('Unsent draft');
    expect(component.body).toBe('Keep this');
  });
  it('shows a usable create-another action while history is still loading', () => {
    const fixture = TestBed.createComponent(AdminMessagesComponent);
    const view = fixture.componentInstance;
    view.completed.set(true);
    fixture.detectChanges();
    const history = http.expectOne('https://api.example.test/admin-api/messages?page=0');
    const button = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (candidate: any) => candidate.textContent.trim() === 'Create another message',
    ) as HTMLButtonElement;
    expect(fixture.nativeElement.textContent).toContain('Announcement submitted');
    expect(button.disabled).toBe(false);
    button.click();
    expect(view.completed()).toBe(false);
    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('#push-title'));
    history.flush({ items: [], hasMore: false, enabled: true, testDevices: [] });
    fixture.destroy();
  });
  it('publishes without a phone alert and never publishes an owner test', async () => {
    expect(component.draft().publishToInbox).toBe(false);
    component.sendPush = false;
    component.deliveryChanged();
    expect(component.draft().publishToInbox).toBe(true);
    expect(component.draft().sendPush).toBe(false);
    component.title = 'Announcement';
    component.body = 'Full text';
    const review = component.review();
    http
      .expectOne('https://api.example.test/admin-api/messages/preview')
      .flush({ count: 0, enabled: true, recipientVersion: 'empty' });
    await review;
    component.confirmed = true;
    const send = component.send();
    const request = http.expectOne('https://api.example.test/admin-api/messages');
    expect(request.request.body.draft.sendPush).toBe(false);
    request.flush({ published: true, queued: false });
    await send;
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    expect(component.completedTitle()).toBe('Announcement published');
  });
  it('keeps published announcements manageable after sending history is cleared', async () => {
    const load = component.load();
    http.expectOne('https://api.example.test/admin-api/messages?page=0').flush({
      items: [{ id: 'published', title: 'Public invitation', published: true }],
      hasMore: false,
    });
    await load;
    expect(component.history[0].id).toBe('published');
    component.withdrawId = 'published';
    const withdraw = component.withdraw('published');
    const request = http.expectOne(
      'https://api.example.test/admin-api/messages/announcements/published',
    );
    expect(request.request.method).toBe('DELETE');
    request.flush({ withdrawn: true });
    await withdraw;
    http
      .expectOne('https://api.example.test/admin-api/messages?page=0')
      .flush({ items: [], hasMore: false });
    expect(component.withdrawId).toBeNull();
  });
  it('requires a fresh review after a real audience change without discarding the draft', async () => {
    component.title = 'Keep this message';
    component.body = 'Full announcement';
    component.preview = { recipientVersion: 'reviewed' };
    component.confirmed = true;
    component.requestId = 'same-send-id';
    const send = component.send();
    http
      .expectOne('https://api.example.test/admin-api/messages')
      .flush(
        { message: 'Notification subscriptions changed since your review.' },
        { status: 409, statusText: 'Conflict' },
      );
    await send;
    expect(component.preview).toBeNull();
    expect(component.confirmed).toBe(false);
    expect(component.title).toBe('Keep this message');
    expect(component.requestId).toBe('same-send-id');
    expect(component.notice()).toContain('subscriptions changed');
  });
  it('renders each message once with independent inbox and phone-alert statuses', async () => {
    const fixture = TestBed.createComponent(AdminMessagesComponent);
    fixture.detectChanges();
    http.expectOne('https://api.example.test/admin-api/messages?page=0').flush({
      enabled: true,
      testDevices: [],
      hasMore: false,
      items: [
        {
          id: 'published',
          title: 'Visible message',
          body: 'One copy',
          published: true,
          phone_alert_requested: false,
          created_at: '2026-11-16T15:00:00Z',
          audience: 'all',
        },
        {
          id: 'removed',
          title: 'Removed message',
          body: 'Previously published',
          published: false,
          removed_from_inbox: true,
          phone_alert_requested: true,
          accepted: 1,
          created_at: '2026-11-16T14:00:00Z',
          audience: 'all',
        },
        {
          id: 'test',
          title: 'Private test',
          body: 'No publication',
          published: false,
          phone_alert_requested: true,
          pending: 1,
          created_at: '2026-11-16T13:00:00Z',
          audience: 'test',
        },
      ],
      announcements: { items: [{ id: 'published', title: 'Visible message' }] },
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
    await fixture.whenStable();
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    const records = fixture.nativeElement.querySelectorAll('.announcement-record');
    expect(records.length).toBe(3);
    expect(records[0].textContent).toContain('Visible in app');
    expect(records[0].textContent).toContain('Not requested');
    expect(records[1].textContent).toContain('Removed from app');
    expect(records[1].textContent).toContain('Submitted for sending');
    expect(records[2].textContent).toContain('Owner test only');
    expect(records[2].textContent).toContain('Not published');
    expect(records[2].textContent).toContain('Queued for sending');
    expect(fixture.nativeElement.textContent.match(/Visible message/g)).toHaveLength(1);
    const removal = Array.from(records[0].querySelectorAll('button')).find((b: any) =>
      b.textContent.includes('Remove from app inbox'),
    ) as HTMLButtonElement;
    removal.click();
    fixture.detectChanges();
    expect(records[0].textContent).toContain('Phone alerts already sent cannot be recalled');
    expect(records[1].querySelector('button')).toBeNull();
    fixture.destroy();
  });
});
