import { Component, input, output } from '@angular/core';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-about-page',
  standalone: true,
  styleUrl: './about-page.component.scss',
  template: `
    <section class="screen-card about-screen glass-card">
      <div class="about-hero">
        <p class="eyebrow">Sanctuary</p>
        <h2>{{ t('About Sanctuary', 'Acerca de Sanctuary', 'O Sanctuary') }}</h2>
        <p>
          {{
            t(
              'Sanctuary is a Catholic companion for prayer, daily readings, saints, liturgical living, and novenas.',
              'Sanctuary es un acompañante católico para la oración, las lecturas diarias, los santos, la vida litúrgica y las novenas.',
              'Sanctuary to katolicki towarzysz modlitwy, codziennych czytań, świętych, życia liturgicznego i nowenn.'
            )
          }}
        </p>
      </div>

      <div class="about-grid">
        <article class="download-card glass-subtle">
          <div>
            <p class="eyebrow">{{ t('iPhone app', 'App para iPhone', 'Aplikacja na iPhone') }}</p>
            <h3>{{ t('Download Sanctuary on the App Store', 'Descarga Sanctuary en App Store', 'Pobierz Sanctuary w App Store') }}</h3>
            <p>
              {{
                t(
                  'Take the same prayer companion with you on iPhone for novenas, saints, liturgical browsing, and reminders.',
                  'Lleva contigo el mismo acompañante de oración en iPhone para novenas, santos, calendario litúrgico y recordatorios.',
                  'Zabierz tego samego towarzysza modlitwy na iPhone: nowenny, swieci, kalendarz liturgiczny i przypomnienia.'
                )
              }}
            </p>
          </div>
          <a class="app-store-action" [href]="appStoreUrl" target="_blank" rel="noreferrer">
            <span>{{ t('Download on the', 'Descargar en', 'Pobierz w') }}</span>
            <strong>{{ t('App Store', 'App Store', 'App Store') }}</strong>
          </a>
        </article>

        <article class="about-card glass-subtle">
          <h3>{{ t('What Sanctuary includes', 'Qué incluye Sanctuary', 'Co zawiera Sanctuary') }}</h3>
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
        </article>

        <article class="about-card glass-subtle">
          <h3>{{ t('References', 'Referencias', 'Źródła') }}</h3>
          <p>
            {{
              t(
                'Sanctuary currently references these public sources for readings and saint information.',
                'Sanctuary actualmente hace referencia a estas fuentes públicas para lecturas e información sobre santos.',
                'Sanctuary obecnie korzysta z tych publicznych źródeł dla czytań i informacji o świętych.'
              )
            }}
          </p>
          <div class="action-stack">
            <a class="secondary-action" href="https://bible.usccb.org/daily-bible-reading" target="_blank" rel="noreferrer">
              {{ t('USCCB Daily Bible Reading', 'Lecturas diarias USCCB', 'Codzienne czytania USCCB') }}
            </a>
            <a class="secondary-action" href="https://www.wikipedia.org/" target="_blank" rel="noreferrer">Wikipedia</a>
          </div>
        </article>

        <article class="about-card about-card--wide glass-subtle">
          <h3>{{ t('Contact & feedback', 'Contacto y comentarios', 'Kontakt i opinie') }}</h3>
          <p>
            {{
              t(
                'To report bugs, request corrections, or send feedback, contact us and include the page or feature you were using along with a short description of the issue.',
                'Para reportar errores, solicitar correcciones o enviar comentarios, contáctanos e incluye la página o función que estabas usando junto con una breve descripción del problema.',
                'Aby zgłosić błąd, poprosić o poprawkę lub przesłać opinię, skontaktuj się z nami i podaj stronę lub funkcję, z której korzystałeś(-aś), wraz z krótkim opisem problemu.'
              )
            }}
          </p>
          <div class="action-stack action-stack--row">
            <a class="primary-action" href="mailto:info@mydailysanctuary.com">
              {{ t('Email Support', 'Escribir a soporte', 'Napisz do wsparcia') }}
            </a>
            <button type="button" class="secondary-action" (click)="openSupport.emit()">
              {{ t('Support', 'Soporte', 'Wsparcie') }}
            </button>
            <button type="button" class="secondary-action" (click)="openPrivacy.emit()">
              {{ t('Privacy Policy', 'Política de privacidad', 'Polityka prywatności') }}
            </button>
          </div>
        </article>
      </div>
    </section>
  `,
})
export class AboutPageComponent {
  protected readonly appStoreUrl = 'https://apps.apple.com/us/app/sanctuary-prayer-peace/id6759986068?uo=4';

  readonly currentLanguage = input<AppLanguage>('en');
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
