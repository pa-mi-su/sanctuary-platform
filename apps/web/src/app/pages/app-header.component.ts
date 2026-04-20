import { Component, input, output } from '@angular/core';

type AppTab = 'home' | 'novenas' | 'liturgical' | 'saints' | 'prayers' | 'me';
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
        <button class="tab" [class.active]="currentTab() === 'me'" type="button" (click)="navigate.emit('me')">
          <span class="tab-icon">●</span>
          <span>{{ t('Me', 'Yo', 'Ja') }}</span>
        </button>
      </div>

      <div class="primary-nav__actions">
        <button class="pill-button nav-pill-button" type="button" (click)="openAbout.emit()">
          <span class="pill-icon">◎</span>
          <span>{{ t('About', 'Acerca de', 'O aplikacji') }}</span>
        </button>
        <label class="pill-button nav-pill-button nav-language-picker" for="app-language-select">
          <span class="pill-icon">⌘</span>
          <span class="nav-language-picker__label">{{ t('Language', 'Idioma', 'Jezyk') }}</span>
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

  readonly navigate = output<AppTab>();
  readonly openAbout = output<void>();
  readonly selectLanguage = output<AppLanguage>();

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
