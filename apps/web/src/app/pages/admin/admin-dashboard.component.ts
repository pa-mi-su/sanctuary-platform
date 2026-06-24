import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import {
  AdminDeviceInstall,
  AdminNotification,
  AdminNotificationDelivery,
  AdminUserAccess,
  AdminUserListItem,
  AdminUserMetrics,
  SanctuaryApiService,
  UserProfile,
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

interface MetricSection {
  title: string;
  summary: string;
  cards: MetricCard[];
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
          <h1>Operations Dashboard</h1>
          <p>Send admin notifications and check the few signals needed to test delivery.</p>
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
          <p>Your account is signed in, but it is not enabled in the admin list.</p>
        </section>
      } @else if (state() === 'error') {
        <section class="notice-card notice-card--danger">
          <h2>Admin Data Unavailable</h2>
          <p>{{ errorMessage() }}</p>
          <button type="button" (click)="reload()">Try Again</button>
        </section>
      } @else {
        <section class="kpi-grid" aria-label="Admin dashboard summary">
          @for (metric of heroMetrics(); track metric.label) {
            <article [class]="'metric-card metric-card--hero metric-card--' + (metric.tone ?? 'neutral')">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.helper }}</p>
            </article>
          }
        </section>

        <section class="metric-sections" aria-label="Detailed operations metrics">
          @for (section of metricSections(); track section.title) {
            <article class="metric-section">
              <div class="section-heading">
                <div>
                  <p class="eyebrow">{{ section.title }}</p>
                  <p>{{ section.summary }}</p>
                </div>
              </div>
              <div class="metrics-grid">
                @for (metric of section.cards; track metric.label) {
                  <article [class]="'metric-card metric-card--' + (metric.tone ?? 'neutral')">
                    <span>{{ metric.label }}</span>
                    <strong>{{ metric.value }}</strong>
                    <p>{{ metric.helper }}</p>
                  </article>
                }
              </div>
            </article>
          }
        </section>

        <section class="admin-grid">
          <article class="admin-panel notification-history-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Admin Access</p>
                <h2>Manage Administrators</h2>
              </div>
              <span class="status-pill">Audit logged</span>
            </div>

            <div class="admin-access-search">
              <label>
                <span>Filter users</span>
                <input
                  type="search"
                  name="adminEmailSearch"
                  autocomplete="off"
                  [(ngModel)]="adminAccessEmail"
                  placeholder="Name or email"
                />
              </label>
            </div>

            @if (adminAccessStatus()) {
              <p class="form-status">{{ adminAccessStatus() }}</p>
            }

            <div class="admin-access-list">
              @for (user of filteredAdminUsers(); track user.userId) {
                <article class="admin-access-row">
                  <div>
                    <strong>{{ adminAccessDisplayName(user) }}</strong>
                    <small>{{ user.email || user.userId }}</small>
                    <span [class]="user.admin ? 'access-pill access-pill--admin' : 'access-pill'">
                      {{ user.admin ? 'Admin enabled' : 'Standard user' }}
                    </span>
                  </div>
                  <label class="admin-toggle">
                    <input
                      type="checkbox"
                      [checked]="user.admin"
                      [disabled]="adminAccessPending() || isSelfAdminRemoval(user)"
                      (change)="toggleAdminAccess(user)"
                    />
                    <span>Admin</span>
                  </label>
                </article>
                @if (isSelfAdminRemoval(user)) {
                  <p class="row-warning">You cannot remove your own admin access.</p>
                }
              } @empty {
                <p class="empty-copy">No matching users loaded.</p>
              }
            </div>
          </article>

          <article class="admin-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Notifications</p>
                <h2>Create Draft</h2>
              </div>
              <span class="status-pill">All users</span>
            </div>

            <form class="notification-form" (ngSubmit)="createDraft()">
              <label>
                <span>Title</span>
                <input type="text" name="title" maxlength="120" [(ngModel)]="draftTitle" placeholder="Sanctuary update" />
              </label>
              <label>
                <span>Message</span>
                <textarea name="message" rows="5" maxlength="500" [(ngModel)]="draftMessage" placeholder="Write a short message for users."></textarea>
              </label>
              @if (draftStatus()) {
                <p class="form-status">{{ draftStatus() }}</p>
              }
              <button type="submit" [disabled]="draftPending() || !draftTitle.trim() || !draftMessage.trim()">
                Save Draft
              </button>
            </form>
          </article>

          <article class="admin-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">History</p>
                <h2>Notification Drafts</h2>
              </div>
            </div>
            <div class="history-list">
              <div [class]="hasPushReadyDevices() ? 'delivery-readiness delivery-readiness--ready' : 'delivery-readiness delivery-readiness--waiting'">
                <div>
                  <strong>{{ deliveryReadinessTitle() }}</strong>
                  <p>{{ deliveryReadinessCopy() }}</p>
                </div>
                <span>{{ pushReadyDeviceCountLabel() }} reachable</span>
              </div>
              @for (notification of visibleNotifications(); track notification.id) {
                <article class="history-row">
                  <div>
                    <strong>{{ notification.title }}</strong>
                    <p>{{ notification.message }}</p>
                    <small>
                      {{ notification.targetCount }} targeted · {{ notification.sentCount }} FCM accepted · {{ notification.failedCount }} failed
                    </small>
                    <small>{{ notificationTimelineLabel(notification) }}</small>
                  </div>
                  <div class="history-actions">
                    <span>{{ notificationStatusLabel(notification) }}</span>
                    @if (notification.status === 'draft') {
                      <small>{{ draftSendReadinessCopy() }}</small>
                      <button
                        type="button"
                        class="button-secondary"
                        [disabled]="sendingNotificationId() === notification.id || !hasPushReadyDevices()"
                        (click)="sendNotification(notification)"
                      >
                        {{ sendingNotificationId() === notification.id ? 'Sending...' : 'Send' }}
                      </button>
                    }
                  </div>
                </article>
              } @empty {
                <p class="empty-copy">No notification drafts yet.</p>
              }
              @if (notifications().length > 4) {
                <button
                  type="button"
                  class="button-secondary history-toggle"
                  (click)="toggleNotificationHistory()"
                >
                  {{ notificationHistoryExpanded() ? 'Show recent only' : 'Show older notifications' }}
                </button>
              }
            </div>
            @if (sendStatus()) {
              <p class="form-status">{{ sendStatus() }}</p>
            }
          </article>
        </section>

        <section class="admin-panel delivery-log-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Delivery Log</p>
              <h2>Recent Firebase Attempts</h2>
              <p>Accepted means Firebase took the message for that token. It does not prove the OS displayed it.</p>
            </div>
            <span>{{ deliveryLog().length }} rows</span>
          </div>

          <div class="delivery-table" role="table" aria-label="Recent notification delivery attempts">
            <div class="delivery-row delivery-row--header" role="row">
              <span>Notification</span>
              <span>Platform</span>
              <span>FCM Status</span>
              <span>Target</span>
              <span>Reason / Note</span>
              <span>Time</span>
            </div>
            @for (delivery of deliveryLog(); track delivery.id) {
              <div class="delivery-row" role="row">
                <span>
                  <strong>{{ delivery.notificationTitle }}</strong>
                  <small>{{ delivery.notificationId }}</small>
                </span>
                <span>{{ delivery.platform || 'unknown' }}</span>
                <span [class]="delivery.status === 'failed' ? 'status-text status-text--failed' : 'status-text status-text--accepted'">
                  {{ deliveryStatusLabel(delivery) }}
                </span>
                <span>
                  <small>{{ deliveryTargetLabel(delivery) }}</small>
                </span>
                <span>
                  <small>{{ deliveryReason(delivery) }}</small>
                </span>
                <span>{{ formatDate(delivery.sentAt || delivery.updatedAt || delivery.createdAt) }}</span>
              </div>
            } @empty {
              <p class="empty-copy">No delivery attempts recorded yet.</p>
            }
          </div>
        </section>

        <section class="admin-panel installs-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Devices</p>
              <h2>Active App Installs</h2>
              <p>Mobile installs seen in the last 2 hours. Deleted apps fall out after they stop checking in.</p>
            </div>
            <span>{{ recentDeviceInstalls().length }} active</span>
          </div>

          <div class="install-table" role="table" aria-label="Active app installs">
            <div class="install-row install-row--header" role="row">
              <span>Device</span>
              <span>Owner</span>
              <span>App</span>
              <span>Language</span>
              <span>Push</span>
              <span>First Reported</span>
              <span>Last Seen</span>
            </div>
            @for (install of recentDeviceInstalls(); track install.id) {
              <div class="install-row" role="row">
                <span>
                  <strong>{{ formatPlatform(install.platform) }}</strong>
                  <small>{{ install.signedIn ? 'Signed-in record' : 'Anonymous record' }}</small>
                </span>
                <span>
                  <strong>{{ installOwnerLabel(install) }}</strong>
                  <small>{{ install.signedIn ? 'Account-linked' : 'Not signed in' }}</small>
                </span>
                <span>{{ install.appVersion || 'unknown' }}</span>
                <span>{{ formatLanguage(install.language) }}</span>
                <span [class]="install.pushReady ? 'status-text status-text--accepted' : 'status-text status-text--failed'">
                  {{ installPushLabel(install) }}
                </span>
                <span>{{ formatDate(install.firstSeenAt) }}</span>
                <span>{{ formatDate(install.lastSeenAt) }}</span>
              </div>
            } @empty {
              <p class="empty-copy">No active mobile installs reported in the last 2 hours.</p>
            }
          </div>
        </section>

        <section class="admin-panel users-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Users</p>
              <h2>Recent Accounts</h2>
            </div>
            <span>{{ users().length }} loaded</span>
          </div>
          <div class="metadata-strip" aria-label="Device metadata summary">
            <span>{{ pushReadyPlatformMixLabel() }} reachable</span>
            <span>{{ formatNumber(metrics()?.activeKnownDeviceCountRecent) }} active installs now</span>
            <span>{{ formatNumber(metrics()?.invalidTokenCount) }} invalid tokens</span>
          </div>

          <div class="user-table" role="table" aria-label="Recent users">
            <div class="table-row table-row--header" role="row">
              <span>User</span>
              <span>App Language</span>
              <span>Reachable Today</span>
              <span>Latest App</span>
              <span>Push</span>
              <span>Last Device Seen</span>
              <span>Last Sign In</span>
            </div>
            @for (user of users(); track user.userId) {
              <div class="table-row" role="row">
                <span>
                  <strong>{{ displayName(user) }}</strong>
                  <small>{{ user.email || user.userId }}</small>
                </span>
                <span>{{ formatLanguage(user.latestDeviceLanguage || user.preferredLanguage) }}</span>
                <span>{{ userReachableDeviceLabel(user) }}</span>
                <span>{{ user.latestAppVersion || 'unknown' }}</span>
                <span>{{ user.notificationsEnabled ? 'Ready' : 'Not ready' }}</span>
                <span>{{ formatDate(user.latestDeviceLastSeenAt) }}</span>
                <span>{{ formatDate(user.lastSignInAt) }}</span>
              </div>
            } @empty {
              <p class="empty-copy">No users loaded yet.</p>
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
  protected readonly users = signal<AdminUserListItem[]>([]);
  protected readonly recentDeviceInstalls = signal<AdminDeviceInstall[]>([]);
  protected readonly notifications = signal<AdminNotification[]>([]);
  protected readonly deliveryLog = signal<AdminNotificationDelivery[]>([]);
  protected readonly currentProfile = signal<UserProfile | null>(null);
  protected readonly draftPending = signal(false);
  protected readonly draftStatus = signal<string | null>(null);
  protected readonly sendingNotificationId = signal<string | null>(null);
  protected readonly sendStatus = signal<string | null>(null);
  protected readonly adminAccessPending = signal(false);
  protected readonly adminAccessStatus = signal<string | null>(null);
  protected readonly notificationHistoryExpanded = signal(false);

  protected draftTitle = '';
  protected draftMessage = '';
  protected adminAccessEmail = '';
  protected readonly appVersion = this.buildInfo.version?.trim() || '0.0.0';
  protected readonly appBuild = this.buildInfo.build?.trim() || 'local';
  protected readonly environmentLabel = this.formatEnvironment(this.buildInfo.environment);

  protected readonly filteredAdminUsers = computed(() => {
    const query = this.adminAccessEmail.trim().toLowerCase();
    if (!query) {
      return this.users();
    }
    return this.users().filter((user) => {
      const displayName = this.adminAccessDisplayName(user).toLowerCase();
      const email = (user.email ?? '').toLowerCase();
      return displayName.includes(query) || email.includes(query);
    });
  });

  protected readonly visibleNotifications = computed(() => {
    const notifications = this.notifications();
    return this.notificationHistoryExpanded() ? notifications : notifications.slice(0, 4);
  });

  protected readonly heroMetrics = computed<MetricCard[]>(() => {
    const metrics = this.metrics();
    return [
      {
        label: 'Registered accounts',
        value: this.formatNumber(metrics?.totalUsers),
        helper: `${this.formatNumber(metrics?.registeredUsersToday)} new in the last 24h`,
        tone: 'primary',
      },
      {
        label: 'Signed in today',
        value: this.formatNumber(metrics?.activeUsersToday),
        helper: `${this.formatNumber(metrics?.activeUsers30Days)} signed in during the last 30 days`,
        tone: 'good',
      },
      {
        label: 'Active app installs now',
        value: this.formatNumber(metrics?.activeKnownDeviceCountRecent),
        helper: 'Mobile installs seen in the last 2 hours',
        tone: 'neutral',
      },
      {
        label: 'Push-ready installs',
        value: this.formatNumber(metrics?.notificationsEnabledDeviceCount),
        helper: `${this.formatNumber(metrics?.pushReadyIosDeviceCount)} iOS, ${this.formatNumber(metrics?.pushReadyAndroidDeviceCount)} Android can receive Firebase`,
        tone: metrics?.notificationsEnabledDeviceCount ? 'good' : 'warning',
      },
    ];
  });

  protected readonly metricSections = computed<MetricSection[]>(() => {
    const metrics = this.metrics();
    return [
      {
        title: 'Audience',
        summary: 'Account and app-usage signals worth watching while testing.',
        cards: [
          {
            label: 'Registered accounts',
            value: this.formatNumber(metrics?.totalUsers),
            helper: 'Users who created an account.',
            tone: 'primary',
          },
          {
            label: 'New accounts last 24h',
            value: this.formatNumber(metrics?.registeredUsersToday),
            helper: 'Fresh registrations in the last 24 hours.',
          },
          {
            label: 'Signed in last 24h',
            value: this.formatNumber(metrics?.activeUsersToday),
            helper: 'Accounts with a recent login signal.',
          },
          {
            label: 'Signed in last 30 days',
            value: this.formatNumber(metrics?.activeUsers30Days),
            helper: 'Longer-term registered account activity.',
          },
          {
            label: 'Anonymous app activity today',
            value: this.formatNumber(metrics?.anonymousActiveDevicesToday),
            helper: 'Not signed in, seen in the last 24 hours.',
            tone: 'neutral',
          },
          {
            label: 'Anonymous app activity 7 days',
            value: this.formatNumber(metrics?.anonymousActiveDevices7Days),
            helper: 'Not signed in, seen in the last 7 days.',
          },
        ],
      },
      {
        title: 'Push Readiness',
        summary: 'Current mobile installs the backend can see and try to notify.',
        cards: [
          {
            label: 'Push-ready installs',
            value: this.formatNumber(metrics?.notificationsEnabledDeviceCount),
            helper: `${this.formatNumber(metrics?.pushReadyIosDeviceCount)} iOS, ${this.formatNumber(metrics?.pushReadyAndroidDeviceCount)} Android with valid token and permission.`,
            tone: 'primary',
          },
          {
            label: 'Active app installs now',
            value: this.formatNumber(metrics?.activeKnownDeviceCountRecent),
            helper: 'Seen in the last 2 hours. This is the install/use count to watch.',
          },
          {
            label: 'Push-ready platform mix',
            value: this.pushReadyPlatformMixLabel(),
            helper: 'Push-ready targets with valid Firebase tokens and notification permission.',
          },
          {
            label: 'Token health',
            value: `${this.formatNumber(metrics?.validTokenCount)} / ${this.formatNumber(metrics?.invalidTokenCount)}`,
            helper: 'Valid / invalid Firebase tokens.',
            tone: metrics?.invalidTokenCount ? 'warning' : 'good',
          },
          {
            label: 'Unknown app version',
            value: this.formatNumber(metrics?.unknownAppVersionDeviceCount),
            helper: 'Records missing version metadata. Lower is better.',
            tone: metrics?.unknownAppVersionDeviceCount ? 'warning' : 'good',
          },
        ],
      },
      {
        title: 'Firebase Handoff',
        summary: 'Whether Firebase accepted each targeted device message. This is not device-display confirmation.',
        cards: [
          {
            label: 'FCM attempts',
            value: this.formatNumber(metrics?.notificationTargetedCount),
            helper: 'Every device target created by admin sends.',
          },
          {
            label: 'FCM accepted / failed',
            value: `${this.formatNumber(metrics?.notificationSentCount)} / ${this.formatNumber(metrics?.notificationFailedCount)}`,
            helper: 'Backend result from Firebase Admin SDK.',
            tone: metrics?.notificationFailedCount ? 'warning' : 'good',
          },
          {
            label: 'FCM acceptance rate',
            value: this.deliverySuccessRate(),
            helper: 'Accepted by Firebase out of all attempted sends.',
            tone: metrics?.notificationFailedCount ? 'warning' : 'good',
          },
        ],
      },
    ];
  });

  constructor() {
    void this.reload();
  }

  protected async reload(): Promise<void> {
    this.state.set('loading');
    this.errorMessage.set('Sanctuary could not load admin data.');

    try {
      const [usersResponse, notifications, deliveryLog] = await Promise.all([
        firstValueFrom(this.api.listAdminUsers(100)),
        firstValueFrom(this.api.listAdminNotifications(20)),
        firstValueFrom(this.api.listAdminNotificationDeliveries(30)),
      ]);
      this.metrics.set(usersResponse.metrics);
      this.users.set(usersResponse.users);
      this.recentDeviceInstalls.set(usersResponse.recentDeviceInstalls ?? []);
      this.notifications.set(notifications);
      this.deliveryLog.set(deliveryLog);
      this.notificationHistoryExpanded.set(false);
      await this.loadCurrentProfile();
      this.state.set('ready');
    } catch (error) {
      this.state.set(this.statusCode(error) === 403 ? 'forbidden' : 'error');
      this.errorMessage.set(this.errorCopy(error));
    }
  }

  protected async createDraft(): Promise<void> {
    const title = this.draftTitle.trim();
    const message = this.draftMessage.trim();
    if (!title || !message) {
      return;
    }

    this.draftPending.set(true);
    this.draftStatus.set(null);
    try {
      await firstValueFrom(this.api.createAdminNotificationDraft({ title, message }));
      this.draftTitle = '';
      this.draftMessage = '';
      this.draftStatus.set('Draft saved.');
      await this.reload();
    } catch (error) {
      this.draftStatus.set(this.errorCopy(error));
    } finally {
      this.draftPending.set(false);
    }
  }

  protected async sendNotification(notification: AdminNotification): Promise<void> {
    if (notification.status !== 'draft' || this.sendingNotificationId()) {
      return;
    }

    if (!this.hasPushReadyDevices()) {
      this.sendStatus.set('No push-ready devices are available yet. Ask a user to sign in on mobile with notifications enabled, then try again.');
      return;
    }

    this.sendingNotificationId.set(notification.id);
    this.sendStatus.set(null);
    try {
      const result = await firstValueFrom(this.api.sendAdminNotification(notification.id));
      this.sendStatus.set(
        `Send complete: ${result.sentCount} sent, ${result.failedCount} failed, ${result.targetCount} targeted.`
      );
      await this.reload();
    } catch (error) {
      this.sendStatus.set(this.notificationSendErrorCopy(error));
    } finally {
      this.sendingNotificationId.set(null);
    }
  }

  protected async toggleAdminAccess(user: AdminUserListItem): Promise<void> {
    if (this.adminAccessPending() || this.isSelfAdminRemoval(user)) {
      return;
    }

    this.adminAccessPending.set(true);
    this.adminAccessStatus.set(null);
    try {
      const updated = await firstValueFrom(this.api.updateAdminAccess(user.userId, { admin: !user.admin }));
      this.users.update((users) => users.map((candidate) => (
        candidate.userId === updated.userId ? { ...candidate, admin: updated.admin } : candidate
      )));
      this.adminAccessStatus.set(updated.admin ? 'Admin access enabled.' : 'Admin access removed.');
      await this.reload();
    } catch (error) {
      this.adminAccessStatus.set(this.errorCopy(error));
    } finally {
      this.adminAccessPending.set(false);
    }
  }

  protected adminAccessDisplayName(user: AdminUserAccess | AdminUserListItem): string {
    return user.displayName || user.email || 'Sanctuary user';
  }

  protected isSelfAdminRemoval(user: AdminUserAccess | AdminUserListItem): boolean {
    return user.admin && this.currentProfile()?.userId === user.userId;
  }

  protected hasPushReadyDevices(): boolean {
    return (this.metrics()?.notificationsEnabledDeviceCount ?? 0) > 0;
  }

  protected deliveryReadinessTitle(): string {
    const count = this.metrics()?.notificationsEnabledDeviceCount ?? 0;
    return count > 0 ? 'Delivery ready' : 'No reachable devices yet';
  }

  protected pushReadyDeviceCountLabel(): string {
    return this.formatNumber(this.metrics()?.notificationsEnabledDeviceCount);
  }

  protected deliveryReadinessCopy(): string {
    const count = this.metrics()?.notificationsEnabledDeviceCount ?? 0;
    if (count > 0) {
      return `Drafts can be sent to ${this.formatNumber(count)} device${count === 1 ? '' : 's'} with a valid push token.`;
    }
    return 'Drafts are saved, but sending is unavailable until a mobile app reports a valid push token and notification permission.';
  }

  protected draftSendReadinessCopy(): string {
    const count = this.metrics()?.notificationsEnabledDeviceCount ?? 0;
    if (count > 0) {
      return `${this.formatNumber(count)} reachable`;
    }
    return 'Waiting for recipients';
  }

  protected notificationStatusLabel(notification: AdminNotification): string {
    if (notification.status === 'sent' && notification.targetCount === 0) {
      return 'not sent';
    }
    return notification.status;
  }

  protected notificationTimelineLabel(notification: AdminNotification): string {
    const date = notification.sentAt ?? notification.updatedAt ?? notification.createdAt;
    const prefix = notification.sentAt ? 'FCM handoff' : notification.status === 'draft' ? 'Updated' : 'Created';
    return `${prefix} ${this.formatDate(date)}`;
  }

  protected deliveryStatusLabel(delivery: AdminNotificationDelivery): string {
    if (delivery.status === 'sent') {
      return 'FCM accepted';
    }
    if (delivery.status === 'failed') {
      return 'FCM failed';
    }
    return delivery.status;
  }

  protected deliveryTargetLabel(delivery: AdminNotificationDelivery): string {
    if (delivery.userDeviceId) {
      return `Signed-in device ${delivery.userDeviceId}`;
    }
    if (delivery.anonymousDeviceId) {
      return `Anonymous device ${delivery.anonymousDeviceId}`;
    }
    return delivery.userId ? `User ${delivery.userId}` : 'Unknown target';
  }

  protected deliveryReason(delivery: AdminNotificationDelivery): string {
    if (delivery.failureReason?.trim()) {
      return delivery.failureReason;
    }
    if (delivery.status === 'sent') {
      return 'Firebase accepted the message. Device display is not confirmed by this backend log.';
    }
    return 'No failure reason recorded.';
  }

  protected toggleNotificationHistory(): void {
    this.notificationHistoryExpanded.update((expanded) => !expanded);
  }

  protected displayName(user: AdminUserListItem): string {
    return user.displayName || user.email || 'Sanctuary user';
  }

  protected installOwnerLabel(install: AdminDeviceInstall): string {
    return install.userDisplayName || install.userEmail || 'Anonymous';
  }

  protected installPushLabel(install: AdminDeviceInstall): string {
    if (install.pushReady) {
      return 'Reachable';
    }
    if (!install.notificationsEnabled) {
      return 'Permission off';
    }
    if (install.tokenStatus !== 'valid') {
      return `Token ${install.tokenStatus}`;
    }
    return 'Not active';
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

  protected userReachableDeviceLabel(user: AdminUserListItem): string {
    if (user.deviceCount <= 0) {
      return '0';
    }
    return `${this.formatNumber(user.deviceCount)} ${user.latestPlatform || 'device'}`;
  }

  protected pushReadyPlatformMixLabel(): string {
    const metrics = this.metrics();
    return `${this.formatNumber(metrics?.pushReadyIosDeviceCount)} iOS / ${this.formatNumber(metrics?.pushReadyAndroidDeviceCount)} Android`;
  }

  protected deliverySuccessRate(): string {
    const metrics = this.metrics();
    const sent = metrics?.notificationSentCount ?? 0;
    const failed = metrics?.notificationFailedCount ?? 0;
    return this.formatPercent(sent, sent + failed);
  }

  protected formatNumber(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-US').format(value ?? 0);
  }

  private formatPercent(numerator: number, denominator: number): string {
    if (denominator <= 0) {
      return '0%';
    }
    return `${Math.round((numerator / denominator) * 100)}%`;
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
    if (status === 409) {
      return 'That change is blocked to keep your admin account safe.';
    }
    if (status === 503) {
      return 'Firebase notifications are not configured yet.';
    }
    return 'Sanctuary could not complete that admin request.';
  }

  private notificationSendErrorCopy(error: unknown): string {
    const status = this.statusCode(error);
    if (status === 409) {
      return 'No push-ready devices are available yet. Ask a user to sign in on mobile with notifications enabled, then try again.';
    }
    return this.errorCopy(error);
  }

  private formatEnvironment(rawEnvironment: string | undefined): string {
    const normalized = rawEnvironment?.trim();
    return normalized ? ` · ${normalized.toUpperCase()}` : '';
  }

  private async loadCurrentProfile(): Promise<void> {
    try {
      this.currentProfile.set(await firstValueFrom(this.api.getMe()));
    } catch {
      this.currentProfile.set(null);
    }
  }
}
