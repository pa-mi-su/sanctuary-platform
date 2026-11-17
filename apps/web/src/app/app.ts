import { AnnouncementsPageComponent } from './pages/announcements-page.component';
import { AnnouncementInboxService } from './core/state/announcement-inbox.service';
import { RouterOutlet } from '@angular/router';
import { Component, Injector, inject, signal } from '@angular/core';
import { HomePageComponent } from './pages/home-page.component';
import { AppHeaderComponent } from './pages/app-header.component';
import { AppFooterComponent } from './pages/app-footer.component';
import { SaintsPageComponent } from './pages/saints-page.component';
import { LiturgicalPageComponent } from './pages/liturgical-page.component';
import { NovenasPageComponent } from './pages/novenas-page.component';
import { PrayersPageComponent } from './pages/prayers-page.component';
import { MePageComponent } from './pages/me-page.component';
import { AuthPageComponent } from './pages/auth-page.component';
import { AboutPageComponent } from './pages/about-page.component';
import { ContentDetailModalComponent } from './pages/content-detail-modal.component';
import { LegalDocumentPageComponent } from './pages/legal-document-page.component';
import { TermSearchPageComponent } from './pages/term-search-page.component';
import { AppShellFacade } from './core/state/app-shell.facade';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HomePageComponent,
    AppHeaderComponent,
    AppFooterComponent,
    AnnouncementsPageComponent,
    SaintsPageComponent,
    LiturgicalPageComponent,
    NovenasPageComponent,
    PrayersPageComponent,
    AuthPageComponent,
    MePageComponent,
    AboutPageComponent,
    LegalDocumentPageComponent,
    ContentDetailModalComponent,
    TermSearchPageComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected settingsLabel(section: string): string {
    const labels: Record<string, string[]> = { Settings: ['Settings','Ajustes','Ustawienia'], Account: ['Account','Cuenta','Konto'], General: ['General','General','Ogólne'], Notifications: ['Notifications','Notificaciones','Powiadomienia'], 'Contact & Support': ['Contact & Support','Contacto y ayuda','Kontakt i pomoc'], Legal: ['Legal','Legal','Informacje prawne'] };
    const language=this.facade.language(); return labels[section]?.[language === 'es' ? 1 : language === 'pl' ? 2 : 0] || section;
  }
  protected settingsSection = signal<string | null>(null);
  protected overlay = signal<'settings' | 'announcements' | null>(
    typeof window !== 'undefined' && window.location.pathname.startsWith('/announcements')
      ? 'announcements'
      : null,
  );
  protected requestedAnnouncement =
    typeof window !== 'undefined' ? window.location.pathname.split('/')[2] || '' : '';
  protected get inbox() {
    return this.injector.get(AnnouncementInboxService);
  }
  protected readonly isAdmin =
    typeof window !== 'undefined' && /^\/admin\/?$/.test(window.location.pathname);
  private readonly injector = inject(Injector);
  protected get facade(): AppShellFacade {
    return this.injector.get(AppShellFacade);
  }
}
