import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly currentTab = signal<'home' | 'novenas' | 'liturgical' | 'saints' | 'me'>('home');
  protected readonly liturgicalView = signal<'day' | 'week' | 'month'>('month');
  protected readonly liturgicalMonthLabel = signal('April 2026');
  protected readonly showAbout = signal(false);
  protected readonly language = signal<'en' | 'es'>('en');

  protected setTab(tab: 'home' | 'novenas' | 'liturgical' | 'saints' | 'me'): void {
    this.currentTab.set(tab);
  }

  protected openAbout(): void {
    this.showAbout.set(true);
  }

  protected closeAbout(): void {
    this.showAbout.set(false);
  }

  protected toggleLanguage(): void {
    this.language.update((current) => (current === 'en' ? 'es' : 'en'));
  }

  protected isEnglish(): boolean {
    return this.language() === 'en';
  }

  protected setLiturgicalView(view: 'day' | 'week' | 'month'): void {
    this.liturgicalView.set(view);
  }

  protected shiftLiturgicalMonth(direction: -1 | 1): void {
    const months = [
      'January 2026',
      'February 2026',
      'March 2026',
      'April 2026',
      'May 2026',
      'June 2026',
      'July 2026',
      'August 2026',
      'September 2026',
      'October 2026',
      'November 2026',
      'December 2026',
    ];

    const currentIndex = months.indexOf(this.liturgicalMonthLabel());
    const nextIndex = (currentIndex + direction + months.length) % months.length;
    this.liturgicalMonthLabel.set(months[nextIndex]);
  }
}
