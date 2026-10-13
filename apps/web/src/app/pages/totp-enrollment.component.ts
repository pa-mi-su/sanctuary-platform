import { Component, effect, input, signal } from '@angular/core';
import { toDataURL } from 'qrcode';

export function totpUri(secret: string, account: string): string {
  const issuer = 'Sanctuary Admin';
  return `otpauth://totp/${encodeURIComponent(`${issuer}:${account.trim()}`)}?secret=${encodeURIComponent(secret)}&issuer=${encodeURIComponent(issuer)}&algorithm=SHA1&digits=6&period=30`;
}

@Component({
  selector: 'app-totp-enrollment',
  standalone: true,
  template: `
    <h3>Set up your authenticator</h3>
    <p>Open your authenticator app, choose <strong>Add account</strong>, then scan this QR code.</p>
    <div class="qr-container">
      @if (qrCode()) {
        <img
          [src]="qrCode()"
          width="280"
          height="280"
          alt="Scan this QR code with your authenticator app to add Sanctuary Admin"
        />
      } @else {
        <p role="status">
          {{
            failed()
              ? 'QR code unavailable. Use the manual setup option below.'
              : 'Preparing your QR code…'
          }}
        </p>
      }
    </div>
    <p class="account-label">Sanctuary Admin · {{ account() }}</p>
    <details>
      <summary>Can’t scan the code?</summary>
      <p>Choose manual entry in your authenticator. Use this setup key with time-based codes.</p>
      <code>{{ secret() }}</code>
    </details>
    <p class="next-step">Enter the six-digit code from your authenticator below to finish setup.</p>
  `,
  styles: `
    :host {
      display: block;
    }
    h3 {
      margin-bottom: 10px;
    }
    p {
      line-height: 1.6;
    }
    .qr-container {
      display: flex;
      justify-content: center;
      margin: 24px 0 12px;
    }
    img {
      display: block;
      width: 280px;
      max-width: 100%;
      height: auto;
      background: white;
      border-radius: 12px;
      padding: 8px;
    }
    .account-label {
      text-align: center;
      font-size: 13px;
      color: #bfd3dc;
      overflow-wrap: anywhere;
    }
    details {
      margin-top: 20px;
      font-size: 13px;
      color: #bfd3dc;
    }
    summary {
      cursor: pointer;
      text-decoration: underline;
      text-underline-offset: 4px;
    }
    summary:focus-visible {
      outline: 3px solid #f2d77d;
      outline-offset: 4px;
    }
    code {
      display: block;
      overflow-wrap: anywhere;
      background: #ffffff10;
      padding: 16px;
      border: 1px solid #ffffff30;
      border-radius: 10px;
      letter-spacing: 1px;
    }
    .next-step {
      font-size: 14px;
      margin-top: 24px;
    }
  `,
})
export class TotpEnrollmentComponent {
  readonly secret = input.required<string>();
  readonly account = input.required<string>();
  readonly qrCode = signal<string | null>(null);
  readonly failed = signal(false);

  constructor() {
    effect((onCleanup) => {
      const uri = totpUri(this.secret(), this.account());
      let active = true;
      this.qrCode.set(null);
      this.failed.set(false);
      onCleanup(() => {
        active = false;
      });
      // Encode locally. Never transmit the enrollment secret to an external QR service.
      void toDataURL(uri, { width: 280, margin: 4, errorCorrectionLevel: 'M' }).then(
        (result) => {
          if (active) this.qrCode.set(result);
        },
        () => {
          if (active) this.failed.set(true);
        },
      );
    });
  }
}
