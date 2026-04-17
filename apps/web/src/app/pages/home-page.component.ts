import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-home-page',
  standalone: true,
  styleUrl: './home-page.component.scss',
  template: `
    <div class="home-page">
      <section class="hero glass-card">
        <div class="hero-stack">
          <p class="eyebrow">
            {{ isEnglish() ? 'Catholic prayer companion' : 'Compañero católico de oración' }}
          </p>

          <div class="logo-wrap">
            <img class="logo-image" src="sanctuary-logo-source.png" alt="Sanctuary logo" />
          </div>

          @if (isEnglish()) {
            <h1>Welcome to your sanctuary</h1>
            <p class="hero-question">How do you want to connect with God today?</p>
            <p class="hero-copy-text">
              Prayer, liturgy, saints, and novenas in one calm place.
            </p>
          } @else {
            <h1>Bienvenido a tu santuario</h1>
            <p class="hero-question">¿Cómo quieres conectarte con Dios hoy?</p>
            <p class="hero-copy-text">
              Oración, liturgia, santos y novenas en un solo lugar de paz.
            </p>
          }
        </div>
      </section>

      <section class="quick-links">
        <button class="nav-card action-card" type="button" (click)="openSaints.emit()">
          <div class="nav-card__left">
            <span class="nav-icon saints">👥</span>
            <div class="nav-text">
              <strong>{{ isEnglish() ? 'Saints' : 'Santos' }}</strong>
              <span>{{ isEnglish() ? 'Feasts and biographies' : 'Fiestas y biografías' }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card" type="button" (click)="openNovenas.emit()">
          <div class="nav-card__left">
            <span class="nav-icon novenas">📘</span>
            <div class="nav-text">
              <strong>Novenas</strong>
              <span>{{ isEnglish() ? 'Journeys of prayer' : 'Jornadas de oración' }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card" type="button" (click)="openPrayers.emit()">
          <div class="nav-card__left">
            <span class="nav-icon prayers">🕯</span>
            <div class="nav-text">
              <strong>{{ isEnglish() ? 'Prayers' : 'Oraciones' }}</strong>
              <span>{{ isEnglish() ? 'Daily essentials' : 'Esenciales diarios' }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card" type="button" (click)="openDaily.emit()">
          <div class="nav-card__left">
            <span class="nav-icon daily">☼</span>
            <div class="nav-text">
              <strong>{{ isEnglish() ? 'Daily' : 'Diario' }}</strong>
              <span>{{ isEnglish() ? 'Readings and seasons' : 'Lecturas y tiempos' }}</span>
            </div>
          </div>
          <span class="nav-arrow">↗</span>
        </button>

        <button class="nav-card action-card" type="button" (click)="openIntentions.emit()">
          <div class="nav-card__left">
            <span class="nav-icon intentions">♥</span>
            <div class="nav-text">
              <strong>{{ isEnglish() ? 'Intentions' : 'Intenciones' }}</strong>
              <span>{{ isEnglish() ? 'Search by need' : 'Buscar por necesidad' }}</span>
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

  readonly openAbout = output<void>();
  readonly toggleLanguage = output<void>();
  readonly openSaints = output<void>();
  readonly openNovenas = output<void>();
  readonly openPrayers = output<void>();
  readonly openDaily = output<void>();
  readonly openIntentions = output<void>();
}
