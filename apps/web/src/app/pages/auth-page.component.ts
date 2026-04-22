import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SanctuaryAuthService } from '../core/auth/sanctuary-auth.service';

type AppLanguage = 'en' | 'es' | 'pl';
type AuthStep = 'landing' | 'login' | 'register' | 'confirm';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [FormsModule],
  styleUrl: './auth-page.component.scss',
  template: `
    <section class="screen-card auth-screen glass-card">
      <div class="auth-layout">
        <div class="auth-copy">
          <p class="eyebrow">{{ t('Sanctuary account', 'Cuenta de Sanctuary', 'Konto Sanctuary') }}</p>
          <h2>{{ heading() }}</h2>
          <p>{{ supportingCopy() }}</p>

          <ul class="benefit-list">
            <li>{{ t('Track novenas in progress', 'Rastrea novenas en curso', 'Sledz rozpoczete nowenny') }}</li>
            <li>{{ t('Save favorite saints and novenas', 'Guarda santos y novenas favoritas', 'Zapisuj ulubionych swietych i nowenny') }}</li>
            <li>{{ t('Resume later on web, iOS, and Android', 'Continúa luego en web, iOS y Android', 'Wracaj pozniej na web, iOS i Android') }}</li>
          </ul>
        </div>

        <article class="auth-panel glass-subtle">
          @if (step() !== 'landing') {
            <button class="text-button" type="button" (click)="goBack()">
              ← {{ t('Back', 'Atrás', 'Powrót') }}
            </button>
          }

          @if (message()) {
            <p class="configuration-copy">{{ message() }}</p>
          }

          @if (error()) {
            <p class="configuration-copy configuration-copy--warning">{{ error() }}</p>
          }

          @if (step() === 'landing') {
            <div class="choice-stack">
              <button class="choice-card" type="button" (click)="step.set('login')">
                <span class="choice-card__eyebrow">{{ t('Returning to Sanctuary', 'Volver a Sanctuary', 'Powrót do Sanctuary') }}</span>
                <strong>{{ t('Login', 'Iniciar sesión', 'Logowanie') }}</strong>
                <small>{{ t('Sign in to your saved saints, novenas, and progress.', 'Inicia sesión para volver a tus santos, novenas y progreso.', 'Zaloguj sie do zapisanych swietych, nowenn i postepow.') }}</small>
              </button>

              <button class="choice-card choice-card--register" type="button" (click)="step.set('register')">
                <span class="choice-card__eyebrow">{{ t('New to Sanctuary', 'Nuevo en Sanctuary', 'Nowe konto Sanctuary') }}</span>
                <strong>{{ t('Register', 'Registrarse', 'Rejestracja') }}</strong>
                <small>{{ t('Create a free account to sync your prayer life beautifully across devices.', 'Crea una cuenta gratuita para sincronizar tu vida de oración entre dispositivos.', 'Utworz darmowe konto, aby pieknie synchronizowac zycie modlitwy miedzy urzadzeniami.') }}</small>
              </button>
            </div>
          }

          @if (step() === 'login') {
            <form class="auth-form" (ngSubmit)="submitLogin()">
              <div class="panel-heading">
                <h3>{{ t('Login', 'Iniciar sesión', 'Logowanie') }}</h3>
                <p>{{ t('Welcome back. Enter your email and password to continue.', 'Bienvenido de nuevo. Ingresa tu correo y contraseña para continuar.', 'Witaj ponownie. Wpisz email i haslo, aby kontynuowac.') }}</p>
              </div>

              <label class="field">
                <span>{{ t('Email', 'Correo', 'Email') }}</span>
                <input type="email" [(ngModel)]="loginEmail" name="loginEmail" autocomplete="email" />
              </label>

              <label class="field">
                <span>{{ t('Password', 'Contraseña', 'Hasło') }}</span>
                <input type="password" [(ngModel)]="loginPassword" name="loginPassword" autocomplete="current-password" />
              </label>

              <button class="primary-action" type="submit" [disabled]="pending()">
                {{ pending() ? t('Signing in…', 'Entrando…', 'Logowanie…') : t('Login', 'Iniciar sesión', 'Zaloguj się') }}
              </button>
            </form>
          }

          @if (step() === 'register') {
            <form class="auth-form" (ngSubmit)="submitRegister()">
              <div class="panel-heading">
                <h3>{{ t('Register', 'Registrarse', 'Rejestracja') }}</h3>
                <p>{{ t('Create your Sanctuary account with the name people should actually see.', 'Crea tu cuenta de Sanctuary con el nombre que las personas realmente deben ver.', 'Utworz konto Sanctuary z imieniem i nazwiskiem, ktore ludzie naprawde zobacza.') }}</p>
              </div>

              <div class="field-grid">
                <label class="field">
                  <span>{{ t('First name', 'Nombre', 'Imię') }}</span>
                  <input type="text" [(ngModel)]="registerFirstName" name="registerFirstName" autocomplete="given-name" />
                </label>

                <label class="field">
                  <span>{{ t('Last name', 'Apellido', 'Nazwisko') }}</span>
                  <input type="text" [(ngModel)]="registerLastName" name="registerLastName" autocomplete="family-name" />
                </label>
              </div>

              <label class="field">
                <span>{{ t('Email', 'Correo', 'Email') }}</span>
                <input type="email" [(ngModel)]="registerEmail" name="registerEmail" autocomplete="email" />
              </label>

              <div class="field-grid">
                <label class="field">
                  <span>{{ t('Password', 'Contraseña', 'Hasło') }}</span>
                  <input type="password" [(ngModel)]="registerPassword" name="registerPassword" autocomplete="new-password" />
                </label>

                <label class="field">
                  <span>{{ t('Confirm password', 'Confirmar contraseña', 'Potwierdź hasło') }}</span>
                  <input type="password" [(ngModel)]="registerPasswordConfirmation" name="registerPasswordConfirmation" autocomplete="new-password" />
                </label>
              </div>

              <section class="password-panel glass-subtle" [class.password-panel--ready]="isPasswordReady()">
                <div class="password-panel__header">
                  <strong>{{ t('Password strength', 'Seguridad de contraseña', 'Siła hasła') }}</strong>
                  <span [class.password-score--ready]="isPasswordReady()">{{ passwordStrengthLabel() }}</span>
                </div>

                <div class="password-checklist">
                  @for (rule of passwordRules(); track rule.label) {
                    <div class="password-check" [class.password-check--met]="rule.met">
                      <span class="password-check__icon">{{ rule.met ? '✓' : '•' }}</span>
                      <span>{{ rule.label }}</span>
                    </div>
                  }
                </div>

                <div class="password-match" [class.password-match--ready]="passwordsMatch() && !!registerPasswordConfirmation">
                  {{
                    passwordsMatch() && registerPasswordConfirmation
                      ? t('Passwords match.', 'Las contraseñas coinciden.', 'Hasła są zgodne.')
                      : t('Passwords must match before you can create the account.', 'Las contraseñas deben coincidir antes de crear la cuenta.', 'Hasła muszą się zgadzać przed utworzeniem konta.')
                  }}
                </div>
              </section>

              <button class="primary-action" type="submit" [disabled]="pending() || !isPasswordReady() || !passwordsMatch()">
                {{ pending() ? t('Creating account…', 'Creando cuenta…', 'Tworzenie konta…') : t('Create account', 'Crear cuenta', 'Utwórz konto') }}
              </button>
            </form>
          }

          @if (step() === 'confirm') {
            <form class="auth-form" (ngSubmit)="submitConfirmation()">
              <div class="panel-heading">
                <h3>{{ t('Confirm your account', 'Confirma tu cuenta', 'Potwierdź konto') }}</h3>
                <p>
                  {{
                    t(
                      'We sent a confirmation code to',
                      'Enviamos un código de confirmación a',
                      'Wysłaliśmy kod potwierdzający na'
                    )
                  }}
                  <strong>{{ confirmationEmail }}</strong>.
                </p>
              </div>

              <label class="field">
                <span>{{ t('Verification code', 'Código de verificación', 'Kod weryfikacyjny') }}</span>
                <input type="text" [(ngModel)]="confirmationCode" name="confirmationCode" autocomplete="one-time-code" inputmode="numeric" />
              </label>

              <button class="primary-action" type="submit" [disabled]="pending()">
                {{ pending() ? t('Confirming…', 'Confirmando…', 'Potwierdzanie…') : t('Confirm account', 'Confirmar cuenta', 'Potwierdź konto') }}
              </button>

              <button class="secondary-action" type="button" [disabled]="pending()" (click)="resendCode()">
                {{ t('Send a new code', 'Enviar un código nuevo', 'Wyślij nowy kod') }}
              </button>
            </form>
          }
        </article>
      </div>
    </section>
  `,
})
export class AuthPageComponent {
  readonly currentLanguage = input<AppLanguage>('en');
  readonly isConfigured = input<boolean>(false);
  readonly authenticated = output<void>();

  private readonly auth = inject(SanctuaryAuthService);

  protected readonly step = signal<AuthStep>('landing');
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);

  protected loginEmail = '';
  protected loginPassword = '';
  protected registerFirstName = '';
  protected registerLastName = '';
  protected registerEmail = '';
  protected registerPassword = '';
  protected registerPasswordConfirmation = '';
  protected confirmationCode = '';
  protected confirmationEmail = '';

  protected passwordRules(): Array<{ label: string; met: boolean }> {
    const password = this.registerPassword;
    return [
      {
        label: this.t('At least 8 characters', 'Al menos 8 caracteres', 'Co najmniej 8 znaków'),
        met: password.length >= 8,
      },
      {
        label: this.t('One uppercase letter', 'Una letra mayúscula', 'Jedna wielka litera'),
        met: /[A-Z]/.test(password),
      },
      {
        label: this.t('One lowercase letter', 'Una letra minúscula', 'Jedna mała litera'),
        met: /[a-z]/.test(password),
      },
      {
        label: this.t('One number', 'Un número', 'Jedna cyfra'),
        met: /\d/.test(password),
      },
      {
        label: this.t('One special character', 'Un carácter especial', 'Jeden znak specjalny'),
        met: /[^A-Za-z0-9]/.test(password),
      },
    ];
  }

  protected passwordsMatch(): boolean {
    return this.registerPassword === this.registerPasswordConfirmation;
  }

  protected isPasswordReady(): boolean {
    return this.passwordRules().every((rule) => rule.met);
  }

  protected passwordStrengthLabel(): string {
    const metCount = this.passwordRules().filter((rule) => rule.met).length;
    if (metCount === this.passwordRules().length) {
      return this.t('Ready', 'Lista', 'Gotowe');
    }
    if (metCount >= 4) {
      return this.t('Almost there', 'Casi lista', 'Prawie gotowe');
    }
    if (metCount >= 2) {
      return this.t('Needs work', 'Necesita trabajo', 'Wymaga poprawy');
    }
    return this.t('Too weak', 'Demasiado débil', 'Za słabe');
  }

  protected readonly heading = computed(() => {
    switch (this.step()) {
      case 'login':
        return this.t('Welcome back', 'Bienvenido de nuevo', 'Witaj ponownie');
      case 'register':
        return this.t('Create your Sanctuary account', 'Crea tu cuenta de Sanctuary', 'Utwórz konto Sanctuary');
      case 'confirm':
        return this.t('Confirm your account', 'Confirma tu cuenta', 'Potwierdź konto');
      default:
        return this.t('Choose your way in', 'Elige cómo entrar', 'Wybierz drogę wejścia');
    }
  });

  protected readonly supportingCopy = computed(() => {
    switch (this.step()) {
      case 'login':
        return this.t(
          'Everything you have saved in Sanctuary should feel close, calm, and ready to continue.',
          'Todo lo que guardaste en Sanctuary debe sentirse cerca, en calma y listo para continuar.',
          'Wszystko, co zapisano w Sanctuary, powinno być blisko, spokojne i gotowe do kontynuacji.'
        );
      case 'register':
        return this.t(
          'A real account gives you a real home for your favorites, novena progress, and future reminders.',
          'Una cuenta real te da un hogar verdadero para tus favoritos, el progreso de novenas y futuros recordatorios.',
          'Prawdziwe konto daje prawdziwy dom dla ulubionych, postępów nowenn i przyszłych przypomnień.'
        );
      case 'confirm':
        return this.t(
          'One more step and your Sanctuary account is ready.',
          'Un paso más y tu cuenta de Sanctuary estará lista.',
          'Jeszcze jeden krok i konto Sanctuary będzie gotowe.'
        );
      default:
        return this.t(
          'Choose login if you already belong here, or register if this is the beginning of your Sanctuary.',
          'Elige iniciar sesión si ya perteneces aquí, o regístrate si este es el comienzo de tu Sanctuary.',
          'Wybierz logowanie, jeśli już tu należysz, albo rejestrację, jeśli to początek twojego Sanctuary.'
        );
    }
  });

  protected async submitLogin(): Promise<void> {
    if (!this.validateConfigured()) {
      return;
    }

    if (!this.loginEmail.trim() || !this.loginPassword.trim()) {
      this.error.set(this.t('Enter your email and password.', 'Ingresa tu correo y contraseña.', 'Wpisz email i hasło.'));
      return;
    }

    this.pending.set(true);
    this.error.set(null);
    this.message.set(null);

    try {
      await this.auth.login({
        email: this.loginEmail.trim(),
        password: this.loginPassword,
      });
      this.pending.set(false);
      this.authenticated.emit();
    } catch {
      this.pending.set(false);
      this.error.set(this.auth.state().message);
    }
  }

  protected async submitRegister(): Promise<void> {
    if (!this.validateConfigured()) {
      return;
    }

    if (!this.registerFirstName.trim() || !this.registerLastName.trim() || !this.registerEmail.trim() || !this.registerPassword) {
      this.error.set(this.t('Complete every field before creating your account.', 'Completa todos los campos antes de crear tu cuenta.', 'Uzupełnij wszystkie pola przed utworzeniem konta.'));
      return;
    }

    if (this.registerPassword !== this.registerPasswordConfirmation) {
      this.error.set(this.t('Your password confirmation does not match.', 'La confirmación de tu contraseña no coincide.', 'Potwierdzenie hasła nie pasuje.'));
      return;
    }

    if (!this.isPasswordReady()) {
      this.error.set(
        this.t(
          'Choose a password that matches every requirement below before creating your account.',
          'Elige una contraseña que cumpla todos los requisitos antes de crear tu cuenta.',
          'Wybierz hasło spełniające wszystkie wymagania poniżej przed utworzeniem konta.'
        )
      );
      return;
    }

    this.pending.set(true);
    this.error.set(null);
    this.message.set(null);

    try {
      const result = await this.auth.register({
        firstName: this.registerFirstName.trim(),
        lastName: this.registerLastName.trim(),
        email: this.registerEmail.trim(),
        password: this.registerPassword,
      });
      this.pending.set(false);
      this.confirmationEmail = result.email;
      this.confirmationCode = '';
      this.message.set(this.t('Your account is almost ready. Enter the confirmation code we emailed you.', 'Tu cuenta casi está lista. Ingresa el código de confirmación que te enviamos por correo.', 'Twoje konto jest prawie gotowe. Wpisz kod potwierdzający wysłany e-mailem.'));
      this.step.set('confirm');
    } catch {
      this.pending.set(false);
      this.error.set(this.auth.state().message);
    }
  }

  protected async submitConfirmation(): Promise<void> {
    if (!this.confirmationEmail || !this.confirmationCode.trim()) {
      this.error.set(this.t('Enter the confirmation code from your email.', 'Ingresa el código de confirmación de tu correo.', 'Wpisz kod potwierdzający z wiadomości e-mail.'));
      return;
    }

    this.pending.set(true);
    this.error.set(null);

    try {
      const message = await this.auth.confirmRegistration({
        email: this.confirmationEmail,
        code: this.confirmationCode.trim(),
      });
      this.pending.set(false);
      this.message.set(message);
      this.step.set('login');
      this.loginEmail = this.confirmationEmail;
      this.loginPassword = '';
    } catch {
      this.pending.set(false);
      this.error.set(this.auth.state().message);
    }
  }

  protected async resendCode(): Promise<void> {
    if (!this.confirmationEmail) {
      return;
    }

    this.pending.set(true);
    this.error.set(null);

    try {
      const message = await this.auth.resendConfirmation(this.confirmationEmail);
      this.pending.set(false);
      this.message.set(message);
    } catch {
      this.pending.set(false);
      this.error.set(this.auth.state().message);
    }
  }

  protected goBack(): void {
    this.error.set(null);
    this.message.set(null);

    if (this.step() === 'confirm') {
      this.step.set('register');
      return;
    }

    this.step.set('landing');
  }

  private validateConfigured(): boolean {
    if (this.isConfigured()) {
      return true;
    }

    this.error.set(this.t('Authentication is not configured for this environment yet.', 'La autenticación todavía no está configurada para este entorno.', 'Uwierzytelnianie nie jest jeszcze skonfigurowane dla tego środowiska.'));
    return false;
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
