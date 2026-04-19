import { Component, inject } from '@angular/core';
import { HomePageComponent } from './pages/home-page.component';
import { AppHeaderComponent } from './pages/app-header.component';
import { SaintsPageComponent } from './pages/saints-page.component';
import { LiturgicalPageComponent } from './pages/liturgical-page.component';
import { NovenasPageComponent } from './pages/novenas-page.component';
import { PrayersPageComponent } from './pages/prayers-page.component';
import { MePageComponent } from './pages/me-page.component';
import { AboutModalComponent } from './pages/about-modal.component';
import { ContentDetailModalComponent } from './pages/content-detail-modal.component';
import { MobileNavComponent } from './pages/mobile-nav.component';
import { AppShellFacade } from './core/state/app-shell.facade';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    HomePageComponent,
    AppHeaderComponent,
    SaintsPageComponent,
    LiturgicalPageComponent,
    NovenasPageComponent,
    PrayersPageComponent,
    MePageComponent,
    AboutModalComponent,
    ContentDetailModalComponent,
    MobileNavComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly facade = inject(AppShellFacade);
}
