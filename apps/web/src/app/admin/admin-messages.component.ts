import {
  Component,
  EventEmitter,
  Output,
  inject,
  signal,
  OnInit,
  ViewChild,
  OnDestroy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';
import { AdminInputComponent } from './admin-input.component';
@Component({
  selector: 'admin-messages',
  standalone: true,
  imports: [FormsModule, DatePipe, AdminInputComponent],
  styleUrl: './admin-messages.component.scss',
  template: ` <header>
      <p class="eyebrow">A LITTLE INVITATION TO PRAYER</p>
      <h1>Messages</h1>
      <p>Send an English notification to people who opted in to Sanctuary announcements.</p>
    </header>
    @if (completed()) {
      <section class="notice completion" role="status" aria-labelledby="send-complete">
        <h2 id="send-complete">Announcement submitted</h2>
        <p>
          Your message was accepted for sending.
          {{
            hasDraft()
              ? 'Your new draft is still here.'
              : 'The form is cleared and ready for a new message.'
          }}
        </p>
        <button type="button" class="primary" (click)="createAnother()">
          {{ hasDraft() ? 'Continue writing' : 'Create another message' }}
        </button>
        <button
          type="button"
          (click)="completed.set(false); notice.set('')"
          aria-label="Dismiss submission confirmation"
        >
          Dismiss
        </button>
      </section>
    } @else if (notice()) {
      <p role="status" class="notice">{{ notice() }}</p>
    }
    <section class="panel">
      <h2>Create a message</h2>
      @if (!enabled) {
        <p>Sending is disabled in this environment. You can still prepare and preview a message.</p>
      }
      <form (ngSubmit)="review()">
        <admin-input
          #composerTitle
          label="Title"
          inputId="push-title"
          name="title"
          [(ngModel)]="title"
          (ngModelChange)="invalidate()"
          [maxlength]="100"
          [required]="true"
        />
        <label
          >Message<textarea
            name="body"
            [(ngModel)]="body"
            (ngModelChange)="invalidate()"
            maxlength="500"
            rows="4"
            required
          ></textarea>
        </label>
        <small>{{ body.length }} / 500 · English</small>
        <div class="columns">
          <label
            >When tapped, open<select name="kind" [(ngModel)]="kind" (ngModelChange)="changeKind()">
              <option value="home">Home / app announcement</option>
              <option value="prayers">Prayer</option>
              <option value="saints">Saint / feast day</option>
              <option value="novenas">Novena invitation</option>
            </select></label
          >
          <label
            >Audience<select name="audience" [(ngModel)]="audience" (ngModelChange)="invalidate()">
              <option value="test">Send test to my device</option>
              <option value="all">All opted-in devices</option>
              <option value="members">Signed-in users</option>
              <option value="guests">Signed-out devices (including unregistered people)</option>
              <option value="ios">iOS devices</option>
              <option value="android">Android devices</option>
            </select></label
          >
        </div>
        <small
          >Signed-in status reflects the device's last successful sync, not online presence.
          Signed-out includes people who never created an account.</small
        >
        @if (kind !== 'home') {
          <div class="columns">
            <admin-input
              label="Find existing content"
              inputId="push-search"
              name="search"
              [(ngModel)]="query"
              [maxlength]="200"
            />
            <button type="button" (click)="searchContent()" [disabled]="busy()">
              Search content
            </button>
          </div>
          <label
            >Content<select
              name="content"
              [(ngModel)]="contentId"
              (ngModelChange)="invalidate()"
              required
            >
              <option value="">Choose content</option>
              @for (row of choices; track row.id) {
                <option [value]="row.id">{{ row.title }}</option>
              }
            </select></label
          >
          <div class="actions">
            <button
              type="button"
              (click)="searchContent(contentPage - 1)"
              [disabled]="busy() || contentPage === 0"
            >
              Previous results
            </button>
            <button
              type="button"
              (click)="searchContent(contentPage + 1)"
              [disabled]="busy() || !contentMore"
            >
              More results
            </button>
          </div>
        }
        @if (audience === 'test') {
          <label
            >My test device<select
              name="device"
              [(ngModel)]="testDevice"
              (ngModelChange)="invalidate()"
              required
            >
              <option value="">Choose your device</option>
              @for (device of devices; track device.id) {
                <option [value]="device.id">
                  {{ device.platform }} · {{ device.app_version }} · Last synced
                  {{ device.updated_at | date: 'short' }}
                </option>
              }
            </select></label
          >
          @if (!devices.length) {
            <p>
              Install the updated app, sign in with your owner email, enable Sanctuary
              announcements, and refresh the Users directory and this screen.
            </p>
          }
        }
        <div class="phone" aria-label="Notification preview">
          <small>SANCTUARY · PREVIEW</small><strong>{{ title || 'Your message title' }}</strong>
          <p>{{ body || 'Your invitation to prayer appears here.' }}</p>
        </div>
        <button
          class="primary"
          [disabled]="
            busy() ||
            !title.trim() ||
            !body.trim() ||
            (kind !== 'home' && !contentId) ||
            (audience === 'test' && !testDevice)
          "
        >
          Review →
        </button>
      </form>
      @if (preview) {
        <section class="review" aria-labelledby="push-review">
          <h2 id="push-review">Review</h2>
          <p><strong>Audience:</strong> {{ audienceLabel(audience) }}</p>
          <p><strong>Opens:</strong> {{ destinationLabel() }}</p>
          @if (!preview.count) {
            <p role="status">No opted-in devices are available for this audience.</p>
          }
          <label class="check"
            ><input type="checkbox" [(ngModel)]="confirmed" /> I reviewed the message, destination,
            and audience.</label
          >
          <button
            class="primary"
            (click)="send()"
            [disabled]="busy() || !confirmed || !enabled || !preview.count"
          >
            {{ audience === 'test' ? 'Send test notification' : 'Send notification' }}
          </button>
        </section>
      }
    </section>
    <section class="panel">
      <div class="actions">
        <h2>Past announcements</h2>
        <button (click)="load()" [disabled]="refreshingHistory()">
          {{ refreshingHistory() ? 'Refreshing…' : 'Refresh history' }}
        </button>
        <button
          type="button"
          (click)="requestClearHistory()"
          [disabled]="busy() || !history.length"
        >
          Clear completed history
        </button>
      </div>
      <p>Submission status does not confirm delivery or reading.</p>
      @if (historyError()) {
        <p role="status">{{ historyError() }}</p>
      }
      @if (confirmClearHistory) {
        <section
          class="history-confirm"
          role="alertdialog"
          aria-labelledby="clear-history-title"
          aria-describedby="clear-history-body"
        >
          <h3 id="clear-history-title">Clear past announcements?</h3>
          <p id="clear-history-body">
            Completed entries up to the latest announcement on this page will be removed from
            history. Pending sends stay visible. This does not remove notifications from phones or
            cancel sends.
          </p>
          <button type="button" (click)="clearHistory()" [disabled]="busy()">
            Clear completed history
          </button>
          <button type="button" (click)="confirmClearHistory = false" [disabled]="busy()">
            Cancel
          </button>
        </section>
      }
      @for (row of history; track row.id) {
        <article>
          <strong>{{ row.title }}</strong>
          <p>{{ row.body }}</p>
          <small>{{ row.created_at | date: 'medium' }} · {{ audienceLabel(row.audience) }}</small>
          <p class="submission-status">{{ submissionStatus(row) }}</p>
        </article>
      } @empty {
        <p>No messages sent yet.</p>
      }
      <div class="actions">
        <button (click)="load(page - 1)" [disabled]="refreshingHistory() || page === 0">
          Previous</button
        ><span>Page {{ page + 1 }}</span
        ><button (click)="load(page + 1)" [disabled]="refreshingHistory() || !more">Next</button>
      </div>
    </section>`,
})
export class AdminMessagesComponent implements OnInit, OnDestroy {
  @Output() sessionExpired = new EventEmitter<void>();
  private http = new HttpClient(inject(HttpBackend));
  private base = inject(SANCTUARY_API_BASE_URL) + '/admin-api';
  busy = signal(false);
  notice = signal('');
  completed = signal(false);
  refreshingHistory = signal(false);
  historyError = signal('');
  confirmClearHistory = false;
  private clearThroughId = '';
  private historyGeneration = 0;
  private destroyed = false;
  @ViewChild('composerTitle') composerTitle?: AdminInputComponent;
  enabled = false;
  title = '';
  body = '';
  kind = 'home';
  contentId = '';
  audience = 'test';
  testDevice = '';
  query = '';
  choices: any[] = [];
  devices: any[] = [];
  history: any[] = [];
  page = 0;
  more = false;
  contentPage = 0;
  contentMore = false;
  preview: any = null;
  confirmed = false;
  requestId = '';
  sentSnapshot = '';
  hasDraft() {
    return (
      (!!this.title.trim() || !!this.body.trim()) &&
      JSON.stringify(this.draft()) !== this.sentSnapshot
    );
  }
  audienceLabel(value: string) {
    return (
      (
        {
          all: 'All opted-in devices',
          members: 'Signed-in users',
          guests: 'Signed-out devices (including unregistered people)',
          test: 'Owner test only',
          ios: 'iOS devices',
          android: 'Android devices',
        } as Record<string, string>
      )[value] || value
    );
  }
  destinationLabel() {
    if (this.kind === 'home') return 'Home';
    const name = this.choices.find((row) => row.id === this.contentId)?.title;
    const kind =
      (
        { prayers: 'Prayer', saints: 'Saint / feast day', novenas: 'Novena' } as Record<
          string,
          string
        >
      )[this.kind] || 'Content';
    return name ? kind + ': ' + name : kind;
  }
  submissionStatus(row: any) {
    if (Number(row.pending) > 0) return 'Queued for sending';
    if (Number(row.unknown) > 0) return 'Sending status could not be confirmed';
    if (Number(row.accepted) > 0) {
      return Number(row.failed) > 0 || Number(row.skipped) > 0
        ? 'Submitted to Firebase with sending issues'
        : 'Submitted to Firebase';
    }
    if (Number(row.failed) > 0) return 'Sending failed';
    if (Number(row.skipped) > 0) return 'Not sent';
    return 'Submission recorded';
  }
  private resetComposer() {
    this.title = '';
    this.body = '';
    this.kind = 'home';
    this.contentId = '';
    this.audience = 'test';
    this.testDevice = '';
    this.query = '';
    this.choices = [];
    this.contentPage = 0;
    this.contentMore = false;
    this.sentSnapshot = '';
    this.invalidate();
  }
  ngOnInit() {
    void this.load();
  }
  draft() {
    return {
      title: this.title,
      body: this.body,
      kind: this.kind,
      contentId: this.contentId,
      audience: this.audience,
      testDevice: this.testDevice || null,
    };
  }
  invalidate() {
    this.completed.set(false);
    this.notice.set('');
    this.preview = null;
    this.confirmed = false;
    this.requestId = '';
  }
  async api(method: string, path: string, body?: unknown) {
    return firstValueFrom(
      this.http
        .request<any>(method, this.base + path, {
          body,
          withCredentials: true,
          headers: { 'X-Sanctuary-Admin': '1', 'X-Sanctuary-Admin-Client': 'cookie-v2' },
        })
        .pipe(timeout(15000)),
    );
  }
  async run(work: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true);
    this.notice.set('');
    try {
      await work();
    } catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 401) this.sessionExpired.emit();
      this.notice.set(
        e instanceof HttpErrorResponse
          ? e.error?.message || 'Request failed. Please try again.'
          : 'Request failed. Please try again.',
      );
    } finally {
      this.busy.set(false);
    }
  }
  ngOnDestroy() {
    this.destroyed = true;
    this.historyGeneration++;
  }
  createAnother() {
    this.completed.set(false);
    this.notice.set('');
    this.composerTitle?.field?.nativeElement.focus();
  }
  async load(page = this.page) {
    const generation = ++this.historyGeneration;
    this.refreshingHistory.set(true);
    this.historyError.set('');
    try {
      const result = await this.api('GET', '/messages?page=' + page);
      if (this.destroyed || generation !== this.historyGeneration) return;
      this.history = result.items;
      this.more = result.hasMore;
      this.devices = result.testDevices;
      this.enabled = result.enabled;
      this.page = page;
    } catch (error) {
      if (this.destroyed || generation !== this.historyGeneration) return;
      if (error instanceof HttpErrorResponse && error.status === 401) this.sessionExpired.emit();
      this.historyError.set(
        'History could not refresh. You can still create a message; try refreshing history again.',
      );
    } finally {
      if (!this.destroyed && generation === this.historyGeneration)
        this.refreshingHistory.set(false);
    }
  }
  requestClearHistory() {
    if (!this.history.length) return;
    this.clearThroughId = this.history[0].id;
    this.confirmClearHistory = true;
  }
  async clearHistory() {
    if (!this.confirmClearHistory || !this.clearThroughId) return;
    const throughMessageId = this.clearThroughId;
    await this.run(async () => {
      const result = await this.api('DELETE', '/messages/history', { throughMessageId });
      this.confirmClearHistory = false;
      this.completed.set(false);
      this.notice.set(
        result.cleared
          ? 'Completed history cleared. Pending sends stay visible.'
          : 'No completed history to clear. Pending sends stay visible.',
      );
      void this.load(0);
    });
  }
  changeKind() {
    this.contentId = '';
    this.query = '';
    this.choices = [];
    this.invalidate();
    if (this.kind !== 'home') void this.searchContent();
  }
  async searchContent(page = 0) {
    await this.run(async () => {
      const result = await this.api(
        'GET',
        '/content/' + this.kind + '?q=' + encodeURIComponent(this.query) + '&page=' + page,
      );
      this.choices = result.items;
      this.contentPage = page;
      this.contentMore = result.hasMore;
    });
  }
  async review() {
    await this.run(async () => {
      const draft = this.draft();
      const snapshot = JSON.stringify(draft);
      const preview = await this.api('POST', '/messages/preview', draft);
      if (JSON.stringify(this.draft()) !== snapshot) return;
      this.preview = preview;
      this.confirmed = false;
      this.requestId ||= crypto.randomUUID();
    });
  }
  async send() {
    if (!this.preview || !this.confirmed) return;
    await this.run(async () => {
      const sentDraft = this.draft();
      await this.api('POST', '/messages', {
        id: this.requestId,
        draft: sentDraft,
        recipientVersion: this.preview.recipientVersion,
      });
      this.sentSnapshot = JSON.stringify(sentDraft);
      if (JSON.stringify(this.draft()) === this.sentSnapshot) this.resetComposer();
      else this.invalidate();
      this.notice.set('Announcement submitted for sending.');
      this.completed.set(true);
      // History is independent: a delayed refresh must never lock the fresh composer.
      void this.load(0);
    });
  }
}
