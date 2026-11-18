import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AnnouncementInboxService } from './announcement-inbox.service';
import { SanctuaryAuthService } from '../auth/sanctuary-auth.service';
import { SANCTUARY_API_BASE_URL } from '../api/sanctuary-api.config';
const base = 'https://inbox.example.test';
const key = 'sanctuary-announcements-guest:' + base;
const row = (id = 'one', time = 100) => ({
  id,
  title: 'An invitation',
  body: 'Full message',
  destination: 'https://mydailysanctuary.com/',
  published_at: time,
});
const settle = () => new Promise((resolve) => setTimeout(resolve, 0));
describe('Announcement inbox state', () => {
  let inbox: AnnouncementInboxService;
  let http: HttpTestingController;
  let auth: any;
  beforeEach(() => {
    localStorage.removeItem(key);
    localStorage.removeItem(key + ':installation-before');
    // Most tests represent an existing browser with a previously initialized guest state.
    localStorage.setItem(key, JSON.stringify({readIds:[],clearedIds:[],clearedBefore:0}));
    auth = { state: signal({ status: 'anonymous', email: null }) };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SANCTUARY_API_BASE_URL, useValue: base },
        { provide: SanctuaryAuthService, useValue: auth },
      ],
    });
    inbox = TestBed.inject(AnnouncementInboxService);
    http = TestBed.inject(HttpTestingController);
    TestBed.tick();
  });
  afterEach(() => {
    http.verify();
    localStorage.removeItem(key);
    localStorage.removeItem(key + ':installation-before');
  });
  async function initial(items = [row()]) {
    http.expectOne(base + '/announcements?page=0').flush({ items, hasMore: false });
    await settle();
  }
  it('starts a fresh installation empty and preserves future messages without uploading the device baseline', async () => {
    await initial();
    localStorage.removeItem(key);
    localStorage.removeItem(key + ':installation-before');
    const fresh = inbox.load();
    http.expectOne(base + '/announcements?page=0').flush({items:[row('old',100)],hasMore:false});
    await fresh;
    expect(inbox.visible()).toEqual([]); expect(inbox.unread()).toBe(false);
    const refresh = inbox.load();
    http.expectOne(base + '/announcements?page=0').flush({items:[row('new',101),row('old',100)],hasMore:false});
    await refresh;
    expect(inbox.visible().map(x=>x.id)).toEqual(['new']);
    await inbox.change('read','new');
    expect(JSON.parse(localStorage.getItem(key)!).clearedBefore).toBe(0);
    auth.state.set({status:'authenticated',email:'existing@sanctuary.invalid'});TestBed.tick();
    const transfer=http.expectOne(base+'/me/announcements/guest');
    expect(transfer.request.body.clearedBefore).toBe(0);
    transfer.flush({readIds:['new'],clearedIds:[],clearedBefore:0});await settle();
    http.expectOne(base+'/announcements?page=0').flush({items:[row('new',101),row('old',100)],hasMore:false});await settle();
    expect(inbox.visible().map(x=>x.id)).toEqual(['new']);
  });
  it('starts a new account after the historical backlog and keeps newer messages unread', async () => {
    await initial([row('old', 100)]);
    auth.state.set({ status: 'authenticated', email: 'new@sanctuary.invalid' });
    TestBed.tick();
    http.expectOne(base + '/me/announcements').flush({ readIds: [], clearedIds: [], clearedBefore: 200 });
    await settle();
    http.expectOne(base + '/announcements?page=0').flush({ items: [row('old', 100), row('boundary', 200)], hasMore: true });
    await settle();
    expect(inbox.visible()).toEqual([]);
    expect(inbox.unread()).toBe(false);
    const next = inbox.load(true);
    http.expectOne(base + '/announcements?page=1').flush({ items: [row('older-page', 50)], hasMore: false });
    await next;
    expect(inbox.visible()).toEqual([]);
    const refresh = inbox.load();
    http.expectOne(base + '/me/announcements').flush({ readIds: [], clearedIds: [], clearedBefore: 200 });
    await settle();
    http.expectOne(base + '/announcements?page=0').flush({ items: [row('new', 201), row('old', 100)], hasMore: false });
    await refresh;
    expect(inbox.visible().map(x => x.id)).toEqual(['new']);
    expect(inbox.unread()).toBe(true);
  });
  it('lets a guest read, clear and undo without signing in or enabling push', async () => {
    await initial();
    expect(inbox.unread()).toBe(true);
    await inbox.change('read', 'one');
    expect(inbox.unread()).toBe(false);
    await inbox.change('clear', 'one');
    expect(inbox.visible()).toEqual([]);
    await inbox.change('restore', 'one');
    expect(inbox.visible()).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem(key)!).readIds).toEqual(['one']);
    http.expectNone(base + '/me/announcements');
  });
  it('transfers guest dismissals to the first account, not the next account', async () => {
    await initial(); await inbox.change('clear', 'one');
    auth.state.set({status:'authenticated',email:'first@sanctuary.invalid'}); TestBed.tick();
    const merge = http.expectOne(base+'/me/announcements/guest');
    expect(merge.request.body.clearedIds).toEqual(['one']);
    merge.flush({readIds:[],clearedIds:['one'],clearedBefore:0}); await settle();
    http.expectOne(base+'/announcements?page=0').flush({items:[row()],hasMore:false}); await settle();
    expect(inbox.visible()).toHaveLength(0); expect(localStorage.getItem(key)).toBeNull();
    auth.state.set({status:'authenticated',email:'second@sanctuary.invalid'}); TestBed.tick();
    http.expectOne(base+'/me/announcements').flush({readIds:[],clearedIds:[],clearedBefore:0}); await settle();
    http.expectOne(base+'/announcements?page=0').flush({items:[row()],hasMore:false}); await settle();
    expect(inbox.visible()).toHaveLength(1);
  });
  it('retains a failed transfer only for the account that claimed it', async () => {
    await initial(); await inbox.change('clear','one');
    auth.state.set({status:'authenticated',email:'first@sanctuary.invalid'}); TestBed.tick();
    http.expectOne(base+'/me/announcements/guest').flush({}, {status:503,statusText:'Offline'}); await settle();
    auth.state.set({status:'authenticated',email:'second@sanctuary.invalid'}); TestBed.tick();
    http.expectOne(base+'/me/announcements').flush({readIds:[],clearedIds:[],clearedBefore:0}); await settle();
    http.expectOne(base+'/announcements?page=0').flush({items:[row()],hasMore:false}); await settle();
    expect(inbox.visible()).toHaveLength(1);
    auth.state.set({status:'authenticated',email:'first@sanctuary.invalid'}); TestBed.tick();
    http.expectOne(base+'/me/announcements/guest').flush({readIds:[],clearedIds:['one'],clearedBefore:0}); await settle();
    http.expectOne(base+'/announcements?page=0').flush({items:[row()],hasMore:false}); await settle();
    expect(inbox.visible()).toHaveLength(0);
  });
  it('ignores a late account receipt response after sign-out', async () => {
    await initial();
    auth.state.set({ status: 'authenticated' });
    TestBed.tick();
    const old = http.expectOne(base + '/me/announcements');
    auth.state.set({ status: 'anonymous' });
    TestBed.tick();
    http.expectOne(base + '/announcements?page=0').flush({ items: [row()], hasMore: false });
    await settle();
    old.flush({ readIds: ['one'], clearedIds: ['one'], clearedBefore: 999 });
    await settle();
    expect(inbox.visible()).toHaveLength(1);
    expect(inbox.unread()).toBe(true);
  });
  it('clear-all hides older pages while leaving newer announcements available', async () => {
    await initial([row('latest', 100)]);
    await inbox.change('clearAll');
    const load = inbox.load();
    http
      .expectOne(base + '/announcements?page=0')
      .flush({ items: [row('new', 200), row('latest', 100), row('older', 50)], hasMore: false });
    await load;
    expect(inbox.visible().map((x) => x.id)).toEqual(['new']);
  });
  it('recovers from a failed feed request without losing local receipts', async () => {
    http.expectOne(base + '/announcements?page=0').error(new ProgressEvent('error'));
    await settle();
    expect(inbox.busy()).toBe(false);
    expect(inbox.error()).toContain('Could not load');
    const retry = inbox.load();
    http.expectOne(base + '/announcements?page=0').flush({ items: [row()], hasMore: false });
    await retry;
    expect(inbox.visible()).toHaveLength(1);
    expect(inbox.error()).toBe('');
  });
  it('freezes the clear-all cutoff when a newer announcement arrives during confirmation', async () => {
    await initial([row('reviewed', 100)]);
    const through = inbox.latest();
    inbox.items.set([row('new', 200), ...inbox.items()]);
    await inbox.change('clearAll', undefined, through);
    expect(inbox.visible().map((item) => item.id)).toEqual(['new']);
  });
  it('opens a cleared announcement explicitly without restoring it to the inbox', async () => {
    const id = '11111111-1111-4111-8111-111111111111';
    await initial([row(id)]);
    await inbox.change('clearAll');
    await inbox.openRequested(id);
    expect(inbox.items()[0].body).toBe('Full message');
    expect(inbox.state().readIds).toContain(id);
    expect(inbox.visible()).toEqual([]);
  });
  it('discards malformed guest storage without crashing the inbox', async () => {
    await initial();
    localStorage.setItem(key, JSON.stringify({ readIds: null }));
    const load = inbox.load();
    http.expectOne(base + '/announcements?page=0').flush({ items: [row()], hasMore: false });
    await load;
    expect(inbox.unread()).toBe(true);
  });
});
