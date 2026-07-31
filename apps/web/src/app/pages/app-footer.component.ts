import { Component, input } from '@angular/core';

type AppLanguage = 'en' | 'es' | 'pl';

@Component({
  selector: 'app-footer',
  standalone: true,
  styleUrl: './app-footer.component.scss',
  template: `
    <footer class="site-footer glass-card">
      <div class="footer-content">
        <div class="brand-lockup">
          <img class="brand-mark" src="/brand-logo.png" alt="" aria-hidden="true" />
          <div class="brand-copy">
            <strong>Sanctuary</strong>
            <span>{{ t('Prayer, peace, and daily devotion', 'Oración, paz y devoción diaria', 'Modlitwa, pokój i codzienna duchowość') }}</span>
          </div>
        </div>

        <span class="footer-rule" aria-hidden="true"></span>

        <div class="footer-actions">
          <section class="social-section" [attr.aria-label]="t('Follow Sanctuary', 'Sigue a Sanctuary', 'Obserwuj Sanctuary')">
            <p class="section-label">{{ t('Follow us', 'Síguenos', 'Obserwuj nas') }}</p>
            <div class="social-links">
              <a
                class="social-link social-link--facebook"
                [href]="facebookUrl"
                target="_blank"
                rel="noopener noreferrer"
                [attr.aria-label]="t('Follow Sanctuary on Facebook', 'Sigue a Sanctuary en Facebook', 'Obserwuj Sanctuary na Facebooku')"
              >
                <span aria-hidden="true">f</span>
              </a>
              <a
                class="social-link social-link--instagram"
                [href]="instagramUrl"
                target="_blank"
                rel="noopener noreferrer"
                [attr.aria-label]="t('Follow Sanctuary on Instagram', 'Sigue a Sanctuary en Instagram', 'Obserwuj Sanctuary na Instagramie')"
              >
                <span class="instagram-mark" aria-hidden="true"></span>
              </a>
            </div>
          </section>

          <span class="action-divider" aria-hidden="true"></span>

          <section class="store-section" [attr.aria-label]="t('Get the Sanctuary app', 'Obtén la app Sanctuary', 'Pobierz aplikację Sanctuary')">
            <a class="store-badge" [href]="appStoreUrl" target="_blank" rel="noopener noreferrer">
              <span class="store-icon store-icon--apple" aria-hidden="true"></span>
              <span class="store-copy">
                <span>{{ t('Download on the', 'Descárgala en', 'Pobierz w') }}</span>
                <strong>App Store</strong>
              </span>
            </a>

            <a
              class="store-badge"
              [href]="googlePlayUrl"
              target="_blank"
              rel="noopener noreferrer"
              [attr.aria-label]="t('Get Sanctuary on Google Play', 'Obtén Sanctuary en Google Play', 'Pobierz Sanctuary z Google Play')"
            >
              <span class="store-icon store-icon--play" aria-hidden="true"></span>
              <span class="store-copy">
                <span>{{ t('Get it on', 'Disponible en', 'Pobierz z') }}</span>
                <strong>Google Play</strong>
              </span>
            </a>
          </section>
        </div>
      </div>
    </footer>
  `,
})
export class AppFooterComponent {
  protected readonly facebookUrl = 'https://www.facebook.com/sanctuarycompanion';
  protected readonly instagramUrl = 'https://www.instagram.com/sanctuarycompanion/';
  protected readonly appStoreUrl = 'https://apps.apple.com/us/app/sanctuary-catholic-companion/id6759986068?uo=4';
  protected readonly googlePlayUrl = 'https://play.google.com/store/apps/details?id=com.pamisu.sanctuary';

  readonly currentLanguage = input<AppLanguage>('en');

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
