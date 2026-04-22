import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { UserPreferencesUpdateRequest, UserProfile } from '../core/api/sanctuary-api.service';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-me-page',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './me-page.component.scss',
  template: `
    <section class="screen-card me-screen glass-card">
      <div class="me-header">
        <div class="identity-block">
          <p class="eyebrow">{{ t('Signed in', 'Sesión iniciada', 'Zalogowano') }}</p>
          <div class="identity-row">
            <div class="avatar-shell" [class.avatar-shell--placeholder]="!profile?.avatarUrl">
              @if (profile?.avatarUrl) {
                <img [src]="profile?.avatarUrl" [alt]="userName || 'Sanctuary account'" />
              } @else {
                <span>{{ initials }}</span>
              }
            </div>

            <div>
              <h2>{{ userName || t('Me', 'Yo', 'Ja') }}</h2>
              <p class="identity-email">{{ profile?.email ?? t('Authenticated with Sanctuary account', 'Autenticado con tu cuenta de Sanctuary', 'Zalogowano do konta Sanctuary') }}</p>
              <p>{{ t('Your favorites, active novenas, and future account settings live here.', 'Aquí viven tus favoritos, novenas activas y la futura configuración de tu cuenta.', 'Tutaj znajdują się ulubione, aktywne nowenny i przyszłe ustawienia konta.') }}</p>
            </div>
          </div>
        </div>

        <article class="panel-card glass-subtle member-card">
          <span class="member-card__label">{{ t('Current streak', 'Racha actual', 'Aktualna passa') }}</span>
          <strong>{{ profile?.currentStreakDays ?? 0 }} {{ t('days', 'días', 'dni') }}</strong>
          <small>{{ t('Quiet consistency matters more than speed.', 'La constancia tranquila importa más que la velocidad.', 'Spokojna konsekwencja liczy się bardziej niż tempo.') }}</small>
        </article>
      </div>

      <section class="stats-grid">
        <article class="panel-card glass-subtle stat-card">
          <span class="stat-value">{{ activeNovenaCount }}</span>
          <span class="stat-label">{{ t('Active novenas', 'Novenas activas', 'Aktywne nowenny') }}</span>
        </article>

        <article class="panel-card glass-subtle stat-card">
          <span class="stat-value">{{ completedNovenaCount }}</span>
          <span class="stat-label">{{ t('Completed novenas', 'Novenas completadas', 'Ukonczone nowenny') }}</span>
        </article>

        <article class="panel-card glass-subtle stat-card">
          <span class="stat-value">{{ favoriteSaintCount }}</span>
          <span class="stat-label">{{ t('Favorite saints', 'Santos favoritos', 'Ulubieni swieci') }}</span>
        </article>

        <article class="panel-card glass-subtle stat-card">
          <span class="stat-value">{{ favoriteNovenaCount }}</span>
          <span class="stat-label">{{ t('Favorite novenas', 'Novenas favoritas', 'Ulubione nowenny') }}</span>
        </article>

        <article class="panel-card glass-subtle stat-card">
          <span class="stat-value">{{ profile?.currentStreakDays ?? 0 }}</span>
          <span class="stat-label">{{ t('Current streak', 'Racha actual', 'Aktualna passa') }}</span>
        </article>
      </section>

      <section class="detail-grid">
        <article class="panel-card glass-subtle profile-card">
          <div class="panel-heading">
            <div>
              <h3>{{ t('Profile', 'Perfil', 'Profil') }}</h3>
              <p>{{ t('Identity details that make your Sanctuary account feel human.', 'Detalles de identidad que hacen que tu cuenta de Sanctuary se sienta humana.', 'Szczegóły tożsamości, dzięki którym konto Sanctuary jest naprawdę twoje.') }}</p>
            </div>
          </div>

          <dl class="info-list">
            <div>
              <dt>{{ t('Name', 'Nombre', 'Imię i nazwisko') }}</dt>
              <dd>{{ userName || t('Sanctuary member', 'Miembro de Sanctuary', 'Członek Sanctuary') }}</dd>
            </div>
            <div>
              <dt>{{ t('Email', 'Correo', 'Email') }}</dt>
              <dd>{{ profile?.email ?? '—' }}</dd>
            </div>
            <div>
              <dt>{{ t('Time zone', 'Zona horaria', 'Strefa czasowa') }}</dt>
              <dd>{{ formTimeZoneId || browserTimeZone }}</dd>
            </div>
            <div>
              <dt>{{ t('Longest streak', 'Racha más larga', 'Najdluzsza passa') }}</dt>
              <dd>{{ profile?.longestStreakDays ?? 0 }} {{ t('days', 'días', 'dni') }}</dd>
            </div>
          </dl>
        </article>

        <article class="panel-card glass-subtle settings-card">
          <div class="panel-heading">
            <div>
              <h3>{{ t('Account settings', 'Configuración de cuenta', 'Ustawienia konta') }}</h3>
              <p>{{ t('We are keeping this section focused for now while reminders and notifications are still being built.', 'Por ahora mantenemos esta sección enfocada mientras recordatorios y notificaciones aún se están construyendo.', 'Na razie utrzymujemy tę sekcję w prostocie, dopóki przypomnienia i powiadomienia nie będą gotowe.') }}</p>
            </div>
          </div>

          <div class="field-grid">
            <label class="field">
              <span>{{ t('Time zone', 'Zona horaria', 'Strefa czasowa') }}</span>
              <input
                type="text"
                [(ngModel)]="formTimeZoneId"
                [placeholder]="browserTimeZone"
                spellcheck="false"
                autocapitalize="off"
                autocorrect="off"
              />
            </label>
          </div>

          <button class="secondary-action" type="button" (click)="useBrowserTimeZone()">
            {{ t('Use browser time zone', 'Usar la zona horaria del navegador', 'Uzyj strefy czasowej przegladarki') }}
          </button>

          <div class="settings-footer">
            <p class="status-copy" [class.status-copy--error]="saveError">
              {{
                saveMessage ??
                t(
                  'Time zone is the only saved account setting here right now. Reminders and notifications will come later.',
                  'La zona horaria es la única configuración guardada aquí por ahora. Los recordatorios y notificaciones llegarán después.',
                  'Na razie zapisujemy tutaj tylko strefę czasową. Przypomnienia i powiadomienia pojawią się później.'
                )
              }}
            </p>

            <button class="primary-action" type="button" [disabled]="savePending" (click)="submitPreferences()">
              {{
                savePending
                  ? t('Saving…', 'Guardando…', 'Zapisywanie…')
                  : t('Save settings', 'Guardar configuración', 'Zapisz ustawienia')
              }}
            </button>
          </div>
        </article>
      </section>
    </section>
  `,
})
export class MePageComponent implements OnChanges {
  @Input() currentLanguage: AppLanguage = 'en';
  @Input() userName: string | null = null;
  @Input() profile: UserProfile | null = null;
  @Input() activeNovenaCount = 0;
  @Input() favoriteNovenaCount = 0;
  @Input() favoriteSaintCount = 0;
  @Input() savePending = false;
  @Input() saveMessage: string | null = null;
  @Input() saveError = false;

  @Output() readonly logout = new EventEmitter<void>();
  @Output() readonly savePreferences = new EventEmitter<UserPreferencesUpdateRequest>();

  protected formPreferredLanguage: AppLanguage = 'en';
  protected formTimeZoneId = '';
  protected formNovenaRemindersEnabled = false;
  protected formFeastRemindersEnabled = false;
  protected formEmailUpdatesEnabled = false;
  protected formOnboardingCompleted = true;
  protected readonly browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['profile']?.currentValue) {
      const profile = changes['profile'].currentValue as UserProfile;
      this.formPreferredLanguage = (profile.preferredLanguage ?? this.currentLanguage ?? 'en') as AppLanguage;
      this.formTimeZoneId = profile.timeZoneId ?? this.browserTimeZone;
      this.formNovenaRemindersEnabled = Boolean(profile.novenaRemindersEnabled);
      this.formFeastRemindersEnabled = Boolean(profile.feastRemindersEnabled);
      this.formEmailUpdatesEnabled = Boolean(profile.emailUpdatesEnabled);
      this.formOnboardingCompleted = profile.onboardingCompleted ?? true;
      return;
    }

    if (changes['currentLanguage'] && !this.profile) {
      this.formPreferredLanguage = this.currentLanguage;
    }
  }

  protected get completedNovenaCount(): number {
    return this.profile?.completedNovenaCount ?? 0;
  }

  protected get initials(): string {
    const label = this.userName?.trim() || this.profile?.email?.trim() || 'S';
    return label
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }

  protected useBrowserTimeZone(): void {
    this.formTimeZoneId = this.browserTimeZone;
  }

  protected submitPreferences(): void {
    this.savePreferences.emit({
      preferredLanguage: this.currentLanguage,
      timeZoneId: this.formTimeZoneId.trim() || this.browserTimeZone,
      novenaRemindersEnabled: this.formNovenaRemindersEnabled,
      feastRemindersEnabled: this.formFeastRemindersEnabled,
      emailUpdatesEnabled: this.formEmailUpdatesEnabled,
      onboardingCompleted: this.formOnboardingCompleted,
    });
  }

  protected languageLabel(language: AppLanguage): string {
    switch (language) {
      case 'es':
        return 'Español';
      case 'pl':
        return 'Polski';
      default:
        return 'English';
    }
  }

  protected t(english: string, spanish: string, polish: string): string {
    switch (this.currentLanguage) {
      case 'es':
        return spanish;
      case 'pl':
        return polish;
      default:
        return english;
    }
  }
}
