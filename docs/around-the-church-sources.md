# Around the Church source policy

`Around the Church` is a native carousel backed by the public Atom/Media RSS
feeds for verified Catholic YouTube channels. Sanctuary displays only metadata
published by the channel: title, description excerpt, thumbnail, publication
date, channel name, and the canonical YouTube watch URL. Tapping a card leaves
Sanctuary and opens the official video.

Sanctuary does not scrape article pages, copy full articles, generate or rewrite
news, download publisher images, proxy videos, or remove YouTube attribution.

## Approved channels

| Requested language | Channel | Channel ID | Item language |
| --- | --- | --- | --- |
| English | EWTN News | `UCaJBwb7XkojUOPbz_6uzPag` | English |
| Spanish | ACI Prensa | `UCYBvW57DuPrWwGdEe-BkMSg` | Spanish |
| Polish | EWTN News fallback | `UCaJBwb7XkojUOPbz_6uzPag` | English |

The Polish UI labels fallback stories as English. Add a Polish channel only
after verifying its official identity, editorial quality, update frequency, and
YouTube channel ID.

## Refresh and failure behavior

- The API checks each channel every 15 minutes, matching YouTube's public feed
  cache interval.
- Responses are publicly cacheable for 15 minutes.
- The API retains the most recent successful in-memory result when a refresh
  fails. The mobile clients retain their last successful result for seven days.
- English is the final fallback when a localized channel is unavailable.
- The carousel contains at most 12 items and automatically advances every 10
  seconds. Manual paging is circular on both mobile platforms.

## Validation and curation

The backend accepts only:

- canonical HTTPS `youtube.com/watch?v=...` links whose video ID matches the
  feed entry;
- HTTPS YouTube thumbnail URLs whose path contains the same video ID; and
- entries with an ISO-8601 publication timestamp.

It rejects livestream placeholders, 24/7 streams, promotional trailers, full
scheduled programs, and any entry whose canonical feed URL identifies it as a
YouTube Short. Duplicate normalized titles are removed without changing the
channel-provided headline. Descriptions are plain-text, length-limited excerpts
of publisher-provided metadata; Sanctuary does not use AI to summarize or alter
reporting.

## Attribution and platform rules

Every card identifies the channel and YouTube, and links to the official watch
page. Changes must continue to comply with the YouTube API Services Terms,
Developer Policies, and Branding Guidelines. Do not replace the canonical link
with a copied article, cached video, or unapproved player.

## Adding a source

Before adding a channel:

1. Confirm that the channel is the publisher's official channel.
2. Record the immutable `UC...` channel ID rather than a mutable handle.
3. Inspect the live feed for relevant, frequently updated news segments.
4. Add parser/service tests for its actual metadata and unwanted content.
5. Verify attribution, external playback, accessibility, caching, and both
   mobile layouts on physical devices.
