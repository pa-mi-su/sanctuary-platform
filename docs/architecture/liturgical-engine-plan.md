# Sanctuary Liturgical Engine Plan

## Goal

Create a backend-owned liturgical engine in Java that becomes the canonical authority for:

- liturgical season by date
- primary observance by date
- transferred feast resolution
- movable-feast anchor dates
- novena serving windows derived from liturgical rules
- daily readings URL generation

This engine should replace the current split legacy behavior where:

- `LiturgicalCalendarEngine` computes calendar/day answers in Swift
- `ContentStore` separately computes novena timing rules and feast anchors

## Why This Matters

The app depends on liturgical correctness for core product behavior:

- season coloring and daily experience
- saint/day relevance
- feast-day browsing
- novena start dates
- novena feast dates
- novena completion windows
- daily readings links

If this engine is wrong, the app is wrong.

## Current Legacy State

The legacy iOS app does **not** use `liturgical_days.json` as its real source of truth.

Actual authority today is split across two Swift systems:

1. `LiturgicalCalendarEngine`
- season computation
- major observances
- transferred Saint Joseph / Annunciation handling
- USCCB daily readings URL generation
- calendar UI/day-detail behavior

2. `ContentStore`
- novena serving windows
- anchor-date resolution from `novenas_index.json`
- additional feast/season anchor calculations
- duplicated transfer logic for Saint Joseph and Annunciation

This duplication is the main risk we should eliminate in the new platform.

## Design Principles

- one canonical engine, not two parallel rule systems
- computation lives in Java service code, not primarily in database rows
- Postgres stores content and novena serving-rule inputs, not the full computed liturgical truth
- outputs can be cached or materialized later if needed, but correctness comes first
- port the existing known-good Swift rules deliberately before expanding scope

## What The Java Engine Must Own

### 1. Date-to-day answer

Given a date, the engine should return:

- normalized date
- liturgical season
- primary rank label
- observances
- readings URL
- optional metadata for downstream use

Suggested model:

- `LiturgicalDayResult`
  - `LocalDate date`
  - `LiturgicalSeason season`
  - `String primaryRank`
  - `List<String> observances`
  - `URI readingsUrl`
  - `String rankType`

### 2. Anchor-date resolution

Given a year and anchor key, the engine should resolve dates for anchors used by novena rules.

Examples:

- easter
- ash_wednesday
- palm_sunday
- good_friday
- pentecost
- trinity_sunday
- corpus_christi
- advent_1
- christmas
- epiphany
- baptism_of_the_lord
- christ_king
- st_joseph
- annunciation

Suggested model:

- `LiturgicalAnchorService`
- `Map<LiturgicalAnchorKey, LocalDate>`

### 3. Feast transfer resolution

The engine must centrally own special transfer behavior already present in the app.

Known current transfers:

- Saint Joseph
- Annunciation

These should not be reimplemented separately in novena scheduling code.

### 4. Novena serving windows

The engine should resolve novena windows from rule metadata stored in Postgres.

Inputs:

- novena serving rule row
- target year

Outputs:

- start date
- end date
- feast date
- status for a given date if needed

Suggested model:

- `NovenaServingWindowResult`
  - `String novenaId`
  - `LocalDate startDate`
  - `LocalDate endDate`
  - `LocalDate feastDate`

## Recommended Java API Contract

### Core service methods

- `LiturgicalDayResult getLiturgicalDay(LocalDate date)`
- `LiturgicalSeason getSeason(LocalDate date)`
- `Optional<String> getPrimaryObservance(LocalDate date)`
- `Map<LiturgicalAnchorKey, LocalDate> getAnchors(int year)`
- `LocalDate getAnchor(LiturgicalAnchorKey key, int year)`
- `LocalDate getTransferredFeast(TransferredFeastKey key, int year)`
- `NovenaServingWindowResult getNovenaServingWindow(NovenaServingRule rule, int year)`

### REST endpoints later

- `GET /calendar/day/{yyyy-mm-dd}`
- `GET /calendar/anchors/{year}`
- `GET /calendar/novenas/{novenaId}/window/{year}`

## Proposed Java Package Layout

- `app.sanctuary.api.calendar`
- `app.sanctuary.api.calendar.model`
- `app.sanctuary.api.calendar.rules`
- `app.sanctuary.api.calendar.service`
- `app.sanctuary.api.calendar.web`

Example classes:

- `LiturgicalCalendarService`
- `LiturgicalAnchorService`
- `TransferredFeastResolver`
- `SeasonResolver`
- `NovenaServingWindowResolver`
- `ReadingsUrlBuilder`
- `LiturgicalDayResult`
- `NovenaServingWindowResult`

## Rule Sources

### In code

The following should live in Java logic:

- Gregorian computus for Easter
- season boundaries
- feast transfers
- rank precedence rules
- readings URL generation
- anchor derivation from Easter / Advent / Christmas cycle

### In Postgres

The following should live in Postgres as inputs:

- content tables
- novena serving rule metadata
- future optional calendar overrides if we need them

## What Not To Do

- do not treat `liturgical_days.json` as the canonical future backbone
- do not rebuild separate calendar and novena rule engines
- do not push all liturgical truth into database rows before we have a correct engine
- do not let feast-transfer logic drift between content lookup and calendar lookup paths

## Port Strategy

### Phase 1: parity with current Swift engine

Port current behavior exactly for:

- Easter computation
- season boundaries
- major observances currently hard-coded in `LiturgicalCalendarEngine`
- transferred Saint Joseph and Annunciation
- all anchor generation currently used by novena rules
- novena rule types currently used by `ContentStore`

### Phase 2: verification harness

Create Java tests using fixed known dates and parity fixtures from the legacy app.

Minimum coverage:

- known Easter years
- Ash Wednesday / Palm Sunday / Good Friday
- Advent 1
- Baptism of the Lord
- Christ the King
- Saint Joseph transfer years
- Annunciation transfer years
- representative novena serving windows for fixed, anchor, relative, and before-feast rules

### Phase 3: backend integration

Use the Java engine for:

- `/calendar/day/{date}`
- novena serving window resolution
- saint/day and novena/day lookup support

## Immediate Next Step

Implement the Java liturgical engine before auth work.

Recommended build order:

1. `LiturgicalSeason` and `LiturgicalDayResult`
2. Easter / date utility layer
3. season resolver
4. transferred feast resolver
5. anchor resolver
6. novena serving window resolver
7. tests mirroring current Swift behavior
8. first public calendar endpoint
