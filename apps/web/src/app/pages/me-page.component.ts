import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { UserProfile } from '../core/api/sanctuary-api.service';
import { MeLinkedItem } from '../core/state/app-shell.facade';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-me-page',
  standalone: true,
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
      </div>

      <section class="linked-grid">
        <article class="panel-card glass-subtle linked-card">
          <div class="panel-heading">
            <div>
              <h3>{{ t('Active novenas', 'Novenas activas', 'Aktywne nowenny') }}</h3>
              <p>{{ t('Pick up right where you left off.', 'Retoma justo donde lo dejaste.', 'Wroc dokladnie tam, gdzie przerwales.') }}</p>
            </div>
          </div>

          @if (activeNovenas.length) {
            <div class="linked-list">
              @for (item of activeNovenas; track item.id) {
                <button class="linked-row" type="button" (click)="openActiveNovena.emit(item)">
                  <span class="linked-thumb" [class.linked-thumb--empty]="!item.imageUrl">
                    @if (item.imageUrl) {
                      <img [src]="item.imageUrl" [alt]="item.title" />
                    } @else {
                      <span>{{ item.title.charAt(0) }}</span>
                    }
                  </span>
                  <span class="linked-copy">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.subtitle }}</small>
                  </span>
                  <span class="linked-arrow">→</span>
                </button>
              }
            </div>
          } @else {
            <p class="empty-copy">{{ t('No novenas in progress yet.', 'Todavía no hay novenas en progreso.', 'Nie masz jeszcze nowenn w toku.') }}</p>
          }
        </article>

        <article class="panel-card glass-subtle linked-card">
          <div class="panel-heading">
            <div>
              <h3>{{ t('Favorite novenas', 'Novenas favoritas', 'Ulubione nowenny') }}</h3>
              <p>{{ t('Jump back into the novenas you want close at hand.', 'Vuelve rápido a las novenas que quieres tener a mano.', 'Szybko wracaj do nowenn, ktore chcesz miec pod reka.') }}</p>
            </div>
          </div>

          @if (favoriteNovenas.length) {
            <div class="linked-list">
              @for (item of favoriteNovenas; track item.id) {
                <button class="linked-row" type="button" (click)="openFavoriteNovena.emit(item)">
                  <span class="linked-thumb" [class.linked-thumb--empty]="!item.imageUrl">
                    @if (item.imageUrl) {
                      <img [src]="item.imageUrl" [alt]="item.title" />
                    } @else {
                      <span>{{ item.title.charAt(0) }}</span>
                    }
                  </span>
                  <span class="linked-copy">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.subtitle }}</small>
                  </span>
                  <span class="linked-arrow">→</span>
                </button>
              }
            </div>
          } @else {
            <p class="empty-copy">{{ t('No favorite novenas yet.', 'Todavía no hay novenas favoritas.', 'Nie masz jeszcze ulubionych nowenn.') }}</p>
          }
        </article>

        <article class="panel-card glass-subtle linked-card">
          <div class="panel-heading">
            <div>
              <h3>{{ t('Favorite saints', 'Santos favoritos', 'Ulubieni święci') }}</h3>
              <p>{{ t('Keep the saints you return to most in one place.', 'Mantén en un solo lugar a los santos a los que más vuelves.', 'Zachowaj swietych, do ktorych wracasz najczesciej, w jednym miejscu.') }}</p>
            </div>
          </div>

          @if (favoriteSaints.length) {
            <div class="linked-list">
              @for (item of favoriteSaints; track item.id) {
                <button class="linked-row" type="button" (click)="openFavoriteSaint.emit(item)">
                  <span class="linked-thumb" [class.linked-thumb--empty]="!item.imageUrl">
                    @if (item.imageUrl) {
                      <img [src]="item.imageUrl" [alt]="item.title" />
                    } @else {
                      <span>{{ item.title.charAt(0) }}</span>
                    }
                  </span>
                  <span class="linked-copy">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.subtitle }}</small>
                  </span>
                  <span class="linked-arrow">→</span>
                </button>
              }
            </div>
          } @else {
            <p class="empty-copy">{{ t('No favorite saints yet.', 'Todavía no hay santos favoritos.', 'Nie masz jeszcze ulubionych swietych.') }}</p>
          }
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
          </dl>
        </article>
      </section>
    </section>
  `,
})
export class MePageComponent {
  @Input() currentLanguage: AppLanguage = 'en';
  @Input() userName: string | null = null;
  @Input() profile: UserProfile | null = null;
  @Input() activeNovenas: MeLinkedItem[] = [];
  @Input() favoriteNovenas: MeLinkedItem[] = [];
  @Input() favoriteSaints: MeLinkedItem[] = [];

  @Output() readonly logout = new EventEmitter<void>();
  @Output() readonly openActiveNovena = new EventEmitter<MeLinkedItem>();
  @Output() readonly openFavoriteNovena = new EventEmitter<MeLinkedItem>();
  @Output() readonly openFavoriteSaint = new EventEmitter<MeLinkedItem>();

  protected get initials(): string {
    const label = this.userName?.trim() || this.profile?.email?.trim() || 'S';
    return label
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
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
