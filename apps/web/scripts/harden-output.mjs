import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

// Production output only; the development server keeps its local API/HMR connections.
export const contentSecurityPolicy = [
  "default-src 'self'",
  "base-uri 'self'",
  "object-src 'none'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https:",
  "font-src 'self' data:",
  "connect-src 'self' https://api.mydailysanctuary.com https://dev-api.mydailysanctuary.com https://cognito-idp.us-east-1.amazonaws.com",
  "form-action 'self' https://*.auth.us-east-1.amazoncognito.com",
  'upgrade-insecure-requests',
].join('; ');

const output = process.env.SANCTUARY_SHARE_PREVIEW_OUTPUT_DIR ?? path.resolve('dist/web/browser');
const index = path.join(output, 'index.html');
const html = await readFile(index, 'utf8');
if (!html.includes('<head>')) throw new Error('Cannot apply production security policy: missing head');
if (html.includes('http-equiv="Content-Security-Policy"')) throw new Error('Duplicate security policy');
if (/\son\w+\s*=/i.test(html)) throw new Error('Inline event handlers conflict with production CSP');
for (const script of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)) {
  if (!/\bsrc\s*=/.test(script[1]) && script[2].trim()) {
    throw new Error('Inline scripts conflict with production CSP');
  }
}
await writeFile(index, html.replace('<head>', `<head>\n<meta http-equiv="Content-Security-Policy" content="${contentSecurityPolicy}">\n<meta name="referrer" content="strict-origin-when-cross-origin">`));
console.log('Applied production content security policy.');
