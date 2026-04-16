import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const repoRoot = path.resolve(__dirname, '..', '..', '..');
const defaultSourcePath = path.resolve(
  '/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json',
);
const defaultOutputPath = path.resolve(
  repoRoot,
  'apps/api/out/novenas.dynamodb.json',
);

function buildTimestamp() {
  return new Date().toISOString();
}

function toDynamoNovenaSummaryItem(novena, timestamp) {
  return {
    PK: 'CONTENT#novena',
    SK: novena.slug,
    id: novena.id,
    slug: novena.slug,
    type: 'novena',
    titleByLocale: novena.titleByLocale,
    descriptionByLocale: novena.descriptionByLocale,
    durationDays: novena.durationDays,
    tags: Array.isArray(novena.tags) ? novena.tags : [],
    imageURL: novena.imageURL ?? null,
    dayCount: Array.isArray(novena.days) ? novena.days.length : 0,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

function toDynamoNovenaDayItem(novena, day, timestamp) {
  return {
    PK: 'CONTENT#novena',
    SK: `${novena.slug}#DAY#${String(day.dayNumber).padStart(2, '0')}`,
    parentSlug: novena.slug,
    id: `${novena.id}#day-${day.dayNumber}`,
    type: 'novena_day',
    dayNumber: day.dayNumber,
    titleByLocale: day.titleByLocale,
    scriptureByLocale: day.scriptureByLocale,
    prayerByLocale: day.prayerByLocale,
    reflectionByLocale: day.reflectionByLocale,
    bodyByLocale: day.bodyByLocale,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

async function main() {
  const sourcePath = process.argv[2] ? path.resolve(process.argv[2]) : defaultSourcePath;
  const outputPath = process.argv[3] ? path.resolve(process.argv[3]) : defaultOutputPath;

  const raw = await fs.readFile(sourcePath, 'utf8');
  const novenas = JSON.parse(raw);

  if (!Array.isArray(novenas)) {
    throw new Error('Expected novenas.json to contain an array at the top level.');
  }

  const timestamp = buildTimestamp();
  const items = novenas.flatMap((novena) => {
    const summaryItem = toDynamoNovenaSummaryItem(novena, timestamp);
    const dayItems = Array.isArray(novena.days)
      ? novena.days.map((day) => toDynamoNovenaDayItem(novena, day, timestamp))
      : [];

    return [summaryItem, ...dayItems];
  });

  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, `${JSON.stringify(items, null, 2)}\n`, 'utf8');

  console.log(`Imported ${items.length} novenas`);
  console.log(`Source: ${sourcePath}`);
  console.log(`Output: ${outputPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
