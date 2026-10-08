# Around the Church source policy

`Around the Church` has two deliberately separate delivery paths:

1. The official Vatican News web component is loaded directly from
   `https://www.vaticannews.va/widget.js` in an isolated in-app browser. Vatican
   News remains the renderer and host of that content; Sanctuary does not parse,
   copy, normalize, or cache it.
2. The native photo carousel consumes only Agenzia Fides material whose article
   pages state that site content is available under CC BY 4.0. Sanctuary stores
   no article body and does not copy publisher images into Sanctuary storage.

The two paths must not be conflated: the Vatican widget is an official embed,
not permission to ingest Vatican RSS content.

## Delivery and personalization

- The API refreshes each approved Fides language feed every 10 minutes and retains
  the last successful result if a publisher is temporarily unavailable.
- API responses may be cached publicly for up to 10 minutes. While the Home
  screen is active, the mobile apps request updated results every 15 minutes;
  pull-to-refresh on iOS and the refresh control on Android request them sooner.
- The carousel advances every 10 seconds and restarts that interval after a
  manual swipe. Users who enable reduced motion do not receive automatic page
  animation on iOS.
- The Vatican widget receives only Sanctuary's selected language (`en`, `es`,
  or `pl`). The Fides native carousel uses English or Spanish; Polish currently
  falls back to the English Fides feed and labels it `EN`. Neither path receives
  a ZIP code, GPS position, state, or country.
- The Home screen displays the official Vatican News widget by default. Users
  can switch to the Fides photo carousel; that carousel wraps continuously in
  both directions instead of stopping at its final story.
- Vatican News controls the freshness of its hosted widget. Sanctuary loads the
  current widget whenever the Home view is created or the language changes; it
  does not poll or cache Vatican content. Fides is refreshed by the Sanctuary API
  every 10 minutes, requested by active mobile Home screens every 15 minutes,
  and refreshed on demand through the native refresh controls.

## Approved

- **Vatican News Widget** — official hosted experience supplied by Vatican News
  at `https://www.vaticannews.va/widget/embed.html`. Sanctuary uses the vendor's
  unmodified web component with mobile mode and 10-second automatic video
  rotation. It supports English, Spanish, and Polish.
- **Agenzia Fides** — source for Sanctuary's native photo carousel, which remains
  independently available when the Vatican widget cannot load. It is the
  official news service of the Pontifical Mission Societies.
  English and Spanish RSS feeds are published at
  `https://www.fides.org/en/news/rss` and
  `https://www.fides.org/es/news/rss`. Fides states on every article page that
  the site contents are licensed under Creative Commons Attribution 4.0.
  Sanctuary displays the canonical article photograph, title, short excerpt,
  publication date, Fides attribution, image credit when provided, and the
  `CC BY 4.0` license label. The API excludes an article when its page does not
  provide an HTTPS `og:image` hosted by `www.fides.org`.

## Not approved

- **Vatican News RSS ingestion** — the official widget is approved for embedding;
  it does not authorize parsing or republishing Vatican RSS text or photographs.
- **EWTN News / CNA / ACI Prensa / EWTN Polska** — an RSS endpoint is not itself
  a license to reproduce publisher text or photographs. Keep these sources out
  of the native carousel until Sanctuary has written syndication permission.
- **OSV News, National Catholic Register, and Catholic Online** — do not enable
  them merely because a feed exists. Record explicit reuse terms or written
  permission first.
- **Polish Bishops' Conference (episkopat.pl)** — the RSS page states that the
  feed grants no reuse license beyond personal use. Written permission is
  required before it can be enabled.
- **USCCB News** — the public news RSS feed does not state that third-party
  photographs embedded in releases may be republished in a mobile application.
  Do not ingest those photographs without written permission covering the app.

## Adding a source

Before code changes, record the official feed URL, canonical hostname,
permitted metadata, thumbnail behavior, attribution requirements, supported
languages, and written permission when required. Add parser fixtures and tests
for the source's real feed structure. Never scrape or store full publisher
articles. Never substitute an unrelated stock image for the exact story image.
