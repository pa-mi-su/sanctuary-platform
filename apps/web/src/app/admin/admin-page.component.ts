import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { AdminMessagesComponent } from './admin-messages.component';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';
import { AdminInputComponent } from './admin-input.component';
import { ADMIN_QR_ENCODER, authenticatorUri } from './admin-qr';
import { ADMIN_IMAGE_PREPARER, AdminFeedbackError } from './admin-images';

type Row = Record<string, any>;
type Field = { name: string; type: string; required: boolean; generated: boolean };
type Dialog = 'discard' | 'publish' | 'terms' | null;
export const ADMIN_IDLE_MS = 8 * 60 * 60_000;

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [FormsModule, DatePipe, AdminInputComponent, AdminMessagesComponent],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.scss',
})
export class AdminPageComponent implements OnInit, OnDestroy {
  // Admin session credentials are HttpOnly, scoped to /admin-api, never in browser storage/URLs.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly origin = inject(SANCTUARY_API_BASE_URL);
  private readonly base = this.origin + '/admin-api';
  private readonly encodeQr = inject(ADMIN_QR_ENCODER);
  private readonly prepareImage = inject(ADMIN_IMAGE_PREPARER);
  readonly signedIn = signal(false);
  readonly checkingSession = signal(true);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly error = signal(false);
  readonly uploading = signal(false);
  readonly qrCode = signal('');
  readonly qrError = signal(false);
  mode = '';
  newPassword = '';
  confirmPassword = '';
  email = '';
  password = '';
  code = '';
  challenge = '';
  secret = '';
  section = 'content';
  entity = 'prayers';
  query = '';
  page = 0;
  hasMore = false;
  rows: Row[] = [];
  fields: Field[] = [];
  values: Row | null = null;
  version = '';
  editingId = '';
  creating = false;
  reason = '';
  confirmPublish = false;
  language = 'en';
  dayFields: Field[] = [];
  newDays: Row[] = [];
  novenaDays: Row[] = [];
  parentNovena: Row | null = null;
  links: Record<string, string[]> = {};
  terms: Record<string, Record<string, Row>> = {};
  tags = '';
  summary: Row = {};
  user: Row | null = null;
  userPage = 0;
  userId = '';
  statusFilter = 'all';
  activityFilter = 'all';
  syncedAt = '';
  auditSnapshot: Row | null = null;
  dialog: Dialog = null;
  dialogError = '';
  private discardAnswer?: (leave: boolean) => void;
  private lastFocused?: HTMLElement;
  private termSearchTimer?: ReturnType<typeof setTimeout>;
  pickerKind = 'intentions';
  pickerQuery = '';
  pickerRows: Row[] = [];
  pickerPage = 0;
  pickerMore = false;
  pickerSelection: string[] = [];
  termStage = 'choose';
  termDraft: Row = {};
  termVersion = '';
  termEditingId = '';
  termReason = '';
  private original = '';
  private generation = 0;
  private qrGeneration = 0;
  private lastActivity = Date.now();
  private lastHeartbeat = Date.now();
  private expiresAt = 0;
  private readonly failedImages = new Set<string>();
  private readonly timeout = setInterval(() => this.checkTimeout(), 60_000);
  readonly languages = [
    ['en', 'English'],
    ['es', 'Español'],
    ['pl', 'Polski'],
  ];
  readonly collections = [
    ['prayers', 'Prayers & rosary'],
    ['saints', 'Saints'],
    ['novenas', 'Novenas'],
    ['intentions', 'Intentions'],
    ['patronages', 'Patronages'],
  ];
  @ViewChild(AdminMessagesComponent) messagesComposer?: AdminMessagesComponent;
  @ViewChild('editorHeading') editorHeading?: ElementRef<HTMLElement>;
  @ViewChild('dialogPanel') set dialogElement(value: ElementRef<HTMLElement> | undefined) {
    if (value)
      setTimeout(() => {
        if (this.dialog && value.nativeElement.isConnected) value.nativeElement.focus();
      });
  }
  @HostListener('document:wheel', ['$event'])
  @HostListener('document:pointerdown', ['$event'])
  @HostListener('document:keydown', ['$event'])
  activity(event?: Event) {
    if (event && !event.isTrusted) return;
    if (this.signedIn() && this.expired()) {
      void this.logout('Your session expired. Please sign in again.');
      return;
    }
    this.lastActivity = Date.now();
  }
  @HostListener('window:beforeunload', ['$event']) beforeUnload(event: BeforeUnloadEvent) {
    if (this.isDirty()) {
      event.preventDefault();
      event.returnValue = '';
    }
  }
  @HostListener('document:keydown', ['$event']) dialogKeys(event: KeyboardEvent) {
    if (!this.dialog) return;
    if (event.key === 'Escape' && !this.busy()) {
      event.preventDefault();
      this.closeDialog();
    }
    if (event.key !== 'Tab') return;
    const panel = document.querySelector<HTMLElement>('.admin-dialog');
    const elements = panel
      ? Array.from(
          panel.querySelectorAll<HTMLElement>(
            'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex="0"]',
          ),
        ).filter((el) => !el.hidden && el.getAttribute('aria-hidden') !== 'true')
      : [];
    if (!elements.length) {
      event.preventDefault();
      panel?.focus();
      return;
    }
    const first = elements[0],
      last = elements[elements.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === panel)) {
      event.preventDefault();
      last.focus();
    } else if (
      !event.shiftKey &&
      (document.activeElement === last || document.activeElement === panel)
    ) {
      event.preventDefault();
      first.focus();
    }
  }
  ngOnInit() {
    void this.restoreSession();
  }
  ngOnDestroy() {
    clearInterval(this.timeout);
    clearTimeout(this.termSearchTimer);
    this.clear();
  }
  private expired() {
    return (
      Date.now() - this.lastActivity >= ADMIN_IDLE_MS ||
      (!!this.expiresAt && Date.now() >= this.expiresAt)
    );
  }
  private checkTimeout() {
    if (!this.signedIn()) return;
    if (this.expired()) {
      void this.logout('Your session expired. Please sign in again.');
      return;
    }
    // Only actual interaction authorizes a heartbeat; idle polling never extends a session.
    if (
      !this.busy() &&
      this.lastActivity > this.lastHeartbeat &&
      Date.now() - this.lastHeartbeat >= 5 * 60_000
    ) {
      this.lastHeartbeat = Date.now();
      void this.api('GET', '/session').catch((error) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clear();
          this.message.set('Your session expired. Please sign in again.');
        }
      });
    }
  }
  private async api<T = any>(method: string, path: string, body?: unknown): Promise<T> {
    const generation = this.generation;
    const result = await firstValueFrom(
      this.http.request<T>(method, this.base + path, {
        body,
        withCredentials: true,
        headers: { 'X-Sanctuary-Admin': '1', 'X-Sanctuary-Admin-Client': 'cookie-v2' },
      }),
    );
    if (generation !== this.generation) {
      if (path === '/verify') {
        try {
          await firstValueFrom(
            this.http.post(
              this.base + '/logout',
              {},
              {
                withCredentials: true,
                headers: { 'X-Sanctuary-Admin': '1', 'X-Sanctuary-Admin-Client': 'cookie-v2' },
              },
            ),
          );
        } catch {
          /* Server-side lifetime remains authoritative if the network is unavailable. */
        }
      }
      throw new Error('Session ended');
    }
    return result;
  }
  private async run(action: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true);
    this.message.set('');
    this.error.set(false);
    this.dialogError = '';
    try {
      await action();
    } catch (error) {
      const text =
        error instanceof HttpErrorResponse
          ? error.error?.message || 'Could not complete this request. Please try again.'
          : error instanceof AdminFeedbackError
            ? error.message
            : 'Could not complete this action. Please try again.';
      if (error instanceof HttpErrorResponse && error.status === 401 && this.signedIn())
        this.clear();
      if (this.dialog) this.dialogError = text;
      else {
        this.error.set(true);
        this.message.set(text);
      }
    } finally {
      this.busy.set(false);
    }
  }
  private async restoreSession() {
    try {
      const info = await this.api('GET', '/session');
      this.acceptSession(info);
      await this.run(() => this.load());
    } catch (error) {
      if (!(error instanceof HttpErrorResponse && error.status === 401)) {
        this.message.set('Could not check your session. You can sign in below.');
      }
    } finally {
      this.checkingSession.set(false);
    }
  }
  private acceptSession(info: Row) {
    this.expiresAt = Date.parse(info['expiresAt']);
    this.signedIn.set(true);
    this.lastActivity = Date.now();
    this.lastHeartbeat = Date.now();
  }
  private async applyChallenge(result: Row) {
    this.challenge = result['challenge'];
    this.secret = result['secret'] || '';
    this.mode = result['mode'];
    this.qrCode.set('');
    this.qrError.set(false);
    const qrGeneration = ++this.qrGeneration;
    if (this.secret) {
      try {
        const image = await this.encodeQr(authenticatorUri(this.secret, this.email));
        if (qrGeneration === this.qrGeneration) this.qrCode.set(image);
      } catch {
        if (qrGeneration === this.qrGeneration) this.qrError.set(true);
      }
    }
  }
  async login() {
    await this.run(async () => {
      const result = await this.api('POST', '/login', {
        email: this.email,
        password: this.password,
      });
      this.password = '';
      await this.applyChallenge(result);
    });
    this.password = '';
  }
  passwordChecks() {
    return [
      { label: 'At least 14 characters', met: this.newPassword.length >= 14 },
      { label: 'An uppercase letter', met: /[A-Z]/.test(this.newPassword) },
      { label: 'A lowercase letter', met: /[a-z]/.test(this.newPassword) },
      { label: 'A number', met: /[0-9]/.test(this.newPassword) },
      { label: 'A symbol', met: /[^A-Za-z0-9\s]/.test(this.newPassword) },
    ];
  }
  passwordReady() {
    return (
      this.passwordChecks().every((check) => check.met) && this.newPassword === this.confirmPassword
    );
  }
  async setPassword() {
    if (!this.passwordReady()) {
      this.error.set(true);
      this.message.set('Meet all password requirements and make sure the passwords match.');
      return;
    }
    await this.run(async () => {
      const result = await this.api('POST', '/password', {
        challenge: this.challenge,
        password: this.newPassword,
      });
      await this.applyChallenge(result);
    });
    this.newPassword = '';
    this.confirmPassword = '';
  }
  async verify() {
    await this.run(async () => {
      const info = await this.api('POST', '/verify', {
        challenge: this.challenge,
        code: this.code,
      });
      this.acceptSession(info);
      this.restart();
      this.email = '';
      await this.load();
    });
    this.code = '';
  }
  async logout(message = 'You have signed out.') {
    this.clear();
    this.message.set(message);
    try {
      await firstValueFrom(
        this.http.post(
          this.base + '/logout',
          {},
          {
            withCredentials: true,
            headers: { 'X-Sanctuary-Admin': '1', 'X-Sanctuary-Admin-Client': 'cookie-v2' },
          },
        ),
      );
    } catch (error) {
      if (!(error instanceof HttpErrorResponse && error.status === 401))
        this.message.set(
          message + ' Server sign-out was unavailable; the session expires automatically.',
        );
    }
  }
  private clear() {
    this.generation++;
    this.restart();
    this.signedIn.set(false);
    this.email = '';
    this.password = '';
    this.rows = [];
    this.fields = [];
    this.values = null;
    this.newDays = [];
    this.novenaDays = [];
    this.parentNovena = null;
    this.links = {};
    this.terms = {};
    this.tags = '';
    this.summary = {};
    this.user = null;
    this.auditSnapshot = null;
    this.original = '';
    this.query = '';
    this.reason = '';
    this.confirmPublish = false;
    this.expiresAt = 0;
    this.resolveDiscard(false);
    this.dialog = null;
    this.dialogError = '';
    this.pickerRows = [];
    this.pickerSelection = [];
    this.pickerQuery = '';
    this.termDraft = {};
    this.termReason = '';
    this.termVersion = '';
    this.termEditingId = '';
    this.failedImages.clear();
    clearTimeout(this.termSearchTimer);
  }
  restart() {
    this.qrGeneration++;
    this.qrCode.set('');
    this.qrError.set(false);
    this.mode = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.challenge = '';
    this.secret = '';
    this.code = '';
  }
  private canonical(value: any): any {
    if (value == null) return '';
    if (Array.isArray(value)) return value.map((v) => this.canonical(v));
    if (typeof value === 'object')
      return Object.fromEntries(
        Object.keys(value)
          .sort()
          .map((key) => [key, this.canonical(value[key])]),
      );
    return value;
  }
  private snapshot() {
    const links = Object.fromEntries(
      Object.entries(this.links).map(([key, ids]) => [key, [...new Set(ids)].sort()]),
    );
    return JSON.stringify(
      this.canonical({
        values: this.values,
        links,
        tags: [
          ...new Set(
            this.tags
              .split(',')
              .map((tag) => tag.trim())
              .filter(Boolean),
          ),
        ].sort(),
        days: this.newDays,
      }),
    );
  }
  isDirty() {
    if (this.section === 'messages') return this.messagesComposer?.hasDraft() ?? false;
    return !!this.values && this.original !== this.snapshot();
  }
  private capture() {
    this.original = this.snapshot();
  }
  private openDialog(dialog: Dialog) {
    this.lastFocused = document.activeElement as HTMLElement;
    this.dialog = dialog;
    this.dialogError = '';
  }
  closeDialog() {
    clearTimeout(this.termSearchTimer);
    this.resolveDiscard(false);
    this.dialog = null;
    this.dialogError = '';
    this.termDraft = {};
    this.termReason = '';
    this.pickerRows = [];
    const focus = this.lastFocused;
    setTimeout(() => {
      if (focus?.isConnected) focus.focus();
    });
  }
  private resolveDiscard(answer: boolean) {
    const resolve = this.discardAnswer;
    this.discardAnswer = undefined;
    resolve?.(answer);
  }
  decideDiscard(leave: boolean) {
    this.resolveDiscard(leave);
    this.closeDialog();
  }
  private async discard(): Promise<boolean> {
    if (!this.isDirty()) return true;
    this.openDialog('discard');
    return new Promise((resolve) => {
      this.discardAnswer = resolve;
    });
  }
  async navigate(section: string) {
    if (!(await this.discard())) return;
    await this.run(async () => {
      if (this.entity === 'novena-days') this.entity = 'novenas';
      this.parentNovena = null;
      this.novenaDays = [];
      this.section = section;
      this.rows = [];
      this.page = 0;
      this.query = '';
      this.values = null;
      this.user = null;
      this.auditSnapshot = null;
      await this.load();
    });
  }
  async search() {
    await this.run(async () => {
      this.rows = [];
      this.page = 0;
      await this.load();
    });
  }
  async chooseCollection(entity: string) {
    if (!(await this.discard())) return;
    this.entity = entity;
    this.values = null;
    this.fields = [];
    this.rows = [];
    this.query = '';
    await this.search();
  }
  private async load(refresh = false) {
    const generation = this.generation;
    if (this.section === 'messages') return;
    if (this.section === 'content') {
      const [result, fields, dayFields] = await Promise.all([
        this.api(
          'GET',
          `/content/${this.entity}?q=${encodeURIComponent(this.query)}&page=${this.page}`,
        ),
        this.api<Field[]>('GET', `/schema/${this.entity}`),
        this.entity === 'novenas'
          ? this.api<Field[]>('GET', '/schema/novena-days')
          : Promise.resolve([]),
      ]);
      if (generation !== this.generation) return;
      this.rows = result.items;
      this.hasMore = result.hasMore;
      this.fields = fields;
      this.dayFields = dayFields.filter((f) => !['id', 'novena_id', 'day_number'].includes(f.name));
    } else if (this.section === 'users') {
      const result = await this.api(
        'GET',
        `/directory?q=${encodeURIComponent(this.query)}&page=${this.page}&status=${this.statusFilter}&activity=${this.activityFilter}&refresh=${refresh}`,
      );
      if (generation !== this.generation) return;
      this.rows = result.items;
      this.hasMore = result.hasMore;
      this.summary = result.summary;
      this.syncedAt = result.syncedAt;
    } else {
      const result = await this.api('GET', `/audit?page=${this.page}`);
      if (generation !== this.generation) return;
      this.rows = result.items;
      this.hasMore = result.hasMore;
    }
  }
  async refreshUsers() {
    await this.run(async () => {
      this.page = 0;
      await this.load(true);
    });
  }
  async turnPage(delta: number) {
    await this.run(async () => {
      const before = this.page;
      this.page += delta;
      try {
        await this.load();
      } catch (error) {
        this.page = before;
        throw error;
      }
    });
  }
  private cacheTerms(kind: string, rows: Row[]) {
    this.terms[kind] ||= {};
    for (const row of rows) this.terms[kind][String(row['id'])] = row;
  }
  private acceptContent(result: Row) {
    this.links = result['links'] || {};
    this.tags = (this.links['tags'] || []).join(', ');
    for (const [kind, rows] of Object.entries(result['related'] || {}))
      this.cacheTerms(kind, rows as Row[]);
    this.values = result['values'];
    this.version = result['version'];
    this.editingId = String(this.values!['id']);
    this.creating = false;
    this.reason = '';
    this.confirmPublish = false;
    this.newDays = [];
    this.language = 'en';
    this.capture();
    setTimeout(() => {
      const heading = this.editorHeading?.nativeElement;
      if (heading?.isConnected) {
        heading.focus({ preventScroll: true });
        heading.scrollIntoView({ block: 'start' });
      }
    });
  }
  async edit(row: Row) {
    if (!(await this.discard())) return;
    await this.run(async () => {
      const id = encodeURIComponent(row['id']);
      const result = await this.api('GET', `/content/${this.entity}/${id}`);
      const days =
        this.entity === 'novenas' ? await this.api('GET', `/novenas/${id}/days`) : { items: [] };
      this.acceptContent(result);
      this.novenaDays = days.items;
    });
  }
  async create() {
    if (!this.fields.length) return;
    if (!(await this.discard())) return;
    this.newDays =
      this.entity === 'novenas'
        ? Array.from({ length: 9 }, (_, i) =>
            Object.fromEntries(
              this.dayFields.map((f) => [
                f.name,
                f.name === 'title_en'
                  ? `Day ${i + 1}`
                  : f.name === 'title_es'
                    ? `Día ${i + 1}`
                    : f.name === 'title_pl'
                      ? `Dzień ${i + 1}`
                      : '',
              ]),
            ),
          )
        : [];
    this.links = {};
    if (['prayers', 'saints', 'novenas'].includes(this.entity)) this.links['tags'] = [];
    for (const kind of this.relationKinds()) this.links[kind] = [];
    this.tags = '';
    this.values = {};
    this.creating = true;
    this.editingId = '';
    this.reason = '';
    this.confirmPublish = false;
    this.language = 'en';
    for (const field of this.fields)
      if (!field.generated) this.values[field.name] = field.type === 'text' ? '' : null;
    if (this.entity === 'novenas') this.values['duration_days'] = 9;
    this.capture();
  }
  relationKinds() {
    return ['saints', 'novenas'].includes(this.entity) ? ['intentions', 'patronages'] : [];
  }

  selectedTerms(kind: string) {
    return (this.links[kind] || []).map(
      (id) => this.terms[kind]?.[id] || { id, title: 'Term ' + id },
    );
  }
  unlink(kind: string, id: string) {
    this.links[kind] = this.links[kind].filter((value) => value !== id);
  }
  async openPicker(kind: string) {
    this.pickerKind = kind;
    this.pickerQuery = '';
    this.pickerPage = 0;
    this.pickerRows = [];
    this.pickerSelection = [...(this.links[kind] || [])];
    this.termStage = 'choose';
    this.openDialog('terms');
    await this.searchTerms();
  }
  scheduleTermSearch() {
    clearTimeout(this.termSearchTimer);
    this.termSearchTimer = setTimeout(() => {
      if (this.dialog !== 'terms' || this.termStage !== 'choose') return;
      if (this.busy()) {
        this.scheduleTermSearch();
        return;
      }
      void this.searchTerms();
    }, 300);
  }
  async searchTerms(page = 0) {
    clearTimeout(this.termSearchTimer);
    const query = this.pickerQuery;
    await this.run(async () => {
      const result = await this.api(
        'GET',
        `/content/${this.pickerKind}?q=${encodeURIComponent(this.pickerQuery)}&page=${page}`,
      );
      if (query !== this.pickerQuery) return;
      this.pickerRows = page === 0 ? result.items : [...this.pickerRows, ...result.items];
      this.pickerPage = page;
      this.pickerMore = result.hasMore;
      this.cacheTerms(this.pickerKind, result.items);
    });
  }
  togglePicker(id: string, checked: boolean) {
    this.pickerSelection = checked
      ? [...new Set([...this.pickerSelection, id])]
      : this.pickerSelection.filter((value) => value !== id);
  }
  applyTerms() {
    this.links[this.pickerKind] = [...this.pickerSelection];
    this.closeDialog();
  }
  createTerm() {
    this.termDraft = { label_en: this.pickerQuery, label_es: '', label_pl: '' };
    this.termEditingId = '';
    this.termReason = '';
    this.termStage = 'edit';
    this.dialogError = '';
  }
  async editSelectedTerm(kind: string, row: Row) {
    this.pickerKind = kind;
    this.pickerSelection = [...(this.links[kind] || [])];
    this.pickerRows = [];
    this.pickerQuery = '';
    this.pickerMore = false;
    this.termStage = 'choose';
    this.openDialog('terms');
    await this.editTerm(row);
  }
  async editTerm(row: Row) {
    await this.run(async () => {
      const result = await this.api('GET', `/content/${this.pickerKind}/${row['id']}`);
      this.termDraft = result.values;
      this.termVersion = result.version;
      this.termEditingId = String(row['id']);
      this.termReason = '';
      this.termStage = 'edit';
    });
  }
  async saveTerm() {
    await this.run(async () => {
      const result = this.termEditingId
        ? await this.api('PUT', `/content/${this.pickerKind}/${this.termEditingId}`, {
            values: this.termDraft,
            version: this.termVersion,
            reason: this.termReason,
            links: {},
          })
        : await this.api('POST', `/terms/${this.pickerKind}`, {
            labels: this.termDraft,
            reason: this.termReason,
          });
      const row = { id: String(result.values.id), title: result.values.label_en };
      this.cacheTerms(this.pickerKind, [row]);
      if (!this.termEditingId)
        this.pickerSelection = [...new Set([...this.pickerSelection, row.id])];
      this.pickerRows = [row, ...this.pickerRows.filter((existing) => existing['id'] !== row.id)];
      this.termStage = 'choose';
      this.termDraft = {};
      this.termReason = '';
      this.message.set('Shared term saved. Apply the selection to link it to this item.');
    });
  }
  private ensureIdentifiers() {
    if (
      !this.creating ||
      !this.values ||
      !['saints', 'prayers', 'novenas', 'intentions', 'patronages'].includes(this.entity)
    )
      return;
    const name = String(
      this.values['name_en'] || this.values['title_en'] || this.values['label_en'] || '',
    );
    const slug =
      name
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '') || 'content';
    this.values['slug'] ||= slug + '-' + crypto.randomUUID().slice(0, 8);
    if (this.fields.some((f) => f.name === 'id' && !f.generated))
      this.values['id'] ||= this.values['slug'];
    if (this.entity === 'saints') this.values['name'] ||= name;
  }
  reviewPublish() {
    this.reason = '';
    this.confirmPublish = false;
    this.openDialog('publish');
  }
  async publish() {
    if (!this.values || !this.confirmPublish || this.reason.trim().length < 3) return;
    await this.run(async () => {
      this.ensureIdentifiers();
      const path =
        `/content/${this.entity}` + (this.creating ? '' : '/' + encodeURIComponent(this.editingId));
      const result = await this.api(this.creating ? 'POST' : 'PUT', path, {
        values: this.values,
        ...(this.creating && this.entity === 'novenas' ? { days: this.newDays } : {}),
        links: {
          ...this.links,
          ...('tags' in this.links
            ? {
                tags: this.tags
                  .split(',')
                  .map((t) => t.trim())
                  .filter(Boolean),
              }
            : {}),
        },
        version: this.version,
        reason: this.reason,
      });
      this.acceptContent(result);
      this.closeDialog();
      this.message.set('Published. The updated content will appear when the app refreshes.');
    });
  }
  async uploadImage(event: Event) {
    const input = event.target as HTMLInputElement,
      file = input.files?.[0];
    if (!file || !this.values) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      this.error.set(true);
      this.message.set('Choose a JPEG or PNG image up to 5 MB.');
      input.value = '';
      return;
    }
    this.uploading.set(true);
    const generation = this.generation;
    const values = this.values;
    await this.run(async () => {
      const prepared = await this.prepareImage(file);
      if (generation !== this.generation || values !== this.values) return;
      const form = new FormData();
      form.append('image', prepared);
      const result = await this.api('POST', '/images', form);
      this.values!['image_url'] = this.origin + result.path;
      this.message.set('Image uploaded. Publish this item to replace its current image.');
    });
    this.uploading.set(false);
    input.value = '';
  }
  imageFailed(url: string) {
    this.failedImages.add(url);
  }
  imageVisible(url: string) {
    return !!url && !this.failedImages.has(url);
  }
  hasImage() {
    return ['saints', 'prayers', 'novenas'].includes(this.entity);
  }
  dayTitle(row: Row) {
    const day = 'Day ' + row['day_number'];
    const title = row['title']?.trim();
    return title && title.toLowerCase() !== day.toLowerCase() ? day + ' · ' + title : day;
  }
  async editNovenaDay(row: Row) {
    if (!(await this.discard())) return;
    await this.run(async () => {
      const parent = { id: this.editingId, title: this.editorTitle() };
      const [result, fields] = await Promise.all([
        this.api('GET', `/content/novena-days/${encodeURIComponent(row['id'])}`),
        this.api<Field[]>('GET', '/schema/novena-days'),
      ]);
      this.parentNovena = parent;
      this.entity = 'novena-days';
      this.fields = fields;
      this.acceptContent(result);
    });
  }
  async backToNovena() {
    if (!this.parentNovena || !(await this.discard())) return;
    const id = encodeURIComponent(this.parentNovena['id']);
    await this.run(async () => {
      const [result, fields, days] = await Promise.all([
        this.api('GET', `/content/novenas/${id}`),
        this.api<Field[]>('GET', '/schema/novenas'),
        this.api('GET', `/novenas/${id}/days`),
      ]);
      this.entity = 'novenas';
      this.fields = fields;
      this.acceptContent(result);
      this.novenaDays = days.items;
      this.parentNovena = null;
    });
  }
  async openUser(id: string, page = 0) {
    await this.run(async () => {
      this.user = await this.api('GET', `/users/${encodeURIComponent(id)}?page=${page}`);
      this.userId = id;
      this.userPage = page;
    });
  }
  async openAudit(id: number) {
    await this.run(async () => {
      this.auditSnapshot = await this.api('GET', `/audit/${id}`);
    });
  }
  stats() {
    return [
      ['Registered accounts', this.summary['registrations']],
      ['Email verified', this.summary['verified']],
      ['Active in 7 days', this.summary['active_7d']],
      ['Active in 30 days', this.summary['active_30d']],
      ['Praying a novena', this.summary['active_novenas']],
      ['Completed novenas', this.summary['completed_novenas']],
    ];
  }
  rowMeta(row: Row) {
    if (['intentions', 'patronages'].includes(this.entity))
      return `${row['saint_count'] ?? 0} saints · ${row['novena_count'] ?? 0} novenas`;
    if (this.entity === 'saints' && row['feast_month'] && row['feast_day'])
      return (
        'Feast · ' +
        new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(
          new Date(Date.UTC(2000, row['feast_month'] - 1, row['feast_day'])),
        )
      );
    if (this.entity === 'novenas') return `${row['duration_days'] || 9}-day novena`;
    if (this.entity === 'prayers') return this.statusLabel(row['category'] || 'Prayer');
    return 'Day ' + (row['day_number'] || '—');
  }
  collectionTitle() {
    if (this.entity === 'novena-days') return 'Novena day';
    return this.collections.find((collection) => collection[0] === this.entity)?.[1] || 'Content';
  }
  editorTitle() {
    if (this.entity === 'novena-days' && this.values)
      return this.dayTitle({ ...this.values, title: this.values['title_en'] });
    return (
      this.values?.['name_en'] ||
      this.values?.['title_en'] ||
      this.values?.['label_en'] ||
      'Untitled'
    );
  }
  generalFields() {
    return this.fields.filter(
      (field) =>
        !/_(en|es|pl)$/.test(field.name) &&
        !['id', 'slug', 'image_url'].includes(field.name) &&
        !(this.entity === 'novena-days' && field.name === 'novena_id'),
    );
  }
  localizedFields() {
    return this.fields.filter((field) => field.name.endsWith('_' + this.language));
  }
  dayLanguageFields() {
    return this.dayFields.filter((field) => field.name.endsWith('_' + this.language));
  }
  essential(name: string) {
    return (
      /^(name_en|title_en|label_en|novena_id|day_number|feast_month|feast_day|duration_days|category)$/.test(
        name,
      ) && !(this.entity === 'novena-days' && name === 'title_en')
    );
  }
  label(name: string) {
    if (name === 'name') return 'Canonical name';
    const result = name.replace(/_(en|es|pl)$/, '').replace(/_/g, ' ');
    return result.charAt(0).toUpperCase() + result.slice(1);
  }
  statusLabel(value: string) {
    return (value || 'Unknown')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/^./, (char) => char.toUpperCase());
  }
  readonlyField(field: Field) {
    return (
      (this.entity === 'novenas' && field.name === 'duration_days') ||
      (!this.creating && ['id', 'slug', 'novena_id', 'day_number'].includes(field.name))
    );
  }
  longField(name: string) {
    return /body|prayer_|reflection|scripture|biography|summary|description|note_/.test(name);
  }
}
