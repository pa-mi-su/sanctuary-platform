import { resolveSanctuaryApiBaseUrl } from './sanctuary-api.config';

describe('API host resolution', () => {
  it('keeps local frontend and API on the same host for session cookies', () => {
    expect(resolveSanctuaryApiBaseUrl({ hostname: '127.0.0.1', search: '' })).toBe(
      'http://127.0.0.1:8080',
    );
    expect(resolveSanctuaryApiBaseUrl({ hostname: 'localhost', search: '' })).toBe(
      'http://localhost:8080',
    );
    expect(resolveSanctuaryApiBaseUrl({ hostname: '127.0.0.1', search: '?api=local' })).toBe(
      'http://127.0.0.1:8080',
    );
  });
  it('preserves deployed environment selection and explicit overrides', () => {
    expect(resolveSanctuaryApiBaseUrl({ hostname: 'dev.mydailysanctuary.com', search: '' })).toBe(
      'https://dev-api.mydailysanctuary.com',
    );
    expect(resolveSanctuaryApiBaseUrl({ hostname: 'mydailysanctuary.com', search: '' })).toBe(
      'https://api.mydailysanctuary.com',
    );
    expect(resolveSanctuaryApiBaseUrl({ hostname: 'localhost', search: '?api=prod' })).toBe(
      'https://api.mydailysanctuary.com',
    );
  });
});
