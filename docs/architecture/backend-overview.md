# Sanctuary Backend Overview

## Core Services
- Amazon Cognito
- API Gateway
- Lambda
- DynamoDB
- S3

## DynamoDB Tables

### sanctuary_content
Purpose:
- public shared content for all clients

Keys:
- PK (string)
- SK (string)

Item patterns:
- PK = CONTENT#novena, SK = <slug>
- PK = CONTENT#novena, SK = <slug>#DAY#<NN>
- PK = CONTENT#saint, SK = <slug>
- PK = CONTENT#prayer, SK = <slug>
- PK = DATE#<MM-DD>, SK = saint#<slug>
- PK = DATE#<MM-DD>, SK = liturgical_day#<slug>

### sanctuary_user_state
Purpose:
- authenticated per-user synced state

Keys:
- PK (string)
- SK (string)

Item patterns:
- PK = USER#<sub>, SK = PROFILE
- PK = USER#<sub>, SK = FAVORITE#saint#<slug>
- PK = USER#<sub>, SK = FAVORITE#novena#<slug>
- PK = USER#<sub>, SK = NOVENA_PROGRESS#<slug>
- PK = USER#<sub>, SK = NOVENA_DAY#<slug>#<NN>
- PK = USER#<sub>, SK = PREFERENCE#notifications
- PK = USER#<sub>, SK = PREFERENCE#language

## Authentication Model
- browsing content does not require login
- syncing state requires login
- Cognito will provide identity for authenticated routes

## Public API
- GET /content/novenas
- GET /content/novenas/{slug}
- GET /content/novenas/{slug}/days/{dayNumber}
- GET /content/saints
- GET /content/saints/{slug}
- GET /content/prayers
- GET /calendar/day/{mm-dd}
- GET /search?q=...

## Authenticated API
- GET /me
- GET /me/favorites
- PUT /me/favorites
- DELETE /me/favorites/{type}/{slug}
- GET /me/novenas
- PUT /me/novenas/{slug}/progress
- PUT /me/preferences
- GET /me/preferences

## Migration Strategy
1. import legacy JSON/content from the current iOS app
2. populate sanctuary_content
3. connect Angular to public content APIs
4. add Cognito and sanctuary_user_state
5. migrate current iOS app to backend endpoints in phases
