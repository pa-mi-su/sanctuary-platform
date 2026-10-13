import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { NewsAdminPageComponent } from './news-admin-page.component';
import { newNewsArticle } from '../core/api/news-api.service';
import { NewsApiService } from '../core/api/news-api.service';
import { SanctuaryAuthService } from '../core/auth/sanctuary-auth.service';

describe('News administrator access', () => {
  it('does not expose editor controls when the server denies access', async () => {
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [
        {
          provide: NewsApiService,
          useValue: { list: () => throwError(() => new HttpErrorResponse({ status: 403 })) },
        },
        { provide: SanctuaryAuthService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.authorized()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Administrator sign-in');
    expect(fixture.nativeElement.textContent).not.toContain('New article');
    expect(fixture.nativeElement.querySelector('.editor')).toBeNull();
  });
  it('opens an empty editor only after the protected API grants access', async () => {
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [
        { provide: NewsApiService, useValue: { list: () => of([]) } },
        { provide: SanctuaryAuthService, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.authorized()).toBe(true);
    const button = [...fixture.nativeElement.querySelectorAll('button')].find((button: any) =>
      button.textContent.includes('New article'),
    ) as HTMLButtonElement;
    button.click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('input[name="title"]')).not.toBeNull();
    expect(fixture.componentInstance.draft?.status).toBe('DRAFT');
  });
  it('explains an expired MFA challenge and returns to sign-in without re-enrollment', async () => {
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [
        {
          provide: NewsApiService,
          useValue: {
            list: () => throwError(() => new HttpErrorResponse({ status: 401 })),
            login: () =>
              of({
                state: 'CODE',
                challengeId: 'test-challenge',
                secret: null,
                message: 'Enter your authenticator code.',
              }),
            verify: () =>
              throwError(
                () =>
                  new HttpErrorResponse({
                    status: 401,
                    error: { message: 'The sign-in session expired. Start sign-in again.' },
                  }),
              ),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    const page = fixture.componentInstance;
    page.email = 'admin@example.org';
    page.password = 'test-password';
    await page.signIn();
    expect(page.challengeId).toBe('test-challenge');
    page.mfaCode = '123456';
    await page.verifyCode();
    expect(page.message()).toContain('session expired');
    expect(page.challengeId).toBeNull();
    expect(page.enrollmentSecret).toBeNull();
    expect(page.authorized()).toBe(false);
  });
  it.each(['DRAFT', 'PUBLISHED'])(
    'shows an explicit %s receipt even if list refresh fails',
    async (status) => {
      const saved = {
        ...newNewsArticle(),
        id: 'saved-id',
        title: 'Test headline',
        summary: 'Summary',
        body: 'Test body',
        author: 'Reporter',
        status,
        updatedAt: '2026-10-13T18:00:00Z',
      };
      const list = vi
        .fn()
        .mockReturnValueOnce(of([]))
        .mockReturnValue(throwError(() => new Error('List refresh failed')));
      TestBed.configureTestingModule({
        imports: [NewsAdminPageComponent],
        providers: [{ provide: NewsApiService, useValue: { list, save: () => of(saved) } }],
      });
      const fixture = TestBed.createComponent(NewsAdminPageComponent);
      await fixture.whenStable();
      const page = fixture.componentInstance;
      page.draft = { ...saved, id: null };
      await page.save();
      await fixture.whenStable();
      expect(page.saveFeedback()?.status).toBe(status);
      expect(page.articles()).toContainEqual(saved);
      const receipt = fixture.nativeElement.querySelector('.save-confirmation');
      expect(receipt.textContent).toContain(
        status === 'DRAFT'
          ? 'Draft saved. Your article is private.'
          : 'Published successfully. Your article is live.',
      );
      if (status === 'PUBLISHED') {
        expect(receipt.querySelector('a').getAttribute('href')).toBe('/news?article=saved-id');
        expect(
          [...fixture.nativeElement.querySelectorAll('button')].some(
            (b: any) => b.textContent.trim() === 'Save changes',
          ),
        ).toBe(true);
      } else {
        expect(receipt.textContent).toContain('Save and publish');
      }
      page.backToArticles();
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('.editor form')).toBeNull();
    },
  );
  it('shows save errors beside the controls without claiming success', async () => {
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [
        {
          provide: NewsApiService,
          useValue: {
            list: () => of([]),
            save: () =>
              throwError(
                () =>
                  new HttpErrorResponse({
                    status: 400,
                    error: { message: 'Photo credit is required.' },
                  }),
              ),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    const page = fixture.componentInstance;
    page.draft = { ...newNewsArticle(), status: 'PUBLISHED' };
    await page.save();
    await fixture.whenStable();
    expect(page.saveFeedback()).toBeNull();
    expect(fixture.nativeElement.querySelector('.save-error').textContent).toContain(
      'Photo credit is required.',
    );
  });
  it('provides recoverable trash controls and explicit homepage positions', async () => {
    const article = {
      ...newNewsArticle(),
      id: 'test-id',
      title: 'Test article',
      placement: 'LEAD',
    };
    const list = vi.fn().mockReturnValue(of([article]));
    const trash = vi.fn().mockReturnValue(of({ ...article, deletedAt: '2026-10-13T18:00:00Z' }));
    const restore = vi.fn().mockReturnValue(of({ ...article, placement: 'MORE' }));
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [{ provide: NewsApiService, useValue: { list, trash, restore } }],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    const page = fixture.componentInstance;
    fixture.nativeElement.querySelector('.article-row').click();
    await fixture.whenStable();
    const choices = [
      ...fixture.nativeElement.querySelectorAll('select[name="placement"] option'),
    ].map((option: any) => option.value);
    expect(choices).toEqual(['LEAD', 'SIDEBAR_1', 'SIDEBAR_2', 'MORE']);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await page.trashArticle(article);
    expect(trash).toHaveBeenCalledWith(article);
    expect(page.draft).toBeNull();
    expect(page.message()).toContain('moved to trash');
    await page.toggleTrash();
    expect(list).toHaveBeenLastCalledWith(true, true);
    await page.restoreArticle(article);
    expect(restore).toHaveBeenCalledWith(article);
    expect(page.message()).toContain('private draft');
    vi.restoreAllMocks();
  });
  it('preserves unsaved article text when authentication expires during save', async () => {
    TestBed.configureTestingModule({
      imports: [NewsAdminPageComponent],
      providers: [
        {
          provide: NewsApiService,
          useValue: {
            list: () => of([]),
            save: () => throwError(() => new HttpErrorResponse({ status: 401 })),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(NewsAdminPageComponent);
    await fixture.whenStable();
    const page = fixture.componentInstance;
    page.draft = { ...newNewsArticle(), body: 'Unsaved original reporting' };
    await page.save();
    expect(page.authorized()).toBe(false);
    expect(page.draft?.body).toBe('Unsaved original reporting');
    expect(page.message()).toContain('unsaved article is kept');
    await page.load();
    expect(page.authorized()).toBe(true);
    expect(page.draft?.body).toBe('Unsaved original reporting');
  });
});
