import { Component, input, output } from '@angular/core';

type AppTab = 'home' | 'novenas' | 'liturgical' | 'saints' | 'prayers' | 'about' | 'auth' | 'me';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-header',
  standalone: true,
  styleUrl: './app-header.component.scss',
  template: `
    <nav class="primary-nav glass-card" aria-label="Primary">
      <div class="primary-nav__tabs">
        <button class="tab" [class.active]="currentTab() === 'home'" type="button" (click)="navigate.emit('home')">
          <span class="tab-icon">⌂</span>
          <span>{{ t('Home', 'Inicio', 'Start') }}</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'novenas'" type="button" (click)="navigate.emit('novenas')">
          <span class="tab-icon">☰</span>
          <span>{{ t('Novenas', 'Novenas', 'Nowenny') }}</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'liturgical'" type="button" (click)="navigate.emit('liturgical')">
          <span class="tab-icon">▦</span>
          <span>{{ t('Liturgical', 'Litúrgico', 'Liturgia') }}</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'saints'" type="button" (click)="navigate.emit('saints')">
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

      <div class="primary-nav__actions" [class.primary-nav__actions--authenticated]="isAuthenticated()">
        @if (isAuthenticated()) {
          <button class="pill-button nav-pill-button logout-button" type="button" (click)="logout.emit()">
            <span class="pill-icon">↩</span>
            <span class="nav-label nav-label--desktop">{{ t('Logout', 'Salir', 'Wyloguj') }}</span>
            <span class="nav-label nav-label--mobile">{{ t('Logout', 'Salir', 'Wyloguj') }}</span>
          </button>
        }
        <button
          class="pill-button nav-pill-button"
          [class.active]="currentTab() === 'about'"
          type="button"
          (click)="navigate.emit('about')"
        >
          <span class="pill-icon">◎</span>
          <span class="nav-label">{{ t('About', 'Acerca de', 'O aplikacji') }}</span>
        </button>
        <label class="pill-button nav-pill-button nav-language-picker" for="app-language-select">
          <span class="pill-icon">⌘</span>
          <span class="nav-language-picker__label nav-language-picker__label--desktop">{{ t('Language', 'Idioma', 'Jezyk') }}</span>
          <span class="nav-language-picker__label nav-language-picker__label--mobile">{{ t('Lang', 'Idioma', 'Jezyk') }}</span>
          <select
            id="app-language-select"
            class="nav-language-picker__select"
            [value]="currentLanguage()"
            (change)="selectLanguage.emit(($any($event.target).value))"
          >
            <option value="en">English</option>
            <option value="es">Spanish</option>
            <option value="pl">Polish</option>
          </select>
        </label>
      </div>
    </nav>
  `,
})
export class AppHeaderComponent {
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
