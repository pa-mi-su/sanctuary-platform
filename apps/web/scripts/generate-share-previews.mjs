import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const SITE_ORIGIN = process.env.SANCTUARY_SHARE_PREVIEW_SITE_ORIGIN ?? 'https://mydailysanctuary.com';
const API_BASE_URL =
  process.env.SANCTUARY_SHARE_PREVIEW_API_BASE_URL ??
  'https://api.mydailysanctuary.com';
const OUTPUT_DIR = process.env.SANCTUARY_SHARE_PREVIEW_OUTPUT_DIR ?? path.resolve('dist/web/browser');
const FALLBACK_IMAGE_URL = `${SITE_ORIGIN}/brand-logo.png`;
const IOS_APP_STORE_ID = '6759986068';
const FETCH_ATTEMPTS = 3;
const FETCH_RETRY_DELAY_MS = 2_000;

const contentTypes = [
  {
    kind: 'saints',
    endpoint: '/content/saints/search?lang=en&query=',
    title: (item) => item.name,
    description: (item) => item.summary,
  },
  {
    kind: 'novenas',
    endpoint: '/content/novenas?lang=en&query=',
    title: (item) => item.title,
    description: (item) => item.description,
  },
  {
    kind: 'prayers',
    endpoint: '/content/prayers?lang=en&query=',
    title: (item) => item.title,
    description: (item) => item.bodyPreview,
  },
];

const indexPath = path.join(OUTPUT_DIR, 'index.html');
const indexHtml = await readFile(indexPath, 'utf8');

let generatedCount = 0;

for (const type of contentTypes) {
  const items = await fetchJson(`${API_BASE_URL}${type.endpoint}`);
  if (!Array.isArray(items)) {
    throw new Error(`Expected ${type.endpoint} to return an array.`);
  }

  for (const item of items) {
    if (!item?.slug) {
      continue;
    }

    const url = `${SITE_ORIGIN}/${type.kind}/${encodeURIComponent(item.slug)}`;
    const title = cleanText(type.title(item)) || 'Sanctuary';
    const description = cleanText(type.description(item)) || 'Open this Catholic prayer companion in Sanctuary.';
    const imageUrl = absoluteUrl(cleanText(item.imageUrl) || FALLBACK_IMAGE_URL);
    const html = renderPreviewHtml(indexHtml, {
      url,
      title: `${title} | Sanctuary`,
      description,
      imageUrl,
    });

    const outputPath = path.join(OUTPUT_DIR, type.kind, item.slug);
    await mkdir(path.dirname(outputPath), { recursive: true });
    await writeFile(outputPath, html);
    generatedCount += 1;
  }
}

console.log(`Generated ${generatedCount} share preview route objects in ${OUTPUT_DIR}.`);

async function fetchJson(url) {
  let lastError;
  for (let attempt = 1; attempt <= FETCH_ATTEMPTS; attempt += 1) {
    try {
      const response = await fetch(url, {
        headers: {
          Accept: 'application/json',
          'User-Agent': 'Sanctuary share preview generator',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch ${url}: ${response.status} ${response.statusText}`);
      }

      return response.json();
    } catch (error) {
      lastError = error;
      if (attempt === FETCH_ATTEMPTS) {
        break;
      }
      console.warn(`Fetch failed for ${url}; retrying (${attempt}/${FETCH_ATTEMPTS})...`);
      await delay(FETCH_RETRY_DELAY_MS * attempt);
    }
  }

  throw lastError;
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function renderPreviewHtml(html, metadata) {
  const tags = [
    ['meta', { name: 'description', content: metadata.description }],
    ['meta', { name: 'apple-itunes-app', content: `app-id=${IOS_APP_STORE_ID}, app-argument=${metadata.url}` }],
    ['meta', { property: 'og:type', content: 'article' }],
    ['meta', { property: 'og:site_name', content: 'Sanctuary' }],
    ['meta', { property: 'og:title', content: metadata.title }],
    ['meta', { property: 'og:description', content: metadata.description }],
    ['meta', { property: 'og:url', content: metadata.url }],
    ['meta', { property: 'og:image', content: metadata.imageUrl }],
    ['meta', { property: 'og:image:secure_url', content: metadata.imageUrl }],
    ['meta', { property: 'og:image:alt', content: metadata.title }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { name: 'twitter:title', content: metadata.title }],
    ['meta', { name: 'twitter:description', content: metadata.description }],
    ['meta', { name: 'twitter:image', content: metadata.imageUrl }],
    ['link', { rel: 'canonical', href: metadata.url }],
  ]
    .map(([tagName, attributes]) => renderTag(tagName, attributes))
    .join('\n    ');

  return html
    .replace(/<title>.*?<\/title>/, `<title>${escapeHtml(metadata.title)}</title>`)
    .replace(/<meta name="apple-itunes-app" content="[^"]*"\s*\/?>\s*/g, '')
    .replace('</head>', `    ${tags}\n  </head>`);
}

function renderTag(tagName, attributes) {
  const renderedAttributes = Object.entries(attributes)
    .map(([key, value]) => `${key}="${escapeHtml(value)}"`)
    .join(' ');

  return tagName === 'link' ? `<${tagName} ${renderedAttributes} />` : `<${tagName} ${renderedAttributes}>`;
}

function cleanText(value) {
  if (typeof value !== 'string') {
    return '';
  }

  return value.replace(/\s+/g, ' ').trim();
}

function absoluteUrl(value) {
  try {
    return new URL(value, SITE_ORIGIN).toString();
  } catch {
    return FALLBACK_IMAGE_URL;
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}
