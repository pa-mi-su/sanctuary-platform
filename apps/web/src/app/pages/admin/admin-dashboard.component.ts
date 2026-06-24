import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import {
  AdminDeviceInstall,
  AdminNotification,
  AdminUserMetrics,
  SanctuaryApiService,
} from '../../core/api/sanctuary-api.service';

type AdminLoadState = 'idle' | 'loading' | 'ready' | 'forbidden' | 'error';
type MetricTone = 'primary' | 'good' | 'warning' | 'neutral';

interface WebBuildInfo {
  version?: string;
  build?: string;
  environment?: string;
}

declare global {
  interface Window {
    SANCTUARY_BUILD_INFO?: WebBuildInfo;
  }
}

interface MetricCard {
  label: string;
  value: string;
  helper: string;
  tone?: MetricTone;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './admin-dashboard.component.scss',
  template: `
    <main class="admin-shell">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Sanctuary Admin</p>
          <h1>Phones</h1>
          <p>Installed phones, active phones, and a simple notification send.</p>
          <p class="build-copy">Version {{ appVersion }} · Build {{ appBuild }}{{ environmentLabel }}</p>
        </div>
        <div class="header-actions">
          <a class="button-secondary button-link" href="/">Return to Site</a>
          <button type="button" (click)="reload()" [disabled]="state() === 'loading'">Refresh</button>
        </div>
      </header>

      @if (state() === 'forbidden') {
        <section class="notice-card notice-card--danger">
          <h2>Admin Access Required</h2>
          <p>Your account is signed in, but it is not enabled for admin access.</p>
        </section>
      } @else if (state() === 'error') {
        <section class="notice-card notice-card--danger">
          <h2>Admin Data Unavailable</h2>
          <p>{{ errorMessage() }}</p>
          <button type="button" (click)="reload()">Try Again</button>
        </section>
      } @else {
        <section class="kpi-grid" aria-label="Phone summary">
          @for (metric of phoneMetrics(); track metric.label) {
            <article [class]="'metric-card metric-card--hero metric-card--' + (metric.tone ?? 'neutral')">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.helper }}</p>
            </article>
          }
        </section>

        <section class="admin-grid">
          <article class="admin-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Notify</p>
                <h2>Send To Reachable Phones</h2>
                <p>{{ notificationReachCopy() }}</p>
              </div>
              <span class="status-pill">{{ formatNumber(metrics()?.notificationsEnabledDeviceCount) }} reachable</span>
            </div>

            <form class="notification-form" (ngSubmit)="sendNotificationNow()">
              <label>
                <span>Title</span>
                <input type="text" name="title" maxlength="120" [(ngModel)]="notificationTitle" />
              </label>
              <label>
                <span>Message</span>
                <textarea name="message" rows="4" maxlength="500" [(ngModel)]="notificationMessage"></textarea>
              </label>
              @if (sendStatus()) {
                <p class="form-status">{{ sendStatus() }}</p>
              }
              <button
                type="submit"
                [disabled]="sendPending() || !notificationTitle.trim() || !notificationMessage.trim() || !hasReachablePhones()"
              >
                {{ sendPending() ? 'Sending...' : 'Send Notification' }}
              </button>
            </form>
          </article>

          <article class="admin-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Recent Sends</p>
                <h2>Notification History</h2>
              </div>
            </div>
            <div class="history-list">
              @for (notification of recentNotifications(); track notification.id) {
                <article class="history-row history-row--simple">
                  <div>
                    <strong>{{ notification.title }}</strong>
                    <p>{{ notification.message }}</p>
                    <small>{{ notificationTimelineLabel(notification) }}</small>
                  </div>
                  <span>
                    {{ notification.targetCount }} targeted · {{ notification.sentCount }} sent · {{ notification.failedCount }} failed
                  </span>
                </article>
              } @empty {
                <p class="empty-copy">No notifications sent yet.</p>
              }
            </div>
          </article>
        </section>

        <section class="admin-panel installs-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Phones</p>
              <h2>Recent Phone Records</h2>
              <p>Each row is deduped by app install when available, then by push token or record id.</p>
            </div>
            <span>{{ recentDeviceInstalls().length }} loaded</span>
          </div>

          <div class="metadata-strip" aria-label="Phone metadata summary">
            <span>{{ pushReadyPlatformMixLabel() }} reachable</span>
            <span>{{ formatNumber(metrics()?.anonymousActiveDevicesToday) }} anonymous active today</span>
            <span>{{ formatNumber(metrics()?.invalidTokenCount) }} bad push tokens</span>
          </div>

          <div class="install-table" role="table" aria-label="Recent phones">
            <div class="install-row install-row--header" role="row">
              <span>Phone</span>
              <span>Owner</span>
              <span>App</span>
              <span>Push</span>
              <span>First Seen</span>
              <span>Last Seen</span>
            </div>
            @for (install of recentDeviceInstalls(); track installKey(install)) {
              <div class="install-row" role="row">
                <span>
                  <strong>{{ formatPlatform(install.platform) }}</strong>
                  <small>{{ install.signedIn ? 'Signed in' : 'Anonymous' }}</small>
                </span>
                <span>
                  <strong>{{ installOwnerLabel(install) }}</strong>
                  <small>{{ install.userEmail || install.userId || 'No account' }}</small>
                </span>
                <span>
                  <strong>{{ install.appVersion || 'unknown' }}</strong>
                  <small>{{ formatLanguage(install.language) }}</small>
                </span>
                <span [class]="install.pushReady ? 'status-text status-text--accepted' : 'status-text status-text--failed'">
                  {{ installPushLabel(install) }}
                </span>
                <span>{{ formatDate(install.firstSeenAt) }}</span>
                <span>{{ formatDate(install.lastSeenAt) }}</span>
              </div>
            } @empty {
              <p class="empty-copy">No phones have reported yet.</p>
            }
          </div>
        </section>
      }
    </main>
  `,
})
export class AdminDashboardComponent {
  private readonly api = inject(SanctuaryApiService);
  private readonly buildInfo = globalThis.window?.SANCTUARY_BUILD_INFO ?? {};

  protected readonly state = signal<AdminLoadState>('idle');
  protected readonly errorMessage = signal('Sanctuary could not load admin data.');
  protected readonly metrics = signal<AdminUserMetrics | null>(null);
  protected readonly recentDeviceInstalls = signal<AdminDeviceInstall[]>([]);
  protected readonly notifications = signal<AdminNotification[]>([]);
  protected readonly sendPending = signal(false);
  protected readonly sendStatus = signal<string | null>(null);

  protected notificationTitle = '';
  protected notificationMessage = '';
  protected readonly appVersion = this.buildInfo.version?.trim() || '0.0.0';
  protected readonly appBuild = this.buildInfo.build?.trim() || 'local';
  protected readonly environmentLabel = this.formatEnvironment(this.buildInfo.environment);

  protected readonly phoneMetrics = computed<MetricCard[]>(() => {
    const metrics = this.metrics();
    return [
      {
        label: 'Known phones',
        value: this.formatNumber(metrics?.knownAppInstallCount),
        helper: 'Distinct app installs the backend has seen.',
        tone: 'primary',
      },
      {
        label: 'Active now',
        value: this.formatNumber(metrics?.activeKnownDeviceCountRecent),
        helper: 'Phones seen in the last 5 minutes.',
        tone: 'good',
      },
      {
        label: 'Can notify',
        value: this.formatNumber(metrics?.notificationsEnabledDeviceCount),
        helper: this.pushReadyPlatformMixLabel(),
        tone: metrics?.notificationsEnabledDeviceCount ? 'good' : 'warning',
      },
      {
        label: 'Accounts',
        value: this.formatNumber(metrics?.totalUsers),
        helper: `${this.formatNumber(metrics?.activeUsersToday)} signed in today`,
        tone: 'neutral',
      },
    ];
  });

  protected readonly recentNotifications = computed(() => (
    this.notifications().filter((notification) => notification.status !== 'draft').slice(0, 5)
  ));

  constructor() {
    void this.reload();
  }

  protected async reload(): Promise<void> {
    this.state.set('loading');
    this.errorMessage.set('Sanctuary could not load admin data.');

    try {
      const [usersResponse, notifications] = await Promise.all([
        firstValueFrom(this.api.listAdminUsers(100)),
        firstValueFrom(this.api.listAdminNotifications(10)),
      ]);
      this.metrics.set(usersResponse.metrics);
      this.recentDeviceInstalls.set(usersResponse.recentDeviceInstalls ?? []);
      this.notifications.set(notifications);
      this.state.set('ready');
    } catch (error) {
      this.state.set(this.statusCode(error) === 403 ? 'forbidden' : 'error');
      this.errorMessage.set(this.errorCopy(error));
    }
  }

  protected async sendNotificationNow(): Promise<void> {
    const title = this.notificationTitle.trim();
    const message = this.notificationMessage.trim();
    if (!title || !message || this.sendPending()) {
      return;
    }

    if (!this.hasReachablePhones()) {
      this.sendStatus.set('No reachable phones are available yet.');
      return;
    }

    this.sendPending.set(true);
    this.sendStatus.set(null);
    try {
      const result = await firstValueFrom(this.api.sendAdminNotification({ title, message }));
      this.notificationTitle = '';
      this.notificationMessage = '';
      this.sendStatus.set(`${result.sentCount} sent, ${result.failedCount} failed, ${result.targetCount} targeted.`);
      await this.reload();
    } catch (error) {
      this.sendStatus.set(this.notificationSendErrorCopy(error));
    } finally {
      this.sendPending.set(false);
    }
  }

  protected hasReachablePhones(): boolean {
    return (this.metrics()?.notificationsEnabledDeviceCount ?? 0) > 0;
  }

  protected notificationReachCopy(): string {
    const metrics = this.metrics();
    return `${this.formatNumber(metrics?.pushReadyIosDeviceCount)} iOS and ${this.formatNumber(metrics?.pushReadyAndroidDeviceCount)} Android phones have permission and a valid push token.`;
  }

  protected notificationTimelineLabel(notification: AdminNotification): string {
    const date = notification.sentAt ?? notification.updatedAt ?? notification.createdAt;
    const label = notification.sentAt ? 'Sent' : notification.status;
    return `${label} ${this.formatDate(date)}`;
  }

  protected installOwnerLabel(install: AdminDeviceInstall): string {
    return install.userDisplayName || install.userEmail || 'Anonymous';
  }

  protected installPushLabel(install: AdminDeviceInstall): string {
    if (install.pushReady) {
      return 'Ready';
    }
    if (!install.notificationsEnabled) {
      return 'Permission off';
    }
    if (!install.hasPushToken) {
      return 'No token';
    }
    if (install.tokenStatus !== 'valid') {
      return `Token ${install.tokenStatus}`;
    }
    return 'Not ready';
  }

  protected installKey(install: AdminDeviceInstall): string {
    return install.clientInstanceId || install.id;
  }

  protected formatPlatform(value: string | null): string {
    switch (value) {
      case 'ios':
        return 'iOS';
      case 'android':
        return 'Android';
      default:
        return value || 'Unknown';
    }
  }

  protected formatDate(value: string | null): string {
    if (!value) {
      return 'Never';
    }
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(new Date(value));
  }

  protected formatLanguage(value: string | null): string {
    switch (value) {
      case 'en':
        return 'English';
      case 'es':
        return 'Spanish';
      case 'pl':
        return 'Polish';
      default:
        return 'Unknown';
    }
  }

  protected pushReadyPlatformMixLabel(): string {
    const metrics = this.metrics();
    return `${this.formatNumber(metrics?.pushReadyIosDeviceCount)} iOS / ${this.formatNumber(metrics?.pushReadyAndroidDeviceCount)} Android`;
  }

  protected formatNumber(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-US').format(value ?? 0);
  }

  private statusCode(error: unknown): number | null {
    return typeof error === 'object' && error !== null && 'status' in error
      ? Number((error as { status: unknown }).status)
      : null;
  }

  private errorCopy(error: unknown): string {
    const status = this.statusCode(error);
    if (status === 401) {
      return 'Please sign in before opening the admin dashboard.';
    }
    if (status === 403) {
      return 'Your account is not enabled for admin access.';
    }
    if (status === 503) {
      return 'Firebase notifications are not configured yet.';
    }
    return 'Sanctuary could not complete that admin request.';
  }

  private notificationSendErrorCopy(error: unknown): string {
    if (this.statusCode(error) === 409) {
      return 'No reachable phones are available yet.';
    }
    return this.errorCopy(error);
  }

  private formatEnvironment(rawEnvironment: string | undefined): string {
    const normalized = rawEnvironment?.trim();
    return normalized ? ` · ${normalized.toUpperCase()}` : '';
  }
}
