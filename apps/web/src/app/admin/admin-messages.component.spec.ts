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
});
