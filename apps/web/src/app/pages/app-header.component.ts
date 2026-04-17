import { Component, input, output } from '@angular/core';

type AppTab = 'home' | 'novenas' | 'liturgical' | 'saints' | 'prayers' | 'me';

@Component({
  selector: 'app-header',
  standalone: true,
  styleUrl: './app-header.component.scss',
  template: `
    <nav class="primary-nav glass-card" aria-label="Primary">
      <div class="primary-nav__tabs">
        <button class="tab" [class.active]="currentTab() === 'home'" type="button" (click)="navigate.emit('home')">
          <span class="tab-icon">⌂</span>
          <span>Home</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'novenas'" type="button" (click)="navigate.emit('novenas')">
          <span class="tab-icon">☰</span>
          <span>Novenas</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'liturgical'" type="button" (click)="navigate.emit('liturgical')">
          <span class="tab-icon">▦</span>
          <span>Liturgical</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'saints'" type="button" (click)="navigate.emit('saints')">
          <span class="tab-icon">♁</span>
          <span>Saints</span>
        </button>
        <button class="tab" [class.active]="currentTab() === 'me'" type="button" (click)="navigate.emit('me')">
          <span class="tab-icon">●</span>
          <span>Me</span>
        </button>
      </div>

      <div class="primary-nav__actions">
        <button class="pill-button nav-pill-button" type="button" (click)="openAbout.emit()">
          <span class="pill-icon">◎</span>
          <span>About</span>
        </button>
        <button class="pill-button nav-pill-button" type="button" (click)="toggleLanguage.emit()">
          <span class="pill-icon">⌘</span>
          <span>{{ isEnglish() ? 'English' : 'Español' }}</span>
        </button>
      </div>
    </nav>
  `,
})
export class AppHeaderComponent {
  readonly currentTab = input<AppTab>('home');
  readonly isEnglish = input<boolean>(true);

  readonly navigate = output<AppTab>();
  readonly openAbout = output<void>();
  readonly toggleLanguage = output<void>();
}
