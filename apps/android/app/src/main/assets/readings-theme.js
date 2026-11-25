(() => {
  if (location.hostname !== 'bible.usccb.org') return;
  const css = `
:root {
  --sanctuary-surface: #D8E3D9;
  --sanctuary-surface-raised: #C4D5C8;
  --sanctuary-surface-card: #E3EBE2;
  --sanctuary-surface-elevated: #D3E1D4;
  --sanctuary-surface-intermediate: #D2E0D4;
  --sanctuary-text: #173D36;
  --sanctuary-text-muted: #465B4D;
  --sanctuary-primary: #24634F;
  --sanctuary-primary-strong: #174B3D;
  --sanctuary-on-accent: #F4F2E7;
  --sanctuary-hero: #073C3B;
  --sanctuary-border: #A9C2B2;
  --sanctuary-border-strong: #6F9380;
  --sanctuary-error: #9C2630;
  --sanctuary-error-surface: #F0DDD7;
  --sanctuary-warning: #755019;
  --sanctuary-warning-surface: #EADFC5;
  --sanctuary-success: #24634F;
  --sanctuary-gold: #75521D;
  --sanctuary-berry: #88596E;
  --sanctuary-advent: #7858B9;
  --sanctuary-christmas: #DCB969;
  --sanctuary-lent: #9B5087;
  --sanctuary-easter: #F5F5FA;
  --sanctuary-ordinary: #3C9B5F;
  --sanctuary-today: #F2CF63;
  --sanctuary-qr-canvas: #FFFFFF;
}
    :root { color-scheme: light !important; }
    html, body { background: var(--sanctuary-surface) !important; color: var(--sanctuary-text) !important; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important; }
    main, article, section, div, .container, .wrapper { background-color: transparent !important; background-image: none !important; }
    main, article, p, li, h1, h2, h3, h4, span { color: var(--sanctuary-text) !important; }
    a, a:visited { color: var(--sanctuary-primary) !important; }
    header, nav, footer { background: var(--sanctuary-primary-strong) !important; }
    header *, nav *, footer * { color: var(--sanctuary-on-accent) !important; }
    h1, h2 { font-family: Georgia, serif !important; }
    h1 { font-size: 2rem !important; line-height: 1.2 !important; }
    h2 { font-size: 1.6rem !important; line-height: 1.3 !important; }
    p { line-height: 1.65 !important; }
    button, .button, input[type=submit] { background: var(--sanctuary-surface-raised) !important; color: var(--sanctuary-text) !important; border-color: var(--sanctuary-border) !important; }
    a[class*=button], a[class*=btn] { border-color: var(--sanctuary-border) !important; border-radius: 10px !important; }
    hr { border-color: var(--sanctuary-border) !important; }
    #sliding-popup, .eu-cookie-compliance-banner { max-height: 45vh !important; overflow-y: auto !important; background: var(--sanctuary-surface-card) !important; box-shadow: 0 -2px 12px color-mix(in srgb, var(--sanctuary-text) 13%, transparent) !important; }
    .eu-cookie-compliance-content { padding: 16px !important; display: flex !important; flex-direction: column !important; gap: 12px !important; }
    .eu-cookie-compliance-message p { font-size: 14px !important; line-height: 1.5 !important; margin: 0 !important; }
    .eu-cookie-compliance-buttons { float: none !important; width: auto !important; }
    .eu-cookie-compliance-buttons button { margin: 0 !important; padding: 8px 18px !important; border-radius: 8px !important; }
  `;
  let style = document.getElementById('sanctuary-reading-theme');
  if (!style) { style = document.createElement('style'); style.id = 'sanctuary-reading-theme'; document.head.appendChild(style); }
  style.textContent = css;
  const themeLinks = () => {
    document.querySelectorAll('a').forEach(a => {
      a.style.setProperty('color', a.closest('header, nav, footer') ? 'var(--sanctuary-on-accent)' : 'var(--sanctuary-primary)', 'important');
      if (a.matches('[class*=btn], [class*=button]')) {
        a.style.setProperty('border-color', 'var(--sanctuary-border)', 'important');
        a.style.setProperty('border-radius', '10px', 'important');
      }
    });
  };
  themeLinks();
  if (!window.sanctuaryReadingObserver) {
    window.sanctuaryReadingObserver = new MutationObserver(themeLinks);
    window.sanctuaryReadingObserver.observe(document.body, { childList: true, subtree: true });
  }
})();