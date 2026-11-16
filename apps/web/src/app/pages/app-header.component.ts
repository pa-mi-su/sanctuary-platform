import { Component, input, output } from '@angular/core';

type AppTab =
  | 'home'
  | 'novenas'
  | 'intentions'
  | 'patronage'
  | 'liturgical'
  | 'saints'
  | 'prayers'
  | 'rosaries'
  | 'about'
  | 'auth'
  | 'me';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-header',
  standalone: true,
  styleUrl: './app-header.component.scss',
  template: `
    <nav class="primary-nav glass-card" aria-label="Primary">
      <div class="primary-nav__tabs">
        <button
          class="tab"
          [class.active]="currentTab() === 'home'"
          type="button"
          (click)="navigate.emit('home')"
        >
          <span class="tab-icon">⌂</span>
          <span>{{ t('Home', 'Inicio', 'Start') }}</span>
        </button>
        <button
          class="tab"
          [class.active]="currentTab() === 'novenas'"
          type="button"
          (click)="navigate.emit('novenas')"
        >
          <span class="tab-icon">☰</span>
          <span>{{ t('Novenas', 'Novenas', 'Nowenny') }}</span>
        </button>
        <button
          class="tab"
          [class.active]="currentTab() === 'liturgical'"
          type="button"
          (click)="navigate.emit('liturgical')"
        >
          <span class="tab-icon">▦</span>
          <span>{{ t('Liturgical', 'Litúrgico', 'Liturgia') }}</span>
        </button>
        <button
          class="tab"
          [class.active]="currentTab() === 'saints'"
          type="button"
          (click)="navigate.emit('saints')"
        >
          <span class="tab-icon">♁</span>
          <span>{{ t('Saints', 'Santos', 'Swieci') }}</span>
        </button>
        <button
          class="tab"
          [class.active]="isAuthenticated() ? currentTab() === 'me' : currentTab() === 'auth'"
          type="button"
          (click)="navigate.emit(isAuthenticated() ? 'me' : 'auth')"
        >
          <span class="tab-icon">{{ isAuthenticated() ? '●' : '◉' }}</span>
          @if (isAuthenticated()) {
            <span class="tab-label">{{ t('Me', 'Yo', 'Ja') }}</span>
          } @else {
            <span class="tab-label tab-label--desktop">
              {{ t('Login / Register', 'Entrar / Registro', 'Login / Rejestracja') }}
            </span>
            <span class="tab-label tab-label--mobile">
              {{ t('Login', 'Entrar', 'Login') }}
            </span>
          }
        </button>
      </div>

      <div
        class="primary-nav__actions"
        [class.primary-nav__actions--authenticated]="isAuthenticated()"
      >
        <details #menu>
          <summary class="pill-button">☰ {{ t('Menu', 'Menú', 'Menu') }}</summary>
          <button class="pill-button" (click)="menu.open = false; openSettings.emit()">
            {{ t('Settings', 'Ajustes', 'Ustawienia') }}
          </button>
          <button class="pill-button" (click)="menu.open = false; navigate.emit('about')">
            {{ t('About & Help', 'Acerca de y ayuda', 'O aplikacji i pomoc') }}
          </button>
          <button class="pill-button" (click)="menu.open = false; openInbox.emit()">
            {{ t('Announcements', 'Anuncios', 'Ogłoszenia') }}
          </button>
        </details>
        <button
          class="pill-button"
          (click)="menu.open = false; openInbox.emit()"
          [attr.aria-label]="t('Announcements', 'Anuncios', 'Ogłoszenia') + (unread() ? ' •' : '')"
        >
          <svg
            aria-hidden="true"
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
          </svg>
          {{ t('Announcements', 'Anuncios', 'Ogłoszenia') }} {{ unread() ? '●' : '' }}
        </button>
      </div>
    </nav>
  `,
})
export class AppHeaderComponent {
  readonly openSettings = output<void>();
  readonly openInbox = output<void>();
  readonly unread = input(false);
  readonly currentTab = input<AppTab>('home');
  readonly isEnglish = input<boolean>(true);
  readonly currentLanguage = input<AppLanguage>('en');
  readonly isAuthenticated = input<boolean>(false);

  readonly navigate = output<AppTab>();
  readonly selectLanguage = output<AppLanguage>();
  readonly logout = output<void>();

  protected t(english: string, spanish: string, polish: string): string {
    switch (this.currentLanguage()) {
      case 'es':
        return spanish;
      case 'pl':
        return polish;
      default:
        return english;
    }
  }
}
