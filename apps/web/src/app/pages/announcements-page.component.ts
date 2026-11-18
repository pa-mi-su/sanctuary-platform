import { Component, inject, input, signal, effect, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AnnouncementInboxService, Announcement } from '../core/state/announcement-inbox.service';
@Component({
  selector: 'app-announcements-page',
  standalone: true,
  imports: [DatePipe],
  template: ` <section class="screen-card glass-card">
    <h2>{{ t('Announcements', 'Anuncios', 'Ogłoszenia') }}</h2>
    <div class="actions">
      <button (click)="inbox.load()" [disabled]="inbox.busy()">
        {{ t('Refresh', 'Actualizar', 'Odśwież') }}</button
      ><button (click)="beginClearAll()" [disabled]="inbox.busy() || !inbox.visible().length">
        {{ t('Clear all', 'Borrar todos', 'Wyczyść wszystkie') }}
      </button>
    </div>
    @if (confirm()) {
      <section role="alertdialog" aria-label="Clear announcements">
        <p>
          {{
            t(
              'Clear all announcements from your inbox?',
              '¿Borrar todos los anuncios de tu bandeja?',
              'Wyczyścić wszystkie ogłoszenia ze skrzynki?'
            )
          }}
        </p>
        <button (click)="clearAll()">
          {{ t('Clear all', 'Borrar todos', 'Wyczyść wszystkie') }}</button
        ><button (click)="confirm.set(false)">{{ t('Cancel', 'Cancelar', 'Anuluj') }}</button>
      </section>
    }
    @if (inbox.error()) {
      <p role="status">{{ inbox.error() }}</p>
    }
    @if (inbox.undo(); as id) {
      <button (click)="inbox.change('restore', id)" [disabled]="inbox.busy()">
        {{ t('Undo clear', 'Deshacer', 'Cofnij') }}
      </button>
    }
    @if (inbox.busy()) {
      <p role="status">{{ t('Loading…', 'Cargando…', 'Wczytywanie…') }}</p>
    }
    @for (row of displayed(); track row.id) {
      <article class="glass-subtle">
        <h3>{{ row.title }}</h3>
        <small>{{ row.published_at | date: 'mediumDate' }}</small>
        <p>{{ row.body }}</p>
        <div class="actions">
          @if (hasContent(row.destination)) {
            <a [href]="safeLink(row.destination)" (click)="open($event, row)">{{
              t('Open content', 'Abrir contenido', 'Otwórz treść')
            }}</a>
          }
          <button (click)="clear(row.id)" [disabled]="inbox.busy()">
            {{ t('Clear', 'Borrar', 'Wyczyść') }}
          </button>
        </div>
      </article>
    }
    @if (!inbox.busy() && !displayed().length) {
      <p>
        {{
          t('No announcements here yet.', 'Todavía no hay anuncios.', 'Nie ma jeszcze ogłoszeń.')
        }}
      </p>
    }
    @if (inbox.more()) {
      <button (click)="inbox.load(true)" [disabled]="inbox.busy()">
        {{ t('Load more', 'Cargar más', 'Więcej') }}
      </button>
    }
  </section>`,
  styles: [
    `
      :host {
        display: block;
        max-width: 680px;
        margin: 0 auto;
      }
      h2 {
        text-align: center;
        font-size: 1.875rem;
      }
      h3 {
        margin: 0;
        font-size: 1.25rem;
        line-height: 1.4;
      }
      article {
        padding: 20px;
        background: #102738;
        border: 1px solid #7cc7de99;
        border-radius: 22px;
        margin: 1rem 0;
      }
      button,
      a {
        font: inherit;
        color: inherit;
        border: 1px solid #7cc7de99;
        background: #2a4153;
        border-radius: 14px;
        padding: 0.8rem;
        min-height: 48px;
        cursor: pointer;
      }
      .actions,
      .heading {
        display: flex;
        gap: 1rem;
        align-items: center;
        justify-content: space-between;
      }
      .heading {
        width: 100%;
        font-weight: 700;
        font-size: 1.2rem;
      }
      p {
        white-space: pre-wrap;
        line-height: 1.6;
      }
      small {
        display: block;
        margin-top: 0.7rem;
      }
    `,
  ],
})
export class AnnouncementsPageComponent {
  requestedId = input('');
  private requested = false;
  private lastReadAttempt = '';
  constructor() {
    effect(() => {
      const id = this.requestedId();
      if (id && !this.requested && !this.inbox.busy()) {
        this.requested = true;
        this.forcedId.set(id);
        void this.inbox.openRequested(id);
      }
    });
    effect(() => {
      if (this.inbox.busy() && !this.inbox.items().length) this.lastReadAttempt = '';
      if (!this.inbox.busy()) {
        const ids = this.displayed()
          .filter((row) => !this.inbox.state().readIds.includes(row.id))
          .map((row) => row.id)
          .slice(0, 100);
        const attempt = ids.join(',');
        if (ids.length && attempt !== this.lastReadAttempt) {
          this.lastReadAttempt = attempt;
          void this.inbox.change('read', undefined, undefined, ids);
        }
      }
    });
  }
  language = input('en');
  inbox = inject(AnnouncementInboxService);
  confirm = signal(false);
  forcedId = signal<string | null>(null);
  clearThrough?: Announcement;
  displayed = computed(() => {
    const rows = this.inbox.visible();
    const forced = this.inbox.items().find((row) => row.id === this.forcedId());
    return forced && !rows.some((row) => row.id === forced.id) ? [forced, ...rows] : rows;
  });
  beginClearAll() {
    this.clearThrough = this.inbox.latest();
    this.confirm.set(true);
  }
  clear(id: string) {
    this.forcedId.set(null);
    void this.inbox.change('clear', id);
  }
  t(en: string, es: string, pl: string) {
    return this.language() === 'es' ? es : this.language() === 'pl' ? pl : en;
  }
  hasContent(value: string) {
    try {
      const u = new URL(value);
      return (
        u.protocol === 'https:' &&
        ['mydailysanctuary.com', 'dev.mydailysanctuary.com'].includes(u.hostname) &&
        /^\/(prayers|saints|novenas)\/[^/]+\/?$/.test(u.pathname)
      );
    } catch {
      return false;
    }
  }
  async open(event: Event, row: Announcement) {
    event.preventDefault();
    await this.inbox.change('read', row.id);
    window.location.assign(this.safeLink(row.destination));
  }
  clearAll() {
    this.confirm.set(false);
    this.forcedId.set(null);
    void this.inbox.change('clearAll', undefined, this.clearThrough);
  }
  safeLink(value: string) {
    try {
      const u = new URL(value);
      return u.protocol === 'https:' &&
        ['mydailysanctuary.com', 'dev.mydailysanctuary.com'].includes(u.hostname)
        ? value
        : '/';
    } catch {
      return '/';
    }
  }
}
