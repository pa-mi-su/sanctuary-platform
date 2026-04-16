# Sanctuary DynamoDB Design

## Table: sanctuary_content

Primary key:
- PK (string)
- SK (string)

## Item patterns

### Content item
- PK = CONTENT#<type>
- SK = <slug>

Examples:
- PK = CONTENT#novena
- SK = 30-day-novena-to-st-joseph

- PK = CONTENT#saint
- SK = saint-martin-i

Attributes:
- id
- slug
- type
- title
- summary
- durationDays
- feastDate
- calendarDate
- body
- tags
- source
- createdAt
- updatedAt

### Date lookup item
- PK = DATE#<MM-DD>
- SK = <type>#<slug>

Examples:
- PK = DATE#04-13
- SK = saint#saint-martin-i

Purpose:
- fetch saints / liturgical content by calendar day

## Future tables

### sanctuary_user_state
Purpose:
- favorites
- novena progress
- reminder preferences

### sanctuary_search
Purpose:
- optional search index if direct content scanning becomes too expensive
