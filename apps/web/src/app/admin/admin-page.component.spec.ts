import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminPageComponent } from './admin-page.component';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';

describe('AdminPageComponent', () => {
  let component: AdminPageComponent;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: SANCTUARY_API_BASE_URL, useValue: 'https://api.example.org' }] });
    component = TestBed.createComponent(AdminPageComponent).componentInstance;
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { component.ngOnDestroy(); http.verify(); });
  it('uses no public cookies and clears the password after the challenge', async () => {
    component.email = 'owner@example.org'; component.password = 'password';
    const pending = component.login();
    const request = http.expectOne('https://api.example.org/admin-api/login');
    expect(request.request.withCredentials).toBe(false);
    expect(request.request.headers.get('X-Sanctuary-Admin')).toBe('1');
    request.flush({ challenge: 'challenge', secret: 'setup-secret', mode: 'enroll' });
    await pending;
    expect(component.password).toBe(''); expect(component.secret).toBe('setup-secret');
    expect(component.signedIn()).toBe(false);
  });
  it('does not restore a session when verification finishes after sign-out', async () => {
    component.challenge = 'challenge'; component.code = '123456';
    const pending = component.verify();
    const request = http.expectOne('https://api.example.org/admin-api/verify');
    await component.logout();
    request.flush({ token: 'late-token' });
    await pending;
    expect(component.signedIn()).toBe(false); expect(component.code).toBe('');
  });
  it('clears MFA setup material on restart', () => {
    component.secret = 'private'; component.challenge = 'challenge'; component.code = '123456';
    component.restart();
    expect(component.secret).toBe(''); expect(component.challenge).toBe(''); expect(component.code).toBe('');
  });
});
