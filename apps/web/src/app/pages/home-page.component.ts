import { Component, input, output } from '@angular/core';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-home-page',
  standalone: true,
  styleUrl: './home-page.component.scss',
  template: `
    <div class="home-page">
      <section class="hero glass-card">
        <div class="hero-stack">
          <p class="eyebrow">
            {{ t('Catholic prayer companion', 'Compañero católico de oración', 'Katolicki towarzysz modlitwy') }}
          </p>

          <div class="logo-wrap">
            <img class="logo-image" src="brand-logo.png" alt="Sanctuary logo" />
          </div>

          <h1>{{ t('Welcome to your sanctuary', 'Bienvenido a tu santuario', 'Witamy w twoim sanktuarium') }}</h1>
          <p class="hero-question">{{ t('How do you want to connect with God today?', '¿Cómo quieres conectarte con Dios hoy?', 'Jak chcesz dzisiaj polaczyc sie z Bogiem?') }}</p>
          <p class="hero-copy-text">
            {{ t('Prayer, liturgy, saints, and novenas in one calm place.', 'Oración, liturgia, santos y novenas en un solo lugar de paz.', 'Modlitwa, liturgia, swieci i nowenny w jednym spokojnym miejscu.') }}
          </p>
        </div>
      </section>

      <section class="quick-links">
        <button class="nav-card action-card saints-card" type="button" (click)="openSaints.emit()">
          <span class="card-illustration" aria-hidden="true">
            <img src="home-cards/saints.svg" alt="" />
          </span>
          <div class="nav-card__content">
            <span class="nav-icon saints">👥</span>
            <div class="nav-text">
              <strong>{{ t('Saints', 'Santos', 'Swieci') }}</strong>
              <span>{{ t('Feasts and biographies', 'Fiestas y biografías', 'Swieta i biografie') }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card novenas-card" type="button" (click)="openNovenas.emit()">
          <span class="card-illustration" aria-hidden="true">
            <img src="home-cards/novenas.svg" alt="" />
          </span>
          <div class="nav-card__content">
            <span class="nav-icon novenas">📘</span>
            <div class="nav-text">
              <strong>Novenas</strong>
              <span>{{ t('Journeys of prayer', 'Jornadas de oración', 'Drogi modlitwy') }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card prayers-card" type="button" (click)="openPrayers.emit()">
          <span class="card-illustration" aria-hidden="true">
            <img src="home-cards/prayers.svg" alt="" />
          </span>
          <div class="nav-card__content">
            <span class="nav-icon prayers">🕯</span>
            <div class="nav-text">
              <strong>{{ t('Prayers', 'Oraciones', 'Modlitwy') }}</strong>
              <span>{{ t('Daily essentials', 'Esenciales diarios', 'Codzienne podstawy') }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card daily-card" type="button" (click)="openDaily.emit()">
          <span class="card-illustration" aria-hidden="true">
            <img src="home-cards/daily-readings.svg" alt="" />
          </span>
          <div class="nav-card__content">
            <span class="nav-icon daily">☼</span>
            <div class="nav-text">
              <strong>{{ t('Daily Readings', 'Lecturas diarias', 'Czytania dzienne') }}</strong>
              <span>{{ t('Readings and seasons', 'Lecturas y tiempos', 'Czytania i okresy') }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card intentions-card" type="button" (click)="openIntentions.emit()">
          <span class="card-illustration" aria-hidden="true">
            <img src="home-cards/intentions.svg" alt="" />
          </span>
          <div class="nav-card__content">
            <span class="nav-icon intentions">♥</span>
            <div class="nav-text">
              <strong>{{ t('Intentions', 'Intenciones', 'Intencje') }}</strong>
              <span>{{ t('Search by need', 'Buscar por necesidad', 'Szukaj wedlug potrzeby') }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>
      </section>
    </div>
  `,
})
export class HomePageComponent {
  readonly isEnglish = input<boolean>(true);
  readonly currentLanguage = input<AppLanguage>('en');

  readonly openSaints = output<void>();
  readonly openNovenas = output<void>();
  readonly openPrayers = output<void>();
  readonly openDaily = output<void>();
  readonly openIntentions = output<void>();

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
