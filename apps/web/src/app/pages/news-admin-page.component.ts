import { Component, inject, signal } from '@angular/core';
import { TotpEnrollmentComponent } from './totp-enrollment.component';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { NewsApiService, NewsArticle, newNewsArticle } from '../core/api/news-api.service';

@Component({
  selector: 'app-news-admin-page',
  standalone: true,
  imports: [FormsModule, DatePipe, TotpEnrollmentComponent],
  styleUrl: './news-admin-page.component.scss',
  template: ` <main class="admin-shell">
    <header>
      <div>
        <p class="eyebrow">Sanctuary · Private newsroom</p>
        <h1>News desk</h1>
      </div>
      <a href="/news">View publication ↗</a>
    </header>
    <nav class="admin-navigation" aria-label="Administration areas">
      <a href="/admin/news" aria-current="page">Newsroom</a
      ><span>App management <small>Planned · Firebase &amp; notifications</small></span>
    </nav>
    <p class="notice" role="status">{{ message() }}</p>
    @if (!authorized()) {
      <section class="access-panel">
        <h2>Administrator sign-in</h2>
        <p>
          Use your separate administrator account and authenticator. Only accounts assigned to the
          news-admin group can access articles and publishing tools.
        </p>
        @if (challengeId) {
          <form (ngSubmit)="verifyCode()">
            @if (enrollmentSecret) {
              <app-totp-enrollment [secret]="enrollmentSecret" [account]="email" />
            }
            <label
              >Authenticator code<input
                name="mfaCode"
                [(ngModel)]="mfaCode"
                inputmode="numeric"
                autocomplete="one-time-code"
                pattern="[0-9]{6}"
                maxlength="6"
                required
            /></label>
            <button [disabled]="busy()">
              {{ enrollmentSecret ? 'Verify authenticator' : 'Verify and sign in' }}
            </button>
          </form>
          <button type="button" class="secondary" (click)="cancelMfa()">Start sign-in again</button>
        } @else {
          <form (ngSubmit)="signIn()">
            <label
              >Email<input
                name="email"
                type="email"
                autocomplete="username"
                [(ngModel)]="email"
                required /></label
            ><label
              >Password<input
                name="password"
                type="password"
                autocomplete="current-password"
                [(ngModel)]="password"
                required /></label
            ><button [disabled]="busy()">Sign in</button>
          </form>
        }
        <button type="button" class="secondary" [disabled]="busy()" (click)="load()">
          Check existing session
        </button>
      </section>
    } @else {
      <div class="toolbar">
        <button type="button" class="secondary" [disabled]="busy()" (click)="toggleTrash()">
          {{ showTrash() ? 'Back to articles' : 'Trash' }}
        </button>
        <button type="button" [disabled]="busy()" (click)="startNew()">+ New article</button
        ><button class="secondary" type="button" [disabled]="busy()" (click)="refresh()">
          Refresh list</button
        ><button class="secondary" type="button" (click)="signOut()">Sign out</button>
      </div>
      <div class="workspace">
        <aside>
          <h2>{{ showTrash() ? 'Trash' : 'Articles' }}</h2>
          @for (article of articles(); track article.id) {
            <button
              type="button"
              class="article-row"
              [disabled]="busy()"
              [class.active]="draft?.id === article.id"
              (click)="showTrash() ? null : edit(article)"
            >
              <strong>{{ article.title }}</strong
              ><span
                >{{ article.status }} · {{ article.category }} ·
                {{ showTrash() ? 'Trashed' : placementLabel(article.placement) }}
              </span>
            </button>
            @if (showTrash()) {
              <button
                type="button"
                class="secondary"
                [disabled]="busy()"
                (click)="restoreArticle(article)"
              >
                Restore as draft
              </button>
            }
          } @empty {
            <p>
              {{ showTrash() ? 'Trash is empty.' : 'No articles yet. Create your first draft.' }}
            </p>
          }
        </aside>
        <section class="editor">
          @if (draft; as article) {
            <h2>{{ article.id ? 'Edit article' : 'New article' }}</h2>
            <form (ngSubmit)="save()">
              <label
                >Headline<input name="title" [(ngModel)]="article.title" maxlength="200" required
              /></label>
              <div class="field-pair">
                <label
                  >Category<select name="category" [(ngModel)]="article.category">
                    <option>Vatican</option>
                    <option>U.S. Church</option>
                  </select></label
                ><label
                  >Author<input name="author" [(ngModel)]="article.author" maxlength="150" required
                /></label>
              </div>
              <label
                >Summary<textarea
                  name="summary"
                  [(ngModel)]="article.summary"
                  maxlength="600"
                  rows="3"
                  required
                ></textarea>
              </label>
              <label
                >Article text<textarea
                  name="body"
                  [(ngModel)]="article.body"
                  maxlength="100000"
                  rows="14"
                  required
                ></textarea></label
              ><small>Plain text. Separate paragraphs with a blank line.</small>
              <label
                >Photograph<input
                  type="file"
                  accept="image/jpeg,image/png"
                  (change)="upload($event)"
                  [disabled]="busy()" /></label
              ><small
                >JPEG or PNG, up to 5 MB. Upload only photographs you have permission to
                publish.</small
              >
              @if (article.imageId) {
                <img
                  class="image-preview"
                  [src]="api.image(article.imageId, true)"
                  [alt]="article.imageAlt"
                /><button type="button" class="secondary" (click)="article.imageId = null">
                  Remove photo from article
                </button>
              }
              <label
                >Photo description (alt text)<input
                  name="alt"
                  [(ngModel)]="article.imageAlt"
                  maxlength="300" /></label
              ><label
                >Caption<input
                  name="caption"
                  [(ngModel)]="article.imageCaption"
                  maxlength="1000" /></label
              ><label
                >Photo credit / permission<input
                  name="credit"
                  [(ngModel)]="article.imageCredit"
                  maxlength="500"
              /></label>
              <label
                >Sources<textarea
                  name="sources"
                  [(ngModel)]="article.sources"
                  maxlength="10000"
                  rows="4"
                  placeholder="One source or HTTPS link per line"
                ></textarea>
              </label>
              <div class="field-pair">
                <label
                  >Status<select
                    name="status"
                    [(ngModel)]="article.status"
                    (ngModelChange)="statusChanged(article)"
                  >
                    <option value="DRAFT">Draft</option>
                    <option value="PUBLISHED">Published</option>
                    <option value="SCHEDULED">Scheduled</option>
                  </select></label
                >
                @if (article.status === 'SCHEDULED') {
                  <label
                    >Publish at (your local time)<input
                      name="publishAt"
                      type="datetime-local"
                      [(ngModel)]="scheduleLocal"
                      required
                  /></label>
                }
              </div>
              <fieldset class="placement-panel">
                <legend>Where should this article appear?</legend>
                <p>
                  These positions control the Latest homepage. Category tabs filter and arrange
                  their stories automatically.
                </p>
                <div class="layout-guide" aria-label="Homepage layout guide">
                  <div class="guide-main">
                    Main story<br /><small>Large feature on the left</small>
                  </div>
                  <div class="guide-side">
                    <div>Sidebar story 1<br /><small>Upper right</small></div>
                    <div>Sidebar story 2<br /><small>Lower right</small></div>
                  </div>
                  <div class="guide-more">More stories · Below the main section</div>
                </div>
                <label
                  >Homepage position<select
                    name="placement"
                    [(ngModel)]="article.placement"
                    [disabled]="article.status === 'SCHEDULED'"
                  >
                    <option value="LEAD">Main story — large feature on the left</option>
                    <option value="SIDEBAR_1">Sidebar story 1 — upper right</option>
                    <option value="SIDEBAR_2">Sidebar story 2 — lower right</option>
                    <option value="MORE">More stories — below the main section</option>
                  </select></label
                >
                <p>
                  {{
                    article.status === 'DRAFT'
                      ? 'This is the planned position. Saving a draft does not change the live homepage.'
                      : article.status === 'SCHEDULED'
                        ? 'Scheduled articles appear in More stories when due. You can move them to a featured position after publication.'
                        : 'This position takes effect when you save.'
                  }}
                </p>
                @if (article.placement !== 'MORE' && article.status !== 'SCHEDULED') {
                  <p>
                    There is one published article per featured position. Publishing here moves the
                    previous article in this position to More stories.
                  </p>
                }
              </fieldset>
              <p class="warning">
                {{
                  article.status === 'PUBLISHED'
                    ? 'Saving will make this article public immediately.'
                    : article.status === 'SCHEDULED'
                      ? 'This article becomes public at the scheduled time.'
                      : 'Drafts are visible only to news administrators.'
                }}
              </p>
              <div class="save-feedback" aria-live="polite" aria-atomic="true">
                @if (saveFeedback(); as receipt) {
                  <div class="save-confirmation">
                    <strong>{{
                      receipt.status === 'DRAFT'
                        ? 'Draft saved. Your article is private.'
                        : receipt.status === 'PUBLISHED'
                          ? 'Published successfully. Your article is live.'
                          : 'Publication scheduled.'
                    }}</strong>
                    @if (receipt.status === 'DRAFT') {
                      <p>
                        When you’re ready, change <strong>Status</strong> to
                        <strong>Published</strong>, then click <strong>Save and publish</strong>.
                      </p>
                    }
                    @if (receipt.status === 'PUBLISHED') {
                      <p>Future edits can be saved with <strong>Save changes</strong>.</p>
                      <a [href]="'/news?article=' + receipt.id" target="_blank" rel="noopener"
                        >View published article ↗</a
                      >
                    }
                    @if (receipt.status === 'SCHEDULED') {
                      <p>Your article will go live {{ receipt.publishAt | date: 'medium' }}.</p>
                    }
                    <p>
                      Homepage position: <strong>{{ placementLabel(receipt.placement) }}</strong
                      >{{ receipt.status === 'DRAFT' ? ' (when published)' : '' }}
                    </p>
                    <small>Saved {{ receipt.updatedAt | date: 'shortTime' }}</small>
                  </div>
                }
                @if (saveError()) {
                  <p class="save-error" role="alert">{{ saveError() }}</p>
                }
              </div>
              <div class="toolbar">
                <button [disabled]="busy()">
                  {{
                    saving()
                      ? 'Saving…'
                      : article.status === 'PUBLISHED'
                        ? savedStatus() === 'PUBLISHED'
                          ? 'Save changes'
                          : 'Save and publish'
                        : article.status === 'SCHEDULED'
                          ? 'Save schedule'
                          : 'Save draft'
                  }}</button
                ><button type="button" class="secondary" (click)="preview.set(!preview())">
                  {{ preview() ? 'Hide' : 'Show' }} preview
                </button>
                <button
                  type="button"
                  class="secondary"
                  [disabled]="busy()"
                  (click)="backToArticles()"
                >
                  Back to articles
                </button>
              </div>
            </form>
            @if (article.id) {
              <button
                type="button"
                class="danger"
                [disabled]="busy()"
                (click)="trashArticle(article)"
              >
                Move to trash</button
              ><small class="trash-help"
                >Removes this article from the website. You can restore it from Trash as a private
                draft.</small
              >
            }
            @if (preview()) {
              <article class="reading-preview">
                <p>{{ article.category }} · Unpublished editor preview</p>
                <h2>{{ article.title }}</h2>
                <p>{{ article.summary }}</p>
                <p>By {{ article.author }}</p>
                <div class="body-text">{{ article.body }}</div>
              </article>
            }
            @if (article.id) {
              <section class="corrections">
                <h3>Corrections</h3>
                <p>Corrections are appended permanently and appear with the published article.</p>
                @for (c of article.corrections; track $index) {
                  <p>{{ c.createdAt | date: 'medium' }} — {{ c.note }}</p>
                }
                <label
                  >Correction note<textarea
                    [(ngModel)]="correctionNote"
                    maxlength="2000"
                    rows="3"
                  ></textarea></label
                ><button
                  type="button"
                  class="secondary"
                  [disabled]="busy() || !correctionNote.trim()"
                  (click)="addCorrection()"
                >
                  Record correction
                </button>
              </section>
            }
          } @else {
            <p>
              {{
                showTrash()
                  ? 'Choose Restore as draft beside an article to bring it back. Restored articles remain private until you publish them again.'
                  : 'Select an article or create a new draft.'
              }}
            </p>
          }
        </section>
      </div>
    }
  </main>`,
})
export class NewsAdminPageComponent {
  readonly api = inject(NewsApiService);
  readonly authorized = signal(false);
  readonly busy = signal(false);
  readonly showTrash = signal(false);
  placementLabel(placement: string | undefined) {
    return (
      (
        {
          LEAD: 'Main story',
          SIDEBAR_1: 'Sidebar story 1',
          SIDEBAR_2: 'Sidebar story 2',
          MORE: 'More stories',
        } as Record<string, string>
      )[placement || 'MORE'] || 'More stories'
    );
  }
  statusChanged(article: NewsArticle) {
    if (article.status === 'SCHEDULED') article.placement = 'MORE';
  }
  async toggleTrash() {
    if (
      this.draft &&
      this.editorSnapshot() !== this.savedSnapshot &&
      !confirm('Discard unsaved changes?')
    )
      return;
    this.draft = null;
    this.showTrash.set(!this.showTrash());
    await this.load();
  }
  async trashArticle(article: NewsArticle) {
    if (
      !confirm(
        'Move this article to trash? It will be removed from the public website. Unsaved edits will not be saved. You can restore the saved article later.',
      )
    )
      return;
    this.busy.set(true);
    try {
      await firstValueFrom(this.api.trash(article));
      this.draft = null;
      this.saveFeedback.set(null);
      this.showTrash.set(false);
      this.articles.set(await firstValueFrom(this.api.list(true)));
      this.message.set(
        'Article moved to trash and removed from the website. Open Trash to restore it.',
      );
    } catch (error) {
      this.fail(error);
    } finally {
      this.busy.set(false);
    }
  }
  async restoreArticle(article: NewsArticle) {
    this.busy.set(true);
    try {
      await firstValueFrom(this.api.restore(article));
      this.articles.set(await firstValueFrom(this.api.list(true, true)));
      this.message.set(
        'Article restored as a private draft in More stories. Open Back to articles to edit and publish it.',
      );
    } catch (error) {
      this.fail(error);
    } finally {
      this.busy.set(false);
    }
  }
  readonly saveFeedback = signal<NewsArticle | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly savedStatus = signal<string | null>(null);
  readonly saving = signal(false);
  private savedSnapshot = '';
  private editorSnapshot() {
    return JSON.stringify({
      article: this.draft,
      schedule: this.draft?.status === 'SCHEDULED' ? this.scheduleLocal : '',
    });
  }
  backToArticles() {
    if (
      this.draft &&
      this.editorSnapshot() !== this.savedSnapshot &&
      !confirm('Discard unsaved changes and return to articles?')
    )
      return;
    this.draft = null;
    this.preview.set(false);
    this.saveFeedback.set(null);
    this.saveError.set(null);
  }
  readonly message = signal('Checking administrator access…');
  readonly articles = signal<NewsArticle[]>([]);
  readonly preview = signal(false);
  email = '';
  password = '';
  draft: NewsArticle | null = null;
  scheduleLocal = '';
  correctionNote = '';
  constructor() {
    void this.load();
  }
  async load() {
    this.busy.set(true);
    try {
      this.articles.set(await firstValueFrom(this.api.list(true, this.showTrash())));
      this.authorized.set(true);
      this.message.set('Private newsroom. Drafts are never shown on the public page.');
    } catch (e) {
      this.fail(e);
    } finally {
      this.busy.set(false);
    }
  }
  async refresh() {
    if (this.draft && !confirm('Refresh the list? Unsaved changes will be discarded.')) return;
    this.draft = null;
    await this.load();
  }
  challengeId: string | null = null;
  enrollmentSecret: string | null = null;
  mfaCode = '';
  cancelMfa() {
    this.challengeId = null;
    this.enrollmentSecret = null;
    this.mfaCode = '';
    this.password = '';
  }
  async signIn() {
    if (!this.email.trim() || !this.password) {
      this.message.set('Enter your administrator email and password.');
      return;
    }
    this.busy.set(true);
    try {
      const result = await firstValueFrom(this.api.login(this.email, this.password));
      this.password = '';
      this.challengeId = result.challengeId;
      this.enrollmentSecret = result.secret;
      this.message.set(result.message);
    } catch (error) {
      this.password = '';
      this.fail(error);
    } finally {
      this.busy.set(false);
    }
  }
  async verifyCode() {
    if (!this.challengeId) return;
    if (!/^[0-9]{6}$/.test(this.mfaCode)) {
      this.message.set('Enter the six-digit code from your authenticator.');
      return;
    }
    this.busy.set(true);
    try {
      const result = await firstValueFrom(this.api.verify(this.challengeId, this.mfaCode));
      this.mfaCode = '';
      this.message.set(result.message);
      if (result.state === 'AUTHENTICATED') {
        this.cancelMfa();
        await this.load();
      } else if (result.state === 'SIGN_IN') {
        this.cancelMfa();
      }
    } catch (error) {
      this.mfaCode = '';
      this.fail(error);
    } finally {
      this.busy.set(false);
    }
  }
  async signOut() {
    try {
      await firstValueFrom(this.api.logout());
    } finally {
      this.authorized.set(false);
      this.articles.set([]);
      this.draft = null;
      this.cancelMfa();
      this.message.set('Signed out of the administrator session.');
    }
  }
  startNew() {
    if (this.draft && !confirm('Discard unsaved changes and create a new article?')) return;
    this.showTrash.set(false);
    this.draft = newNewsArticle();
    this.savedStatus.set(null);
    this.saveFeedback.set(null);
    this.saveError.set(null);
    this.savedSnapshot = this.editorSnapshot();
    this.preview.set(false);
    this.scheduleLocal = '';
  }
  edit(article: NewsArticle) {
    if (this.draft && !confirm('Discard unsaved changes and open this article?')) return;
    this.draft = structuredClone(article);
    this.savedStatus.set(article.status);
    this.saveFeedback.set(null);
    this.saveError.set(null);
    this.savedSnapshot = this.editorSnapshot();
    this.preview.set(false);
    this.correctionNote = '';
    this.scheduleLocal = article.publishAt ? this.localDate(article.publishAt) : '';
    this.savedSnapshot = this.editorSnapshot();
  }
  async save() {
    if (!this.draft || this.busy()) return;
    this.busy.set(true);
    this.saving.set(true);
    this.saveFeedback.set(null);
    this.saveError.set(null);
    try {
      const article = { ...this.draft };
      article.publishAt =
        article.status === 'SCHEDULED'
          ? new Date(this.scheduleLocal).toISOString()
          : article.status === 'PUBLISHED'
            ? article.publishAt
            : null;
      const saved = await firstValueFrom(this.api.save(article));
      this.draft = saved;
      this.savedSnapshot = this.editorSnapshot();
      this.savedStatus.set(saved.status);
      this.saveFeedback.set(saved);
      this.articles.update((articles) => [saved, ...articles.filter((a) => a.id !== saved.id)]);
      this.message.set(
        saved.status === 'DRAFT'
          ? 'Draft saved. Your article is private.'
          : saved.status === 'PUBLISHED'
            ? 'Published successfully. Your article is live.'
            : 'Publication scheduled.',
      );
      // Saving succeeded independently of list refresh. A refresh failure must not look like a failed save.
      try {
        this.articles.set(await firstValueFrom(this.api.list(true)));
      } catch {
        /* Retain the saved article and receipt. */
      }
    } catch (error) {
      this.fail(error);
      this.saveError.set(this.message());
    } finally {
      this.busy.set(false);
      this.saving.set(false);
    }
  }
  async upload(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.draft) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5242880) {
      this.message.set('Choose a JPEG or PNG image up to 5 MB.');
      return;
    }
    this.busy.set(true);
    try {
      const result = await firstValueFrom(this.api.upload(file));
      if (this.draft) this.draft.imageId = result.id;
      this.message.set('Photo uploaded. Save the article to attach it.');
    } catch (e) {
      this.fail(e);
    } finally {
      this.busy.set(false);
      input.value = '';
    }
  }
  async addCorrection() {
    if (!this.draft?.id) return;
    if (
      !confirm(
        'Record this permanent correction? The editor will reload the saved article; save any article edits first.',
      )
    )
      return;
    this.busy.set(true);
    try {
      this.draft = await firstValueFrom(this.api.correction(this.draft.id, this.correctionNote));
      this.correctionNote = '';
      this.articles.set(await firstValueFrom(this.api.list(true)));
      this.message.set('Correction recorded.');
    } catch (e) {
      this.fail(e);
    } finally {
      this.busy.set(false);
    }
  }
  private localDate(value: string) {
    const date = new Date(value);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  }
  private fail(error: unknown) {
    const e = error as { status?: number; error?: { message?: string; detail?: string } };
    if (e.status === 401 || e.status === 403) {
      this.authorized.set(false);
      this.articles.set([]);
      this.cancelMfa();
      this.message.set(
        this.draft
          ? 'Your session ended or access changed. Your unsaved article is kept here. Sign in again to continue.'
          : e.error?.message ||
              e.error?.detail ||
              'Access restricted. Sign in with your administrator account and authenticator.',
      );
    } else if (e.status === 409) {
      this.message.set(
        'This article changed in another session. Copy your edits, refresh the list, and reopen it before saving.',
      );
    } else {
      this.message.set(
        e.status === 0
          ? 'News backend is unavailable. Administrator access remains locked.'
          : e.error?.detail ||
              e.error?.message ||
              'Could not complete this action. Check your connection and article fields.',
      );
    }
  }
}
