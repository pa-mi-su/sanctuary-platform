import { Component, input, output } from '@angular/core';
import { NovenaSummary } from '../core/api/sanctuary-api.service';

type CalendarView = 'day' | 'week' | 'month';
type NovenasMode = 'calendar' | 'list' | 'intentions';
type SeasonKey = 'ADVENT' | 'CHRISTMAS' | 'LENT' | 'EASTER' | 'ORDINARY';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-novenas-page',
  standalone: true,
  styleUrl: './novenas-page.component.scss',
  template: `
    <section class="screen-card glass-card">
      @if (mode() !== 'calendar') {
        <div class="screen-header">
          <button class="circle-button" type="button" (click)="goHome.emit()">‹</button>
          <div>
            <h2>{{ mode() === 'intentions' ? t('Intentions', 'Intenciones', 'Intencje') : t('Novenas', 'Novenas', 'Nowenny') }}</h2>
            <p class="meta-text">{{ intentionsResultsLabel() }}</p>
          </div>
        </div>

        <label class="search-bar glass-subtle">
          <span>⌕</span>
          <input
            class="search-input"
            type="text"
            [value]="query()"
            (input)="updateQuery.emit($any($event.target).value)"
            [placeholder]="searchPlaceholder()"
          />
        </label>
      } @else {
        <div class="screen-header split">
          <button class="circle-button" type="button" (click)="shiftDate.emit(-1)">‹</button>
          <div class="screen-title">
            <h2>{{ selectedDateLabel() }}</h2>
            <p>{{ novenasCountLabel() }}</p>
          </div>
          <button class="circle-button" type="button" (click)="shiftDate.emit(1)">›</button>
        </div>

        <div class="chip-row">
          <button class="chip selected" type="button" (click)="resetDate.emit()">
            {{ isSelectedDateToday() ? t('Today', 'Hoy', 'Dzisiaj') : t('Jump to Today', 'Ir a hoy', 'Przejdz do dzisiaj') }}
          </button>
          <button class="chip" [class.active-blue]="novenasView() === 'day'" type="button" (click)="changeView.emit('day')">Day</button>
          <button class="chip" [class.active-blue]="novenasView() === 'week'" type="button" (click)="changeView.emit('week')">Week</button>
          <button class="chip" [class.active-blue]="novenasView() === 'month'" type="button" (click)="changeView.emit('month')">Month</button>
        </div>

        <div class="season-legend">
          @for (item of seasonLegend(); track item.key) {
            <span class="season-pill" [attr.data-season]="item.key">{{ item.label }}</span>
          }
        </div>

        @if (novenasView() !== 'day') {
          <div class="calendar-headings">
            @for (label of weekdayLabels(); track label) {
              <span>{{ label }}</span>
            }
          </div>

          <div class="calendar-grid" [class.week-grid]="novenasView() === 'week'" [class.month-grid]="novenasView() === 'month'">
          @for (day of calendarDays(); track day.date ?? $index) {
            <button
              class="calendar-day calendar-button"
              [attr.data-season]="day.seasonKey"
              [class.empty]="!day.date"
              [class.selected]="day.date === selectedDate()"
              [class.today]="day.date === todayDate()"
              type="button"
              [disabled]="!day.date"
              (click)="day.date && pickDate.emit(day.date)"
            >
              <strong>{{ day.dayNumber ?? '' }}</strong>
              <span>{{ day.label }}</span>
            </button>
          }
          </div>
        }
      }

      @if (novenasLoadFailed()) {
        <div class="mode-panel glass-subtle">
          <strong>{{ t('Novenas', 'Novenas', 'Nowenny') }}</strong>
          <p>{{ apiErrorCopy() }}</p>
        </div>
      }

      @if (mode() !== 'calendar') {
        @if (searchResults().length) {
          <section class="list-stack intentions-list">
            @for (novena of searchResults(); track novena.slug) {
              <button class="content-card glass-subtle content-button" type="button" (click)="openNovena.emit(novena)">
                <div class="content-card__media" [style.background-image]="cardImageStyle(novena.imageUrl)">
                  @if (!novena.imageUrl) {
                    <span class="content-card__fallback">📘</span>
                  }
                </div>
                <div class="content-card__body">
                  <h3>{{ novena.title }}</h3>
                  <p>{{ novena.description }}</p>
                  <span class="content-tag">{{ mode() === 'intentions' ? intentionsSummary(novena) : novenaDayCountLabel(novena) }}</span>
                </div>
              </button>
            }
          </section>
        } @else {
          <div class="mode-panel glass-subtle compact">
            <strong>{{ mode() === 'intentions' ? t('Intentions Search', 'Búsqueda de intenciones', 'Wyszukiwanie intencji') : t('Novenas', 'Novenas', 'Nowenny') }}</strong>
            <p>{{ intentionsEmptyCopy() }}</p>
          </div>
        }
      } @else {
        @if (selectedNovenaHeadline()) {
          <button class="saint-highlight novena-highlight glass-subtle content-button" type="button" [attr.data-season]="selectedSeasonKey()" (click)="openNovena.emit(selectedNovenaHeadline()!)">
            <div class="saint-date">
              <strong>{{ selectedDateDayNumber() }}</strong>
              <span>{{ selectedNovenaHeadline()!.title }}</span>
            </div>
            <div class="saint-photo" [style.background-image]="selectedNovenaImageStyle()"></div>
            <div class="saint-action">↗</div>
          </button>
        } @else {
          <article class="saint-highlight novena-highlight novena-empty-highlight glass-subtle" [attr.data-season]="selectedSeasonKey()">
            <div class="saint-date">
              <strong>{{ selectedDateDayNumber() }}</strong>
              <span>{{ noNovenasCopy() }}</span>
            </div>
          </article>
        }

        <section class="preview-grid">
          <article class="preview-panel glass-subtle">
            <div class="preview-header">
                <div>
                  <h3>{{ previewTodayTitle() }}</h3>
                  <p>{{ todayPreviewLabel() }}</p>
                </div>
            </div>

            @if (todayPrimaryNovena()) {
              <button class="preview-item" type="button" (click)="openNovena.emit(todayPrimaryNovena()!)">
                <div class="preview-item__media" [style.background-image]="cardImageStyle(todayPrimaryNovena()!.imageUrl)">
                  @if (!todayPrimaryNovena()!.imageUrl) {
                    <span class="content-card__fallback">📘</span>
                  }
                </div>
                <div class="preview-item__body">
                  <strong>{{ todayPrimaryNovena()!.title }}</strong>
                  <span>{{ novenaDayCountLabel(todayPrimaryNovena()!) }}</span>
                </div>
              </button>
            } @else {
              <p class="preview-empty">{{ noNovenasCopy() }}</p>
            }
          </article>

          <article class="preview-panel glass-subtle">
            <div class="preview-header">
              <div>
                <h3>{{ previewSelectedTitle() }}</h3>
                <p>{{ selectedPreviewLabel() }}</p>
              </div>
            </div>

            @if (isSelectedDateToday()) {
              <p class="preview-empty">{{ selectedSameAsTodayCopy() }}</p>
            } @else if (selectedPrimaryNovena()) {
              <button class="preview-item" type="button" (click)="openNovena.emit(selectedPrimaryNovena()!)">
                <div class="preview-item__media" [style.background-image]="cardImageStyle(selectedPrimaryNovena()!.imageUrl)">
                  @if (!selectedPrimaryNovena()!.imageUrl) {
                    <span class="content-card__fallback">📘</span>
                  }
                </div>
                <div class="preview-item__body">
                  <strong>{{ selectedPrimaryNovena()!.title }}</strong>
                  <span>{{ novenaDayCountLabel(selectedPrimaryNovena()!) }}</span>
                </div>
              </button>
            } @else {
              <p class="preview-empty">{{ noNovenasCopy() }}</p>
            }
          </article>
        </section>
      }
    </section>
  `,
})
export class NovenasPageComponent {
  readonly isEnglish = input<boolean>(true);
  readonly currentLanguage = input<AppLanguage>('en');
  readonly mode = input.required<NovenasMode>();
  readonly query = input<string>('');
  readonly selectedDate = input.required<string>();
  readonly todayDate = input.required<string>();
  readonly selectedDateLabel = input.required<string>();
  readonly selectedDateDayNumber = input.required<number>();
  readonly novenasCountLabel = input.required<string>();
  readonly novenasView = input.required<CalendarView>();
  readonly weekdayLabels = input.required<string[]>();
  readonly calendarDays = input.required<Array<{ date: string | null; dayNumber: number | null; label: string; seasonKey?: SeasonKey | null }>>();
  readonly seasonLegend = input.required<Array<{ key: SeasonKey; label: string }>>();
  readonly novenasLoadFailed = input<boolean>(false);
  readonly apiErrorCopy = input.required<string>();
  readonly intentionsResultsLabel = input.required<string>();
  readonly searchPlaceholder = input.required<string>();
  readonly intentionsEmptyCopy = input.required<string>();
  readonly noNovenasCopy = input.required<string>();
  readonly selectedSameAsTodayCopy = input.required<string>();
  readonly previewTodayTitle = input.required<string>();
  readonly previewSelectedTitle = input.required<string>();
  readonly todayPreviewLabel = input.required<string>();
  readonly selectedPreviewLabel = input.required<string>();
  readonly searchResults = input.required<NovenaSummary[]>();
  readonly todayNovenas = input.required<NovenaSummary[]>();
  readonly selectedNovenas = input.required<NovenaSummary[]>();
  readonly todayPrimaryNovenaInput = input<NovenaSummary | null>(null, { alias: 'todayPrimaryNovena' });
  readonly selectedPrimaryNovenaInput = input<NovenaSummary | null>(null, { alias: 'selectedPrimaryNovena' });
  readonly selectedNovenaHeadline = input<NovenaSummary | null>(null);
  readonly selectedNovenaImageStyle = input<string | null>(null);
  readonly selectedSeasonKey = input<SeasonKey | null>(null);

  readonly goHome = output<void>();
  readonly shiftDate = output<-1 | 1>();
  readonly resetDate = output<void>();
  readonly changeView = output<CalendarView>();
  readonly updateQuery = output<string>();
  readonly pickDate = output<string>();
  readonly openNovena = output<NovenaSummary>();

  protected isSelectedDateToday(): boolean {
    return this.selectedDate() === this.todayDate();
  }

  protected novenaDayCountLabel(novena: NovenaSummary): string {
    return this.t(
      `${novena.durationDays}-day novena`,
      `Novena de ${novena.durationDays} días`,
      `${novena.durationDays}-dniowa nowenna`
    );
  }

  protected todayPrimaryNovena(): NovenaSummary | null {
    return this.todayPrimaryNovenaInput();
  }

  protected selectedPrimaryNovena(): NovenaSummary | null {
    return this.selectedPrimaryNovenaInput();
  }

  private featuredNovena(novenas: NovenaSummary[]): NovenaSummary | null {
    if (!novenas.length) {
      return null;
    }

    return [...novenas].sort((left, right) => {
      if (left.durationDays !== right.durationDays) {
        return left.durationDays - right.durationDays;
      }

      return left.title.localeCompare(right.title);
    })[0];
  }

  protected cardImageStyle(imageUrl: string | null | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(6, 12, 18, 0.05), rgba(6, 12, 18, 0.28)), url(${imageUrl})`;
  }

  protected intentionsSummary(novena: NovenaSummary): string {
    const cleaned = (novena.intentions ?? [])
      .map((intention) => intention.trim())
      .filter(Boolean);

    if (!cleaned.length) {
      return this.t('Intentions', 'Intenciones', 'Intencje');
    }

    return cleaned.slice(0, 3).join(' • ');
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
