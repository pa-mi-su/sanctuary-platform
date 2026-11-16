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
        <button class="heading" (click)="expand(row)" [attr.aria-expanded]="expanded() === row.id">
          <span>{{ inbox.state().readIds.includes(row.id) ? '' : '● ' }}{{ row.title }}</span
          ><span>{{ expanded() === row.id ? '⌃' : '⌄' }}</span>
        </button>
        <small>{{ row.published_at | date: 'mediumDate' }}</small>
        <p [class.preview]="expanded() !== row.id">{{ row.body }}</p>
        <div class="actions">
          <a [href]="safeLink(row.destination)" (click)="open($event, row)">{{
            t('Open content', 'Abrir contenido', 'Otwórz treść')
          }}</a
          ><button (click)="clear(row.id)" [disabled]="inbox.busy()">
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
      }
      article {
        padding: 1.2rem;
        border: 1px solid #7894a0;
        border-radius: 22px;
        margin: 1rem 0;
      }
      button,
      a {
        font: inherit;
        color: inherit;
        border: 1px solid #7894a0;
        background: #264553;
        border-radius: 14px;
        padding: 0.8rem;
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
      .preview {
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
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
  constructor() {
    effect(() => {
      const id = this.requestedId();
      if (id && !this.requested && !this.inbox.busy()) {
        this.requested = true;
        this.expanded.set(id);
        this.forcedId.set(id);
        void this.inbox.openRequested(id);
      }
    });
  }
  language = input('en');
  inbox = inject(AnnouncementInboxService);
  expanded = signal<string | null>(null);
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
  expand(row: Announcement) {
    this.expanded.set(this.expanded() === row.id ? null : row.id);
    void this.inbox.change('read', row.id);
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
