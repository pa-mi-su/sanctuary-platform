# Novena Item Shape

## Table
sanctuary_content

## Primary key
- PK = CONTENT#novena
- SK = <slug>

## Attributes
- id
- slug
- type
- titleByLocale
- descriptionByLocale
- durationDays
- tags
- imageURL
- days
- createdAt
- updatedAt

## Example
```json
{
  "PK": "CONTENT#novena",
  "SK": "14_holy_helpers",
  "id": "14_holy_helpers",
  "slug": "14_holy_helpers",
  "type": "novena",
  "titleByLocale": {
    "en": "Fourteen Holy Helpers Novena",
    "es": "Novena de los Catorce Auxiliares Santos",
    "pl": "Nowenna do Czternastu Świętych Wspomożycieli"
  },
  "descriptionByLocale": {
    "en": "Invoke Heaven’s emergency response team.",
    "es": "Invoca al equipo de respuesta de emergencia del Cielo.",
    "pl": "Wezwij niebiański zespół ratunkowy."
  },
  "durationDays": 9,
  "tags": ["Novena", "Devotion", "14 Holy Helpers"],
  "imageURL": "https://sanctuaryapp-media-prod.s3.us-east-1.amazonaws.com/novenas/14_holy_helpers.png",
  "days": [],
  "createdAt": "2026-04-14T00:00:00Z",
  "updatedAt": "2026-04-14T00:00:00Z"
}

