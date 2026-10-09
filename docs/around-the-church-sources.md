# Around the Church source policy

`Around the Church` is a native article carousel backed by RSS feeds operated
by approved Catholic news publishers. Sanctuary displays only the feed's
headline, description excerpt, publication date, related image, image credit,
publisher name, and canonical article URL. Tapping a card opens the complete
article on the publisher's website.

Sanctuary does not scrape article pages, copy article bodies, rewrite reporting,
download publisher images, proxy media, or replace publisher attribution.

## Approved feeds

| Requested language | Publisher | Official feed | Item language |
| --- | --- | --- | --- |
| English | EWTN News | `https://www.ewtnnews.com/rss` | English |
| Spanish | ACI Prensa | `https://www.aciprensa.com/rss/noticias.xml` | Spanish |
| Polish | EWTN News fallback | `https://www.ewtnnews.com/rss` | English |

The Polish UI labels fallback stories as English. Add a Polish publisher only
after verifying its official identity, feed ownership, editorial quality,
update frequency, image metadata, and terms.

## Refresh and failure behavior

- The API checks the publisher feeds every 15 minutes, which matches the TTL
  declared by the EWTN feed.
- API responses are publicly cacheable for 15 minutes.
- The API retains the most recent successful in-memory result when a refresh
  fails. Mobile clients retain their most recent successful result for seven
  days.
- English is the final fallback when a localized feed is unavailable.
- The carousel contains at most 12 articles and automatically advances every
  10 seconds. Manual paging is circular on iOS and Android.

## Validation and presentation

The backend accepts an item only when it has:

- an HTTPS canonical URL on the approved publisher domain;
- an RFC 1123 publication timestamp; and
- an HTTPS, publisher-supplied related image on the approved media domain.

For EWTN News and ACI Prensa, article URLs must remain on the publisher's domain
and images must remain on EWTN's Cloudinary account. Image captions and
photographer credits are taken from Media RSS or the image block included in
the feed. Items without a valid related image are omitted rather than filled
with an unrelated or generated image.

Duplicate normalized headlines are removed without changing the
publisher-provided headline. Descriptions are converted to plain text and
length-limited for the card; Sanctuary does not use AI to summarize or alter
the reporting.

## Adding a source

Before adding a feed:

1. Confirm that the publisher operates the feed and article domain.
2. Confirm that the feed supplies canonical article links and related images.
3. Add exact allowlists for the article and image hosts.
4. Add parser tests using the source's real metadata structure.
5. Review the publisher's feed terms and attribution requirements.
6. Verify external article opening, accessibility, caching, and both mobile
   layouts on physical devices.
