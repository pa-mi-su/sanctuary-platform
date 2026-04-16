# Sanctuary API Endpoints

## GET /content/novenas

Purpose:
- return the list of novenas for web, desktop, and future mobile API usage

Response shape:
```json
{
  "items": [
    {
      "id": "novena-001",
      "slug": "30-day-novena-to-st-joseph",
      "type": "novena",
      "title": "30 Day Novena to St Joseph",
      "summary": "Seek St. Joseph's powerful intercession today.",
      "durationDays": 30
    }
  ]
}
```

## Future endpoints
- GET /content/novenas/:slug
- GET /content/saints
- GET /content/saints/:slug
- GET /content/prayers
- GET /calendar/day/:mm-dd
- GET /search?q=...
