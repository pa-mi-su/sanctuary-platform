import { Component, input, output } from '@angular/core';
import { PrayerSummary } from '../core/api/sanctuary-api.service';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-prayers-page',
  standalone: true,
  styleUrl: './prayers-page.component.scss',
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header">
          <button class="circle-button" type="button" (click)="goHome.emit()">‹</button>
          <div>
          <h2>{{ t('Prayers', 'Oraciones', 'Modlitwy') }}</h2>
          <p class="meta-text">{{ resultsLabel() }}</p>
        </div>
      </div>

      <label class="search-bar glass-subtle">
        <span>⌕</span>
        <input
          class="search-input"
          type="text"
          [value]="query()"
          (input)="updateQuery.emit($any($event.target).value)"
          [placeholder]="t('Search prayers', 'Buscar oraciones', 'Szukaj modlitw')"
        />
      </label>

      @if (loadFailed()) {
        <div class="mode-panel glass-subtle">
          <strong>{{ t('Prayers', 'Oraciones', 'Modlitwy') }}</strong>
          <p>{{ apiErrorCopy() }}</p>
        </div>
      } @else {
        <section class="list-stack">
          @for (prayer of prayers(); track prayer.slug) {
            <button class="content-card glass-subtle content-button" type="button" (click)="openPrayer.emit(prayer)">
              <div class="content-card__media" [style.background-image]="cardImageStyle(prayer.imageUrl)">
                @if (!prayer.imageUrl) {
                  <span class="content-card__fallback">🕯</span>
                }
              </div>
              <div class="content-card__body">
                <h3>{{ prayer.title }}</h3>
                <p>{{ prayerPreviewLabel(prayer) }}</p>
                <span class="content-tag">{{ prayer.category }}</span>
              </div>
            </button>
          }
        </section>
      }
    </section>
  `,
})
export class PrayersPageComponent {
  readonly isEnglish = input<boolean>(true);
  readonly currentLanguage = input<AppLanguage>('en');
  readonly query = input<string>('');
  readonly resultsLabel = input.required<string>();
  readonly loadFailed = input<boolean>(false);
  readonly apiErrorCopy = input.required<string>();
  readonly prayers = input.required<PrayerSummary[]>();

  readonly goHome = output<void>();
  readonly updateQuery = output<string>();
  readonly openPrayer = output<PrayerSummary>();

  protected prayerPreviewLabel(prayer: PrayerSummary): string {
    return prayer.bodyPreview;
  }

  protected cardImageStyle(imageUrl: string | null | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(6, 12, 18, 0.05), rgba(6, 12, 18, 0.28)), url(${imageUrl})`;
  }

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
