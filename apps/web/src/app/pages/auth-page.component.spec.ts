import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthPageComponent } from './auth-page.component';
import { SanctuaryAuthService } from '../core/auth/sanctuary-auth.service';

// Exercise navigation and async races through the actual component, without real credentials.
describe('Account form lifecycle', () => {
  const auth = {
    state: signal({ status: 'anonymous', message: null }),
    forgotPassword: vi.fn(), register: vi.fn(), login: vi.fn(), confirmRegistration: vi.fn(),
  };
  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({ providers: [{ provide: SanctuaryAuthService, useValue: auth }] });
  });
  function setup(step = 'login') {
    const fixture = TestBed.createComponent(AuthPageComponent);
    fixture.componentRef.setInput('isConfigured', true);
    fixture.componentRef.setInput('initialStep', step);
    fixture.detectChanges();
    // Protected members are deliberately exposed only in this test harness.
    const form = fixture.componentInstance as any;
    return { fixture, form };
  }
  it('offers an accessible clear action for a nonempty email field', async () => {
    const { fixture, form } = setup();
    await fixture.whenStable();
    const input = fixture.nativeElement.querySelector('input[name="loginEmail"]');
    input.value = 'example@example.test';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Clear email"]').click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(form.loginEmail).toBe('');
    expect(fixture.nativeElement.querySelector('input[name="loginEmail"]').value).toBe('');
  });
  it('clears all abandoned fields and reveal flags through repeated Back navigation', () => {
    const { form } = setup('reset');
    form.loginEmail = form.registerEmail = form.forgotEmail = form.resetEmail = 'example@example.test';
    form.registerFirstName = 'Example';
    form.loginPassword = form.registerPassword = form.resetPassword = 'Synthetic-password-123';
    form.confirmationCode = form.resetCode = '123456';
    form.showLoginPassword = form.showResetPassword = true;
    form.goBack();
    expect(form.step()).toBe('forgot');
    for (const key of ['loginEmail', 'registerEmail', 'forgotEmail', 'resetEmail', 'registerFirstName', 'loginPassword', 'registerPassword', 'resetPassword', 'confirmationCode', 'resetCode']) {
      expect(form[key], key).toBe('');
    }
    expect(form.showLoginPassword).toBe(false);
    expect(form.showResetPassword).toBe(false);
    form.goBack();
    expect(form.step()).toBe('login');
    form.goBack();
    expect(form.step()).toBe('landing');
  });
  it('does not repopulate a destroyed form after a delayed reset-code response', async () => {
    let resolve!: (value: string) => void;
    auth.forgotPassword.mockImplementation(() => new Promise<string>(r => resolve = r));
    const { fixture, form } = setup('forgot');
    form.forgotEmail = 'example@example.test';
    const request = form.submitForgotPassword();
    fixture.destroy();
    resolve('Code sent');
    await request;
    expect(form.resetEmail).toBe('');
    expect(form.forgotEmail).toBe('');
    expect(form.step()).toBe('forgot');
  });
  it('ignores a late response after the parent starts a different account flow', async () => {
    let resolve!: (value: string) => void;
    auth.forgotPassword.mockImplementation(() => new Promise<string>(r => resolve = r));
    const { fixture, form } = setup('forgot');
    form.forgotEmail = 'example@example.test';
    const request = form.submitForgotPassword();
    fixture.componentRef.setInput('initialStep', 'register');
    fixture.detectChanges();
    resolve('Code sent');
    await request;
    expect(form.step()).toBe('register');
    expect(form.resetEmail).toBe('');
    expect(form.pending()).toBe(false);
  });
  it('retains only the target email when advancing to code entry', async () => {
    auth.forgotPassword.mockResolvedValue('Code sent');
    const { form } = setup('login');
    form.loginEmail = 'example@example.test';
    form.loginPassword = 'Synthetic-password-123';
    form.openForgotPassword();
    expect(form.loginPassword).toBe('');
    await form.submitForgotPassword();
    expect(form.step()).toBe('reset');
    expect(form.resetEmail).toBe('example@example.test');
    expect(form.resetCode).toBe('');
    expect(form.resetPassword).toBe('');
  });
  it('signs in automatically after verification without persisting the registration password', async () => {
    auth.register.mockResolvedValue({ email: 'synthetic@sanctuary.invalid' });
    auth.confirmRegistration.mockResolvedValue('Confirmed');
    auth.login.mockImplementation(async () => { auth.state.set({ status: 'authenticated', message: null }); });
    const { form } = setup('register');
    form.registerFirstName = 'Test'; form.registerLastName = 'Account';
    form.registerEmail = 'synthetic@sanctuary.invalid';
    form.registerPassword = form.registerPasswordConfirmation = 'Synthetic-password-123';
    await form.submitRegister();
    expect(form.step()).toBe('confirm');
    expect(form.registerPassword).toBe('Synthetic-password-123');
    expect(form.registerPasswordConfirmation).toBe('');
    form.confirmationCode = '123456';
    await form.submitConfirmation();
    expect(auth.login).toHaveBeenCalledWith({email: 'synthetic@sanctuary.invalid', password: 'Synthetic-password-123'});
    expect(form.registerPassword).toBe('');
  });
  it('drops the temporary registration password when verification is abandoned', async () => {
    auth.register.mockResolvedValue({ email: 'synthetic@sanctuary.invalid' });
    const { form } = setup('register');
    form.registerFirstName = 'Test'; form.registerLastName = 'Account'; form.registerEmail = 'synthetic@sanctuary.invalid';
    form.registerPassword = form.registerPasswordConfirmation = 'Synthetic-password-123';
    await form.submitRegister(); form.goBack();
    expect(form.registerPassword).toBe(''); expect(auth.login).not.toHaveBeenCalled();
  });

});
