(() => {
  const origin = window.location.origin;
  const hostname = window.location.hostname;
  const localHosts = new Set(['localhost', '127.0.0.1']);
  const productionHosts = new Set(['mydailysanctuary.com', 'www.mydailysanctuary.com']);
  const authEnabled = localHosts.has(hostname) || productionHosts.has(hostname);

  window.SANCTUARY_AUTH_CONFIG = {
    enabled: authEnabled,
    cognitoDomain: 'https://sanctuary-160885294528-prod.auth.us-east-1.amazoncognito.com',
    clientId: '7e3anthnuctm8p9nqck6kesjm9',
    redirectUri: origin,
    logoutUri: origin,
    scopes: ['openid', 'email', 'profile'],
  };
})();
