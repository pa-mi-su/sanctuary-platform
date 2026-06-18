import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import {
  AdminNotification,
  AdminUserListItem,
  AdminUserMetrics,
  SanctuaryApiService,
} from '../../core/api/sanctuary-api.service';

type AdminLoadState = 'idle' | 'loading' | 'ready' | 'forbidden' | 'error';

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
          <p>Monitor users and prepare notification drafts for future FCM delivery.</p>
        </div>
        <button type="button" (click)="reload()" [disabled]="state() === 'loading'">Refresh</button>
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
        <section class="metrics-grid" aria-label="User and device metrics">
          @for (metric of metricCards(); track metric.label) {
            <article class="metric-card">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </article>
          }
        </section>

        <section class="admin-grid">
          <article class="admin-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Notifications</p>
                <h2>Create Draft</h2>
              </div>
              <span class="status-pill">Send disabled in MVP</span>
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
              @for (notification of notifications(); track notification.id) {
                <article class="history-row">
                  <div>
                    <strong>{{ notification.title }}</strong>
                    <p>{{ notification.message }}</p>
                  </div>
                  <span>{{ notification.status }}</span>
                </article>
              } @empty {
                <p class="empty-copy">No notification drafts yet.</p>
              }
            </div>
          </article>
        </section>

        <section class="admin-panel users-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Users</p>
              <h2>Recent Accounts</h2>
            </div>
            <span>{{ users().length }} loaded</span>
          </div>

          <div class="user-table" role="table" aria-label="Recent users">
            <div class="table-row table-row--header" role="row">
              <span>User</span>
              <span>Language</span>
              <span>Devices</span>
              <span>Latest App</span>
              <span>Last Device Seen</span>
              <span>Last Sign In</span>
            </div>
            @for (user of users(); track user.userId) {
              <div class="table-row" role="row">
                <span>
                  <strong>{{ displayName(user) }}</strong>
                  <small>{{ user.email || user.userId }}</small>
                </span>
                <span>{{ user.latestDeviceLanguage || user.preferredLanguage || 'unknown' }}</span>
                <span>{{ user.deviceCount }} {{ user.latestPlatform || '' }}</span>
                <span>{{ user.latestAppVersion || 'unknown' }}</span>
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

  protected readonly state = signal<AdminLoadState>('idle');
  protected readonly errorMessage = signal('Sanctuary could not load admin data.');
  protected readonly metrics = signal<AdminUserMetrics | null>(null);
  protected readonly users = signal<AdminUserListItem[]>([]);
  protected readonly notifications = signal<AdminNotification[]>([]);
  protected readonly draftPending = signal(false);
  protected readonly draftStatus = signal<string | null>(null);

  protected draftTitle = '';
  protected draftMessage = '';

  protected readonly metricCards = computed(() => {
    const metrics = this.metrics();
    return [
      { label: 'Users', value: metrics?.totalUsers ?? 0 },
      { label: 'Active Today', value: metrics?.activeUsersToday ?? 0 },
      { label: 'Active 7 Days', value: metrics?.activeUsers7Days ?? 0 },
      { label: 'Active 30 Days', value: metrics?.activeUsers30Days ?? 0 },
      { label: 'Devices', value: metrics?.deviceCount ?? 0 },
      { label: 'Devices 7 Days', value: metrics?.activeDevices7Days ?? 0 },
      { label: 'Devices 30 Days', value: metrics?.activeDevices30Days ?? 0 },
      { label: 'iOS', value: metrics?.iosDeviceCount ?? 0 },
      { label: 'Android', value: metrics?.androidDeviceCount ?? 0 },
      { label: 'English', value: metrics?.englishDeviceCount ?? 0 },
      { label: 'Spanish', value: metrics?.spanishDeviceCount ?? 0 },
      { label: 'Polish', value: metrics?.polishDeviceCount ?? 0 },
      { label: 'Notifications On', value: metrics?.notificationsEnabledDeviceCount ?? 0 },
      { label: 'Invalid Tokens', value: metrics?.invalidTokenCount ?? 0 },
      { label: 'Unknown Version', value: metrics?.unknownAppVersionDeviceCount ?? 0 },
    ];
  });

  constructor() {
    void this.reload();
  }

  protected async reload(): Promise<void> {
    this.state.set('loading');
    this.errorMessage.set('Sanctuary could not load admin data.');

    try {
      const [usersResponse, notifications] = await Promise.all([
        firstValueFrom(this.api.listAdminUsers(100)),
        firstValueFrom(this.api.listAdminNotifications(50)),
      ]);
      this.metrics.set(usersResponse.metrics);
      this.users.set(usersResponse.users);
      this.notifications.set(notifications);
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

  protected displayName(user: AdminUserListItem): string {
    return user.displayName || user.email || 'Sanctuary user';
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
    return 'Sanctuary could not complete that admin request.';
  }
}
