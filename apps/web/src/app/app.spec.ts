import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { SANCTUARY_API_BASE_URL } from './core/api/sanctuary-api.config';
import { SANCTUARY_AUTH_CONFIG } from './core/auth/sanctuary-auth.config';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
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
});
