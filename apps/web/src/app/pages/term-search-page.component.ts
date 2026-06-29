import { Component, input, output } from '@angular/core';
import { NovenaSummary, SaintSummary, SearchTerm } from '../core/api/sanctuary-api.service';

type TermMode = 'intentions' | 'patronage';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-term-search-page',
  standalone: true,
  styleUrl: './novenas-page.component.scss',
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header">
        <button class="circle-button" type="button" (click)="goHome.emit()">‹</button>
        <div>
          <h2>{{ mode() === 'patronage' ? t('Patronage', 'Patrocinios', 'Patronaty') : t('Intentions', 'Intenciones', 'Intencje') }}</h2>
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
          [placeholder]="mode() === 'patronage' ? t('Search patronage', 'Buscar patronazgo', 'Szukaj patronatu') : t('Search intentions', 'Buscar intenciones', 'Szukaj intencji')"
        />
      </label>

      @if (loadFailed()) {
        <div class="mode-panel glass-subtle">
          <strong>{{ t('Search', 'Buscar', 'Szukaj') }}</strong>
          <p>{{ apiErrorCopy() }}</p>
        </div>
      } @else if (!selectedTerm()) {
        @if (terms().length) {
          <section class="list-stack intentions-list">
            @for (term of terms(); track term.key) {
              <button class="content-card glass-subtle content-button" type="button" (click)="selectTerm.emit(term)">
                <div class="content-card__media term-thumbnail" [class.stacked]="previewImageUrls(term).length > 1">
                  @if (previewImageUrls(term).length) {
                    @for (url of previewImageUrls(term); track url) {
                      <img [src]="url" alt="" />
                    }
                  } @else {
                    <span class="content-card__fallback">{{ mode() === 'patronage' ? '♔' : '♥' }}</span>
                  }
                </div>
                <div class="content-card__body">
                  <h3>{{ term.label }}</h3>
                  <p>{{ termResultPreview(term) }}</p>
                </div>
              </button>
            }
          </section>
        } @else {
          <div class="mode-panel glass-subtle compact">
            <strong>{{ mode() === 'patronage' ? t('Patronage', 'Patrocinios', 'Patronaty') : t('Intentions', 'Intenciones', 'Intencje') }}</strong>
            <p>{{ emptyCopy() }}</p>
          </div>
        }
      } @else {
        <div class="chip-row">
          <button class="chip selected" type="button" (click)="clearTerm.emit()">‹ {{ selectedTerm()!.label }}</button>
        </div>

        @if (mode() === 'patronage') {
          <section class="list-stack intentions-list">
            @for (saint of saints(); track saint.slug) {
              <button class="content-card glass-subtle content-button" type="button" (click)="openSaint.emit(saint)">
                <div class="content-card__media" [style.background-image]="cardImageStyle(saint.imageUrl)">
                  @if (!saint.imageUrl) { <span class="content-card__fallback">♔</span> }
                </div>
                <div class="content-card__body">
                  <h3>{{ saint.name }}</h3>
                  <p>{{ saint.summary }}</p>
                  <span class="content-tag">{{ patronageLabel(saint) || saint.feastLabel }}</span>
                </div>
              </button>
            }
          </section>
        } @else {
          <section class="list-stack intentions-list">
            @for (novena of novenas(); track novena.slug) {
              <button class="content-card glass-subtle content-button" type="button" (click)="openNovena.emit(novena)">
                <div class="content-card__media" [style.background-image]="cardImageStyle(novena.imageUrl)">
                  @if (!novena.imageUrl) { <span class="content-card__fallback">📘</span> }
                </div>
                <div class="content-card__body">
                  <h3>{{ novena.title }}</h3>
                  <p>{{ novena.description }}</p>
                  <span class="content-tag">{{ durationLabel(novena.durationDays) }}</span>
                </div>
              </button>
            }
          </section>
        }
      }
    </section>
  `,
})
export class TermSearchPageComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly mode = input.required<TermMode>();
  readonly query = input('');
  readonly terms = input<SearchTerm[]>([]);
  readonly selectedTerm = input<SearchTerm | null>(null);
  readonly saints = input<SaintSummary[]>([]);
  readonly novenas = input<NovenaSummary[]>([]);
  readonly loadFailed = input(false);
  readonly apiErrorCopy = input.required<string>();
  readonly resultsLabel = input.required<string>();
  readonly emptyCopy = input.required<string>();

  readonly goHome = output<void>();
  readonly updateQuery = output<string>();
  readonly selectTerm = output<SearchTerm>();
  readonly clearTerm = output<void>();
  readonly openSaint = output<SaintSummary>();
  readonly openNovena = output<NovenaSummary>();

  protected t(english: string, spanish: string, polish: string): string {
    switch (this.currentLanguage()) {
      case 'es': return spanish;
      case 'pl': return polish;
      default: return english;
    }
  }

  protected resultCountLabel(count: number): string {
    switch (this.currentLanguage()) {
      case 'es': return `${count} ${count === 1 ? 'resultado' : 'resultados'}`;
      case 'pl': return `${count} wyników`;
      default: return `${count} ${count === 1 ? 'result' : 'results'}`;
    }
  }

  protected termResultPreview(term: SearchTerm): string {
    const labels = (term.resultLabels ?? []).filter(Boolean);
    if (!labels.length) {
      return this.resultCountLabel(term.resultCount);
    }

    const preview = labels.join(' • ');
    const remaining = term.resultCount - labels.length;
    return remaining > 0 ? `${preview} +${remaining}` : preview;
  }

  protected durationLabel(days: number): string {
    switch (this.currentLanguage()) {
      case 'es': return `${days} ${days === 1 ? 'día' : 'días'}`;
      case 'pl': return `${days} dni`;
      default: return `${days} ${days === 1 ? 'day' : 'days'}`;
    }
  }

  protected patronageLabel(saint: SaintSummary): string {
    return saint.patronages?.filter(Boolean).slice(0, 3).join(' • ') ?? '';
  }

  protected previewImageUrls(term: SearchTerm): string[] {
    return (term.imageUrls ?? []).filter(Boolean).slice(0, 3);
  }

  protected cardImageStyle(url: string | null | undefined): string | null {
    return url ? `url("${url}")` : null;
  }
}
