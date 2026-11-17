import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AccountRemindersComponent } from './account-reminders.component';
import { SanctuaryAuthService } from '../core/auth/sanctuary-auth.service';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';
const base = 'https://reminders.sanctuary.invalid';
const row = { kind: 'novenas', slug: 'test', title: 'Test novena', hour: 14, minute: 55 };
const settle = () => new Promise((resolve) => setTimeout(resolve, 0));
describe('Account reminder management', () => {
  let http: HttpTestingController;
  let auth: any;
  let component: AccountRemindersComponent;
  beforeEach(() => {
    auth = { state: signal({ status: 'authenticated', email: 'first@sanctuary.invalid' }) };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SanctuaryAuthService, useValue: auth },
        { provide: SANCTUARY_API_BASE_URL, useValue: base },
      ],
    });
    const fixture = TestBed.createComponent(AccountRemindersComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    TestBed.tick();
  });
  afterEach(() => http.verify());
  async function initial() {
    http.expectOne(base + '/me/reminders').flush([row]);
    await settle();
    http
      .expectOne(base + '/content/novenas/test')
      .flush({ imageUrl: 'https://images.sanctuary.invalid/test.jpg' });
    await settle();
  }
  it('edits the exact minute and updates the displayed reminder after saving', async () => {
    await initial();
    component.edit(row);
    expect(component.time).toBe('14:55');
    component.time = '15:00';
    const saved = component.save();
    const request = http.expectOne(base + '/me/reminders');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ ...row, hour: 15, minute: 0 });
    request.flush(null);
    await settle();
    http.expectOne(base + '/me/reminders').flush([{ ...row, hour: 15, minute: 0 }]);
    await saved;
    expect(component.items()[0].minute).toBe(0);
    expect(component.editing()).toBeNull();
  });
  it('retains the editor after a failed save and prevents duplicate submissions', async () => {
    await initial();
    component.edit(row);
    component.time = '08:30';
    const first = component.save();
    await component.save();
    const request = http.expectOne(base + '/me/reminders');
    request.flush({}, { status: 503, statusText: 'Offline' });
    await first;
    expect(component.editing()).toEqual(row);
    expect(component.problem()).toContain('Could not save');
    expect(component.busy()).toBe(false);
  });
  it('does not show a previous account’s delayed reminder list', async () => {
    const old = http.expectOne(base + '/me/reminders');
    auth.state.set({ status: 'authenticated', email: 'second@sanctuary.invalid' });
    TestBed.tick();
    http.expectOne(base + '/me/reminders').flush([]);
    await settle();
    old.flush([row]);
    await settle();
    expect(component.items()).toEqual([]);
    http.expectNone(base + '/content/novenas/test');
  });
});
