(() => {
  const origin = window.location.origin;
  const hostname = window.location.hostname;
  const localHosts = new Set(['localhost', '127.0.0.1']);
  const devHosts = new Set(['dev.mydailysanctuary.com']);
  const productionHosts = new Set(['mydailysanctuary.com', 'www.mydailysanctuary.com']);
  const devAuthConfig = {
    cognitoDomain: 'https://us-east-1ocrxzjkbv.auth.us-east-1.amazoncognito.com',
    clientId: '2ukk5f1esf1gmgi9aadvvbjjgs',
  };
  const prodAuthConfig = {
    cognitoDomain: 'https://sanctuary-160885294528-prod.auth.us-east-1.amazoncognito.com',
    clientId: '7e3anthnuctm8p9nqck6kesjm9',
  };
  const selectedAuthConfig = devHosts.has(hostname) || localHosts.has(hostname) ? devAuthConfig : prodAuthConfig;
  const authEnabled = localHosts.has(hostname) || devHosts.has(hostname) || productionHosts.has(hostname);

  window.SANCTUARY_AUTH_CONFIG = {
    enabled: authEnabled,
    cognitoDomain: selectedAuthConfig.cognitoDomain,
    clientId: selectedAuthConfig.clientId,
    redirectUri: origin,
    logoutUri: origin,
    scopes: ['openid', 'email', 'profile'],
  };
})();
