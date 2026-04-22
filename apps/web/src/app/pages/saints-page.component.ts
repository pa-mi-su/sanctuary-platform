import { Component, input, output } from '@angular/core';
import { SaintSummary } from '../core/api/sanctuary-api.service';

type CalendarView = 'day' | 'week' | 'month';
type SaintsMode = 'calendar' | 'list';
type SeasonKey = 'ADVENT' | 'CHRISTMAS' | 'LENT' | 'EASTER' | 'ORDINARY';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-saints-page',
  standalone: true,
  styleUrl: './saints-page.component.scss',
  template: `
    <section class="screen-card glass-card">
      @if (mode() === 'list') {
        <div class="screen-header">
          <button class="circle-button" type="button" (click)="goHome.emit()">‹</button>
          <div>
            <h2>{{ t('Saints', 'Santos', 'Swieci') }}</h2>
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
            [placeholder]="t('Search saints', 'Buscar santos', 'Szukaj swietych')"
          />
        </label>

        @if (saintsLoadFailed()) {
          <div class="mode-panel glass-subtle">
            <strong>{{ t('Saints', 'Santos', 'Swieci') }}</strong>
            <p>{{ apiErrorCopy() }}</p>
          </div>
        } @else {
          <section class="list-stack">
            @for (saint of saintResults(); track saint.slug) {
              <button class="content-card glass-subtle content-button" type="button" (click)="openSaint.emit(saint)">
                <div class="content-card__media" [style.background-image]="cardImageStyle(saint.imageUrl)">
                  @if (!saint.imageUrl) {
                    <span class="content-card__fallback">✧</span>
                  }
                </div>
                <div class="content-card__body">
                  <h3>{{ saint.name }}</h3>
                  <p>{{ saint.summary }}</p>
                  <span class="content-tag">{{ saint.feastLabel }}</span>
                </div>
              </button>
            }
          </section>
        }
      } @else {
        <div class="screen-header split">
          <button class="circle-button" type="button" (click)="shiftDate.emit(-1)">‹</button>
          <div class="screen-title">
            <h2>{{ selectedDateLabel() }}</h2>
            <p>{{ saintsCountLabel() }}</p>
          </div>
          <button class="circle-button" type="button" (click)="shiftDate.emit(1)">›</button>
        </div>

        <div class="chip-row">
          <button class="chip selected" type="button" (click)="resetDate.emit()">
            {{ isSelectedDateToday() ? t('Today', 'Hoy', 'Dzisiaj') : t('Jump to Today', 'Ir a hoy', 'Przejdz do dzisiaj') }}
          </button>
          <button class="chip" [class.active-blue]="saintsView() === 'day'" type="button" (click)="changeView.emit('day')">Day</button>
          <button class="chip" [class.active-blue]="saintsView() === 'week'" type="button" (click)="changeView.emit('week')">Week</button>
          <button class="chip" [class.active-blue]="saintsView() === 'month'" type="button" (click)="changeView.emit('month')">Month</button>
        </div>

        <div class="season-legend">
          @for (item of seasonLegend(); track item.key) {
            <span class="season-pill" [attr.data-season]="item.key">{{ item.label }}</span>
          }
        </div>

        @if (saintsView() !== 'day') {
          <div class="calendar-headings">
            @for (label of weekdayLabels(); track label) {
              <span>{{ label }}</span>
            }
          </div>

          <div class="calendar-grid" [class.week-grid]="saintsView() === 'week'" [class.month-grid]="saintsView() === 'month'">
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

        @if (selectedSaintHeadline()) {
          <button class="saint-highlight glass-subtle content-button" type="button" [attr.data-season]="selectedSeasonKey()" (click)="openSaint.emit(selectedSaintHeadline()!)">
            <div class="saint-date">
              <strong>{{ selectedDateDayNumber() }}</strong>
              <span>{{ selectedSaintHeadline()!.name }}</span>
            </div>
            <div class="saint-photo" [style.background-image]="selectedSaintImageStyle()"></div>
            <div class="saint-action">↗</div>
          </button>
        }

        <section class="preview-grid">
          <article class="preview-panel glass-subtle">
            <div class="preview-header">
              <div>
                <h3>{{ previewTodayTitle() }}</h3>
                <p>{{ todayPreviewLabel() }}</p>
              </div>
            </div>

            @if (saintsLoadFailed()) {
              <p class="preview-empty">{{ apiErrorCopy() }}</p>
            } @else if (todaySaints().length) {
              <div class="preview-list">
                @for (saint of todaySaints(); track saint.slug) {
                  <button class="preview-item" type="button" (click)="openSaint.emit(saint)">
                    <div class="preview-item__media" [style.background-image]="cardImageStyle(saint.imageUrl)">
                      @if (!saint.imageUrl) {
                        <span class="content-card__fallback">✧</span>
                      }
                    </div>
                    <div class="preview-item__body">
                      <strong>{{ saint.name }}</strong>
                      <span>{{ saint.feastLabel }}</span>
                    </div>
                  </button>
                }
              </div>
            } @else {
              <p class="preview-empty">{{ noSaintsCopy() }}</p>
            }
          </article>

          <article class="preview-panel glass-subtle">
            <div class="preview-header">
              <div>
                <h3>{{ previewSelectedTitle() }}</h3>
                <p>{{ selectedPreviewLabel() }}</p>
              </div>
            </div>

            @if (saintsLoadFailed()) {
              <p class="preview-empty">{{ apiErrorCopy() }}</p>
            } @else if (isSelectedDateToday()) {
              <p class="preview-empty">{{ selectedSameAsTodayCopy() }}</p>
            } @else if (selectedSaints().length) {
              <div class="preview-list">
                @for (saint of selectedSaints(); track saint.slug) {
                  <button class="preview-item" type="button" (click)="openSaint.emit(saint)">
                    <div class="preview-item__media" [style.background-image]="cardImageStyle(saint.imageUrl)">
                      @if (!saint.imageUrl) {
                        <span class="content-card__fallback">✧</span>
                      }
                    </div>
                    <div class="preview-item__body">
                      <strong>{{ saint.name }}</strong>
                      <span>{{ saint.feastLabel }}</span>
                    </div>
                  </button>
                }
              </div>
            } @else {
              <p class="preview-empty">{{ noSaintsCopy() }}</p>
            }
          </article>
        </section>
      }
    </section>
  `,
})
export class SaintsPageComponent {
  readonly isEnglish = input<boolean>(true);
  readonly currentLanguage = input<AppLanguage>('en');
  readonly mode = input<SaintsMode>('calendar');
  readonly query = input<string>('');
  readonly resultsLabel = input<string>('');
  readonly selectedDate = input.required<string>();
  readonly todayDate = input.required<string>();
  readonly selectedDateLabel = input.required<string>();
  readonly selectedDateDayNumber = input.required<number>();
  readonly saintsCountLabel = input.required<string>();
  readonly saintsView = input.required<CalendarView>();
  readonly weekdayLabels = input.required<string[]>();
  readonly calendarDays = input.required<Array<{ date: string | null; dayNumber: number | null; label: string; seasonKey?: SeasonKey | null }>>();
  readonly seasonLegend = input.required<Array<{ key: SeasonKey; label: string }>>();
  readonly selectedSaintHeadline = input<SaintSummary | null>(null);
  readonly selectedSaintImageStyle = input<string | null>(null);
  readonly selectedSeasonKey = input<SeasonKey | null>(null);
  readonly saintsLoadFailed = input<boolean>(false);
  readonly apiErrorCopy = input.required<string>();
  readonly todaySaints = input.required<SaintSummary[]>();
  readonly selectedSaints = input.required<SaintSummary[]>();
  readonly todayPreviewLabel = input.required<string>();
  readonly selectedPreviewLabel = input.required<string>();
  readonly previewTodayTitle = input.required<string>();
  readonly previewSelectedTitle = input.required<string>();
  readonly noSaintsCopy = input.required<string>();
  readonly selectedSameAsTodayCopy = input.required<string>();
  readonly saintResults = input<SaintSummary[]>([]);

  readonly goHome = output<void>();
  readonly updateQuery = output<string>();
  readonly shiftDate = output<-1 | 1>();
  readonly resetDate = output<void>();
  readonly changeView = output<CalendarView>();
  readonly pickDate = output<string>();
  readonly openSaint = output<SaintSummary>();

  protected isSelectedDateToday(): boolean {
    return this.selectedDate() === this.todayDate();
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
