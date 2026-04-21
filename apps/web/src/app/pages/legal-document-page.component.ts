import { Component, input, output } from '@angular/core';

type LegalDocumentType = 'support' | 'privacy';
type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-legal-document-page',
  standalone: true,
  styleUrl: './legal-document-page.component.scss',
  template: `
    <section class="screen-card document-page glass-card">
      <button class="back-button" type="button" (click)="back.emit()">
        {{ t('Back to About', 'Volver a Acerca de', 'Wroc do O aplikacji') }}
      </button>

      <div class="document-header">
        <p class="eyebrow">Sanctuary</p>
        <h2>{{ title() }}</h2>
        @if (type() === 'privacy') {
          <p class="document-effective">
            {{ t('Effective date: April 13, 2026', 'Fecha de vigencia: 13 de abril de 2026', 'Data wejscia w zycie: 13 kwietnia 2026') }}
          </p>
        }
      </div>

      @if (type() === 'support') {
        <div class="document-body">
          <p>
            {{
              t(
                'If you need help with Sanctuary, have a bug to report, or want to suggest an improvement, contact us at',
                'Si necesitas ayuda con Sanctuary, quieres reportar un error o sugerir una mejora, contáctanos en',
                'Jesli potrzebujesz pomocy z Sanctuary, chcesz zglosic blad lub zaproponowac ulepszenie, skontaktuj sie z nami pod adresem'
              )
            }}
            <a href="mailto:info@mydailysanctuary.com">info&#64;mydailysanctuary.com</a>.
          </p>

          <section>
            <h3>{{ t('Help and Feedback', 'Ayuda y comentarios', 'Pomoc i opinie') }}</h3>
            <p>
              {{
                t(
                  'Please include your device type, browser or platform, and a short description of the issue so we can help as quickly as possible.',
                  'Incluye el tipo de dispositivo, el navegador o la plataforma y una breve descripción del problema para que podamos ayudarte lo antes posible.',
                  'Podaj typ urzadzenia, przegladarke lub platforme oraz krotki opis problemu, abysmy mogli pomoc jak najszybciej.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('App Features', 'Funciones de la app', 'Funkcje aplikacji') }}</h3>
            <p>
              {{
                t(
                  'Sanctuary includes Catholic prayers, novenas, saint reflections, liturgical calendar content, and optional reminders to support prayer throughout the day.',
                  'Sanctuary incluye oraciones católicas, novenas, reflexiones de santos, contenido del calendario litúrgico y recordatorios opcionales para acompañar la oración durante el día.',
                  'Sanctuary zawiera modlitwy katolickie, nowenny, rozwazania o swietych, tresci kalendarza liturgicznego oraz opcjonalne przypomnienia wspierajace modlitwe przez caly dzien.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Response Time', 'Tiempo de respuesta', 'Czas odpowiedzi') }}</h3>
            <p>
              {{
                t(
                  'We do our best to respond to support requests promptly.',
                  'Hacemos todo lo posible por responder rápidamente a las solicitudes de soporte.',
                  'Dokladamy wszelkich staran, aby szybko odpowiadac na prosby o wsparcie.'
                )
              }}
            </p>
          </section>
        </div>
      } @else {
        <div class="document-body">
          <p>
            {{
              t(
                'Sanctuary respects your privacy. This policy explains what information the app uses and how it is handled.',
                'Sanctuary respeta tu privacidad. Esta política explica qué información usa la app y cómo se maneja.',
                'Sanctuary szanuje Twoja prywatnosc. Niniejsza polityka wyjasnia, jakich informacji uzywa aplikacja i w jaki sposob sa one przetwarzane.'
              )
            }}
          </p>

          <section>
            <h3>{{ t('Information We Collect', 'Información que recopilamos', 'Jakie informacje zbieramy') }}</h3>
            <p>
              {{
                t(
                  'Sanctuary is designed to work primarily with local content on your device. We do not require account creation to use the app.',
                  'Sanctuary está diseñado para funcionar principalmente con contenido local en tu dispositivo. No exigimos crear una cuenta para usar la app.',
                  'Sanctuary zostal zaprojektowany tak, aby dzialac przede wszystkim z lokalna zawartoscia na Twoim urzadzeniu. Nie wymagamy tworzenia konta, aby korzystac z aplikacji.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Location Data', 'Datos de ubicación', 'Dane lokalizacyjne') }}</h3>
            <p>
              {{
                t(
                  'If you choose to allow location access, Sanctuary uses your location to help find nearby parishes. Location access is optional and can be changed at any time in your device settings.',
                  'Si decides permitir el acceso a tu ubicación, Sanctuary la usa para ayudarte a encontrar parroquias cercanas. El acceso a la ubicación es opcional y puedes cambiarlo en cualquier momento en la configuración de tu dispositivo.',
                  'Jesli zdecydujesz sie zezwolic na dostep do lokalizacji, Sanctuary wykorzysta ja, aby pomoc znalezc pobliskie parafie. Dostep do lokalizacji jest opcjonalny i mozna go zmienic w dowolnym momencie w ustawieniach urzadzenia.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Notifications', 'Notificaciones', 'Powiadomienia') }}</h3>
            <p>
              {{
                t(
                  'If you choose to allow notifications, Sanctuary uses notification permissions to send reminder notifications for prayer and novena activity. Notifications are optional and can be disabled at any time in your device settings.',
                  'Si decides permitir las notificaciones, Sanctuary usa esos permisos para enviar recordatorios sobre la oración y la actividad de novenas. Las notificaciones son opcionales y pueden desactivarse en cualquier momento desde la configuración de tu dispositivo.',
                  'Jesli zdecydujesz sie zezwolic na powiadomienia, Sanctuary wykorzysta te uprawnienia do wysylania przypomnien o modlitwie i aktywnosci nowenn. Powiadomienia sa opcjonalne i mozna je wylaczyc w dowolnym momencie w ustawieniach urzadzenia.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Data Sharing', 'Intercambio de datos', 'Udostepnianie danych') }}</h3>
            <p>{{ t('We do not sell your personal information.', 'No vendemos tu información personal.', 'Nie sprzedajemy Twoich danych osobowych.') }}</p>
          </section>

          <section>
            <h3>{{ t('Your Choices', 'Tus opciones', 'Twoje wybory') }}</h3>
            <p>
              {{
                t(
                  'You can deny location and notification permissions and still use the core app experience.',
                  'Puedes rechazar los permisos de ubicación y notificaciones y seguir usando la experiencia principal de la app.',
                  'Mozesz odmowic dostepu do lokalizacji i powiadomien, a mimo to nadal korzystac z podstawowej funkcjonalnosci aplikacji.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Contact', 'Contacto', 'Kontakt') }}</h3>
            <p>
              {{ t('If you have privacy questions, contact', 'Si tienes preguntas sobre privacidad, contacta con', 'Jesli masz pytania dotyczace prywatnosci, skontaktuj sie z') }}
              <a href="mailto:info@mydailysanctuary.com">info&#64;mydailysanctuary.com</a>.
            </p>
          </section>
        </div>
      }

      <a class="primary-action" href="mailto:info@mydailysanctuary.com">
        {{ t('Email Support', 'Escribir a soporte', 'Napisz do wsparcia') }}
      </a>
    </section>
  `,
})
export class LegalDocumentPageComponent {
  readonly type = input<LegalDocumentType>('support');
  readonly currentLanguage = input<AppLanguage>('en');
  readonly back = output<void>();

  protected title(): string {
    return this.type() === 'support'
      ? this.t('Sanctuary Support', 'Soporte de Sanctuary', 'Wsparcie Sanctuary')
      : this.t('Sanctuary Privacy Policy', 'Política de privacidad de Sanctuary', 'Polityka prywatnosci Sanctuary');
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
