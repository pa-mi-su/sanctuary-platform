# Calendar and Home surface parity — November 27, 2026

Continue using the shared 27-role color contract in `design/tokens/colors.json`. No environment-specific theme has been added.

- Canvas: surface; panels: surface-card; unselected calendar dates: surface-raised; visible tile boundaries: border-strong; selection: primary with on-accent text. Today has a primary outline when not selected.
- Web Saints and Novenas week/month views use distinct rounded tiles, responsive spacing, and larger desktop labels. Day cards use the same neutral boundaries rather than a full seasonal outline. Season is communicated through the legend and date marker.
- iOS and Android calendar date tiles use the same raised sage and visible boundary roles. Existing selection and today behavior is retained.
- Web Home navigation cards use light sage surfaces with dark teal labels and icons; dark teal is reserved for the welcome hero and accents.
- Liturgical is removed from desktop and mobile web navigation to match native navigation. Daily Readings remains available. Existing calendar data support is retained for season context.
- Web, iOS and Android have one theme implementation each, shared by DEV/UAT/production builds. Environment settings select service endpoints, identity and signing, not a palette. Existing deployments only receive these changes when released; this change does not deploy production.

Validation: web production build and 69 tests passed; iOS Sanctuary-Dev simulator build and Android assembleDevDebug passed; shared palette/contrast and native theme checks passed. Local browser inspection confirmed outlined month/week cells, colored legends, and absence of the Liturgical tab. Local preview API requests were unavailable, so those visual checks used empty/error states rather than confirming live calendar content.
