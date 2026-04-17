import { Component, input, output } from '@angular/core';
import { LiturgicalDayResponse } from '../core/api/sanctuary-api.service';

type CalendarView = 'day' | 'week' | 'month';

@Component({
  selector: 'app-liturgical-page',
  standalone: true,
  styleUrl: './liturgical-page.component.scss',
  template: `
    <section class="screen-card glass-card">
      <div class="screen-header split">
        <button class="circle-button" type="button" (click)="shiftDate.emit(-1)">‹</button>
        <div class="screen-title">
          <h2>{{ selectedDateLabel() }}</h2>
          <p>{{ selectedDateSeasonLabel() }}</p>
        </div>
        <button class="circle-button" type="button" (click)="shiftDate.emit(1)">›</button>
      </div>

      <div class="chip-row">
        <button class="chip selected" type="button" (click)="resetDate.emit()">
          {{ isSelectedDateToday() ? 'Today' : 'Jump to Today' }}
        </button>
        <button class="chip" [class.active-blue]="liturgicalView() === 'day'" type="button" (click)="changeView.emit('day')">Day</button>
        <button class="chip" [class.active-blue]="liturgicalView() === 'week'" type="button" (click)="changeView.emit('week')">Week</button>
        <button class="chip" [class.active-blue]="liturgicalView() === 'month'" type="button" (click)="changeView.emit('month')">Month</button>
      </div>

      @if (liturgicalView() !== 'day') {
        <div class="calendar-headings">
          @for (label of weekdayLabels(); track label) {
            <span>{{ label }}</span>
          }
        </div>

        <div class="calendar-grid" [class.week-grid]="liturgicalView() === 'week'" [class.month-grid]="liturgicalView() === 'month'">
          @for (day of calendarDays(); track day.date ?? $index) {
            <button
              class="calendar-day calendar-button"
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

      @if (liturgicalLoadFailed()) {
        <div class="mode-panel glass-subtle">
          <strong>Liturgical Day</strong>
          <p>{{ apiErrorCopy() }}</p>
        </div>
      } @else {
        <section class="preview-grid">
          <article class="preview-panel glass-subtle">
            <div class="preview-header">
              <div>
                <h3>{{ previewTodayTitle() }}</h3>
                <p>{{ todayPreviewLabel() }}</p>
              </div>
            </div>

            @if (todayLiturgical()) {
              <div class="preview-copy">
                <strong>{{ todayLiturgical()!.primaryRank }}</strong>
                <p>{{ liturgicalSubtitle(todayLiturgical()!) }}</p>
                <a class="text-link" [href]="todayLiturgical()!.readingsUrl" target="_blank" rel="noreferrer">
                  Open daily readings
                </a>
              </div>
            } @else {
              <p class="preview-empty">{{ noLiturgicalCopy() }}</p>
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
            } @else if (selectedLiturgical()) {
              <div class="preview-copy">
                <strong>{{ selectedLiturgical()!.primaryRank }}</strong>
                <p>{{ liturgicalSubtitle(selectedLiturgical()!) }}</p>
                <a class="text-link" [href]="selectedLiturgical()!.readingsUrl" target="_blank" rel="noreferrer">
                  Open daily readings
                </a>
              </div>
            } @else {
              <p class="preview-empty">{{ noLiturgicalCopy() }}</p>
            }
          </article>
        </section>
      }
    </section>
  `,
})
export class LiturgicalPageComponent {
  readonly selectedDate = input.required<string>();
  readonly todayDate = input.required<string>();
  readonly selectedDateLabel = input.required<string>();
  readonly selectedDateSeasonLabel = input.required<string>();
  readonly liturgicalView = input.required<CalendarView>();
  readonly weekdayLabels = input.required<string[]>();
  readonly calendarDays = input.required<Array<{ date: string | null; dayNumber: number | null; label: string }>>();
  readonly liturgicalLoadFailed = input<boolean>(false);
  readonly apiErrorCopy = input.required<string>();
  readonly todayLiturgical = input<LiturgicalDayResponse | null>(null);
  readonly selectedLiturgical = input<LiturgicalDayResponse | null>(null);
  readonly todayPreviewLabel = input.required<string>();
  readonly selectedPreviewLabel = input.required<string>();
  readonly previewTodayTitle = input.required<string>();
  readonly previewSelectedTitle = input.required<string>();
  readonly noLiturgicalCopy = input.required<string>();
  readonly selectedSameAsTodayCopy = input.required<string>();

  readonly shiftDate = output<-1 | 1>();
  readonly resetDate = output<void>();
  readonly changeView = output<CalendarView>();
  readonly pickDate = output<string>();

  protected isSelectedDateToday(): boolean {
    return this.selectedDate() === this.todayDate();
  }

  protected liturgicalSubtitle(day: LiturgicalDayResponse): string {
    return `${day.season.replaceAll('_', ' ')} • ${day.rankType}`;
  }
}
