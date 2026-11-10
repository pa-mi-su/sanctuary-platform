import { Component, HostListener, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';

type Row = Record<string, any>;
type Field = { name: string; type: string; required: boolean; generated: boolean };
@Component({
  selector: 'app-admin-page', standalone: true, imports: [FormsModule, DatePipe],
  templateUrl: './admin-page.component.html', styleUrl: './admin-page.component.scss',
})
export class AdminPageComponent implements OnDestroy {
  // Bypass public account cookies/refresh interception. Admin tokens live only in this component.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly base = inject(SANCTUARY_API_BASE_URL) + '/admin-api';
  private token = '';
  readonly signedIn = signal(false);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly error = signal(false);
  mode = ''; newPassword = ''; confirmPassword = '';
  email = ''; password = ''; code = ''; challenge = ''; secret = '';
  section = 'content'; entity = 'prayers'; query = ''; page = 0; hasMore = false;
  rows: Row[] = []; fields: Field[] = []; values: Row | null = null; version = ''; editingId = '';
  creating = false; reason = ''; confirmPublish = false;
  dayFields: Field[] = []; newDays: Row[] = [];
  links: Record<string, string[]> = {}; options: Record<string, Row[]> = {}; tags = '';
  summary: Row = {}; user: Row | null = null; userPage = 0; userId = ''; activeOnly = false;
  registrationCursor = ''; registrationNext = ''; registrationHistory: string[] = [];
  auditSnapshot: Row | null = null;
  private lastActivity = Date.now();
  private generation = 0;
  private readonly timeout = setInterval(() => {
    if (this.signedIn() && Date.now() - this.lastActivity >= 15 * 60_000) void this.logout('Signed out after 15 minutes of inactivity.');
  }, 15_000);
  readonly collections = [
    ['prayers', 'Prayers & rosary'], ['saints', 'Saints'], ['novenas', 'Novenas'],
    ['novena-days', 'Novena days'], ['intentions', 'Intentions'], ['patronages', 'Patronages'],
  ];
  @HostListener('document:pointerdown') @HostListener('document:keydown') activity() {
    if (this.signedIn() && Date.now() - this.lastActivity >= 15 * 60_000) { void this.logout('Your session expired.'); return; }
    this.lastActivity = Date.now();
  }
  ngOnDestroy() { clearInterval(this.timeout); this.clear(); }
  private async api<T = any>(method: string, path: string, body?: unknown): Promise<T> {
    const generation = this.generation;
    const result = await firstValueFrom(this.http.request<T>(method, this.base + path, {
      body, withCredentials: false,
      headers: { 'X-Sanctuary-Admin': '1', ...(this.token ? { Authorization: `Bearer ${this.token}` } : {}) },
    }));
    if (generation !== this.generation) throw new Error('Session ended');
    return result;
  }
  private async run(action: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true); this.message.set(''); this.error.set(false);
    try { await action(); }
    catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 401 && this.signedIn()) this.clear();
      this.error.set(true); this.message.set(e instanceof HttpErrorResponse ? (e.error?.message || 'Request failed. Please try again.') : 'Could not complete this action.');
    } finally { this.busy.set(false); }
  }
  async login() {
    await this.run(async () => {
      const result = await this.api('POST', '/login', { email: this.email, password: this.password });
      this.password = ''; this.challenge = result.challenge; this.secret = result.secret; this.mode = result.mode;
    });
    this.password = '';
  }
  async setPassword() {
    if (this.newPassword !== this.confirmPassword) { this.error.set(true); this.message.set('The passwords must match.'); return; }
    await this.run(async () => {
      const result = await this.api('POST', '/password', { challenge: this.challenge, password: this.newPassword });
      this.challenge = result.challenge; this.secret = result.secret; this.mode = result.mode;
    });
    this.newPassword = ''; this.confirmPassword = '';
  }
  async verify() {
    await this.run(async () => {
      const result = await this.api('POST', '/verify', { challenge: this.challenge, code: this.code });
      this.token = result.token; this.signedIn.set(true); this.challenge = ''; this.secret = ''; this.code = ''; this.email = '';
      this.lastActivity = Date.now(); await this.load();
    });
    this.code = '';
  }
  async logout(message = 'Signed out.') {
    const token = this.token; this.clear(); this.message.set(message);
    if (token) {
      try { await firstValueFrom(this.http.post(this.base + '/logout', {}, { headers: { 'X-Sanctuary-Admin': '1', Authorization: `Bearer ${token}` } })); }
      catch { this.message.set(message + ' Remote sign-out was unavailable; the server session expires automatically.'); }
    }
  }
  private clear() {
    this.mode = ''; this.newPassword = ''; this.confirmPassword = '';
    this.generation++; this.token = ''; this.signedIn.set(false); this.password = ''; this.code = ''; this.email = '';
    this.challenge = ''; this.secret = ''; this.rows = []; this.newDays = []; this.dayFields = []; this.links = {}; this.options = {}; this.tags = ''; this.values = null; this.fields = []; this.user = null;
    this.summary = {}; this.auditSnapshot = null; this.reason = ''; this.confirmPublish = false;
    this.query = ''; this.registrationHistory = []; this.registrationCursor = ''; this.registrationNext = '';
  }
  restart() { this.mode = ''; this.newPassword = ''; this.confirmPassword = ''; this.challenge = ''; this.secret = ''; this.code = ''; }
  async navigate(section: string) {
    if (!this.discard()) return;
    await this.run(async () => {
      this.section = section; this.page = 0; this.query = ''; this.values = null; this.user = null; this.auditSnapshot = null;
      this.registrationCursor = ''; this.registrationHistory = []; await this.load();
    });
  }
  async search() { await this.run(async () => { this.page = 0; await this.load(); }); }
  async chooseCollection() { if (!this.discard()) return; this.values = null; await this.search(); }
  private discard() { return !this.values || window.confirm('Leave this editor? Unsaved changes will be lost.'); }
  private async load() {
    const generation = this.generation;
    const page = this.page;
    if (this.section === 'content') {
      const [result, fields, options, dayFields] = await Promise.all([this.api('GET', `/content/${this.entity}?q=${encodeURIComponent(this.query)}&page=${page}`), this.api<Field[]>('GET', `/schema/${this.entity}`), this.api('GET', `/relations/${this.entity}`), this.entity === 'novenas' ? this.api<Field[]>('GET', '/schema/novena-days') : Promise.resolve([])]);
      if (generation !== this.generation) return;
      this.rows = result.items; this.hasMore = result.hasMore; this.fields = fields; this.options = options; this.dayFields = dayFields.filter(f => !['id', 'novena_id', 'day_number'].includes(f.name));
    } else if (this.section === 'users') {
      const [result, summary] = await Promise.all([this.api('GET', `/users?q=${encodeURIComponent(this.query)}&page=${page}&active=${this.activeOnly}`), this.api('GET', '/users/summary')]);
      if (generation !== this.generation) return;
      this.rows = result.items; this.hasMore = result.hasMore; this.summary = summary;
    } else if (this.section === 'registrations') {
      const result = await this.api('GET', `/registrations?cursor=${encodeURIComponent(this.registrationCursor)}`);
      if (generation !== this.generation) return;
      this.rows = result.items; this.registrationNext = result.cursor;
    } else {
      const result = await this.api('GET', `/audit?page=${page}`);
      if (generation !== this.generation) return;
      this.rows = result.items; this.hasMore = result.hasMore;
    }
  }
  async turnPage(delta: number) { await this.run(async () => { this.page += delta; await this.load(); }); }
  async registrationsPage(next: boolean) {
    await this.run(async () => {
      if (next) { this.registrationHistory.push(this.registrationCursor); this.registrationCursor = this.registrationNext; }
      else this.registrationCursor = this.registrationHistory.pop() || '';
      await this.load();
    });
  }
  async edit(row: Row) {
    if (!this.discard()) return;
    await this.run(async () => {
      const result = await this.api('GET', `/content/${this.entity}/${encodeURIComponent(row['id'])}`);
      this.links = result.links; this.tags = (result.links.tags || []).join(', ');
      this.values = result.values; this.version = result.version; this.editingId = row['id']; this.creating = false; this.reason = ''; this.confirmPublish = false;
    });
  }
  create() {
    if (!this.discard()) return;
    this.newDays = this.entity === 'novenas' ? Array.from({ length: 9 }, (_, i) => Object.fromEntries(this.dayFields.map(f => [f.name, f.name === 'title_en' ? `Day ${i + 1}` : f.name === 'title_es' ? `Día ${i + 1}` : f.name === 'title_pl' ? `Dzień ${i + 1}` : '']))) : [];
    this.links = Object.fromEntries(Object.keys(this.options).map(key => [key, []])); this.tags = '';
    this.values = {}; this.creating = true; this.editingId = ''; this.reason = ''; this.confirmPublish = false;
    for (const field of this.fields) if (!field.generated) this.values[field.name] = field.type === 'text' ? '' : null;
    if (this.entity === 'novenas') this.values['duration_days'] = 9;
  }
  async publish() {
    if (!this.values) return;
    await this.run(async () => {
      const path = `/content/${this.entity}` + (this.creating ? '' : '/' + encodeURIComponent(this.editingId));
      const result = await this.api(this.creating ? 'POST' : 'PUT', path, { values: this.values, ...(this.creating && this.entity === 'novenas' ? { days: this.newDays } : {}), links: { ...this.links, ...('tags' in this.links ? { tags: this.tags.split(',').map(t => t.trim()).filter(Boolean) } : {}) }, version: this.version, reason: this.reason });
      this.values = result.values; this.version = result.version; this.editingId = String(result.values.id); this.creating = false;
      this.confirmPublish = false; this.reason = ''; this.message.set('Published. Updated content is available on the next app refresh.');
    });
  }
  async openUser(id: string, page = 0) {
    await this.run(async () => { this.user = await this.api('GET', `/users/${encodeURIComponent(id)}?page=${page}`); this.userId = id; this.userPage = page; });
  }
  async openAudit(id: number) { await this.run(async () => { this.auditSnapshot = await this.api('GET', `/audit/${id}`); }); }
  toggleLink(key: string, id: string, checked: boolean) { this.links[key] = checked ? [...(this.links[key] || []), id] : (this.links[key] || []).filter(value => value !== id); }
  stats() { return [['Profiles', this.summary['profiles']], ['Active · 24h', this.summary['active_24h']], ['Active · 7d', this.summary['active_7d']], ['Active · 30d', this.summary['active_30d']], ['Active novenas', this.summary['active_novenas']], ['Completed novenas', this.summary['completed_novenas']]]; }
  essential(name: string) { return /^(id|slug|name_en|title_en|label_en|novena_id|day_number|feast_month|feast_day|duration_days|category)$/.test(name); }
  label(name: string) { return name.replace(/_/g, ' ').replace(/\b(en|es|pl)$/, code => ({ en: '(English)', es: '(Spanish)', pl: '(Polish)' })[code] || code); }
  readonlyField(field: Field) { return !this.creating && ['id', 'slug', 'novena_id', 'day_number'].includes(field.name); }
  longField(name: string) { return /body|prayer_|reflection|scripture|biography|summary|description|note_/.test(name); }
}
