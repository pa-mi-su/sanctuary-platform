import { Component, input, output } from '@angular/core';

type AppTab = 'home' | 'novenas' | 'intentions' | 'liturgical' | 'saints' | 'prayers' | 'me';

@Component({
  selector: 'app-mobile-nav',
  standalone: true,
  styleUrl: './mobile-nav.component.scss',
  template: `
    <nav class="tab-bar mobile-tab-bar glass-card" aria-label="Primary">
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
    </nav>
  `,
})
export class MobileNavComponent {
  readonly currentTab = input<AppTab>('home');
  readonly navigate = output<AppTab>();
}
