import { Component, input, output, signal } from '@angular/core';

type AppLanguage = 'en' | 'es' | 'pl';
type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  styleUrl: './auth-page.component.scss',
  template: `
    <section class="screen-card auth-screen glass-card">
      <div class="auth-layout">
        <div class="auth-copy">
          <p class="eyebrow">{{ t('Sanctuary account', 'Cuenta de Sanctuary', 'Konto Sanctuary') }}</p>
          <h2>
            {{
              mode() === 'login'
                ? t('Welcome back', 'Bienvenido de nuevo', 'Witaj ponownie')
                : t('Begin your prayer journey', 'Comienza tu camino de oración', 'Rozpocznij droge modlitwy')
            }}
          </h2>
          <p>
            {{
              t(
                'A free account will keep your novenas, saints, and prayer progress close wherever you open Sanctuary.',
                'Una cuenta gratuita mantendrá tus novenas, santos y progreso de oración cerca donde abras Sanctuary.',
                'Darmowe konto zachowa twoje nowenny, swietych i postep modlitwy wszedzie tam, gdzie otworzysz Sanctuary.'
              )
            }}
          </p>

          <ul class="benefit-list">
            <li>{{ t('Track novenas in progress', 'Rastrea novenas en curso', 'Sledz rozpoczete nowenny') }}</li>
            <li>{{ t('Save favorite saints and novenas', 'Guarda santos y novenas favoritas', 'Zapisuj ulubionych swietych i nowenny') }}</li>
            <li>{{ t('Resume later on web, iOS, and Android', 'Continúa luego en web, iOS y Android', 'Wracaj pozniej na web, iOS i Android') }}</li>
          </ul>
        </div>

        <article class="auth-panel glass-subtle">
          <div class="mode-switch" role="tablist" aria-label="Authentication mode">
            <button class="mode-button" [class.active]="mode() === 'login'" type="button" (click)="mode.set('login')">
              {{ t('Login', 'Iniciar sesión', 'Logowanie') }}
            </button>
            <button class="mode-button" [class.active]="mode() === 'register'" type="button" (click)="mode.set('register')">
              {{ t('Register', 'Registrarse', 'Rejestracja') }}
            </button>
          </div>

          <h3>
            {{
              mode() === 'login'
                ? t('Choose how to continue', 'Elige cómo continuar', 'Wybierz, jak kontynuowac')
                : t('Create your free account', 'Crea tu cuenta gratuita', 'Utworz darmowe konto')
            }}
          </h3>
          <p>
            {{
              mode() === 'login'
                ? t('Sign in to return to your saved prayers and active novenas.', 'Inicia sesión para volver a tus oraciones guardadas y novenas activas.', 'Zaloguj sie, aby wrocic do zapisanych modlitw i aktywnych nowenn.')
                : t('Register to unlock prayer tracking, saved saints, saved novenas, and future sync.', 'Regístrate para desbloquear seguimiento de oración, santos guardados, novenas guardadas y sincronización futura.', 'Zarejestruj sie, aby wlaczyc sledzenie modlitwy, zapisanych swietych, zapisane nowenny i przyszla synchronizacje.')
            }}
          </p>

          <button class="primary-action" type="button" (click)="mode() === 'login' ? login.emit() : register.emit()">
            {{
              mode() === 'login'
                ? t('Continue to my Sanctuary', 'Continuar a mi Sanctuary', 'Przejdz do mojego Sanctuary')
                : t('Register and continue', 'Registrarse y continuar', 'Zarejestruj i kontynuuj')
            }}
          </button>

          @if (!isConfigured()) {
            <p class="configuration-copy">
              {{
                t(
                  'Cognito is ready to connect here. Add the user pool domain and app client config to enable live login.',
                  'Cognito está listo para conectarse aquí. Agrega el dominio del user pool y el cliente de la app para activar el acceso real.',
                  'Cognito jest gotowe do podlaczenia tutaj. Dodaj domene user pool i klienta aplikacji, aby wlaczyc logowanie.'
                )
              }}
            </p>
          }

          @if (authMessage()) {
            <p class="configuration-copy configuration-copy--warning">{{ authMessage() }}</p>
          }

          <p class="legal-copy">
            {{
              t(
                'Account identity will be connected to Cognito next; this page is the production-ready shell for that flow.',
                'La identidad de cuenta se conectará a Cognito después; esta página es la estructura lista para producción de ese flujo.',
                'Tozsamosc konta zostanie nastepnie podlaczona do Cognito; ta strona jest gotowa baza tego przeplywu.'
              )
            }}
          </p>
        </article>
      </div>
    </section>
  `,
})
export class AuthPageComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly isConfigured = input<boolean>(false);
  readonly authMessage = input<string | null>(null);
  readonly login = output<void>();
  readonly register = output<void>();

  protected readonly mode = signal<AuthMode>('login');

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
