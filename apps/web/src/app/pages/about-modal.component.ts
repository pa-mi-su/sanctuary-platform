import { Component, input, output } from '@angular/core';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-about-modal',
  standalone: true,
  styleUrl: './about-modal.component.scss',
  template: `
    <div class="modal-backdrop" (click)="close.emit()"></div>
    <section class="about-modal glass-card" [attr.aria-label]="t('About Sanctuary', 'Acerca de Sanctuary', 'O Sanctuary')">
      <div class="about-header">
        <div class="about-heading">
          <img class="about-logo" src="sanctuary-logo-source.png" alt="Sanctuary logo" />
          <div>
            <p class="about-eyebrow">Sanctuary</p>
            <h2>{{ t('About Sanctuary', 'Acerca de Sanctuary', 'O Sanctuary') }}</h2>
          </div>
        </div>
        <button class="close-button" type="button" (click)="close.emit()">×</button>
      </div>

      <div class="about-body">
        <p class="about-copy">
          {{
            t(
              'Sanctuary is a Catholic prayer companion built to keep prayers, novenas, saints, and the liturgical calendar together in one calm place.',
              'Sanctuary es un compañero católico de oración creado para reunir oraciones, novenas, santos y el calendario litúrgico en un solo lugar sereno.',
              'Sanctuary to katolicki towarzysz modlitwy stworzony po to, aby modlitwy, nowenny, święci i kalendarz liturgiczny były razem w jednym spokojnym miejscu.'
            )
          }}
        </p>

        <p class="about-copy">
          {{
            t(
              'If you need help, want to report a bug, or have a question about the app, we would love to hear from you.',
              'Si necesitas ayuda, quieres reportar un error o tienes una pregunta sobre la app, nos encantará escucharte.',
              'Jeśli potrzebujesz pomocy, chcesz zgłosić błąd lub masz pytanie dotyczące aplikacji, chętnie się z Tobą skontaktujemy.'
            )
          }}
        </p>

        <a class="about-email-button" href="mailto:info@mydailysanctuary.com">
          {{ t('Email info@mydailysanctuary.com', 'Escribe a info@mydailysanctuary.com', 'Napisz na info@mydailysanctuary.com') }}
        </a>
      </div>

      <div class="about-links">
        <button type="button" class="about-link-button" (click)="openSupport.emit()">
          {{ t('Support', 'Soporte', 'Wsparcie') }}
        </button>
        <button type="button" class="about-link-button" (click)="openPrivacy.emit()">
          {{ t('Privacy Policy', 'Política de privacidad', 'Polityka prywatnosci') }}
        </button>
      </div>
    </section>
  `,
})
export class AboutModalComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly close = output<void>();
  readonly openSupport = output<void>();
  readonly openPrivacy = output<void>();

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
