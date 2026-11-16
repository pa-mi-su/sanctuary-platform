import { Injectable, inject, signal, computed, effect, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';
import { SANCTUARY_API_BASE_URL } from '../api/sanctuary-api.config';
import { SanctuaryAuthService } from '../auth/sanctuary-auth.service';
export interface Announcement {
  id: string;
  title: string;
  body: string;
  destination: string;
  published_at: number;
}
interface InboxState {
  readIds: string[];
  clearedIds: string[];
  clearedBefore: number;
}
const empty = (): InboxState => ({ readIds: [], clearedIds: [], clearedBefore: 0 });
@Injectable({ providedIn: 'root' })
export class AnnouncementInboxService {
  private http = inject(HttpClient);
  private base = inject(SANCTUARY_API_BASE_URL);
  private auth = inject(SanctuaryAuthService);
  items = signal<Announcement[]>([]);
  state = signal<InboxState>(empty());
  busy = signal(false);
  error = signal('');
  more = signal(false);
  undo = signal<string | null>(null);
  private page = 0;
  private generation = 0;
  private key = 'sanctuary-announcements-guest:' + this.base;
  visible = computed(() =>
    this.items().filter(
      (x) => !this.state().clearedIds.includes(x.id) && x.published_at > this.state().clearedBefore,
    ),
  );
  unread = computed(() => this.visible().some((x) => !this.state().readIds.includes(x.id)));
  constructor() {
    effect(() => {
      const status = this.auth.state().status;
      void this.load(false, status === 'authenticated');
    });
    const refresh = () => {
      if (!document.hidden && !this.busy()) void this.load();
    };
    window.addEventListener('focus', refresh);
    inject(DestroyRef).onDestroy(() => window.removeEventListener('focus', refresh));
  }

  private request<T>(method: string, path: string, body?: unknown) {
    return firstValueFrom(
      this.http
        .request<T>(method, this.base + path, { body, withCredentials: true })
        .pipe(timeout(15000)),
    );
  }
  async load(next = false, member = this.auth.state().status === 'authenticated') {
    const generation = ++this.generation;
    this.busy.set(true);
    this.error.set('');
    try {
      if (!next) {
        this.items.set([]);
        this.state.set(empty());
        this.page = 0;
        this.undo.set(null);
        if (member) {
          const loaded = await this.request<InboxState>('GET', '/me/announcements');
          if (generation !== this.generation) return;
          this.state.set(loaded);
        } else {
          try {
            const stored = JSON.parse(localStorage.getItem(this.key) || 'null');
            this.state.set(
              stored &&
                Array.isArray(stored.readIds) &&
                stored.readIds.every((id: unknown) => typeof id === 'string') &&
                Array.isArray(stored.clearedIds) &&
                stored.clearedIds.every((id: unknown) => typeof id === 'string') &&
                Number.isFinite(stored.clearedBefore)
                ? stored
                : empty(),
            );
          } catch {
            this.state.set(empty());
          }
        }
      }
      const page = next ? this.page + 1 : 0;
      const feed = await this.request<{ items: Announcement[]; hasMore: boolean }>(
        'GET',
        '/announcements?page=' + page,
      );
      if (generation !== this.generation) return;
      this.items.set(
        next
          ? [
              ...this.items(),
              ...feed.items.filter((row) => !this.items().some((x) => x.id === row.id)),
            ]
          : feed.items,
      );
      this.page = page;
      this.more.set(feed.hasMore);
    } catch {
      if (generation === this.generation)
        this.error.set('Could not load announcements. Please try again.');
    } finally {
      if (generation === this.generation) this.busy.set(false);
    }
  }
  async openRequested(id: string) {
    if (!/^[0-9a-f-]{36}$/i.test(id)) return;
    const generation = this.generation;
    try {
      if (!this.items().some((row) => row.id === id)) {
        const row = await this.request<Announcement>('GET', '/announcements/' + id);
        if (generation !== this.generation) return;
        this.items.set([row, ...this.items()]);
      }
      if (generation === this.generation) await this.change('read', id);
    } catch {
      this.error.set('This announcement is no longer available.');
    }
  }
  latest() {
    return this.items().reduce<Announcement | undefined>(
      (latest, row) => (!latest || row.published_at > latest.published_at ? row : latest),
      undefined,
    );
  }
  async change(action: string, id?: string, through?: Announcement) {
    if (this.busy()) return;
    const generation = this.generation;
    this.busy.set(true);
    this.error.set('');
    try {
      let state = this.state();
      if (this.auth.state().status === 'authenticated')
        state = await this.request<InboxState>('POST', '/me/announcements', {
          action,
          ids: id ? [id] : [],
          throughId: action === 'clearAll' ? (through ?? this.latest())?.id : null,
        });
      else {
        if (action === 'read' && id)
          state = { ...state, readIds: [...new Set([...state.readIds, id])] };
        if (action === 'clear' && id)
          state = { ...state, clearedIds: [...new Set([...state.clearedIds, id])] };
        if (action === 'restore')
          state = { ...state, clearedIds: state.clearedIds.filter((x) => x !== id) };
        if (action === 'clearAll')
          state = {
            ...state,
            clearedBefore: Math.max(
              state.clearedBefore,
              (through ?? this.latest())?.published_at || 0,
            ),
          };
        localStorage.setItem(this.key, JSON.stringify(state));
      }
      if (generation !== this.generation) return;
      this.state.set(state);
      this.undo.set(action === 'clear' ? id || null : null);
    } catch {
      if (generation === this.generation)
        this.error.set('Could not update your inbox. Please try again.');
    } finally {
      if (generation === this.generation) this.busy.set(false);
    }
  }
}
