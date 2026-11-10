import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminPageComponent, ADMIN_IDLE_MS } from './admin-page.component';
import { ADMIN_QR_ENCODER, authenticatorUri } from './admin-qr';
import { ADMIN_IMAGE_PREPARER } from './admin-images';
import { SANCTUARY_API_BASE_URL } from '../core/api/sanctuary-api.config';

const base = 'https://api.example.org/admin-api';
const info = () => ({
  email: 'owner@example.test',
  expiresAt: new Date(Date.now() + 24 * 60 * 60_000).toISOString(),
  idleHours: 8,
  maximumHours: 24,
});
const content = () => ({
  values: {
    id: 'saint-1',
    slug: 'saint-1',
    name_en: 'Saint Example',
    biography_en: 'Original biography',
    image_url: 'https://example.org/old.jpg',
  },
  version: 'v1',
  links: { tags: [], intentions: ['7'], patronages: [] },
  related: { intentions: [{ id: '7', title: 'Peace' }], patronages: [] },
});

describe('Admin workspace behavior', () => {
  let component: AdminPageComponent;
  let http: HttpTestingController;
  let encoder: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    encoder = vi.fn().mockResolvedValue('data:image/png;base64,synthetic');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SANCTUARY_API_BASE_URL, useValue: 'https://api.example.org' },
        { provide: ADMIN_QR_ENCODER, useValue: encoder },
        { provide: ADMIN_IMAGE_PREPARER, useValue: (file: File) => Promise.resolve(file) },
      ],
    });
    component = TestBed.runInInjectionContext(() => new AdminPageComponent());
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    component.ngOnDestroy();
    http.verify();
    vi.useRealTimers();
  });
  async function edit() {
    component.entity = 'saints';
    const pending = component.edit({ id: 'saint-1' });
    await Promise.resolve();
    http.expectOne(base + '/content/saints/saint-1').flush(content());
    await pending;
  }
  it('renders QR locally with issuer, correct label and a synthetic secret', async () => {
    component.email = 'owner@example.test';
    component.password = 'not-a-real-password';
    const pending = component.login();
    const request = http.expectOne(base + '/login');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ challenge: 'challenge', secret: 'JBSWY3DPEHPK3PXP', mode: 'enroll' });
    await pending;
    const uri = new URL(encoder.mock.calls[0][0]);
    expect(uri.protocol).toBe('otpauth:');
    expect(uri.searchParams.get('secret')).toBe('JBSWY3DPEHPK3PXP');
    expect(uri.searchParams.get('issuer')).toBe('Sanctuary');
    expect(decodeURIComponent(uri.pathname)).toContain('owner@example.test');
    expect(component.qrCode()).toContain('data:image/png');
    expect(component.password).toBe('');
    expect(component.signedIn()).toBe(false);
    component.restart();
    expect(component.qrCode()).toBe('');
    expect(component.secret).toBe('');
  });
  it('checks all password requirements and the confirmation independently', () => {
    component.newPassword = 'short';
    expect(component.passwordReady()).toBe(false);
    expect(
      component.passwordChecks().find((check) => check.label === 'A lowercase letter')?.met,
    ).toBe(true);
    component.newPassword = 'ExamplePassword123!';
    component.confirmPassword = 'different';
    expect(component.passwordReady()).toBe(false);
    component.confirmPassword = component.newPassword;
    expect(component.passwordReady()).toBe(true);
  });
  it('does not warn when an existing item was only opened', async () => {
    await edit();
    expect(component.isDirty()).toBe(false);
    const pending = component.navigate('audit');
    await Promise.resolve();
    expect(component.dialog).toBeNull();
    http.expectOne(base + '/audit?page=0').flush({ items: [], hasMore: false });
    await pending;
  });
  it('warns only for real changes, preserves work on cancel, and recognizes a reverted edit', async () => {
    await edit();
    component.values!['biography_en'] = 'Changed';
    expect(component.isDirty()).toBe(true);
    const pending = component.navigate('audit');
    expect(component.dialog).toBe('discard');
    component.decideDiscard(false);
    await pending;
    expect(component.values!['biography_en']).toBe('Changed');
    component.values!['biography_en'] = 'Original biography';
    expect(component.isDirty()).toBe(false);
  });
  it('does not treat tag/relationship ordering or a shared display-name update as edits', async () => {
    await edit();
    component.terms['intentions']['7']['title'] = 'Corrected name';
    expect(component.isDirty()).toBe(false);
    component.unlink('intentions', '7');
    expect(component.isDirty()).toBe(true);
    component.links['intentions'] = ['7'];
    expect(component.isDirty()).toBe(false);
  });
  it('keeps picker changes local until Apply and never deletes a shared record on unlink', async () => {
    await edit();
    const pending = component.openPicker('intentions');
    http.expectOne(base + '/content/intentions?q=&page=0').flush({
      items: [
        { id: '7', title: 'Peace' },
        { id: '8', title: 'Family' },
      ],
      hasMore: false,
    });
    await pending;
    component.togglePicker('8', true);
    expect(component.links['intentions']).toEqual(['7']);
    component.applyTerms();
    expect(component.links['intentions']).toEqual(['7', '8']);
    component.unlink('intentions', '7');
    expect(component.links['intentions']).toEqual(['8']);
    expect(component.terms['intentions']['7']['title']).toBe('Peace');
  });
  it('creates a shared term without publishing the parent or losing existing selections', async () => {
    await edit();
    component.pickerKind = 'intentions';
    component.pickerSelection = ['7'];
    component.dialog = 'terms';
    component.termDraft = { label_en: 'Teachers', label_es: 'Docentes', label_pl: 'Nauczyciele' };
    component.termReason = 'Add teachers intention';
    const pending = component.saveTerm();
    const request = http.expectOne(base + '/terms/intentions');
    expect(request.request.body.reason).toBe('Add teachers intention');
    request.flush({ values: { id: 9, label_en: 'Teachers' }, version: 'term-v1' });
    await pending;
    expect(component.links['intentions']).toEqual(['7']);
    expect(component.pickerSelection).toEqual(['7', '9']);
    component.applyTerms();
    expect(component.links['intentions']).toEqual(['7', '9']);
  });
  it('uploads using multipart, previews the replacement and leaves publication explicit', async () => {
    await edit();
    const file = new File(['synthetic'], 'portrait.png', { type: 'image/png' });
    const input = { files: [file], value: 'portrait.png' };
    const pending = component.uploadImage({ target: input } as unknown as Event);
    await Promise.resolve();
    const request = http.expectOne(base + '/images');
    expect(request.request.body instanceof FormData).toBe(true);
    request.flush({ path: '/content/images/synthetic-id' });
    await pending;
    expect(component.values!['image_url']).toBe(
      'https://api.example.org/content/images/synthetic-id',
    );
    expect(component.isDirty()).toBe(true);
  });
  it('discards a prepared upload and shared-term drafts after sign-out', async () => {
    await edit();
    let finish!: (file: File) => void;
    (component as any).prepareImage = () => new Promise<File>((resolve) => (finish = resolve));
    component.termDraft = { label_en: 'Private draft' };
    component.pickerSelection = ['7'];
    const file = new File(['synthetic'], 'portrait.png', { type: 'image/png' });
    const pending = component.uploadImage({
      target: { files: [file], value: 'portrait.png' },
    } as unknown as Event);
    const signOut = component.logout();
    http.expectOne(base + '/logout').flush({});
    await signOut;
    finish(file);
    await pending;
    http.expectNone(base + '/images');
    expect(component.values).toBeNull();
    expect(component.termDraft).toEqual({});
    expect(component.pickerSelection).toEqual([]);
  });
  it('restores a cookie-backed session and loads the workspace without browser token storage', async () => {
    component.ngOnInit();
    http.expectOne(base + '/session').flush(info());
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne(base + '/content/prayers?q=&page=0').flush({ items: [], hasMore: false });
    http.expectOne(base + '/schema/prayers').flush([]);
    await Promise.resolve();
    expect(component.signedIn()).toBe(true);
    expect(ADMIN_IDLE_MS).toBe(8 * 60 * 60_000);
  });
  it('does not extend idle sessions with background heartbeats', () => {
    vi.useFakeTimers();
    component.signedIn.set(true);
    vi.advanceTimersByTime(10 * 60_000);
    http.expectNone(base + '/session');
  });
  it('does not restore secrets or sessions after a late verification response', async () => {
    component.challenge = 'challenge';
    component.code = '123456';
    const verification = component.verify();
    const request = http.expectOne(base + '/verify');
    const signOut = component.logout();
    http.expectOne(base + '/logout').flush({});
    await signOut;
    request.flush(info());
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne(base + '/logout').flush({});
    await verification;
    expect(component.signedIn()).toBe(false);
    expect(component.code).toBe('');
    expect(component.qrCode()).toBe('');
  });
  it('encodes reserved characters in authenticator labels', () => {
    const uri = new URL(authenticatorUri('SYNTHETIC', 'owner+test@example.test'));
    expect(uri.searchParams.get('secret')).toBe('SYNTHETIC');
    expect(decodeURIComponent(uri.pathname)).toContain('owner+test@example.test');
  });
});
