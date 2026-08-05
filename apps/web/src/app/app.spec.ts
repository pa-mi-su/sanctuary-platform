import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { SANCTUARY_API_BASE_URL } from './core/api/sanctuary-api.config';
import { SANCTUARY_AUTH_CONFIG } from './core/auth/sanctuary-auth.config';
import { AppShellFacade } from './core/state/app-shell.facade';

describe('App', () => {
  beforeEach(async () => {
    window.history.replaceState({}, '', '/');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: SANCTUARY_API_BASE_URL,
          useValue: 'http://localhost:8080',
        },
        {
          provide: SANCTUARY_AUTH_CONFIG,
          useValue: {
            enabled: false,
            cognitoDomain: '',
            clientId: '',
            redirectUri: 'http://localhost:4200',
            logoutUri: 'http://localhost:4200',
            scopes: ['openid', 'email', 'profile'],
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    window.history.replaceState({}, '', '/');
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Welcome to your sanctuary');
  });

  it('should render the privacy policy from its direct URL', async () => {
    window.history.replaceState({}, '', '/privacy');
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sanctuary Privacy Policy');
    expect(compiled.textContent).toContain('Information We Collect');
  });

  it('routes registered users from the account prompt directly to sign in', () => {
    const fixture = TestBed.createComponent(App);
    const facade = TestBed.inject(AppShellFacade);
    facade.requireAccount();
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('.account-required-actions button')) as HTMLButtonElement[];
    buttons.find((button) => button.textContent?.includes('Sign In'))?.click();

    expect(facade.currentTab()).toBe('auth');
    expect(facade.authInitialStep()).toBe('login');
    expect(facade.accountRequiredPrompt()).toBe(false);
  });

  it('routes new users from the account prompt directly to registration', () => {
    const fixture = TestBed.createComponent(App);
    const facade = TestBed.inject(AppShellFacade);
    facade.requireAccount();
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('.account-required-actions button')) as HTMLButtonElement[];
    buttons.find((button) => button.textContent?.includes('Create Account'))?.click();

    expect(facade.currentTab()).toBe('auth');
    expect(facade.authInitialStep()).toBe('register');
    expect(facade.accountRequiredPrompt()).toBe(false);
  });
});
