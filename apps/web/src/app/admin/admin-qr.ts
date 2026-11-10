import { InjectionToken } from '@angular/core';
import { toDataURL } from 'qrcode';

// Encode entirely in this browser. A real enrollment secret never leaves the app for QR rendering.
export const ADMIN_QR_ENCODER = new InjectionToken<(text: string) => Promise<string>>(
  'ADMIN_QR_ENCODER',
  {
    providedIn: 'root',
    factory: () => (text) => toDataURL(text, { width: 240, margin: 3, errorCorrectionLevel: 'M' }),
  },
);
export function authenticatorUri(secret: string, email: string): string {
  return `otpauth://totp/${encodeURIComponent('Sanctuary Admin:' + email)}?secret=${encodeURIComponent(secret)}&issuer=Sanctuary&algorithm=SHA1&digits=6&period=30`;
}
