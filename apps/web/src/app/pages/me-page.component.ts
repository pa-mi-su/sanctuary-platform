import { Component, input, output } from '@angular/core';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-me-page',
  standalone: true,
  styleUrl: './me-page.component.scss',
  template: `
    <section class="screen-card me-screen glass-card">
      <div class="me-header">
        <div>
          <p class="eyebrow">{{ t('Signed in', 'Sesión iniciada', 'Zalogowano') }}</p>
          <h2>{{ userName() ?? t('Me', 'Yo', 'Ja') }}</h2>
          <p>{{ t('Your novenas in progress and saved favorites.', 'Tus novenas en curso y favoritos guardados.', 'Twoje rozpoczete nowenny i zapisane ulubione.') }}</p>
        </div>
        <button class="logout-button" type="button" (click)="logout.emit()">
          {{ t('Logout', 'Salir', 'Wyloguj') }}
        </button>
      </div>

      <section class="list-stack">
        <article class="panel-card glass-subtle">
          <h3>{{ t('Novenas in Progress', 'Novenas en curso', 'Nowenny w toku') }}</h3>
          <p>{{ t('0 in progress', '0 en curso', '0 w toku') }}</p>
          <div class="divider"></div>
          <p>{{ t('No novenas in progress.', 'No hay novenas en curso.', 'Brak rozpoczetych nowenn.') }}</p>
        </article>

        <article class="panel-card glass-subtle">
          <h3>{{ t('Favorite Novenas', 'Novenas favoritas', 'Ulubione nowenny') }}</h3>
          <div class="divider"></div>
          <p>{{ t('No favorite novenas yet.', 'Todavía no hay novenas favoritas.', 'Brak ulubionych nowenn.') }}</p>
        </article>

        <article class="panel-card glass-subtle">
          <h3>{{ t('Favorite Saints', 'Santos favoritos', 'Ulubieni swieci') }}</h3>
          <div class="divider"></div>
          <p>{{ t('No favorite saints yet.', 'Todavía no hay santos favoritos.', 'Brak ulubionych swietych.') }}</p>
        </article>
      </section>
    </section>
  `,
})
export class MePageComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly userName = input<string | null>(null);
  readonly logout = output<void>();

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
