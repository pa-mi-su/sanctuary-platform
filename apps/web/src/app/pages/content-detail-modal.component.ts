import { Component, input, output } from '@angular/core';
import { NovenaDayDetail, NovenaDetail, PrayerDetail, SaintDetail } from '../core/api/sanctuary-api.service';

interface NovenaProgress {
  novenaId: string;
  startedAt: string;
  currentDay: number;
  completedDays: number[];
  status: 'active' | 'paused' | 'completed';
}

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-content-detail-modal',
  standalone: true,
  styleUrl: './content-detail-modal.component.scss',
  template: `
    <div class="modal-backdrop" (click)="close.emit()"></div>
    <section class="detail-modal glass-card" aria-label="Content detail">
      <div class="about-header">
        <h2>
          @if (saintDetail()) {
            {{ saintDetail()!.name }}
          } @else if (prayerDetail()) {
            {{ prayerDetail()!.title }}
          } @else if (novenaDetail()) {
            {{ novenaDetail()!.title }}
          }
        </h2>
        <button class="close-button" type="button" (click)="close.emit()">×</button>
      </div>

      @if (saintDetail()) {
        <div class="detail-stack">
          @if (isAuthenticated()) {
            <div class="detail-actions">
              <button class="favorite-button" [class.active]="isSaintFavorite()" type="button" (click)="toggleSaintFavorite.emit()">
                {{ isSaintFavorite() ? 'Favorited Saint' : 'Favorite Saint' }}
              </button>
            </div>
          }
          <div class="detail-hero">
            <div class="detail-image" [style.background-image]="imageStyle(saintDetail()!.imageUrl)"></div>
            <div class="detail-meta">
              <span class="content-tag">{{ saintFeastDateLabel(saintDetail()!) }}</span>
              <span class="content-tag">{{ saintDetail()!.feastLabel }}</span>
            </div>
          </div>
          <section class="detail-section detail-info-card">
            <h3>{{ t('Summary', 'Resumen', 'Podsumowanie') }}</h3>
            <p>{{ saintDetail()!.summary }}</p>
          </section>
          <section class="detail-section detail-info-card">
            <h3>{{ t('Biography', 'Biografía', 'Biografia') }}</h3>
            <p>{{ saintDetail()!.biography }}</p>
          </section>
        </div>
      }

      @if (prayerDetail()) {
        <div class="detail-stack">
          <div class="detail-hero prayer-detail-hero">
            <div class="detail-image prayer-image" [style.background-image]="imageStyle(prayerDetail()!.imageUrl)"></div>
            <div class="detail-meta">
              @if (prayerMeta(prayerDetail()!); as category) {
                <span class="content-tag">{{ category }}</span>
              }
              <p>{{ prayerDetail()!.alternateTitle }}</p>
            </div>
          </div>
          <section class="detail-section">
            <h3>Prayer</h3>
            <p class="detail-copy">{{ displayPrayerBody(prayerDetail()!) }}</p>
          </section>
          <section class="detail-section">
            <h3>Note</h3>
            <p>{{ prayerDetail()!.note }}</p>
          </section>
          @if (prayerDetail()!.tags.length) {
            <section class="detail-chip-row">
              @for (tag of prayerDetail()!.tags; track tag) {
                <span class="content-tag">{{ tag }}</span>
              }
            </section>
          }
        </div>
      }

      @if (novenaDetail()) {
        <div class="detail-stack">
          @if (isAuthenticated()) {
            <div class="detail-actions">
              <button class="favorite-button" [class.active]="isNovenaFavorite()" type="button" (click)="toggleNovenaFavorite.emit()">
                {{ isNovenaFavorite() ? 'Favorited Novena' : 'Favorite Novena' }}
              </button>
              @if (!novenaProgress()) {
                <button class="primary-action" type="button" (click)="startNovena.emit()">Start Novena</button>
              } @else {
                <button class="danger-action" type="button" (click)="stopNovena.emit()">Stop Novena</button>
              }
            </div>
          }
          <div class="detail-hero">
            <div class="detail-image" [style.background-image]="imageStyle(novenaDetail()!.imageUrl)"></div>
            <div class="detail-meta">
              <span class="content-tag">{{ novenaDayCountLabel(novenaDetail()!) }}</span>
              <p>{{ novenaDetail()!.description }}</p>
            </div>
          </div>
          @if (novenaDetail()!.intentions.length) {
            <section class="detail-section">
              <h3>Intentions</h3>
              @for (intention of novenaDetail()!.intentions; track intention) {
                <div class="detail-list-row"><span>{{ intention }}</span></div>
              }
            </section>
          }
          @if (novenaDetail()!.days.length) {
            @if (novenaProgress()) {
              <section class="progress-card">
                <div>
                  <strong>{{ novenaProgressLabel(novenaDetail()!, novenaProgress()!) }}</strong>
                  <span>{{ novenaProgress()!.status === 'completed' ? 'Completed' : 'In progress' }}</span>
                </div>
                <div class="progress-bar">
                  <span [style.width.%]="novenaProgressPercent(novenaDetail()!, novenaProgress()!)"></span>
                </div>
              </section>
            }
            <section class="detail-section">
              <h3>Days</h3>
              <div class="detail-chip-row">
                @for (day of novenaDetail()!.days; track day.dayNumber) {
                  <button
                    class="day-chip"
                    type="button"
                    [class.active-blue]="selectedNovenaDay()?.dayNumber === day.dayNumber"
                    [class.completed]="isDayCompleted(day.dayNumber)"
                    (click)="selectNovenaDay.emit(day.dayNumber)"
                  >
                    {{ isDayCompleted(day.dayNumber) ? '✓' : '' }} Day {{ day.dayNumber }}
                  </button>
                }
              </div>
            </section>
            @if (!isAuthenticated()) {
              <p class="complete-note">Log in or register to start this novena and track your progress.</p>
            }
            @if (selectedNovenaDay()) {
              <section class="detail-section">
                <h3>{{ selectedNovenaDay()!.title }}</h3>
                <p class="detail-copy">{{ selectedNovenaDay()!.body }}</p>
                @if (selectedNovenaDay()!.scripture) {
                  <p><strong>Scripture:</strong> {{ selectedNovenaDay()!.scripture }}</p>
                }
                @if (selectedNovenaDay()!.reflection) {
                  <p><strong>Reflection:</strong> {{ selectedNovenaDay()!.reflection }}</p>
                }
                @if (selectedNovenaDay()!.prayer) {
                  <p class="detail-copy"><strong>Prayer:</strong> {{ selectedNovenaDay()!.prayer }}</p>
                }
                @if (novenaProgress() && !isDayCompleted(selectedNovenaDay()!.dayNumber)) {
                  <button class="primary-action" type="button" (click)="completeNovenaDay.emit()">
                    Mark Day {{ selectedNovenaDay()!.dayNumber }} Complete
                  </button>
                } @else if (novenaProgress()) {
                  <p class="complete-note">This day is complete.</p>
                } @else if (isAuthenticated()) {
                  <p class="complete-note">Start this novena to track daily progress.</p>
                }
              </section>
            }
          }
        </div>
      }
    </section>
  `,
})
export class ContentDetailModalComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly saintDetail = input<SaintDetail | null>(null);
  readonly prayerDetail = input<PrayerDetail | null>(null);
  readonly novenaDetail = input<NovenaDetail | null>(null);
  readonly selectedNovenaDay = input<NovenaDayDetail | null>(null);
  readonly novenaProgress = input<NovenaProgress | null>(null);
  readonly isAuthenticated = input<boolean>(false);
  readonly isSaintFavorite = input<boolean>(false);
  readonly isNovenaFavorite = input<boolean>(false);

  readonly close = output<void>();
  readonly selectNovenaDay = output<number>();
  readonly startNovena = output<void>();
  readonly stopNovena = output<void>();

  protected visibleCategory(category: string | null | undefined): string | null {
    if (!category) {
      return null;
    }

    const normalized = category.trim();
    if (!normalized || normalized.toLowerCase() === 'user_provided' || normalized.toLowerCase() === 'rosary') {
      return null;
    }

    return normalized;
  }

  protected prayerMeta(prayer: PrayerDetail): string | null {
    return this.visibleCategory(prayer.category);
  }

  protected displayPrayerBody(prayer: PrayerDetail): string {
    if (prayer.category.toLowerCase() !== 'rosary') {
      return prayer.body;
    }

    const heading = prayer.alternateTitle?.trim();
    if (!heading) {
      return prayer.body;
    }

    const normalized = prayer.body.replace(/\r\n/g, '\n').trimStart();
    const lines = normalized.split('\n');
    if (lines[0]?.trim() !== heading) {
      return prayer.body;
    }

    return lines.slice(1).join('\n').trimStart();
  }
  readonly completeNovenaDay = output<void>();
  readonly toggleSaintFavorite = output<void>();
  readonly toggleNovenaFavorite = output<void>();

  protected imageStyle(imageUrl: string | null | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return `linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.24)), url(${imageUrl})`;
  }

  protected novenaDayCountLabel(novena: NovenaDetail): string {
    return `${novena.days.length}-day novena`;
  }

  protected saintFeastDateLabel(saint: SaintDetail): string {
    const year = new Date().getFullYear();
    const date = new Date(year, saint.feastMonth - 1, saint.feastDay);

    const formatted = new Intl.DateTimeFormat(this.dateLocale(), {
      month: 'short',
      day: 'numeric',
    }).format(date);

    return `${this.t('Feast date', 'Fecha de fiesta', 'Data swieta')}: ${formatted}`;
  }

  protected isDayCompleted(dayNumber: number): boolean {
    return this.novenaProgress()?.completedDays.includes(dayNumber) ?? false;
  }

  protected novenaProgressPercent(novena: NovenaDetail, progress: NovenaProgress): number {
    if (novena.days.length === 0) {
      return 0;
    }

    return Math.round((progress.completedDays.length / novena.days.length) * 100);
  }

  protected novenaProgressLabel(novena: NovenaDetail, progress: NovenaProgress): string {
    return `${progress.completedDays.length} of ${novena.days.length} days complete`;
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

  private dateLocale(): string {
    switch (this.currentLanguage()) {
      case 'es':
        return 'es-ES';
      case 'pl':
        return 'pl-PL';
      default:
        return 'en-US';
    }
  }
}
