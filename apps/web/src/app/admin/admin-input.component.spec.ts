import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { AdminInputComponent } from './admin-input.component';

@Component({
  standalone: true,
  imports: [FormsModule, AdminInputComponent],
  template: `<form>
    <admin-input
      label="Password"
      inputId="test-password"
      type="password"
      name="password"
      [(ngModel)]="password"
      [required]="true"
    />
  </form>`,
})
class Host {
  password = 'Synthetic value';
}

describe('Accessible admin field controls', () => {
  it('shows and hides a password without changing it and clears the bound form value', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    const input = root.querySelector('input')!;
    expect(input.type).toBe('password');
    const show = root.querySelector<HTMLButtonElement>('[aria-label="Show Password"]')!;
    show.click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(fixture.componentInstance.password).toBe('Synthetic value');
    expect(root.querySelector('[aria-pressed="true"]')).toBeTruthy();
    root.querySelector<HTMLButtonElement>('[aria-label="Clear Password"]')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.password).toBe('');
    expect(input.value).toBe('');
    expect(document.activeElement).toBe(input);
  });
});
