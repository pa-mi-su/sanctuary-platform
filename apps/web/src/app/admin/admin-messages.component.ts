import { Component, EventEmitter, Output, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
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
    @if (notice()) {
      <p role="status" class="notice">{{ notice() }}</p>
    }
    <section class="panel">
      <h2>Create a message</h2>
      @if (!enabled) {
        <p>Sending is disabled in this environment. You can still prepare and preview a message.</p>
      }
      <form (ngSubmit)="review()">
        <admin-input
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
          Review recipients →
        </button>
      </form>
      @if (preview) {
        <section class="review" aria-labelledby="push-review">
          <h2 id="push-review">Review before sending</h2>
          <p>
            <strong>{{ preview.count }} app installations</strong> ·
            {{ audienceLabel(audience) }}
          </p>
          <p>Destination: {{ preview.destination }}</p>
          <p>
            This counts app installations, not unique phones. Reinstalls and automated tests can
            leave older registrations.
          </p>
          <details class="recipient-list">
            <summary>See all {{ preview.count }} recipient devices</summary>
            <p>
              Device IDs identify installations, not phone models. Signed-out devices have no
              account identity.
            </p>
            <label
              >Find a recipient
              <input
                type="search"
                [(ngModel)]="recipientSearch"
                placeholder="Search account, platform, version or device ID"
              />
            </label>
            <p role="status">{{ filteredRecipients().length }} of {{ preview.count }} devices</p>
            <ul>
              @for (device of filteredRecipients(); track device.id) {
                <li>
                  <strong
                    >{{ device.platform === 'ios' ? 'iOS' : 'Android' }}
                    {{ device.device_label || '' }} ·
                    {{
                      device.display_name ||
                        device.email ||
                        (device.signedIn ? 'Signed-in account' : 'Signed-out device')
                    }}</strong
                  >
                  @if (device.email) {
                    <span>{{ device.email }}</span>
                  }
                  <span
                    >{{ device.signedIn ? 'Signed in' : 'Guest / signed out' }} · App
                    {{ device.app_version }}</span
                  >
                  <span>Device {{ device.id }}</span>
                  <span>Last sync {{ device.updated_at | date: 'medium' }}</span>
                  <button type="button" (click)="retireCandidate = device">
                    Retire this registration
                  </button>
                </li>
              } @empty {
                <li>No matching devices.</li>
              }
            </ul>
          </details>
          @if (retireCandidate) {
            <section role="alertdialog" aria-label="Retire device registration" class="panel">
              <h3>Retire this registration?</h3>
              <p>
                {{ retireCandidate.platform }} ·
                {{ retireCandidate.device_label || retireCandidate.id }}
              </p>
              <p>
                It will be excluded from future sends. A currently active app can register again on
                its next sync. Sending history is retained.
              </p>
              <button type="button" (click)="retireDevice()" [disabled]="busy()">
                Retire registration
              </button>
              <button type="button" (click)="retireCandidate = null">Cancel</button>
            </section>
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
        <h2>Sending history</h2>
        <button (click)="load()" [disabled]="busy()">Refresh results</button>
      </div>
      <p>
        Accepted means Firebase accepted a send. It does not confirm delivery, display, or reading.
      </p>
      @for (row of history; track row.id) {
        <article>
          <strong>{{ row.title }}</strong>
          <p>{{ row.body }}</p>
          <small
            >{{ row.created_at | date: 'medium' }} · {{ audienceLabel(row.audience) }} ·
            {{ row.actor_sub }}</small
          >
          <p>
            {{ row.recipients }} recipients · {{ row.accepted }} accepted · {{ row.failed }} failed
            · {{ row.pending }} pending · {{ row.skipped }} skipped · {{ row.unknown }} uncertain
          </p>
          <details class="recipient-list" (toggle)="historyToggled(row, $event)">
            <summary>View recipient devices and results</summary>
            <p>
              Device IDs come from this send. Model and app version reflect the current
              registration; removed registrations may have limited details.
            </p>
            <label
              >Find a device<input
                type="search"
                [(ngModel)]="historySearch[row.id]"
                placeholder="Search device, account or result"
            /></label>
            @if (!historyDevices[row.id]) {
              <p>Open to load recipient details.</p>
            }
            <ul>
              @for (device of filteredHistory(row.id); track device.id) {
                <li>
                  <strong
                    >{{ device.device_label || device.platform || 'Removed registration' }} ·
                    {{ device.state }}</strong
                  >
                  <span>{{
                    device.email || device.display_name || 'No linked account at send time'
                  }}</span>
                  <span
                    >Device {{ device.device_id || 'No longer retained' }} · App
                    {{ device.app_version || 'Unknown' }}</span
                  >
                  @if (device.error_code) {
                    <span>{{ device.error_code }}</span>
                  }
                </li>
              } @empty {
                <li>No matching device results loaded.</li>
              }
            </ul>
          </details>
        </article>
      } @empty {
        <p>No messages sent yet.</p>
      }
      <div class="actions">
        <button (click)="load(page - 1)" [disabled]="busy() || page === 0">Previous</button
        ><span>Page {{ page + 1 }}</span
        ><button (click)="load(page + 1)" [disabled]="busy() || !more">Next</button>
      </div>
    </section>`,
})
export class AdminMessagesComponent implements OnInit {
  @Output() sessionExpired = new EventEmitter<void>();
  private http = new HttpClient(inject(HttpBackend));
  private base = inject(SANCTUARY_API_BASE_URL) + '/admin-api';
  busy = signal(false);
  notice = signal('');
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
  retireCandidate: any = null;
  historyDevices: Record<string, any[]> = {};
  historySearch: Record<string, string> = {};
  async historyToggled(row: any, event: Event) {
    if (!(event.target as HTMLDetailsElement).open) return;
    await this.run(async () => {
      const result = await this.api('GET', '/messages/' + row.id + '/recipients');
      this.historyDevices[row.id] = result.devices;
    });
  }
  filteredHistory(id: string) {
    const query = (this.historySearch[id] || '').trim().toLowerCase();
    return (this.historyDevices[id] || []).filter((d: any) =>
      [d.device_id, d.device_label, d.platform, d.email, d.display_name, d.state, d.error_code]
        .join(' ')
        .toLowerCase()
        .includes(query),
    );
  }
  async retireDevice() {
    if (!this.retireCandidate) return;
    const id = this.retireCandidate.id;
    await this.run(async () => {
      await this.api('POST', '/messages/devices/' + id + '/retire', {});
      this.retireCandidate = null;
      this.invalidate();
      this.notice.set('Registration retired. Review recipients again before sending.');
    });
  }
  recipientSearch = '';
  filteredRecipients() {
    const query = this.recipientSearch.trim().toLowerCase();
    return (this.preview?.devices || []).filter((device: any) =>
      [
        device.id,
        device.platform,
        device.app_version,
        device.email,
        device.display_name,
        device.signedIn ? 'signed in' : 'guest signed out',
      ]
        .join(' ')
        .toLowerCase()
        .includes(query),
    );
  }
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
    this.retireCandidate = null;
    this.preview = null;
    this.confirmed = false;
    this.requestId = '';
  }
  async api(method: string, path: string, body?: unknown) {
    return firstValueFrom(
      this.http.request<any>(method, this.base + path, {
        body,
        withCredentials: true,
        headers: { 'X-Sanctuary-Admin': '1', 'X-Sanctuary-Admin-Client': 'cookie-v2' },
      }),
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
  async load(page = this.page) {
    await this.run(async () => {
      const result = await this.api('GET', '/messages?page=' + page);
      this.history = result.items;
      this.more = result.hasMore;
      this.devices = result.testDevices;
      this.enabled = result.enabled;
      this.page = page;
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
      this.recipientSearch = '';
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
      this.invalidate();
      const result = await this.api('GET', '/messages?page=0');
      this.history = result.items;
      this.page = 0;
      this.more = result.hasMore;
      this.notice.set('Message queued. Refresh results to see Firebase acceptance and failures.');
    });
  }
}
