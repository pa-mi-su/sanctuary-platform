import { Component, Input, DestroyRef, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, timeout } from 'rxjs';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';
import { SanctuaryAuthService } from '../core/auth/sanctuary-auth.service';
interface Reminder {
  kind: string;
  slug: string;
  title: string;
  hour: number;
  minute: number;
}
@Component({
  selector: 'app-account-reminders',
  standalone: true,
  imports: [FormsModule],
  template: ` <section class="panel-card glass-subtle">
    <h3>
      {{ t('Scheduled reminders', 'Recordatorios programados', 'Zaplanowane przypomnienia') }}
    </h3>
    @if (problem()) {
      <p role="alert">{{ problem() }}</p>
    }
    @for (item of items(); track item.kind + '/' + item.slug) {
      <article class="reminder-row">
        <a [href]="'/' + item.kind + '/' + item.slug">
          @if (images()[item.kind + '/' + item.slug]) {
            <img [src]="images()[item.kind + '/' + item.slug]" alt="" />
          }
          <span
            ><strong>{{ item.title }}</strong
            ><small>{{ clock(item) }}</small></span
          >
        </a>
        <button type="button" (click)="edit(item)" [disabled]="busy()">
          {{ t('Edit', 'Editar', 'Edytuj') }}
        </button>
        <button
          type="button"
          (click)="remove(item)"
          [disabled]="busy()"
          [attr.aria-label]="t('Delete reminder', 'Eliminar recordatorio', 'Usuń przypomnienie') + ': ' + item.title"
        >
          {{ t('Delete', 'Eliminar', 'Usuń') }}
        </button>
      </article>
    } @empty {
      <p>
        {{
          t(
            'No reminders scheduled. Set one from a prayer or novena in the mobile app.',
            'No hay recordatorios. Crea uno desde una oración o novena en la app móvil.',
            'Brak przypomnień. Ustaw je przy modlitwie lub nowennie w aplikacji mobilnej.'
          )
        }}
      </p>
    }
    @if (editing()) {
      <form (ngSubmit)="save()" class="reminder-editor">
        <label
          >{{ t('Reminder time', 'Hora del recordatorio', 'Godzina przypomnienia') }}
          <input type="time" name="time" required [(ngModel)]="time"
        /></label>
        <button type="submit" [disabled]="busy()">
          {{ t('Save reminder', 'Guardar recordatorio', 'Zapisz przypomnienie') }}
        </button>
        <button type="button" (click)="editing.set(null)">
          {{ t('Cancel', 'Cancelar', 'Anuluj') }}
        </button>
      </form>
    }
    @if (items().length) {
      <button type="button" (click)="clear()" [disabled]="busy()">
        {{
          t(
            'Clear all reminders',
            'Eliminar todos los recordatorios',
            'Usuń wszystkie przypomnienia'
          )
        }}
      </button>
    }
  </section>`,
  styles: `
    :host {
      display: block;
      margin-bottom: 1rem;
    }
    .panel-card {
      padding: 1.2rem;
      border: 1px solid #63818e;
      border-radius: 24px;
    }
    .reminder-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 0;
      border-bottom: 1px solid #63818e;
    }
    a {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex: 1;
      color: inherit;
      text-decoration: none;
    }
    img {
      width: 56px;
      height: 64px;
      object-fit: cover;
      border-radius: 12px;
    }
    small {
      display: block;
      margin-top: 0.4rem;
    }
    button,
    input {
      padding: 0.6rem;
      border: 1px solid #63818e;
      border-radius: 12px;
      background: #203c4b;
      color: inherit;
    }
    .reminder-editor {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      padding: 1rem 0;
    }
  `,
})
export class AccountRemindersComponent {
  @Input() currentLanguage: 'en' | 'es' | 'pl' = 'en';
  t(en: string, es: string, pl: string) {
    return this.currentLanguage === 'es' ? es : this.currentLanguage === 'pl' ? pl : en;
  }
  private http = inject(HttpClient);
  private base = inject(SANCTUARY_API_BASE_URL);
  private auth = inject(SanctuaryAuthService);
  items = signal<Reminder[]>([]);
  images = signal<Record<string, string>>({});
  editing = signal<Reminder | null>(null);
  busy = signal(false);
  problem = signal('');
  time = '';
  private generation = 0;
  constructor() {
    effect(() => {
      const email = this.auth.state().email;
      const member = this.auth.state().status === 'authenticated';
      this.generation++;
      this.items.set([]);
      this.editing.set(null);
      if (member && email) void this.load();
    });
    const refresh = () => {
      if (!document.hidden && !this.busy()) void this.load();
    };
    window.addEventListener('focus', refresh);
    const timer = window.setInterval(refresh, 30000);
    inject(DestroyRef).onDestroy(() => {
      this.generation++;
      clearInterval(timer);
      window.removeEventListener('focus', refresh);
    });
  }
  private request<T>(method: string, path: string, body?: unknown) {
    return firstValueFrom(
      this.http
        .request<T>(method, this.base + path, { body, withCredentials: true })
        .pipe(timeout(15000)),
    );
  }
  async load() {
    if (this.auth.state().status !== 'authenticated') return;
    const generation = this.generation;
    try {
      const rows = await this.request<Reminder[]>('GET', '/me/reminders');
      if (generation !== this.generation) return;
      this.items.set(rows);
      for (const row of rows) {
        const key = row.kind + '/' + row.slug;
        if (!this.images()[key]) {
          const content = await this.request<{ imageUrl?: string }>('GET', '/content/' + key).catch(
            () => ({}) as { imageUrl?: string },
          );
          if (generation !== this.generation) return;
          if (content.imageUrl)
            this.images.update((images) => ({ ...images, [key]: content.imageUrl! }));
        }
      }
    } catch {
      if (generation === this.generation)
        this.problem.set(
          this.t(
            'Reminders could not update. Please try again.',
            'No se pudieron actualizar los recordatorios.',
            'Nie udało się odświeżyć przypomnień.',
          ),
        );
    }
  }
  clock(item: Reminder) {
    return new Date(2000, 0, 1, item.hour, item.minute).toLocaleTimeString([], {
      hour: 'numeric',
      minute: '2-digit',
    });
  }
  edit(item: Reminder) {
    this.editing.set(item);
    this.time = String(item.hour).padStart(2, '0') + ':' + String(item.minute).padStart(2, '0');
  }
  async save() {
    const item = this.editing();
    if (!item || !/^\d{2}:\d{2}$/.test(this.time)) return;
    const [hour, minute] = this.time.split(':').map(Number);
    await this.mutate('PUT', '/me/reminders', { ...item, hour, minute });
  }
  async remove(item: Reminder) {
    await this.mutate('DELETE', '/me/reminders/' + item.kind + '/' + item.slug);
  }
  async clear() {
    if (
      window.confirm(
        this.t(
          'Remove all your prayer and novena reminders?',
          '¿Eliminar todos tus recordatorios?',
          'Usunąć wszystkie przypomnienia?',
        ),
      )
    )
      await this.mutate('DELETE', '/me/reminders');
  }
  private async mutate(method: string, path: string, body?: unknown) {
    if (this.busy()) return;
    const generation = this.generation;
    this.busy.set(true);
    this.problem.set('');
    try {
      await this.request(method, path, body);
      if (generation !== this.generation) return;
      this.editing.set(null);
      await this.load();
    } catch {
      if (generation === this.generation)
        this.problem.set(
          this.t(
            'Could not save your reminder. Please try again.',
            'No se pudo guardar el recordatorio.',
            'Nie udało się zapisać przypomnienia.',
          ),
        );
    } finally {
      if (generation === this.generation) this.busy.set(false);
    }
  }
}
