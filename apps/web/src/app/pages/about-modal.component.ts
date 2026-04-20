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
        <button class="close-pill" type="button" (click)="close.emit()">
          {{ t('Close', 'Cerrar', 'Zamknij') }}
        </button>
      </div>

      <div class="about-stack">
        <section class="about-card">
          <h2>Sanctuary</h2>
          <p>
            {{
              t(
                'Sanctuary is a Catholic companion for prayer, daily readings, saints, liturgical living, and novenas.',
                'Sanctuary es un acompañante católico para la oración, las lecturas diarias, los santos, la vida litúrgica y las novenas.',
                'Sanctuary to katolicki towarzysz modlitwy, codziennych czytań, świętych, życia liturgicznego i nowenn.'
              )
            }}
          </p>
          <ul>
            <li>
              {{
                t(
                  'Liturgical: day, week, and month calendar views with season context and direct daily readings links.',
                  'Litúrgico: vistas de calendario por día, semana y mes con contexto de temporada y enlaces directos a las lecturas diarias.',
                  'Liturgia: widoki kalendarza dziennego, tygodniowego i miesięcznego z kontekstem okresu oraz bezpośrednimi linkami do codziennych czytań.'
                )
              }}
            </li>
            <li>
              {{
                t(
                  'Saints: date-aware saint listings, detailed profiles, and searchable content.',
                  'Santos: listados de santos según la fecha, perfiles detallados y contenido con búsqueda.',
                  'Święci: listy świętych zależne od daty, szczegółowe profile i przeszukiwalne treści.'
                )
              }}
            </li>
            <li>
              {{
                t(
                  'Novenas: rule-based start dates, end-date tracking, intentions search, and progress management.',
                  'Novenas: fechas de inicio basadas en reglas, seguimiento de fecha final, búsqueda de intenciones y gestión del progreso.',
                  'Nowenny: daty rozpoczęcia oparte na regułach, śledzenie dat końcowych, wyszukiwanie intencji i zarządzanie postępem.'
                )
              }}
            </li>
          </ul>
        </section>

        <section class="about-card">
          <h2>{{ t('References', 'Referencias', 'Źródła') }}</h2>
          <p>
            {{
              t(
                'Sanctuary currently references these public sources for readings and saint information.',
                'Sanctuary actualmente hace referencia a estas fuentes públicas para lecturas e información sobre santos.',
                'Sanctuary obecnie korzysta z tych publicznych źródeł dla czytań i informacji o świętych.'
              )
            }}
          </p>
          <ul>
            <li>{{ t('USCCB (daily readings)', 'USCCB (lecturas diarias)', 'USCCB (codzienne czytania)') }}</li>
            <li>Wikipedia</li>
          </ul>
          <div class="about-actions">
            <a class="about-secondary-button" href="https://bible.usccb.org/daily-bible-reading" target="_blank" rel="noreferrer">
              {{ t('USCCB Daily Bible Reading', 'Lecturas diarias USCCB', 'Codzienne czytania USCCB') }}
            </a>
            <a class="about-secondary-button" href="https://www.wikipedia.org/" target="_blank" rel="noreferrer">
              Wikipedia
            </a>
          </div>
        </section>

        <section class="about-card">
          <h2>{{ t('Contact & feedback', 'Contacto y comentarios', 'Kontakt i opinie') }}</h2>
          <p>
            {{
              t(
                'To report bugs, request corrections, or send feedback, contact us and include the page or feature you were using along with a short description of the issue.',
                'Para reportar errores, solicitar correcciones o enviar comentarios, contáctanos e incluye la página o función que estabas usando junto con una breve descripción del problema.',
                'Aby zgłosić błąd, poprosić o poprawkę lub przesłać opinię, skontaktuj się z nami i podaj stronę lub funkcję, z której korzystałeś(-aś), wraz z krótkim opisem problemu.'
              )
            }}
          </p>
          <div class="about-actions">
            <a class="about-email-button" href="mailto:info@mydailysanctuary.com">
              {{ t('Email info@mydailysanctuary.com', 'Escribe a info@mydailysanctuary.com', 'Napisz na info@mydailysanctuary.com') }}
            </a>
            <button type="button" class="about-link-button" (click)="openSupport.emit()">
              {{ t('Support', 'Soporte', 'Wsparcie') }}
            </button>
            <button type="button" class="about-link-button" (click)="openPrivacy.emit()">
              {{ t('Privacy Policy', 'Política de privacidad', 'Polityka prywatności') }}
            </button>
          </div>
        </section>
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
