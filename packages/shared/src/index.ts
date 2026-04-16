export type LocaleCode = 'en' | 'es' | 'pl';
export type LocalizedText = Record<LocaleCode, string>;
export type ContentType = 'saint' | 'novena' | 'prayer' | 'liturgical_day';

export interface BaseContentItem {
  id: string;
  slug: string;
  type: ContentType;
}

export interface NovenaDay {
  dayNumber: number;
  titleByLocale: LocalizedText;
  scriptureByLocale: LocalizedText;
  prayerByLocale: LocalizedText;
  reflectionByLocale: LocalizedText;
  bodyByLocale: LocalizedText;
}

export interface NovenaItem extends BaseContentItem {
  type: 'novena';
  titleByLocale: LocalizedText;
  descriptionByLocale: LocalizedText;
  durationDays: number;
  tags: string[];
  imageURL?: string;
  days: NovenaDay[];
}

export interface SaintItem extends BaseContentItem {
  type: 'saint';
  titleByLocale: LocalizedText;
  descriptionByLocale?: LocalizedText;
  feastDate?: string;
}

export interface PrayerItem extends BaseContentItem {
  type: 'prayer';
  titleByLocale: LocalizedText;
  bodyByLocale?: LocalizedText;
}

export interface LiturgicalDayItem extends BaseContentItem {
  type: 'liturgical_day';
  titleByLocale: LocalizedText;
  calendarDate?: string;
}

export type ContentItem =
  | NovenaItem
  | SaintItem
  | PrayerItem
  | LiturgicalDayItem;
