# Around the Church source policy

`Around the Church` is a link-preview reader. It may ingest only official
publisher syndication feeds. Sanctuary stores no article body and does not copy
publisher images into Sanctuary storage. A card shows the publisher-supplied
headline, short description and feed thumbnail, identifies the publisher, and
opens the canonical publisher page. Every article and image hostname must be
explicitly allowlisted in the API. The mobile clients consume only the
normalized Sanctuary API response.

## Delivery and personalization

- The API refreshes each approved language feed every 10 minutes and retains
  the last successful result if a publisher is temporarily unavailable.
- API responses may be cached publicly for up to 10 minutes. While the Home
  screen is active, the mobile apps request updated results every 15 minutes;
  pull-to-refresh on iOS and the refresh control on Android request them sooner.
- The carousel advances every 10 seconds and restarts that interval after a
  manual swipe. Users who enable reduced motion do not receive automatic page
  animation on iOS.
- Selection is based only on the language chosen in Sanctuary: English uses
  EWTN News, Spanish uses ACI Prensa, and Polish uses EWTN Polska. The news
  endpoint does not receive or inspect a ZIP code, GPS position, state, or
  country. Users in different regions therefore see the same global Catholic
  feed for the same selected language.

## Approved

- **EWTN News** — primary English source. Its official RSS feed is
  `https://www.ewtnnews.com/rss` and supplies current headlines, concise
  descriptions, canonical article links, exact story thumbnails and photo
  credits. Sanctuary displays those values as a linked preview and loads images
  directly from EWTN's `res.cloudinary.com` host.
- **ACI Prensa** — primary Spanish source. Its official RSS feed is
  `https://www.aciprensa.com/rss/news` and supplies Spanish headlines,
  descriptions, canonical links and exact story thumbnails hosted by EWTN.
- **EWTN Polska** — primary Polish source. Its official RSS feed is
  `https://ewtn.pl/feed/` and supplies Polish headlines, descriptions,
  canonical links and story thumbnails hosted by `ewtn.pl`.
- **Agenzia Fides** — automatic fallback only when the appropriate EWTN feed is
  unavailable. It is the official news service of the Pontifical Mission Societies.
  English and Spanish RSS feeds are published at
  `https://www.fides.org/en/news/rss` and
  `https://www.fides.org/es/news/rss`. Fides states on every article page that
  the site contents are licensed under Creative Commons Attribution 4.0.
  Sanctuary displays the canonical article photograph, title, short excerpt,
  publication date, Fides attribution, image credit when provided, and the
  `CC BY 4.0` license label. The API excludes an article when its page does not
  provide an HTTPS `og:image` hosted by `www.fides.org`.

## Not approved

- **Vatican News** — its public legal notice prohibits reproduction and
  collection of portal content and requires written authorization for links.
  Do not add Vatican News content or links unless Sanctuary receives written
  authorization from the Dicastery for Communication.
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
