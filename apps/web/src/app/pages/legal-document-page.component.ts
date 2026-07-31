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
            {{ t('Effective date: July 30, 2026', 'Fecha de vigencia: 30 de julio de 2026', 'Data wejscia w zycie: 30 lipca 2026') }}
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
                  'You can use Sanctuary without an account. If you create an account, Sanctuary processes your name, email address, authentication information, preferences, favorites, and novena progress so those features can work and sync across supported devices. We may also process basic technical information, such as request timestamps and IP addresses, to deliver and secure the service.',
                  'Puedes usar Sanctuary sin una cuenta. Si creas una cuenta, Sanctuary procesa tu nombre, correo electrónico, información de autenticación, preferencias, favoritos y progreso de novenas para que esas funciones funcionen y se sincronicen entre dispositivos compatibles. También podemos procesar información técnica básica, como marcas de tiempo de solicitudes y direcciones IP, para prestar y proteger el servicio.',
                  'Mozesz korzystac z Sanctuary bez konta. Jesli utworzysz konto, Sanctuary przetwarza Twoje imie, adres e-mail, informacje uwierzytelniajace, preferencje, ulubione elementy i postepy nowenn, aby te funkcje dzialaly i synchronizowaly sie na obslugiwanych urzadzeniach. Mozemy rowniez przetwarzac podstawowe informacje techniczne, takie jak znaczniki czasu zadan i adresy IP, aby swiadczyc i zabezpieczac usluge.'
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
            <p>
              {{
                t(
                  'We do not sell your personal information or use it for third-party advertising. We share data only with trusted service providers as needed to operate, secure, and distribute Sanctuary, or when required by law. These providers include Amazon Web Services for hosting, databases, authentication, and service delivery, and Google Play for Android app distribution.',
                  'No vendemos tu información personal ni la usamos para publicidad de terceros. Compartimos datos únicamente con proveedores de confianza cuando es necesario para operar, proteger y distribuir Sanctuary, o cuando lo exige la ley. Estos proveedores incluyen Amazon Web Services para alojamiento, bases de datos, autenticación y prestación del servicio, y Google Play para distribuir la aplicación Android.',
                  'Nie sprzedajemy Twoich danych osobowych ani nie wykorzystujemy ich do reklam podmiotow trzecich. Udostepniamy dane wylacznie zaufanym dostawcom, gdy jest to potrzebne do dzialania, zabezpieczenia i dystrybucji Sanctuary lub gdy wymaga tego prawo. Dostawcy ci obejmuja Amazon Web Services w zakresie hostingu, baz danych, uwierzytelniania i swiadczenia uslugi oraz Google Play w zakresie dystrybucji aplikacji Android.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Data Retention and Deletion', 'Retención y eliminación de datos', 'Przechowywanie i usuwanie danych') }}</h3>
            <p>
              {{
                t(
                  'Account data is retained while your account is active. You can permanently delete your account from the Me section of the app. Your account profile, favorites, novena progress, and preferences are then removed. We may retain limited identifiers in hashed or restricted form when needed for security, fraud prevention, legal obligations, or to honor the deletion.',
                  'Los datos de la cuenta se conservan mientras tu cuenta esté activa. Puedes eliminarla permanentemente desde la sección Yo de la aplicación. Entonces se eliminan el perfil, los favoritos, el progreso de novenas y las preferencias. Podemos conservar identificadores limitados de forma cifrada o restringida cuando sea necesario por seguridad, prevención del fraude, obligaciones legales o para respetar la eliminación.',
                  'Dane konta sa przechowywane, gdy konto jest aktywne. Mozesz trwale usunac konto w sekcji Ja aplikacji. Profil konta, ulubione, postepy nowenn i preferencje zostana wtedy usuniete. Mozemy zachowac ograniczone identyfikatory w postaci skrotu lub z ograniczonym dostepem, gdy jest to potrzebne dla bezpieczenstwa, zapobiegania oszustwom, obowiazkow prawnych lub realizacji usuniecia.'
                )
              }}
            </p>
          </section>

          <section>
            <h3>{{ t('Your Choices', 'Tus opciones', 'Twoje wybory') }}</h3>
            <p>
              {{
                t(
                  'You can decline notification permission and still use the core app experience. You can review and update your preferences in the app, remove local app data through your device settings, or delete your account from the Me section.',
                  'Puedes rechazar el permiso de notificaciones y seguir usando la experiencia principal. Puedes revisar y actualizar tus preferencias en la aplicación, eliminar los datos locales desde los ajustes del dispositivo o eliminar tu cuenta desde la sección Yo.',
                  'Mozesz odmowic zgody na powiadomienia i nadal korzystac z podstawowej funkcjonalnosci aplikacji. Mozesz przegladac i aktualizowac preferencje w aplikacji, usunac dane lokalne w ustawieniach urzadzenia lub usunac konto w sekcji Ja.'
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

      <p class="document-footnote">
        {{
          t(
            'Sanctuary © 2026. All rights reserved.',
            'Sanctuary © 2026. Todos los derechos reservados.',
            'Sanctuary © 2026. Wszelkie prawa zastrzezone.'
          )
        }}
      </p>
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
