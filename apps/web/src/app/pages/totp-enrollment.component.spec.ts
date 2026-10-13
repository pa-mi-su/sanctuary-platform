import { TestBed } from '@angular/core/testing';
import { TotpEnrollmentComponent, totpUri } from './totp-enrollment.component';

describe('Authenticator QR enrollment', () => {
  it('uses a standard authenticator URI with a matching issuer and account label', () => {
    const uri = new URL(totpUri('JBSWY3DPEHPK3PXP', 'admin+test@example.org'));
    expect(uri.protocol).toBe('otpauth:');
    expect(uri.hostname).toBe('totp');
    expect(decodeURIComponent(uri.pathname.slice(1))).toBe(
      'Sanctuary Admin:admin+test@example.org',
    );
    expect(uri.searchParams.get('issuer')).toBe('Sanctuary Admin');
    expect(uri.searchParams.get('secret')).toBe('JBSWY3DPEHPK3PXP');
    expect(uri.searchParams.get('digits')).toBe('6');
    expect(uri.searchParams.get('period')).toBe('30');
  });
  it('renders a local QR image and keeps manual entry collapsed', async () => {
    TestBed.configureTestingModule({ imports: [TotpEnrollmentComponent] });
    const fixture = TestBed.createComponent(TotpEnrollmentComponent);
    fixture.componentRef.setInput('secret', 'JBSWY3DPEHPK3PXP');
    fixture.componentRef.setInput('account', 'admin@example.org');
    await fixture.whenStable();
    await vi.waitFor(() =>
      expect(fixture.componentInstance.qrCode()).toMatch(/^data:image\/png;base64,/),
    );
    await fixture.whenStable();
    const image = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(image.getAttribute('src')).toMatch(/^data:image\/png;base64,/);
    expect(fixture.nativeElement.querySelector('details').open).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Can’t scan the code?');
  });
});
